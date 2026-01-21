package com.ms.square.debugoverlay.internal.data.source

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import com.ms.square.debugoverlay.internal.data.Percentage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class MemoryDataSource(
  context: Context,
  private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  private val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

  /**
   * Recommended maximum PSS value for graph scaling.
   */
  val maxPss: Float by lazy {
    MemoryMetricsCalculator.calculateMaxPss(
      memoryClass = am.memoryClass,
      largeMemoryClass = am.largeMemoryClass,
      isLowRamDevice = am.isLowRamDevice
    )
  }

  fun heapUsage(interval: Duration = 1L.seconds): Flow<Percentage> = flow {
    while (currentCoroutineContext().isActive) {
      val runtime = Runtime.getRuntime()
      emit(
        MemoryMetricsCalculator.calculateHeapUsagePercentage(
          usedMemory = runtime.totalMemory() - runtime.freeMemory(),
          maxMemory = runtime.maxMemory()
        )
      )
      delay(interval)
    }
  }.flowOn(defaultDispatcher)

  // 3secs default interval as PSS often doesn't change a lot.
  fun pss(interval: Duration = 3L.seconds): Flow<Float> = flow {
    while (currentCoroutineContext().isActive) {
      // activity manager's getProcessMemoryInfo only refreshes every 5 mins (MEMORY_INFO_THROTTLE_TIME); thus
      // use Debug.getMemoryInfo instead as it's acceptable not to include some protected allocations such as graphics.
      val processMemInfo = Debug.MemoryInfo()
      Debug.getMemoryInfo(processMemInfo)
      emit(MemoryMetricsCalculator.convertKbToMb(processMemInfo.totalPss))
      delay(interval)
    }
  }.flowOn(ioDispatcher)
}
