import nl.ochagavia.krossover.ClassName
import nl.ochagavia.krossover.KotlinFunctionParam
import nl.ochagavia.krossover.KotlinLibrary
import nl.ochagavia.krossover.KotlinType
import nl.ochagavia.krossover.codegen.ClassHierarchy
import nl.ochagavia.krossover.codegen.PublicApi
import nl.ochagavia.krossover.codegen.PythonHelper
import nl.ochagavia.krossover.codegen.RustConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class PythonHelperTests {
    @Test
    fun testNestedClassDefName() {
        val class1 = ClassName.notNested("com.example.Outer\$Inner")
        assertEquals("_Outer_Inner", PythonHelper.nestedClassDefName(class1))

        val class2 = ClassName.notNested("com.example.Outer\$Inner1\$Inner2")
        assertEquals("_Outer_Inner1_Inner2", PythonHelper.nestedClassDefName(class2))
    }

    // Tests for typeAnnotation with nullable types
    @Test
    fun testTypeAnnotationNullableString() {
        val type = KotlinType(ClassName.string, isNullable = true)
        assertEquals("Optional[str]", PythonHelper.typeAnnotation(type))
    }

    @Test
    fun testTypeAnnotationNonNullableString() {
        val type = KotlinType(ClassName.string, isNullable = false)
        assertEquals("str", PythonHelper.typeAnnotation(type))
    }

    @Test
    fun testTypeAnnotationNullableInt() {
        val type = KotlinType(ClassName.int, isNullable = true)
        assertEquals("Optional[int]", PythonHelper.typeAnnotation(type))
    }

    @Test
    fun testTypeAnnotationNullableUserDefined() {
        val type = KotlinType(ClassName.notNested("com.example.MyClass"), isNullable = true)
        assertEquals("Optional[MyClass]", PythonHelper.typeAnnotation(type))
    }

    @Test
    fun testTypeAnnotationNullableList() {
        val innerType = KotlinType(ClassName.string, isNullable = false)
        val type = KotlinType(ClassName.list, isNullable = true, params = listOf(innerType))
        assertEquals("Optional[List[str]]", PythonHelper.typeAnnotation(type))
    }

    // Tests for returnTypeAnnotation with nullable types
    @Test
    fun testReturnTypeAnnotationNullableString() {
        val type = KotlinType(ClassName.string, isNullable = true)
        assertEquals(" -> Optional[str]", PythonHelper.returnTypeAnnotation(type))
    }

    @Test
    fun testReturnTypeAnnotationUnit() {
        val type = KotlinType(ClassName.unit, isNullable = false)
        assertEquals("", PythonHelper.returnTypeAnnotation(type))
    }

    // Helper to create empty PublicApi for tests
    private fun emptyPublicApi(): PublicApi {
        val emptyLibrary = KotlinLibrary(
            classes = hashMapOf(),
            enums = hashMapOf(),
            nestedClasses = hashMapOf(),
            sealedSubclasses = hashSetOf(),
            externalTypes = emptyList()
        )
        return PublicApi(
            classes = hashMapOf(),
            sealedSubclasses = hashSetOf(),
            enums = hashMapOf(),
            nestedClasses = hashMapOf(),
            classHierarchy = ClassHierarchy(emptyLibrary),
            libName = "test",
            rustConfig = RustConfig("test_sys", emptyMap())
        )
    }

    // Tests for castParam with nullable types
    @Test
    fun testCastParamNonNullableString() {
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.string, isNullable = false))
        assertEquals("_python_str_to_java_string(myParam)", PythonHelper.castParam(emptyPublicApi(), param))
    }

    @Test
    fun testCastParamNullableString() {
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.string, isNullable = true))
        assertEquals(
            "_python_str_to_java_string(myParam) if myParam is not None else ffi.NULL",
            PythonHelper.castParam(emptyPublicApi(), param)
        )
    }

    @Test
    fun testCastParamNonNullableUserDefined() {
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.notNested("com.example.MyClass"), isNullable = false))
        assertEquals("myParam._jni_ref", PythonHelper.castParam(emptyPublicApi(), param))
    }

    @Test
    fun testCastParamNullableUserDefined() {
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.notNested("com.example.MyClass"), isNullable = true))
        assertEquals(
            "myParam._jni_ref if myParam is not None else ffi.NULL",
            PythonHelper.castParam(emptyPublicApi(), param)
        )
    }

    @Test
    fun testCastParamNullableList() {
        val innerType = KotlinType(ClassName.string, isNullable = false)
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.list, isNullable = true, params = listOf(innerType)))
        assertEquals(
            "_to_kotlin_list(myParam) if myParam is not None else ffi.NULL",
            PythonHelper.castParam(emptyPublicApi(), param)
        )
    }

    // Tests for fromKotlinConversionFn with nullable types
    @Test
    fun testFromKotlinConversionFnNonNullableString() {
        val type = KotlinType(ClassName.string, isNullable = false)
        assertEquals("_java_string_to_python_str", PythonHelper.fromKotlinConversionFn(emptyPublicApi(), type, 0))
    }

    @Test
    fun testFromKotlinConversionFnNullableString() {
        val type = KotlinType(ClassName.string, isNullable = true)
        assertEquals(
            "lambda x0: None if x0 == ffi.NULL else (_java_string_to_python_str)(x0)",
            PythonHelper.fromKotlinConversionFn(emptyPublicApi(), type, 0)
        )
    }

    @Test
    fun testFromKotlinConversionFnNullableUserDefined() {
        val type = KotlinType(ClassName.notNested("com.example.MyClass"), isNullable = true)
        assertEquals(
            "lambda x0: None if x0 == ffi.NULL else (lambda x0: _from_kotlin_object(MyClass, x0))(x0)",
            PythonHelper.fromKotlinConversionFn(emptyPublicApi(), type, 0)
        )
    }
}
