package com.ms.square.debugoverlay.extension.timber

import android.os.Process
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.LogTracker
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import com.ms.square.debugoverlay.internal.data.EvictingQueue
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.LogLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong

/**
 * Timber.Tree that captures logs for DebugOverlay.
 *
 * This tree is **automatically planted** via AndroidX Startup when the
 * `debugoverlay-extension-timber` dependency is added. No manual setup required.
 *
 * All logs sent through Timber will be displayed in the DebugOverlay Log tab
 * with "Timber" as the source indicator, replacing the default system logcat reader.
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
public class DebugOverlayTimberTree(maxStoredLogs: Int = DEFAULT_MAX_LOGS) :
  Timber.Tree(),
  LogTracker {

  override val sourceName: String = SOURCE_NAME

  private val idGenerator = AtomicLong(0)

  @OptIn(InternalDebugOverlayApi::class)
  private val recentLogs = EvictingQueue<LogEntry>(maxStoredLogs)
  private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
  override val logs: Flow<List<LogEntry>> = _logs.asStateFlow()

  init {
    // Auto-register with DebugOverlay when tree is created
    DebugOverlay.configure { copy(logTracker = this@DebugOverlayTimberTree) }
  }

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    val now = System.currentTimeMillis()
    val fullMessage = if (t != null) "$message\n${t.stackTraceToString()}" else message

    val entry = LogEntry(
      id = idGenerator.getAndIncrement(),
      timestampMs = now,
      level = LogLevel.fromInt(priority),
      tag = tag ?: DEFAULT_TAG,
      pid = Process.myPid(),
      tid = Process.myTid(),
      threadName = Thread.currentThread().name,
      message = fullMessage
    )

    @OptIn(InternalDebugOverlayApi::class)
    _logs.value = recentLogs.addAndSnapshot(entry)
  }

  private companion object {
    const val DEFAULT_MAX_LOGS = 300
    const val SOURCE_NAME = "Timber"
    const val DEFAULT_TAG = "Timber"
  }
}
