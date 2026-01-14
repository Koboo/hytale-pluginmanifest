plugins {
    id("java")
    id("maven-publish")
    id("com.gradleup.shadow") version ("9.3.1")
}

subprojects {
    apply {
        plugin("java")
        plugin("maven-publish")
        plugin("com.gradleup.shadow")
    }

    group = "eu.koboo"
    version = "1.0.23"

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

    publishing {
        repositories {
            mavenLocal()
            maven {
                name = "entixReposilite"
                url = uri("https://repo.entix.eu/snapshots")
                credentials {
                    username = System.getenv("ENTIX_REPO_USER")
                    password = System.getenv("ENTIX_REPO_PASS")
                }
            }
        }
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