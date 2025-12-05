package com.ms.square.debugoverlay.internal.util

import androidx.compose.ui.graphics.Color

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
