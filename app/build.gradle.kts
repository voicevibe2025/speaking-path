import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.voicevibe"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.voicevibe"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
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
    // Signing config from keystore.properties (optional)
    val keystoreProps = Properties()
    val keystorePropsFile = rootProject.file("keystore.properties")
    if (keystorePropsFile.exists()) {
        try {
            keystoreProps.load(FileInputStream(keystorePropsFile))
            signingConfigs {
                create("release") {
                    storeFile = rootProject.file(keystoreProps["storeFile"] as String)
                    storePassword = keystoreProps["storePassword"] as String
                    keyAlias = keystoreProps["keyAlias"] as String
                    keyPassword = keystoreProps["keyPassword"] as String
                }
            }
            buildTypes.getByName("release") {
                signingConfig = signingConfigs.getByName("release")
            }
            logger.lifecycle("Loaded release signing config from keystore.properties")
        } catch (e: Exception) {
            logger.warn("Failed to load keystore.properties: ${e.message}")
        }
    } else {
        logger.lifecycle("keystore.properties not found at project root. Release build will be unsigned.")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Enable desugaring so java.time works on API < 26
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Read GOOGLE_API_KEY from django/.env file
    val googleApiKey: String = try {
        // Path from app/build.gradle.kts to django/.env
        val envFile = rootProject.file("../django/.env")
        val properties = Properties()
        properties.load(FileInputStream(envFile))
        properties.getProperty("GOOGLE_API_KEY", "")
    } catch (e: Exception) {
        // Fallback to environment variable if file not found or error
        System.getenv("GOOGLE_API_KEY") ?: ""
    }

    // Read FIREBASE_WEB_CLIENT_ID from django/.env (or env var)
    val firebaseWebClientId: String = try {
        val envFile = rootProject.file("../django/.env")
        val properties = Properties()
        properties.load(FileInputStream(envFile))
        properties.getProperty("FIREBASE_WEB_CLIENT_ID", "")
    } catch (e: Exception) {
        System.getenv("FIREBASE_WEB_CLIENT_ID") ?: ""
    }

    buildTypes.forEach {
        it.buildConfigField("String", "GEMINI_API_KEY", "\"$googleApiKey\"")
        it.buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$firebaseWebClientId\"")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
    jvmToolchain(17)
}

dependencies {
    // Core library desugaring for java.time on older Android versions
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    // Core Android & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // WebSocket
    implementation(libs.java.websocket)

    // Coroutines
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)

    // Image Loading
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Audio Recording
    implementation(libs.ffmpeg.audio)
    implementation(libs.wave.recorder)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Charts
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)

    // Lottie
    implementation(libs.lottie.compose)

    // Date & Time
    implementation(libs.kotlinx.datetime)

    // Splash Screen
    implementation(libs.androidx.splashscreen)

    // CameraX
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Biometric
    implementation(libs.androidx.biometric)

    // WorkManager
    implementation(libs.androidx.work)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // Security
    implementation(libs.security.crypto)

    // Timber
    implementation(libs.timber)

    // Gemini
    implementation(libs.gemini.generativeai)

    // Firebase Auth + Google Sign-In
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.mockk)
    androidTestImplementation(libs.coroutines.test)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.leakcanary)
}

// Apply Google Services plugin only if google-services.json is present
val hasGoogleServicesJson = file("google-services.json").exists()
if (hasGoogleServicesJson) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.lifecycle("google-services.json not found in app/. Skipping Google Services plugin.")
}
