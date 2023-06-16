import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "one.devos"
version = System.getenv("GITHUB_SHA") ?: "Unknown"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    // Logger libraries for writing to the console
    api("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.4.8")
    implementation("ch.qos.logback:logback-core:1.4.8")
    // Kotlin
    implementation(kotlin("stdlib-common"))
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.7.1")
    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    // TOML Serialization - YW Devin
    implementation("com.akuleshov7:ktoml-core:0.5.0")
    implementation("com.akuleshov7:ktoml-source:0.5.0")
    implementation("com.akuleshov7:ktoml-file:0.5.0")
    // JDA
    implementation("com.github.DV8FromtheWorld:JDA:v5.0.0-beta.10") {
        exclude(module = "opus-java")
    }
    // Kotlin Extensions for JDA
    implementation("com.github.minndevelopment:jda-ktx:0.9.6-alpha.22")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xinline-classes", "-opt-in=kotlin.RequiresOptIn")
    }
}

tasks.withType<ShadowJar> {
    archiveFileName.set("Yiski5-Fat.jar")
    manifest {
        attributes(
            mapOf(
                "Main-class" to "one.devos.yiski.Yiski",
                "Implementation-Version" to project.version
            )
        )
    }
}