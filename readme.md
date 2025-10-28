# Krossover

> Generate idiomatic bindings for your Kotlin library in any programming language

Currently supported languages:

- Python
- Rust

> [!NOTE]
> This project is in its very early days. Expect rough edges and sparse documentation. PRs are welcome!

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

#### Creating and using the wrapper libraries

Krossover does not scaffold full libraries for each supported programming languages. Instead, you are meant to manually create libraries for each language you are interested in, which gives you full control over the exposed API (e.g. you could even create additional functions that are not present in the original Kotlin source). Krossover will merely generate the subset of the code that wraps the Kotlin library, and put the resulting source files wherever you tell it to.

The generated code assumes your Kotlin library is available as a native shared library. The only
realistic way to achieve this, as far as we know, is through a tool called GraalVM Native Image. To
facilitate this, Krossover generates a `jni-config.json` file that should be fed into GraalVM (that way, GraalVM will know which classes should be included in the native binary).

Manually scaffolding the wrapper libraries can be tricky. Compiling your Kotlin code using GraalVM
Native image is challenging as well. Given the current lack of documentation, refer to the [KSON
repository](https://github.com/kson-org/kson/) for a real-world project that uses Krossover.

## Inner workings

Krossover has mainly two components, each with its own subproject:

- `ksp-processor`: a compiler plugin that gathers metadata about the public API of your library.
- `plugin`: based on the gathered metadata, generates wrapper code in the supported programming
  languages. Provides a Gradle plugin so you can easily use Krossover from a Gradle project.

Internally, Kotlin functions are exposed through the JNI. This is an implementation detail,
invisible to the user.

## Ideas for the future

- Support statically linked libraries, instead of only shared libraries (this does not apply to
  Python, which by definition uses dynamic linking, but is relevant for compiled languages such as
  Rust).
- Support third-party language providers. Currently, the only way to add support for a new language
  in Krossover is to implement it in this repository. It would be interesting to allow independent
  implementations that build on top of Krossover to support additional languages.