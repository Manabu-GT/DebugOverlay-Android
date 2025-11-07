package com.ms.square.debugoverlay.internal.data.source

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.Percentage
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException
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
      @Suppress("MagicNumber")
      emit(Percentage.ofClamped(usedMemory.toFloat() / runtime.maxMemory() * 100f))
      delay(interval)
    }
  }

  @Suppress("TooGenericExceptionCaught")
  fun pss(interval: Duration = 1L.seconds): Flow<Float> = flow {
    var lastPss = 0f
    while (currentCoroutineContext().isActive) {
      try {
        val processMemInfo = am.getProcessMemoryInfo(intArrayOf(Process.myPid()))[0]
        val pssInMBytes = processMemInfo.totalPss / KB_TO_MB
        lastPss = pssInMBytes
        emit(pssInMBytes)
      } catch (e: CancellationException) {
        // Rethrow CancellationException to ensure proper cancellation propagation
        throw e
      } catch (e: RuntimeException) {
        Logger.e("error in querying the PSS", e)
        emit(lastPss)
      }
      delay(interval)
    }
  }
}
