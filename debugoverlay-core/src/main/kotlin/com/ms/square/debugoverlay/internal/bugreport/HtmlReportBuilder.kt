package com.ms.square.debugoverlay.internal.bugreport

import android.graphics.Bitmap
import android.util.Base64
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportData
import com.ms.square.debugoverlay.internal.bugreport.model.UserInput
import com.ms.square.debugoverlay.internal.data.model.AppExitInfo
import com.ms.square.debugoverlay.internal.data.model.AppExitReason
import com.ms.square.debugoverlay.internal.data.model.DeviceInfo
import com.ms.square.debugoverlay.internal.data.model.JankStatsUiState
import com.ms.square.debugoverlay.internal.util.HTTP_SUCCESS_END
import com.ms.square.debugoverlay.internal.util.HTTP_SUCCESS_START
import com.ms.square.debugoverlay.internal.util.escapeHtml
import com.ms.square.debugoverlay.internal.util.formatBytes
import com.ms.square.debugoverlay.internal.util.formatBytesFromKb
import com.ms.square.debugoverlay.internal.util.formatFullTimestamp
import com.ms.square.debugoverlay.internal.util.formatTimestamp
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.NetworkRequest
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

/**
 * Builds an HTML bug report with embedded screenshot and diagnostic data.
 *
 * Features:
 * - Material Design 3 inspired dark theme
 * - Collapsible sections for logs, network requests, etc.
 * - Screenshot embedded as base64 data URI for self-contained viewing
 * - Responsive design
 *
 * Note: The screenshot is embedded as base64 rather than referencing a separate file because
 * when opening HTML from a ZIP via content:// URIs (e.g., on Android), the FileProvider only
 * grants permission to the specific file being opened. Relative path references like
 * "screenshot.png" cannot be resolved since the viewer has no permission to access sibling
 * files in the ZIP.
 */
@Suppress("TooManyFunctions", "LongMethod", "LargeClass")
internal object HtmlReportBuilder {

  /**
   * Builds a complete HTML report and writes it to the given file.
   *
   * Note: This writes directly without atomic operations (temp file + rename).
   * The caller ([BugReportZipWriter]) passes a temp file and catches exceptions,
   * skipping failed entries to produce a partial report rather than failing entirely.
   */
  fun build(data: BugReportData, file: File) {
    file.bufferedWriter().use { writer ->
      writer.write(buildHtmlString(data))
    }
  }

  /**
   * Injects user input into an existing HTML report via placeholder replacement.
   *
   * The HTML contains placeholders ([TITLE_PLACEHOLDER], [DESCRIPTION_PLACEHOLDER]) that
   * are replaced with actual values. This approach is more robust than searching for
   * literal HTML strings, as it won't silently fail if the template changes.
   *
   * @param html The original HTML content with placeholders
   * @param userInput User-provided title and description
   * @return HTML with user input injected
   */
  internal fun injectUserInput(html: String, userInput: UserInput): String {
    // Replace title placeholder with user title or default
    val title = userInput.title.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE
    var result = html.replace(TITLE_PLACEHOLDER, title.escapeHtml())

    // Replace description placeholder with section or empty string
    val descriptionHtml = userInput.description.takeIf { it.isNotBlank() }
      ?.let { buildDescriptionSection(it) }
      ?: ""
    result = result.replace(DESCRIPTION_PLACEHOLDER, descriptionHtml)

    return result
  }

  /**
   * Replaces placeholders with default values when no user input is provided.
   * This ensures the HTML output doesn't contain raw placeholder comments.
   */
  internal fun injectDefaults(html: String): String = html
    .replace(TITLE_PLACEHOLDER, DEFAULT_TITLE.escapeHtml())
    .replace(DESCRIPTION_PLACEHOLDER, "")

