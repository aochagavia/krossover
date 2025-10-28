package nl.ochagavia.krossover.codegen

import nl.ochagavia.krossover.ClassName
import nl.ochagavia.krossover.KotlinFunctionParam
import nl.ochagavia.krossover.KotlinType
import nl.ochagavia.krossover.gradle.ReturnTypeMapping

object RustHelper {
    @JvmStatic
    fun qualifiedClassName(className: ClassName): String =
        if (className.isNestedClass()) {
            val parts = className.unqualifiedNameParts()
            val mod = parts.take(parts.size - 1).joinToString("::") { IdentHelper.snakeCase(it) }
            val prefix =
                if (mod.isEmpty()) {
                    ""
                } else {
                    "$mod::"
                }
            "$prefix${parts.last()}"
        } else {
            className.unqualifiedName()
        }

    @JvmStatic
    fun enumVariantName(variantName: ClassName): String = IdentHelper.pascalCase(variantName.unqualifiedName())

    @JvmStatic
    fun sanitizeIdentifier(identifier: String): String {
        val rustKeywords = listOf("type")
        if (rustKeywords.contains(identifier)) {
            return "${identifier}_"
        }

        return identifier
    }

    @JvmStatic
    fun formatDocString(
        indent: String,
        docString: String?,
    ): String {
        if (docString == null) {
            return ""
        }

        val builder = StringBuilder()
        val lines = docString.lines()
        lines.forEachIndexed { i, line ->
            if ((i == 0 || i == lines.size - 1) && line.trim().isEmpty()) {
                return@forEachIndexed
            }

            builder.append(indent)
            builder.append("/// ")
            builder.append(line.trimStart())
            builder.append("\n")
        }

        return builder.toString().trimEnd()
    }

    @JvmStatic
    fun castParamToObject(param: KotlinFunctionParam): String {
        if (param.type.name == ClassName.boolean) {
            return "let ${param.name} = ${param.name} as c_int;"
        }

        val primitive = JniHelper.toJniPrimitive(param.type)
        if (primitive != null) {
            // No casting is necessary for primitives
            return ""
        }

        val expr =
            when (param.type.name) {
                ClassName.string -> "${param.name}.to_kotlin_object()"
                ClassName.list -> "util::to_kotlin_list(${param.name})"
                ClassName.map -> "util::to_kotlin_map(${param.name})"
                // User-defined
                else -> "${param.name}.to_kotlin_object()"
            }

        return "let ${param.name}_ptr = $expr;\nlet ${param.name} = ${param.name}_ptr.as_kotlin_object();"
    }

    @JvmStatic
    fun paramType(type: KotlinType): String {
        val rustType =
            when (type.name) {
                // Primitives
                ClassName.byte,
                ClassName.char,
                -> "i8"

                ClassName.short -> "i16"
                ClassName.int -> "i32"
                ClassName.long -> "i64"
                ClassName.boolean -> "bool"
                ClassName.float -> "f32"
                ClassName.double -> "f64"
                // Built-ins and collections
                ClassName.string -> "&str"
                ClassName.any -> "KotlinAny"
                ClassName.list -> "&[${genericParamTypeAnnotation(emptyMap(), type, 0, false)}]"
                ClassName.map ->
                    "&std::collections::HashMap<${
                        genericParamTypeAnnotation(
                            emptyMap(),
                            type,
                            0,
                            false,
                        )
                    }, ${genericParamTypeAnnotation(emptyMap(), type, 1, false)}>"
                // We consider anything else to be user-defined
                else -> qualifiedClassName(type.name)
            }

