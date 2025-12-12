package com.ms.square.debugoverlay.model

import android.util.Log

/**
 * Data class representing a single log entry.
 */
public data class LogEntry(
  val id: Long,
  val timestampMs: Long,
  val level: LogLevel,
  val tag: String,
  val pid: Int,
  val tid: Int,
  val threadName: String,
  val message: String,
)

public enum class LogLevel {
  VERBOSE,
  DEBUG,
  INFO,
  WARN,
  ERROR,
  ;

  public companion object {
    public fun fromString(string: String): LogLevel = when (string.uppercase()) {
      "V" -> VERBOSE
      "D" -> DEBUG
      "I" -> INFO
      "W" -> WARN
      "E", "F" -> ERROR
      else -> DEBUG
    }

    public fun fromInt(priority: Int): LogLevel = when (priority) {
      Log.VERBOSE -> VERBOSE
      Log.DEBUG -> DEBUG
      Log.INFO -> INFO
      Log.WARN -> WARN
      Log.ERROR, Log.ASSERT -> ERROR
      else -> DEBUG
    }
  }
}
