plugins {
    id("java")
    id("com.vanniktech.maven.publish") version("0.35.0")
}

dependencies {
    implementation("org.json:json:20251224")
}

mavenPublishing {
    mavenPublishing {
        coordinates(
            project.group.toString(),
            project.name.toString(),
            project.version.toString()
        )

        val githubDomainPath = findProperty("githubDomainPath") as String
        val githubUrl = findProperty("githubUrl") as String
        val githubUserUrl = findProperty("githubUserUrl") as String
        val authorName = findProperty("authorName") as String
        val authorId = findProperty("authorId") as String
        val licenseUrl = findProperty("licenseUrl") as String
        val licenseName = findProperty("licenseName") as String
        val projectDescription = findProperty("projectDescription") as String
        val projectDisplayName = findProperty("projectDisplayName") as String

        pom {
            name.set(projectDisplayName)
            description.set("AnnotationProcessor $projectDescription")
            inceptionYear.set("2026")
            url.set(githubUrl)
            licenses {
                license {
                    name.set(licenseName)
                    url.set(licenseUrl)
                    distribution.set(licenseUrl)
                }
            }
            developers {
                developer {
                    id.set(authorId)
                    name.set(authorName)
                    url.set(githubUserUrl)
                }
            }
            scm {
                url.set(githubUrl)
                connection.set("scm:git:git:$githubDomainPath.git")
                developerConnection.set("scm:git:ssh://git@$githubDomainPath.git")
            }
        }
    }
    publishToMavenCentral()
    signAllPublications()
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
}