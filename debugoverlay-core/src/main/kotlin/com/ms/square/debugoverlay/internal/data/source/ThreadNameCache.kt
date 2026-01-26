package com.ms.square.debugoverlay.internal.data.source

import androidx.collection.LruCache
import java.io.File
import java.io.IOException

private const val DEFAULT_MAX_SIZE = 100

/**
 * Caches thread names resolved from `/proc/self/task/{tid}/comm`.
 *
 * Thread names can technically change at runtime (via `Thread.setName()`), but changes are
 * rare in practice. Caching avoids repeated file I/O and is acceptable for debug logging.
 * The main thread (where pid == tid) always returns "main" without file lookup.
 *
 * @param maxSize Maximum number of thread names to cache. Defaults to 100.
 * @param threadNameResolver Function to resolve thread name from tid. Injectable for testing.
 */
internal class ThreadNameCache(
  maxSize: Int = DEFAULT_MAX_SIZE,
  private val threadNameResolver: (tid: Int) -> String? = ::resolveThreadNameFromProc,
) {
  private val cache = LruCache<Int, String>(maxSize)

  /**
   * Resolves thread name for the given pid/tid pair.
   *
   * @return "main" if pid == tid, otherwise the thread name from cache or `/proc`.
   *   Falls back to "Thread-{tid}" if the name cannot be read.
   */
  @Synchronized
  fun resolve(pid: Int, tid: Int): String {
    // Main thread: pid == tid
    if (pid == tid) return "main"

    return cache.getOrPut(tid) {
      if (tid <= 0) return@getOrPut "Thread-$tid"
      threadNameResolver(tid) ?: "Thread-$tid"
    }
  }
}

private fun LruCache<Int, String>.getOrPut(key: Int, defaultValue: () -> String): String =
  get(key) ?: defaultValue().also { put(key, it) }

/**
 * Resolves thread name by reading `/proc/self/task/{tid}/comm`.
 *
 * @return The thread name, or null if it cannot be read (invalid tid, file not found, etc.)
 */
private fun resolveThreadNameFromProc(tid: Int): String? = try {
  File("/proc/self/task/$tid/comm").readText().trim()
} catch (_: IOException) {
  null
} catch (_: SecurityException) {
  null
}
