package com.ms.square.debugoverlay.internal.data.source

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import com.ms.square.debugoverlay.internal.data.Percentage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val KB_TO_MB = 1024f

internal class MemoryDataSource(context: Context) {

  private val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

  /**
   * Recommended maximum PSS value for graph scaling.
   *
   * Uses device's memory class (per-app limit) as baseline:
   * - Standard devices: memoryClass (typically 192-512 MB)
   * - Large heap devices: largeMemoryClass (typically 512-1024 MB)
   *
   * PSS includes heap + native + shared libraries, so we use 2x multiplier.
   * This provides headroom while keeping the graph readable.
   */
  val maxPss: Float by lazy {
    val memoryClassMB = if (am.isLowRamDevice) {
      // Low-RAM devices: use standard memory class
      am.memoryClass.toFloat()
    } else {
      // Normal devices: prefer large memory class if available
      am.largeMemoryClass.toFloat()
    }

    // PSS typically 1.5-2x heap size; use 2x for safety margin
    (memoryClassMB * 2).coerceAtLeast(512f) // Minimum 512MB for readability
  }

  fun heapUsage(interval: Duration = 1L.seconds): Flow<Percentage> = flow {
    while (currentCoroutineContext().isActive) {
      val runtime = Runtime.getRuntime()
      val usedMemory = runtime.totalMemory() - runtime.freeMemory()
      emit(Percentage.ofClamped(usedMemory.toFloat() / runtime.maxMemory()))
      delay(interval)
    }
  }.flowOn(Dispatchers.Default)

  // 3secs default interval as PSS often doesn't change a lot.
  fun pss(interval: Duration = 3L.seconds): Flow<Float> = flow {
    while (currentCoroutineContext().isActive) {
      // activity manager's getProcessMemoryInfo only refreshes every 5 mins (MEMORY_INFO_THROTTLE_TIME); thus
      // use Debug.getMemoryInfo instead as it's acceptable not to include some protected allocations such as graphics.
      val processMemInfo = Debug.MemoryInfo()
      Debug.getMemoryInfo(processMemInfo)
      val pssInMBytes = processMemInfo.totalPss / KB_TO_MB
      emit(pssInMBytes)
      delay(interval)
    }
  }.flowOn(Dispatchers.IO)
}