  private fun buildDescriptionSection(description: String): String = buildString {
    append("    <div class=\"section\">\n")
    append("      <div class=\"section-header\" onclick=\"toggleSection(this)\">\n")
    append("        <h2>Description</h2>\n")
    append("        <span class=\"chevron\">&#9660;</span>\n")
    append("      </div>\n")
    append("      <div class=\"section-content\">\n")
    append("        <div class=\"pre-content\">${description.escapeHtml()}</div>\n")
    append("      </div>\n")
    append("    </div>")
  }

  private fun buildHtmlString(data: BugReportData): String = buildString {
    val timestamp = formatFullTimestamp(data.timestampMs)

    append("<!DOCTYPE html>\n")
    append("<html lang=\"en\">\n")
    append("<head>\n")
    append("  <meta charset=\"UTF-8\">\n")
    append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
    append("  <title>Bug Report - $timestamp</title>\n")
    appendStyles()
    append("</head>\n")
    append("<body>\n")
    append("  <div class=\"container\">\n")

    // Header with placeholders (replaced by injectUserInput() when user submits)
    appendHeader(timestamp)

    // Screenshot section
    appendScreenshotSection(data.screenshot)

    // Device Info section
    appendDeviceInfoSection(data.deviceInfo)

    // Logcat logs section (always present)
    appendLogsSection(data.logcatLogs, "System Logcat", "logcat")

    // Custom tracker logs section (if tracker registered)
    data.customTrackerData?.let { customData ->
      appendLogsSection(customData.logs, customData.sourceName, "custom-logs")
    }

    // Network section
    appendNetworkSection(data.networkRequests)

    // JankStats section
    appendJankStatsSection(data.jankStats)

    // App Exits section
    appendAppExitsSection(data.appExitInfos)

    // UI Hierarchy section
    appendUiHierarchySection(data.uiHierarchy)

    // Footer
    appendFooter()

    append("  </div>\n")
    appendScript()
    append("</body>\n")
    append("</html>\n")
  }

