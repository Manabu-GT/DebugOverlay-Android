package com.ms.square.debugoverlay.model

import android.util.Log
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Data class representing a single log entry.
 *
 * This class supports kotlinx.serialization for export and custom integrations.
 *
 * **Security considerations:** The [message] field is displayed directly in the
 * debug overlay UI. Callers should sanitize or redact sensitive data (PII,
 * credentials, tokens, etc.) before constructing LogEntry instances, especially
 * in builds that may be shared with testers or captured in screen recordings.
 */
@Serializable
public data class LogEntry(
  val id: String = UUID.randomUUID().toString(),
  val timestampMs: Long,
  val level: LogLevel,
  val tag: String,
  val pid: Int,
  val tid: Int,
  val threadName: String,
  val message: String,
)

@Serializable
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
      "E", "F" -> ERROR // F = FATAL, mapped to ERROR
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
