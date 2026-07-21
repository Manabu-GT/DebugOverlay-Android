package com.ms.square.debugoverlay.internal.bugreport

import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.internal.bugreport.model.AppInfo
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportData
import com.ms.square.debugoverlay.model.NetworkRequest
import org.junit.Test

class HtmlReportBuilderTest {

  private val appInfo = AppInfo(
    packageName = "com.example.app",
    versionName = "1.0.0",
    versionCode = 1,
    targetSdkVersion = 34,
    minSdkVersion = 24,
    isDebuggable = true,
    installerStore = "Unknown",
    installerPackage = null,
    firstInstallTime = 0L,
    lastUpdateTime = 0L
  )

  private fun reportData(networkRequests: List<NetworkRequest>) = BugReportData(
    capturedAt = 1706400000000L,
    userInput = null,
    appInfo = appInfo,
    screenshot = null,
    deviceInfo = null,
    logcatLogs = emptyList(),
    customLogSourceData = null,
    networkRequests = networkRequests,
    jankStats = null,
    appExitInfos = emptyList(),
    uiHierarchy = null
  )

  private fun networkRequest(responseBody: String?, responseHeaders: Map<String, String> = emptyMap()) = NetworkRequest(
    protocol = "h2",
    method = "GET",
    url = "https://example.com/api",
    statusCode = 200,
    durationMs = 42,
    responseSize = responseBody?.length?.toLong(),
    requestSize = 0,
    responseHeaders = responseHeaders,
    responseBody = responseBody
  )

  private fun buildHtml(networkRequests: List<NetworkRequest>): String =
    HtmlReportBuilder.buildHtmlString(reportData(networkRequests))

  @Test
  fun `pretty-prints JSON response body detected via Content-Type header`() {
    val html = buildHtml(
      listOf(
        networkRequest(
          responseBody = """{"name":"test","count":2}""",
          responseHeaders = mapOf("Content-Type" to "application/json")
        )
      )
    )

    // Pretty-printed JSON is indented onto multiple lines; the compact form is not present.
    assertThat(html).contains("&quot;name&quot;: &quot;test&quot;")
    assertThat(html).doesNotContain("""{"name":"test","count":2}""")
  }

  @Test
  fun `pretty-prints JSON response body detected via content sniffing when header missing`() {
    val html = buildHtml(listOf(networkRequest(responseBody = """{"ok":true}""")))

    assertThat(html).contains("&quot;ok&quot;: true")
  }

  @Test
  fun `leaves non-JSON response body unformatted`() {
    val body = "plain text response body"
    val html = buildHtml(
      listOf(networkRequest(responseBody = body, responseHeaders = mapOf("Content-Type" to "text/plain")))
    )

    assertThat(html).contains(body)
    assertThat(html).doesNotContain("View Full")
    assertThat(html).doesNotContain("data-full=")
  }

  @Test
  fun `small body does not render a View Full toggle`() {
    val html = buildHtml(listOf(networkRequest(responseBody = """{"ok":true}""")))

    assertThat(html).doesNotContain("data-full=")
    assertThat(html).doesNotContain("onclick=\"toggleFullBody(this)\"")
  }

  @Test
  fun `large body renders a View Full toggle with expanded content`() {
    val largeArray = (1..500).joinToString(prefix = "[", postfix = "]") { "\"item-$it\"" }
    val html = buildHtml(
      listOf(networkRequest(responseBody = largeArray, responseHeaders = mapOf("Content-Type" to "application/json")))
    )

    assertThat(html).contains("data-full=\"")
    assertThat(html).contains("View Full")
    assertThat(html).doesNotContain("View More") // fits entirely within the cap, so it's genuinely "full"
    assertThat(html).contains("item-500") // well under the 64KB cap, so fully present in data-full
    assertThat(html).contains("truncated") // truncation notice still shown in the inline preview
  }

  @Test
  fun `very large body caps the View Full content and labels it as partial`() {
    // ~200KB of raw content - comfortably exceeds the internal 64KB cap on the "View Full" payload,
    // guarding against a single report ballooning into hundreds of MB when many large bodies are captured.
    val hugeArray = (1..9999).joinToString(prefix = "[", postfix = "]") { "\"item-$it\"" }
    val html = buildHtml(
      listOf(networkRequest(responseBody = hugeArray, responseHeaders = mapOf("Content-Type" to "application/json")))
    )

    assertThat(html).contains("item-1&quot;") // early content is well within the cap
    assertThat(html).doesNotContain("item-9999") // beyond the cap, so not embedded anywhere in the report
    // Label must make clear this is a partial view, not the true "full" body.
    assertThat(html).contains("View More (first 64.0 KB of")
  }
}
