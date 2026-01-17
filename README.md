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
- Resolves properties automatically
- Exposes configuration in ``build.gradle.kts``
- Automatically detects ``HytaleServer.jar``
- Automatically adds ``HytaleServer.jar`` as dependency
- Supports ``com.gradleup.shadow`` (shadowJar)
- Supports ``org.gradle.configuration-cache``
- Supports ``org.gradle.configureondemand``
- Supports ``org.gradle.cache``

## Gradle Plugin

- Exposes a configuration for manually setting properties.
- Tries to resolve required properties by current project and OS information.
- Can override automatically resolved properties

### Installation

- [Hosted @ GradlePluginPortal](https://plugins.gradle.org/plugin/eu.koboo.pluginmanifest)
- [Hosted @ EntixReposilite](https://repo.entix.eu/#/releases/eu/koboo/pluginmanifest-plugin)
- ![Latest version](https://img.shields.io/gradle-plugin-portal/v/eu.koboo.pluginmanifest?label=latest+version)

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
    // Here you can configure:
    // 1. manifest.json generation properties
    // 2. If and where your HytaleServer.jar is
    //
    // For more information see GitHub:
    // https://github.com/Koboo/hytale-pluginmanifest

    clientInstallation {
        // Where should we check for your Hytale installation?
        // If it's in the default installation directory,
        // the plugin will probably detect its location automatically.
        clientInstallDirectory = "C:/Users/Koboo/AppData/Roaming/Hytale"

        // Which patchline do you want to use?
        patchline = Patchline.RELEASE
    }

    runtimeConfiguration {
        // If you want to automatically build your plugin
        // and run it on the same HytaleServer.jar,
        // just set any directory here.
        runtimeDirectory = "D:/PluginManifestRuntime"

        // Shortcuts for the commonly used server arguments
        acceptEarlyPlugins = false
        allowOp = false
        enableNativeAcces = true
        bindAddress = "0.0.0.0:5520"

        // Customize as you like
        jvmArguments = listOf("-Xmx2048m")
        serverArguments = listOf("--assets CustomAssets.zip")
    }

    manifestConfiguration {
        // Required
        pluginGroup = "eu.koboo" // Defaults to your project's group
        pluginName = "MyPlugin" // Defaults to your project's name
        pluginVersion = "1.0.0" // Defaults to your project's version

        // Required, sets the required serverVersion for your plugin.
        // Needs to be set in SemVerRange format
        // "*"          - Any serverVersion
        // ">=1.0.0"    - serverVersion needs to be greater or equal to 1.0.0
        serverVersion = "*" // Defaults to "*"

        // Required, that's your Main-Class / starting point of the plugin.
        // The provided class has to "extends JavaPlugin".
        pluginMainClass = "eu.koboo.myplugin.MyPlugin" // Detected by source-file scanning

        // Optional
        pluginDescription = "MyPlugin, that does awesome things!"
        // Optional, if set it needs to be a valid URL.
        pluginWebsite = "https://github.com/Koboo/MyPlugin"

        // The plugin doesn't start automatically with the server,
        // so you need to start it manually ingame with the commands:
        // /plugin load <PLUGIN_NAME>
        disabledByDefault = false // Defaults to false

        // Does this plugin contain any assets?
        // These can be i.e., model-, texture, or ui-files
        includesAssetPack = false // Defaults to false

        // Minimizes the JSON string written into manifest.json
        // What is minimizing?
        // Minimizing just removes all unnecessary spaces and line-breaks,
        // so the file size gets reduced to the bare minimum but sacrifices readability.
        minimizeJson = false // Defaults to false

        // You need to set at least 1 author.
        // If you set NO author, a default author is created with:
        // 1. Your OS-user -> System.getProperty("user.name")
        // 2. If no userName available -> project.name + "-Author" (e.g. "MyPlugin-Author")
        authors {
            author {
                name = "Koboo"
                email = "admin@koboo.eu"
                url = "https://koboo.eu/"
            }
            author {
                name = "AnotherAuthor"
                email = "author@example.com"
            }
            author {
                name = "OnlyNameIsRequiredOnAuthor"
            }
        }

        // Optional, if you don't have any plugin dependency,
        // you can safely remove/delete this.
        pluginDependencies {
            // Dependency is required -> Plugin fails if dependency is not available
            required("Nitrado:WebServer")
            // Dependency is optional -> Plugin does not fail if dependency is not available
            optional("Nitrado:QueryPlugin", ">=1.0.0")
            // MyPlugin needs to load before this plugin (Not tested if it fails, if the plugin is not available)
            loadBefore("OtherGroup:OtherPlugin")
        }
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

| Source                         | Example                                                            | Result                                   | Override with            |
|--------------------------------|--------------------------------------------------------------------|------------------------------------------|--------------------------|
| Gradle ``build.gradle.kts``    | ``group = "eu.koboo"``                                             | ``"Group": "koboo"``                     | ``pluginGroup``          |
| Gradle ``settings.gradle.kts`` | ``rootProject.name = "MyPlugin"``                                  | ``"Name": "MyPlugin"``                   | ``pluginName``           |
| Gradle ``build.gradle.kts``    | ``version = "1.0.0"``                                              | ``"Version": "1.0.0"``                   | ``pluginVersion``        |
| OS Username                    | ``String username = System.getProperty("user.name");``             | ``"Authors": [ { "Name": "Koboo" } ]``   | See manual configuration |
| Project's java files           | ``public class eu.koboo.myplugin.MyPlugin extends JavaPlugin { }`` | ``"Main": "eu.koboo.myplugin.MyPlugin"`` | ``pluginMainClass``      |
| Default serverVersion          | /                                                                  | ``"ServerVersion": "*"``                 | ``serverVersion``        |

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
    public YourPlugin(@NonNull JavaPluginInit init) {
        super(init);
    }
}
````

The Gradle plugin's generated ``manifest.json``:

````json
{
    "Group": "koboo",
    "Name": "hytale-plugin-template",
    "ServerVersion": "*",
    "Main": "your.plugin.YourPlugin",
    "Version": "1.0.0",
    "Authors": [
        {
            "Name": "Koboo"
        }
    ]
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

## Add EntixReposilite as repository

Add this to your ``settings.gradle.kts`` for snapshot-versions.

1. <img src=".idea/groovy_logo.png" height="10em" alt="Groovy Logo"></img> **Groovy DSL: ``settings.gradle``**

  ````groovy
   pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name 'entixReposilite'
            url 'https://repo.entix.eu/snapshots'
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
         url = uri("https://repo.entix.eu/snapshots")
       }
     }
   }
   ````

## Dependencies

- ``org.projectlombok:lombok:1.18.42``
- ``org.jetbrains:annotations:26.0.2-1``
- ``com.github.javaparser:javaparser-core:3.25.9``

## Credits

- [Hytale Server Manual](https://support.hytale.com/hc/en-us/articles/45326769420827-Hytale-Server-Manual)
- [HytaleModding Docs](https://hytalemodding.dev/en/docs)
- [FlatIcon / FreePik](https://www.flaticon.com/free-icons/stamp)
- [Kotlin Logo](https://commons.wikimedia.org/wiki/File:Kotlin_Icon.png)
- [Groovy Logo](https://www.pngfind.com/mpng/iRwoTwo_file-groovy-logo-svg-groovy-language-logo-hd/)
- [Maven Logo](https://www.stickpng.com/de/img/comics-und-fantasy/technologieunternehmen/apache-maven-federn)
- [HytaleGradlePlugin](https://github.com/MrMineO5/HytaleGradlePlugin)