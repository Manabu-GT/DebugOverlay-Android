plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.mavenPublish)
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

android {
  namespace = "com.ms_square.debugoverlay_ext_netstats"
  compileSdk = rootProject.extra["compileSdkVersion"] as Int

  defaultConfig {
    minSdk = rootProject.extra["minSdkVersion"] as Int
  }

  testOptions {
    targetSdk = rootProject.extra["targetSdkVersion"] as Int
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
}

dependencies {
  api(project(":debugoverlay"))
  testImplementation(libs.junit4)
}
