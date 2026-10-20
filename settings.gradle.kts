rootProject.name = "TModeler"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()

        mavenLocal()
        maven {
            url = uri("https://www.jitpack.io" )
        }
    }

    /*
    plugins {
        id("app.cash.sqldelight") version "2.0.0"
    }*/
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// include(":composeApp")
// include(":server")
include(":shared")
include(":tmodeler-ksp")
include(":reflectron-test")