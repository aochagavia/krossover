package nl.ochagavia.krossover.gradle

import kotlinx.serialization.json.Json
import nl.ochagavia.krossover.ClassName
import nl.ochagavia.krossover.KotlinLibrary
import nl.ochagavia.krossover.codegen.CodeGenerator
import nl.ochagavia.krossover.codegen.RustConfig
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GenerateBindingsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val publicApiMetadataFile: RegularFileProperty

    @get:OutputFile
    abstract val jniHeaderPath: RegularFileProperty

    @get:OutputDirectory
    abstract val pythonDir: RegularFileProperty

    @get:OutputDirectory
    abstract val rustDir: RegularFileProperty

    @get:Input
    abstract val libName: Property<String>

    @get:Input
    abstract val rustJniSysModule: Property<String>

    @get:Nested
    abstract val rustReturnTypeMappings: MapProperty<ClassName, ReturnTypeMapping>

    @TaskAction
    fun generate() {
        val metadataJson = publicApiMetadataFile.get().asFile.readText(Charsets.UTF_8)
        val metadata = Json.decodeFromString(KotlinLibrary.serializer(), metadataJson)
        val rustConfig = RustConfig(rustJniSysModule.get(), rustReturnTypeMappings.get())
        val codeGenerator = CodeGenerator(metadata, libName.get(), rustConfig)
        codeGenerator.generatePython(pythonDir.get().asFile)
        codeGenerator.generateRust(rustDir.get().asFile)
        copyJniHeader()
    }

    private fun copyJniHeader() {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val fileName =
            if (isWindows) {
                "jni_simplified_win.h"
            } else {
                "jni_simplified_unix.h"
            }
        val platformSpecificJniHeaderFile = CodeGenerator.readTextFileFromResources("scaffolding/jni-headers/$fileName")
        jniHeaderPath.get().asFile.writeText(platformSpecificJniHeaderFile)
    }
}
