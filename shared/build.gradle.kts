import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    id("maven-publish")
}

kotlin {
    jvmToolchain(11)

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }

    androidTarget {
        publishLibraryVariants("release")
    }
    
    // iOS : desactive par defaut (non publie en 1.0.x). Activer avec -Ptmodeler.ios=true sur macOS.
    if (providers.gradleProperty("tmodeler.ios").orNull == "true") {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "Shared"
                isStatic = true
            }
        }
    }
    
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.library()
        generateTypeScriptDefinitions()
        browser()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        }
        jvmMain.dependencies {
            implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.23")
            implementation("org.xerial:sqlite-jdbc:3.44.1.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit"))
            implementation("junit:junit:4.13.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
            implementation("org.json:json:20250517")
        }
        androidMain.dependencies {
            implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.23")
            implementation("androidx.sqlite:sqlite:2.3.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
            implementation("com.github.sevar83:android-spatialite:2.0.1")
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            compileOnly("com.google.firebase:firebase-messaging:24.1.0")
        }
        // iosMain : rien a declarer pour l'instant (iOS desactive par defaut)

        all {
            languageSettings {
                optIn("kotlin.contracts.ExperimentalContracts")
                optIn("kotlin.experimental.ExperimentalTypeInference")
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
                optIn("kotlin.experimental.ExperimentalObjCName")
                optIn("kotlin.experimental.ExperimentalAnnotation")
                optIn("kotlin.experimental.ExperimentalMultiplatform")
                optIn("kotlin.experimental.ExperimentalTime")
                optIn("kotlin.ExperimentalUnsignedTypes")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlin.experimental.ExperimentalAllowBreakContinueInInlineLambdas")
                //optIn("kotlin.experimental.ExperimentalContextReceivers")
            }
        }
    }
}

repositories {
    google()
    mavenCentral()
    mavenLocal()

    maven {
        url = uri("https://www.jitpack.io" )
    }
}

android {
    namespace = "ai.tambo.tmodeler"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

val pubGroup = providers.gradleProperty("tmodeler.group").get()
val pubVersion = providers.gradleProperty("tmodeler.version").get()

publishing {
    // Publications standard Kotlin Multiplatform, renommees :
    // TModeler (racine), TModeler-jvm, TModeler-android, TModeler-wasm-js
    // afterEvaluate : le plugin Kotlin/Android fixe son artifactId tardivement
    afterEvaluate {
        publications.withType<MavenPublication>().configureEach {
            groupId = pubGroup
            version = pubVersion
            artifactId = when (name) {
                "kotlinMultiplatform" -> "TModeler"
                "androidRelease" -> "TModeler-android"
                "wasmJs" -> "TModeler-wasm-js"
                else -> "TModeler-${name.lowercase()}"
            }
        }
    }

    repositories {
        mavenLocal()
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("1.9.23")
        }
        if (requested.group == "org.jetbrains.kotlinx") {
            when (requested.name) {
                "kotlinx-serialization-json" -> useVersion("1.6.3")
                "kotlinx-datetime" -> useVersion("0.5.0")
            }
        }
    }
}

// Reflectron : KSP branché UNIQUEMENT sur wasmJs,
// jamais sur jvm/android — zéro risque pour les environnements déjà en prod.
dependencies {
    add("kspWasmJs", project(":tmodeler-ksp"))
}
