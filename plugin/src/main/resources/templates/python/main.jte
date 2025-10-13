@import nl.ochagavia.krossover.codegen.PublicApi

@param PublicApi publicApi
from __future__ import annotations

import sys
import threading
from typing import cast, Any, Callable, Dict, List, Type, TypeAlias
from cffi import FFI
from pathlib import Path
from enum import Enum

tls = threading.local()
tls.attached_jni_thread = None

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
    "win32": "kson.dll",
    "darwin": "libkson.dylib",
    "linux": "libkson.so",
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
    raise Exception("failed to load KSON dynamic library")

jvm = ffi.gc(jvm_ptr[0], lambda x: x[0].DestroyJavaVM(x))

###############
# JNI Helpers #
###############

def _attach_jni_thread() -> Any:
    if tls.attached_jni_thread:
        return tls.attached_jni_thread

    env_ptr = ffi.new("JNIEnv **")
    if jvm[0].AttachCurrentThread(jvm, ffi.cast("void **", env_ptr), ffi.NULL) != JNI_OK:
        raise RuntimeError("failed to attach JNI thread")

    tls.attached_jni_thread = env_ptr[0]
    return env_ptr[0]

def _detach_jni_thread():
    if jvm[0].DetachCurrentThread(jvm) != JNI_OK:
        raise RuntimeError("failed to detach JNI thread")
    tls.attached_jni_thread = None

def _delete_local_ref(env, jni_ref: Any):
    env[0].DeleteLocalRef(env, ffi.cast("jobject", jni_ref))

def _delete_global_ref(jni_ref):
    should_detach = tls.attached_jni_thread is None
    env = _attach_jni_thread()
    env[0].DeleteGlobalRef(env, ffi.cast("jobject", jni_ref))
    if should_detach:
        _detach_jni_thread()

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
    env = _attach_jni_thread()
    clazz = _get_class(env, class_name)
    constructor = _get_method(env, clazz, b"<init>", constructor_signature)
    jni_ref = env[0].NewObject(env, clazz, constructor, *args)
    _raise_exception_if_any(env)
    jni_ref_global = _to_gc_global_ref(env, jni_ref)
    _detach_jni_thread()
    return jni_ref_global

def _access_static_field(class_name: bytes, field_name: bytes, field_type: bytes) -> Any:
    env = _attach_jni_thread()
    c = _get_class(env, class_name)
    signature_cstr = ffi.new("char[]", field_type)
    field_name_cstr = ffi.new("char[]", field_name)

    # Get static id
    field = env[0].GetStaticFieldID(env, c, field_name_cstr, signature_cstr)
    _raise_if_null(env, field)

    # Access field
    field_value = _to_gc_global_ref(env, env[0].GetStaticObjectField(env, c, field))
    _raise_if_null(env, field_value)
    _detach_jni_thread()
    return field_value

def _call_method_raw(env: Any, class_name: bytes, jni_ref: Any, func_name: bytes, func_signature: bytes, jni_call_name: str, args: List[Any]) -> Any:
    clazz = _get_class(env, class_name)
    method = _get_method(env, clazz, func_name, func_signature)
    result = getattr(env[0], f"Call{jni_call_name}")(env, jni_ref, method, *args)
    _raise_exception_if_any(env)
    return result

def _call_method(class_name: bytes, jni_ref: Any, func_name: bytes, func_signature: bytes, jni_call_name: str, args: List[Any]) -> Any:
    env = _attach_jni_thread()
    result = _call_method_raw(env, class_name, jni_ref, func_name, func_signature, jni_call_name, args)
    if jni_call_name == "ObjectMethod":
        result = _to_gc_global_ref(env, result)
    _detach_jni_thread()
    return result

def _python_str_to_java_string(s: str) -> Any:
    utf16_bytes = s.encode("utf-16-le")
    utf16_str_len = len(utf16_bytes) / 2
    if utf16_str_len.is_integer():
        utf16_str_len = int(utf16_str_len)
    else:
        raise RuntimeError("entered unreachable code: raw string length was not divisible by 2")
    utf16_str = ffi.new("char[]", utf16_bytes)

    env = _attach_jni_thread()
    jni_ref = env[0].NewString(env, ffi.cast("jchar *", utf16_str), utf16_str_len)
    _raise_if_null(env, jni_ref)
    jni_ref = _to_gc_global_ref(env, jni_ref)
    _detach_jni_thread()
    return jni_ref

