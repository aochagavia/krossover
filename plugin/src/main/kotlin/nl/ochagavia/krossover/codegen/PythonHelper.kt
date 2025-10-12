package nl.ochagavia.krossover.codegen

import nl.ochagavia.krossover.ClassName
import nl.ochagavia.krossover.JvmType
import nl.ochagavia.krossover.KotlinClass
import nl.ochagavia.krossover.KotlinFunctionParam

object PythonHelper {
    @JvmStatic
    fun trimGetter(getterName: String): String = getterName.removePrefix("get").replaceFirstChar { it.lowercase() }

    @JvmStatic
    fun classDef(clazz: KotlinClass): String {
        val name =
            if (clazz.name.isNestedClass()) {
                nestedClassDefName(clazz.name)
            } else {
                clazz.name.unqualifiedName()
            }

        val superclass = clazz.superclass
        val inherits =
            if (superclass == null) {
                ""
            } else {
                "(${superclass.name.unqualifiedNameWithNesting('.')})"
            }

        return "$name$inherits"
    }

    @JvmStatic
    fun nestedClassDefName(className: ClassName): String = "_${className.unqualifiedNameWithNesting('_')}"

    @JvmStatic
    fun castParam(
        publicApi: PublicApi,
        param: KotlinFunctionParam,
    ): String {
        if (publicApi.enums.containsKey(param.type.name)) {
            return "${param.name}._to_kotlin_enum()"
        }

        val primitive = JniHelper.toJniPrimitive(param.type)
        if (primitive != null) {
            return "ffi.cast('$primitive', ${param.name})"
        }

        return when (param.type.name) {
            ClassName.string -> "_python_str_to_java_string(${param.name})"
            else -> "${param.name}._jni_ref"
        }
    }

    @JvmStatic
    fun returnStatement(
        publicApi: PublicApi,
        returnType: JvmType?,
    ): String? {
        if (returnType == null) {
            return null
        }

        val fn = fromKotlinConversionFn(publicApi, returnType, 0)
        return "return ($fn)(result)"
    }

    fun fromKotlinConversionFn(
        publicApi: PublicApi,
        type: JvmType,
        nesting: Int,
    ): String {
        val lambdaParam = "x$nesting"
        val className = type.name.unqualifiedNameWithNesting('.')
        if (publicApi.enums.containsKey(type.name)) {
            return "lambda $lambdaParam: $className._from_kotlin_enum($lambdaParam)"
        }

        if (JniHelper.toJniPrimitive(type) != null) {
            // Primitives require no casting
            return "lambda $lambdaParam: $lambdaParam"
        }

        return when (type.name) {
            ClassName.string -> "_java_string_to_python_str"
            ClassName.map -> {
                if (type.params.size != 2) {
                    "lambda $lambdaParam: raise NotImplementedError"
                } else {
                    val keyConversion = fromKotlinConversionFn(publicApi, type.params[0], nesting + 1)
                    val valueConversion = fromKotlinConversionFn(publicApi, type.params[1], nesting + 1)
                    "lambda $lambdaParam: _from_kotlin_map($lambdaParam, $keyConversion, $valueConversion)"
                }
            }
            ClassName.list -> {
                if (type.params.isEmpty()) {
                    "lambda $lambdaParam: raise NotImplementedError"
                } else {
                    val itemConversion = fromKotlinConversionFn(publicApi, type.params[0], nesting + 1)
                    "lambda $lambdaParam: _from_kotlin_list($lambdaParam, $itemConversion)"
                }
            }
            else -> {
                if (publicApi.classHierarchy.hasChildren(type.name)) {
                    "lambda $lambdaParam: $className._downcast($lambdaParam)"
                } else {
                    "lambda $lambdaParam: _from_kotlin_object($className, $lambdaParam)"
                }
            }
        }
    }

    @JvmStatic
    fun formatDocString(
        indent: String,
        docString: String?,
    ): String {
        if (docString == null) {
            return ""
        }

        val tripleQuote = "\"\"\""

        val builder = StringBuilder()

        var lineCount = 0
        val lines = docString.lines()
        var firstNonEmptyLine = true
        lines.forEachIndexed { i, line ->
            if ((i == 0 || i == lines.size - 1) && line.isBlank()) {
                return@forEachIndexed
            }

            builder.append(indent)

            if (firstNonEmptyLine) {
                builder.append(tripleQuote)
                firstNonEmptyLine = false
            }

            builder.append(trimFirstSpace(line).trimEnd())
            builder.append("\n")
            lineCount++
        }

        if (builder.isNotEmpty()) {
            if (lineCount == 1) {
                builder.deleteAt(builder.length - 1)
            } else {
                builder.append(indent)
            }

            builder.append(tripleQuote)
            builder.append("\n")
        }

        return builder.toString()
    }

    private fun trimFirstSpace(line: String): String =
        if (line.firstOrNull() == ' ') {
            line.substring(1)
        } else {
            line
        }
}
