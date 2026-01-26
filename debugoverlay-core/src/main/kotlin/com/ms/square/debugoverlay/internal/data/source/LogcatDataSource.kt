package com.ms.square.debugoverlay.internal.data.source

import android.os.Build
import androidx.annotation.GuardedBy
import com.ms.square.debugoverlay.LogSource
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.EvictingQueue
import com.ms.square.debugoverlay.internal.util.throttleLatest
import com.ms.square.debugoverlay.model.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
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
  private val maxEntries: Int = 300,
) : LogSource,
  Closeable {

  override val sourceName: String = "Logcat"

  private val processLock = Object()

  @GuardedBy("processLock")
  private var currentProcess: Process? = null

  /**
   * Stream logcat entries. Keeps last N entries in memory.
   * Private StateFlow for direct .value access in [queryLogcatSnapshot].
   */
  private val _logs: StateFlow<List<LogEntry>> = flow {
    val entries = EvictingQueue<LogEntry>(maxEntries)
    var reader: BufferedReader? = null
    try {
      /**
       * NOTE: The -T flag with a number fetches the last N lines from this app and continue to listens
       * for new logs (-t option fetches once and exists immediately).
       */
      val process = Runtime.getRuntime().exec("logcat -v threadtime,printable,epoch -T $maxEntries").also {
        synchronized(processLock) {
          currentProcess = it
        }
      }
      reader = InputStreamReader(process.inputStream).buffered()

      while (currentCoroutineContext().isActive) {
        // readLine() returns null at end of stream, so exit early if a process dies unexpectedly
        val line = reader.readLine() ?: break
        parser.parse(line)?.let {
          entries.add(it)
          emit(entries)
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
    .throttleLatest(500.milliseconds)
    .map { it.toList() }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope,
      started = SharingStarted.WhileSubscribed(),
      initialValue = emptyList()
    )

  /** Public API for [LogSource] interface. */
  override val logs: Flow<List<LogEntry>> = _logs

  /**
   * Returns a snapshot of logcat logs for bug reports.
   * Uses cached value if streaming was active (debug panel was viewed), otherwise captures directly.
   */
  suspend fun queryLogcatSnapshot(): List<LogEntry> {
    val cached = _logs.value
    if (cached.isNotEmpty()) return cached

    return captureLogcatOnce()
  }

  private suspend fun captureLogcatOnce(): List<LogEntry> = withContext(Dispatchers.IO) {
    buildList {
      // -t N = fetch N recent lines and EXIT (vs -T which streams continuously)
      val process = Runtime.getRuntime()
        .exec("logcat -v threadtime,printable,epoch -t $maxEntries")

      try {
        InputStreamReader(process.inputStream).useLines { lines ->
          lines.forEach { line ->
            parser.parse(line)?.let { add(it) }
          }
        }
      } catch (e: IOException) {
        Logger.e("Failed to capture logcat snapshot", e)
      } catch (e: SecurityException) {
        Logger.e("Failed to capture logcat snapshot", e)
      } finally {
        process.safeDestroy()
      }
    }
  }

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
