import java.net.URI

plugins {
    id("java")
    id("java-gradle-plugin")
    //id("com.gradle.plugin-publish") version("2.0.0") apply false
    id("maven-publish")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains:annotations:26.0.2-1")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

gradlePlugin {
    plugins {
        create("plugin-manifest") {
            id = "plugin-manifest"
            displayName = "Hytale Plugin Manifest Generator"
            description = "Gradle plugin to automatically generate the manifest.json for Hytale plugins."
            implementationClass = "eu.koboo.pluginmanifest.PluginManifestPlugin"
            tags.addAll("hytale", "plugin", "manifest", "generator")
        }
    }
}

publishing {
    repositories {
        mavenLocal()
        maven {
            name = "entixReposilite"
            //var repoUrl = project.version.toString().endsWith("-SNAPSHOT") ? "snapshots" : "releases"
            url = URI.create("https://repo.entix.eu/snapshots")
            credentials {
                username = System.getenv("ENTIX_REPO_USER")
                password = System.getenv("ENTIX_REPO_PASS")
            }
        }
    }
}