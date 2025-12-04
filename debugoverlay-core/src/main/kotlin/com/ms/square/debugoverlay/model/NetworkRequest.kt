package com.ms.square.debugoverlay.model

import java.util.UUID

/**
 * Data class for network request.
 */
public data class NetworkRequest(
  // Base NetworkRequest fields
  val id: String = UUID.randomUUID().toString(),
  val protocol: String?, // http/1.1, h2, quic..etc
  val method: HttpMethod, // GET, POST, etc.
  val fullUrl: String, // https://test.com/api/v1/feed
  val shortUrl: String, // /api/v1/feed
  val statusCode: Int?, // 200, 404, etc.
  val durationMs: Long, // 245
  val responseSize: Long?, // bytes
  val requestSize: Long?, // bytes
  val timestamp: Long = System.currentTimeMillis(),
  // Extended fields for detail screen
  val requestHeaders: Map<String, String> = emptyMap(),
  val responseHeaders: Map<String, String> = emptyMap(),
  val requestBody: String? = null,
  val responseBody: String? = null,
  val error: NetworkError? = null,
)

public enum class HttpMethod {
  GET,
  POST,
  PUT,
  DELETE,
  PATCH,
  HEAD,
  OPTIONS,
  TRACE,
  UNKNOWN,
}

/**
 * Error information for failed requests.
 */
public data class NetworkError(val title: String, val message: String, val stackTrace: String? = null)
