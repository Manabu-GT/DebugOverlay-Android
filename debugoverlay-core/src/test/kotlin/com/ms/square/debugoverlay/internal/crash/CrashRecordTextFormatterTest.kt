package com.ms.square.debugoverlay.internal.crash

import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.internal.bugreport.model.CustomLogSourceData
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.LogLevel
import com.ms.square.debugoverlay.model.NetworkError
import com.ms.square.debugoverlay.model.NetworkRequest
import org.junit.Test

class CrashRecordTextFormatterTest {

  private val baseRecord = CrashRecord(
    timestampMs = 1_700_000_000_000L,
    threadName = "main",
    exceptionType = "java.lang.IllegalStateException",
    message = "boom",
    stackTrace = "java.lang.IllegalStateException: boom\n\tat Foo.bar(Foo.kt:1)",
    appInfo = null,
    logcatLogs = emptyList(),
    customLogSourceData = null,
    networkRequests = emptyList()
  )

  @Test
  fun `includes exception type, message, and stack trace`() {
    val text = formatCrashRecordAsText(baseRecord)

    assertThat(text).contains("java.lang.IllegalStateException: boom")
    assertThat(text).contains("--- STACK TRACE ---")
    assertThat(text).contains("at Foo.bar(Foo.kt:1)")
  }

  @Test
  fun `omits log and network sections when empty`() {
    val text = formatCrashRecordAsText(baseRecord)

    assertThat(text).doesNotContain("--- LOGCAT")
    assertThat(text).doesNotContain("--- NETWORK REQUESTS")
  }

  @Test
  fun `includes logcat, custom log, and network sections when present`() {
    val record = baseRecord.copy(
      logcatLogs = listOf(fakeLogEntry("logcat line")),
      customLogSourceData = CustomLogSourceData(logs = listOf(fakeLogEntry("timber line")), sourceName = "Timber"),
      networkRequests = listOf(
        NetworkRequest(
          protocol = "http/1.1",
          method = "GET",
          url = "https://example.com",
          statusCode = 200,
          durationMs = 42L,
          responseSize = 100L,
          requestSize = 0L,
          timestampMs = 1_700_000_000_000L
        )
      )
    )

    val text = formatCrashRecordAsText(record)

    assertThat(text).contains("--- LOGCAT (1) ---")
    assertThat(text).contains("logcat line")
    assertThat(text).contains("--- TIMBER (1) ---")
    assertThat(text).contains("timber line")
    assertThat(text).contains("--- NETWORK REQUESTS (1) ---")
    assertThat(text).contains("GET https://example.com -> 200 (42ms)")
  }

  @Test
  fun `appends error title and message for failed network requests`() {
    val record = baseRecord.copy(
      networkRequests = listOf(
        NetworkRequest(
          protocol = null,
          method = "GET",
          url = "https://example.com",
          statusCode = null,
          durationMs = 5_000L,
          responseSize = null,
          requestSize = 0L,
          timestampMs = 1_700_000_000_000L,
          error = NetworkError(title = "IOException", message = "Connection reset by peer")
        )
      )
    )

    val text = formatCrashRecordAsText(record)

    assertThat(text).contains("[ERROR: IOException: Connection reset by peer]")
  }

  private fun fakeLogEntry(message: String) = LogEntry(
    timestampMs = 1_700_000_000_000L,
    level = LogLevel.INFO,
    tag = "Test",
    pid = 1,
    tid = 1,
    threadName = "main",
    message = message
  )
}
