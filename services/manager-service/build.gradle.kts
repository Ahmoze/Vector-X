plugins { alias(libs.plugins.agp.lib) }

android {
    buildFeatures { aidl = true }

    buildTypes { release { isMinifyEnabled = false } }

    namespace = "org.ahmoze.vector.lspd.managerservice"
}

dependencies { api(libs.rikkax.parcelablelist) }
