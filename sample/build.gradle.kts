import java.util.Properties

plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

fun gitHash(): String = Runtime
  .getRuntime()
  .exec("git rev-parse --short HEAD")
  .inputStream
  .bufferedReader()
  .readText()
  .trim()

android {
  namespace = "com.ms.square.debugoverlay.sample"
  compileSdk = rootProject.extra["compileSdkVersion"] as Int

  // to allow buildConfigField usage within defaultConfig
  buildFeatures.buildConfig = true

  defaultConfig {
    applicationId = "com.ms.square.debugoverlay.sample"
    minSdk = rootProject.extra["minSdkVersion"] as Int
    targetSdk = rootProject.extra["targetSdkVersion"] as Int

    versionCode = 1
    versionName = "1.0.0"

    buildConfigField("String", "GIT_HASH", "\"${gitHash()}\"")

    testInstrumentationRunner = "com.ms.square.debugoverlay.DebugOverlayTestRunner"
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
    release {
      // Enables code-related app optimization.
      isMinifyEnabled = true
      // Enables resource shrinking.
      isShrinkResources = true
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
  implementation(libs.androidx.annotation)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.material.icons.extended)
  implementation(libs.androidx.activity.compose)

  // Navigation 3
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  implementation(libs.kotlinx.serialization.core)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)

  // HTTP Client
  implementation(libs.okhttp)

  // Image Loading
  implementation(libs.coil)

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
