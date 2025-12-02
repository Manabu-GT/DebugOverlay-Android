package com.ms.square.debugoverlay.internal.data.model

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

// Log level colors for light theme
private val VERBOSE_COLOR_LIGHT = Color(0xFF757575)
private val DEBUG_COLOR_LIGHT = Color(0xFF2196F3)
private val INFO_COLOR_LIGHT = Color(0xFF4CAF50)
private val WARN_COLOR_LIGHT = Color(0xFFFF9800)
private val ERROR_COLOR_LIGHT = Color(0xFFF44336)

// Log level colors for dark theme
private val VERBOSE_COLOR_DARK = Color(0xFFBDBDBD)
private val DEBUG_COLOR_DARK = Color(0xFF64B5F6)
private val INFO_COLOR_DARK = Color(0xFF81C784)
private val WARN_COLOR_DARK = Color(0xFFFFB74D)
private val ERROR_COLOR_DARK = Color(0xFFE57373)

/**
 * Get the color for this log level based on the current theme.
 */
@Composable
internal fun LogLevel.toColor(): Color {
  val isDark = isSystemInDarkTheme()
  return when (this) {
    LogLevel.VERBOSE -> if (isDark) VERBOSE_COLOR_DARK else VERBOSE_COLOR_LIGHT
    LogLevel.DEBUG -> if (isDark) DEBUG_COLOR_DARK else DEBUG_COLOR_LIGHT
    LogLevel.INFO -> if (isDark) INFO_COLOR_DARK else INFO_COLOR_LIGHT
    LogLevel.WARN -> if (isDark) WARN_COLOR_DARK else WARN_COLOR_LIGHT
    LogLevel.ERROR -> if (isDark) ERROR_COLOR_DARK else ERROR_COLOR_LIGHT
  }
}
