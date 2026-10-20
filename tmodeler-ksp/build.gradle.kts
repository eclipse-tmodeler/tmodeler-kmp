plugins {
    alias(libs.plugins.kotlinJvm)
    id("maven-publish")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = providers.gradleProperty("tmodeler.group").get()
            artifactId = "tmodeler-ksp"
            version = providers.gradleProperty("tmodeler.version").get()
            from(components["java"])
        }
    }
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.23-1.0.20")
}

kotlin {
    jvmToolchain(11)
}
