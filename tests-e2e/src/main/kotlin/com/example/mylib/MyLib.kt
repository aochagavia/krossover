package com.example.mylib

// Sealed classes
sealed class MyOptional {
    class Some(val value: Int) : MyOptional()
    object None : MyOptional()
}

sealed class MyResult {
    class Success(val value: Int) : MyResult()
    class Failure(val message: String) : MyResult()
}

// Objects
object Math {
    fun divide(x: Int, y: Int): MyResult {
        return if (y == 0) {
            MyResult.Failure("attempted to divide by zero")
        } else {
            MyResult.Success(x / y)
        }
    }
}

// Companion objects
class NonZeroInt private constructor(val value: Int) {
    companion object {
        fun fromInt(value: Int): MyResult {
            if (value == 0) {
                return MyResult.Failure("attempted to create `NonZeroInt`, but the provided value was `0`")
            }

            return MyResult.Success(value)
        }
    }

    // Member functions
    fun divideByNonZero(x: Int): Int {
        return x / value
    }
}
