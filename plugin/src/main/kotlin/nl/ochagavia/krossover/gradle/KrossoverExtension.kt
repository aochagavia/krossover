package nl.ochagavia.krossover.gradle

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import java.nio.file.Path
import javax.inject.Inject

open class KrossoverExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        val rootClasses: ListProperty<String> = objects.listProperty(String::class.java)
        val exposedPackages: ListProperty<String> = objects.listProperty(String::class.java)
        val additionalJniClasses: ListProperty<String> = objects.listProperty(String::class.java)
        val libName: Property<String> = objects.property(String::class.java)
        val jniHeaderOutputFile: Property<Path> = objects.property(Path::class.java)

        val python: KrossoverPythonExtension = objects.newInstance(KrossoverPythonExtension::class.java)

        fun python(action: Action<in KrossoverPythonExtension>) = action.execute(python)

        val rust: KrossoverRustExtension = objects.newInstance(KrossoverRustExtension::class.java)

        fun rust(action: Action<in KrossoverRustExtension>) = action.execute(rust)
    }

open class KrossoverPythonExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        val outputDir: Property<Path> = objects.property(Path::class.java)
    }

open class KrossoverRustExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        val outputDir: Property<Path> = objects.property(Path::class.java)
        val jniSysModule: Property<String> = objects.property(String::class.java)
        val returnTypeMappings: ListProperty<ReturnTypeMapping> = objects.listProperty(ReturnTypeMapping::class.java)
    }

data class ReturnTypeMapping(
    @get:Input
    val kotlinType: String,
    @get:Input
    val rustType: String,
    @get:Input
    val conversionFunction: String,
)
