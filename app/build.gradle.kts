plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.healthieru"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.healthieru"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // ADDED: lets screens access views without findViewById
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Fragment
    implementation(libs.androidx.fragment.ktx)

    // Room (database)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Needed for lifecycleScope and repeatOnLifecycle in the screens
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // WorkManager (reminders)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}