package com.ms.square.debugoverlay.internal.data.source

import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.EvictingQueue
import com.ms.square.debugoverlay.internal.data.model.LogLevel
import com.ms.square.debugoverlay.internal.data.model.LogcatEntry
import com.ms.square.debugoverlay.internal.util.throttleLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

private val THREADTIME_FORMAT_REGEX =
  """(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+([^:]+):\s+(.+)""".toRegex()

internal class LogcatDataSource {

  private val threadNameCache = ConcurrentHashMap<Int, String>()

  /**
   * Stream logcat entries. Keeps last N entries in memory.
   */
  fun logcatStream(maxEntries: Int = 300): Flow<List<LogcatEntry>> = flow {
    var id = 1L
    val entries = EvictingQueue<LogcatEntry>(maxEntries)
    var process: Process? = null
    var reader: BufferedReader? = null
    try {
      process = Runtime.getRuntime().exec("logcat -v threadtime,printable -T $maxEntries")
      reader = BufferedReader(InputStreamReader(process.inputStream))

      while (currentCoroutineContext().isActive) {
        val line = reader.readLine()
        line.parseLogcatEntry(id)?.let {
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
      process?.destroy()
    }
  }
    .throttleLatest(500.milliseconds)
    .map { it.toList() }
    .flowOn(Dispatchers.IO)

  @Suppress("DestructuringDeclarationWithTooManyEntries")
  private fun String.parseLogcatEntry(id: Long): LogcatEntry? {
    // Format with -v threadtime: "11-17 12:05:41.810 11744 11744 D [DebugOverlay]: onPause() called for MainActivity"
    // Pattern: MM-DD HH:MM:SS.mmm PID TID LEVEL TAG: MESSAGE
    return THREADTIME_FORMAT_REGEX.matchEntire(this)?.let { match ->
      val (timestamp, pid, tid, level, tag, message) = match.destructured
      val pidInt = pid.trim().toInt()
      val tidInt = tid.trim().toInt()
      LogcatEntry(
        id = id,
        timestamp = timestamp.trim(),
        level = LogLevel.fromString(level),
        tag = tag.trim(),
        pid = pidInt,
        tid = tidInt,
        threadName = getThreadName(pidInt, tidInt),
        message = message.trim(),
        rawLine = this
      )
    }
  }

  private fun getThreadName(pid: Int, tid: Int): String {
    // if the thread is the main thread, just return "main"
    return if (pid == tid) {
      "main"
    } else {
      threadNameCache.getOrPut(tid) {
        try {
          File("/proc/self/task/$tid/comm").readText().trim()
        } catch (_: IOException) {
          "Thread-$tid" // Fallback to this format if we can't read the thread name
        }
      }
    }
  }
}
