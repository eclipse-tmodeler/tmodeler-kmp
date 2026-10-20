# TModeler on iOS

> This document complements the [general guide](01-tmodeler.md).
>
> **Status: 🚧 in progress.** The library's iOS target exists (native iOS code: SQLite driver, files,
> thread handling) but it has **not yet been validated end to end** on a device or simulator, and
> no reference iOS app is shipped. This document describes the planned path; it will be completed once
> the platform is validated.

## 1. What is planned

| Item | Status |
|---|---|
| Kotlin/Native targets: `iosArm64` (device), `iosSimulatorArm64` and `iosX64` (simulator) | declared in the library |
| SQLite database, files, threads (`TGate`) on iOS | implemented in the library |
| Synchronization engine (TBridge) on iOS | not verified |
| Push notifications (APNs / FCM) via `Tnc` | not available |
| Reference SwiftUI app | not shipped (only the Kotlin Multiplatform skeleton) |

## 2. Prerequisites

- A **Mac** with **Xcode** (15 or later): Kotlin/Native compilation for iOS is not possible on Windows or
  Linux.
- **JDK 17**, **Android Studio** (with the Kotlin Multiplatform plugin) or **IntelliJ IDEA**, or **Fleet** for
  Kotlin; **Xcode** for the Swift part.
- An Apple developer account to test on a real device.

## 3. Principle

As on Android, your Kotlin code (models, features, controllers) is compiled into a **framework** (`Shared`),
imported by the SwiftUI app:

```swift
import Shared

struct ContentView: View { /* calls your Kotlin controllers */ }
```

In the Gradle project, the iOS targets produce the framework:

```kotlin
listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
    it.binaries.framework { baseName = "Shared"; isStatic = true }
}
```

Xcode then calls the Gradle task `embedAndSignAppleFrameworkForXcode`.

## 4. Principles to follow from now on

To make your code ready for iOS:

- Put models, features and controllers in a Kotlin Multiplatform module (`commonMain`), not in an Android
  module.
- Keep the UI passive: Swift displays, Kotlin decides (same contract as [Flutter](03-flutter.md) and
  [React](04-react.md)).
- Call data access through `TGate.data`; return to the main thread with `TGate.main`.

## 5. Next steps

Once the platform is validated, this document will cover: framework installation, initialization in
`@main App`, `Info.plist` and permissions, push notifications (APNs), notification tap, offline-first,
App Store publishing, troubleshooting.
