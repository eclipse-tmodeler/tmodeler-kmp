plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    // alias(libs.plugins.composeMultiplatform) apply false
    // alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

// ---------------------------------------------------------------------------
// Publication Maven Central (Central Portal)
//   1. SIGNING_KEY / SIGNING_PASSWORD (variables d'environnement) : cle PGP armoree
//   2. .\gradlew.bat centralBundle  -> build/tmodeler-<version>-central-bundle.zip
//   3. Envoi manuel du zip sur https://central.sonatype.com/publishing
// Aucun secret dans le depot.
// ---------------------------------------------------------------------------
val centralPublications = setOf("kotlinMultiplatform", "jvm", "androidRelease", "wasmJs", "maven")
val centralStagingDir = layout.buildDirectory.dir("central-staging")

val cleanCentralStaging = tasks.register<Delete>("cleanCentralStaging") {
    delete(centralStagingDir)
}

subprojects {
    plugins.withId("maven-publish") {
        apply(plugin = "signing")

        val emptyJavadocJar = tasks.register<Jar>("emptyJavadocJar") {
            archiveClassifier.set("javadoc")
        }

        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "centralStaging"
                    url = uri(centralStagingDir)
                }
            }

            publications.withType<MavenPublication>().configureEach {
                if (name != "maven") {
                    artifact(emptyJavadocJar)
                }
                pom {
                    name.set(provider { artifactId })
                    description.set(
                        "TModeler: offline-first data modeling and synchronization framework " +
                            "for Kotlin Multiplatform (Android, JVM, Web)."
                    )
                    url.set("https://github.com/eclipse-tmodeler/tmodeler-kmp")
                    inceptionYear.set("2026")
                    organization {
                        name.set("TAMBO ai")
                        url.set("https://tamboai.co")
                    }
                    licenses {
                        license {
                            name.set("Eclipse Public License - v 2.0")
                            url.set("https://www.eclipse.org/legal/epl-2.0/")
                            distribution.set("repo")
                        }
                    }
                    developers {
                        developer {
                            id.set("tambo-ai")
                            name.set("Olivier TAMBO")
                            email.set("contact@tamboai.co")
                            organization.set("TAMBO ai")
                            organizationUrl.set("https://tamboai.co")
                        }
                    }
                    scm {
                        url.set("https://github.com/eclipse-tmodeler/tmodeler-kmp")
                        connection.set("scm:git:https://github.com/eclipse-tmodeler/tmodeler-kmp.git")
                        developerConnection.set("scm:git:ssh://git@github.com/eclipse-tmodeler/tmodeler-kmp.git")
                    }
                    issueManagement {
                        system.set("GitHub")
                        url.set("https://github.com/eclipse-tmodeler/tmodeler-kmp/issues")
                    }
                }
            }
        }

        val signingKey = providers.environmentVariable("SIGNING_KEY")
        val signingPassword = providers.environmentVariable("SIGNING_PASSWORD")
        if (signingKey.isPresent) {
            extensions.configure<SigningExtension> {
                useInMemoryPgpKeys(signingKey.get(), signingPassword.getOrElse(""))
                sign(extensions.getByType<PublishingExtension>().publications)
            }
        }

        // Evite les erreurs d'ordre entre tâches Sign et Publish (Kotlin Multiplatform)
        tasks.withType<AbstractPublishToMaven>().configureEach {
            dependsOn(tasks.withType<Sign>())
        }

        tasks.register("publishToCentralStaging") {
            group = "publishing"
            description = "Publie les artefacts a livrer sur Maven Central dans build/central-staging."
            dependsOn(cleanCentralStaging)
            dependsOn(tasks.withType<PublishToMavenRepository>().matching {
                it.repository.name == "centralStaging" && it.publication.name in centralPublications
            })
        }

        tasks.withType<PublishToMavenRepository>().configureEach {
            if (name.endsWith("ToCentralStagingRepository")) mustRunAfter(cleanCentralStaging)
        }
    }
}

if (gradle.startParameter.taskNames.any { it.endsWith("centralBundle") } &&
    !providers.environmentVariable("SIGNING_KEY").isPresent
) {
    throw GradleException("SIGNING_KEY is not set: the Central bundle must be signed.")
}

tasks.register<Zip>("centralBundle") {
    group = "publishing"
    description = "Construit le zip a envoyer sur le Central Portal (artefacts signes)."
    dependsOn(":shared:publishToCentralStaging", ":tmodeler-ksp:publishToCentralStaging")
    from(centralStagingDir)
    archiveFileName.set("tmodeler-${providers.gradleProperty("tmodeler.version").get()}-central-bundle.zip")
    destinationDirectory.set(layout.buildDirectory)
}