plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.dexcount)
  alias(libs.plugins.mavenPublish)
  id("kotlin-parcelize")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

android {
  namespace = "com.ms.square.debugoverlay"

  compileSdk {
    version = release(rootProject.extra["compileSdkVersion"] as Int)
  }

  defaultConfig {
    minSdk = rootProject.extra["minSdkVersion"] as Int

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  testOptions {
    targetSdk = rootProject.extra["targetSdkVersion"] as Int
    // Enable Android resources in unit tests so Robolectric can access R.*
    unitTests.isIncludeAndroidResources = true
  }

  // force usage of prefix to avoid naming conflicts
  resourcePrefix = "debugoverlay_"

  buildFeatures {
    compose = true
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
  implementation(libs.androidx.localbroadcastmanager)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.ui)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.ui.tooling.preview)

  debugImplementation(libs.androidx.compose.ui.tooling)

  // Lifecycle for synthetic lifecycle owner
  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.savedstate)

  implementation(libs.kotlinx.collections.immutable)
}
