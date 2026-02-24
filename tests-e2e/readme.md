# E2E tests

This directory contains a standalone Kotlin library, krossover-generated bindings
for it, and tests that exercise those bindings (to ensure the code works as expected
when it actually runs).

For everything to work, you will need to run `./gradlew publishDev` first in the root project (so
krossover gets published to a local repository).

Important: the commands below assume you have the necessary development tools. If that is not the
case, you can either install them by hand or run the commands using `pixi` (which will automatically
provision the development tools).

### Running all tests

Prefer running `./gradlew checkE2e` (or `pixi run ./gradlew checkE2e`) at the root
directory, to ensure the last version of the plugin has been published.

### Running tests for a specific language

Ensure everything is ready by running `./gradlew buildAndGenerateBindings` in this project's
directory. After that, you can run tests for specific languages:

- `uv run --project mylib-python pytest`
- `cargo test --manifest-path mylib-rust/mylib/Cargo.toml`
