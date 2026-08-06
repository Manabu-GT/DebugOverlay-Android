package com.ms.square.debugoverlay.internal.util

import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.model.NetworkError
import com.ms.square.debugoverlay.model.NetworkRequest
import org.junit.Test

class NetworkRequestFormattingTest {

  @Test
  fun `toClipboardText includes all sections for a full request and response`() {
    val request = NetworkRequest(
      protocol = "h2",
      method = "POST",
      url = "https://api.example.com/v1/users",
      statusCode = 200,
      durationMs = 245,
      responseSize = 128,
      requestSize = 42,
      timestampMs = 1_700_000_000_000,
      requestHeaders = mapOf("content-type" to "application/json"),
      responseHeaders = mapOf("content-type" to "application/json"),
      requestBody = """{"name":"test"}""",
      responseBody = """{"result":"ok"}"""
    )

    val result = request.toClipboardText()

    assertThat(result).isEqualTo(
      """
      POST https://api.example.com/v1/users
      Status: 200 OK
      Duration: 245 ms
      Timestamp: ${formatClipboardTimestamp(request.timestampMs)}
      Request Size: 42 B
      Response Size: 128 B

      --- Request Headers ---
      content-type: application/json

      --- Request Body ---
      {
          "name": "test"
      }

      --- Response Headers ---
      content-type: application/json

      --- Response Body ---
      {
          "result": "ok"
      }
      """.trimIndent()
    )
  }

  @Test
  fun `toClipboardText omits headers and body sections when absent`() {
    val request = NetworkRequest(
      protocol = "http/1.1",
      method = "GET",
      url = "https://api.example.com/v1/ping",
      statusCode = 204,
      durationMs = 12,
      responseSize = 0,
      requestSize = 0,
      timestampMs = 1_700_000_000_000
    )

    val result = request.toClipboardText()

    assertThat(result).isEqualTo(
      """
      GET https://api.example.com/v1/ping
      Status: 204 No Content
      Duration: 12 ms
      Timestamp: ${formatClipboardTimestamp(request.timestampMs)}
      Request Size: 0 B
      Response Size: 0 B
      """.trimIndent()
    )
  }

  @Test
  fun `toClipboardText leaves non-JSON body unformatted`() {
    val request = NetworkRequest(
      protocol = "http/1.1",
      method = "GET",
      url = "https://api.example.com/v1/text",
      statusCode = 200,
      durationMs = 5,
      responseSize = 11,
      requestSize = 0,
      timestampMs = 1_700_000_000_000,
      responseBody = "plain text"
    )

    val result = request.toClipboardText()

    assertThat(result).contains("--- Response Body ---\nplain text")
  }

  @Test
  fun `toClipboardText detects JSON via a capitalized Content-Type header`() {
    val request = NetworkRequest(
      protocol = "http/1.1",
      method = "GET",
      url = "https://api.example.com/v1/data",
      statusCode = 200,
      durationMs = 10,
      responseSize = 20,
      requestSize = 0,
      timestampMs = 1_700_000_000_000,
      responseHeaders = mapOf("Content-Type" to "application/json"),
      responseBody = """{"a":1}"""
    )

    val result = request.toClipboardText()

    assertThat(result).contains(
      """
      --- Response Body ---
      {
          "a": 1
      }
      """.trimIndent()
    )
  }

  @Test
  fun `toClipboardText truncates a body larger than the clipboard cap`() {
    val hugeBody = "a".repeat(HUGE_BODY_LENGTH)
    val request = NetworkRequest(
      protocol = "http/1.1",
      method = "GET",
      url = "https://api.example.com/v1/large",
      statusCode = 200,
      durationMs = 10,
      responseSize = HUGE_BODY_LENGTH.toLong(),
      requestSize = 0,
      timestampMs = 1_700_000_000_000,
      responseBody = hugeBody
    )

    val result = request.toClipboardText()

    assertThat(result).contains("[truncated: showing 64.0 KB of")
    assertThat(result).doesNotContain(hugeBody)
  }

  @Test
  fun `toClipboardText shows error section for a transport failure with no response data`() {
    val request = NetworkRequest(
      protocol = "http/1.1",
      method = "GET",
      url = "https://api.example.com/v1/fail",
      statusCode = null,
      durationMs = 1000,
      responseSize = null,
      requestSize = 0,
      timestampMs = 1_700_000_000_000,
      error = NetworkError(
        title = "Connection failed",
        message = "Unable to resolve host",
        stackTrace = "java.net.UnknownHostException: api.example.com"
      )
    )

    val result = request.toClipboardText()

    assertThat(result).isEqualTo(
      """
      GET https://api.example.com/v1/fail
      Status: Error
      Duration: 1000 ms
      Timestamp: ${formatClipboardTimestamp(request.timestampMs)}
      Request Size: 0 B
      Response Size: —

      --- Error ---
      Connection failed
      Unable to resolve host
      java.net.UnknownHostException: api.example.com
      """.trimIndent()
    )
  }

  @Test
  fun `toClipboardText includes both error and response sections for an HTTP error status`() {
    val request = NetworkRequest(
      protocol = "http/1.1",
      method = "GET",
      url = "https://api.example.com/v1/resource",
      statusCode = 404,
      durationMs = 80,
      responseSize = 42,
      requestSize = 0,
      timestampMs = 1_700_000_000_000,
      responseHeaders = mapOf("content-type" to "application/json"),
      responseBody = """{"error":"not found"}""",
      error = NetworkError(title = "HTTP 404", message = "Not Found")
    )

    val result = request.toClipboardText()

    assertThat(result).isEqualTo(
      """
      GET https://api.example.com/v1/resource
      Status: 404 Not Found
      Duration: 80 ms
      Timestamp: ${formatClipboardTimestamp(request.timestampMs)}
      Request Size: 0 B
      Response Size: 42 B

      --- Error ---
      HTTP 404
      Not Found

      --- Response Headers ---
      content-type: application/json

      --- Response Body ---
      {
          "error": "not found"
      }
      """.trimIndent()
    )
  }
}

private const val HUGE_BODY_LENGTH = 70_000
