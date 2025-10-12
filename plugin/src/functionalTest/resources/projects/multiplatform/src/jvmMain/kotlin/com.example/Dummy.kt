package com.example

class Dummy {
    fun foo(): NestedClass1 = NestedClass1()
}

class NestedClass1

object Object {
    fun foo(): NestedClass2 = NestedClass2()
}

class NestedClass2 {
    fun foo(x: DoublyNestedClass) {
    }
}

class DoublyNestedClass

class IgnoredRootClass {
    fun ignoredFun(x: IgnoredNestedClass1): IgnoredNestedClass1 = IgnoredNestedClass1()
}

class IgnoredNestedClass1

class IgnoredNestedClass2
