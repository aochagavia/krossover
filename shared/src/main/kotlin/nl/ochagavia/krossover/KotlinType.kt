package nl.ochagavia.krossover

import kotlinx.serialization.Serializable

@Serializable
data class KotlinType(
    val name: ClassName,
    val params: List<KotlinType> = emptyList(),
    val isStackAllocated: Boolean = false,
) {
    companion object {
        @JvmStatic
        fun unit(): KotlinType = KotlinType(ClassName.unit)
    }
}
