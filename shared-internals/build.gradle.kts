plugins {
    id("kotlin-conventions")
    kotlin("plugin.serialization") version "2.2.20"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom {
                name.set("Krossover Shared Internals")
                description.set("Internal implementation details of Krossover. This package is not intended for direct use.")
                url.set("https://github.com/aochagavia/krossover")
            }
        }
    }
}
