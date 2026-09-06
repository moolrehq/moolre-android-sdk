plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish` // ✅ Required for publishing
}

android {
    namespace = "com.moolre.sdk.views"
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

    buildFeatures { viewBinding = true }
}

dependencies {
    api(project(":moolre-core"))
    api(project(":moolre-checkout-android"))
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Keep the XML adapter aligned with the checkout runtime's cached AndroidX
// artifacts. This rule is local to the adapter and is not published.
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

// Publishing Configuration
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.moolre"
            artifactId = "android-sdk-views"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
