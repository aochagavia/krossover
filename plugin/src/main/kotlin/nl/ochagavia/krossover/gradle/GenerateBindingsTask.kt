package nl.ochagavia.krossover.gradle

import kotlinx.serialization.json.Json
import nl.ochagavia.krossover.JvmLibrary
import nl.ochagavia.krossover.codegen.CodeGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GenerateBindingsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val publicApiMetadataFile: RegularFileProperty

    @get:OutputFile
    abstract val pythonFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val metadataJson = publicApiMetadataFile.get().asFile.readText(Charsets.UTF_8)
        val metadata = Json.decodeFromString(JvmLibrary.serializer(), metadataJson)
        pythonFile.get().asFile.writeText(CodeGenerator.generatePython(metadata))
    }
}
