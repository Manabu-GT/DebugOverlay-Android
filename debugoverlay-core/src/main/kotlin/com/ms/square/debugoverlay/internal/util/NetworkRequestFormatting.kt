package com.ms.square.debugoverlay.internal.util

import com.ms.square.debugoverlay.internal.data.TextType
import com.ms.square.debugoverlay.model.NetworkError
import com.ms.square.debugoverlay.model.NetworkRequest

/**
 * Format the full request/response transaction as plain text for clipboard copy.
 * Mirrors the Overview/Headers/Body sections shown on the network request detail screen,
 * omitting any section that has nothing to show. Response headers/body are included whenever
 * captured, even alongside an error section - the OkHttp extension populates [NetworkRequest]'s
 * `error` for any HTTP status of 400 or above while still capturing the response.
 */
internal fun NetworkRequest.toClipboardText(): String = buildString {
  appendLine("$method $url")
  appendLine("Status: ${statusCode?.let { "$it ${it.httpStatusMessage}" } ?: "Error"}")
  appendLine("Duration: $durationMs ms")
  appendLine("Timestamp: ${formatClipboardTimestamp(timestampMs)}")
  appendLine("Request Size: ${formatBytes(requestSize)}")
  append("Response Size: ${formatBytes(responseSize)}")

  appendHeadersSection("Request Headers", requestHeaders)
  appendBodySection("Request Body", requestBody, requestHeaders.contentType())

  error?.let { appendErrorSection(it) }
  appendHeadersSection("Response Headers", responseHeaders)
  appendBodySection("Response Body", responseBody, responseHeaders.contentType())
}

private fun StringBuilder.appendHeadersSection(title: String, headers: Map<String, String>) {
  if (headers.isEmpty()) return
  append("\n\n--- $title ---\n")
  append(headers.entries.joinToString("\n") { (name, value) -> "$name: $value" })
}

private fun StringBuilder.appendBodySection(title: String, body: String?, contentType: String?) {
  if (body.isNullOrEmpty()) return
  append("\n\n--- $title ---\n")
  val formatted = if (TextType.from(body, contentType) == TextType.JSON) formatJsonIfPossible(body) else body
  append(formatted.truncateForClipboard())
}

private fun StringBuilder.appendErrorSection(error: NetworkError) {
  append("\n\n--- Error ---\n")
  append(error.title)
  append('\n')
  append(error.message)
  error.stackTrace?.let {
    append('\n')
    append(it)
  }
}

/**
 * Caps a single body at [MAX_CLIPBOARD_BODY_LENGTH]. The OkHttp extension allows bodies up to
 * 2MB each by default; concatenating an uncapped request and response body into one ClipData
 * risks TransactionTooLargeException when it crosses the clipboard's Binder call.
 */
private fun String.truncateForClipboard(): String = if (length <= MAX_CLIPBOARD_BODY_LENGTH) {
  this
} else {
  val shownSize = formatBytes(MAX_CLIPBOARD_BODY_LENGTH.toLong())
  val totalSize = formatBytes(length.toLong())
  "${take(MAX_CLIPBOARD_BODY_LENGTH)}...\n\n[truncated: showing $shownSize of $totalSize]"
}

private const val MAX_CLIPBOARD_BODY_LENGTH = 64 * 1024
