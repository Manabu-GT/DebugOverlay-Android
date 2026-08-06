package com.ms.square.debugoverlay.internal.crash

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

private const val BASE_TIMESTAMP_MS = 1_700_000_000_000L

@RunWith(RobolectricTestRunner::class)
class CrashRecordStorageTest {

  private val storage = DefaultCrashRecordStorage(
    context = RuntimeEnvironment.getApplication(),
    maxRecords = 3
  )

  @Test
  fun `writeSync evicts oldest records beyond maxRecords`() = runTest {
    repeat(5) { index -> storage.writeSync(fakeRecord(index)) }

    val records = storage.listCrashRecords()

    assertThat(records).hasSize(3)
  }

  @Test
  fun `listCrashRecords returns most recent first`() = runTest {
    repeat(5) { index -> storage.writeSync(fakeRecord(index)) }

    val records = storage.listCrashRecords()

    // The 3 retained records are the 3 most recently written (indices 2, 3, 4), newest first.
    assertThat(records.map { it.record.timestampMs })
      .containsExactly(
        BASE_TIMESTAMP_MS + 4_000,
        BASE_TIMESTAMP_MS + 3_000,
        BASE_TIMESTAMP_MS + 2_000
      )
      .inOrder()
  }

  @Test
  fun `deleteCrashRecord removes the record`() = runTest {
    storage.writeSync(fakeRecord(0))
    val info = storage.listCrashRecords().single()

    storage.deleteCrashRecord(info)

    assertThat(storage.listCrashRecords()).isEmpty()
  }

  @Test
  fun `deleteCrashRecord refuses to delete a file outside the records directory`() = runTest {
    storage.writeSync(fakeRecord(0))
    val outsideFile = File.createTempFile("crash_outside", ".json")
    outsideFile.writeText("not a real record")
    val maliciousInfo = CrashRecordInfo(filePath = outsideFile.absolutePath, record = fakeRecord(0))

    storage.deleteCrashRecord(maliciousInfo)

    assertThat(outsideFile.exists()).isTrue()
    outsideFile.delete()
  }

  private fun fakeRecord(index: Int) = CrashRecord(
    timestampMs = BASE_TIMESTAMP_MS + index * 1_000L,
    threadName = "main",
    exceptionType = "java.lang.RuntimeException",
    message = "boom $index",
    stackTrace = "java.lang.RuntimeException: boom $index",
    appInfo = null,
    logcatLogs = emptyList(),
    customLogSourceData = null,
    networkRequests = emptyList()
  )
}
