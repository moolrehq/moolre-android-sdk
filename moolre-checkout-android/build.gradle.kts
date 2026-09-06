plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.moolre.sdk.checkout"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        viewBinding = true
    }
}

dependencies {
    api(project(":moolre-core"))
    api(libs.androidx.appcompat)
    api(libs.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Keep the checkout runtime buildable with the Android artifacts available in
// the repository cache; published consumers retain the normal module graph.
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

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.moolre"
            artifactId = "android-sdk-checkout"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
