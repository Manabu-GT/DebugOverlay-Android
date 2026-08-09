package com.ms.square.debugoverlay.internal.crash

import com.ms.square.debugoverlay.internal.bugreport.model.AppInfo
import com.ms.square.debugoverlay.internal.bugreport.model.CustomLogSourceData
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

/**
 * A crash captured by [CrashHandler] and persisted to disk so it survives process death.
 *
 * @param version Schema version, for forward-compatible reads via `ignoreUnknownKeys`. Carries
 *   [EncodeDefault] because it always equals its default, which the serializer would otherwise
 *   omit — leaving stored records with no version to read.
 * @param id unique identifier for the crash record
 * @param timestampMs When the exception was caught (epoch millis).
 * @param threadName Name of the thread that crashed.
 * @param exceptionType Fully-qualified exception class name.
 * @param message The exception's message, if any.
 * @param stackTrace Full stack trace text, including any "Caused by" chain.
 * @param appInfo App info captured.
 * @param logcatLogs Recent logcat entries leading up to the crash.
 * @param customLogSourceData Recent custom log source entries, null if none registered.
 * @param networkRequests Recent network requests leading up to the crash.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class CrashRecord(
  @EncodeDefault
  val version: Int = 1,
  @EncodeDefault
  val id: String = UUID.randomUUID().toString(),
  val timestampMs: Long,
  val threadName: String,
  val exceptionType: String,
  val message: String?,
  val stackTrace: String,
  val appInfo: AppInfo?,
  val logcatLogs: List<LogEntry>,
  val customLogSourceData: CustomLogSourceData?,
  val networkRequests: List<NetworkRequestSummary>,
)

/**
 * The subset of a [NetworkRequest] a crash record keeps.
 *
 * Deliberately not [NetworkRequest] itself: that carries request/response bodies (up to 2MB
 * each by default in the OkHttp extension) and [com.ms.square.debugoverlay.model.NetworkError]
 * repeats the response body in its `stackTrace`. Persisting those could mean serializing 2+
 * megabytes on the crashing thread — most likely to fail exactly when the crash is an
 * OutOfMemoryError — to store data neither the detail screen nor the text export ever renders.
 * These are the only fields both consumers read.
 */
@Serializable
internal data class NetworkRequestSummary(
  val timestampMs: Long,
  val method: String,
  val url: String,
  val statusCode: Int?,
  val durationMs: Long,
  val errorTitle: String? = null,
  val errorMessage: String? = null,
)

/** Keeps only the fields a crash record renders — see [NetworkRequestSummary]. */
internal fun NetworkRequest.toSummary() = NetworkRequestSummary(
  timestampMs = timestampMs,
  method = method,
  url = url,
  statusCode = statusCode,
  durationMs = durationMs,
  errorTitle = error?.title,
  errorMessage = error?.message
)

/** A [CrashRecord] paired with the file it was loaded from. */
internal data class CrashRecordInfo(val filePath: String, val record: CrashRecord) {
  val file: File get() = File(filePath)
}
