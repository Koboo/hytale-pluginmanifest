plugins {
    id("maven-publish")
    id("signing")
}

publishing {
    repositories {
        mavenLocal()
        maven {
            name = "entixReposilite"
            val repository = "snapshots"
//            val repository =
//                if (isSnapshot) {
//                    "snapshots"
//                } else {
//                    "releases"
//                }
            url = uri("https://repo.entix.eu/$repository")
            credentials {
                username = System.getenv("ENTIX_REPO_USER")
                password = System.getenv("ENTIX_REPO_PASS")
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                artifactId = project.name
                groupId = project.group.toString()
                version = project.version.toString()
                name.set("pluginmanifest")
                description.set("API for creating and validating a Hytale Plugin manifest.json")
                url.set("https://github.com/Koboo/hytale-pluginmanifest")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://mit-license.org/")
                    }
                }

                developers {
                    developer {
                        id.set("koboo")
                        name.set("Koboo")
                        email.set("admin@koboo.eu")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/Koboo/hytale-pluginmanifest")
                    developerConnection.set("scm:git:https://github.com/Koboo/hytale-pluginmanifest")
                    url.set("https://github.com/Koboo/hytale-pluginmanifest")
                }
            }
        }
    }

    signing {
        val signingKeyId = findProperty("signing.keyId") as String?
        val signingKeyPassword = findProperty("signing.password") as String?
        val signingSecretKeyFile = findProperty("signing.secretKeyASC") as String?

        useInMemoryPgpKeys(
            signingKeyId,
            file(signingSecretKeyFile!!).readText(),
            signingKeyPassword,
        )

        sign(publishing.publications["mavenJava"])
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    // all publishing tasks must run after signing
    mustRunAfter(tasks.withType<Sign>())
}