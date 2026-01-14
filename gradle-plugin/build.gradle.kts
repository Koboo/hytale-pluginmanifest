import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar

plugins {
    id("java")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version("2.0.0")
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(project(":manifest-api"))
    implementation("org.jetbrains:annotations:26.0.2-1")
    implementation("com.github.javaparser:javaparser-core:3.25.9")
}

gradlePlugin {
    plugins {
        create("pluginManifestPlugin") {
            id = "eu.koboo.pluginmanifest"
            displayName = "Hytale Plugin Manifest Generator"
            description = "Gradle plugin to automatically generate the manifest.json for Hytale plugins."
            implementationClass = "eu.koboo.pluginmanifest.gradle.PluginManifestPlugin"
            website = "https://github.com/Koboo/hytale-pluginmanifest"
            vcsUrl = "https://github.com/Koboo/hytale-pluginmanifest"
            tags = setOf("hytale", "manifest", "generator")
        }
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
}