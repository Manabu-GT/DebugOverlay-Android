@file:OptIn(InternalDebugOverlayApi::class)

package com.ms.square.debugoverlay.internal.data.source

import android.os.Build
import androidx.annotation.GuardedBy
import androidx.collection.LruCache
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.EvictingQueue
import com.ms.square.debugoverlay.internal.util.throttleLatest
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Matches epoch logcat format: "1733921286.215 11744 11744 D Tag: message"
private val LOGCAT_FORMAT_REGEX =
  """(\d+\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+([^:]+):\s+(.+)""".toRegex()

private const val THREADNAME_CACHE_SIZE = 100

/**
 * This only reads current app logs, not other apps (such requires a signature-level permission -> READ_LOGS).
 */
internal class LogcatDataSource(scope: CoroutineScope, maxEntries: Int = 300) : Closeable {

  private val processLock = Object()

  @GuardedBy("processLock")
  private var currentProcess: Process? = null

  /**
   * Stream logcat entries. Keeps last N entries in memory.
   */
  val logs: Flow<List<LogEntry>> = flow {
    var id = 1L
    val entries = EvictingQueue<LogEntry>(maxEntries)
    val threadNameCache = LruCache<Int, String>(maxSize = THREADNAME_CACHE_SIZE)
    var reader: BufferedReader? = null
    try {
      val process = Runtime.getRuntime().exec("logcat -v threadtime,printable,epoch -T $maxEntries").also {
        synchronized(processLock) {
          currentProcess = it
        }
      }
      reader = BufferedReader(InputStreamReader(process.inputStream))

      while (currentCoroutineContext().isActive) {
        // readLine() returns null at end of stream
        val line = reader.readLine()
        line?.trim()?.parseLogcatEntry(id, threadNameCache)?.let {
          entries.add(it)
          id++
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

  @Suppress("DestructuringDeclarationWithTooManyEntries")
  private fun String.parseLogcatEntry(id: Long, threadNameCache: LruCache<Int, String>): LogEntry? {
    // Format with -v threadtime,printable,epoch: "1733921286.215 11744 11744 D Tag: message"
    // Pattern: EPOCH_SECONDS.mmm PID TID LEVEL TAG: MESSAGE
    return LOGCAT_FORMAT_REGEX.matchEntire(this)?.let { match ->
      val (epochStr, pid, tid, level, tag, message) = match.destructured
      val pidInt = pid.trim().toInt()
      val tidInt = tid.trim().toInt()
      val timestampMs = try {
        epochStr.toDouble().seconds.inWholeMilliseconds
      } catch (_: NumberFormatException) {
        // fallback to current time so that this log at least shows up
        System.currentTimeMillis()
      }
      LogEntry(
        id = id,
        timestampMs = timestampMs,
        level = LogLevel.fromString(level),
        tag = tag.trim(),
        pid = pidInt,
        tid = tidInt,
        threadName = threadNameCache.getThreadName(pidInt, tidInt),
        message = message.trim()
      )
    }
  }

  private fun LruCache<Int, String>.getThreadName(pid: Int, tid: Int): String {
    // if the thread is the main thread, just return as "main"
    return if (pid == tid) {
      "main"
    } else {
      getOrPut(tid) {
        // Validate TID is a positive integer
        if (tid <= 0) return@getOrPut "Thread-$tid"
        try {
          val file = File("/proc/self/task/$tid/comm")
          // Verify the canonical path is still under /proc/self/task/
          if (!file.canonicalPath.startsWith("/proc/self/task/")) {
            return@getOrPut "Thread-$tid"
          }
          file.readText().trim()
        } catch (_: IOException) {
          "Thread-$tid"
        } catch (_: SecurityException) {
          "Thread-$tid"
        }
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

private fun <K : Any, V : Any> LruCache<K, V>.getOrPut(tid: K, function: () -> V): V = get(tid) ?: function().also {
  put(tid, it)
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
