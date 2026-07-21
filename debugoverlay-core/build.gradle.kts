plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.dexcount)
  alias(libs.plugins.mavenPublish)
  id("jacoco")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

kotlin {
  explicitApi()
}

android {
  namespace = "com.ms.square.debugoverlay.core"

  compileSdk = libs.versions.androidCompileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.androidMinSdk.get().toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  testOptions {
    targetSdk = libs.versions.androidTargetSdk.get().toInt()
    // Enable Android resources in unit tests so Robolectric can access R.*
    unitTests.isIncludeAndroidResources = true
  }

  // force usage of prefix to avoid naming conflicts
  resourcePrefix = "debugoverlay_"

  buildFeatures {
    compose = true
    buildConfig = true
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
}

dependencies {
  implementation(libs.androidx.core)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.annotation)
  implementation(libs.material)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.ui)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.material3.windowsizeclass)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.extended)

  debugImplementation(libs.androidx.compose.ui.tooling)

  // Json
  implementation(libs.kotlinx.serialization.json)

  // View hierarchy inspection
  implementation(libs.radiography)
  // Used for app window overlay management, radiography lib depends on this as well
  implementation(libs.curtains)

  // Frame performance monitoring
  implementation(libs.androidx.metrics.performance)

  // Lifecycle for synthetic lifecycle owner
  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.savedstate)

  testImplementation(libs.junit4)
  testImplementation(libs.truth)
  testImplementation(libs.mockk)
  testImplementation(libs.turbine)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
}
