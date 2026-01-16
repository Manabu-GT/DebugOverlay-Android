package com.ms.square.debugoverlay.internal.bugreport

internal object FileNames {
  const val SCREENSHOT = "screenshot.png"
  const val HTML_REPORT = "bug_report.html"
  const val LOGCAT_LOGS = "logcat_logs.json"
  const val NETWORK_REQUESTS = "network_requests.json"
  const val DEVICE_INFO = "device_info.json"
  const val JANK_STATS = "jank_stats.json"
  const val APP_EXITS = "app_exits.txt"
  const val UI_HIERARCHY = "ui_hierarchy.txt"
  const val METADATA = "metadata.json"

  /**
   * Generates a filename for custom tracker logs based on source name.
   * Example: "Timber" -> "timber_logs.json"
   */
  fun trackerLogsFilename(sourceName: String): String = "${sourceName.lowercase()}_logs.json"
}
