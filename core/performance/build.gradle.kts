plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.blankpages.trackify.performance"
    compileSdk {
        version = release(37)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:domain"))
    testImplementation(libs.junit)
}
