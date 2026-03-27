pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}
// Required by Gradle 9+ for JDK auto-provisioning (downloads the correct JDK when not locally available)
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

// Although default in Gradle 9, this is still needed with android.newDsl=false
// to generate typesafe project accessors compatible with the old DSL.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "DebugOverlay-Android"

include(":sample")
include(":debugoverlay", ":debugoverlay-core", ":debugoverlay-extension-okhttp", ":debugoverlay-extension-timber")
