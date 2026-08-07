package com.ms.square.debugoverlay.internal.crash

import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.internal.bugreport.model.AppInfo
import com.ms.square.debugoverlay.internal.bugreport.model.CustomLogSourceData
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.LogLevel
import com.ms.square.debugoverlay.model.NetworkRequest
import org.junit.Test

class CrashHandlerTest {

  private val previousHandler = FakeUncaughtExceptionHandler()
  private val storage = FakeCrashRecordStorage()

  private fun createHandler(
    logs: List<LogEntry> = emptyList(),
    customLogs: CustomLogSourceData? = null,
    networkRequests: List<NetworkRequest> = emptyList(),
    appInfo: AppInfo? = null,
    maxLogLines: Int = 100,
  ) = CrashHandler(
    previousHandler = previousHandler,
    storage = storage,
    cachedAppInfoProvider = { appInfo },
    logcatSnapshotProvider = { logs },
    customLogSnapshotProvider = { customLogs },
    networkRequestsSnapshotProvider = { networkRequests },
    maxLogLines = maxLogLines
  )

  @Test
  fun `uncaughtException writes a crash record and delegates to the previous handler`() {
    val handler = createHandler()
    val thread = Thread.currentThread()
    val throwable = IllegalStateException("boom")

    handler.uncaughtException(thread, throwable)

    val written = storage.written
    assertThat(written).isNotNull()
    assertThat(written!!.threadName).isEqualTo(thread.name)
    assertThat(written.exceptionType).isEqualTo("java.lang.IllegalStateException")
    assertThat(written.message).isEqualTo("boom")
    assertThat(previousHandler.invokedWith).isEqualTo(thread to throwable)
  }

  @Test
  fun `uncaughtException writes null appInfo when the background fetch has not completed yet`() {
    val handler = createHandler(appInfo = null)

    handler.uncaughtException(Thread.currentThread(), RuntimeException("boom"))

    assertThat(storage.written!!.appInfo).isNull()
  }

  @Test
  fun `uncaughtException includes appInfo once the background fetch has completed`() {
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
    val handler = createHandler(appInfo = appInfo)

    handler.uncaughtException(Thread.currentThread(), RuntimeException("boom"))

    assertThat(storage.written!!.appInfo).isEqualTo(appInfo)
  }

  @Test
  fun `uncaughtException delegates to previous handler even when storage write throws`() {
    storage.shouldThrowOnWrite = true
    val handler = createHandler()

    handler.uncaughtException(Thread.currentThread(), RuntimeException("boom"))

    assertThat(previousHandler.invokedWith).isNotNull()
  }

  @Test
  fun `uncaughtException trims logs, custom logs, and network requests to maxLogLines`() {
    val logs = (1..10).map { fakeLogEntry(it) }
    val customLogs = CustomLogSourceData(logs = (1..10).map { fakeLogEntry(it) }, sourceName = "Timber")
    val requests = (1..10).map { fakeNetworkRequest(it) }
    val handler = createHandler(logs = logs, customLogs = customLogs, networkRequests = requests, maxLogLines = 3)

    handler.uncaughtException(Thread.currentThread(), RuntimeException("boom"))

    val written = storage.written!!
    assertThat(written.logcatLogs).hasSize(3)
    assertThat(written.logcatLogs).isEqualTo(logs.takeLast(3))
    assertThat(written.customLogSourceData!!.logs).hasSize(3)
    assertThat(written.networkRequests).hasSize(3)
  }

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

private class FakeCrashRecordStorage : CrashRecordStorage {
  var written: CrashRecord? = null
  var shouldThrowOnWrite: Boolean = false

  override fun writeSync(record: CrashRecord) {
    if (shouldThrowOnWrite) error("simulated write failure")
    written = record
  }

  override suspend fun listCrashRecords(): List<CrashRecordInfo> = emptyList()
  override suspend fun deleteCrashRecord(info: CrashRecordInfo) = Unit
}

private class FakeUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {
  var invokedWith: Pair<Thread, Throwable>? = null

  override fun uncaughtException(t: Thread, e: Throwable) {
    invokedWith = t to e
  }
}
