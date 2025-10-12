rootProject.name = "krossover"

val group = "nl.ochagavia.krossover"

includeBuild("build-plugin")

includeBuild("shared") {
    dependencySubstitution {
        substitute(module("$group:shared")).using(project(":"))
    }
}

includeBuild("ksp-processor") {
    dependencySubstitution {
        substitute(module("$group:ksp-processor")).using(project(":"))
    }
}

includeBuild("plugin") {
    dependencySubstitution {
        substitute(module("$group:plugin")).using(project(":"))
    }
}
