package nl.ochagavia.krossover.codegen

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.TemplateException
import gg.jte.output.StringOutput
import gg.jte.resolve.ResourceCodeResolver
import nl.ochagavia.krossover.KotlinLibrary
import org.gradle.api.GradleException
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

class CodeGenerator {
    val publicApi: PublicApi
    val engine: TemplateEngine

    constructor(
        kotlinLibrary: KotlinLibrary,
        libName: String,
        rustConfig: RustConfig,
    ) {
        val codeResolver = ResourceCodeResolver("templates")
        engine = TemplateEngine.create(codeResolver, Path("."), ContentType.Plain, this::class.java.classLoader)
        engine.setTrimControlStructures(true)
        publicApi =
            PublicApi(
                kotlinLibrary.classes,
                kotlinLibrary.sealedSubclasses,
                kotlinLibrary.enums,
                kotlinLibrary.nestedClasses,
                ClassHierarchy(kotlinLibrary),
                libName,
                rustConfig,
            )
    }

    fun render(template: String): String {
        val output = StringOutput()
        try {
            engine.render(template, publicApi, output)
        } catch (e: TemplateException) {
            if (e.cause == null) {
                throw e
            } else {
                val fullStackTrace = e.cause!!.stackTraceToString()
                val stackTraceFragment = fullStackTrace.take(400)
                val stackTraceCropped =
                    if (stackTraceFragment.length < fullStackTrace.length) {
                        "\n<stack trace cropped for readability>"
                    } else {
                        ""
                    }
                throw GradleException("failed to render template: $stackTraceFragment$stackTraceCropped")
            }
        }
        return output.toString()
    }

    fun generatePython(dir: File) {
        val lib = render("python/main.jte")

        dir.toPath().createDirectories()
        dir.resolve("__init__.py").writeText(lib)
    }

    fun generateRust(dir: File) {
        val lib = render("rust/main.jte")

        dir.toPath().createDirectories()
        dir.resolve("mod.rs").writeText(lib)

        val sourceFile = readTextFileFromResources("scaffolding/rust/util.rs")
        dir.resolve("util.rs").writeText(sourceFile)
    }

    companion object {
        internal fun readTextFileFromResources(path: String): String =
            CodeGenerator::class.java.classLoader
                .getResourceAsStream(path)!!
                .readAllBytes()
                .toString(Charsets.UTF_8)
    }
}
