import java.util.Properties

// The Amplitude API key is not in git. It lives in android/api-key.properties
// and reaches the code as BuildConfig.AMPLITUDE_API_KEY.
val apiKeys = Properties().apply {
    val file = rootProject.file("api-key.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "de.angebote.trackingtest"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.angebote.trackingtest"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "AMPLITUDE_API_KEY",
            "\"${apiKeys.getProperty("AMPLITUDE_API_KEY", "")}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        compose = true
        buildConfig = true   // off by default since AGP 8
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.amplitude.analytics)
    implementation(libs.amplitude.session.replay)
}
