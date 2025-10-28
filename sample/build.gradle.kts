import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

fun gitHash(): String {
    return Runtime.getRuntime().exec("git rev-parse --short HEAD").inputStream.bufferedReader().readText().trim()
}

android {
    namespace = "com.ms_square.debugoverlay.sample"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    // to allow buildConfigField usage within defaultConfig
    buildFeatures.buildConfig = true

    defaultConfig {
        applicationId = "com.ms_square.debugoverlay.sample"
        minSdk = rootProject.extra["minSdkVersion"] as Int
        targetSdk = rootProject.extra["targetSdkVersion"] as Int

        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "GIT_HASH", "\"${gitHash()}\"")

        testInstrumentationRunner = "com.ms_square.debugoverlay.DebugOverlayTestRunner"
    }

    signingConfigs {
        create("release") {
            val keyProps = Properties()
            // double check if keystore.properties exists to avoid exception
            if (file("../keystore.properties").exists()) {
                keyProps.load(file("../keystore.properties").inputStream())
            }
            storeFile = if (keyProps["storeFile"] != null) file(keyProps["storeFile"] as String) else null
            storePassword = keyProps["storePassword"] as? String
            keyAlias = keyProps["keyAlias"] as? String
            keyPassword = keyProps["keyPassword"] as? String
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // check if keystore.properties exists in the root
            if (file("../keystore.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }
}

// Enables outputting the results of its stability inference for inspection.
// Run ./gradlew :sample:assembleRelease -PcomposeCompilerReports=true --rerun-tasks
// to force running compose compiler reports.
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}

dependencies {
    debugImplementation(project(":debugoverlay"))
    releaseImplementation(project(":debugoverlay-no-op"))
    testImplementation(project(":debugoverlay-no-op"))

    implementation(project(":debugoverlay-ext-timber")) {
        exclude(module = "debugoverlay")
    }

    implementation(project(":debugoverlay-ext-netstats")) {
        exclude(module = "debugoverlay")
    }

    implementation(libs.androidx.core)
    implementation(libs.google.material)
    implementation(libs.androidx.annotation)

    implementation(libs.androidx.constraintlayout)

    implementation(libs.timber)

    // LeakCanary
    debugImplementation(libs.leakcanary.android)

    // Set this dependency to build and run Espresso tests
    androidTestImplementation("com.android.support.test.espresso:espresso-core:2.2.2") {
        exclude(group = "com.android.support", module = "support-annotations")
    }

    // ScreenShot taker for instrumentation tests
    androidTestImplementation(files("libs/cloudtestingscreenshotter_lib.aar"))

    testImplementation(libs.junit4)
}
