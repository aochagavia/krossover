use anyhow::Context;
use std::env;
use std::fs;
use std::path::{Path, PathBuf};

fn get_native_artifacts(
    out_dir: &Path,
) -> anyhow::Result<()> {
    for entry in fs::read_dir(&"../artifacts")? {
        let entry = entry?;
        let source_path = entry.path();
        if source_path.is_file() {
            let file_name = source_path.file_name().unwrap();
            let dest_path = out_dir.join(file_name);
            fs::copy(&source_path, &dest_path)?;
            println!("cargo:rerun-if-changed={}", source_path.display());
        }
    }

    Ok(())
}

fn main() -> anyhow::Result<()> {
    let out_dir = PathBuf::from(env::var("OUT_DIR").unwrap());

    get_native_artifacts(&out_dir).context("Failed to copy native artifacts")?;

    // Generate bindings
    let bindings = bindgen::Builder::default()
        .header(
            out_dir
                .join("jni_simplified.h")
                .display()
                .to_string(),
        )
        .generate()
        .context("Unable to generate bindings")?;

    bindings
        .write_to_file(out_dir.join("bindings.rs"))
        .context("Couldn't write bindings!")?;

    // Tell the compiler where to find the dynamic library
    println!("cargo:rustc-link-search=native={}", out_dir.display());
    println!("cargo:rustc-link-lib=dylib=mylib");

    #[cfg(not(target_os = "windows"))]
    println!("cargo:rustc-link-lib=dylib=z");

    Ok(())
}
