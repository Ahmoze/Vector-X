val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra

plugins {
    id("java-library")
}

java {
    sourceCompatibility = androidSourceCompatibility
    targetCompatibility = androidTargetCompatibility
    sourceSets {
        main {
            java.setSrcDirs(listOf("manifest-editor/lib/src/main/java"))
            resources.setSrcDirs(emptyList<String>())
        }
    }
}

tasks.processResources {
    from("manifest-editor/lib/src/main/assets") {
        into("assets")
    }
}
