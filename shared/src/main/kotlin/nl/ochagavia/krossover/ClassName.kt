package nl.ochagavia.krossover

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ClassName.CustomSerializer::class)
@ConsistentCopyVisibility
data class ClassName private constructor(
    // Class names are represented according to the names they have in the file system, using dots as path separators (e.g. `com.example.Outer$Inner`)
    private val name: String,
) {
    companion object {
        fun notNested(name: String): ClassName = ClassName(name)

        /** Create a new [ClassName] based on the package name and the fully qualified class name
         *
         * @param packageName The name of the package in which the class is defined (e.g. `com.example`).
         * @param fullyQualifiedJavaClassName The fully qualified name of the class (e.g. `com.example.MyClass` or `com.example.Outer.Inner`).
         */
        fun potentiallyNested(
            packageName: String,
            fullyQualifiedJavaClassName: String,
        ): ClassName {
            val className = fullyQualifiedJavaClassName.removePrefix(packageName).removePrefix(".")

            // Any remaining dots in the class name mean there is nesting, which we represent by `$`
            // to differentiate that from the package separator (this is similar to what class names
            // become when compiled and packaged in a JAR).
            val unambiguousClassName = className.replace('.', '$')

            val name =
                if (packageName.isEmpty()) {
                    unambiguousClassName
                } else {
                    "$packageName.$unambiguousClassName"
                }

            return ClassName(name)
        }

        val string = notNested("kotlin.String")
        val map = notNested("kotlin.collections.Map")
        val list = notNested("kotlin.collections.List")
        val any = notNested("kotlin.Any")
        val unit = notNested("kotlin.Unit")
        val int = notNested("kotlin.Int")
        val long = notNested("kotlin.Long")
        val byte = notNested("Kotlin.Byte")
        val boolean = notNested("kotlin.Boolean")
        val char = notNested("kotlin.Char")
        val short = notNested("kotlin.Short")
        val float = notNested("kotlin.Float")
        val double = notNested("kotlin.Double")
    }

    fun withNestedClass(
        packageName: String,
        fullyQualifiedJavaClassName: String,
    ): ClassName {
        val classToNest = potentiallyNested(packageName, fullyQualifiedJavaClassName)
        return ClassName("${this.name}\$${classToNest.name}")
    }

    fun isNestedClass(): Boolean = name.contains('$')

    fun fullyQualifiedName(): String = name

    fun fullyQualifiedJniName(): String = name.replace(".", "/")

    fun fullyQualifiedJavaName(): String = name.replace('$', '.')

    fun unqualifiedNameWithNesting(overrideNestedClassSeparator: String? = null): String {
        val nameWithoutPackage = name.substringAfterLast('.', name)
        return if (overrideNestedClassSeparator == null) {
            nameWithoutPackage
        } else {
            nameWithoutPackage.replace("$", overrideNestedClassSeparator)
        }
    }

    fun unqualifiedName(): String {
        val nameWithoutPackage = name.substringAfterLast('.', name)
        val nameWithoutParent = nameWithoutPackage.substringAfterLast('$', nameWithoutPackage)
        return nameWithoutParent
    }

    fun packageName(): String = name.substringBeforeLast('.')

    object CustomSerializer : KSerializer<ClassName> {
        private val delegate = String.serializer()

        override val descriptor: SerialDescriptor = delegate.descriptor

        override fun serialize(
            encoder: Encoder,
            value: ClassName,
        ) {
            delegate.serialize(encoder, value.name)
        }

        override fun deserialize(decoder: Decoder): ClassName = ClassName(delegate.deserialize(decoder))
    }
}
