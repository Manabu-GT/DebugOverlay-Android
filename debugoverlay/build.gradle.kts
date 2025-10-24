plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dexcount)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

android {
    namespace = "com.ms_square.debugoverlay"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int
    }

    testOptions {
        targetSdk = rootProject.extra["targetSdkVersion"] as Int
        // Enable Android resources in unit tests so Robolectric can access R.*
        unitTests.isIncludeAndroidResources = true
    }

    // force usage of prefix to avoid naming conflicts
    resourcePrefix = "debugoverlay_"

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.androidx.core)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.localbroadcastmanager)
}

// for maven central deployment
//apply from: 'https://raw.githubusercontent.com/chrisbanes/gradle-mvn-push/master/gradle-mvn-push.gradle'
