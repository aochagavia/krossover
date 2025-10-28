package nl.ochagavia.krossover.ksp

import com.google.devtools.ksp.processing.CodeGenerator
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
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

class ClassDeclCollectorOptions(
    val rootClasses: List<String>,
    val packages: List<String>,
    val apiJsonPath: Path,
)

class ClassDeclCollectorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val rootClasses = environment.options["rootClasses"]?.split(',') ?: emptyList()
        val packages = environment.options["packages"]?.split(',') ?: emptyList()

        val apiJsonPath =
            environment.options["apiJsonPath"]
                ?: throw IllegalArgumentException("apiJsonPath must be specified")

        val options = ClassDeclCollectorOptions(rootClasses, packages, Path(apiJsonPath))

        return ClassDeclCollector(environment.codeGenerator, options)
    }
}

class ClassDeclCollector(
    val codeGenerator: CodeGenerator,
    val options: ClassDeclCollectorOptions,
) : SymbolProcessor {
    var done: Boolean = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
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

        // Write the metadata
        val metadata = kotlinVisitor.getPackageMetadata()
        val json = Json.encodeToString(metadata)
        options.apiJsonPath.createParentDirectories()
        options.apiJsonPath.writeText(json)

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
