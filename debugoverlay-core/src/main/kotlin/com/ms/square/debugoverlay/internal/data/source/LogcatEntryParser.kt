package com.ms.square.debugoverlay.internal.data.source

import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.LogLevel
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

// Matches epoch logcat format: "1733921286.215 11744 11744 D Tag: message"
private val LOGCAT_FORMAT_REGEX =
  """(\d+\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+([^:]+):\s*(.*)""".toRegex()

/**
 * Parser for logcat output in epoch format.
 *
 * Parses lines formatted with `-v threadtime,printable,epoch`:
 * ```
 * 1733921286.215 11744 11744 D MyTag: Hello World
 * ```
 *
 * Pattern: `EPOCH_SECONDS.mmm PID TID LEVEL TAG: MESSAGE`
 *
 * @param threadNameCache Cache for resolving thread names from pid/tid.
 */
internal class LogcatEntryParser(private val threadNameCache: ThreadNameCache = ThreadNameCache()) {

  /**
   * Parses a logcat line into a [LogEntry].
   *
   * @param line The raw logcat line to parse
   * @return Parsed [LogEntry] or null if the line doesn't match the expected format
   */
  @Suppress("DestructuringDeclarationWithTooManyEntries")
  fun parse(line: String): LogEntry? = LOGCAT_FORMAT_REGEX.matchEntire(line.trim())?.let { match ->
    val (epochStr, pid, tid, level, tag, message) = match.destructured
    val pidInt = pid.trim().toInt()
    val tidInt = tid.trim().toInt()
    val timestampMs = try {
      (epochStr.toDouble() * TimeUnit.SECONDS.toMillis(1)).roundToLong()
    } catch (_: NumberFormatException) {
      // Fallback to current time so the log at least shows up
      System.currentTimeMillis()
    }

    LogEntry(
      timestampMs = timestampMs,
      level = LogLevel.fromString(level),
      tag = tag.trim(),
      pid = pidInt,
      tid = tidInt,
      threadName = threadNameCache.resolve(pidInt, tidInt),
      message = message.trim()
    )
  }
}
