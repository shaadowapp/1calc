import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

// Load MATHLY_API_KEY from local.properties if not present as a project property
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
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shaadow.onecalculator"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "MATHLY_API_KEY", '"' + (mathlyApiKey ?: "") + '"')
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
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.rhino.android)
    implementation(libs.androidx.viewpager2)
    implementation(libs.material)
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
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
}

configurations.all {
    resolutionStrategy {
        force("androidx.transition:transition:1.6.0")
    }
}