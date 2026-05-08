import java.util.Properties

plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt.android)
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

android {
  namespace = "com.ms.square.debugoverlay.sample"
  compileSdk = libs.versions.androidCompileSdk.get().toInt()

  defaultConfig {
    applicationId = "com.ms.square.debugoverlay.sample"
    minSdk = libs.versions.androidMinSdk.get().toInt()
    targetSdk = libs.versions.androidTargetSdk.get().toInt()

    versionCode = 1
    versionName = "1.0.0"
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
    create("releaseWithOverlay") {
      initWith(getByName("release"))
      matchingFallbacks += "release"
      applicationIdSuffix = ".internal"
      versionNameSuffix = "-withOverlay"
    }
  }

  buildFeatures {
    compose = true
    // to allow buildConfigField usage within defaultConfig
    buildConfig = true
  }

  sourceSets {
    // Shared source set for builds that include DebugOverlay (debug + releaseWithOverlay)
    named("debug") {
      kotlin.srcDirs("src/debugOverlay/kotlin")
    }
    named("releaseWithOverlay") {
      kotlin.srcDirs("src/debugOverlay/kotlin")
    }
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
  debugImplementation(project(":debugoverlay-extension-okhttp"))
  debugImplementation(project(":debugoverlay-extension-timber"))
  debugImplementation(project(":debugoverlay-extension-trigger-shake"))
  "releaseWithOverlayImplementation"(project(":debugoverlay"))
  "releaseWithOverlayImplementation"(project(":debugoverlay-extension-okhttp"))
  "releaseWithOverlayImplementation"(project(":debugoverlay-extension-timber"))
  "releaseWithOverlayImplementation"(project(":debugoverlay-extension-trigger-shake"))
  implementation(libs.androidx.core)
  implementation(libs.androidx.annotation)
  implementation(libs.material)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.activity.compose)

  // Navigation 3
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  implementation(libs.kotlinx.serialization.core)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)

  // Hilt
  implementation(libs.hilt.android)
  implementation(libs.hilt.navigation.compose)
  ksp(libs.hilt.android.ksp)

  // Frame performance monitoring
  implementation(libs.androidx.metrics.performance)

  // HTTP Client
  implementation(libs.okhttp)

  // Image Loading
  implementation(libs.coil)

  implementation(libs.timber)

  // LeakCanary
  debugImplementation(libs.leakcanary.android)

  testImplementation(libs.junit4)
}

// Configure all JavaCompile tasks (including Hilt-generated) to use Java 21 toolchain
// Workaround as the Hilt Gradle plugin creates its own JavaCompile tasks that don't inherit the project toolchain
tasks.withType<JavaCompile>().configureEach {
  javaCompiler.set(
    javaToolchains.compilerFor {
      languageVersion.set(JavaLanguageVersion.of(21))
    }
  )
}
