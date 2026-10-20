import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

ksp {
    arg("tmodeler.registryPackage", "ai.tambo.tmodeler.reflectrontest.generated")
}

dependencies {
    add("kspWasmJs", project(":tmodeler-ksp"))
}
