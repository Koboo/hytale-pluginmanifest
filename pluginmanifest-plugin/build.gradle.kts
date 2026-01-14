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
    implementation(project(":pluginmanifest-api"))
    implementation("org.jetbrains:annotations:26.0.2-1")
    implementation("com.github.javaparser:javaparser-core:3.25.9")
}

gradlePlugin {
    val githubUrl = findProperty("githubUrl") as String
    val projectDescription = findProperty("projectDescription") as String
    val projectDisplayName = findProperty("projectDisplayName") as String

    plugins {
        create("pluginManifestPlugin") {
            id = "eu.koboo.pluginmanifest"
            displayName = projectDisplayName
            description = "Gradle plugin $projectDescription"
            implementationClass = "eu.koboo.pluginmanifest.gradle.PluginManifestPlugin"
            website = githubUrl
            vcsUrl = githubUrl
            tags = setOf("hytale", "manifest", "generator")
        }
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
}