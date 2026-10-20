# TModeler on native Android (Kotlin / Java)

> This document complements the [general guide](01-tmodeler.md). It does not repeat the common concepts (models,
> queries, `TFlow`, `Tnc`, engines…): it describes **what is specific to Android**. Read §4 to §6 of the
> general guide first.

## Table of contents

1. [What this document covers](#1-what-this-document-covers)
2. [Environment](#2-environment)
3. [Creating the project](#3-creating-the-project)
4. [Gradle configuration](#4-gradle-configuration)
5. [The `Application` class](#5-the-application-class)
6. [The Manifest](#6-the-manifest)
7. [Recommended architecture: MVVM](#7-recommended-architecture-mvvm)
8. [Screens: Jetpack Compose and Views](#8-screens-jetpack-compose-and-views)
9. [Files, camera, audio: permissions](#9-files-camera-audio-permissions)
10. [Notifications and handling notification taps](#10-notifications-and-handling-notification-taps)
11. [Using TModeler from Java](#11-using-tmodeler-from-java)
12. [Code shrinking (R8 / ProGuard)](#12-code-shrinking-r8--proguard)
13. [Tests](#13-tests)
14. [Android troubleshooting](#14-android-troubleshooting)
15. [Production release checklist](#15-production-release-checklist)

---

## 1. What this document covers

| Topic | Where |
|---|---|
| Installing the Android SDK, the IDE, the emulator | §2 |
| Gradle (plugins, dependencies, Kotlin options) | §4 |
| Initializing TModeler in `Application` | §5 |
| `AndroidManifest.xml`, permissions, network security | §6 |
| MVVM with `ViewModel`, `StateFlow`, `TFlow` | §7 |
| Displaying lists (Compose / RecyclerView) | §8 |
| FCM notifications, taps, cold start | §10 |
| Java, R8, tests | §11 to §13 |

---

## 2. Environment

| Tool | Tested version | Note |
|---|---|---|
| **Android Studio** | Hedgehog (2023.1) or newer | Recommended IDE. IntelliJ IDEA also works with the Android plugin |
| JDK | 17 (the one bundled with Android Studio is fine) | to build an **app**; the TModeler library itself targets Java 11 |
| Android Gradle Plugin (AGP) | 8.2 to 8.11 | |
| Gradle | 8.2 to 8.14 | project wrapper |
| Kotlin | 1.9.x or 2.1.x | `-Xcontext-receivers` required (§4) |
| `minSdk` | **21** minimum | required by TModeler |
| `compileSdk` | 34 or higher | |

Installation:

1. Install Android Studio, open **Settings ▸ Languages & Frameworks ▸ Android SDK**, and install a
   platform (API 34+), *Build-Tools* and *Platform-Tools*.
2. Create an emulator (Device Manager) **with Google Play** if you want to test FCM notifications
   ("Google APIs" images without the Play Store do not always receive push messages), or plug in a phone
   (Developer options ▸ USB debugging).
3. Verify: `adb devices` must list the device.

> **Windows: is the system drive full?** Move the caches using the environment variables
> `GRADLE_USER_HOME` (Gradle cache) and `ANDROID_USER_HOME` / `ANDROID_AVD_HOME` (emulators), then restart the IDE.

---

## 3. Creating the project

*New Project ▸ Empty Activity* (Compose) or *Empty Views Activity*, language **Kotlin**. Choose
`minSdk = 21` or higher. Note the **package** (`applicationId`): it is also used by Firebase (§10).

---

## 4. Gradle configuration

### 4.1 `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")           // required by the SpatiaLite dependencies
    }
}
```

> TModeler is on Maven Central: `mavenCentral()` is enough. Only add `mavenLocal()` if you build TModeler from source (guide §5.2).

### 4.2 `app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")      // if you use kotlinx.serialization
    id("com.google.gms.google-services")                  // only if you use FCM (§10)
}

android {
    namespace = "com.exemple.monapp"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.exemple.monapp"
        minSdk = 21
        targetSdk = 34
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xcontext-receivers"      // REQUIRED
    }
}

dependencies {
    implementation("co.tamboai:TModeler:1.0.1")
    // Optional: engines (general guide §17)
    implementation("co.tamboai:TBridge:1.0.1")          // optional, on request (contact@tamboai.co): synchronization + Tnc
    // implementation("co.tamboai:TVault:1.0.1")        // encryption
    // implementation("co.tamboai:TAi:1.0.1")           // artificial intelligence

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // FCM (notifications outside the app)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
}
```

Google Services plugin (project-level `build.gradle.kts` file):

```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

Click **Sync Project with Gradle Files**. If the sync fails, see §14.

> TModeler **models** (classes inheriting from `TModel`) do not need any KSP plugin on Android: only the web
> target uses code generation ([React guide](04-react.md)).

---

## 5. The `Application` class

TModeler is initialized **once**, in `Application.onCreate()`, before any `Activity`, any `Service`
(FCM included) and any `BroadcastReceiver`. This is what makes tapping a notification work
when the app was dead (§10).

```kotlin
class MonApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = TServerConfig()
            .baseServerHost("https://api.example.com")
            .baseServerMediaHost("media")
            .baseLocalMediaDir("media")
            .baseLocalTmpDir("tmp")
            .autoSync(false)
            .supportsSpatial(false)

        TModeler.start()
            .synchronization(TBridge.start())
            .notifications {
                host("https://api.example.com")
                websocket { reconnectDelayMs(3_000) }
                fcm {
                    channelId("mon_canal")
                    channelName("My app")
                    defaultTitle("My app")
                    accept { data -> data["type"] == "message" }   // filter for the pushes to display
                }
            }
            .connect(
                Tdb.Builder()
                    .type(Tdb.Type.SQLITE)
                    .context(Tcontext(this))
                    .dbDir("db/")
                    .dbName("MonApp-Main-1")
                    .server(config)
                    .accept(User().clazz().getPackage())
                    .start()!!
            )

        // Create the tables
        User.tms.commit()
    }
}
```

> The exact options of `.notifications { … }` are described in the [general guide §16](01-tmodeler.md#16-instant-notifications-tnc).

Declare it in the Manifest: `android:name=".MonApplication"` (§6).

**One database per user?** If your app handles several accounts, include the account identifier in
`dbName` and re-run the initialization (or restart the process) when the account changes.

---

## 6. The Manifest

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />   <!-- Android 13+ -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />         <!-- only for voice messages -->

    <application
        android:name=".MonApplication"
        android:label="My app"
        android:icon="@mipmap/ic_launcher"
        android:networkSecurityConfig="@xml/network_security_config"
        android:allowBackup="true"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Nothing to declare for FCM: the receiving service is provided by TModeler (Tnc). -->
    </application>
</manifest>
```

Important points:

| Element | Why |
|---|---|
| `android:name` on `<application>` | without it, TModeler is **not** initialized when a push wakes the app |
| `launchMode="singleTop"` | a notification tap arrives in `onNewIntent` instead of creating a new activity |
| `INTERNET` | server, WebSocket, FCM |
| `POST_NOTIFICATIONS` | mandatory from Android 13 onward **and** must be requested at runtime (§10) |
| `networkSecurityConfig` | allows or restricts cleartext traffic (below) |

### Cleartext HTTP traffic (development)

By default, Android 9+ forbids `http://`. For local development only, create
`res/xml/network_security_config.xml`:

```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>   <!-- your PC's host as seen from the emulator -->
    </domain-config>
</network-security-config>
```

In production, use **HTTPS** and remove the exceptions.

> `10.0.2.2` designates the host PC inside the emulator. On a real phone, use the PC's IP address on the local network.

---

## 7. Recommended architecture: MVVM

```
UI (Compose / Fragment)   →   ViewModel   →   Repository   →   TModeler (models, TFlow, TFeature)
        ▲                          │
        └──── StateFlow ◄──────────┘
```

TModeler + MVVM principles:

1. **The local database is the source of truth.** The UI always reads local data; the network only
   refreshes it in the background ([guide §19](01-tmodeler.md#19-designing-an-offline-first-app)).
2. **Data access goes through `TGate.data`**; only the result returns to the main thread.
3. The `ViewModel` exposes `StateFlow`s; it knows nothing about the UI.

### Repository

```kotlin
class MessageRepository {
    fun newFlow(conversation: Messaging) =
        TFlow(Message.tms, TFlow.Mode.MANUAL, 30)

    fun save(msg: Message) = TGate.data { msg.save() }

    // Silent server refresh (Feature defined in the general guide §15)
    fun refresh(conversation: Messaging) {
        RefreshMessagingFeature.load(conversation) { /* the data is already in the local database */ }
    }
}
```

### ViewModel

```kotlin
class ChatViewModel(private val repo: MessageRepository) : ViewModel() {
    private lateinit var flow: TFlow<Message>
    private lateinit var conversation: Messaging

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state

    fun open(conv: Messaging) {
        conversation = conv
        flow = repo.newFlow(conv)
        // 1) display the local data IMMEDIATELY
        loadMore()
        viewModelScope.launch {
            flow.all.collect { list -> _state.update { it.copy(messages = list) } }
        }
        // 2) refresh in the background, without blocking the screen
        repo.refresh(conv)
    }

    fun loadMore() {
        flow.load(
            query = { manager, t ->
                manager.with(t).lazy().filter(t.messaging eq conversation).order(-t._id).sub(30, 0).build()
            },
            onLoaded = { page -> _state.update { it.copy(hasMore = page.size >= 30) } }
        )
    }
}

data class ChatState(val messages: List<Message> = emptyList(), val hasMore: Boolean = true)
```

Rules:

- **Never** load an entire list with `all()` in a screen: use `TFlow`.
- Do not block the UI on the network: display the local data, then let the `StateFlow` update when the
  new data arrives (see also `Tnc` for real-time updates).
- A screen rotation must not recreate the `TFlow`: keep it in the `ViewModel`.
- Models are **mutable**; for Compose, copy them into an immutable state (a UI data class) before
  exposing them, or trigger a recomposition by replacing the list.

---

## 8. Screens: Jetpack Compose and Views

### Compose

```kotlin
@Composable
fun ChatScreen(vm: ChatViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    LazyColumn(reverseLayout = true) {
        items(state.messages, key = { it._id }) { m -> MessageBubble(m) }
        if (state.hasMore) item { LaunchedEffect(Unit) { vm.loadMore() } }   // end of list reached
    }
}
```

### Views (RecyclerView)

Observe `state` in `lifecycleScope.launch { repeatOnLifecycle(STARTED) { … } }` and pass the list to a
`ListAdapter` + `DiffUtil` (key: `_id`).

During the very first load, display **skeletons** (placeholders) rather than a full-screen spinner.

---

## 9. Files, camera, audio: permissions

| Need | Permission / API |
|---|---|
| Pick an image/video/file | *Photo Picker* (`ActivityResultContracts.PickVisualMedia`) or *Storage Access Framework*: **no permission** |
| Take a photo | `ActivityResultContracts.TakePicture` (+ `FileProvider`); `CAMERA` only if you open the camera yourself |
| Record a voice message | `RECORD_AUDIO` (runtime permission) |
| Play downloaded media | private app file: no permission |

TModeler handles files through the models' file fields ([guide §14](01-tmodeler.md#14-files-and-media));
on the Android side, you simply provide the file (a path or a URI copied into the app's storage).

---

## 10. Notifications and handling notification taps

### 10.1 Firebase (FCM)

1. Firebase Console ▸ **Add an Android app** with your `applicationId`.
2. Download **`google-services.json`** and place it in `app/`.
3. `com.google.gms.google-services` plugin enabled (§4); `firebase-messaging` dependency.
4. On the server, configure the Firebase service account credentials (see
   [guide §18](01-tmodeler.md#18-server-side-python--django)).
5. **Do not commit** `google-services.json` or the service keys to a public repository.

TModeler (the `Tnc` module) registers the FCM token with the server and displays the notifications it receives.

### 10.2 Requesting the permission (Android 13+)

```kotlin
// MainActivity.onCreate
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
}
```

Without this permission being granted, **no** notification is displayed (neither the channel nor FCM).

### 10.3 Opening the right conversation on tap

Tapping a notification launches the main activity with an `Intent` containing the push data
(for example `messagingId`). Three situations:

| App | What happens | Where to read the intent |
|---|---|---|
| **Dead** (process killed) | Android starts the process → `Application.onCreate` (TModeler initializes, signed-in user restored) → `Activity.onCreate` | `onCreate`: `intent` |
| **In the background** | the existing activity returns to the foreground | `onNewIntent` (thanks to `singleTop`) |
| **In the foreground** | tap on a local notification | `onNewIntent` |

```kotlin
class MainActivity : ComponentActivity() {
    private val openChat = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent { AppRoot(openChat) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        val id = intent?.getStringExtra("messagingId")?.toLongOrNull() ?: return
        intent.removeExtra("messagingId")        // avoids replaying the action when the activity is recreated
        openChat.value = id                      // the UI navigates to the conversation
    }
}
```

In the UI, when `openChat` receives an id **and the user is signed in** and the database is ready, navigate to
the corresponding conversation (the id in the push is the **server id**: find the local object by that id, then
refresh it in the background if it is missing).

### 10.4 Displaying a local notification yourself

If you create a notification yourself (for example from an `accept`/`onOpen`), build a
`PendingIntent` to the main activity, with the extras, `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE` and a
unique `requestCode` per conversation (otherwise two notifications share the same intent):

```kotlin
val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)!!.apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    putExtra("messagingId", serverId.toString())
}
val pending = PendingIntent.getActivity(
    context, serverId.toInt(), launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
```

> Create the `NotificationChannel` (Android 8+) before the first notification: `IMPORTANCE_HIGH` so that it
> shows up as a banner.

---

## 11. Using TModeler from Java

TModeler is written in Kotlin. A **Java** project must still compile with the Kotlin plugin (to resolve
the Kotlin Multiplatform dependencies); add `org.jetbrains.kotlin.android` and keep `-Xcontext-receivers`.

Constraints:

- `suspend` APIs and Kotlin lambdas can be called from Java via `kotlin.jvm.functions.Function1`, etc.
  This is verbose; preferably write a **thin Kotlin layer** (repositories) and call it from Java.
- Static accesses go through `Message.Companion.getTms()` (or `Message.tms` if the `@JvmStatic` annotation
  is present).
- Initialization (§5) is done in Kotlin or through a Kotlin utility class called from your Java `Application`.

> Java support is not TModeler's primary use case: validate your use case on one screen before
> migrating an entire codebase.

---

## 12. Code shrinking (R8 / ProGuard)

In a *release* build with `minifyEnabled true`, keep your models (they are discovered by reflection):

```
-keep class com.exemple.monapp.models.** { *; }
-keep class ai.tambo.** { *; }
-keepattributes Signature, *Annotation*, InnerClasses
```

Adapt `com.exemple.monapp.models` to the package of your models. Always test a release build before
publishing.

---

## 13. Tests

- **Instrumented tests** (`androidTest`): run the real TModeler database on a device/emulator. Initialize
  TModeler in `@Before` with a test `dbName` (for example suffixed with a timestamp) and delete the database at the
  end.
- **JVM unit tests**: if your logic does not depend on Android, test it with JVM support
  ([JVM guide](05-jvm.md)).
- **ViewModels**: inject a fake `Repository` and test with `kotlinx-coroutines-test`.

Commands:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
adb logcat -s TModeler Tnc
```

---

## 14. Android troubleshooting

| Symptom | Likely cause | Solution |
|---|---|---|
| `Context receivers are not enabled` | Kotlin option missing | add `-Xcontext-receivers` (§4.2) |
| `Could not resolve co.tamboai:TModeler` | `mavenCentral()` missing | add `mavenCentral()` (§4.1) and check the version number |
| `Could not resolve … android-spatialite` | jitpack repository missing | add `maven("https://jitpack.io")` |
| The build fails on `minSdk` | `minSdk < 21` | set it to 21 |
| `Duplicate class kotlin…` | mixed Kotlin versions | align Kotlin/stdlib and `kotlinx-*` |
| Startup crash `TModeler not initialized` | init missing or too late | initialize in `Application.onCreate`, and declare `android:name` |
| No tables / `no such table` | `commit()` forgotten | `Model.tms.commit()` for each model |
| Crash on database access on the main thread | access outside `TGate.data` | wrap it in `TGate.data { … }` |
| No push received | permission denied, `google-services.json` missing, token not registered | §10.1, §10.2; check the token in the logs |
| Push received but tapping it opens the app without a conversation | extra read only once, or `onNewIntent` not handled | §10.3 (`singleTop` + `onNewIntent` + `getIntent`) |
| `Cleartext HTTP traffic not permitted` | cleartext HTTP | HTTPS or §6 |
| Works in *debug*, not in *release* | R8 strips the models | §12 |
| The build is slow / disk full | Gradle cache on the system drive | move `GRADLE_USER_HOME` |

---

## 15. Production release checklist

- [ ] HTTPS everywhere, `networkSecurityConfig` without development exceptions
- [ ] `google-services.json` from the **production Firebase project**
- [ ] R8 tested with the rules from §12
- [ ] Schema migration planned ([guide §20](01-tmodeler.md#20-known-limitations-per-platform)): `migrate()` backs up, empties, then recreates a table
- [ ] Minimal permissions in the Manifest
- [ ] Keys and credentials kept out of the repository
- [ ] Notification tap tested in all three states (dead / background / foreground)
- [ ] Offline test: airplane mode, opening the conversation list and a conversation (local data
      must display immediately)
