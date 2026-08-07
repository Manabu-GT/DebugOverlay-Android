package com.ms.square.debugoverlay.internal.crash

import android.content.Context
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.util.checkFolderExists
import com.ms.square.debugoverlay.internal.util.isDirectChildOf
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

private const val CRASH_RECORDS_SUBDIR = "debugoverlay_crash_records"
internal const val DEFAULT_MAX_CRASH_RECORDS = 5

/**
 * Persists [CrashRecord]s to disk so they survive process death, and lets the next
 * launch discover and review them.
 */
internal sealed interface CrashRecordStorage {
  /**
   * Writes [record] to disk. Does not evict old records — see [listCrashRecords].
   *
   * Must be safe to call synchronously from [Thread.UncaughtExceptionHandler.uncaughtException]:
   * no suspension, no dispatcher hop, no more work than writing this one file. Any failure
   * is swallowed and logged — the caller must be able to unconditionally proceed to the
   * previous crash handler afterward.
   */
  fun writeSync(record: CrashRecord)

  /**
   * Loads all persisted crash records, most recent first.
   *
   * Also evicts records beyond the retention limit first. Retention doesn't need real-time
   * enforcement, so this is deferred here — off the crash path — rather than done in
   * [writeSync]; between crashes, the on-disk count can transiently exceed the limit until
   * this is next called.
   */
  suspend fun listCrashRecords(): List<CrashRecordInfo>

  /** Deletes a single persisted crash record. */
  suspend fun deleteCrashRecord(info: CrashRecordInfo)
}

/**
 * Default implementation of [CrashRecordStorage] using the app's no-backup data directory.
 *
 * Records are flat JSON files (no per-record folder needed, unlike bug report drafts,
 * since a crash record is a single self-contained blob) named `crash_<timestampMs>_<uuid>.json`
 * so lexicographic filename order matches chronological order.
 *
 * @param context Application context for no-backup directory access
 * @param maxRecords Maximum number of records retained; oldest evicted first
 */
internal class DefaultCrashRecordStorage(
  private val context: Context,
  private val maxRecords: Int = DEFAULT_MAX_CRASH_RECORDS,
) : CrashRecordStorage {

  private val json = Json { ignoreUnknownKeys = true }

  // Plain JVM monitor, not a coroutines Mutex: writeSync() runs outside any coroutine
  // context (it's called directly from uncaughtException()). Also guards the eviction +
  // listing step in listCrashRecords() against a concurrent writeSync(), so a write and a
  // list/evict can't race on the directory contents.
  private val writeLock = Any()

  private val recordsDir by lazy {
    File(context.noBackupFilesDir, CRASH_RECORDS_SUBDIR).also {
      it.checkFolderExists()
    }
  }

  override fun writeSync(record: CrashRecord) {
    synchronized(writeLock) {
      val file = File(recordsDir, fileNameFor(record))
      file.writeText(json.encodeToString(CrashRecord.serializer(), record))
    }
  }

  private fun fileNameFor(record: CrashRecord) = "crash_${record.timestampMs}_${UUID.randomUUID()}.json"

  // Must be called while holding writeLock.
  private fun evictOldRecordsLocked() {
    val files = recordsDir.listFiles()?.filter { it.isFile } ?: return
    if (files.size <= maxRecords) return
    files.sortedDescending().drop(maxRecords).forEach { it.delete() }
  }

  override suspend fun listCrashRecords(): List<CrashRecordInfo> = withContext(Dispatchers.IO) {
    // Eviction happens here rather than in writeSync(): retention doesn't need real-time
    // enforcement, only "eventually pruned back down" — deferring it off the crash path
    // keeps writeSync() to the bare minimum needed before delegating to the previous handler.
    val files = synchronized(writeLock) {
      evictOldRecordsLocked()
      recordsDir.listFiles()?.filter { it.isFile }?.sortedDescending() ?: emptyList()
    }
    files.mapNotNull { file -> loadRecord(file)?.let { CrashRecordInfo(file.absolutePath, it) } }
  }

  override suspend fun deleteCrashRecord(info: CrashRecordInfo): Unit = withContext(Dispatchers.IO) {
    val file = info.file
    if (!file.isDirectChildOf(recordsDir)) {
      Logger.w("Refusing to delete crash record outside records directory: ${file.absolutePath}")
      return@withContext
    }
    if (file.exists() && !file.delete()) {
      Logger.w("Failed to delete crash record: ${file.absolutePath}")
    }
  }

  private fun loadRecord(file: File): CrashRecord? =
    runCatchingNonCancellation {
      json.decodeFromString(CrashRecord.serializer(), file.readText())
    }.getOrElse { e ->
      Logger.w("Failed to parse crash record ${file.name}: ${e.message}")
      null
    }
}
