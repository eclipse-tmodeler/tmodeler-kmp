# Changelog

## 1.0.1 — fix Gradle module metadata

- Fixed published artifacts: the root module now references `TModeler-jvm`, `TModeler-android` and `TModeler-wasm-js`, which are all published. Version 1.0.0 referenced artifacts that were never uploaded and cannot be resolved by Gradle — **use 1.0.1**.
- `TModeler-jvm` is now published.
- iOS targets are disabled by default (`-Ptmodeler.ios=true` to enable on macOS).

## 1.0.0 — first public Kotlin Multiplatform release (broken metadata, superseded)

- Typed models, fields, relations, CRUD, queries, `TFlow`, `TGate`.
- Targets: Android, JVM, Web (Kotlin/Wasm) — iOS in progress.
- `tmodeler-ksp` model registry generator for the Web.
- Engines: `TSyncEngine`, `TSecurityEngine`, `TAiEngine` (optional).
- Documentation and getting-started test suite (`shared/src/jvmTest`).
