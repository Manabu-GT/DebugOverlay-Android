package com.ms.square.debugoverlay.model

import java.util.UUID

/**
 * Data class for network request.
 */
public data class NetworkRequest(
  val id: String = "${UUID.randomUUID()}",
  val method: HttpMethod, // GET, POST, etc.
  val fullUrl: String, // https://test.com/api/v1/feed
  val shortUrl: String, // /api/v1/feed
  val statusCode: Int?, // 200, 404, etc.
  val durationMs: Long, // 245
  val responseSize: Long, // bytes
  val requestSize: Long = 0, // bytes
  val timestamp: Long = System.currentTimeMillis(),
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
