# Hytale Plugin Manifest Generator

This project enables you to generate your Hytale Plugin's ``manifest.json`` automatically.

## Overview

- [Gradle Plugin](#gradle-plugin)
    - [What does it do?](#what-does-it-do)
    - [Installation](#installation-gradle-plugin)
    - [Configuration](#configuration-gradle-plugin)
    - [Automatic properties](#automatic-properties-gradle-plugin)
    - [Automatic configuration example](#automatic-properties-gradle-plugin)
- [AnnotationProcessor](#annotationprocessor)
  - [Installation](#installation-annotationprocessor)
  - [Configuration](#configuration-annotationprocessor)
- [Manifest specification](#manifest-specification)

## Gradle Plugin

The Gradle plugin exposes a configuration for manually setting the values but also tries to resolve
the required values by using your Gradle project. The automatically resolved values can be overridden.

### What does it do?

- Resolves values automatically
- Exposes Gradle configuration
- Validates provided values
- Logs user-friendly errors on validation issues
- Generates a valid ``manifest.json`` files
- Includes the generated ``manifest.json`` file into your plugin JAR-file
- Works with ``shadowJar``
- Works with ``org.gradle.configuration-cache=true``

### Installation (Gradle Plugin)

The Gradle plugin will be hosted on the Gradle plugin portal.

**Required environment**

- ``Gradle 9.2.1`` or newer
- ``JDK 25`` or newer

You only need to apply the plugin on your project.

**Groovy DSL: ``build.gradle``**

````groovy
plugins {
    id 'eu.koboo.pluginmanifest' version '1.0.23'
}
````

**Kotlin DSL: ``build.gradle.kts``**

````groovy
plugins {
    id("eu.koboo.pluginmanifest") version("1.0.23")
}
````

### Configuration (Gradle Plugin)

You can configure all values of the ``manifest.json`` inside the ``pluginManifest { }``.
Here is an example of all currently supported values:

````kotlin
pluginManifest {
    pluginGroup("Koboo")
    pluginName("MyPlugin")
    pluginVersion("1.0.0")
    // Or you can set all values at once by calling
    pluginMeta("Koboo", "MyPlugin", "1.0.0")

    pluginDescription("MyPlugin, that does awesome things!")
    pluginWebsite("https://github.com/Koboo/MyPlugin")
    pluginMainClass("eu.koboo.myplugin.MyPlugin")

    pluginDisabledByDefault(false)
    pluginIncludesAssetPack(false)

    minimizeJson(false)

    authors {
        author {
            authorName("Koboo")
            authorEmail("admin@koboo.eu")
            authorUrl("https://koboo.eu/")
        }
        author {
            authorName("AnotherAuthor")
            authorEmail("author@example.com")
            authorUrl("https://example.com/")
        }
    }

    pluginDependencies {
        required("Nitrado:WebServer")
        optional("Nitrado:QueryPlugin", ">=1.0.0")
        loadBefore("OtherGroup:OtherPlugin")
    }
}
````

### Automatic properties (Gradle Plugin)

Because, we all hate writing the same stuff all over again and we love automation, the Gradle plugin tries
to resolve the required values automatically.

In the table below, you can see where and how the Gradle plugin resolves automatically generated values.

Table description:

- ``manifest.json Key`` - The JSON key within the ``manifest.json`` file
- ``Source`` - From where does the plugin get the value?
- ``Example`` - How would the ``Source`` look like?
- ``Result`` - What does it look like in the ``manifest.json``?
- ``Override with`` - How can I override these values?

| ``manifest.json`` Key | Source                         | Example                                                             | Result                                  | Override with               |
|-----------------------|--------------------------------|---------------------------------------------------------------------|-----------------------------------------|-----------------------------|
| ``Group``             | Gradle ``build.gradle.kts``    | ``group = "eu.koboo"``                                              | ``"Group": "koboo"``                    | ``pluginGroup(String)``     |
| ``Name``              | Gradle ``settings.gradle.kts`` | ``rootProject.name = "MyPlugin"``                                   | ``"Name": "MyPlugin"``                  | ``pluginName(String)``      |
| ``Version``           | Gradle ``build.gradle.kts``    | ``version = "1.0.0"``                                               | ``"Version": "1.0.0"``                  | ``pluginVersion(String)``   |
| ``Author``            | OS Username                    | ``String username = System.getProperty("user.name");``              | ``"Authors": [ { "Name": "Koboo" } ]``  | See manual configuration    |
| ``Main``              | Project's java files           | ``public class eu.koboo.myplugin.MyPlugin extends JavaPlugin { } `` | ``"Main": "eu.koboo.myplugin.MyPlugin`` | ``pluginMainClass(String)`` |

> [!IMPORTANT]
> The Gradle plugin scans your source files (``*.java``) and tries to find a file, which ``extends JavaPlugin``
> If no class is found, a warning is logged
> If more than one class is found, a warning is logged

#### Automatic configuration example (Gradle Plugin)

Here you can see a complete example, where and how the values are generated, based on a common Gradle project structure
with a single module.

File: ``build.gradle.kts``

````kotlin
group = "eu.koboo"
version = "1.0.0-SNAPSHOT"
````

File: ``settings.gradle.kts``

````kotlin
rootProject.name = "MyPlugin"
````

File: ``src/main/java/eu/koboo/myplugin/MyPLugin.java``

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


### Installation (AnnotationProcessor)

The AnnotationProcessor will be hosted on MavenCentral.

**Required environment**

- ``Gradle 9.2.1`` or newer
- ``JDK 25`` or newer

You only need to add the dependency to your project.

**Groovy DSL: ``build.gradle``**

````groovy
dependency {
    compileOnly 'eu.koboo:pluginmanifest-api:1.0.23'
    annotationProcessor 'eu.koboo:pluginmanifest-api:1.0.23'
}
````

**Kotlin DSL: ``build.gradle.kts``**

````groovy
dependency {
  compileOnly("eu.koboo:pluginmanifest-api:1.0.23")
  annotationProcessor("eu.koboo:pluginmanifest-api:1.0.23")
}
````

### Configuration (AnnotationProcessor)

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
public class MyPlugin extends JavaPlugin{

}
````

## Manifest specification

Some keys and values of the ``manifest.json`` are required and some are optional. They are explaind in this table:

| ``manifest.json`` Key    | Validation                                                  | Required | Example                                |
|--------------------------|-------------------------------------------------------------|----------|----------------------------------------|
| ``Group``                | Only ``A-Z``, ``a-z``, ``0-9`` or ``-``                     | ✅        | ``Koboos-Plugins``                     |
| ``Name``                 | Only ``A-Z``, ``a-z``, ``0-9`` or ``-``                     | ✅        | ``my-plugin``                          |
| ``Version``              | SemVer format ``MAJOR.MINOR.PATCH-RELEASE1.RELEASE2+BUILD`` | ✅        | ``1.0.0-SNAPSHOT.PRERELEASE+1d27cwq``  |
| ``Description``          | UTF-8 ``String``                                            | ❌        | ``My awesome plugin that does things`` |
| ``Authors`` - ``Name``   | UTF-8 ``String``                                            | ✅        | ``Koboo``                              |
| ``Authors`` - ``Email``  | Email-Schema ``{PREFIX}@{DOMAIN}.{TLD}``                    | ❌        | ``admin@koboo.eu``                     |
| ``Authors`` - ``Url``    | Url-Schema ``{https\|http}://{DOMAIN}.{TLD}``               | ❌        | ``https://koboo.eu``                   |
| ``Website``              | Url-Schema ``{https\|http}://{DOMAIN}.{TLD}``               | ❌        | ``https://github.com/Koboo/MyPlugin``  |
| ``ServerVersion``        | SemVerRange format                                          | ✅        | ``*``, ``>=1.0.0``                     |
| ``Dependencies``         | ``"{PluginGroup:PluginName}": "{SemVerRange}"``             | ❌        | ``"Koboos-Plugins": "*"``              |
| ``OptionalDependencies`` | See above, same as ``Dependencies``                         | ❌        | See above, same as ``Dependencies``    |
| ``LoadBefore``           | See above, same as ``Dependencies``                         | ❌        | See above, same as ``Dependencies``    |
| ``Main``                 | Fully qualified class name                                  | ✅        | ``eu.koboo.myplugin.MyPlugin``         |

