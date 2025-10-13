package nl.ochagavia.krossover.codegen

@ConsistentCopyVisibility
data class JniPrimitive private constructor(
    val type: String,
) {
    companion object {
        val jint = JniPrimitive("jint")
        val jlong = JniPrimitive("jlong")
        val jbyte = JniPrimitive("jbyte")
        val jboolean = JniPrimitive("jboolean")
        val jchar = JniPrimitive("jchar")
        val jshort = JniPrimitive("jshort")
        val jfloat = JniPrimitive("jfloat")
        val jdouble = JniPrimitive("jdouble")
    }

    override fun toString(): String = type
}
