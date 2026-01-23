# Hytale Plugin Manifest Generator

<a href="https://discord.koboo.eu/">
<img src="https://img.shields.io/discord/1021053609359708211?color=purple" alt="PluginPortal">
</a>
<a href="https://plugins.gradle.org/plugin/eu.koboo.pluginmanifest">
<img src="https://img.shields.io/gradle-plugin-portal/v/eu.koboo.pluginmanifest?color=green" alt="PluginPortal">
</a>
<a href="LICENSE">
<img src="https://img.shields.io/github/license/Koboo/hytale-pluginmanifest?color=blue" alt="LICENSE">
</a>

This project enables you to generate your Hytale Plugin's ``manifest.json`` automatically.

## Overview

- [Installation](#installation)
- [Configuration](#configuration)
- [Automatic properties](#automatic-properties)
- [Automatic configuration example](#automatic-configuration-example)
- [Manifest specification](#manifest-specification)
- [Add snapshots repository](#add-snapshots-repository)
- [Dependencies](https://github.com/Koboo/hytale-pluginmanifest/blob/main/build.gradle.kts#L18)
- [Credits](#credits)
- [MIT LICENSE](LICENSE)

## Features

- Validates, generates and includes ``manifest.json`` for your plugin
- Automatically detects your client-installation
- Runs a development server using your client-installation
- Adds ``HytaleServer.jar`` as a dependency to your project
- Decompiles ``HytaleServer.jar`` into ``HytaleServer-sources.jar``
- Supports ``Windows``, ``Linux`` and ``Mac``
- Supports ``com.gradleup.shadow`` (shadowJar)
- Supports ``org.gradle.configuration-cache``
- Supports ``org.gradle.configureondemand``
- Supports ``org.gradle.cache``

## Installation

- [Hosted @ GradlePluginPortal](https://plugins.gradle.org/plugin/eu.koboo.pluginmanifest)
- [Hosted @ EntixReposilite](https://repo.entix.eu/#/releases/eu/koboo/pluginmanifest-plugin)

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

## Configuration

You can override properties of the generated ``manifest.json``.

````kotlin
pluginManifest {
    // Configuration for your client installation detection
    clientInstallation {
        // Where should we check for your Hytale installation?
        // If it's in the default installation directory,
        // the plugin will detect it automatically.
        clientInstallDirectory = "C:/Users/Koboo/AppData/Roaming/Hytale"

        // Which patchline do you want to use?
        // Currently supported:
        // - RELEASE
        // - PRE_RELEASE
        // Both patchlines use an own directory in the client-installation.
        // So you can switch between both anytime.
        // You have to make sure, the client already downloaded the patchline previously.
        patchline = Patchline.RELEASE
    }

    // Configuration for the server runtime directory
    runtimeConfiguration {
        // If you want to automatically build your plugin
        // and run it on the same HytaleServer.jar,
        // just set any directory here.
        runtimeDirectory = "run/" // Defaults to "null" (Not configured)

        // Set this to false if you provide an absolute path in "runtimeDirectory",
        // otherwise the file resolving will be buggy.
        isProjectRelative = true // Defaults to "true"

        // Should we copy the plugin to the "mods/" directory of the server
        // or should we append the projects "build/libs/" as mod directory?
        // If you have multiple jar inside your "build/libs/" i.e.
        // - "*-sources.jar"
        // - "*-javadoc.jar"
        // - "*-all.jar"
        // You should enable this option,
        // because the server tries to load every jar file as a plugin.
        // We try to automatically copy the correct plugin jar,
        // if there is more than 1 jar file inside "build/libs/".
        copyPluginToRuntime = false // Defaults to "false"

        // We delete the logs directory before we start the server.
        // Why? Because we save diskSpace then ever we can.
        deleteLogsOnStart = true // Defaults to "true"

        // Shortcuts for the commonly used server arguments
        allowOp = true // Defaults to "true"
        bindAddress = "0.0.0.0:5520" // Defaults to "0.0.0.0:5520"

        // Customize as you like
        // These are just example values
        jvmArguments = listOf("-Xmx2048m") // Defaults to "EMPTY"
        serverArguments = listOf("--assets CustomAssets.zip") // Defaults to "EMPTY"
    }

    // Configuration for the manifest.json generation
    manifestConfiguration {
        // Required (AUTOMATICALLY RESOLVED)
        pluginGroup = "Koboo" // Defaults to your project's group
        pluginName = "MyPlugin" // Defaults to your project's name
        pluginVersion = "1.0.0" // Defaults to your project's version

        // Required (AUTOMATICALLY RESOLVED)
        // Sets the required serverVersion for your plugin.
        // Needs to be set in SemVerRange format
        // "*"          - Any serverVersion
        // ">=1.0.0"    - serverVersion needs to be greater or equal to 1.0.0
        serverVersion = "*" // Defaults to "*"

        // Required (AUTOMATICALLY RESOLVED)
        // That's your Main-Class / starting point of the plugin.
        // The provided class has to "extends JavaPlugin".
        pluginMainClass = "eu.koboo.myplugin.MyPlugin" // Detected by source-file scanning

        // Optional
        pluginDescription = "MyPlugin, that does awesome things!"
        // Optional, if set it needs to be a valid URL.
        pluginWebsite = "https://github.com/Koboo/MyPlugin"

        // Optional
        // The plugin doesn't start automatically with the server,
        // so you need to start it manually in-game with the commands:
        // /plugin load <PLUGIN_NAME>
        disabledByDefault = false // Defaults to false

        // Optional (AUTOMATICALLY RESOLVED)
        // Does this plugin contain any assets?
        // These can be i.e., model-, texture, or ui-files
        includesAssetPack = false // Defaults to false

        // Required (one author)
        // If didn't configure an author,
        // we fall back to resolve these authors:
        // 1. Your OS-user -> System.getProperty("user.name")
        // 2. Project Name -> project.name + " Author" (e.g. "MyPlugin Author")
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
            // Dependency version fallbacks to "*" (any version)
            required("Nitrado:WebServer")
            // Dependency is optional -> Plugin does not fail to load if dependency is not available
            // Dependency version needs to be greater or equal to "1.0.0"
            optional("Nitrado:QueryPlugin", ">=1.0.0")
            // MyPlugin needs to load before this plugin
            // (Not tested if load fails or not)
            loadBefore("OtherGroup:OtherPlugin")
        }
    }
}
````

## Automatic properties

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

## Automatic configuration example

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

| ``manifest.json`` Key    | Validation                                                  | Required | Example                                |
|--------------------------|-------------------------------------------------------------|----------|----------------------------------------|
| ``Group``                | UTF-8 ``String``                                            | ✅        | ``Koboos-Plugins``                     |
| ``Name``                 | UTF-8 ``String``                                            | ✅        | ``my-plugin``                          |
| ``Version``              | SemVer format ``MAJOR.MINOR.PATCH-RELEASE1.RELEASE2+BUILD`` | ✅        | ``1.0.0-SNAPSHOT.PRERELEASE+1d27cwq``  |
| ``ServerVersion``        | SemVerRange format                                          | ✅        | ``*``, ``>=1.0.0``                     |
| ``Main``                 | Fully qualified class name                                  | ✅        | ``eu.koboo.myplugin.MyPlugin``         |
| ``Authors`` - ``Name``   | UTF-8 ``String``                                            | ✅        | ``Koboo``                              |
| ``Authors`` - ``Email``  | E-Mail-Address format ``{PREFIX}@{DOMAIN}.{TLD}``           | ❌        | ``admin@koboo.eu``                     |
| ``Authors`` - ``Url``    | URI format ``{https\|http}://{DOMAIN}.{TLD}``               | ❌        | ``https://koboo.eu``                   |
| ``Description``          | UTF-8 ``String``                                            | ❌        | ``My awesome plugin that does things`` |
| ``Website``              | URI format ``{https\|http}://{DOMAIN}.{TLD}``               | ❌        | ``https://github.com/Koboo/MyPlugin``  |
| ``Dependencies``         | ``"{PluginGroup:PluginName}": "{SemVerRange}"``             | ❌        | ``"Koboos-Plugins": "*"``              |
| ``OptionalDependencies`` | See above, same as ``Dependencies``                         | ❌        | See above, same as ``Dependencies``    |
| ``LoadBefore``           | See above, same as ``Dependencies``                         | ❌        | See above, same as ``Dependencies``    |

## Add snapshots repository

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

## Credits

- [Hytale Server Manual](https://support.hytale.com/hc/en-us/articles/45326769420827-Hytale-Server-Manual)
- [HytaleModding Docs](https://hytalemodding.dev/en/docs)
- [FlatIcon / FreePik](https://www.flaticon.com/free-icons/stamp)
- [Kotlin Logo](https://commons.wikimedia.org/wiki/File:Kotlin_Icon.png)
- [Groovy Logo](https://www.pngfind.com/mpng/iRwoTwo_file-groovy-logo-svg-groovy-language-logo-hd/)
- [Maven Logo](https://www.stickpng.com/de/img/comics-und-fantasy/technologieunternehmen/apache-maven-federn)
- [HytaleGradlePlugin](https://github.com/MrMineO5/HytaleGradlePlugin)