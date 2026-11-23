plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.blankpages.trackify.domain"
    compileSdk {
        version = release(37)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Room annotations > Model annotations only, not the Room implementation.
    implementation("androidx.room:room-common:2.7.1")
}
