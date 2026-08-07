package com.ms.square.debugoverlay.internal.crash

import android.os.Process
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.model.AppInfo
import com.ms.square.debugoverlay.internal.bugreport.model.CustomLogSourceData
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlin.system.exitProcess

/**
 * Persists a [CrashRecord] to disk on an uncaught exception, then always delegates to
 * [previousHandler] so the app's normal crash behavior (and any other installed crash
 * reporter, e.g. Crashlytics) is unaffected.
 *
 * Installed once by [com.ms.square.debugoverlay.DebugOverlay.install]. All data reads
 * are non-suspending, in-memory snapshots — this must never spawn a subprocess, hop a
 * dispatcher, or otherwise risk not completing before the process dies.
 *
 * @param previousHandler The handler that was installed before this one, captured once
 *   at install time. Never re-fetched, so a handler installed by another SDK *after*
 *   DebugOverlay is never clobbered.
 * @param storage Where the crash record is written.
 * @param cachedAppInfoProvider Returns app info fetched once, off the main thread, shortly
 *   after install (see [com.ms.square.debugoverlay.DebugOverlay.install]) — never queried
 *   fresh here, since that would mean PackageManager IPC calls in the crash path. Returns
 *   null if a crash happens before that background fetch completes.
 * @param logcatSnapshotProvider Non-suspending snapshot of the in-memory Logcat buffer.
 * @param customLogSnapshotProvider Non-suspending snapshot of the custom log source, if any.
 * @param networkRequestsSnapshotProvider Non-suspending snapshot of recent network requests.
 * @param maxLogLines Maximum number of entries kept per log/request source.
 */
internal class CrashHandler(
  private val previousHandler: Thread.UncaughtExceptionHandler?,
  private val storage: CrashRecordStorage,
  private val cachedAppInfoProvider: () -> AppInfo?,
  private val logcatSnapshotProvider: () -> List<LogEntry>,
  private val customLogSnapshotProvider: () -> CustomLogSourceData?,
  private val networkRequestsSnapshotProvider: () -> List<NetworkRequest>,
  private val maxLogLines: Int = DEFAULT_MAX_LOG_LINES,
) : Thread.UncaughtExceptionHandler {

  @Suppress("TooGenericExceptionCaught")
  override fun uncaughtException(thread: Thread, throwable: Throwable) {
    try {
      storage.writeSync(buildCrashRecord(thread, throwable))
    } catch (t: Throwable) {
      // Deliberately broad: capture failure must never prevent the delegate call below
      // from running, since that's what keeps other crash reporters (e.g. Crashlytics)
      // and the platform's own crash handling working.
      runCatching { Logger.e("CrashHandler failed to capture crash record", t) }
    } finally {
      delegateToPreviousHandler(thread, throwable)
    }
  }

  private fun buildCrashRecord(thread: Thread, throwable: Throwable): CrashRecord {
    val maxLines = maxLogLines.coerceAtLeast(0)
    val customLogSnapshot = customLogSnapshotProvider()?.let { data ->
      data.copy(logs = data.logs.takeLast(maxLines))
    }
    return CrashRecord(
      timestampMs = System.currentTimeMillis(),
      threadName = thread.name,
      exceptionType = throwable.javaClass.name,
      message = throwable.message,
      stackTrace = throwable.stackTraceToString(),
      appInfo = cachedAppInfoProvider(),
      logcatLogs = logcatSnapshotProvider().takeLast(maxLines),
      customLogSourceData = customLogSnapshot,
      networkRequests = networkRequestsSnapshotProvider().takeLast(maxLines)
    )
  }

  private fun delegateToPreviousHandler(thread: Thread, throwable: Throwable) {
    val handler = previousHandler
    if (handler != null) {
      handler.uncaughtException(thread, throwable)
    } else {
      // Unreachable on real devices: the platform always installs a default handler
      // before Application.onCreate(). Guards test doubles / edge-case environments
      // where none was ever installed, so the process still terminates.
      Process.killProcess(Process.myPid())
      exitProcess(EXIT_CODE_UNCAUGHT_EXCEPTION)
    }
  }

  private companion object {
    const val EXIT_CODE_UNCAUGHT_EXCEPTION = 10
    const val DEFAULT_MAX_LOG_LINES = 100
  }
}
