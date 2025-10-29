plugins {
    id("kotlin-conventions")
    kotlin("plugin.serialization") version "2.2.20"
    `java-gradle-plugin`
}

dependencies {
    implementation("nl.ochagavia.krossover:shared-internals:${rootProject.version}")

    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.2.20-2.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")

    implementation("gg.jte:jte:3.2.1")

    testImplementation(kotlin("test"))
}

val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val functionalTest by tasks.registering(Test::class) {
    description = "Runs functional (end-to-end) tests with Gradle TestKit."
    group = "verification"
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath

    systemProperty(
        "e2e.baseDir",
        layout.buildDirectory
            .dir("tmp/functionalTest")
            .get()
            .asFile.absolutePath,
    )

    systemProperty(
        "projectRoot",
        layout.projectDirectory.asFile.absolutePath,
    )
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

// Store the package's version in resources is redundant, because we are also storing it in the
// jar's metadata. However, the resources approach is more robust (i.e. it also works on functional
// tests).
tasks.register("writeVersion") {
    val outDir = layout.buildDirectory.dir("generated/resources/version")
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().asFile.resolve("version.properties")
        file.parentFile.mkdirs()
        file.writeText("version=${project.version}\n")
    }
}

sourceSets.main {
    resources.srcDir(tasks.named("writeVersion"))
}

gradlePlugin {
    website = "https://github.com/aochagavia/krossover"
    vcsUrl = "https://github.com/aochagavia/krossover"

    plugins {
        register("greetingsPlugin") {
            id = "nl.ochagavia.krossover"
            displayName = "Krossover"
            description = "Generate idiomatic bindings for your Kotlin library in any programming language"
            tags = listOf("kotlin", "ffi", "codegen")
            implementationClass = "nl.ochagavia.krossover.gradle.KrossoverPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifact(tasks.kotlinSourcesJar)
            artifactId = "nl.ochagavia.krossover.gradle.plugin"

            pom {
                name.set("Krossover Gradle Plugin")
                description.set("Generate idiomatic bindings for your Kotlin library in any programming language")
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
