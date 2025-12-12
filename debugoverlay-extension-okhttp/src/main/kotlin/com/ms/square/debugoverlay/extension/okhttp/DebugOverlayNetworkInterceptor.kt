package com.ms.square.debugoverlay.extension.okhttp

import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.NetworkRequestTracker
import com.ms.square.debugoverlay.extension.okhttp.internal.isProbablyUtf8
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import com.ms.square.debugoverlay.internal.data.EvictingQueue
import com.ms.square.debugoverlay.model.NetworkError
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import okio.GzipSource
import java.io.IOException
import java.net.HttpURLConnection.HTTP_BAD_GATEWAY
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.net.HttpURLConnection.HTTP_UNAVAILABLE
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * Default maximum body size (2MB) before truncation.
 * Prevents OOM when capturing large responses.
 */
public const val DEFAULT_MAX_BODY_SIZE: Long = 2 * 1024 * 1024 // 2MB

/**
 * Number of UTF-8 code points to sample when detecting binary vs text content.
 */
private const val UTF8_DETECTION_CODE_POINTS: Long = 16L

public val DEFAULT_HEADERS_REDACT: Set<String> = setOf(
  "authorization",
  "api-key",
  "x-api-key",
  "cookie",
  "set-cookie",
  "x-auth-token",
  "x-csrf-token",
  "x-session-id",
  "proxy-authorization",
  "x-access-token"
)

public val DEFAULT_QUERY_PARAMS_REDACT: Set<String> = setOf(
  "token",
  "key",
  "password"
)

private const val HTTP_CLIENT_ERROR_START = 400
private const val HTTP_CLIENT_ERROR_END = 499
private const val HTTP_SERVER_ERROR_START = 500
private const val HTTP_SERVER_ERROR_END = 599

/**
 * OkHttp interceptor that captures network requests including headers and bodies for DebugOverlay.
 * **Note:** Automatically registers with [DebugOverlay] on creation.
 * Recommended to use as a singleton via DI (e.g., Hilt) to avoid duplicate registrations as
 * creating multiple instances will cause each instance to overwrite the previous registration.
 *
 * **Initialization:** DebugOverlay is installed automatically via AndroidX Startup before
 * Application.onCreate(). This interceptor can be constructed at any point (including before
 * installation) - configuration will be applied when DebugOverlay becomes available.
 *
 * Usage:
 * ```kotlin
 * val debugOverlayNetworkInterceptor: Interceptor = DebugOverlayNetworkInterceptor()
 *
 * val okHttpClient = OkHttpClient.Builder()
 *     .addNetworkInterceptor(debugOverlayNetworkInterceptor) or .addInterceptor(debugOverlayNetworkInterceptor)
 *     .build()
 * ```
 */
