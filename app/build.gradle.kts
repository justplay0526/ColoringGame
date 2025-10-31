plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kapt)
    alias(libs.plugins.navigation.safeArgs)
}

android {
    namespace = "com.justplay.coloringgame"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.justplay.coloringgame"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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
}

dependencies {
    implementation(libs.bundles.androidx)

    implementation(libs.material)
    implementation(libs.androidx.room)
    implementation(libs.glide)
    implementation(libs.okhttp)
    implementation(libs.coroutines.play.services)


    testImplementation(libs.junit)
    androidTestImplementation(libs.bundles.test)

    kapt(libs.bundles.compiler)
}