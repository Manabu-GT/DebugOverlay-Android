package com.ms.square.debugoverlay.internal.util

import com.ms.square.debugoverlay.internal.data.TextType
import com.ms.square.debugoverlay.model.NetworkError
import com.ms.square.debugoverlay.model.NetworkRequest

/**
 * Format the full request/response transaction as plain text for clipboard copy.
 * Mirrors the Overview/Headers/Body sections shown on the network request detail screen,
 * omitting any section that has nothing to show.
 */
internal fun NetworkRequest.toClipboardText(): String = buildString {
  appendLine("$method $url")
  appendLine("Status: ${statusCode?.let { "$it ${it.httpStatusMessage}" } ?: "Error"}")
  appendLine("Duration: $durationMs ms")
  appendLine("Timestamp: ${formatTimestamp(timestampMs)}")
  appendLine("Request Size: ${formatBytes(requestSize)}")
  append("Response Size: ${formatBytes(responseSize)}")

  appendHeadersSection("Request Headers", requestHeaders)
  appendBodySection("Request Body", requestBody, requestHeaders["content-type"])

  if (error != null) {
    appendErrorSection(error)
  } else {
    appendHeadersSection("Response Headers", responseHeaders)
    appendBodySection("Response Body", responseBody, responseHeaders["content-type"])
  }
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
  append(formatted)
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