def _java_string_to_python_str(jni_ref: Any) -> str:
    env = _attach_jni_thread()
    native_chars = env[0].GetStringChars(env, jni_ref, ffi.NULL)
    _raise_if_null(env, native_chars)
    native_chars_byte_len = env[0].GetStringLength(env, jni_ref) * 2
    python_str = bytes(cast(Any, ffi.buffer(native_chars, native_chars_byte_len))).decode("utf-16-le", "strict")
    env[0].ReleaseStringChars(env, jni_ref, native_chars)
    _detach_jni_thread()
    return python_str

def _jni_class_name(jni_ref: Any):
    if jni_ref == ffi.NULL:
        raise RuntimeError("entered unreachable code: attempted to obtain class name of null object")

    env = _attach_jni_thread()
    clazz = env[0].GetObjectClass(env, jni_ref)
    _raise_if_null(env, clazz)
    name_local = _call_method_raw(env, b"java/lang/Class", clazz, b"getName", b"()Ljava/lang/String;", "ObjectMethod", [])
    name = _to_gc_global_ref(env, name_local)
    _delete_local_ref(env, clazz)
    _detach_jni_thread()
    return _java_string_to_python_str(name)

def _from_kotlin_object(python_class, jni_ref):
    obj = object.__new__(python_class)
    obj._jni_ref = jni_ref
    return obj

def _from_kotlin_list(
    jni_ref: Any, wrap_item_fn: Callable[[Any], Any]
) -> List[Any]:
    python_list: List[Any] = []
    iterator_class_name = b"org/kson/SimpleListIterator"
    iterator = _construct(iterator_class_name, b"(Ljava/util/List;)V", [jni_ref])
    while True:
        item_ref = _call_method(iterator_class_name, iterator, b"next", b"()Ljava/lang/Object;", "ObjectMethod", [])
        if item_ref == ffi.NULL:
            break
        python_list.append(wrap_item_fn(item_ref))

    return python_list

def _to_kotlin_list(list: List[Any]) -> Any:
    raise RuntimeError("not implemented")

def _to_kotlin_map(list: Dict[Any, Any]) -> Any:
    raise RuntimeError("not implemented")

def _from_kotlin_map(
    jni_ref: Any,
    wrap_key_fn: Callable[[Any], Any],
    wrap_value_fn: Callable[[Any], Any]
) -> Dict[Any, Any]:
    python_dict: dict[Any, Any] = {}
    iterator_class_name = b"org/kson/SimpleMapIterator"
    pair_class_name = b"org/kson/SimpleMapEntry"
    iterator = _construct(iterator_class_name, b"(Ljava/util/Map;)V", [jni_ref])
    while True:
        pair_ref = _call_method(iterator_class_name, iterator, b"next", b"()Lorg/kson/SimpleMapEntry;", "ObjectMethod", [])
        if pair_ref == ffi.NULL:
            break
        _key_ref =  _call_method(pair_class_name, pair_ref, b"getKey", b"()Ljava/lang/Object;", "ObjectMethod", [])
        _value_ref =  _call_method(pair_class_name, pair_ref, b"getValue", b"()Ljava/lang/Object;", "ObjectMethod", [])
        python_dict[wrap_key_fn(_key_ref)] = wrap_value_fn(_value_ref)

    return python_dict

############
# Wrappers #
############

@for(var clazz : publicApi.getClasses().values())
@if(!clazz.getName().isNestedClass())
<%-- Only generate code here for non-nested classes, because nested ones will be handled by their parents --%>
@template.python.classDefinition(publicApi = publicApi, clazz = clazz)
@endif
@endfor

@for(var enumClass : publicApi.getEnums().values())
@template.python.enumDefinition(enumClass = enumClass)
@endfor
