plugins {
    kotlin("jvm")
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks.kotlinSourcesJar)

            pom {
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
            url = uri(project.projectDir.parentFile.resolve("build/maven-bundle"))
        }

        // Useful for local development
        maven {
            name = "dev"
            url = uri(project.projectDir.parentFile.resolve("build/maven-dev"))
        }
    }
}

signing {
    this.setRequired({
        gradle.taskGraph.allTasks.any {
            it.name.contains("publishMavenJavaPublicationToBundleRepository")
        }
    })

    useInMemoryPgpKeys(
        project.findProperty("signingInMemoryKey") as String?,
        project.findProperty("signingInMemoryKeyPassword") as String?,
    )

    sign(publishing.publications["mavenJava"])
}

java {
    withJavadocJar()
}

// Configure artifact
group = "nl.ochagavia.krossover"
version = "1.0.2-SNAPSHOT"

// Configure java version
val javaVersion = "11"

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget
                .fromTarget(javaVersion),
        )
    }
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version.toString(),
        )
    }
}
