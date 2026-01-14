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
  namespace = "com.ms.square.debugoverlay"
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
  api(projects.debugoverlayCore)
  implementation(libs.androidx.startup.runtime)
  testImplementation(libs.junit4)
}
