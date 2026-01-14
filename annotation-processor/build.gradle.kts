plugins {
    id("java-library")
    publish
}

dependencies {
    api(project(":manifest-api"))
    implementation("org.json:json:20251224")
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
}