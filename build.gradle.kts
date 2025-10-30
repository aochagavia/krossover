import java.util.*

plugins {
    id("me.filippov.gradle.jvm.wrapper") version "0.14.0"
}

val sharedProps = Properties().apply {
    project.file("jdk.properties").inputStream().use { load(it) }
}

// NOTE: `./gradlew wrapper` must be run for edit to this config to take effect
jvmWrapper {
    unixJvmInstallDir = sharedProps.getProperty("unixJvmInstallDir")
    winJvmInstallDir = sharedProps.getProperty("winJvmInstallDir")
    macAarch64JvmUrl = sharedProps.getProperty("macAarch64JvmUrl")
    macX64JvmUrl = sharedProps.getProperty("macX64JvmUrl")
    linuxAarch64JvmUrl = sharedProps.getProperty("linuxAarch64JvmUrl")
    linuxX64JvmUrl = sharedProps.getProperty("linuxX64JvmUrl")
    windowsX64JvmUrl = sharedProps.getProperty("windowsX64JvmUrl")
}

repositories {
    mavenCentral()
}

tasks.register("cleanMvnRepo") {
    delete(".mvn-repo")
}

// The plugin project and its dependencies
val pluginAndDeps = listOf("shared-internals", "ksp-processor", "plugin")

// Useful for local development and testing
tasks.register("publishDev") {
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

    // Make sure the plugin has been published
    mustRunAfter("publishLocal")
    dependsOn("publishLocal")
    dependsOn(gradle.includedBuild("plugin").task(":functionalTest"))
}
