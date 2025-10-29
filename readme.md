# Krossover

> Generate idiomatic bindings for your Kotlin library in any programming language

Currently supported languages:

- Python
- Rust

## Project status

This project is developed on a "scratch my own itch basis". It's main purpose as of today is to
generate idiomatic bindings for the [KSON project](https://github.com/kson-org/kson/). This means
that Krossover is mature enough to be used in a real-world codebase, but also that it is currently
rough around the edges and lacking proper documentation (see [usage](#usage) for some guidance).

You are welcome to discuss potential features in the issue tracker (e.g. adding support for a
language you are interested in) and to ask questions in the KSON Zulip (check out the
[#krossover](https://kson-org.zulipchat.com/#narrow/channel/540263-krossover) channel). Note that I
intend to prioritize features that are useful for KSON, but I'm nevertheless open to reviewing and
merging pull requests that improve Krossover for usage in other projects (let's discuss beforehand,
just to be sure). I'm also available for krossover-related [consulting](https://ochagavia.nl):
answering advanced questions, implementing new features, etc.

## Usage

#### Gradle setup

Add the plugin to your Gradle build:

```kotlin
plugins {
    id("nl.ochagavia.krossover") version "<desired-version>"
}
```

Configure the plugin:

```kotlin
krossover {
    // The name of the library artifact (e.g. `awesome` maps to `libawesome.so
    // on Linux).
    libName = "awesome"

    // The root classes that act as entrypoints for the public API. Krossover
    // will expose them and all classes that are publicly referenced by them.
    rootClasses = listOf("org.awesome.Awesome")

    // Classes will only be exposed if they are defined inside the specified
    // packages.
    exposedPackages = listOf("org.awesome")

    // A platform-specific JNI header file is used by the wrapper libraries.
    // The right header file for the current platform will be automatically
    // copied to the provided path.
    jniHeaderOutputFile = project.projectDir
        .resolve("build/jni/jni_simplified.h")
        .toPath()

    python {
        // Python-related files are generated under the provided directory.
        outputDir = Path("${rootProject.projectDir}/lib-python/src/awesome")
    }

    rust {
        // Rust-related files are generated under the provided directory.
        outputDir = Path("${rootProject.projectDir}/lib-rust/awesome/src/generated")

        // The generated Rust source code will attempt to use JNI symbols from
        // the `awesome_sys` Rust module.
        jniSysModule = "awesome_sys"
    }
}
```

#### Creating and using wrapper libraries

Krossover does not scaffold full libraries for each supported programming languages. Instead, you
are meant to manually create those libraries, so you have more control over the exposed API (e.g.
the resulting library could even provide functions that are not present in the original Kotlin
source). When properly configured, Krossover will leave your library code alone and merely generate
the source files that wrap your Kotlin library.

The generated code assumes your Kotlin library has been compiled to native and available as a shared
library. The only realistic way to achieve this, as far as I know, is through a tool called [GraalVM
Native Image](https://www.graalvm.org/jdk25/reference-manual/native-image/). To facilitate this,
Krossover generates a `jni-config.json` file that should be fed into GraalVM (that way, GraalVM will
know which classes should be included in the native binary).

Manually scaffolding the wrapper libraries can be tricky. Compiling your Kotlin code using GraalVM
Native image is challenging too. Given the current lack of documentation, refer to the [KSON
repository](https://github.com/kson-org/kson/) for a real-world project that uses Krossover. Feel
free to [ask questions on Zulip](https://kson-org.zulipchat.com/#narrow/channel/540263-krossover) as
well.

## Project structure

Krossover has the following components, each living in its own subproject:

- `ksp-processor`: a compiler plugin that gathers metadata about the public API of your library.
- `plugin`: based on the gathered metadata, generates wrapper code in the supported programming
  languages. Provides a Gradle plugin so you can easily use Krossover from a Gradle project.
- `shared-internals`: data types used both in `ksp-processor` and `plugin`.

## Ideas for the future

- Provide more information on how to use GralVM to compile Kotlin projects.
- Support third-party language providers. Currently, the only way to add support for a new language
  in Krossover is to implement it in this repository. It should be possible to design a system that
  enables independent implementations of target languages.
- Automatically support all JVM-based languages by obtaining metadata from Java bytecode (e.g. a JAR
  file) instead of from the Kotlin compiler. Important note: the Kotlin compiler provides detailed
  metadata about a library's class hierarchy, which allows Krossover to produce highly idiomatic
  wrapper code. We would need to verify that all necessary metadata is present in JAR files before
  going down this road.
- Provide a CLI application next to (or instead of) the Gradle plugin, for easier integration with
  non-Gradle projects.