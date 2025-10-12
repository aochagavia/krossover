plugins {
    `kotlin-dsl`
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "project"
            url = uri("../.mvn-repo")
        }
    }
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven {
        name = "project"
        url = uri("../.mvn-repo")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20")
}
