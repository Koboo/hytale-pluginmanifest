# Hytale Plugin Manifest Generator

<a href="https://central.sonatype.com/artifact/eu.koboo/pluginmanifest-api"><img src="https://img.shields.io/badge/maven-central-blue" alt="MavenCentral"></a>
<a href="https://plugins.gradle.org/plugin/eu.koboo.pluginmanifest"><img src="https://img.shields.io/badge/gradle-plugin_portal-blue" alt="PluginPortal"></a>
<a href="https://discord.koboo.eu/"><img src="https://img.shields.io/badge/discord-server-blue" alt="PluginPortal"></a>

This project enables you to generate your Hytale Plugin's ``manifest.json`` automatically.

## Overview

- [Gradle Plugin](#gradle-plugin)
    - [Installation](#installation)
    - [Configuration](#configuration)
    - [Automatic properties](#automatic-properties)
    - [Automatic configuration example](#automatic-configuration-example)
- [AnnotationProcessor](#annotationprocessor)
    - [Installation](#installation-1)
    - [Configuration](#configuration-1)
- [Manifest specification](#manifest-specification)
- [Dependencies](#dependencies)
- [Credits](#credits)
- [MIT LICENSE](LICENSE)

## Features

- Validates provided properties
- Logs user-friendly errors on validation issues
- Generates ``manifest.json``
- Includes generated ``manifest.json`` file into JAR-file
- Overrides existing ``manifest.json``
- Supports ``Windows``, ``Linux`` and ``Mac``
- 🟢 Resolves properties automatically
- 🟢 Exposes configuration for ``build.gradle.kts``
- 🟢 Automatically detect ``HytaleServer.jar``
- 🟢 Automatically add ``HytaleServer.jar`` as project dependency
- 🟢 Works with ``shadowJar``
- 🟢 Works with ``org.gradle.configuration-cache=true``

> [!NOTE]
> All features marked with 🟢 are only available for the [Gradle-Plugin](#gradle-plugin)

## Gradle Plugin

- Exposes a configuration for manually setting properties.
- Tries to resolve required properties by current project and OS information.
- Can override automatically resolved properties

### Installation

- [Gradle Plugin Portal](https://plugins.gradle.org/plugin/eu.koboo.pluginmanifest)
- [Entix Reposilite](https://repo.entix.eu/#/releases/eu/koboo/pluginmanifest-plugin)
- ![Latest version](https://img.shields.io/gradle-plugin-portal/v/eu.koboo.pluginmanifest?label=latest+version)
- [pluginmanifest-plugin](https://github.com/Koboo/hytale-pluginmanifest/tree/main/pluginmanifest-plugin)

**Required environment**

- ``Gradle 9.2.1`` or newer
- ``JDK 25`` or newer

1. <img src=".idea/groovy_logo.png" height="10em" alt="Groovy Logo"></img> **Groovy DSL: ``build.gradle``**
    ````groovy
    plugins {
        id 'eu.koboo.pluginmanifest' version 'LATEST_VERSION'
    }
    ````

2. <img src=".idea/kotlin_logo.png" height="10em" alt="Kotlin Logo"></img> **Kotlin DSL: ``build.gradle.kts``**
    ````kotlin
    plugins {
        id("eu.koboo.pluginmanifest") version("LATEST_VERSION")
    }
    ````

[See latest version](https://plugins.gradle.org/plugin/eu.koboo.pluginmanifest)

### Configuration

You can override properties of the generated ``manifest.json``.

````kotlin
pluginManifest {
    // Required
    pluginGroup("Koboo")
    // Required
    pluginName("MyPlugin")
    // Required
    pluginVersion("1.0.0")

    // Or you can set all properties at once by calling
    pluginMeta("Koboo", "MyPlugin", "1.0.0")

    // Required, sets the required serverVersion for your plugin.
    // Needs to be set in SemVerRange format
    // "*"          - Any serverVersion
    // ">=1.0.0"    - serverVersion needs to be greater or equal to 1.0.0
    // For Early-Access you can just set the wildcard "*".
    serverVersion("*")

    // Optional
    pluginDescription("MyPlugin, that does awesome things!")
    // Optional, if set it needs to be a valid URL.
    pluginWebsite("https://github.com/Koboo/MyPlugin")

    // That's your Main-Class / starting point of the plugin.
    // The provided class has to "extends JavaPlugin".
    pluginMainClass("eu.koboo.myplugin.MyPlugin")

    // The plugin doesn't start automatically with the server,
    // so you need to start it manually ingame with the commands:
    // /plugin load <PLUGIN_NAME>
    pluginDisabledByDefault(false)

    // Does this plugin contain any assets?
    // These can be i.e., model-, texture, or ui-files
    pluginIncludesAssetPack(false)

    // Minimizes the JSON string written into manifest.json
    // What is minimizing?
    // Minimizing just removes all unnecessary spaces and line-breaks,
    // so the file size gets reduced to the bare minimum but sacrifices readability.
    minimizeJson(false)

    // Automatically adds the HytaleServer.jar as dependency
    // It searches in the following locations by order:
    // - {rootProjectDirectory}/HytaleServer.jar
    // - {rootProjectDirectory}/libs/HytaleServer.jar
    // - {appDataDirectory}/Hytale/install/release/package/game/latest/Server/HytaleServer.jar
    // Here are examples of the above paths: (from Windows)
    // - C:/Users/Koboo/Projects/hytale-plugin-template/HytaleServer.jar
    // - C:/Users/Koboo/Projects/hytale-plugin-template/libs/HytaleServer.jar
    // - C:/Users/Koboo/AppData/Roaming/Hytale/install/release/package/game/latest/Server/HytaleServer.jar
    applyServerDependency(true)

    // By setting a value into serverJarPath(String) the following happens:
    // The plugin...
    // 1. ...checks if the file exists
    // 2. ...adds this file as dependency if it exists
    // 3. ...doesn't search in the locations mentioned in "applyServerDependency"
    // 4. ...logs an error if the file does not exist
    // You can specify an absolute or relative path.
    //
    // !! NOTE: IT NEEDS "applyServerDependency(true)" !!
    //          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    serverJarPath("C:/Users/Koboo/Downloads/HytaleServer.jar")

    // You need to set at least 1 author.
    authors {
        author {
            authorName("Koboo")
            authorEmail("admin@koboo.eu")
            authorUrl("https://koboo.eu/")
        }
        author {
            authorName("AnotherAuthor")
            authorEmail("author@example.com")
        }
        author {
            authorName("OnlyNameIsRequiredOnAuthor")
        }
    }

    // Optional, if you don't have any plugin dependency,
    // you can safely remove this block.
    pluginDependencies {
        // Dependency is required -> Plugin fails if dependency is not available
        required("Nitrado:WebServer")
        // Dependency is optional -> Plugin does not fail if dependency is not available
        optional("Nitrado:QueryPlugin", ">=1.0.0")
        // MyPlugin needs to load before this plugin (Not tested if it fails, if the plugin is not available)
        loadBefore("OtherGroup:OtherPlugin")
    }
}
````

### Automatic properties

Because we all hate writing the same stuff all over again, and we love automation, the Gradle plugin tries
to resolve the required properties automatically.

In the table below, you can see where we get properties from.

Table description:

- ``Source`` - Where does the gradle-plugin get the property?
- ``Example`` - How would the ``Source`` look like?
- ``Result`` - What does it look like in the ``manifest.json``?
- ``Override with`` - How can I override this property?

| Source                         | Example                                                            | Result                                   | Override with               |
|--------------------------------|--------------------------------------------------------------------|------------------------------------------|-----------------------------|
| Gradle ``build.gradle.kts``    | ``group = "eu.koboo"``                                             | ``"Group": "koboo"``                     | ``pluginGroup(String)``     |
| Gradle ``settings.gradle.kts`` | ``rootProject.name = "MyPlugin"``                                  | ``"Name": "MyPlugin"``                   | ``pluginName(String)``      |
| Gradle ``build.gradle.kts``    | ``version = "1.0.0"``                                              | ``"Version": "1.0.0"``                   | ``pluginVersion(String)``   |
| OS Username                    | ``String username = System.getProperty("user.name");``             | ``"Authors": [ { "Name": "Koboo" } ]``   | See manual configuration    |
| Project's java files           | ``public class eu.koboo.myplugin.MyPlugin extends JavaPlugin { }`` | ``"Main": "eu.koboo.myplugin.MyPlugin"`` | ``pluginMainClass(String)`` |
| Default serverVersion          | /                                                                  | ``"ServerVersion": "*"``                 | ``serverVersion(String)``   |

> [!IMPORTANT]
> The Gradle plugin scans your source files (``*.java``) and tries to find a file, which ``extends JavaPlugin``
> If no class is found, a warning is logged
> If more than one class is found, a warning is logged
> If only one class is found, it's used as pluginMainClass

#### Automatic configuration example

Here you can see a complete example, where and how the properties are generated,
based on a common Gradle project structure with a single module.

File: ``build.gradle.kts``

````kotlin
group = "eu.koboo"
version = "1.0.0-SNAPSHOT"
````

File: ``settings.gradle.kts``

````kotlin
rootProject.name = "MyPlugin"
````

File: ``src/main/java/eu/koboo/myplugin/MyPlugin.java``

````java
public class YourPlugin extends JavaPlugin {
    public YourPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
    }
}
````

The Gradle plugin's generated ``manifest.json``:

````json
{
    "Group": "koboo",
    "Name": "hytale-plugin-template",
    "Version": "1.0.0",
    "Authors": [
        {
            "Name": "Koboo"
        }
    ],
    "ServerVersion": "*",
    "Main": "your.plugin.YourPlugin"
}
````

## AnnotationProcessor

### Installation

- [Maven Central](https://central.sonatype.com/artifact/eu.koboo/pluginmanifest-api)
- [Entix Reposilite](https://repo.entix.eu/#/releases/eu/koboo/pluginmanifest-api)
- ![Latest api](https://img.shields.io/maven-central/v/eu.koboo/pluginmanifest-api?label=latest+version)
- [pluginmanifest-api](https://github.com/Koboo/hytale-pluginmanifest/tree/main/pluginmanifest-api)

**Required environment**

- ``JDK 25`` or newer

1. <img src=".idea/groovy_logo.png" height="10em" alt="Groovy Logo"></img> **Groovy DSL: ``build.gradle``**
    ````groovy
    dependency {
        compileOnly 'eu.koboo:pluginmanifest-api:LATEST_VERSION'
        annotationProcessor 'eu.koboo:pluginmanifest-api:LATEST_VERSION'
    }
    ````

2. <img src=".idea/kotlin_logo.png" height="10em" alt="Kotlin Logo"></img> **Kotlin DSL: ``build.gradle.kts``**
   ````kotlin
   dependency {
       compileOnly("eu.koboo:pluginmanifest-api:LATEST_VERSION")
       annotationProcessor("eu.koboo:pluginmanifest-api:LATEST_VERSION")
   }
   ````
3. <img src=".idea/maven_logo.png" height="13em" alt="Maven Logo"></img> **Maven: ``pom.xml``**
    ````xml
    <dependency>
        <groupId>eu.koboo</groupId>
        <artifactId>pluginmanifest-api</artifactId>
        <version>LATEST_VERSION</version>
        <scope>provided</scope>
    </dependency>
    ````

[See latest version](https://central.sonatype.com/artifact/eu.koboo/pluginmanifest-api)

### Configuration

Here is a complete example of how to use the provided annotations.

````java

@PluginManifest(
    group = "Koboo",
    name = "MyPlugin",
    version = "1.0.0",
    description = "My awesome description",
    authors = {
        @PluginAuthor(
            name = "Koboo"
        ),
        @PluginAuthor(
            name = "OtherAuthor",
            email = "author@example.com",
            url = "https://example.com"
        )
    },
    website = "https://github.com/Koboo/MyPlugin",
    dependencies = {
        @PluginDependency(pluginId = "Nitrado:WebServer"),
        @PluginDependency(pluginId = "Nitrado:QueryPlugin", type = DependencyType.OPTIONAL),
        @PluginDependency(pluginId = "Group:Name", version = ">=1.0.0", type = DependencyType.LOAD_BEFORE),
    },
    disabledByDefault = true,
    includesAssetPack = true
)
public class MyPlugin extends JavaPlugin {

}
````

## Manifest specification

- Keys and values of ``manifest.json`` are required or optional.
- Keys and values need to be validated.

| ``manifest.json`` Key    | Validation                                                  | Required | Example                                |
|--------------------------|-------------------------------------------------------------|----------|----------------------------------------|
| ``Group``                | UTF-8 ``String``                                            | ✅        | ``Koboos-Plugins``                     |
| ``Name``                 | UTF-8 ``String``                                            | ✅        | ``my-plugin``                          |
| ``Version``              | SemVer format ``MAJOR.MINOR.PATCH-RELEASE1.RELEASE2+BUILD`` | ✅        | ``1.0.0-SNAPSHOT.PRERELEASE+1d27cwq``  |
| ``Description``          | UTF-8 ``String``                                            | ❌        | ``My awesome plugin that does things`` |
| ``Authors`` - ``Name``   | UTF-8 ``String``                                            | ✅        | ``Koboo``                              |
| ``Authors`` - ``Email``  | E-Mail-Address format ``{PREFIX}@{DOMAIN}.{TLD}``           | ❌        | ``admin@koboo.eu``                     |
| ``Authors`` - ``Url``    | URI format ``{https\|http}://{DOMAIN}.{TLD}``               | ❌        | ``https://koboo.eu``                   |
| ``Website``              | URI format ``{https\|http}://{DOMAIN}.{TLD}``               | ❌        | ``https://github.com/Koboo/MyPlugin``  |
| ``ServerVersion``        | SemVerRange format                                          | ✅        | ``*``, ``>=1.0.0``                     |
| ``Dependencies``         | ``"{PluginGroup:PluginName}": "{SemVerRange}"``             | ❌        | ``"Koboos-Plugins": "*"``              |
| ``OptionalDependencies`` | See above, same as ``Dependencies``                         | ❌        | See above, same as ``Dependencies``    |
| ``LoadBefore``           | See above, same as ``Dependencies``                         | ❌        | See above, same as ``Dependencies``    |
| ``Main``                 | Fully qualified class name                                  | ✅        | ``eu.koboo.myplugin.MyPlugin``         |

## Add Reposilite as repository

### In ``build.gradle.kts``

1. <img src=".idea/groovy_logo.png" height="10em" alt="Groovy Logo"></img> **Groovy DSL: ``build.gradle``**

  ````groovy
  repositories {
    mavenCentral()
    maven {
        name 'entixReposilite'
        url 'https://repo.entix.eu/releases'
    }
}
  ````

2. <img src=".idea/kotlin_logo.png" height="10em" alt="Kotlin Logo"></img> **Kotlin DSL: ``build.gradle.kts``**

  ````kotlin
  repositories {
    mavenCentral()
    maven {
        name = "entixReposilite"
        url = uri("https://repo.entix.eu/releases")
    }
}
  ````

### In ``settings.gradle.kts``

1. <img src=".idea/groovy_logo.png" height="10em" alt="Groovy Logo"></img> **Groovy DSL: ``settings.gradle``**

  ````groovy
   pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name 'entixReposilite'
            url 'https://repo.entix.eu/releases'
        }
    }
}
  ````

2. <img src=".idea/kotlin_logo.png" height="10em" alt="Kotlin Logo"></img> **Kotlin DSL: ``settings.gradle.kts``**
   ````kotlin
   pluginManagement {
     repositories {
       gradlePluginPortal()
       maven {
         name = "entixReposilite"
         url = uri("https://repo.entix.eu/releases")
       }
     }
   }
   ````

## Dependencies

``pluginmanifest-api``dependencies:

- ``org.json:json:20251224``

``pluginmanifest-plugin`` dependencies:

- ``pluginmanifest-api``
- ``org.jetbrains:annotations:26.0.2-1``
- ``com.github.javaparser:javaparser-core:3.25.9``

## Credits

- [Hytale Server Manual](https://support.hytale.com/hc/en-us/articles/45326769420827-Hytale-Server-Manual)
- [HytaleModding Docs](https://hytalemodding.dev/en/docs)
- [FlatIcon / FreePik](https://www.flaticon.com/free-icons/stamp)
- [Kotlin Logo](https://commons.wikimedia.org/wiki/File:Kotlin_Icon.png)
- [Groovy Logo](https://www.pngfind.com/mpng/iRwoTwo_file-groovy-logo-svg-groovy-language-logo-hd/)
- [Maven Logo](https://www.stickpng.com/de/img/comics-und-fantasy/technologieunternehmen/apache-maven-federn)