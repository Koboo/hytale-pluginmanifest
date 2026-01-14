import java.net.URI

plugins {
    id("java")
    id("maven-publish")
}

subprojects {
    apply {
        plugin("java")
        plugin("maven-publish")
    }

    group = "eu.koboo.pluginmanifest"
    version = "1.0.22-ALPHA"

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

    publishing {
        repositories {
            mavenLocal()
            maven {
                name = "entixReposilite"
                var repositoryByVersion =
                    if (!project.version.toString().contains("-")) {
                        "releases";
                    } else {
                        "snapshots";
                    }
                url = URI.create("https://repo.entix.eu/$repositoryByVersion")
                credentials {
                    username = System.getenv("ENTIX_REPO_USER")
                    password = System.getenv("ENTIX_REPO_PASS")
                }
            }
        }
//        // GradlePlugin has custom publishing
//        if(project.name.toString() != "gradle-plugin") {
//            publications {
//                create<MavenPublication>("maven") {
//                    from(components["java"])
//                }
//            }
//        }
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