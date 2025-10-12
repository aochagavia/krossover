package nl.ochagavia.krossover.jni

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.ochagavia.krossover.ClassName

@Serializable(with = JniConfigAsListSerializer::class)
class JniConfig(
    val classes: List<JniClassConfig>,
)

@Serializable
class JniClassConfig(
    val name: ClassName,
    @Required
    val allPublicConstructors: Boolean = true,
    @Required
    val allPublicFields: Boolean = true,
    @Required
    val allPublicMethods: Boolean = true,
)

object JniConfigAsListSerializer : KSerializer<JniConfig> {
    private val delegate = ListSerializer(JniClassConfig.serializer())

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(
        encoder: Encoder,
        value: JniConfig,
    ) {
        delegate.serialize(encoder, value.classes)
    }

    override fun deserialize(decoder: Decoder): JniConfig {
        val list = delegate.deserialize(decoder)
        return JniConfig(list)
    }
}
