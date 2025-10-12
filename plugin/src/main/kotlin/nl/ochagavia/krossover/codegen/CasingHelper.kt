package nl.ochagavia.krossover.codegen

import nl.ochagavia.krossover.KotlinFunctionParam

object CasingHelper {
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
    fun snakeCase(name: String): String = splitCamelCased(name).joinToString("_") { it.lowercase() }

    @JvmStatic
    fun snakeCase(param: KotlinFunctionParam): KotlinFunctionParam {
        val cased = splitCamelCased(param.name).joinToString("_") { it.lowercase() }
        return KotlinFunctionParam(cased, param.type)
    }
}
