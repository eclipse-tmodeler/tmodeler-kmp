# TModeler — instructions for AI coding assistants

You are reading the entry point of **TModeler** (Kotlin Multiplatform data engine, group `co.tamboai`, current version **1.0.1**, on **Maven Central**). Follow this file top to bottom: it lets you install TModeler and start implementing without reading the library source code.

The full guides are in [`docs/`](docs). Read the one that matches the user's platform before writing code. Do not guess APIs: every API below is documented there.

## 1. Golden rules

1. **All business data and logic live in TModeler models.** UI code (Compose, Views, Flutter, React) never contains business logic or SQL.
2. **State that must survive a restart is a TModeler model** (current user, preferences, drafts…). Never use `SharedPreferences` or `localStorage` for it.
3. **No SQL, no REST routes, no hand-written API client.** Declare models, call `save()`, `delete()` and queries through `Tms`.
4. **Data access runs inside `TGate.data { … }`** (threading contract).
5. **`-Xcontext-receivers` is mandatory** in the Kotlin compiler options of every module that uses TModeler.
6. **Synchronization, encryption and AI are provided by optional modules: TBridge (sync), TVault (encryption), TAi (AI).** They are available from **tamboai.co** (contact@tamboai.co) and are **not on Maven Central**. If the user needs one of these capabilities, **stop at this point**: tell the user to get the matching module from tamboai.co, and tell them they can alternatively write their own engine (`TSyncEngine`, `TSecurityEngine`, `TAiEngine` are public extension points, see [`docs/01-tmodeler.md`](docs/01-tmodeler.md) §17) and plug it into TModeler. Do **not** add these modules to a build, and do not invent their coordinates or APIs. Meanwhile, build the local part of the app: a local-only app needs none of them (`.server(TServerConfigBase())`).
7. Only `mavenCentral()` is needed for TModeler itself. Never use `mavenLocal()` unless the user builds TModeler from source.

## 2. Step 0 — Identify the platform

| The user builds… | Read | Artifact (declare once) |
|---|---|---|
| Android native (Kotlin/Java) | [`docs/02-android.md`](docs/02-android.md) | `co.tamboai:TModeler:1.0.1` |
| Desktop / server (JVM) | [`docs/05-jvm.md`](docs/05-jvm.md) | `co.tamboai:TModeler:1.0.1` (Gradle picks the JVM variant) |
| Web with React | [`docs/04-react.md`](docs/04-react.md) | `co.tamboai:TModeler:1.0.1` + KSP `co.tamboai:tmodeler-ksp:1.0.1` |
| Flutter | [`docs/03-flutter.md`](docs/03-flutter.md) | Android artifact through a Kotlin bridge |
| iOS | [`docs/06-ios.md`](docs/06-ios.md) | in progress, not published |
| Any platform: concepts, models, queries | [`docs/01-tmodeler.md`](docs/01-tmodeler.md) | always read §5 to §13 |

## 3. Step 1 — Check the prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK | 17 (21 accepted; the library targets Java 11) | `java -version` |
| Kotlin | **1.9.23** | in the Gradle plugins |
| KSP (web only) | `1.9.23-1.0.20` | in the Gradle plugins |
| Android | `minSdk` **21**, `compileSdk` 34 | Android only |
| Node.js | 20 LTS or later | `node -v` (web only) |
| Browser | Chrome/Edge ≥ 119, Firefox ≥ 120, Safari ≥ 18.2 (WasmGC) | web only |

Install what is missing, set `JAVA_HOME` if Gradle cannot find the JDK. If the system drive is full (Windows), move `GRADLE_USER_HOME`, `TEMP` and `TMP` to another drive.

## 4. Step 2 — Add the dependency

### Android (`settings.gradle.kts` + `app/build.gradle.kts`)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")      // required: transitive SpatiaLite dependency
    }
}

