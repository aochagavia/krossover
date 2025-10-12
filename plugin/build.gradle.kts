plugins {
    id("kotlin-conventions")
    kotlin("plugin.serialization") version "2.2.20"
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("krossover") {
            id = "nl.ochagavia.krossover"
            implementationClass = "nl.ochagavia.krossover.gradle.KrossoverPlugin"
        }
    }
}

dependencies {
    implementation("nl.ochagavia.krossover:shared:${rootProject.version}")

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
