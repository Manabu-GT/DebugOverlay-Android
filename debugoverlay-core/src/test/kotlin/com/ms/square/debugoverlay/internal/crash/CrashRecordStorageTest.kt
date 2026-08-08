package com.ms.square.debugoverlay.internal.crash

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

private const val BASE_TIMESTAMP_MS = 1_700_000_000_000L

@RunWith(RobolectricTestRunner::class)
class CrashRecordStorageTest {

  // Only for files written outside the app dirs: Robolectric already gives each test method its
  // own sandbox for context.noBackupFilesDir and tears it down afterwards, so recordsDir needs
  // no cleanup of its own. This rule deletes its contents pass or fail.
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  private val context = RuntimeEnvironment.getApplication()

  private val storage = DefaultCrashRecordStorage(context = context, maxRecords = 3)

  private val recordsDir = File(context.noBackupFilesDir, "debugoverlay_crash_records")

  @Test
  fun `listCrashRecords evicts oldest records beyond maxRecords`() = runTest {
    repeat(5) { index -> storage.writeSync(fakeRecord(index)) }

    val records = storage.listCrashRecords()

    assertThat(records).hasSize(3)
  }

  @Test
  fun `writeSync persists the schema version`() = runTest {
    storage.writeSync(fakeRecord(0))

    val storedJson = recordsDir.listFiles().orEmpty().single().readText()

    assertThat(storedJson).contains("\"version\":1")
  }

  @Test
  fun `listCrashRecords ignores and cleans up a temp file left by an unfinished write`() = runTest {
    storage.writeSync(fakeRecord(0))
    val orphanedTemp = File(recordsDir, "crash_${BASE_TIMESTAMP_MS}_orphan.json.tmp")
    orphanedTemp.writeText("{\"partial\":")

    val records = storage.listCrashRecords()

    assertThat(records).hasSize(1)
    assertThat(orphanedTemp.exists()).isFalse()
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
    val outsideFile = temporaryFolder.newFile("crash_outside.json")
    outsideFile.writeText("not a real record")
    val maliciousInfo = CrashRecordInfo(filePath = outsideFile.absolutePath, record = fakeRecord(0))

    storage.deleteCrashRecord(maliciousInfo)

    assertThat(outsideFile.exists()).isTrue()
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
