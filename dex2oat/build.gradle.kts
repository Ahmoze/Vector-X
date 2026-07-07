plugins { alias(libs.plugins.agp.lib) }

android {
    namespace = "org.ahmoze.vector.dex2oat"

    androidResources { enable = false }

    externalNativeBuild { cmake { path("src/main/cpp/CMakeLists.txt") } }
}
