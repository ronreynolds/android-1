import java.util.Properties

val signingProps = Properties().apply {
    val file = rootProject.file("app/signing.properties")
    if (file.exists()) {
        load(file.inputStream())
    } else {
        throw GradleException("Missing signing.properties file")
    }
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.ronreynolds.android.clock"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ronreynolds.android.clock"
        minSdk = 23 // ZTE Z981 (Android 6.0); Samsung S21+ is API-35 (Android 15.0)
        targetSdk = 37
        versionCode = 1
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        create("release") {
            storeFile = file(signingProps["storeFile"]!!)
            storePassword = signingProps["storePassword"]!!.toString()
            keyAlias = signingProps["keyAlias"]!!.toString()
            keyPassword = signingProps["keyPassword"]!!.toString()
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.assertj)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}