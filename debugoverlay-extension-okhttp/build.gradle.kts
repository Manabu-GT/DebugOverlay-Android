plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.dexcount)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.bcv)
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

kotlin {
  explicitApi()
}

apiValidation {
  nonPublicMarkers.add("com.ms.square.debugoverlay.internal.InternalDebugOverlayApi")
}

android {
  namespace = "com.ms.square.debugoverlay.extension.okhttp"

  compileSdk = libs.versions.androidCompileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.androidMinSdk.get().toInt()
  }

  testOptions {
    targetSdk = libs.versions.androidTargetSdk.get().toInt()
  }

  // force usage of prefix to avoid naming conflicts
  resourcePrefix = "debugoverlay_"

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
}

dependencies {
  implementation(projects.debugoverlayCore)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp)

  testImplementation(libs.junit4)
  testImplementation(libs.truth)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.robolectric)
}
