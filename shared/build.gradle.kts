plugins {
    id("kotlin-conventions")
    kotlin("plugin.serialization") version "2.2.20"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
    testImplementation(kotlin("test"))
}
