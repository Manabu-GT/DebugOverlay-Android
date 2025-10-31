import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
  /**
   * Use `apply false` in the top-level build.gradle file to add a Gradle
   * plugin as a build dependency but not apply it to the current (root)
   * project. Don't use `apply false` in sub-projects. For more information,
   * see Applying external plugins with same version to subprojects.
   */
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.androidLibrary) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.dexcount) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.detekt) apply false
  alias(libs.plugins.spotless)
}

// http://www.gradle.org/docs/current/dsl/org.gradle.api.plugins.ExtraPropertiesExtension.html
extra.apply {
  set("compileSdkVersion", 36)
  set("minSdkVersion", 26)
  set("targetSdkVersion", 36)
}

val reportMerge by tasks.registering(io.gitlab.arturbosch.detekt.report.ReportMergeTask::class) {
  output.set(rootProject.layout.buildDirectory.file("reports/detekt/merge.sarif"))
}

subprojects {
  // Apply only to modules that actually use Kotlin
  plugins.withId("org.jetbrains.kotlin.jvm") { apply(plugin = "io.gitlab.arturbosch.detekt") }
  plugins.withId("org.jetbrains.kotlin.android") { apply(plugin = "io.gitlab.arturbosch.detekt") }

  // Configure Detekt when present
  plugins.withId("io.gitlab.arturbosch.detekt") {
    extensions.configure<DetektExtension> {
      // Good defaults + your overrides
      buildUponDefaultConfig = true
      allRules = false
      config.from(rootProject.files("config/detekt/detekt.yml"))

      basePath = rootProject.projectDir.absolutePath

      // Limit to real sources for speed
      source.setFrom(
        files(
          "src/main/java",
          "src/main/kotlin",
          "src/test/java",
          "src/test/kotlin",
          "src/androidTest/java",
          "src/androidTest/kotlin"
        )
      )
    }

    // Configure SARIF reports for GitHub Code Scanning
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
      reports {
        xml.required.set(true)
        html.required.set(true)
        txt.required.set(false)
        sarif.required.set(true)
        md.required.set(false)
      }

      // Make reportMerge depend on this detekt task
      reportMerge.configure {
        input.from(sarifReportFile)
        mustRunAfter(this@configureEach)
      }
    }
  }

  fun hookCheckWhen(pluginId: String, taskName: String) {
    pluginManager.withPlugin(pluginId) {
      tasks.named("check").configure { dependsOn(taskName) }
    }
  }

  // let check also run spotlessCheck + detekt if those plugins exist
  plugins.withId("com.android.library") {
    hookCheckWhen("io.gitlab.arturbosch.detekt", "detekt")
    hookCheckWhen("com.diffplug.spotless", "spotlessCheck")
  }
  plugins.withId("com.android.application") {
    hookCheckWhen("io.gitlab.arturbosch.detekt", "detekt")
    hookCheckWhen("com.diffplug.spotless", "spotlessCheck")
  }
}

apply(from = "$rootDir/gradle/scripts/code-formatting.gradle")
