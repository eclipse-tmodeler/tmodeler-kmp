# TModeler Kotlin Multiplatform

TModeler is a **data engine-oriented framework** for building data-centric applications. It lets you model, store, query, observe and (optionally) synchronize and secure your business data from a single Kotlin model definition, on **Android**, **JVM**, **Web (Kotlin/Wasm)** and, progressively, **iOS** — and from **Flutter** and **React** front-ends through a thin bridge.

This repository is the **official Kotlin Multiplatform distribution** of TModeler. A sibling distribution exists for C++: [`tmodeler-cpp`](https://github.com/eclipse-tmodeler/tmodeler-cpp).

> The UI layer never owns business logic or SQL: models, rules and persistence live in TModeler; Android (Compose/Views), Flutter and React are only views.

---

## Highlights

- **Typed models** — declare fields (`TString`, `TInt`, `TDate`, lists, relations, files…) with plain Kotlin delegates.
- **Full CRUD** — `save()`, `delete()`, `Tms` for centralized access; offline-first by design.
- **Relations** — one-to-one, one-to-many, many-to-many, with the same object appearing several times in a list.
- **Queries** — filters, ordering, pagination, joins, aggregates, and reactive `TFlow` lists.
- **Threading contract** — `TGate` guarantees data operations run in the right context.
- **Files & media** — managed storage attached to models.
- **Features & Tnc** — one server call = one `TFeature`; instant notification channels.
- **Pluggable engines** — `TSyncEngine`, `TSecurityEngine`, `TAiEngine`: TModeler works on its own, extensions are optional.
- **Reflection-free on the Web** — a KSP plugin (`tmodeler-ksp`) generates the model registry.

## Supported platforms

| Platform | Targets | Status |
|---|---|---|
| Android native (Kotlin / Java) | Android API 21+ | ✅ stable |
| JVM (desktop / server) | JDK 11+ (17 / 21 recommended) | ✅ stable |
| Web (React, Kotlin/Wasm-JS) | `wasmJs` | ✅ working |
| Flutter | via Android bridge | ✅ working |
| iOS | `iosArm64`, `iosSimulatorArm64`, `iosX64` | 🚧 in progress |

## Documentation

> **Using an AI coding assistant?** Point it to [`AGENTS.md`](AGENTS.md) (also published as [`llms.txt`](llms.txt)): it contains everything needed to install TModeler and start implementing, step by step.

Start here — the guides are self-sufficient (no need to read the source code).

| Guide | Content |
|---|---|
| [`docs/01-tmodeler.md`](docs/01-tmodeler.md) | **Getting started**: philosophy, architecture, prerequisites & IDE, installation, models, fields, relations, CRUD, queries, `TFlow`, `TGate`, files, features, notifications, engines, server side, offline-first, limits, troubleshooting, quick reference |
| [`docs/02-android.md`](docs/02-android.md) | Android native (Kotlin / Java): Gradle, Manifest, MVVM |
| [`docs/03-flutter.md`](docs/03-flutter.md) | Flutter ↔ Kotlin bridge |
| [`docs/04-react.md`](docs/04-react.md) | React + TModeler (Kotlin/Wasm) |
| [`docs/05-jvm.md`](docs/05-jvm.md) | JVM (desktop / server) |
| [`docs/06-ios.md`](docs/06-ios.md) | iOS (in progress) |

Read `01` first, then the guide of your platform.

## Quick install

TModeler is published on **Maven Central** (group `co.tamboai`) for Android, JVM and Web (Kotlin/Wasm). Requirements: JDK 17 or 21 and **Android Studio** or **IntelliJ IDEA** (see §4 of the getting-started guide). Node.js is needed for web only.

In your project:

```kotlin
repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("co.tamboai:TModeler:1.0.1")   // Gradle picks the Android, JVM or Wasm variant
}
```

| Platform | Artifact |
|---|---|
| Android | `co.tamboai:TModeler-android:1.0.1` (resolved from `TModeler`) |
| JVM | `co.tamboai:TModeler-jvm:1.0.1` (resolved from `TModeler`) |
| Web (Kotlin/Wasm) | `co.tamboai:TModeler-wasm-js:1.0.1` + KSP plugin `co.tamboai:tmodeler-ksp:1.0.1` |

Building from source (contributors only):

```bash
git clone https://github.com/eclipse-tmodeler/tmodeler-kmp.git
cd tmodeler-kmp
./gradlew :shared:publishToMavenLocal :tmodeler-ksp:publishMavenPublicationToMavenLocal   # then add mavenLocal()
```

Full details (repositories, per-platform dependencies, first start) are in [`docs/01-tmodeler.md`](docs/01-tmodeler.md#5-installation).

## Repository layout

```
tmodeler-kmp/
├── shared/          TModeler library (commonMain, androidMain, jvmMain, wasmJsMain, iosMain)
│   └── src/jvmTest/ Getting-started test suite (see below)
├── tmodeler-ksp/    KSP processor generating the model registry (Web)
├── reflectron-test/ Web registry generation tests
├── docs/            Documentation (getting started + one guide per platform)
├── tests/           Sample / test apps, one folder per platform
├── LICENSE          Eclipse Public License 2.0
├── NOTICE
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
└── SECURITY.md
```

## Tests and samples

- **Core test suite** — `shared/src/jvmTest`: ~90 tests on TModeler alone (no extension), organized by learning step: `gettingstarted`, `fields`, `relations`, `queries`, `flow`, `engines`, `threads`. They double as runnable examples.

  ```bash
  ./gradlew :shared:jvmTest
  ```

- **Platform apps** — [`tests/`](tests/) holds one sample app per platform (the *TmChat* reference chat):

  | Folder | Platform |
  |---|---|
  | [`tests/android-native`](tests/android-native) | Android native |
  | [`tests/flutter`](tests/flutter) | Flutter |
  | [`tests/react`](tests/react) | React (web, Kotlin/Wasm) |
  | [`tests/jvm`](tests/jvm) | JVM |
  | [`tests/ios`](tests/ios) | iOS |

## Optional extensions

TModeler is complete on its own. Synchronization, encryption and AI are optional modules (TBridge, TVault, TAi), plugged in through engines (`TSyncEngine`, `TSecurityEngine`, `TAiEngine`) and available on request at [contact@tamboai.co](mailto:contact@tamboai.co) or [tamboai.co](https://tamboai.co); see §17 of the getting-started guide.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md). To report a vulnerability, see [SECURITY.md](SECURITY.md).

## Contact

TAMBO ai — [tamboai.co](https://tamboai.co) — [contact@tamboai.co](mailto:contact@tamboai.co)

## License

Eclipse Public License 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
