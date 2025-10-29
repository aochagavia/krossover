plugins {
    kotlin("jvm")
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

signing {
    useInMemoryPgpKeys(
        project.findProperty("signingInMemoryKey") as String,
        project.findProperty("signingInMemoryKeyPassword") as String
    )
}

java {
    withJavadocJar()
}

// Configure artifact
group = "nl.ochagavia.krossover"
version = "1.0.0"

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
