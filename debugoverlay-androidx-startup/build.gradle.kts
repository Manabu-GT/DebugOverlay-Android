plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlin.android)
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
  }

  testOptions {
    targetSdk = rootProject.extra["targetSdkVersion"] as Int
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
}

dependencies {
  api(project(":debugoverlay-core"))
  implementation(libs.androidx.startup.runtime)
  testImplementation(libs.junit4)
}