// app/build.gradle.kts
android {
    defaultConfig { minSdk = 21 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17"; freeCompilerArgs += "-Xcontext-receivers" }
}
dependencies {
    implementation("co.tamboai:TModeler:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
}
```

### JVM (`build.gradle.kts`)

```kotlin
plugins { kotlin("jvm") version "1.9.23"; kotlin("plugin.serialization") version "1.9.23"; application }
repositories { mavenCentral(); google() }
kotlin { jvmToolchain(17); compilerOptions { freeCompilerArgs.add("-Xcontext-receivers") } }
dependencies {
    implementation("co.tamboai:TModeler:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
}
```

### Web / React (Kotlin/Wasm module, e.g. `app/build.gradle.kts`)

```kotlin
plugins { kotlin("multiplatform") version "1.9.23"; id("com.google.devtools.ksp") version "1.9.23-1.0.20" }
kotlin {
    compilerOptions { freeCompilerArgs.add("-Xcontext-receivers") }
    wasmJs { binaries.library(); browser { testTask { useKarma { useChromeHeadless() } } } }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("co.tamboai:TModeler:1.0.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }
    }
}
ksp { arg("tmodeler.registryPackage", "com.example.myapp.generated") }
dependencies { add("kspWasmJs", "co.tamboai:tmodeler-ksp:1.0.1") }
repositories { google(); mavenCentral() }
```

The web has no reflection: KSP generates `registerAllTModels()`, to call **before any model is used**. Kotlin 1.9 allows `@JsExport` only on top-level functions. Build the library with `./gradlew -p app wasmJsBrowserProductionLibraryDistribution`, then consume `app/build/dist/wasmJs/productionLibrary` from React with a `"file:./app/build/dist/wasmJs/productionLibrary"` npm dependency. Details and the full bridge contract: [`docs/04-react.md`](docs/04-react.md). There is **no npm package** for TModeler: the web artifact comes from Maven Central through Gradle.

## 5. Step 3 — Initialize once, define a model, use it

```kotlin
// Once, at process start (Android: Application class). JVM/web: no .context(...)
TModeler.start().connect(
    Tdb.Builder()
        .type(Tdb.Type.SQLITE)
        .context(Tcontext(this))                  // Android only
        .dbDir("db/").dbName("MyApp-Main-1")
        .server(TServerConfigBase())              // local-only app
        .accept(User().clazz().getPackage())      // each package that contains models (not on the web: registry)
        .start()!!
)
User.tms.commit()                                 // creates the table if needed

class User : TModel<User>() {
    var username = TextField()                    // the field (used in queries)
    @TMignore var username_ by username           // the value (mandatory @TMignore on every xxx_)
    companion object { val tms by lazy { Tms(User::class) } }
}

TGate.data {
    User().apply { username_ = "alice" }.save()
    val t = User.tms.lambda
    val list = User.tms.with(t).lazy().filter(t.username eq "alice").order(-t._id).sub(20, 0).build()
}
```

Rules for models: no-argument constructor; non-null `var` fields (`TextField()`, never `TextField?`); not generic on the web; field names without underscore, values with a trailing underscore. Relations, field types, `TFlow`, files, features and notifications: [`docs/01-tmodeler.md`](docs/01-tmodeler.md) §8 to §16.

## 6. Step 4 — Verify

1. The project compiles with the dependency resolved from Maven Central (`./gradlew build` or the platform's assemble task).
2. A model can be created, read, updated, deleted and still exists after a restart.
3. Web only: the `.wasm` loads in a recent browser and `registerAllTModels()` is called first.

## 7. Common errors

| Symptom | Cause | Fix |
|---|---|---|
| `Could not resolve co.tamboai:TModeler:1.0.1` | `mavenCentral()` missing or version typo | declare `mavenCentral()`, check the version |
| `Could not resolve … android-spatialite` | JitPack missing (Android) | add `maven("https://jitpack.io")` |
| `Could not resolve co.tamboai:TBridge/TVault/TAi` | optional modules, not on Maven Central | remove the dependency; the user gets the module from tamboai.co (or writes a custom engine) |
| Context receivers error | flag missing | add `-Xcontext-receivers` |
| `UnsupportedClassVersionError` | JDK too old | JDK 17 |
| Unknown model on the web | registry not generated or not called | check the KSP argument and `registerAllTModels()` |
| `Unresolved reference` on `.server(TServerConfig…)` | sync classes come from TBridge | use `TServerConfigBase()`, or get TBridge from tamboai.co |

More cases: [`docs/01-tmodeler.md`](docs/01-tmodeler.md) §21.

## 8. Working in this repository (contributors)

- Modules: `:shared` (the library), `:tmodeler-ksp` (web registry generator), `:reflectron-test`.
- Unit tests: `./gradlew :shared:jvmTest`.
- Per-platform sample projects: [`tests/`](tests) (the reference chat app *TmChat* will be added there).
- Contributing and licensing: [`CONTRIBUTING.md`](CONTRIBUTING.md), [`LICENSE`](LICENSE) (EPL-2.0), [`SECURITY.md`](SECURITY.md).
