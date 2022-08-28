import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "one.devos"
version = "5.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    // Logger libraries for writing to the console
    api("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("ch.qos.logback:logback-core:1.2.11")
    // Kotlin
    implementation(kotlin("stdlib-common"))
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.6.4")
    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    // TOML Serialization - YW Devin
    implementation("com.akuleshov7:ktoml-core:0.2.13")
    implementation("com.akuleshov7:ktoml-file:0.2.13")
    // JDA
    implementation("com.github.DV8FromtheWorld:JDA:v5.0.0-alpha.18") {
        exclude(module = "opus-java")
    }
    // Kotlin Extensions for JDA
    implementation("com.github.minndevelopment:jda-ktx:081a177")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn")
    }
}

tasks.withType<ShadowJar> {
    archiveFileName.set("Yiski.jar")
    manifest {
        attributes(
            mapOf(
                "Main-class" to "one.devos.yiski.Yiski",
                "Implementation-Version" to project.version
            )
        )
    }
}