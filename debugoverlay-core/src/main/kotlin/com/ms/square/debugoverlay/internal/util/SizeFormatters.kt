package com.ms.square.debugoverlay.internal.util

private const val BYTES_PER_KB = 1024L
private const val BYTES_PER_MB = 1024L * 1024L
private const val BYTES_PER_GB = 1024L * 1024L * 1024L
private const val KB_PER_MB = 1024L

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
 * Format kilobytes to human-readable string.
 */
internal fun formatBytesFromKb(kb: Long?): String = formatBytes(kb?.times(BYTES_PER_KB))

/**
 * Format text size to human-readable string.
 */
internal fun formatTextSize(length: Int): String = when {
  length < BYTES_PER_KB -> "$length chars"
  length < BYTES_PER_MB -> "${length / BYTES_PER_KB} KB"
  else -> "${"%.1f".format(length / (BYTES_PER_MB.toDouble()))} MB"
}

/**
 * Format memory in KB to MB string, returning "N/A" if value is 0 (not captured).
 * Uses 1 decimal place for values under 10 MB for better precision near thresholds.
 */
internal fun formatMemoryKbToMb(valueKb: Long): String {
  if (valueKb <= 0) return "N/A"
  val mb = valueKb / KB_PER_MB.toDouble()
  @Suppress("MagicNumber")
  return if (mb < 10) "%.1f MB".format(mb) else "${mb.toLong()} MB"
}
