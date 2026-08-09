package com.ms.square.debugoverlay.internal.crash

import com.ms.square.debugoverlay.internal.util.formatFullTimestamp
import com.ms.square.debugoverlay.internal.util.toClipboardText

private const val SEPARATOR_WIDTH = 80

/**
 * Formats a [CrashRecord] as human-readable plain text for sharing (e.g. to a teammate
 * or a GitHub issue), mirroring [com.ms.square.debugoverlay.formatBugReportMarkdown]'s
 * intent of a readable rendering rather than raw JSON.
 */
internal fun formatCrashRecordAsText(record: CrashRecord): String = buildString {
  appendLine("=".repeat(SEPARATOR_WIDTH))
  appendLine("${record.exceptionType}: ${record.message ?: "(no message)"}")
  appendLine("=".repeat(SEPARATOR_WIDTH))
  appendLine("Time: ${formatFullTimestamp(record.timestampMs)}")
  appendLine("Thread: ${record.threadName}")
  record.appInfo?.let { appInfo ->
    appendLine("Package: ${appInfo.packageName}")
    appInfo.versionName?.let { appendLine("Version: $it (${appInfo.versionCode})") }
  }
  appendLine()

  appendLine("--- STACK TRACE ---")
  appendLine(record.stackTrace)

  if (record.logcatLogs.isNotEmpty()) {
    appendLine()
    appendLine("--- LOGCAT (${record.logcatLogs.size}) ---")
    record.logcatLogs.forEach { appendLine(it.toClipboardText()) }
  }

  record.customLogSourceData?.let { customLogs ->
    if (customLogs.logs.isNotEmpty()) {
      appendLine()
      appendLine("--- ${customLogs.sourceName.uppercase()} (${customLogs.logs.size}) ---")
      customLogs.logs.forEach { appendLine(it.toClipboardText()) }
    }
  }

  if (record.networkRequests.isNotEmpty()) {
    appendLine()
    appendLine("--- NETWORK REQUESTS (${record.networkRequests.size}) ---")
    record.networkRequests.forEach { appendLine(it.toExportLine()) }
  }
}

private fun NetworkRequestSummary.toExportLine(): String {
  val errorSuffix = errorTitle?.let { " [ERROR: $it: $errorMessage]" }.orEmpty()
  return "${formatFullTimestamp(timestampMs)} $method $url -> ${statusCode ?: "?"} (${durationMs}ms)$errorSuffix"
}
