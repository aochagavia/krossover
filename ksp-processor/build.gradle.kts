plugins {
    id("kotlin-conventions")
}

dependencies {
    implementation("nl.ochagavia.krossover:shared-internals:${project.version}")
    implementation("com.google.devtools.ksp:symbol-processing-api:1.5.30-1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
}
