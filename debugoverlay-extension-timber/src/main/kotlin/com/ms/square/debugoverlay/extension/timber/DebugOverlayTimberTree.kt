package com.ms.square.debugoverlay.extension.timber

import android.os.Process
import com.ms.square.debugoverlay.Clearable
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.LogSource
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import com.ms.square.debugoverlay.internal.data.EvictingQueue
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.LogLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

private const val DEFAULT_MAX_LOGS = 300
private const val SOURCE_NAME = "Timber"
private const val DEFAULT_TAG = "Timber"

/**
 * Timber.Tree that captures logs for DebugOverlay.
 *
 * This tree is **automatically planted** via AndroidX Startup when the
 * `debugoverlay-extension-timber` dependency is added. No manual setup required.
 *
 * All logs sent through Timber will be displayed in an additional "Timber" tab
 * in the DebugOverlay debug panel, alongside the built-in Logcat tab.
 *
 * **Usage:** Just add the dependency - no code required!
 * ```kotlin
 * // build.gradle.kts
 * debugImplementation("com.ms-square:debugoverlay-extension-timber:x.x.x")
 * ```
 *
 * **Lifecycle:** Lives for entire process lifetime. Logs cleared on process death.
 *
 * **Memory:** Max ~50-100KB (300 entries × ~170-330 bytes each) with default [maxStoredLogs].
 *
 * **Thread safety:** All operations are thread-safe. Logs can be written from any thread.
 *
 * @param maxStoredLogs Maximum number of log entries to keep in memory.
 *   Older entries are evicted when this limit is reached.
 */
@OptIn(InternalDebugOverlayApi::class)
public class DebugOverlayTimberTree(maxStoredLogs: Int = DEFAULT_MAX_LOGS) :
  Timber.Tree(),
  LogSource,
  Clearable {

  override val sourceName: String = SOURCE_NAME

  private val recentLogs = EvictingQueue<LogEntry>(maxStoredLogs)
  private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
  override val logs: Flow<List<LogEntry>> = _logs.asStateFlow()

  init {
    // Auto-register with DebugOverlay when tree is created
    DebugOverlay.configure { customLogSource = this@DebugOverlayTimberTree }
  }

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    val now = System.currentTimeMillis()
    // TODO: Consider truncating large messages (e.g., 8KB limit) to bound memory usage.
    //  Stack traces can be very large and 300 entries with huge traces exceeds the KDoc estimate.
    val fullMessage = if (t != null) "$message\n${t.stackTraceToString()}" else message

    val entry = LogEntry(
      timestampMs = now,
      level = LogLevel.fromInt(priority),
      tag = tag ?: DEFAULT_TAG,
      pid = Process.myPid(),
      tid = Process.myTid(),
      threadName = Thread.currentThread().name,
      message = fullMessage
    )

    recentLogs.add(entry)
    _logs.update { recentLogs.toList() }
  }

  override fun clear() {
    recentLogs.clear()
    _logs.update { emptyList() }
  }
}
