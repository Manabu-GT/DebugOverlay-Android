import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

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
  alias(libs.plugins.hilt.android) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.dexcount) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.detekt) apply false
  alias(libs.plugins.spotless)
}

val reportMerge by tasks.registering(io.gitlab.arturbosch.detekt.report.ReportMergeTask::class) {
  output.set(rootProject.layout.buildDirectory.file("reports/detekt/merge.sarif"))
}

val jacocoFileFilter = listOf(
  // Android generated
  "**/R.class",
  "**/R$*.class",
  "**/BuildConfig.*",
  "**/Manifest*.*",
  // Test classes
  "**/*Test*.*",
  // Android framework components
  "**/*Activity.class",
  "**/*Activity$*.class",
  "**/*Fragment.class",
  "**/*Fragment$*.class",
  // Compose generated
  "**/*ComposableSingletons*.class",
  "**/ComposableSingletons*.class",
  // Composable UI functions
  "**/ui/**",
  "**/bugreport/ui/**"
)

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

  // Configure JaCoCo when plugin is applied (plugin applied in each module's build.gradle.kts)
  plugins.withId("jacoco") {
    configure<JacocoPluginExtension> {
      toolVersion = libs.versions.jacoco.get()
    }

    // Enable coverage for Robolectric tests (classes loaded without location info)
    tasks.withType<Test>().configureEach {
      extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
      }
    }

    tasks.register<JacocoReport>("jacocoTestReport") {
      dependsOn("testDebugUnitTest")

      reports {
        xml.required.set(true)
        html.required.set(true)
      }

      val buildDir = project.layout.buildDirectory.get().asFile
      val kotlinDebugTree = fileTree("$buildDir/tmp/kotlin-classes/debug") {
        exclude(jacocoFileFilter)
      }
      val javaDebugTree = fileTree("$buildDir/intermediates/javac/debug/classes") {
        exclude(jacocoFileFilter)
      }

      sourceDirectories.setFrom(
        files(
          "${project.projectDir}/src/main/kotlin",
          "${project.projectDir}/src/main/java"
        )
      )
      classDirectories.setFrom(files(kotlinDebugTree, javaDebugTree))
      executionData.setFrom(files("$buildDir/jacoco/testDebugUnitTest.exec"))
    }
  }
}

apply(from = "$rootDir/gradle/scripts/code-formatting.gradle")

// Merged JaCoCo Report
plugins.apply("jacoco")

configure<JacocoPluginExtension> {
  toolVersion = libs.versions.jacoco.get()
}

tasks.register<JacocoReport>("mergedJacocoReport") {
  val jacocoProjects = subprojects.filter { it.name != "sample" }

  // Only depend on jacocoTestReport tasks (which already depend on tests)
  dependsOn(jacocoProjects.map { it.tasks.withType<JacocoReport>() })

  reports {
    xml.required.set(true)
    html.required.set(true)
    csv.required.set(true)
  }

  classDirectories.setFrom(
    files(
      jacocoProjects.flatMap {
        val buildDir = it.layout.buildDirectory.get().asFile
        listOf(
          fileTree("$buildDir/tmp/kotlin-classes/debug") {
            exclude(jacocoFileFilter)
          },
          fileTree("$buildDir/intermediates/javac/debug/classes") {
            exclude(jacocoFileFilter)
          }
        )
      }
    )
  )
  sourceDirectories.setFrom(
    files(
      jacocoProjects.flatMap {
        listOf(
          "${it.projectDir}/src/main/kotlin",
          "${it.projectDir}/src/main/java"
        )
      }
    )
  )
  executionData.setFrom(
    files(
      jacocoProjects.map {
        val buildDir = it.layout.buildDirectory.get().asFile
        "$buildDir/jacoco/testDebugUnitTest.exec"
      }
    )
  )
}
