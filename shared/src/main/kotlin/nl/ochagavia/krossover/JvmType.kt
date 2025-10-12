package nl.ochagavia.krossover

import kotlinx.serialization.Serializable

@Serializable
data class JvmType(
    val name: ClassName,
    val params: List<JvmType> = emptyList(),
    val isStackAllocated: Boolean = false,
) {
    companion object {
        @JvmStatic
        fun unit(): JvmType = JvmType(ClassName.unit)
    }
}
