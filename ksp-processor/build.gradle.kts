plugins {
    id("kotlin-conventions")
}

dependencies {
    implementation("nl.ochagavia.krossover:shared-internals:${project.version}")
    implementation("com.google.devtools.ksp:symbol-processing-api:1.5.30-1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom {
                name.set("Krossover KSP Processor")
                description.set("This package is not intended for direct use, but should be used through the krossover plugin.")
                url.set("https://github.com/aochagavia/krossover")
            }
        }
    }
}