@OptIn(InternalDebugOverlayApi::class)
public class DebugOverlayNetworkInterceptor(
  maxStoredRequests: Int = 100,
  private val headersNameToRedact: Set<String> = DEFAULT_HEADERS_REDACT,
  private val queryParamsNameToRedact: Set<String> = DEFAULT_QUERY_PARAMS_REDACT,
  private val maxBodySize: Long = DEFAULT_MAX_BODY_SIZE,
) : Interceptor,
  NetworkRequestTracker {

  private val entries = EvictingQueue<NetworkRequest>(maxStoredRequests)
  private val _requests = MutableStateFlow<List<NetworkRequest>>(emptyList())

  init {
    DebugOverlay.configure {
      copy(networkRequestTracker = this@DebugOverlayNetworkInterceptor)
    }
  }

  override val requests: Flow<List<NetworkRequest>> = _requests.asStateFlow()

  @Suppress("TooGenericExceptionCaught")
  override fun intercept(chain: Interceptor.Chain): Response {
    val protocol = chain.connection()?.protocol()?.toString()
    val request = chain.request()

    // Capture request data
    val requestData = captureRequestData(request)

    // Execute request
    val startNs = System.nanoTime()
    val response: Response = try {
      chain.proceed(request)
    } catch (e: Exception) {
      // Network failure
      val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
      // Track failed request
      addRequest(
        protocol = protocol,
        method = request.method,
        url = request.url,
        durationMs = tookMs,
        requestData = requestData,
        error = NetworkError(
          title = "Network Error",
          message = "Failed to connect: ${e.message}",
          stackTrace = e.stackTraceToString()
        )
      )
      throw e
    }

    val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
    // Capture response data
    val responseData = captureResponseData(response)

    // Track successful request
    addRequest(
      protocol = protocol,
      method = request.method,
      url = request.url,
      durationMs = tookMs,
      statusCode = response.code,
      requestData = requestData,
      responseData = responseData,
      error = if (response.code >= HTTP_CLIENT_ERROR_START) {
        createErrorFromResponse(response, responseData.content)
      } else {
        null
      }
    )

    return response
  }

  private fun captureHeaders(headers: Headers): Map<String, String> = headers.names().associateWith { name ->
    if (headersNameToRedact.any { it.equals(name, ignoreCase = true) }) {
      "[REDACTED]"
    } else {
      headers[name] ?: ""
    }
  }

  private fun captureRequestData(request: Request): NetworkData {
    val requestHeaders = captureHeaders(request.headers)
    val requestContentType = captureRequestContentType(request)
    val requestContentLength = captureRequestContentLength(request)

    val body = request.body ?: return NetworkData(
      headers = requestHeaders,
      contentType = requestContentType,
      contentSize = requestContentLength,
      content = null
    )

    if (bodyHasUnknownEncoding(request.headers)) {
      return NetworkData(
        headers = requestHeaders,
        contentType = requestContentType,
        contentSize = requestContentLength,
        content = "N/A - [unknown content encoding body omitted]"
      )
    }

    // Bidirectional streaming
    if (body.isDuplex()) {
      return NetworkData(
        headers = requestHeaders,
        contentType = requestContentType,
        contentSize = requestContentLength,
        content = "N/A - [duplex request body omitted]"
      )
    }

    // Can only be read once
    if (body.isOneShot()) {
      return NetworkData(
        headers = requestHeaders,
        contentType = requestContentType,
        contentSize = requestContentLength,
        content = "N/A - [one-shot body omitted]"
      )
    }

    // Early size check to avoid buffering large requests
    if (requestContentLength != null && requestContentLength > maxBodySize) {
      return NetworkData(
        headers = requestHeaders,
        contentType = requestContentType,
        contentSize = requestContentLength,
        content = "N/A - [body ($requestContentType) too large: $requestContentLength-byte body omitted]"
      )
    }

    return captureRequestBodyContent(
      body = body,
      headers = request.headers,
      requestHeaders = requestHeaders,
      requestContentType = requestContentType,
      requestContentLength = requestContentLength
    )
  }

  private fun captureRequestBodyContent(
    body: RequestBody,
    headers: Headers,
    requestHeaders: Map<String, String>,
    requestContentType: String?,
    requestContentLength: Long?,
  ): NetworkData {
    var buffer = Buffer()
    body.writeTo(buffer)

    var gzippedLength: Long? = null
    if ("gzip".equals(headers["Content-Encoding"], ignoreCase = true)) {
      gzippedLength = buffer.size
      try {
        val decompressed = Buffer()
        GzipSource(buffer).use { gzippedSource ->
          decompressed.writeAll(gzippedSource)
        }
        buffer = decompressed
      } catch (e: IOException) {
        return NetworkData(
          headers = requestHeaders,
          contentType = requestContentType,
          contentSize = gzippedLength,
          content = "N/A - [failed to decompress gzip: ${e.message}]"
        )
      }
    }

    // Reports compressed size when gzipped
    val contentSizeToReport = gzippedLength ?: requestContentLength

    if (!buffer.isProbablyUtf8(UTF8_DETECTION_CODE_POINTS)) {
      return NetworkData(
        headers = requestHeaders,
        contentType = requestContentType,
        contentSize = contentSizeToReport,
        content = "N/A - [binary ${buffer.size}-byte $requestContentType body omitted]"
      )
    }

    return if (buffer.size > maxBodySize) {
      NetworkData(
        headers = requestHeaders,
        contentType = requestContentType,
        contentSize = contentSizeToReport,
        content = "N/A - [raw body too large: ${buffer.size}-byte body omitted]"
      )
    } else {
      NetworkData(
        headers = requestHeaders,
        contentType = requestContentType,
        contentSize = contentSizeToReport,
        content = buffer.readString(charset = body.contentType().charsetOrUtf8())
      )
    }
  }

  private fun captureResponseData(response: Response): NetworkData {
    val responseHeaders = captureHeaders(response.headers)
    val responseContentType = captureResponseContentType(response)
    val responseContentLength = captureResponseContentLength(response)

    if (!response.promisesBody()) {
      return NetworkData(
        headers = responseHeaders,
        contentType = responseContentType,
        contentSize = responseContentLength,
        content = null
      )
    }

    if (bodyHasUnknownEncoding(response.headers)) {
      return NetworkData(
        headers = responseHeaders,
        contentType = responseContentType,
        contentSize = responseContentLength,
        content = "N/A - [unknown content encoding body omitted]"
      )
    }

    if (bodyIsStreaming(response)) {
      return NetworkData(
        headers = responseHeaders,
        contentType = responseContentType,
        contentSize = responseContentLength,
        content = "N/A - [streaming body omitted]"
      )
    }

    // Early size check to avoid buffering large responses
    if (responseContentLength != null && responseContentLength > maxBodySize) {
      return NetworkData(
        headers = responseHeaders,
        contentType = responseContentType,
        contentSize = responseContentLength,
        content = "N/A - [body ($responseContentType) too large: $responseContentLength-byte body omitted]"
      )
    }

    return captureResponseBodyContent(
      response = response,
      responseHeaders = responseHeaders,
      responseContentType = responseContentType,
      responseContentLength = responseContentLength
    )
  }

  private fun captureResponseBodyContent(
    response: Response,
    responseHeaders: Map<String, String>,
    responseContentType: String?,
    responseContentLength: Long?,
  ): NetworkData {
    val body = response.body
    val source = body.source()
    val maxBytesToBuffer = minOf(Long.MAX_VALUE, maxBodySize + 1) // +1 to detect overflow
    source.request(maxBytesToBuffer)
    if (source.buffer.size > maxBodySize) {
      return NetworkData(
        headers = responseHeaders,
        contentType = responseContentType,
        contentSize = source.buffer.size,
        content = "N/A - [response too large: ${source.buffer.size}-byte body omitted]"
      )
    }

    // Clone buffer to preserve original for OkHttp to read
    var buffer = source.buffer.clone()
    var gzippedLength: Long? = null

    if ("gzip".equals(response.headers["Content-Encoding"], ignoreCase = true)) {
      gzippedLength = buffer.size
      try {
        GzipSource(buffer).use { gzippedSource ->
          buffer = Buffer()
          buffer.writeAll(gzippedSource)
        }
      } catch (e: IOException) {
        return NetworkData(
          headers = responseHeaders,
          contentType = responseContentType,
          contentSize = gzippedLength,
          content = "N/A - [failed to decompress gzip: ${e.message}]"
        )
      }
    }

    // Reports compressed size when gzipped
    val contentSizeToReport = gzippedLength ?: responseContentLength

    if (!buffer.isProbablyUtf8(UTF8_DETECTION_CODE_POINTS)) {
      return NetworkData(
        headers = responseHeaders,
        contentType = responseContentType,
        contentSize = contentSizeToReport,
        content = "N/A - [binary ${buffer.size}-byte $responseContentType body omitted]"
      )
    }

    return if (buffer.size > maxBodySize) {
      NetworkData(
        headers = responseHeaders,
        contentType = responseContentType,
        contentSize = contentSizeToReport,
        content = "N/A - [raw body too large: ${buffer.size}-byte body omitted]"
      )
    } else {
      NetworkData(
        headers = responseHeaders,
        contentType = responseContentType,
        contentSize = contentSizeToReport,
        content = buffer.readString(charset = body.contentType().charsetOrUtf8())
      )
    }
  }

  @Suppress("LongParameterList")
  private fun addRequest(
    protocol: String?,
    method: String,
    url: HttpUrl,
    statusCode: Int? = null,
    durationMs: Long,
    requestData: NetworkData? = null,
    responseData: NetworkData? = null,
    error: NetworkError? = null,
  ) {
    val redactUrl = redactUrl(url)
    val newRequest = NetworkRequest(
      protocol = protocol,
      method = method,
      url = redactUrl.toString(),
      statusCode = statusCode,
      durationMs = durationMs,
      responseSize = responseData?.contentSize,
      requestSize = requestData?.contentSize,
      requestHeaders = requestData?.headers ?: emptyMap(),
      responseHeaders = responseData?.headers ?: emptyMap(),
      requestBody = requestData?.content,
      responseBody = responseData?.content,
      timestamp = System.currentTimeMillis(),
      error = error
    )

    entries.add(newRequest)
    _requests.value = entries.toList()
  }

  private fun redactUrl(url: HttpUrl): HttpUrl {
    if (queryParamsNameToRedact.isEmpty() || url.querySize == 0) {
      return url
    }
    return url
      .newBuilder()
      .query(null)
      .apply {
        for (i in 0 until url.querySize) {
          val parameterName = url.queryParameterName(i)
          val newValue = if (queryParamsNameToRedact.any { it.equals(parameterName, ignoreCase = true) }) {
            "[REDACTED]"
          } else {
            url.queryParameterValue(i)
          }
          addEncodedQueryParameter(parameterName, newValue)
        }
      }.build()
  }
}

