package nl.ochagavia.krossover.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

class ClassDeclCollectorOptions(
    val rootClasses: List<String>,
    val packages: List<String>,
    val apiJsonPath: Path,
)

class ClassDeclCollectorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        environment.logger.info("krossover: creating symbol processor...")
        val rootClasses = environment.options["rootClasses"]?.split(',') ?: emptyList()
        val packages = environment.options["packages"]?.split(',') ?: emptyList()

        val apiJsonPath = environment.options["apiJsonPath"]
        if (apiJsonPath == null) {
            environment.logger.error("apiJsonPath not specified")
            throw IllegalArgumentException("apiJsonPath must be specified")
        }

        val options = ClassDeclCollectorOptions(rootClasses, packages, Path(apiJsonPath))

        return ClassDeclCollector(environment.logger, environment.codeGenerator, options)
    }
}

class ClassDeclCollector(
    val logger: KSPLogger,
    val codeGenerator: CodeGenerator,
    val options: ClassDeclCollectorOptions,
) : SymbolProcessor {
    var done: Boolean = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("krossover: symbol processor is running...")
        if (done) {
            return emptyList()
        }

        // Gather class declarations
        val visitor = TopLevelClassDeclarationVisitor(options.rootClasses)
        resolver.getAllFiles().forEach { file ->
            file.accept(visitor, Unit)
        }

        // Gather public API metadata
        val kotlinVisitor =
            PackageMetadataVisitor(visitor.topLevelClassDeclarations, options.packages)
        while (kotlinVisitor.visitNextClass()) {
            // Iterate until all relevant classes have been visited
        }

        // Write the metadata to a KSP-managed file (this shouldn't be necessary, but it solved an
        // issue in which the KSP task would be skipped right after a `gradle clean`)
        val stream = codeGenerator.createNewFile(Dependencies.ALL_FILES, "krossover", "api", "json")
        val writer = stream.writer()
        val metadata = kotlinVisitor.getPackageMetadata()
        val json = Json.encodeToString(metadata)
        writer.write(json)
        writer.close()

        // Write the metadata again, this time to a file controlled by us
        options.apiJsonPath.createParentDirectories()
        options.apiJsonPath.writeText(json)

        logger.info(
            "krossover: class metadata collected through KSP and written to `${options.apiJsonPath.absolutePathString()}` (${metadata.classes.size} classes exposed)",
        )
        done = true
        return arrayListOf()
    }
}

class TopLevelClassDeclarationVisitor(
    rootClasses: List<String>,
) : KSTopDownVisitor<Unit, Unit>() {
    val allowedTopLevelClasses: HashSet<String> = HashSet(rootClasses)
    val topLevelClassDeclarations: ArrayList<KSClassDeclaration> = arrayListOf()

    override fun defaultHandler(
        node: KSNode,
        data: Unit,
    ) {
        // Nothing to do here
    }

    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: Unit,
    ) {
        val name = classDeclaration.qualifiedName?.asString() ?: classDeclaration.simpleName.asString()
        if (allowedTopLevelClasses.contains(name)) {
            topLevelClassDeclarations.add(classDeclaration)
        }
    }
}
