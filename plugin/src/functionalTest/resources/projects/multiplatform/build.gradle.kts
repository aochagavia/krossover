plugins {
    kotlin("multiplatform") version "2.2.20"
    id("nl.ochagavia.krossover")
}

repositories {
    mavenCentral()
    maven {
        val projectRoot = gradle.startParameter.projectProperties["projectRoot"]!!
        url = uri(file(projectRoot).resolve("../build/maven-dev"))
    }
}

kotlin {
    jvm {
    }
}

krossover {
    libName = "example"
    rootClasses = listOf("com.example.Dummy", "com.example.Object")
    exposedPackages = listOf("com.example")

    jniHeaderOutputFile = project.projectDir
        .resolve("build/jni/jni_simplified.h")
        .toPath()

    python {
        outputDir = project.projectDir.resolve("build/python").toPath()
    }

    rust {
        outputDir = project.projectDir.resolve("build/rust").toPath()
        jniSysModule = "example_sys"
    }
}