private fun captureRequestContentType(request: Request): String? {
  val headers = request.headers
  val requestBody = request.body
  return requestBody?.contentType()?.toString() ?: headers["Content-Type"]
}

private fun captureRequestContentLength(request: Request): Long? {
  val headers = request.headers
  val requestBody = request.body
  // NOTE: this could return -1 in certain cases (e.g., streaming)
  return requestBody?.contentLength()?.takeIf { it >= 0 } ?: headers["Content-Length"]?.toLongOrNull()
}

private fun captureResponseContentType(response: Response): String? {
  val headers = response.headers
  val responseBody = response.body
  return responseBody.contentType()?.toString() ?: headers["Content-Type"]
}

private fun captureResponseContentLength(response: Response): Long? {
  val headers = response.headers
  val responseBody = response.body
  // NOTE: this could return -1 in certain cases (e.g., streaming)
  return responseBody.contentLength().takeIf { it >= 0 } ?: headers["Content-Length"]?.toLongOrNull()
}

private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
  val contentEncoding = headers["Content-Encoding"] ?: return false
  return !contentEncoding.equals("identity", ignoreCase = true) &&
    !contentEncoding.equals("gzip", ignoreCase = true)
}

private fun bodyIsStreaming(response: Response): Boolean {
  val contentType = response.body.contentType()
  return contentType != null && contentType.type == "text" && contentType.subtype == "event-stream"
}

