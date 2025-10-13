package nl.ochagavia.krossover.codegen

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.TemplateException
import gg.jte.output.StringOutput
import gg.jte.resolve.ResourceCodeResolver
import nl.ochagavia.krossover.JvmLibrary
import org.gradle.api.GradleException
import kotlin.io.path.Path

object CodeGenerator {
    fun generatePython(publicApi: JvmLibrary): String {
        val codeResolver = ResourceCodeResolver("templates")
        val engine = TemplateEngine.create(codeResolver, Path("."), ContentType.Plain, this.javaClass.getClassLoader())
        engine.setTrimControlStructures(true)

        val output = StringOutput()
        val publicApi =
            PublicApi(
                publicApi.classes,
                publicApi.enums,
                publicApi.nestedClasses,
                ClassHierarchy(publicApi),
            )
        try {
            engine.render("python/main.jte", publicApi, output)
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
}