  private fun StringBuilder.appendStyles() {
    append(
      """
  <style>
    :root {
      --bg-primary: #1a1a2e;
      --bg-secondary: #16213e;
      --bg-tertiary: #0f3460;
      --text-primary: #e8e8e8;
      --text-secondary: #a0a0a0;
      --accent: #7c4dff;
      --accent-light: #b47cff;
      --success: #4caf50;
      --warning: #ff9800;
      --error: #f44336;
      --border: #2a2a4a;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: var(--bg-primary);
      color: var(--text-primary);
      line-height: 1.6;
      padding: 20px;
    }
    .container { max-width: 1200px; margin: 0 auto; }
    header {
      background: linear-gradient(135deg, var(--bg-tertiary), var(--bg-secondary));
      border-radius: 12px;
      padding: 24px;
      margin-bottom: 24px;
      border: 1px solid var(--border);
    }
    h1 { font-size: 1.5rem; font-weight: 600; margin-bottom: 8px; }
    .timestamp { color: var(--text-secondary); font-size: 0.875rem; }
    .section {
      background: var(--bg-secondary);
      border-radius: 12px;
      margin-bottom: 16px;
      border: 1px solid var(--border);
      overflow: hidden;
    }
    .section-header {
      background: var(--bg-tertiary);
      padding: 16px 20px;
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      align-items: center;
      user-select: none;
    }
    .section-header:hover { background: #1a4a7a; }
    .section-header h2 {
      font-size: 1rem;
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .section-header .count {
      background: var(--accent);
      color: white;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 0.75rem;
    }
    .section-header .chevron { transition: transform 0.2s; }
    .section.collapsed .chevron { transform: rotate(-90deg); }
    .section.collapsed .section-content { display: none; }
    .section-content { padding: 16px 20px; }
    .screenshot-container { text-align: center; padding: 20px; }
    .screenshot {
      max-width: 100%;
      max-height: 500px;
      border-radius: 8px;
      border: 1px solid var(--border);
      box-shadow: 0 4px 20px rgba(0,0,0,0.3);
    }
    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 12px;
    }
    .info-item {
      background: var(--bg-primary);
      padding: 12px 16px;
      border-radius: 8px;
    }
    .info-label {
      color: var(--text-secondary);
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 4px;
    }
    .info-value { font-weight: 500; word-break: break-word; }
    .log-entry {
      font-family: 'JetBrains Mono', 'Fira Code', monospace;
      font-size: 0.8rem;
      padding: 8px 12px;
      border-bottom: 1px solid var(--border);
      display: grid;
      grid-template-columns: 90px 24px 1fr;
      gap: 12px;
    }
    .log-entry:last-child { border-bottom: none; }
    .log-entry:hover { background: var(--bg-primary); }
    .log-time { color: var(--text-secondary); }
    .log-level { font-weight: 600; }
    .log-level.verbose { color: #b4b4b4; }
    .log-level.debug { color: #2196f3; }
    .log-level.info { color: var(--success); }
    .log-level.warn { color: var(--warning); }
    .log-level.error { color: var(--error); }
    .log-message { word-break: break-word; }
    .log-tag { color: var(--accent-light); }
    .network-entry { padding: 12px 16px; border-bottom: 1px solid var(--border); }
    .network-entry:last-child { border-bottom: none; }
    .network-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
    .method {
      background: var(--accent);
      color: white;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 600;
    }
    .status {
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 600;
    }
    .status.success { background: var(--success); color: white; }
    .status.error { background: var(--error); color: white; }
    .url { font-family: monospace; font-size: 0.85rem; color: var(--text-secondary); word-break: break-all; }
    .network-meta { display: flex; gap: 16px; font-size: 0.8rem; color: var(--text-secondary); }
    .details-toggle {
      color: var(--accent-light);
      font-size: 0.75rem;
      cursor: pointer;
      margin-top: 8px;
    }
    .details-toggle:hover { text-decoration: underline; }
    .network-details {
      display: none;
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid var(--border);
    }
    .network-entry.expanded .network-details { display: block; }
    .network-entry.expanded .details-toggle .arrow { transform: rotate(90deg); }
    .details-toggle .arrow { display: inline-block; transition: transform 0.2s; }
    .detail-section { margin-bottom: 12px; }
    .detail-section:last-child { margin-bottom: 0; }
    .detail-label { color: var(--text-secondary); font-size: 0.7rem; text-transform: uppercase; margin-bottom: 4px; }
    .headers-list { font-family: monospace; font-size: 0.75rem; background: var(--bg-primary); padding: 8px; border-radius: 4px; }
    .header-item { padding: 2px 0; word-break: break-all; }
    .header-name { color: var(--accent-light); }
    .body-content { font-family: monospace; font-size: 0.75rem; background: var(--bg-primary); padding: 8px; border-radius: 4px; white-space: pre-wrap; word-break: break-all; max-height: 200px; overflow-y: auto; }
    .error-content { background: rgba(244, 67, 54, 0.1); border: 1px solid var(--error); }
    .stat-item { display: flex; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid var(--border); }
    .stat-item:last-child { border-bottom: none; }
    .exit-entry { padding: 12px 16px; border-bottom: 1px solid var(--border); }
    .exit-entry:last-child { border-bottom: none; }
    .exit-reason { font-weight: 600; color: var(--warning); }
    .pre-content {
      font-family: 'JetBrains Mono', 'Fira Code', monospace;
      font-size: 0.75rem;
      background: var(--bg-primary);
      padding: 16px;
      border-radius: 8px;
      overflow-x: auto;
      white-space: pre-wrap;
      word-break: break-word;
    }
    .empty-state { color: var(--text-secondary); font-style: italic; padding: 20px; text-align: center; }
    footer { text-align: center; padding: 24px; color: var(--text-secondary); font-size: 0.8rem; }
    footer a { color: var(--accent-light); text-decoration: none; }
    /* Responsive styles for mobile */
    @media (max-width: 600px) {
      body { padding: 12px; }
      header { padding: 16px; }
      h1 { font-size: 1.25rem; }
      .section-header { padding: 12px 16px; }
      .section-content { padding: 12px 16px; }
      .info-grid { grid-template-columns: 1fr; }
      .log-entry {
        grid-template-columns: 1fr;
        gap: 4px;
      }
      .log-entry .log-time { display: none; }
      .log-entry .log-level {
        display: inline;
        margin-right: 8px;
      }
      .log-entry .log-message { display: inline; }
      .network-meta { flex-wrap: wrap; gap: 8px; }
      .screenshot { max-height: 300px; }
    }
  </style>
      """.trimIndent()
    )
  }

