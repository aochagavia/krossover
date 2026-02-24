import java.util.*

val sharedProps = Properties().apply {
    project.file("jdk.properties").inputStream().use { load(it) }
}

repositories {
    mavenCentral()
}

// The plugin project and its dependencies
val pluginAndDeps = listOf("shared-internals", "ksp-processor", "plugin")

// Useful for local development and testing
val publishDev by tasks.registering {
    pluginAndDeps.forEach {
        dependsOn(gradle.includedBuild(it).task(":publishMavenJavaPublicationToDevRepository"))
    }
}

tasks.register<Zip>("bundleZipForMavenCentral") {
    group = "publishing"
    description = "Zips the locally published Maven repository for manual upload."

    pluginAndDeps.forEach {
        dependsOn(gradle.includedBuild(it).task(":publishMavenJavaPublicationToBundleRepository"))
    }

    // Zip the contents of the generated Maven repo layout
    from(layout.buildDirectory.dir("maven-bundle"))

    archiveFileName.set("maven-central-bundle-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

tasks.register("unitTest") {
    group = "verification"

    pluginAndDeps.forEach {
        dependsOn(gradle.includedBuild(it).task(":test"))
    }
}

tasks.register("functionalTest") {
    group = "verification"

    // Make sure the everything has been published
    dependsOn(publishDev)
    dependsOn(gradle.includedBuild("plugin").task(":functionalTest"))
}

val checkE2e by tasks.registering(Exec::class) {
    dependsOn(publishDev)

    group = "verification"
    commandLine = "./gradlew check".split(" ")
    workingDir = projectDir.resolve("tests-e2e")
    standardOutput = System.out
    errorOutput = System.err
    isIgnoreExitValue = false
}

tasks.register("check") {
    group = "verification"

    dependsOn("functionalTest", checkE2e)

    pluginAndDeps.forEach {
        dependsOn(gradle.includedBuild(it).task(":check"))
    }
}