private fun createErrorFromResponse(response: Response, body: String?): NetworkError {
  val statusCode = response.code
  val statusMessage = when (statusCode) {
    HTTP_BAD_REQUEST -> "Bad Request"
    HTTP_UNAUTHORIZED -> "Unauthorized"
    HTTP_FORBIDDEN -> "Forbidden"
    HTTP_NOT_FOUND -> "Not Found"
    HTTP_INTERNAL_ERROR -> "Internal Server Error"
    HTTP_BAD_GATEWAY -> "Bad Gateway"
    HTTP_UNAVAILABLE -> "Service Unavailable"
    else -> "Error"
  }

  return NetworkError(
    title = "$statusCode $statusMessage",
    message = when (statusCode) {
      in HTTP_CLIENT_ERROR_START..HTTP_CLIENT_ERROR_END -> "Client error: The request was invalid or cannot be served."
      in HTTP_SERVER_ERROR_START..HTTP_SERVER_ERROR_END -> "Server error: The server failed to fulfill a valid request."
      else -> "Request failed with status $statusCode"
    },
    stackTrace = body
  )
}

private fun MediaType?.charsetOrUtf8(): Charset = this?.charset() ?: Charsets.UTF_8

private data class NetworkData(
  val headers: Map<String, String>,
  val contentType: String?,
  val contentSize: Long?,
  val content: String?,
)
