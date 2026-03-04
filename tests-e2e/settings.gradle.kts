rootProject.name = "mylib"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "dev"
            url = uri("../build/maven-dev")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("core")
