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
 *
 * @param maxClipBoardLength per-body character cap before truncation, defaulting to
 *   [MAX_CLIPBOARD_BODY_LENGTH].
 */
internal fun NetworkRequest.toClipboardText(maxClipBoardLength: Int = MAX_CLIPBOARD_BODY_LENGTH): String = buildString {
  appendLine("$method $url")
  appendLine("Status: ${statusCode?.let { "$it ${it.httpStatusMessage}" } ?: "Error"}")
  appendLine("Duration: $durationMs ms")
  appendLine("Timestamp: ${formatClipboardTimestamp(timestampMs)}")
  appendLine("Request Size: ${formatBytes(requestSize)}")
  append("Response Size: ${formatBytes(responseSize)}")

  appendHeadersSection("Request Headers", requestHeaders)
  appendBodySection("Request Body", requestBody, requestHeaders.contentType(), maxClipBoardLength)

  error?.let { appendErrorSection(it) }
  appendHeadersSection("Response Headers", responseHeaders)
  appendBodySection("Response Body", responseBody, responseHeaders.contentType(), maxClipBoardLength)
}

private fun StringBuilder.appendHeadersSection(title: String, headers: Map<String, String>) {
  if (headers.isEmpty()) return
  append("\n\n--- $title ---\n")
  append(headers.entries.joinToString("\n") { (name, value) -> "$name: $value" })
}

private fun StringBuilder.appendBodySection(
  title: String,
  body: String?,
  contentType: String?,
  maxClipBoardLength: Int,
) {
  if (body.isNullOrEmpty()) return
  append("\n\n--- $title ---\n")
  val formatted = if (TextType.from(body, contentType) == TextType.JSON) formatJsonIfPossible(body) else body
  append(formatted.truncateForClipboard(maxClipBoardLength))
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
 * Caps a single body at [maxClipBoardLength]. The OkHttp extension allows bodies up to
 * 2MB each by default; concatenating an uncapped request and response body into one ClipData
 * risks TransactionTooLargeException when it crosses the clipboard's Binder call.
 */
private fun String.truncateForClipboard(maxClipBoardLength: Int): String = if (length <= maxClipBoardLength) {
  this
} else {
  val shownSize = formatBytes(maxClipBoardLength.toLong())
  val totalSize = formatBytes(length.toLong())
  "${take(maxClipBoardLength)}...\n\n[truncated: showing $shownSize of $totalSize]"
}

private const val MAX_CLIPBOARD_BODY_LENGTH = 64 * 1024
