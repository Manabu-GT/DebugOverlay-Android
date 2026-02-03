package com.ms.square.debugoverlay

import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.internal.util.formatTimestamp
import com.ms.square.debugoverlay.model.AppInfoSummary
import com.ms.square.debugoverlay.model.BugReportSummary
import com.ms.square.debugoverlay.model.DeviceInfoSummary
import org.junit.Test

class BugReportFormatterTest {

  private val appInfo = AppInfoSummary(
    packageName = "com.example.app",
    versionName = "1.2.3",
    versionCode = 42,
    isDebuggable = true
  )

  private val deviceInfo = DeviceInfoSummary(
    manufacturer = "Google",
    model = "Pixel 8",
    androidVersion = "14",
    apiLevel = 34,
    locale = "en_US"
  )

  @Test
  fun `full summary produces valid markdown with all sections`() {
    val summary = BugReportSummary(
      title = "App crashes on login",
      description = "Steps:\n1. Open app\n2. Tap login",
      appInfo = appInfo,
      deviceInfo = deviceInfo,
      capturedAt = 1706400000000L // 2024-01-28 ~00:00 UTC
    )

    val markdown = formatBugReportMarkdown(summary)

    assertThat(markdown).contains("## Summary")
    assertThat(markdown).contains("App crashes on login")
    assertThat(markdown).contains("## Details")
    assertThat(markdown).contains("Steps:\n1. Open app\n2. Tap login")
    assertThat(markdown).contains("## Environment")
    assertThat(markdown).contains("com.example.app")
    assertThat(markdown).contains("1.2.3 (42)")
    assertThat(markdown).contains("Google Pixel 8")
    assertThat(markdown).contains("14 (API 34)")
    assertThat(markdown).contains("en_US")
    assertThat(markdown).contains(formatTimestamp(summary.capturedAt))
    assertThat(markdown).contains("## Attachments")
  }

  @Test
  fun `null or blank description omits Details section`() {
    val nullDescriptionSummary = BugReportSummary(
      title = "Bug Report",
      description = null,
      appInfo = appInfo,
      deviceInfo = deviceInfo,
      capturedAt = 1706400000000L
    )

    assertThat(formatBugReportMarkdown(nullDescriptionSummary))
      .doesNotContain("## Details")

    val blankDescriptionSummary = nullDescriptionSummary.copy(
      description = "   "
    )
    assertThat(formatBugReportMarkdown(blankDescriptionSummary))
      .doesNotContain("## Details")
  }

  @Test
  fun `null deviceInfo omits device lines`() {
    val summary = BugReportSummary(
      title = "Bug Report",
      description = null,
      appInfo = appInfo,
      deviceInfo = null,
      capturedAt = 1706400000000L
    )

    val markdown = formatBugReportMarkdown(summary)

    assertThat(markdown).doesNotContain("Device |")
    assertThat(markdown).doesNotContain("Android |")
    assertThat(markdown).doesNotContain("Locale |")
  }

  @Test
  fun `null versionName omits version line`() {
    val noVersionAppInfo = appInfo.copy(versionName = null)
    val summary = BugReportSummary(
      title = "Bug Report",
      description = null,
      appInfo = noVersionAppInfo,
      deviceInfo = deviceInfo,
      capturedAt = 1706400000000L
    )

    val markdown = formatBugReportMarkdown(summary)

    assertThat(markdown).doesNotContain("| Version |")
  }
}
