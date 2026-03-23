package nl.ochagavia.krossover.codegen

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.TemplateException
import gg.jte.output.StringOutput
import nl.ochagavia.krossover.ClassName
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
        engine = TemplateEngine.createPrecompiled(Path("."), ContentType.Plain, this::class.java.classLoader)
        engine.setTrimControlStructures(true)

        val classHierarchy = ClassHierarchy(kotlinLibrary)
        publicApi =
            PublicApi(
                kotlinLibrary.classes,
                sortClasses(kotlinLibrary.classes.keys, classHierarchy),
                kotlinLibrary.sealedSubclasses,
                kotlinLibrary.enums,
                kotlinLibrary.nestedClasses,
                classHierarchy,
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
        return output.toString().lines().joinToString("\n") { it.trimEnd() }
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

/**
 * Topologically sort classes so parents are listed before their children
 */
private fun sortClasses(
    classes: Iterable<ClassName>,
    classHierarchy: ClassHierarchy,
): List<ClassName> {
    val visited = mutableSetOf<ClassName>()
    val sorted = mutableListOf<ClassName>()

    // Note that this is not a generic topological sort, but works here because nodes have
    // at most one child (Kotlin only supports single-parent inheritance). Hence, BFS is
    // enough.
    val queue = ArrayDeque<ClassName>()
    for (className in classes) {
        if (!classHierarchy.hasSuperclass(className)) {
            // Classes without a parent come first, then their children (recursively)
            queue.add(className)
        }
    }

    while (queue.isNotEmpty()) {
        val className = queue.removeFirst()
        if (visited.contains(className)) {
            continue
        }

        visited.add(className)
        sorted.add(className)

        for (child in classHierarchy.directChildren(className)) {
            queue.add(child)
        }
    }

    return sorted
}
