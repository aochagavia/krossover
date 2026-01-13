import nl.ochagavia.krossover.ClassName
import nl.ochagavia.krossover.KotlinFunctionParam
import nl.ochagavia.krossover.KotlinType
import nl.ochagavia.krossover.codegen.RustHelper
import kotlin.test.Test
import kotlin.test.assertEquals

class RustHelperTests {
    // Tests for paramType with nullable types
    @Test
    fun testParamTypeNullableString() {
        val type = KotlinType(ClassName.string, isNullable = true)
        assertEquals("Option<&str>", RustHelper.paramType(type))
    }

    @Test
    fun testParamTypeNonNullableString() {
        val type = KotlinType(ClassName.string, isNullable = false)
        assertEquals("&str", RustHelper.paramType(type))
    }

    @Test
    fun testParamTypeNullableInt() {
        val type = KotlinType(ClassName.int, isNullable = true)
        assertEquals("Option<i32>", RustHelper.paramType(type))
    }

    @Test
    fun testParamTypeNonNullableInt() {
        val type = KotlinType(ClassName.int, isNullable = false)
        assertEquals("i32", RustHelper.paramType(type))
    }

    @Test
    fun testParamTypeNullableBoolean() {
        val type = KotlinType(ClassName.boolean, isNullable = true)
        assertEquals("Option<bool>", RustHelper.paramType(type))
    }

    @Test
    fun testParamTypeNullableUserDefined() {
        val type = KotlinType(ClassName.notNested("com.example.MyClass"), isNullable = true)
        assertEquals("Option<MyClass>", RustHelper.paramType(type))
    }

    @Test
    fun testParamTypeNonNullableUserDefined() {
        val type = KotlinType(ClassName.notNested("com.example.MyClass"), isNullable = false)
        assertEquals("MyClass", RustHelper.paramType(type))
    }

    @Test
    fun testParamTypeNullableList() {
        val innerType = KotlinType(ClassName.string, isNullable = false)
        val type = KotlinType(ClassName.list, isNullable = true, params = listOf(innerType))
        assertEquals("Option<&[&str]>", RustHelper.paramType(type))
    }

    // Tests for castParamToObject with nullable types
    @Test
    fun testCastParamToObjectNonNullableString() {
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.string, isNullable = false))
        val result = RustHelper.castParamToObject(param)
        assertEquals(
            "let myParam_ptr = myParam.to_kotlin_object();\nlet myParam = myParam_ptr.as_kotlin_object();",
            result
        )
    }

    @Test
    fun testCastParamToObjectNullableString() {
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.string, isNullable = true))
        val result = RustHelper.castParamToObject(param)
        assertEquals(
            "let myParam_ptr = myParam.map(|v| v.to_kotlin_object());\nlet myParam = myParam_ptr.as_ref().map(|p| p.as_kotlin_object()).unwrap_or(std::ptr::null_mut());",
            result
        )
    }

    @Test
    fun testCastParamToObjectNonNullableUserDefined() {
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.notNested("com.example.MyClass"), isNullable = false))
        val result = RustHelper.castParamToObject(param)
        assertEquals(
            "let myParam_ptr = myParam.to_kotlin_object();\nlet myParam = myParam_ptr.as_kotlin_object();",
            result
        )
    }

    @Test
    fun testCastParamToObjectNullableUserDefined() {
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.notNested("com.example.MyClass"), isNullable = true))
        val result = RustHelper.castParamToObject(param)
        assertEquals(
            "let myParam_ptr = myParam.map(|v| v.to_kotlin_object());\nlet myParam = myParam_ptr.as_ref().map(|p| p.as_kotlin_object()).unwrap_or(std::ptr::null_mut());",
            result
        )
    }

    @Test
    fun testCastParamToObjectNullableList() {
        val innerType = KotlinType(ClassName.string, isNullable = false)
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.list, isNullable = true, params = listOf(innerType)))
        val result = RustHelper.castParamToObject(param)
        assertEquals(
            "let myParam_ptr = myParam.map(|v| util::to_kotlin_list(v));\nlet myParam = myParam_ptr.as_ref().map(|p| p.as_kotlin_object()).unwrap_or(std::ptr::null_mut());",
            result
        )
    }

    @Test
    fun testCastParamToObjectNullableMap() {
        val keyType = KotlinType(ClassName.string, isNullable = false)
        val valueType = KotlinType(ClassName.int, isNullable = false)
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.map, isNullable = true, params = listOf(keyType, valueType)))
        val result = RustHelper.castParamToObject(param)
        assertEquals(
            "let myParam_ptr = myParam.map(|v| util::to_kotlin_map(v));\nlet myParam = myParam_ptr.as_ref().map(|p| p.as_kotlin_object()).unwrap_or(std::ptr::null_mut());",
            result
        )
    }

    @Test
    fun testCastParamToObjectNonNullableBoolean() {
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.boolean, isNullable = false))
        val result = RustHelper.castParamToObject(param)
        assertEquals("let myParam = myParam as c_int;", result)
    }

    @Test
    fun testCastParamToObjectNullableBoolean() {
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.boolean, isNullable = true))
        val result = RustHelper.castParamToObject(param)
        assertEquals("let myParam = myParam.map(|v| v as c_int);", result)
    }

    @Test
    fun testCastParamToObjectNonNullableInt() {
        val param = KotlinFunctionParam("myParam", KotlinType(ClassName.int, isNullable = false))
        val result = RustHelper.castParamToObject(param)
        // Primitives (except boolean) require no casting
        assertEquals("", result)
    }

    // Tests for returnTypeAnnotation with nullable types
    @Test
    fun testReturnTypeAnnotationNullableString() {
        val type = KotlinType(ClassName.string, isNullable = true)
        val result = RustHelper.returnTypeAnnotation(emptyMap(), type)
        assertEquals(" -> Option<String>", result)
    }

    @Test
    fun testReturnTypeAnnotationNonNullableString() {
        val type = KotlinType(ClassName.string, isNullable = false)
        val result = RustHelper.returnTypeAnnotation(emptyMap(), type)
        assertEquals(" -> String", result)
    }

    @Test
    fun testReturnTypeAnnotationNullableUserDefined() {
        val type = KotlinType(ClassName.notNested("com.example.MyClass"), isNullable = true)
        val result = RustHelper.returnTypeAnnotation(emptyMap(), type)
        assertEquals(" -> Option<MyClass>", result)
    }

    @Test
    fun testReturnTypeAnnotationNullableList() {
        val innerType = KotlinType(ClassName.string, isNullable = false)
        val type = KotlinType(ClassName.list, isNullable = true, params = listOf(innerType))
        val result = RustHelper.returnTypeAnnotation(emptyMap(), type)
        assertEquals(" -> Option<Vec<String>>", result)
    }

    @Test
    fun testReturnTypeAnnotationUnit() {
        val type = KotlinType(ClassName.unit, isNullable = false)
        val result = RustHelper.returnTypeAnnotation(emptyMap(), type)
        assertEquals("", result)
    }

    @Test
    fun testReturnTypeAnnotationNull() {
        val result = RustHelper.returnTypeAnnotation(emptyMap(), null)
        assertEquals("", result)
    }
}