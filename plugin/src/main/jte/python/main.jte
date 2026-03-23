@import nl.ochagavia.krossover.codegen.PublicApi

@param PublicApi publicApi
from __future__ import annotations

import sys
import threading
from typing import cast, Any, Callable, Dict, List, Optional, Type, TypeAlias
from cffi import FFI
from pathlib import Path
from enum import Enum

tls = threading.local()

JNI_OK = 0

# Utility functions
def _raise_exception_if_any(env: Any):
    if env[0].ExceptionCheck(env) == 1:
        env[0].ExceptionDescribe(env)
        env[0].ExceptionClear(env)
        raise Exception("entered unreachable code")

def _raise_if_null(env: Any, ptr: Any):
    if ptr == ffi.NULL:
        _raise_exception_if_any(env)
        raise Exception("entered unreachable code")

# Initialize library
ffi = FFI()

package_dir = Path(__file__).parent
with open(package_dir / "jni_simplified.h", "r") as f:
    header = f.read()
ffi.cdef(header)

LIBRARY_NAMES: Dict[str, str] = {
    "win32": "${publicApi.getLibName()}.dll",
    "darwin": "lib${publicApi.getLibName()}.dylib",
    "linux": "lib${publicApi.getLibName()}.so",
}

lib_name = LIBRARY_NAMES.get(sys.platform)
if lib_name is None:
    raise RuntimeError(f"Unsupported platform: {sys.platform}")

lib: Any = ffi.dlopen(str(package_dir / lib_name))
env_ptr = ffi.new("JNIEnv **")
jvm_ptr = ffi.new("JavaVM **")

vm_args = ffi.new("JavaVMInitArgs *")
vm_args[0].version = 0x00010008  # JNI_VERSION_1_8
vm_args[0].nOptions = 0
vm_args[0].options = ffi.NULL
vm_args[0].ignoreUnrecognized = 1  # JNI_TRUE

if lib.JNI_CreateJavaVM(jvm_ptr, ffi.cast("void **", env_ptr), vm_args) != 0:
    raise Exception("failed to load ${publicApi.getLibName()} dynamic library")

jvm = ffi.gc(jvm_ptr[0], lambda x: x[0].DestroyJavaVM(x))

###############
# JNI Helpers #
###############

class AttachedJniThread:
    """Automatically attaches/detaches the thread to the JNI."""

    should_detach: bool

    def __enter__(self):
        self.should_detach = False

        if getattr(tls, 'attached_jni_thread', None):
            return tls.attached_jni_thread

        env_ptr = ffi.new("JNIEnv **")
        if jvm[0].AttachCurrentThread(jvm, ffi.cast("void **", env_ptr), ffi.NULL) != JNI_OK:
            raise RuntimeError("failed to attach JNI thread")

        tls.attached_jni_thread = env_ptr[0]
        self.should_detach = True
        return env_ptr[0]

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.should_detach:
            if jvm[0].DetachCurrentThread(jvm) != JNI_OK:
                raise RuntimeError("failed to detach JNI thread")
            tls.attached_jni_thread = None


def _delete_local_ref(env, jni_ref: Any):
    env[0].DeleteLocalRef(env, ffi.cast("jobject", jni_ref))

def _delete_global_ref(jni_ref):
    with AttachedJniThread() as env:
        env[0].DeleteGlobalRef(env, ffi.cast("jobject", jni_ref))

def _to_gc_global_ref(env, jni_ref: Any) -> Any:
    global_jni_ref = env[0].NewGlobalRef(env, jni_ref)
    _delete_local_ref(env, jni_ref)
    return ffi.gc(global_jni_ref, _delete_global_ref)

def _get_class(env, class_name: bytes) -> Any:
    class_name_cstr = ffi.new("char[]", class_name)
    clazz = env[0].FindClass(env, class_name_cstr)
    _raise_if_null(env, clazz)
    return _to_gc_global_ref(env, clazz)

def _get_method(env, clazz: Any, method_name: bytes, method_signature: bytes) -> Any:
    method_name_cstr = ffi.new("char[]", method_name)
    method_sig_cstr = ffi.new("char[]", method_signature)
    method = env[0].GetMethodID(env, clazz, method_name_cstr, method_sig_cstr)
    _raise_if_null(env, method)
    return method

