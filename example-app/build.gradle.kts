plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.navigation.safeargs.kotlin)
}

android {
    namespace = "com.moolre.example" // Keep your preferred namespace
    compileSdk = 35 // Use the higher version

    defaultConfig {
        applicationId = "com.moolre.example" // Keep your preferred applicationId
        minSdk = 24
        targetSdk = 35 // Use the higher version
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true // Add viewBinding for traditional views
    }
}

dependencies {
    // Import Moolre SDK module
    implementation(project(":moolre-views"))

    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat) // Added from second config

    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Traditional View System
    implementation(libs.material) // Material components for views
    implementation(libs.androidx.navigation.fragment) // Navigation component
    implementation(libs.androidx.navigation.ui)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// The local AndroidX cache contains Activity 1.8.1 but not the 1.8.0 KTX
// artifact requested by AppCompat/Navigation's atomic group. Keep this
// compatibility mapping local to the XML sample; published consumers resolve
// the normal upstream coordinates.
configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("androidx.activity:activity:1.8.0"))
            .using(module("androidx.activity:activity:1.8.1"))
        substitute(module("androidx.activity:activity-ktx:1.8.0"))
            .using(module("androidx.activity:activity-ktx:1.8.1"))
        substitute(module("androidx.collection:collection-ktx:1.4.2"))
            .using(module("androidx.collection:collection-ktx:1.5.0"))
    }
}
