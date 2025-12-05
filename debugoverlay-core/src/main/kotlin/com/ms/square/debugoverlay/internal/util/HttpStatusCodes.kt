package com.ms.square.debugoverlay.internal.util

import java.net.HttpURLConnection.HTTP_BAD_GATEWAY
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_MOVED_PERM
import java.net.HttpURLConnection.HTTP_MOVED_TEMP
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.net.HttpURLConnection.HTTP_UNAVAILABLE

// HTTP Status Code ranges
internal const val HTTP_SUCCESS_START = 200
internal const val HTTP_SUCCESS_END = 299
internal const val HTTP_REDIRECT_START = 300
internal const val HTTP_REDIRECT_END = 399
internal const val HTTP_CLIENT_ERROR_START = 400
internal const val HTTP_CLIENT_ERROR_END = 499
internal const val HTTP_SERVER_ERROR_START = 500
internal const val HTTP_SERVER_ERROR_END = 599

private val HTTP_STATUS_MESSAGES = mapOf(
  HTTP_OK to "OK",
  HTTP_CREATED to "Created",
  HTTP_NO_CONTENT to "No Content",
  HTTP_MOVED_PERM to "Moved Permanently",
  HTTP_MOVED_TEMP to "Temporary Redirect",
  HTTP_NOT_MODIFIED to "Not Modified",
  HTTP_BAD_REQUEST to "Bad Request",
  HTTP_UNAUTHORIZED to "Unauthorized",
  HTTP_FORBIDDEN to "Forbidden",
  HTTP_NOT_FOUND to "Not Found",
  HTTP_INTERNAL_ERROR to "Internal Server Error",
  HTTP_BAD_GATEWAY to "Bad Gateway",
  HTTP_UNAVAILABLE to "Service Unavailable"
)

internal val Int.httpStatusMessage: String
  get() = HTTP_STATUS_MESSAGES[this] ?: ""
