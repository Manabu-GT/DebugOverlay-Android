package com.ms.square.debugoverlay.internal.data.model

/**
 * Data class representing a single log entry.
 */
internal data class LogcatEntry(
  val id: Long,
  val timestamp: String,
  val level: LogLevel,
  val tag: String,
  val pid: Int,
  val tid: Int,
  val threadName: String,
  val message: String,
  val rawLine: String,
)

internal enum class LogLevel {
  VERBOSE,
  DEBUG,
  INFO,
  WARN,
  ERROR,
  ;

  companion object {
    fun fromString(string: String): LogLevel = when (string.uppercase()) {
      "V" -> VERBOSE
      "D" -> DEBUG
      "I" -> INFO
      "W" -> WARN
      "E" -> ERROR
      else -> DEBUG
    }
  }
}
