package com.ms.square.debugoverlay.internal.data.source

import android.os.Build
import androidx.annotation.GuardedBy
import androidx.annotation.IntRange
import com.ms.square.debugoverlay.Clearable
import com.ms.square.debugoverlay.LogSource
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.EvictingQueue
import com.ms.square.debugoverlay.internal.util.throttleLatest
import com.ms.square.debugoverlay.model.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * This only reads current app logs, not other apps (such requires a signature-level permission -> READ_LOGS).
 */
@OptIn(InternalDebugOverlayApi::class)
internal class LogcatDataSource(
  scope: CoroutineScope,
  private val parser: LogcatEntryParser = LogcatEntryParser(),
  @IntRange(from = 1) initialMaxEntries: Int,
) : LogSource,
  Clearable,
  Closeable {

  override val sourceName: String = "Logcat"

  private val processLock = Any()

  @GuardedBy("processLock")
  private var currentProcess: Process? = null
  private val entries = EvictingQueue<LogEntry>(initialMaxEntries)

  /**
   * Maximum number of entries retained in the in-memory buffer. Resizing takes effect on the
   * queue immediately; the running subprocess keeps the `-T N` arg it was started with, which
   * only affects how much history was replayed at process start.
   */
  var maxEntries: Int
    @IntRange(from = 1)
    get() = entries.capacity
    set(@IntRange(from = 1) value) {
      entries.capacity = value
    }

  // Drops OS-replayed entries from before the last clear, since `-T N` walks the OS ring
  // buffer at producer start. Wall-clock epoch ms, matching `logcat -v ... epoch`.
  @Volatile private var clearMarkerMs: Long = 0L

  // Forces a downstream re-read after clear() so the UI sees `[]` instantly,
  // even when the producer is idle.
  private val clearSignal = MutableSharedFlow<Unit>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )

  /**
   * Producer signal flow. Emits Unit ticks (not data) whenever a new entry is
   * appended to [entries]. The downstream `.map { entries.toList() }` reads
   * the queue's current state — the tick payload is irrelevant.
   */
  private val producerSignal: Flow<Unit> = flow {
    // Starts fresh. With Eagerly sharing this runs once per process, but the clear is kept
    // so a restarted producer can't double-count the history `logcat -T N` replays.
    entries.clear()
    var reader: BufferedReader? = null
    try {
      /**
       * NOTE: The -T flag with a number fetches the last N lines from this app and continue to listens
       * for new logs (-t option fetches once and exits immediately).
       */
      val process = ProcessBuilder(
        "logcat",
        "-v",
        "threadtime,printable,epoch",
        "-T",
        maxEntries.toString()
      ).start().also {
        synchronized(processLock) {
          currentProcess = it
        }
      }
      reader = InputStreamReader(process.inputStream).buffered()

      while (currentCoroutineContext().isActive) {
        // readLine() returns null at end of stream, so exit early if a process dies unexpectedly
        val line = reader.readLine() ?: break
        parser.parse(line)?.let { entry ->
          // Drop OS-replayed entries from before the last clear. `-T N` replays the
          // last N ring-buffer lines when the subprocess starts, which can predate a clear().
          // (Rare caveat: a backward system-clock jump could mis-drop a real entry.)
          if (entry.timestampMs < clearMarkerMs) return@let
          entries.add(entry)
          emit(Unit)
        }
      }
    } catch (e: IOException) {
      Logger.e("Failed to read logcat", e)
    } catch (e: SecurityException) {
      Logger.e("Failed to read logcat", e)
    } finally {
      reader?.close()
      safeDestroyProcess()
    }
  }

  /**
   * Stream logcat entries. Keeps last N entries in memory.
   *
   * `throttleLatest` is applied only to `producerSignal` so noisy producers
   * are rate-limited, while `clearSignal` flows straight through merge to
   * the downstream `.map` — making clear() visually instant.
   */
  private val _logs: StateFlow<List<LogEntry>> = merge(
    producerSignal.throttleLatest(500.milliseconds),
    clearSignal
  )
    .map { entries.toList() }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope,
      // Eagerly, not WhileSubscribed: the buffer has to be warm when an uncaught exception
      // arrives, and a crash rarely happens with the debug panel open. Under WhileSubscribed
      // the subprocess only ran while the Logcat tab was visible, so crash records captured
      // an empty log list for anyone who never opened the panel — and a stale one for anyone
      // who had opened and closed it. Matches customLogSourceLogs/networkRequests, which the
      // repository already shares eagerly for the same reason. Costs one `logcat -T N`
      // subprocess for the process lifetime, with memory bounded by maxEntries.
      started = SharingStarted.Eagerly,
      initialValue = emptyList()
    )

  /** Public API for [LogSource] interface. */
  override val logs: Flow<List<LogEntry>> = _logs

  override fun clear() {
    clearMarkerMs = System.currentTimeMillis()
    entries.clear()
    clearSignal.tryEmit(Unit)
  }

  /**
   * Returns a snapshot of the in-memory log buffer without suspending or spawning a subprocess,
   * so it stays safe to call while the process is dying — see
   * [com.ms.square.debugoverlay.internal.data.DebugOverlayDataRepository.writeCrashRecordSync],
   * which calls this from the crashing thread.
   *
   * The buffer is filled from process start (the producer is shared eagerly), so this does not
   * depend on the debug panel ever having been opened. Bug reports read it too. Entries are
   * already filtered against [clear] as they are appended, so no post-filtering is needed here.
   */
  fun queryLogcatSnapshot(): List<LogEntry> = entries.toList()

  override fun close() {
    safeDestroyProcess()
  }

  private fun safeDestroyProcess() {
    synchronized(processLock) {
      currentProcess?.safeDestroy()
      currentProcess = null
    }
  }
}

private fun Process.safeDestroy() {
  destroy()
  // Wait for process to actually terminate (with timeout)
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    try {
      waitFor(1, TimeUnit.SECONDS)
    } catch (_: InterruptedException) { }
    // Force kill if still alive
    if (isAlive) {
      destroyForcibly()
    }
  }
}
