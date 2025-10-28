package nl.ochagavia.krossover.gradle

import kotlinx.serialization.json.Json
import nl.ochagavia.krossover.ClassName
import nl.ochagavia.krossover.KotlinLibrary
import nl.ochagavia.krossover.jni.JniClassConfig
import nl.ochagavia.krossover.jni.JniConfig
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GenerateGraalNativeImageConfigTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val publicApiMetadataFile: RegularFileProperty

    @get:Input
    abstract val additionalJniClasses: ListProperty<String>

    @get:OutputFile
    abstract val jniConfigOutputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val requiredJavaClasses =
            listOf(
                "java.lang.Class",
                "java.util.List",
                "java.util.Map",
                "java.util.Iterator", // Necessary to iterate through maps and lists
                "java.util.Set", // Necessary to iterate through maps
                "java.util.Map\$Entry", // Necessary to iterate through maps
            )

        val metadataJson = publicApiMetadataFile.get().asFile.readText(Charsets.UTF_8)
        val metadata = Json.decodeFromString(KotlinLibrary.serializer(), metadataJson)
        val classes = metadata.classes.map { JniClassConfig(it.value.name) }
        val enums = metadata.enums.map { JniClassConfig(it.key) }
        val additional =
            additionalJniClasses.getOrElse(emptyList<String>()).plus(requiredJavaClasses).map {
                JniClassConfig(ClassName.notNested(it))
            }
        val jniConfig = JniConfig(classes + enums + additional)
        jniConfigOutputFile.get().asFile.writeText(Json.encodeToString(jniConfig))
    }
}
