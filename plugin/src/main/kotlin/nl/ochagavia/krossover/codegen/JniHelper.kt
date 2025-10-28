package nl.ochagavia.krossover.codegen

import nl.ochagavia.krossover.ClassName
import nl.ochagavia.krossover.KotlinFunctionParam
import nl.ochagavia.krossover.KotlinType

object JniHelper {
    @JvmStatic
    fun returnTypeToJniCallName(type: KotlinType?): String {
        val s =
            when (type?.name) {
                ClassName.int -> "Int"
                ClassName.long -> "Long"
                ClassName.byte -> "Byte"
                ClassName.boolean -> "Boolean"
                ClassName.char -> "Char"
                ClassName.short -> "Short"
                ClassName.float -> "Float"
                ClassName.double -> "Double"
                ClassName.unit -> "Void"
                null -> "Void"
                else -> "Object"
            }

        return "${s}Method"
    }

    @JvmStatic
    fun toJniPrimitive(type: KotlinType): JniPrimitive? =
        when (type.name) {
            ClassName.int -> JniPrimitive.jint
            ClassName.long -> JniPrimitive.jlong
            ClassName.byte -> JniPrimitive.jbyte
            ClassName.boolean -> JniPrimitive.jboolean
            ClassName.char -> JniPrimitive.jchar
            ClassName.short -> JniPrimitive.jshort
            ClassName.float -> JniPrimitive.jfloat
            ClassName.double -> JniPrimitive.jdouble
            else -> null
        }

    @JvmStatic
    fun toJniFunctionSignature(
        params: List<KotlinFunctionParam>,
        returnType: KotlinType?,
    ): String {
        val returnTypeJni =
            if (returnType == null || returnType == KotlinType.unit()) {
                "V"
            } else {
                toJniSignature(returnType)
            }

        return "(${params.joinToString("") { toJniSignature(it.type) }})$returnTypeJni"
    }

    @JvmStatic
    fun toJniSignature(type: KotlinType): String {
        val primitive = toJniPrimitive(type)
        return if (primitive == null) {
            val jvmType =
                when (type.name) {
                    // Some Kotlin types get compiled down to their java equivalents
                    ClassName.string -> "java/lang/String"
                    ClassName.list -> "java/util/List"
                    ClassName.map -> "java/util/Map"
                    else -> type.name.fullyQualifiedJniName()
                }
            "L$jvmType;"
        } else {
            primitiveToJniSignature(primitive)!!
        }
    }

    private fun primitiveToJniSignature(type: JniPrimitive): String? {
        return when (type) {
            JniPrimitive.jint -> "I"
            JniPrimitive.jlong -> "J"
            JniPrimitive.jbyte -> "B"
            JniPrimitive.jboolean -> "Z"
            JniPrimitive.jchar -> "C"
            JniPrimitive.jshort -> "S"
            JniPrimitive.jfloat -> "F"
            JniPrimitive.jdouble -> "D"
            else -> return null
        }
    }
}
