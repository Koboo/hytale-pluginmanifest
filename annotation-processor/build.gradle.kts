plugins {
    id("java")
    id("com.gradleup.shadow") version ("9.3.1")
}

dependencies {
    implementation(project(":manifest-api"))
    implementation("org.json:json:20251224")
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}