# TModeler on the JVM (desktop, server, tests)

> This document complements the [general guide](01-tmodeler.md). It does not repeat the shared concepts (models,
> queries, `TFlow`…): it describes **what is specific to the JVM** (console application, desktop, service,
> automated tests).
>
> **Status**: the JVM target is functional (SQLite database through JDBC, library tests run on the JVM).
> The examples below are deliberately minimal.

## Table of contents

1. [When to use the JVM](#1-when-to-use-the-jvm)
2. [Environment](#2-environment)
3. [Creating the project](#3-creating-the-project)
4. [Initialization](#4-initialization)
5. [JVM specifics](#5-jvm-specifics)
6. [Tests](#6-tests)
7. [JVM troubleshooting](#7-jvm-troubleshooting)

---

## 1. When to use the JVM

| Use case | Example |
|---|---|
| **Automated tests** of your models and features, without an emulator | `./gradlew test` |
| Command-line tool, data import/export script | migration, reports |
| **Desktop** application (Compose Desktop, Swing, JavaFX) | administration tool |
| **Shared** Kotlin Multiplatform module (`jvm` target) alongside Android | common code tested on the JVM |

For a mobile app, see the [Android guide](02-android.md); for a data server, see the
[general guide §18](01-tmodeler.md#18-server-side-python--django) (the reference server is written in Python/Django).

---

## 2. Environment

| Tool | Version | Note |
|---|---|---|
| **IntelliJ IDEA** (Community or Ultimate) | recent | Recommended IDE for the JVM, Kotlin plugin included |
| JDK | **11 minimum** (17 or 21 recommended) | the library targets Java 11 |
| Gradle | project wrapper | |
| Kotlin | 1.9.x or 2.1.x | `-Xcontext-receivers` is required |

---

## 3. Creating the project

*New Project ▸ Kotlin ▸ Gradle (Kotlin DSL)*, then `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    application
}

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(17)
    compilerOptions { freeCompilerArgs.add("-Xcontext-receivers") }     // REQUIRED
}

dependencies {
    implementation("co.tamboai:TModeler:1.0.1")        // Gradle automatically picks the JVM variant
    // Optional: engines (general guide §17)
    // Optional modules, on request (contact@tamboai.co):
    // implementation("co.tamboai:TBridge:1.0.1")      // server synchronization + Tnc
    // implementation("co.tamboai:TVault:1.0.1")       // encryption
    // implementation("co.tamboai:TAi:1.0.1")          // artificial intelligence

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    testImplementation(kotlin("test"))
}

application { mainClass.set("MainKt") }
tasks.test { useJUnitPlatform() }
```

In a **Kotlin Multiplatform project**, declare the dependency only once in `commonMain`; Gradle resolves
the variant for each target (JVM, Android, wasm).

Click **Load Gradle Changes** in IntelliJ after every change.

---

## 4. Initialization

Same API as on the other platforms, **without** `.context(...)`:

```kotlin
import ai.tambo.tmodeler.db.Tdb
import ai.tambo.tmodeler.modeler.TModeler
import ai.tambo.tmodeler.extension.synchronization.core.TServerConfigBase

fun main() {
    TModeler.start()
        .connect(
            Tdb.Builder()
                .type(Tdb.Type.SQLITE)
                .dbDir("./data/")                       // database folder (file system path)
                .dbName("monapp")                       // SQLite file
                .server(TServerConfigBase())            // no synchronization
                .accept(User().clazz().getPackage())    // models package
                .start()!!
        )

    User.tms.commit()
    // … your code
}
```

With a synchronization, security or AI engine: `.synchronization(…)` / `.security(…)` / `.intelligence(…)` before `.connect(...)`
([general guide §17](01-tmodeler.md#17-engines-tsyncengine-tsecurityengine-and-taiengine)).

> Library test example: `.security(TSecurityEngine()).connect(Tdb.Builder().type(SQLITE).dbDir("/tmp/sql")
> .dbName("test9").autoServe(true).server(TServerConfigBase()).accept(pkg).start()!!)`.

---

## 5. JVM specifics

| Topic | Detail |
|---|---|
| **Database** | SQLite through **JDBC** (`sqlite-jdbc`, bundled with TModeler). One `<dbName>` file in `dbDir` |
| **Paths** | `dbDir` is a real file system path; create it beforehand if needed |
| **Threads** | `TGate.data { … }` remains mandatory for data access. `TGate.main` has no UI thread in a console app: on desktop, you need a main dispatcher (Swing/JavaFX/Compose) |
| **Termination** | a console program may exit before an asynchronous `TGate` operation completes: wait with `TGate.dataAwait { … }` or a `CountDownLatch` |
| **Files** | `TFile` uses the real file system |
| **Spatial** | `TspatialiteDriver` is available if the SpatiaLite extension is installed on the machine (`supportsSpatial(true)`) |
| **Network / push** | `Tnc` works over WebSocket; FCM does not exist on the JVM |

---

## 6. Tests

JVM tests are the fastest way to validate your models and queries.

```kotlin
class UserTest {
    @BeforeTest fun setup() {
        TModeler.start().connect(
            Tdb.Builder().type(Tdb.Type.SQLITE)
                .dbDir(createTempDir("tm").path + "/").dbName("test")
                .server(TServerConfigBase())
                .accept(User().clazz().getPackage()).start()!!
        )
        User.tms.commit()
    }

    @Test fun saveThenRead() {
        TGate.dataAwait {
            val u = User(); u.username_ = "alice"; u.save()
            assertEquals(1, User.tms.size())
        }
    }
}
```

```powershell
.\gradlew.bat test
```

Use a **temporary database** per test to avoid interference.

---

## 7. JVM troubleshooting

| Symptom | Cause | Solution |
|---|---|---|
| `Context receivers are not enabled` | Kotlin option missing | `-Xcontext-receivers` (§3) |
| `Could not resolve co.tamboai:TModeler` | `mavenCentral()` missing | add `mavenCentral()` and check the version number |
| `UnsupportedClassVersionError` | JDK too old | JDK ≥ 11 (17 recommended) |
| `No suitable driver` / `sqlite` not found | dependency excluded | do not exclude `sqlite-jdbc` |
| Database not found / `unable to open database file` | `dbDir` does not exist | create the folder |
| The program exits without printing anything | asynchronous operation not awaited | `TGate.dataAwait` / latch |
| `TGate.main` does nothing | no main thread | provide a main dispatcher or use `TGate.data` |
