package nl.ochagavia.krossover.codegen

import nl.ochagavia.krossover.KotlinFunctionParam

enum class Casing {
    // All uppercase letters, separated by underscores
    Uppercase,
    CamelCase,
}

object IdentHelper {
    fun split(
        name: String,
        sourceCasing: Casing,
    ): List<String> =
        when (sourceCasing) {
            Casing.Uppercase -> splitUpperCased(name)
            Casing.CamelCase -> splitCamelCased(name)
        }

    fun splitUpperCased(name: String): List<String> = name.split('_')

    fun splitCamelCased(name: String): List<String> {
        val parts = mutableListOf<String>()
        var currentPartStart = 0
        name.forEachIndexed { i, char ->
            if (char.isUpperCase()) {
                if (i > 0) {
                    parts.add(name.substring(currentPartStart, i))
                    currentPartStart = i
                }
            }
        }

        if (currentPartStart < name.length) {
            parts.add(name.substring(currentPartStart))
        }

        return parts
    }

    @JvmStatic
    fun pascalCase(name: String): String =
        split(name, Casing.Uppercase).joinToString("") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }

    @JvmStatic
    fun snakeCase(name: String): String = split(name, Casing.CamelCase).joinToString("_") { it.lowercase() }

    @JvmStatic
    fun snakeCase(param: KotlinFunctionParam): KotlinFunctionParam = KotlinFunctionParam(snakeCase(param.name), param.type)

    @JvmStatic
    fun trimGetter(getterName: String): String = getterName.removePrefix("get").replaceFirstChar { it.lowercase() }
}
