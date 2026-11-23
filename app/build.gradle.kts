plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.blankpages.trackify"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.blankpages.trackify"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                // R8 > Avoid packageScope: limited scope produced an APK that exited
                // instantly. Default R8 gives a verified ~0.4 s cold start.
            }
        }

        // Baseline Profile > Pre-create this variant; AGP 9 copies optimization
        // from release via initWith, so disable it explicitly here.
        create("nonMinifiedRelease") {
            initWith(getByName("release"))
            isDebuggable = false
            isProfileable = true
            isMinifyEnabled = false
            isShrinkResources = false
            optimization {
                enable = false
            }
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

baselineProfile {
    saveInSrc = true
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    // Profile installer > Applies the bundled profile on local installs.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Modules > Internal modules
    implementation(project(":core:domain"))
    implementation(project(":core:data"))

    // DI > Hilt
    val hiltVersion = "2.60.1"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
}