  private fun StringBuilder.appendHeader(timestamp: String) {
    append("    <header>\n")
    append("      <h1>$TITLE_PLACEHOLDER</h1>\n")
    append("      <div class=\"timestamp\">Generated: ${timestamp.escapeHtml()}</div>\n")
    append("    </header>\n")
    append("    $DESCRIPTION_PLACEHOLDER\n")
  }

  private fun StringBuilder.appendScreenshotSection(screenshot: Bitmap?) {
    append("    <div class=\"section\">\n")
    append("      <div class=\"section-header\" onclick=\"toggleSection(this)\">\n")
    append("        <h2>Screenshot</h2>\n")
    append("        <span class=\"chevron\">&#9660;</span>\n")
    append("      </div>\n")
    append("      <div class=\"section-content screenshot-container\">\n")

    if (screenshot != null) {
      // Embed as base64 data URI - see class KDoc for why this is necessary
      val base64Data = encodeScreenshotToBase64(screenshot)
      append("        <img class=\"screenshot\" src=\"data:image/png;base64,$base64Data\" alt=\"App Screenshot\">\n")
    } else {
      append("        <div class=\"empty-state\">Screenshot not available</div>\n")
    }

    append("      </div>\n")
    append("    </div>\n")
  }

  private fun encodeScreenshotToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, UNUSED_PNG_QUALITY, outputStream)
    val bytes = outputStream.toByteArray()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
  }

  private fun StringBuilder.appendDeviceInfoSection(deviceInfo: DeviceInfo?) {
    append("    <div class=\"section\">\n")
    append("      <div class=\"section-header\" onclick=\"toggleSection(this)\">\n")
    append("        <h2>Device Information</h2>\n")
    append("        <span class=\"chevron\">&#9660;</span>\n")
    append("      </div>\n")
    append("      <div class=\"section-content\">\n")

    if (deviceInfo != null) {
      append("        <div class=\"info-grid\">\n")
      // Device basics
      appendInfoItem("Device", "${deviceInfo.hardware.manufacturer} ${deviceInfo.hardware.model}")
      appendInfoItem("Brand", deviceInfo.hardware.brand)

      // Android & Build
      appendInfoItem("Android", "${deviceInfo.system.androidVersion} (API ${deviceInfo.system.apiLevel})")
      appendInfoItem("Security Patch", deviceInfo.system.securityPatch)
      appendInfoItem("Build", "${deviceInfo.system.buildType} / ${deviceInfo.system.buildId}")
      appendInfoItem("Kernel", deviceInfo.system.kernelVersion)

      // Display
      appendInfoItem("Screen", "${deviceInfo.hardware.screenResolution} @ ${deviceInfo.hardware.screenDensity}")
      val currentHz = deviceInfo.hardware.currentRefreshRate.roundToInt()
      val maxHz = deviceInfo.hardware.maxRefreshRate.roundToInt()
      appendInfoItem("Refresh Rate", "${currentHz}Hz (max ${maxHz}Hz)")
      appendInfoItem("OpenGL", deviceInfo.hardware.openGlVersion)

      // CPU
      appendInfoItem(
        "CPU",
        "${deviceInfo.hardware.cpuArchitecture} (${deviceInfo.hardware.cpuCores} cores)"
      )
      appendInfoItem("ABIs", deviceInfo.hardware.supportedAbis.joinToString(", "))

      // Memory & Storage
      appendInfoItem(
        "RAM",
        "${formatBytes(deviceInfo.hardware.availableRam)} / ${formatBytes(deviceInfo.hardware.totalRam)}"
      )
      appendInfoItem(
        "Storage",
        "${formatBytes(deviceInfo.hardware.availableStorage)} / ${formatBytes(deviceInfo.hardware.totalStorage)}"
      )

      // Battery & Network
      appendInfoItem("Battery", "${deviceInfo.battery.level}% (${deviceInfo.battery.status})")
      appendInfoItem(
        "Network",
        "${deviceInfo.network.type}${if (deviceInfo.network.isConnected) " - Connected" else ""}"
      )

      // Localization
      appendInfoItem("Locale", "${deviceInfo.system.locale} (${deviceInfo.system.language})")
      appendInfoItem("Time Zone", deviceInfo.system.timeZone)

      // System status
      appendInfoItem(
        "Environment",
        buildList {
          if (deviceInfo.system.isEmulator) add("Emulator")
          if (deviceInfo.system.isRooted) add("Rooted")
          if (isEmpty()) add("Normal")
        }.joinToString(", ")
      )
      deviceInfo.system.installerPackage?.let { appendInfoItem("Installer", it) }
      deviceInfo.system.playServicesVersion?.let { appendInfoItem("Play Services", it) }

      // Hardware features
      appendInfoItem(
        "Features",
        buildList {
          if (deviceInfo.hardware.hardwareFeature.hasNfc) add("NFC")
          if (deviceInfo.hardware.hardwareFeature.hasBluetooth) add("Bluetooth")
          if (deviceInfo.hardware.hardwareFeature.hasCamera) add("Camera")
          if (deviceInfo.hardware.hardwareFeature.hasFingerprint) add("Fingerprint")
        }.joinToString(", ").ifEmpty { "None" }
      )

      append("        </div>\n")
    } else {
      append("        <div class=\"empty-state\">Device information not available</div>\n")
    }

    append("      </div>\n")
    append("    </div>\n")
  }

  private fun StringBuilder.appendInfoItem(label: String, value: String) {
    append("          <div class=\"info-item\">\n")
    append("            <div class=\"info-label\">${label.escapeHtml()}</div>\n")
    append("            <div class=\"info-value\">${value.escapeHtml()}</div>\n")
    append("          </div>\n")
  }

  private fun StringBuilder.appendLogsSection(logs: List<LogEntry>, title: String, sectionId: String) {
    append("    <div class=\"section\" id=\"$sectionId\">\n")
    append("      <div class=\"section-header\" onclick=\"toggleSection(this)\">\n")
    append("        <h2>${title.escapeHtml()}</h2>\n")
    if (logs.isNotEmpty()) {
      append("        <span class=\"count\">${logs.size} entries</span>\n")
    }
    append("        <span class=\"chevron\">&#9660;</span>\n")
    append("      </div>\n")
    append("      <div class=\"section-content\" style=\"padding: 0; max-height: 400px; overflow-y: auto;\">\n")

    if (logs.isEmpty()) {
      append("        <div class=\"empty-state\">No log entries</div>\n")
    } else {
      logs.forEach { log ->
        val levelClass = log.level.name.lowercase()
        val levelChar = log.level.name.first()
        val time = formatTimestamp(log.timestampMs)
        append("        <div class=\"log-entry\">\n")
        append("          <span class=\"log-time\">${time.escapeHtml()}</span>\n")
        append("          <span class=\"log-level $levelClass\">$levelChar</span>\n")
        append("          <span class=\"log-message\">")
        append("<span class=\"log-tag\">[${log.tag.escapeHtml()}]</span> ${log.message.escapeHtml()}")
        append("</span>\n")
        append("        </div>\n")
      }
    }

    append("      </div>\n")
    append("    </div>\n")
  }

  private fun StringBuilder.appendNetworkSection(requests: List<NetworkRequest>) {
    append("    <div class=\"section\">\n")
    append("      <div class=\"section-header\" onclick=\"toggleSection(this)\">\n")
    append("        <h2>Network Requests</h2>\n")
    if (requests.isNotEmpty()) {
      append("        <span class=\"count\">${requests.size} requests</span>\n")
    }
    append("        <span class=\"chevron\">&#9660;</span>\n")
    append("      </div>\n")
    append("      <div class=\"section-content\" style=\"padding: 0; max-height: 400px; overflow-y: auto;\">\n")

    if (requests.isEmpty()) {
      append("        <div class=\"empty-state\">No network requests</div>\n")
    } else {
      requests.forEach { appendNetworkEntry(it) }
    }

    append("      </div>\n")
    append("    </div>\n")
  }

  private fun StringBuilder.appendNetworkEntry(request: NetworkRequest) {
    val isSuccess = request.statusCode?.let { it in HTTP_SUCCESS_START..HTTP_SUCCESS_END } == true
    val statusClass = if (isSuccess) "success" else "error"
    val statusText = request.statusCode?.toString() ?: "ERR"
    val hasDetails = request.hasDetails()
    append("        <div class=\"network-entry\">\n")
    append("          <div class=\"network-header\">\n")
    append("            <span class=\"method\">${request.method.escapeHtml()}</span>\n")
    append("            <span class=\"status $statusClass\">$statusText</span>\n")
    request.protocol?.let {
      append("            <span style=\"color: var(--text-secondary); font-size: 0.75rem;\">")
      append("${it.escapeHtml()}</span>\n")
    }
    append("          </div>\n")
    append("          <div class=\"url\">${request.url.escapeHtml()}</div>\n")
    append("          <div class=\"network-meta\">\n")
    append("            <span>${request.durationMs}ms</span>\n")
    request.requestSize?.let { append("            <span>&#8593; ${formatBytes(it)}</span>\n") }
    request.responseSize?.let { append("            <span>&#8595; ${formatBytes(it)}</span>\n") }
    append("          </div>\n")
    if (hasDetails) {
      append("          <div class=\"details-toggle\" onclick=\"toggleNetworkDetails(this)\">")
      append("<span class=\"arrow\">&#9654;</span> Details</div>\n")
      append("          <div class=\"network-details\">\n")
      appendNetworkDetails(request)
      append("          </div>\n")
    }
    append("        </div>\n")
  }

  private fun NetworkRequest.hasDetails(): Boolean = requestHeaders.isNotEmpty() ||
    responseHeaders.isNotEmpty() ||
    requestBody != null ||
    responseBody != null ||
    error != null

  private fun StringBuilder.appendNetworkDetails(request: NetworkRequest) {
    // Error section (most important - show first)
    request.error?.let { error ->
      append("            <div class=\"detail-section\">\n")
      append("              <div class=\"detail-label\">Error</div>\n")
      append("              <div class=\"body-content error-content\">")
      append("${error.title.escapeHtml()}: ${error.message.escapeHtml()}")
      error.stackTrace?.let { append("\n\n${it.truncateBody().escapeHtml()}") }
      append("</div>\n")
      append("            </div>\n")
    }
    // Request headers
    if (request.requestHeaders.isNotEmpty()) {
      append("            <div class=\"detail-section\">\n")
      append("              <div class=\"detail-label\">Request Headers</div>\n")
      append("              <div class=\"headers-list\">\n")
      request.requestHeaders.forEach { (name, value) ->
        append("                <div class=\"header-item\">")
        append("<span class=\"header-name\">${name.escapeHtml()}:</span> ${value.escapeHtml()}")
        append("</div>\n")
      }
      append("              </div>\n")
      append("            </div>\n")
    }
    // Request body
    request.requestBody?.let { body ->
      append("            <div class=\"detail-section\">\n")
      append("              <div class=\"detail-label\">Request Body</div>\n")
      append("              <div class=\"body-content\">${body.truncateBody().escapeHtml()}</div>\n")
      append("            </div>\n")
    }
    // Response headers
    if (request.responseHeaders.isNotEmpty()) {
      append("            <div class=\"detail-section\">\n")
      append("              <div class=\"detail-label\">Response Headers</div>\n")
      append("              <div class=\"headers-list\">\n")
      request.responseHeaders.forEach { (name, value) ->
        append("                <div class=\"header-item\">")
        append("<span class=\"header-name\">${name.escapeHtml()}:</span> ${value.escapeHtml()}")
        append("</div>\n")
      }
      append("              </div>\n")
      append("            </div>\n")
    }
    // Response body
    request.responseBody?.let { body ->
      append("            <div class=\"detail-section\">\n")
      append("              <div class=\"detail-label\">Response Body</div>\n")
      append("              <div class=\"body-content\">${body.truncateBody().escapeHtml()}</div>\n")
      append("            </div>\n")
    }
  }

  private fun StringBuilder.appendJankStatsSection(jankStats: JankStatsUiState?) {
    append("    <div class=\"section\">\n")
    append("      <div class=\"section-header\" onclick=\"toggleSection(this)\">\n")
    append("        <h2>Performance (JankStats)</h2>\n")
    append("        <span class=\"chevron\">&#9660;</span>\n")
    append("      </div>\n")
    append("      <div class=\"section-content\" style=\"padding: 0;\">\n")

    if (jankStats == null || jankStats.totalFrames == 0) {
      append("        <div class=\"empty-state\">No frame data available</div>\n")
    } else {
      val jankPercent = "%.1f".format(jankStats.jankPercentage.value)
      append("        <div class=\"stat-item\">")
      append("<span>Total Frames</span><span>${jankStats.totalFrames}</span></div>\n")
      append("        <div class=\"stat-item\"><span>Janky Frames</span>")
      append("<span style=\"color: var(--warning);\">${jankStats.jankyFrames} ($jankPercent%)</span>")
      append("</div>\n")
      append("        <div class=\"stat-item\">")
      append("<span>Avg Frame Time</span><span>${jankStats.avgFrameDurationMs}ms</span>")
      append("</div>\n")
    }

    append("      </div>\n")
    append("    </div>\n")
  }

  private fun StringBuilder.appendAppExitsSection(exits: List<AppExitInfo>) {
    append("    <div class=\"section${if (exits.isEmpty()) "" else " collapsed"}\">\n")
    append("      <div class=\"section-header\" onclick=\"toggleSection(this)\">\n")
    append("        <h2>Recent App Exits</h2>\n")
    if (exits.isNotEmpty()) {
      append("        <span class=\"count\">${exits.size} exits</span>\n")
    }
    append("        <span class=\"chevron\">&#9660;</span>\n")
    append("      </div>\n")
    append("      <div class=\"section-content\" style=\"padding: 0;\">\n")

    if (exits.isEmpty()) {
      append("        <div class=\"empty-state\">No app exit events recorded</div>\n")
    } else {
      exits.forEach { exit -> appendAppExitEntry(exit) }
    }

    append("      </div>\n")
    append("    </div>\n")
  }

  private fun StringBuilder.appendAppExitEntry(exit: AppExitInfo) {
    val time = formatFullTimestamp(exit.timestampMs)
    val severityColor = when (exit.reason.severity) {
      AppExitReason.Severity.CRITICAL -> "var(--error)"
      AppExitReason.Severity.WARNING -> "var(--warning)"
      AppExitReason.Severity.INFO -> "var(--text-secondary)"
    }

    append("        <div class=\"exit-entry\">\n")
    append("          <div class=\"exit-reason\" style=\"color: $severityColor;\">")
    append("${exit.reason.label.escapeHtml()}</div>\n")

    // Time, process, importance
    append("          <div style=\"color: var(--text-secondary); font-size: 0.85rem;\">")
    append("$time &bull; ${exit.processName.escapeHtml()} &bull; ${exit.importance.label}")
    append("</div>\n")

    // Memory usage (PSS/RSS) - show only available values
    if (exit.pssKb > 0 || exit.rssKb > 0) {
      val memoryParts = buildList {
        if (exit.pssKb > 0) add("PSS ${formatBytesFromKb(exit.pssKb)}")
        if (exit.rssKb > 0) add("RSS ${formatBytesFromKb(exit.rssKb)}")
      }
      append("          <div style=\"color: var(--text-secondary); font-size: 0.85rem; margin-top: 2px;\">")
      append("Memory: ${memoryParts.joinToString(" / ")}")
      append("</div>\n")
    }

    // Description from system
    exit.description?.let { desc ->
      append("          <div style=\"color: var(--text-secondary); font-size: 0.85rem; margin-top: 4px;\">")
      append(desc.escapeHtml())
      append("</div>\n")
    }

    // Explanation (helpful context for the exit reason)
    append(
      "          <div style=\"color: var(--accent-light); font-size: 0.8rem; margin-top: 6px; font-style: italic;\">"
    )
    append(exit.reason.explanation.escapeHtml())
    append("</div>\n")

    // Stack trace (expandable)
    exit.trace?.let { trace ->
      append("          <div class=\"details-toggle\" onclick=\"toggleExitTrace(this)\">")
      append("<span class=\"arrow\">&#9654;</span> Stack Trace</div>\n")
      append("          <div class=\"exit-trace\" style=\"display: none; margin-top: 8px;\">\n")
      append("            <pre class=\"pre-content\">${trace.truncateBody(MAX_TRACE_LENGTH).escapeHtml()}</pre>\n")
      append("          </div>\n")
    }

    append("        </div>\n")
  }

  private fun StringBuilder.appendUiHierarchySection(uiHierarchy: String?) {
    append("    <div class=\"section collapsed\">\n")
    append("      <div class=\"section-header\" onclick=\"toggleSection(this)\">\n")
    append("        <h2>UI Hierarchy</h2>\n")
    append("        <span class=\"chevron\">&#9660;</span>\n")
    append("      </div>\n")
    append("      <div class=\"section-content\">\n")

    if (uiHierarchy.isNullOrBlank()) {
      append("        <div class=\"empty-state\">UI hierarchy not available</div>\n")
    } else {
      append("        <pre class=\"pre-content\">${uiHierarchy.escapeHtml()}</pre>\n")
    }

    append("      </div>\n")
    append("    </div>\n")
  }

  private fun StringBuilder.appendFooter() {
    append("    <footer>\n")
    append("      Generated by <a href=\"https://github.com/Manabu-GT/DebugOverlay-Android\">DebugOverlay</a>\n")
    append("    </footer>\n")
  }

  private fun StringBuilder.appendScript() {
    append(
      """
  <script>
    function toggleSection(header) {
      header.parentElement.classList.toggle('collapsed');
    }
    function toggleNetworkDetails(toggle) {
      toggle.parentElement.classList.toggle('expanded');
    }
    function toggleExitTrace(toggle) {
      var trace = toggle.nextElementSibling;
      var arrow = toggle.querySelector('.arrow');
      if (trace.style.display === 'none') {
        trace.style.display = 'block';
        arrow.style.transform = 'rotate(90deg)';
      } else {
        trace.style.display = 'none';
        arrow.style.transform = 'rotate(0deg)';
      }
    }
  </script>
      """.trimIndent()
    )
  }

  private fun String.truncateBody(maxLength: Int = MAX_BODY_LENGTH): String = if (length <= maxLength) {
    this
  } else {
    val shownSize = formatBytes(maxLength.toLong())
    val totalSize = formatBytes(length.toLong())
    "${take(maxLength)}...\n\n[truncated: showing $shownSize of $totalSize]"
  }

  private const val MAX_BODY_LENGTH = 2048
  private const val MAX_TRACE_LENGTH = 8192

  // Placeholders for metadata injection - used by injectMetadata()
  private const val TITLE_PLACEHOLDER = "<!--TITLE_PLACEHOLDER-->"
  private const val DESCRIPTION_PLACEHOLDER = "<!--DESCRIPTION_PLACEHOLDER-->"
  private const val DEFAULT_TITLE = "Bug Report"
}
