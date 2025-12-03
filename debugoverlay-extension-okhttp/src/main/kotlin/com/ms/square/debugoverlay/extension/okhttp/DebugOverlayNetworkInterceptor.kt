package com.ms.square.debugoverlay.extension.okhttp

import com.ms.square.debugoverlay.NetworkRequestTracker
import com.ms.square.debugoverlay.model.HttpMethod
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

public class DebugOverlayNetworkInterceptor(private val maxStoredRequests: Int = 100) :
  Interceptor,
  NetworkRequestTracker {

  private val _requests = MutableStateFlow<List<NetworkRequest>>(emptyList())

  override val requests: Flow<List<NetworkRequest>> = _requests.asStateFlow()

  @Suppress("TooGenericExceptionCaught")
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val startTime = System.currentTimeMillis()

    // Get request size. contentLength returns -1 if unknown (streaming)
    val requestSize = request.body?.contentLength() ?: -1

    // Execute request
    val response: Response
    val statusCode: Int
    val responseSize: Long

    try {
      response = chain.proceed(request)
      statusCode = response.code

      // Get response size from Content-Length header or body
      // NOTE: this could return -1 for chunked/streaming responses
      responseSize = (
        response.header("Content-Length")?.toLongOrNull()
          ?: response.body.contentLength()
        )
    } catch (e: IOException) {
      // Network failure - record with 0 status code
      val endTime = System.currentTimeMillis()
      addRequest(
        method = request.method,
        url = request.url,
        statusCode = null,
        durationMs = endTime - startTime,
        responseSize = 0,
        requestSize = requestSize
      )
      throw e
    } catch (e: Exception) {
      // Other failure
      val endTime = System.currentTimeMillis()
      addRequest(
        method = request.method,
        url = request.url,
        statusCode = null,
        durationMs = endTime - startTime,
        responseSize = 0,
        requestSize = requestSize
      )
      throw e
    }

    val endTime = System.currentTimeMillis()

    // Record successful request
    addRequest(
      method = request.method,
      url = request.url,
      statusCode = statusCode,
      durationMs = endTime - startTime,
      responseSize = responseSize,
      requestSize = requestSize
    )

    return response
  }

  private fun addRequest(
    method: String,
    url: HttpUrl,
    statusCode: Int?,
    durationMs: Long,
    responseSize: Long,
    requestSize: Long,
  ) {
    val newRequest = NetworkRequest(
      method = method.toHttpMethod(),
      fullUrl = url.sanitizedUrl(),
      shortUrl = url.sanitizedUrl(fullUrl = false),
      statusCode = statusCode,
      durationMs = durationMs,
      responseSize = responseSize,
      requestSize = requestSize,
      timestamp = System.currentTimeMillis()
    )

    // Keep only the last N requests
    _requests.update { currentList ->
      (currentList + newRequest).takeLast(maxStoredRequests)
    }
  }
}

private fun String.toHttpMethod(): HttpMethod = when (this) {
  "GET" -> HttpMethod.GET
  "POST" -> HttpMethod.POST
  "PUT" -> HttpMethod.PUT
  "DELETE" -> HttpMethod.DELETE
  "PATCH" -> HttpMethod.PATCH
  "HEAD" -> HttpMethod.HEAD
  "OPTIONS" -> HttpMethod.OPTIONS
  "TRACE" -> HttpMethod.TRACE
  else -> HttpMethod.UNKNOWN
}

private val SENSITIVE_URL_PATTERN = Regex("(token|key|password)=[^&]+")

private fun HttpUrl.sanitizedUrl(fullUrl: Boolean = true): String {
  val rawUrl = if (fullUrl) {
    toString()
  } else {
    encodedPath + (encodedQuery?.let { "?$it" } ?: "")
  }
  return rawUrl.replace(SENSITIVE_URL_PATTERN, "$1=***")
}
