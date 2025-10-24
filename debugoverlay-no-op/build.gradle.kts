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
    }

    // force usage of prefix to avoid naming conflicts,
    resourcePrefix = "debugoverlay_"

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    testImplementation(libs.junit4)
    testImplementation(libs.mockito.core)
}

// for maven central deployment
//apply from: 'https://raw.githubusercontent.com/chrisbanes/gradle-mvn-push/master/gradle-mvn-push.gradle'
