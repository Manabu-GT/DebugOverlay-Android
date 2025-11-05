package com.ms.square.debugoverlay.internal.data.source

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.util.Log
import com.ms.square.debugoverlay.internal.data.Percentage
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val LOGTAG = "MemoryDataSource"
private const val KB_TO_MB = 1024f

internal class MemoryDataSource(context: Context) {

  private val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

  fun heapUsage(interval: Duration = 1L.seconds): Flow<Percentage> {
    return flow {
      while (currentCoroutineContext().isActive) {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        emit(Percentage.ofClamped(usedMemory.toFloat() / runtime.maxMemory() * 100f))
        delay(interval)
      }
    }
  }

  fun pss(interval: Duration = 1L.seconds): Flow<Float> {
    return flow {
      var lastPss = 0f
      while (currentCoroutineContext().isActive) {
        try {
          val processMemInfo = am.getProcessMemoryInfo(intArrayOf(Process.myPid()))[0]
          val pssInMBytes = processMemInfo.totalPss / KB_TO_MB
          lastPss = pssInMBytes
          emit(pssInMBytes)
        } catch (e: RuntimeException) {
          Log.e(LOGTAG, "error in querying the PSS", e)
          emit(lastPss)
        }
        delay(interval)
      }
    }
  }
}
