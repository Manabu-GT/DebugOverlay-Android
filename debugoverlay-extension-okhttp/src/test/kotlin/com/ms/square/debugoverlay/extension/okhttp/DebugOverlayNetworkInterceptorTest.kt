package com.ms.square.debugoverlay.extension.okhttp

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.GzipSink
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DebugOverlayNetworkInterceptorTest {

  private val mockWebServer = MockWebServer()
  private val interceptor = DebugOverlayNetworkInterceptor(autoInstall = false)
  private val client = OkHttpClient.Builder()
    .addInterceptor(interceptor)
    .build()

  @Before
  fun setUp() {
    mockWebServer.start()
  }

  @After
  fun tearDown() {
    mockWebServer.shutdown()
  }

  @Test
  fun `intercept captures successful GET request`() = runTest {
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("""{"message": "hello"}""")
        .addHeader("Content-Type", "application/json")
    )

    val request = Request.Builder()
      .url(mockWebServer.url("/api/test"))
      .get()
      .build()

    client.newCall(request).execute().close()

    val requests = interceptor.requests.first()
    assertThat(requests).hasSize(1)

    val capturedRequest = requests.first()
    assertThat(capturedRequest.method).isEqualTo("GET")
    assertThat(capturedRequest.url).contains("/api/test")
    assertThat(capturedRequest.statusCode).isEqualTo(200)
    assertThat(capturedRequest.responseBody).isEqualTo("""{"message": "hello"}""")
    assertThat(capturedRequest.error).isNull()
  }

  @Test
  fun `intercept captures error response`() = runTest {
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(404)
        .setBody("Not Found")
    )

    val request = Request.Builder()
      .url(mockWebServer.url("/api/missing"))
      .get()
      .build()

    client.newCall(request).execute().close()

    val requests = interceptor.requests.first()
    assertThat(requests).hasSize(1)

    val capturedRequest = requests.first()
    assertThat(capturedRequest.statusCode).isEqualTo(404)
    assertThat(capturedRequest.error).isNotNull()
    assertThat(capturedRequest.error?.title).contains("404")
  }

  @Test
  fun `intercept redacts authorization header`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(200))

    val request = Request.Builder()
      .url(mockWebServer.url("/api/secure"))
      .addHeader("Authorization", "Bearer secret-token-12345")
      .get()
      .build()

    client.newCall(request).execute().close()

    val requests = interceptor.requests.first()
    assertThat(requests).hasSize(1)

    val capturedRequest = requests.first()
    assertThat(capturedRequest.requestHeaders["Authorization"]).isEqualTo("[REDACTED]")
  }

  @Test
  fun `intercept redacts api-key header`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(200))

    val request = Request.Builder()
      .url(mockWebServer.url("/api/data"))
      .addHeader("X-API-Key", "my-secret-api-key")
      .get()
      .build()

    client.newCall(request).execute().close()

    val requests = interceptor.requests.first()
    val capturedRequest = requests.first()
    assertThat(capturedRequest.requestHeaders["X-API-Key"]).isEqualTo("[REDACTED]")
  }

  @Test
  fun `intercept redacts sensitive query parameters`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(200))

    val request = Request.Builder()
      .url(mockWebServer.url("/api/auth?token=secret123&user=public"))
      .get()
      .build()

    client.newCall(request).execute().close()

    val requests = interceptor.requests.first()
    val capturedRequest = requests.first()
    assertThat(capturedRequest.url).contains("token=[REDACTED]")
    assertThat(capturedRequest.url).contains("user=public")
  }

  @Test
  fun `intercept preserves non-sensitive headers`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(200))

    val request = Request.Builder()
      .url(mockWebServer.url("/api/test"))
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .get()
      .build()

    client.newCall(request).execute().close()

    val requests = interceptor.requests.first()
    val capturedRequest = requests.first()
    assertThat(capturedRequest.requestHeaders["Content-Type"]).isEqualTo("application/json")
    assertThat(capturedRequest.requestHeaders["Accept"]).isEqualTo("application/json")
  }

  @Test
  fun `intercept captures multiple requests in order`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(200))
    mockWebServer.enqueue(MockResponse().setResponseCode(201))
    mockWebServer.enqueue(MockResponse().setResponseCode(202))

    listOf("/first", "/second", "/third").forEach { path ->
      val request = Request.Builder()
        .url(mockWebServer.url(path))
        .get()
        .build()
      client.newCall(request).execute().close()
    }

    val requests = interceptor.requests.first()
    assertThat(requests).hasSize(3)
    assertThat(requests[0].url).contains("/first")
    assertThat(requests[1].url).contains("/second")
    assertThat(requests[2].url).contains("/third")
  }

  @Test
  fun `intercept respects maxStoredRequests limit`() = runTest {
    val limitedInterceptor = DebugOverlayNetworkInterceptor(maxStoredRequests = 2, autoInstall = false)
    val limitedClient = OkHttpClient.Builder()
      .addInterceptor(limitedInterceptor)
      .build()

    repeat(5) {
      mockWebServer.enqueue(MockResponse().setResponseCode(200))
    }

    repeat(5) { index ->
      val request = Request.Builder()
        .url(mockWebServer.url("/request$index"))
        .get()
        .build()
      limitedClient.newCall(request).execute().close()
    }

    val requests = limitedInterceptor.requests.first()
    assertThat(requests).hasSize(2)
    // Should have the most recent requests
    assertThat(requests[0].url).contains("/request3")
    assertThat(requests[1].url).contains("/request4")
  }

  @Test
  fun `intercept captures POST request body`() = runTest {
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(201)
        .setBody("""{"id": 123}""")
    )

    val jsonBody = """{"name": "test", "value": 42}"""
    val request = Request.Builder()
      .url(mockWebServer.url("/api/create"))
      .post(jsonBody.toRequestBody("application/json".toMediaType()))
      .build()

    client.newCall(request).execute().close()

    val requests = interceptor.requests.first()
    assertThat(requests).hasSize(1)

    val capturedRequest = requests.first()
    assertThat(capturedRequest.method).isEqualTo("POST")
    assertThat(capturedRequest.requestBody).isEqualTo(jsonBody)
    assertThat(capturedRequest.statusCode).isEqualTo(201)
  }

  @Test
  fun `intercept decompresses gzipped response body`() = runTest {
    val originalBody = """{"message": "this is a gzipped response"}"""

    // Create gzipped body
    val gzippedBuffer = Buffer()
    GzipSink(gzippedBuffer).use { gzipSink ->
      gzipSink.write(Buffer().writeUtf8(originalBody), originalBody.length.toLong())
    }

    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(gzippedBuffer)
        .addHeader("Content-Encoding", "gzip")
    )

    val request = Request.Builder()
      .url(mockWebServer.url("/api/compressed"))
      .get()
      .build()

    client.newCall(request).execute().close()

    val requests = interceptor.requests.first()
    assertThat(requests).hasSize(1)

    val capturedRequest = requests.first()
    assertThat(capturedRequest.responseBody).isEqualTo(originalBody)
  }

  @Test
  fun `intercept handles corrupt gzipped response gracefully`() = runTest {
    val corruptGzipData = ByteArray(10) { 0x1F.toByte() } // Invalid gzip

    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(Buffer().write(corruptGzipData))
        .addHeader("Content-Encoding", "gzip")
    )

    val request = Request.Builder()
      .url(mockWebServer.url("/api/corrupt"))
      .get()
      .build()

    client.newCall(request).execute().close()

    val requests = interceptor.requests.first()
    val capturedRequest = requests.first()
    assertThat(capturedRequest.responseBody).contains("failed to read response body")
  }
}
