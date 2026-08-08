package com.ms.square.debugoverlay.internal.crash

import com.ms.square.debugoverlay.internal.bugreport.model.AppInfo
import com.ms.square.debugoverlay.internal.bugreport.model.CustomLogSourceData
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.NetworkRequest

internal const val DEFAULT_MAX_LOG_LINES = 100

/**
 * Assembles a [CrashRecord] from already-captured, in-memory data.
 *
 * Pure and non-suspending: every input is passed in, so this can run on the crashing thread
 * and can be tested without a [android.content.Context]. Each log/request source is trimmed
 * to the last [maxLogLines] entries, and network requests are reduced to
 * [NetworkRequestSummary] so bodies never reach disk.
 *
 * @param appInfo App info fetched once shortly after install; null if the crash beat that
 *   background fetch.
 */
internal fun buildCrashRecord(
  thread: Thread,
  throwable: Throwable,
  appInfo: AppInfo?,
  logcatLogs: List<LogEntry>,
  customLogSourceData: CustomLogSourceData?,
  networkRequests: List<NetworkRequest>,
  maxLogLines: Int = DEFAULT_MAX_LOG_LINES,
): CrashRecord {
  val maxLines = maxLogLines.coerceAtLeast(0)
  return CrashRecord(
    timestampMs = System.currentTimeMillis(),
    threadName = thread.name,
    exceptionType = throwable.javaClass.name,
    message = throwable.message,
    stackTrace = throwable.stackTraceToString(),
    appInfo = appInfo,
    logcatLogs = logcatLogs.takeLast(maxLines),
    customLogSourceData = customLogSourceData?.let { it.copy(logs = it.logs.takeLast(maxLines)) },
    networkRequests = networkRequests.takeLast(maxLines).map { it.toSummary() }
  )
}
