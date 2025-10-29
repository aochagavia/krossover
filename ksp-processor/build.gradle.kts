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
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifact(tasks.kotlinSourcesJar)

            pom {
                name.set("Krossover KSP Processor")
                description.set("This package is not intended for direct use, but should be used through the krossover plugin.")
                url.set("https://github.com/aochagavia/krossover")

                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("aochagavia")
                        name.set("Adolfo Ochagavía")
                        email.set("maven-central@adolfo.ochagavia.nl")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/aochagavia/krossover.git")
                    developerConnection.set("scm:git:git@github.com:aochagavia/krossover.git")
                    url.set("https://github.com/aochagavia/krossover")
                }
            }
        }
    }

    repositories {
        // Write publications into a local folder that we'll zip and upload
        // (the automation story between Gradle and Maven Central is... painful)
        maven {
            name = "bundle"
            url = uri(layout.buildDirectory.dir("maven-bundle"))
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

tasks.register<Zip>("bundleZip") {
    group = "publishing"
    description = "Zips the locally published Maven repository for manual upload."

    dependsOn("publishMavenJavaPublicationToBundleRepository")

    // Zip the contents of the generated Maven repo layout
    from(layout.buildDirectory.dir("maven-bundle"))

    archiveFileName.set("maven-central-bundle-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}
