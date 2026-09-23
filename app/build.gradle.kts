import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.androidx.navigation.safeargs.kotlin)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun localProperty(name: String, default: String): String =
    localProperties.getProperty(name, default)

fun String.toBuildConfigLiteral(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.moolre.moolre_android_sdk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.moolre.moolre_android_sdk"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "MOOLRE_ENVIRONMENT",
            "\"${localProperty("moolre.environment", "SANDBOX").toBuildConfigLiteral()}\""
        )
        buildConfigField(
            "String",
            "MOOLRE_API_USER",
            "\"${localProperty("moolre.apiUser", "replace-with-your-moolre-username").toBuildConfigLiteral()}\""
        )
        buildConfigField(
            "String",
            "MOOLRE_PUBLIC_KEY",
            "\"${localProperty("moolre.publicKey", "replace-with-your-sandbox-public-key").toBuildConfigLiteral()}\""
        )
        buildConfigField(
            "String",
            "MOOLRE_ACCOUNT_NUMBER",
            "\"${localProperty("moolre.accountNumber", "replace-with-your-sandbox-account-number").toBuildConfigLiteral()}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Must match the redirectUrl ("moolre-compose://payment-callback") passed to the
        // Compose payment launcher in MainActivity, so MoolreCheckoutActivity's intent-filter
        // catches the Custom Tabs redirect back into the app. Required even when using the
        // SDK's default redirectUrl - see moolre-checkout-android/README.md.
        manifestPlaceholders["moolreRedirectScheme"] = "moolre-compose"
        manifestPlaceholders["moolreRedirectHost"] = "payment-callback"
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
        buildConfig = true
    }
}

dependencies {
    implementation(project(":moolre-sdk-compose"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui) {
        exclude(group = "androidx.lifecycle", module = "lifecycle-runtime-compose")
    }
    implementation(libs.androidx.compose.foundation.android)
    implementation(libs.androidx.compose.foundation.layout.android)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.lifecycle.runtime.compose.android)
    implementation(libs.androidx.material3) {
        exclude(group = "androidx.compose.material", module = "material-icons-core")
        exclude(group = "androidx.compose.material", module = "material-ripple")
    }
    implementation(libs.androidx.compose.material.icons.core.android)
    implementation(libs.androidx.compose.material.ripple.android)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Keep the app's Compose dependency graph aligned with the Android artifacts
// used by the SDK Compose module. This is intentionally module-local rather
// than a project-wide resolution rule.
configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("androidx.compose.material:material-icons-core"))
            .using(module("androidx.compose.material:material-icons-core-android:1.7.8"))
        substitute(module("androidx.compose.material:material-ripple"))
            .using(module("androidx.compose.material:material-ripple-android:1.8.3"))
    }
}
