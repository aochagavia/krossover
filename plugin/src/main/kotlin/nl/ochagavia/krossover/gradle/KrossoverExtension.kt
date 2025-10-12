package nl.ochagavia.krossover.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.nio.file.Path
import javax.inject.Inject

open class KrossoverExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        val rootClasses: ListProperty<String> = objects.listProperty(String::class.java)
        val packages: ListProperty<String> = objects.listProperty(String::class.java)
        val additionalJniClasses: ListProperty<String> = objects.listProperty(String::class.java)
        val outputPackageName: Property<String> = objects.property(String::class.java).convention("public-api")
        val fileName: Property<String> = objects.property(String::class.java).convention("api.json")
        val pythonOutputFile: Property<Path> = objects.property(Path::class.java)
    }