        return if (type.isNullable) {
            "Option<$rustType>"
        } else {
            rustType
        }
    }

    private fun returnType(
        returnTypeMappings: Map<ClassName, ReturnTypeMapping>,
        type: KotlinType,
    ): String {
        val mapping = returnTypeMappings[type.name]
        if (mapping != null) {
            return mapping.rustType
        }

        val rustType =
            when (type.name) {
                // Primitives
                ClassName.byte,
                ClassName.char,
                -> "i8"
                ClassName.short -> "i16"
                ClassName.int -> "i32"
                ClassName.long -> "i64"
                ClassName.boolean -> "bool"
                ClassName.float -> "f32"
                ClassName.double -> "f64"
                // Built-ins and collections
                ClassName.string -> "String"
                ClassName.any -> "KotlinAny"
                ClassName.list -> "Vec<${genericParamTypeAnnotation(returnTypeMappings, type, 0, true)}>"
                ClassName.map -> "std::collections::HashMap<${genericParamTypeAnnotation(
                    returnTypeMappings,
                    type,
                    0,
                    true,
                )}, ${genericParamTypeAnnotation(returnTypeMappings, type, 1, true)}>"
                // We consider anything else to be user-defined
                else -> qualifiedClassName(type.name)
            }

        return if (type.isNullable) {
            "Option<$rustType>"
        } else {
            rustType
        }
    }

    private fun genericParamTypeAnnotation(
        returnTypeMappings: Map<ClassName, ReturnTypeMapping>,
        type: KotlinType,
        paramIndex: Int,
        owned: Boolean,
    ): String {
        val param = type.params.getOrNull(paramIndex) ?: return "KotlinAny"
        return if (owned) {
            returnType(returnTypeMappings, param)
        } else {
            paramType(param)
        }
    }

    @JvmStatic
    fun returnStatement(
        returnTypeMappings: Map<ClassName, ReturnTypeMapping>,
        returnType: KotlinType?,
    ): String? {
        if (returnType == null) {
            return null
        }

        if (returnType.name == ClassName.boolean) {
            return "result != 0"
        }

        if (JniHelper.toJniPrimitive(returnType) != null) {
            // Primitives require no casting
            return "result"
        }

        val conversionFn = returnTypeMappings[returnType.name]?.conversionFunction
        val fn =
            when (returnType.name) {
                ClassName.map -> "util::from_kotlin_value_map"
                ClassName.list -> "util::from_kotlin_list"
                else -> "FromKotlinObject::from_kotlin_object"
            }

        return if (conversionFn == null) {
            "$fn(result)"
        } else {
            "$conversionFn($fn(result))"
        }
    }

    @JvmStatic
    fun returnTypeAnnotation(
        returnTypeMappings: Map<ClassName, ReturnTypeMapping>,
        type: KotlinType?,
    ): String {
        if (type == null || type.name == ClassName.unit) return ""
        return " -> ${returnType(returnTypeMappings, type)}"
    }

    @JvmStatic
    fun enumConstructor(
        outerType: ClassName,
        variantName: ClassName,
    ): String {
        val constructor = StringBuilder()
        val packageName = outerType.packageName()
        val outerTypeQualified = outerType.fullyQualifiedName()
        val variantQualified = variantName.fullyQualifiedName()

        val startIndex =
            if (variantQualified.startsWith(outerTypeQualified) && variantQualified[outerTypeQualified.length] == '$') {
                outerTypeQualified.length
            } else {
                throw IllegalArgumentException("outer class name was not a prefix of the variant name")
            }

        val classNameBoundaries =
            variantQualified.indices
                .drop(startIndex)
                .filter {
                    variantQualified[it] == '$'
                }.plusElement(variantQualified.length)
        classNameBoundaries.windowed(2, 1) {
            val outerName = ClassName.potentiallyNested(packageName, variantQualified.take(it[0]))
            val innerName = ClassName.potentiallyNested(packageName, variantQualified.take(it[1]))
            constructor.append(qualifiedClassName(outerName))
            constructor.append("::")
            constructor.append(innerName.unqualifiedName())
            constructor.append('(')
        }

        // The last part gets special treatment
        constructor.append(qualifiedClassName(variantName))
        constructor.append("::from_kotlin_object(obj)")

        repeat(classNameBoundaries.size - 1) {
            constructor.append(')')
        }

        return constructor.toString()
    }
}
