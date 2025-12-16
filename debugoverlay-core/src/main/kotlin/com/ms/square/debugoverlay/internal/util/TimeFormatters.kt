package com.ms.square.debugoverlay.internal.util

import androidx.annotation.MainThread
import com.ms.square.debugoverlay.model.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MILLIS_PER_SECOND = 1000L
private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_HOUR = 3_600_000L
private const val MILLIS_PER_DAY = 86_400_000L

// cache given its frequent usage on the main/UI thread
@get:MainThread
private val TIMESTAMP_FORMATTER = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

/**
 * Should only be called on the main/UI thread as it uses a non-thread safe formatter.
 */
@MainThread
internal fun formatTimestamp(timestamp: Long): String = TIMESTAMP_FORMATTER.format(Date(timestamp))

/**
 * Format timestamp for clipboard copy (e.g., "12-11 14:35:22.786").
 */
private fun formatClipboardTimestamp(timestamp: Long): String =
  SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date(timestamp))

/**
 * Format log entry for clipboard copy.
 * Output format: "MM-dd HH:mm:ss.SSS PID TID LEVEL TAG: message"
 */
internal fun LogEntry.toClipboardText(): String =
  "${formatClipboardTimestamp(timestampMs)} $pid $tid ${level.name.first()} $tag: $message"

/**
 * Format timestamp as relative time (e.g., "2s ago", "5m ago", "2h ago").
 */
internal fun formatRelativeTime(timestamp: Long): String {
  val now = System.currentTimeMillis()
  val diff = now - timestamp

  return when {
    diff < MILLIS_PER_SECOND -> "just now" // Less than 1 second
    diff < MILLIS_PER_MINUTE -> "${diff / MILLIS_PER_SECOND}s ago" // Less than 1 minute
    diff < MILLIS_PER_HOUR -> "${diff / MILLIS_PER_MINUTE}m ago" // Less than 1 hour
    diff < MILLIS_PER_DAY -> "${diff / MILLIS_PER_HOUR}h ago" // Less than 1 day
    else -> "${diff / MILLIS_PER_DAY}d ago" // Days
  }
}

/**
 * Format timestamp as full date/time string (e.g., "Dec 11, 2025 at 3:45:30 PM").
 */
internal fun formatFullTimestamp(timestamp: Long): String =
  SimpleDateFormat("MMM d, yyyy 'at' h:mm:ss a", Locale.US).format(Date(timestamp))

/**
 * Format timestamp for filenames (e.g., "20251215_143045").
 * Safe for use in filenames on all platforms.
 */
internal fun formatFilenameTimestamp(timestampMs: Long): String =
  SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestampMs))