def _construct(class_name: bytes, constructor_signature: bytes, args: Any) -> Any:
    with AttachedJniThread() as env:
        clazz = _get_class(env, class_name)
        constructor = _get_method(env, clazz, b"<init>", constructor_signature)
        jni_ref = env[0].NewObject(env, clazz, constructor, *args)
        _raise_exception_if_any(env)
        return _to_gc_global_ref(env, jni_ref)

def _access_static_field(class_name: bytes, field_name: bytes, field_type: bytes) -> Any:
    with AttachedJniThread() as env:
        c = _get_class(env, class_name)
        signature_cstr = ffi.new("char[]", field_type)
        field_name_cstr = ffi.new("char[]", field_name)

        # Get static id
        field = env[0].GetStaticFieldID(env, c, field_name_cstr, signature_cstr)
        _raise_if_null(env, field)

        # Access field
        field_value = _to_gc_global_ref(env, env[0].GetStaticObjectField(env, c, field))
        _raise_if_null(env, field_value)
        return field_value

def _call_method_raw(env: Any, class_name: bytes, jni_ref: Any, func_name: bytes, func_signature: bytes, jni_call_name: str, args: List[Any]) -> Any:
    clazz = _get_class(env, class_name)
    method = _get_method(env, clazz, func_name, func_signature)
    result = getattr(env[0], f"Call{jni_call_name}")(env, jni_ref, method, *args)
    _raise_exception_if_any(env)
    return result

def _call_method(class_name: bytes, jni_ref: Any, func_name: bytes, func_signature: bytes, jni_call_name: str, args: List[Any]) -> Any:
    with AttachedJniThread() as env:
        result = _call_method_raw(env, class_name, jni_ref, func_name, func_signature, jni_call_name, args)
        if jni_call_name == "ObjectMethod":
            result = _to_gc_global_ref(env, result)
        return result

def _python_str_to_java_string(s: str) -> Any:
    utf16_bytes = s.encode("utf-16-le")
    utf16_str_len = len(utf16_bytes) / 2
    if utf16_str_len.is_integer():
        utf16_str_len = int(utf16_str_len)
    else:
        raise RuntimeError("entered unreachable code: raw string length was not divisible by 2")
    utf16_str = ffi.new("char[]", utf16_bytes)

    with AttachedJniThread() as env:
        jni_ref = env[0].NewString(env, ffi.cast("jchar *", utf16_str), utf16_str_len)
        _raise_if_null(env, jni_ref)
        return _to_gc_global_ref(env, jni_ref)

def _java_string_to_python_str(jni_ref: Any) -> str:
    with AttachedJniThread() as env:
        native_chars = env[0].GetStringChars(env, jni_ref, ffi.NULL)
        _raise_if_null(env, native_chars)
        native_chars_byte_len = env[0].GetStringLength(env, jni_ref) * 2
        python_str = bytes(cast(Any, ffi.buffer(native_chars, native_chars_byte_len))).decode("utf-16-le", "strict")
        env[0].ReleaseStringChars(env, jni_ref, native_chars)
        return python_str

def _jni_class_name(jni_ref: Any):
    if jni_ref == ffi.NULL:
        raise RuntimeError("entered unreachable code: attempted to obtain class name of null object")

    with AttachedJniThread() as env:
        clazz = env[0].GetObjectClass(env, jni_ref)
        _raise_if_null(env, clazz)
        name_local = _call_method_raw(env, b"java/lang/Class", clazz, b"getName", b"()Ljava/lang/String;", "ObjectMethod", [])
        name = _to_gc_global_ref(env, name_local)
        _delete_local_ref(env, clazz)
        return _java_string_to_python_str(name)

def _from_kotlin_object(python_class, jni_ref):
    obj = object.__new__(python_class)
    obj._jni_ref = jni_ref
    return obj

def _get_jni_ref(object) -> Any:
    if hasattr(object, '_jni_ref'):
        return object._jni_ref

    if isinstance(object, str):
        return _python_str_to_java_string(object)

    raise TypeError(f"Cannot convert value to Kotlin: expected object with _jni_ref attribute or something convertible to a Kotlin type, got {type(object).__name__}")

