plugins {
    id("java")
    id("com.gradleup.shadow") version ("9.3.1")
}

subprojects {
    apply {
        plugin("java")
        plugin("com.gradleup.shadow")
    }

    group = "eu.koboo.pluginmanifest"
    version = "1.0.22"

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.42")
        annotationProcessor("org.projectlombok:lombok:1.18.42")
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        withSourcesJar()
        withJavadocJar()
    }

    tasks {
        compileJava {
            options.encoding = "UTF-8"
        }
        javadoc {
            options.encoding = "UTF-8"
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

    tasks.withType<Javadoc>().configureEach {
        (options as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
    }
}


sourceSets {
    main {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
    test {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}