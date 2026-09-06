plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "com.moolre.sdk.compose"
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
        compose = true
    }
}

dependencies {
    // Core models and results appear in the public Compose API.
    api(project(":moolre-core"))
    implementation(project(":moolre-checkout-android"))

    // Modifier and Color appear in the public composable signature.
    api(libs.androidx.ui) {
        exclude(group = "androidx.lifecycle", module = "lifecycle-runtime-compose")
    }

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation.android)
    implementation(libs.androidx.compose.foundation.layout.android)
    implementation(libs.androidx.lifecycle.runtime.compose.android)
    implementation(libs.androidx.material3) {
        exclude(group = "androidx.compose.material", module = "material-icons-core")
        exclude(group = "androidx.compose.material", module = "material-ripple")
    }
    implementation(libs.androidx.compose.material.icons.core.android)
    implementation(libs.androidx.compose.material.ripple.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
}

// Material 3 1.3.2 publishes these dependencies under their multiplatform
// coordinates. The Android variants are the artifacts available to Android
// consumers and keep this module buildable with the checked-in dependency
// cache. The substitution is scoped to this Compose artifact only.
configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("androidx.compose.material:material-icons-core"))
            .using(module("androidx.compose.material:material-icons-core-android:1.7.8"))
        substitute(module("androidx.compose.material:material-ripple"))
            .using(module("androidx.compose.material:material-ripple-android:1.8.3"))
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.moolre"
            artifactId = "android-sdk-compose"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