def _from_kotlin_list(
    jni_ref: Any, wrap_item_fn: Callable[[Any], Any]
) -> List[Any]:
    python_list: List[Any] = []
    iterator_class_name = b"java/util/Iterator"
    iterator = _call_method(b"java/util/List", jni_ref, b"iterator", b"()Ljava/util/Iterator;", "ObjectMethod", [])
    while True:
        has_next = _call_method(iterator_class_name, iterator, b"hasNext", b"()Z", "BooleanMethod", [])
        if has_next == 0:
            break

        item_ref = _call_method(iterator_class_name, iterator, b"next", b"()Ljava/lang/Object;", "ObjectMethod", [])
        python_list.append(wrap_item_fn(item_ref))

    return python_list

def _to_kotlin_list(list: List[Any]) -> Any:
    with AttachedJniThread() as env:

        # Create a new ArrayList
        array_list_class = _get_class(env, b"java/util/ArrayList")
        constructor = _get_method(env, array_list_class, b"<init>", b"()V")
        array_list = env[0].NewObject(env, array_list_class, constructor)
        _raise_exception_if_any(env)
        array_list = _to_gc_global_ref(env, array_list)

        # Add each element to the list
        for item in list:
            if not hasattr(item, '_jni_ref'):
                raise TypeError(f"Cannot convert item to Kotlin: expected object with _jni_ref attribute, got {type(item).__name__}")
            item_ref = item._jni_ref
            _call_method_raw(env, b"java/util/ArrayList", array_list, b"add", b"(Ljava/lang/Object;)Z", "BooleanMethod", [item_ref])
            _raise_exception_if_any(env)

        return array_list

def _from_kotlin_map(
    jni_ref: Any,
    wrap_key_fn: Callable[[Any], Any],
    wrap_value_fn: Callable[[Any], Any]
) -> Dict[Any, Any]:
    python_dict: dict[Any, Any] = {}
    entry_set = _call_method(b"java/util/Map", jni_ref, b"entrySet", b"()Ljava/util/Set;", "ObjectMethod", [])
    iterator = _call_method(b"java/util/Set", entry_set, b"iterator", b"()Ljava/util/Iterator;", "ObjectMethod", [])

    iterator_class_name = b"java/util/Iterator"
    pair_class_name = b"java/util/Map$Entry"
    while True:
        has_next = _call_method(iterator_class_name, iterator, b"hasNext", b"()Z", "BooleanMethod", [])
        if has_next == 0:
            break

        pair_ref = _call_method(iterator_class_name, iterator, b"next", b"()Ljava/lang/Object;", "ObjectMethod", [])
        _key_ref =  _call_method(pair_class_name, pair_ref, b"getKey", b"()Ljava/lang/Object;", "ObjectMethod", [])
        _value_ref =  _call_method(pair_class_name, pair_ref, b"getValue", b"()Ljava/lang/Object;", "ObjectMethod", [])
        python_dict[wrap_key_fn(_key_ref)] = wrap_value_fn(_value_ref)

    return python_dict

def _to_kotlin_map(items: Dict[Any, Any]) -> Any:
    with AttachedJniThread() as env:

        # Create a new HashMap
        map_class = _get_class(env, b"java/util/HashMap")
        constructor = _get_method(env, map_class, b"<init>", b"()V")
        map = env[0].NewObject(env, map_class, constructor)
        _raise_exception_if_any(env)
        map = _to_gc_global_ref(env, map)

        # Add entries to it
        for key, value in items.items():
            key_ref = _get_jni_ref(key)
            value_ref = _get_jni_ref(value)
            _call_method_raw(env, b"java/util/HashMap", map, b"put", b"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", "ObjectMethod", [key_ref, value_ref])
            _raise_exception_if_any(env)

        return map


class KotlinObjectBase:
    def __eq__(self, other):
        return _call_method(b"java/lang/Object", self._jni_ref, b"equals", b"(Ljava/lang/Object;)Z", "BooleanMethod", [other._jni_ref])

    def __hash__(self):
        return _call_method(b"java/lang/Object", self._jni_ref, b"hashCode", b"()I", "IntMethod", [])

############
# Wrappers #
############

@for(var className : publicApi.getTopologicallySortedClasses())
@if(!className.isNestedClass())
<%-- Only generate code here for non-nested classes, because nested ones will be handled by their parents --%>
@template.python.classDefinition(publicApi = publicApi, clazz = publicApi.getClasses().get(className))
@endif
@endfor

@for(var enumClass : publicApi.getEnums().values())
@template.python.enumDefinition(enumClass = enumClass)
@endfor
