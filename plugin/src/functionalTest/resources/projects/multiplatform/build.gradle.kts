plugins {
    kotlin("multiplatform") version "2.2.20"
    id("nl.ochagavia.krossover")
}

repositories {
    mavenCentral()
    maven {
        val projectRoot = gradle.startParameter.projectProperties["projectRoot"]!!
        url = uri(file(projectRoot).resolve("../.mvn-repo"))
    }
}

kotlin {
    jvm {
    }
}

krossover {
    rootClasses.set(listOf("com.example.Dummy", "com.example.Object"))
    packages.set(listOf("com.example"))
    outputPackageName.set("public-api")
    fileName.set("api.json")
}
