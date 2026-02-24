import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import java.nio.file.Files
import kotlin.io.path.Path

plugins {
    kotlin("jvm") version "2.2.20"
    id("org.graalvm.buildtools.native") version "0.11.1"
    id("nl.ochagavia.krossover") version "1.0.7-SNAPSHOT"
}

repositories {
    mavenCentral()
    maven {
        name = "dev"
        url = uri("../build/maven-dev")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

krossover {
    // The name of the library artifact (e.g. `mylib` maps to `libmylib.so
    // on Linux).
    libName = "mylib"

    // The root classes that act as entrypoints for the public API. Krossover
    // will expose them and all classes that are publicly referenced by them.
    rootClasses = listOf("com.example.mylib.Math", "com.example.mylib.NonZeroInt")

    // Classes will only be exposed if they are defined inside the specified
    // packages.
    exposedPackages = listOf("com.example")

    // A platform-specific JNI header file is used by the wrapper libraries.
    // The right header file for the current platform will be automatically
    // copied to the provided path.
    jniHeaderOutputFile = project.projectDir
        .resolve("build/jni/jni_simplified.h")
        .toPath()

    python {
        // Python-related files are generated under the provided directory.
        outputDir = Path("${project.projectDir}/mylib-python/src/mylib")
    }

    rust {
        // Rust-related files are generated under the provided directory.
        outputDir = Path("${project.projectDir}/mylib-rust/mylib/src/generated")

        // The generated Rust source code will attempt to use JNI symbols from
        // the `mylib_sys` Rust module.
        jniSysModule = "mylib_sys"
    }
}

graalvmNative {
    binaries {
        named("main") {
            sharedLibrary.set(true)

            // Pass configuration file specifying exports
            val jniConfig = project.projectDir.resolve("build/kotlin/krossover/metadata/jni-config.json")
            buildArgs.add("-H:+UnlockExperimentalVMOptions")
            buildArgs.add("-H:JNIConfigurationFiles=${jniConfig}")

            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(25))
                vendor.set(JvmVendorSpec.GRAAL_VM)
            })
        }
    }
}

tasks.named("nativeCompile") {
    dependsOn("generateJniConfig")
}

// Fix to get `nativeCompile` to work, taken from https://github.com/oss-review-toolkit/ort/blob/70a60e8724785aabf51528e8234c95d630136e4b/buildSrc/src/main/kotlin/ort-application-conventions.gradle.kts#L112-L152
tasks.named<BuildNativeImageTask>("nativeCompile") {
    // Gradle's "Copy" task cannot handle symbolic links, see https://github.com/gradle/gradle/issues/3982. That is why
    // links contained in the GraalVM distribution archive get broken during provisioning and are replaced by empty
    // files. Address this by recreating the links in the toolchain directory.
    val toolchainDir = options.get().javaLauncher.get().executablePath.asFile.parentFile.run {
        if (name == "bin") parentFile else this
    }

    val toolchainFiles = toolchainDir.walkTopDown().filter { it.isFile }
    val emptyFiles = toolchainFiles.filter { it.length() == 0L }

    // Find empty toolchain files that are named like other toolchain files and assume these should have been links.
    val links = toolchainFiles.mapNotNull { file ->
        emptyFiles.singleOrNull { it != file && it.name == file.name }?.let {
            file to it
        }
    }

    // Fix up symbolic links.
    links.forEach { (target, link) ->
        logger.quiet("Fixing up '$link' to link to '$target'.")

        if (link.delete()) {
            Files.createSymbolicLink(link.toPath(), target.toPath())
        } else {
            logger.warn("Unable to delete '$link'.")
        }
    }
}

val buildAndGenerateBindings by tasks.registering {
    val nativeCompileTask = tasks.named<BuildNativeImageTask>("nativeCompile")
    dependsOn("generateJniBindings", nativeCompileTask)

    // Copy the shared library and header file
    val os = org.gradle.internal.os.OperatingSystem.current()
    val sharedLibraryTargetFile = when {
        os.isWindows -> "mylib.dll"
        os.isLinux -> "libmylib.so"
        os.isMacOsX -> "libmylib.dylib"
        else -> throw Exception("Unsupported OS")
    }
    val nativeCompile = nativeCompileTask.get()
    val sharedLibraryFileExtension = sharedLibraryTargetFile.split(".").last()
    val outputFile = nativeCompile.outputDirectory.get().asFile.resolve("mylib.${sharedLibraryFileExtension}")

    doLast {
        copy {
            from(outputFile)
            into("mylib-python/src/mylib")
            rename(".*", sharedLibraryTargetFile)
        }
        copy {
            from(outputFile)
            into("mylib-rust/artifacts")
            rename(".*", sharedLibraryTargetFile)
        }

        // Copy the header file
        copy {
            from("build/jni/jni_simplified.h")
            into("mylib-python/src/mylib")
        }
        copy {
            from("build/jni/jni_simplified.h")
            into("mylib-rust/artifacts")
        }
    }

    group = "build"
}

val testPython by tasks.registering(Exec::class) {
    dependsOn(buildAndGenerateBindings)

    group = "verification"
    commandLine = "uv run --project mylib-python pytest".split(" ")
    standardOutput = System.out
    errorOutput = System.err
    isIgnoreExitValue = false
}

val testRust by tasks.registering(Exec::class) {
    dependsOn(buildAndGenerateBindings)

    group = "verification"
    commandLine = "cargo test --manifest-path mylib-rust/mylib/Cargo.toml".split(" ")
    standardOutput = System.out
    errorOutput = System.err
    isIgnoreExitValue = false
}

tasks.named("check") {
    dependsOn(testPython, testRust)
}
