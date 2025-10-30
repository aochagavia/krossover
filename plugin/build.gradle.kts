plugins {
    id("kotlin-conventions")
    kotlin("plugin.serialization") version "2.2.20"
    id("gg.jte.gradle") version "3.2.1"
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

jte {
    getContentType().set(gg.jte.ContentType.Plain)
    generate()
}

// Not sure why this is needed, but Gradle complains otherwise
tasks.named("kotlinSourcesJar") {
    dependsOn("generateJte")
}

tasks.withType<Javadoc>().configureEach {
    // Generated JTE template classes should not be scanned by javadoc
    exclude("gg.jte/**")
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
        named<MavenPublication>("mavenJava") {
            artifactId = "nl.ochagavia.krossover.gradle.plugin"

            pom {
                name.set("Krossover Gradle Plugin")
                description.set("Generate idiomatic bindings for your Kotlin library in any programming language")
                url.set("https://github.com/aochagavia/krossover")
            }
        }
    }
}
