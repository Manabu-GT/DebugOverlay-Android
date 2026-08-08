package com.ms.square.debugoverlay.internal.crash

import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.internal.bugreport.model.AppInfo
import com.ms.square.debugoverlay.internal.bugreport.model.CustomLogSourceData
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.LogLevel
import com.ms.square.debugoverlay.model.NetworkRequest
import org.junit.Test

class CrashRecordBuilderTest {

  @Test
  fun `buildCrashRecord captures the thread and exception details`() {
    val thread = Thread.currentThread()

    val record = buildRecord(thread = thread, throwable = IllegalStateException("boom"))

    assertThat(record.threadName).isEqualTo(thread.name)
    assertThat(record.exceptionType).isEqualTo("java.lang.IllegalStateException")
    assertThat(record.message).isEqualTo("boom")
    assertThat(record.stackTrace).contains("java.lang.IllegalStateException: boom")
  }

  // appInfo is queried on the crash path and may fail, so null is a supported input.
  @Test
  fun `buildCrashRecord carries appInfo through, keeping null when it could not be fetched`() {
    val appInfo = AppInfo(
      packageName = "com.test.app",
      versionName = "1.0.0",
      versionCode = 1,
      targetSdkVersion = 34,
      minSdkVersion = 21,
      isDebuggable = true,
      installerStore = "Unknown",
      installerPackage = null,
      firstInstallTime = 0L,
      lastUpdateTime = 0L
    )

    assertThat(buildRecord(appInfo = appInfo).appInfo).isEqualTo(appInfo)
    assertThat(buildRecord(appInfo = null).appInfo).isNull()
  }

  @Test
  fun `buildCrashRecord trims logs, custom logs, and network requests to maxLogLines`() {
    val logs = (1..10).map { fakeLogEntry(it) }

    val record = buildRecord(
      logcatLogs = logs,
      customLogSourceData = CustomLogSourceData(logs = (1..10).map { fakeLogEntry(it) }, sourceName = "Timber"),
      networkRequests = (1..10).map { fakeNetworkRequest(it) },
      maxLogLines = 3
    )

    assertThat(record.logcatLogs).hasSize(3)
    assertThat(record.logcatLogs).isEqualTo(logs.takeLast(3))
    assertThat(record.customLogSourceData?.logs).hasSize(3)
    assertThat(record.networkRequests).hasSize(3)
  }

  private fun buildRecord(
    thread: Thread = Thread.currentThread(),
    throwable: Throwable = RuntimeException("boom"),
    appInfo: AppInfo? = null,
    logcatLogs: List<LogEntry> = emptyList(),
    customLogSourceData: CustomLogSourceData? = null,
    networkRequests: List<NetworkRequest> = emptyList(),
    maxLogLines: Int = 100,
  ) = buildCrashRecord(
    thread = thread,
    throwable = throwable,
    appInfo = appInfo,
    logcatLogs = logcatLogs,
    customLogSourceData = customLogSourceData,
    networkRequests = networkRequests,
    maxLogLines = maxLogLines
  )

  private fun fakeLogEntry(index: Int) = LogEntry(
    timestampMs = index.toLong(),
    level = LogLevel.INFO,
    tag = "Test",
    pid = 1,
    tid = 1,
    threadName = "main",
    message = "message $index"
  )

  private fun fakeNetworkRequest(index: Int) = NetworkRequest(
    protocol = "http/1.1",
    method = "GET",
    url = "https://example.com/$index",
    statusCode = 200,
    durationMs = 10L,
    responseSize = 100L,
    requestSize = 0L,
    timestampMs = index.toLong()
  )
}
