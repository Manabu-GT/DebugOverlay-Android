package com.ms.square.debugoverlay.internal.bugreport

import java.util.Locale

/** Matches valid filename characters: alphanumeric, underscore, dot, hyphen. */
private val VALID_FILENAME_REGEX = Regex("""^[a-zA-Z0-9_.-]+$""")

/**
 * Validates a filename for use in bug reports.
 * @return Error message if invalid, null if valid
 */
internal fun validateFilename(filename: String): String? {
  if (filename.isBlank()) {
    return "Filename cannot be blank"
  }
  if (!VALID_FILENAME_REGEX.matches(filename)) {
    return "Filename contains invalid characters (allowed: a-z, A-Z, 0-9, _, ., -)"
  }
  if (filename.startsWith('.')) {
    return "Filename cannot start with '.'"
  }
  return null
}

private const val DEFAULT_SOURCE_NAME = "unknown"

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
   * Generates a sanitized filename for custom log source logs based on source name.
   *
   * Strips invalid characters (keeping only [a-zA-Z0-9_.-]), uses locale-independent
   * lowercase, and falls back to "unknown" if the result is empty.
   *
   * Example: "Timber" -> "timber_logs.json"
   */
  fun customLogSourceFilename(sourceName: String): String {
    val sanitized = sourceName
      .replace(Regex("[^a-zA-Z0-9_.-]"), "")
      .trimStart('.')
      .lowercase(Locale.ROOT)
      .ifEmpty { DEFAULT_SOURCE_NAME }
    return "${sanitized}_logs.json"
  }
}
