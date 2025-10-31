plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.dexcount)
  alias(libs.plugins.mavenPublish)
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

  buildTypes {
    release {
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
