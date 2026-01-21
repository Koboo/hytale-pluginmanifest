import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar

plugins {
    id("maven-publish")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version("2.0.0")
    id("com.gradleup.shadow") version ("9.3.1")
}

group = "eu.koboo"
version = "1.0.24-rc.1"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    implementation("org.jetbrains:annotations:26.0.2-1")
    implementation("com.github.javaparser:javaparser-core:3.25.9")
    implementation("org.vineflower:vineflower:1.11.2")
}

gradlePlugin {
    plugins {
        create("pluginManifestPlugin") {
            id = "eu.koboo.pluginmanifest"
            displayName = "Hytale Plugin Manifest Generator"
            description = "Gradle plugin to automatically generate the manifest.json for Hytale plugins."
            implementationClass = "eu.koboo.pluginmanifest.gradle.plugin.PluginManifestPlugin"
            website = "https://github.com/Koboo/hytale-pluginmanifest"
            vcsUrl = "https://github.com/Koboo/hytale-pluginmanifest"
            tags = setOf("hytale", "manifest", "generator")
        }
    }
}

tasks.shadowJar {
    // Required by gradle-publish-plugin
    archiveClassifier.set("")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    javadoc {
        options.encoding = "UTF-8"
        (options as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(emptyList<String>())
    }
    test {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}

publishing {
    repositories {
        mavenLocal()
        maven {
            name = "entixReposiliteReleases"
            url = uri("https://repo.entix.eu/releases")
            credentials {
                username = System.getenv("ENTIX_REPO_USER")
                password = System.getenv("ENTIX_REPO_PASS")
            }
        }
        maven {
            name = "entixReposiliteSnapshots"
            url = uri("https://repo.entix.eu/snapshots")
            credentials {
                username = System.getenv("ENTIX_REPO_USER")
                password = System.getenv("ENTIX_REPO_PASS")
            }
        }
    }
}