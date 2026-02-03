package com.example

class Dummy {
    fun foo(): NestedClass1 = NestedClass1()

    fun processList(items: List<NestedClass1>): Int {
        return items.size
    }

    fun withNullableParam(required: String, optional: String?): String = required + (optional ?: "")

    class NestedDummy
}

class NestedClass1

object Object {
    fun foo(): NestedClass2 = NestedClass2()
}

class NestedClass2 {
    fun foo(x: DoublyNestedClass) {
    }

    fun processItems(items: List<DoublyNestedClass>): Int {
        return items.size
    }
}

class DoublyNestedClass

class IgnoredRootClass {
    fun ignoredFun(x: IgnoredNestedClass1): IgnoredNestedClass1 = IgnoredNestedClass1()
}

class IgnoredNestedClass1

class IgnoredNestedClass2
