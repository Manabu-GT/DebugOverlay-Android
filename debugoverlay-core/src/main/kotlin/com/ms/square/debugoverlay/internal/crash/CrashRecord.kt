package com.ms.square.debugoverlay.internal.crash

import com.ms.square.debugoverlay.internal.bugreport.model.AppInfo
import com.ms.square.debugoverlay.internal.bugreport.model.CustomLogSourceData
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

/**
 * A crash captured by [CrashHandler] and persisted to disk so it survives process death.
 *
 * @param version Schema version, for forward-compatible reads via `ignoreUnknownKeys`.
 * @param timestampMs When the exception was caught (epoch millis).
 * @param threadName Name of the thread that crashed.
 * @param exceptionType Fully-qualified exception class name.
 * @param message The exception's message, if any.
 * @param stackTrace Full stack trace text, including any "Caused by" chain.
 * @param appInfo App info captured at install time (immutable for the process lifetime).
 * @param logcatLogs Recent logcat entries leading up to the crash.
 * @param customLogSourceData Recent custom log source entries, null if none registered.
 * @param networkRequests Recent network requests leading up to the crash.
 */
@Serializable
internal data class CrashRecord(
  val version: Int = 1,
  val id: String = UUID.randomUUID().toString(),
  val timestampMs: Long,
  val threadName: String,
  val exceptionType: String,
  val message: String?,
  val stackTrace: String,
  val appInfo: AppInfo?,
  val logcatLogs: List<LogEntry>,
  val customLogSourceData: CustomLogSourceData?,
  val networkRequests: List<NetworkRequest>,
)

/** A [CrashRecord] paired with the file it was loaded from. */
internal data class CrashRecordInfo(val filePath: String, val record: CrashRecord) {
  val file: File get() = File(filePath)
}
