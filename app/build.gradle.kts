import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

// ---------------------------------------------------------------------------
// M-Pesa (Safaricom Daraja) credentials.
//
// Read from local.properties, which is gitignored and therefore never committed.
// See local.properties for the required keys. If they are missing the build still
// succeeds, but the payment screen reports that M-Pesa is not configured.
// ---------------------------------------------------------------------------
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun localProp(key: String, fallback: String = ""): String =
    (localProperties.getProperty(key) ?: fallback).trim()

android {
    namespace = "com.example.student_registration"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.student_registration"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Injected into BuildConfig so Java code can read the M-Pesa settings.
        buildConfigField("String", "MPESA_ENV",
            "\"${localProp("MPESA_ENV", "sandbox")}\"")
        buildConfigField("String", "MPESA_CONSUMER_KEY",
            "\"${localProp("MPESA_CONSUMER_KEY")}\"")
        buildConfigField("String", "MPESA_CONSUMER_SECRET",
            "\"${localProp("MPESA_CONSUMER_SECRET")}\"")
        buildConfigField("String", "MPESA_SHORTCODE",
            "\"${localProp("MPESA_SHORTCODE", "174379")}\"")
        buildConfigField("String", "MPESA_PASSKEY",
            "\"${localProp("MPESA_PASSKEY")}\"")
        buildConfigField("String", "MPESA_TRANSACTION_TYPE",
            "\"${localProp("MPESA_TRANSACTION_TYPE", "CustomerPayBillOnline")}\"")
        buildConfigField("String", "MPESA_PARTY_B",
            "\"${localProp("MPESA_PARTY_B", localProp("MPESA_SHORTCODE", "174379"))}\"")
        buildConfigField("String", "MPESA_CALLBACK_URL",
            "\"${localProp("MPESA_CALLBACK_URL", "https://example.com/mpesa/callback")}\"")
    }

    buildFeatures {
        // Required on AGP 8+: BuildConfig generation is off by default.
        buildConfig = true
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
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation(libs.firebase.firestore)

    // HTTP client for the M-Pesa (Daraja) STK Push API
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
