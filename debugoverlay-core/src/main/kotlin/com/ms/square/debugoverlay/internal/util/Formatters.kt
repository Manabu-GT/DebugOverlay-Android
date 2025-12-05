package com.ms.square.debugoverlay.internal.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val BYTES_PER_KB = 1024L
private const val BYTES_PER_MB = 1024L * 1024L
private const val BYTES_PER_GB = 1024L * 1024L * 1024L

private val JSON_FORMATTER = Json {
  prettyPrint = true
}

/**
 * Format bytes to human-readable string.
 */
internal fun formatBytes(bytes: Long?): String = when {
  bytes == null || bytes < 0 -> "—"
  bytes < BYTES_PER_KB -> "$bytes B"
  bytes < BYTES_PER_MB -> {
    val kb = bytes / BYTES_PER_KB.toDouble()
    @Suppress("MagicNumber")
    if (kb < 10) "%.2f KB".format(kb) else "%.1f KB".format(kb)
  }
  bytes < BYTES_PER_GB -> "%.1f MB".format(bytes / BYTES_PER_MB.toDouble())
  else -> "%.1f GB".format(bytes / BYTES_PER_GB.toDouble())
}

/**
 * Format text size to human-readable string.
 */
internal fun formatTextSize(length: Int): String = when {
  length < BYTES_PER_KB -> "$length chars"
  length < BYTES_PER_MB -> "${length / BYTES_PER_KB} KB"
  else -> "${"%.1f".format(length / (BYTES_PER_MB.toDouble()))} MB"
}

internal fun formatTimestamp(timestamp: Long): String {
  val date = Date(timestamp)
  val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
  return formatter.format(date)
}

/**
 * Format timestamp as relative time (e.g., "2s ago", "5m ago", "2h ago").
 */
@Suppress("MagicNumber")
internal fun formatRelativeTime(timestamp: Long): String {
  val now = System.currentTimeMillis()
  val diff = now - timestamp

  return when {
    diff < 1000 -> "just now" // Less than 1 second
    diff < 60_000 -> "${diff / 1000}s ago" // Less than 1 minute
    diff < 3_600_000 -> "${diff / 60_000}m ago" // Less than 1 hour
    diff < 86_400_000 -> "${diff / 3_600_000}h ago" // Less than 1 day
    else -> "${diff / 86_400_000}d ago" // Days
  }
}

internal fun formatJson(json: String): String = try {
  val element = Json.parseToJsonElement(json)
  JSON_FORMATTER.encodeToString(JsonElement.serializer(), element)
} catch (_: Exception) {
  json
}
