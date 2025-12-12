package com.ms.square.debugoverlay.internal.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ms.square.debugoverlay.internal.data.model.AppExitReason
import com.ms.square.debugoverlay.internal.data.model.LogLevel

// HTTP method colors
private val METHOD_COLOR_GET = Color(0xFF03DAC6) // Cyan
private val METHOD_COLOR_POST = Color(0xFFFFC107) // Amber
private val METHOD_COLOR_PUT = Color(0xFF2196F3) // Blue
private val METHOD_COLOR_DELETE = Color(0xFFF44336) // Red
private val METHOD_COLOR_PATCH = Color(0xFF9C27B0) // Purple
private val METHOD_COLOR_HEAD = Color(0xFF009688) // Teal
private val METHOD_COLOR_OPTIONS = Color(0xFFBDBDBD) // Light gray
private val METHOD_COLOR_UNKNOWN = Color(0xFF757575) // Gray

internal val String.httpMethodColor: Color
  get() = when (this) {
    "GET" -> METHOD_COLOR_GET
    "POST" -> METHOD_COLOR_POST
    "PUT" -> METHOD_COLOR_PUT
    "DELETE" -> METHOD_COLOR_DELETE
    "PATCH" -> METHOD_COLOR_PATCH
    "HEAD" -> METHOD_COLOR_HEAD
    "OPTIONS" -> METHOD_COLOR_OPTIONS
    else -> METHOD_COLOR_UNKNOWN
  }

// Status code colors
private val STATUS_COLOR_SUCCESS = Color(0xFF4CAF50) // Green
private val STATUS_COLOR_REDIRECT = Color(0xFF2196F3) // Blue
private val STATUS_COLOR_CLIENT_ERROR = Color(0xFFF44336) // Red
private val STATUS_COLOR_SERVER_ERROR = Color(0xFFFF5722) // Deep orange
private val STATUS_COLOR_UNKNOWN = Color(0xFF757575) // Gray

internal val Int?.httpStatusColor: Color
  get() = when (this) {
    in HTTP_SUCCESS_START..HTTP_SUCCESS_END -> STATUS_COLOR_SUCCESS
    in HTTP_REDIRECT_START..HTTP_REDIRECT_END -> STATUS_COLOR_REDIRECT
    in HTTP_CLIENT_ERROR_START..HTTP_CLIENT_ERROR_END -> STATUS_COLOR_CLIENT_ERROR
    in HTTP_SERVER_ERROR_START..HTTP_SERVER_ERROR_END -> STATUS_COLOR_SERVER_ERROR
    else -> STATUS_COLOR_UNKNOWN
  }

// Log level colors for light theme
private val LOG_VERBOSE_LIGHT = Color(0xFF757575)
private val LOG_DEBUG_LIGHT = Color(0xFF2196F3)
private val LOG_INFO_LIGHT = Color(0xFF4CAF50)
private val LOG_WARN_LIGHT = Color(0xFFFF9800)
private val LOG_ERROR_LIGHT = Color(0xFFF44336)

// Log level colors for dark theme
private val LOG_VERBOSE_DARK = Color(0xFFBDBDBD)
private val LOG_DEBUG_DARK = Color(0xFF64B5F6)
private val LOG_INFO_DARK = Color(0xFF81C784)
private val LOG_WARN_DARK = Color(0xFFFFB74D)
private val LOG_ERROR_DARK = Color(0xFFE57373)

/**
 * Get the color for this log level based on the current theme.
 */
@Composable
internal fun LogLevel.toColor(): Color {
  val isDark = isSystemInDarkTheme()
  return when (this) {
    LogLevel.VERBOSE -> if (isDark) LOG_VERBOSE_DARK else LOG_VERBOSE_LIGHT
    LogLevel.DEBUG -> if (isDark) LOG_DEBUG_DARK else LOG_DEBUG_LIGHT
    LogLevel.INFO -> if (isDark) LOG_INFO_DARK else LOG_INFO_LIGHT
    LogLevel.WARN -> if (isDark) LOG_WARN_DARK else LOG_WARN_LIGHT
    LogLevel.ERROR -> if (isDark) LOG_ERROR_DARK else LOG_ERROR_LIGHT
  }
}

// Severity colors for light theme
private val SEVERITY_CRITICAL_LIGHT = Color(0xFFF44336)
private val SEVERITY_WARNING_LIGHT = Color(0xFFFF9800)
private val SEVERITY_INFO_LIGHT = Color(0xFF2196F3)

// Severity colors for dark theme
private val SEVERITY_CRITICAL_DARK = Color(0xFFE57373)
private val SEVERITY_WARNING_DARK = Color(0xFFFFB74D)
private val SEVERITY_INFO_DARK = Color(0xFF64B5F6)

/**
 * Get the color for this severity based on the current theme.
 */
@Composable
internal fun AppExitReason.Severity.toColor(): Color {
  val isDark = isSystemInDarkTheme()
  return when (this) {
    AppExitReason.Severity.CRITICAL -> if (isDark) SEVERITY_CRITICAL_DARK else SEVERITY_CRITICAL_LIGHT
    AppExitReason.Severity.WARNING -> if (isDark) SEVERITY_WARNING_DARK else SEVERITY_WARNING_LIGHT
    AppExitReason.Severity.INFO -> if (isDark) SEVERITY_INFO_DARK else SEVERITY_INFO_LIGHT
  }
}
