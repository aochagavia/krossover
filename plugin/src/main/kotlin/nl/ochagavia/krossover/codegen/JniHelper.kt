package nl.ochagavia.krossover.codegen

import nl.ochagavia.krossover.ClassName
import nl.ochagavia.krossover.JvmType
import nl.ochagavia.krossover.KotlinFunctionParam

object JniHelper {
    @JvmStatic
    fun returnTypeToJniCallName(type: JvmType?): String {
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
    fun toJniPrimitive(type: JvmType): String? =
        when (type.name) {
            ClassName.int -> "jint"
            ClassName.long -> "jlong"
            ClassName.byte -> "jbyte"
            ClassName.boolean -> "jboolean"
            ClassName.char -> "jchar"
            ClassName.short -> "jshort"
            ClassName.float -> "jfloat"
            ClassName.double -> "jdouble"
            else -> null
        }

    @JvmStatic
    fun toJniFunctionSignature(
        params: List<KotlinFunctionParam>,
        returnType: JvmType?,
    ): String {
        val returnTypeJni =
            if (returnType == null || returnType == JvmType.unit()) {
                "V"
            } else {
                toJniSignatureType(returnType)
            }

        return "(${params.joinToString("") { toJniSignatureType(it.type) }})$returnTypeJni"
    }

    private fun toJniSignatureType(type: JvmType): String {
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

    private fun primitiveToJniSignature(type: String): String? {
        return when (type) {
            "jint" -> "I"
            "jlong" -> "J"
            "jbyte" -> "B"
            "jboolean" -> "Z"
            "jchar" -> "C"
            "jshort" -> "S"
            "jfloat" -> "F"
            "jdouble" -> "D"
            else -> return null
        }
    }
}
