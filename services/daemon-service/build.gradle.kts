plugins { alias(libs.plugins.agp.lib) }

android {
    buildFeatures { aidl = true }

    buildTypes { release { isMinifyEnabled = false } }

    sourceSets {
        named("main") {
            java.srcDirs("src/main/java", "../libxposed/service/src/main")
            aidl.srcDirs("src/main/aidl", "../libxposed/interface/src/main/aidl")
        }
    }

    aidlPackagedList += "org/ahmoze/vector/lspd/models/Module.aidl"
    namespace = "org.ahmoze.vector.lspd.daemonservice"
}

dependencies {
    compileOnly(libs.androidx.annotation)
    compileOnly(projects.shared.libxposedAnnotation)
    compileOnly(projects.hiddenapi.stubs)
}
