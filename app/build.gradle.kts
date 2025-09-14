import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

// Load MATHLY_API_KEY from local.properties securely
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val mathlyApiKey: String? = (project.findProperty("MATHLY_API_KEY") as String?)
    ?: localProperties.getProperty("MATHLY_API_KEY")

kapt {
    correctErrorTypes = true
    useBuildCache = true
}

android {
    namespace = "com.shaadow.onecalculator"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.shaadow.onecalculator"
        minSdk = 21  // Reduced from 31 to 21 for broader compatibility (Android 5.0+)
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "MATHLY_API_KEY", '"' + (mathlyApiKey ?: "") + '"')
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.rhino.android)
    implementation(libs.androidx.viewpager2)
    implementation(libs.flexbox)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.androidx.transition)
    implementation(libs.androidx.material3.window.size.class1)
    implementation(libs.androidx.window)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.vosk.android)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.okhttp)
    implementation("io.noties.markwon:core:4.6.2") {
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }
    implementation("io.noties.markwon:ext-latex:4.6.2") {
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }
    implementation("io.noties.markwon:syntax-highlight:4.6.2") {
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }

    // Google ML Kit for scanner functionality
    implementation(libs.text.recognition)
    implementation(libs.play.services.mlkit.text.recognition)

    // Additional Google Play Services
    implementation(libs.play.services.tasks)

    // For ListenableFuture in CameraX
    implementation(libs.guava)

    // Coil for image loading
    implementation("io.coil-kt:coil:2.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Media3 (ExoPlayer) for video and audio playback
    val media3Version = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    // PDF Viewer - Using WebView approach instead of AndroidX PDF viewer

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Zip4j for password-protected ZIP files
    implementation("net.lingala.zip4j:zip4j:2.11.5")

}

configurations.all {
    resolutionStrategy {
        force("androidx.transition:transition:1.6.0")
    }
}