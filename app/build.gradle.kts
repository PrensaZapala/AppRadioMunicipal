

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.muni.radiomunicipalzapala"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.muni.radiomunicipalzapala"
        minSdk = 24
        targetSdk = 35
        versionCode = 14
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Habilita la ofuscación y optimización
            isMinifyEnabled = true
            // Añade las reglas de ProGuard/R8
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Si utilizas código nativo, genera los símbolos de depuración
            ndk {
                debugSymbolLevel = "FULL"  // Puede ser 'SYMBOL_TABLE' o 'FULL'
            }
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
    // Core library.
    implementation(libs.androidx.leanback)

    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.media3.exoplayer)
    //UI library
    implementation(libs.androidx.media3.ui)
    //DASH support library.
    implementation(libs.androidx.media3.exoplayer.dash)
    //HLS support library.
    implementation(libs.androidx.media3.exoplayer.hls)
    // ... other dependencies
    implementation(libs.androidx.appcompat)
    implementation(libs.glide)
    implementation(libs.androidx.constraintlayout)
    annotationProcessor(libs.compiler)
    implementation(libs.lottie)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}


