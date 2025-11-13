package com.ms.square.debugoverlay.internal.data.source

import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import androidx.annotation.FloatRange
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.Percentage
import com.ms.square.debugoverlay.internal.util.millisToSeconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val REGEX_FOR_STAT = " +".toRegex()

// This process (user)
private const val STAT_UTIME_INDEX = 13

// This process (kernel)
private const val STAT_STIME_INDEX = 14

// Children (user)
private const val STAT_CUTIME_INDEX = 15

// Children (kernel)
private const val STAT_CSTIME_INDEX = 16

// +2: array needs size 17 for index 16, split limit is size+1
private const val REGEX_SPLIT_LIMIT = STAT_CSTIME_INDEX + 2

internal class CpuDataSource {

  private val numCpuCores = Os.sysconf(OsConstants._SC_NPROCESSORS_CONF)

  /**
   * A way for user-space applications to find out the granularity of the system's software clock,
   * which is maintained by the kernel and measures time in units called "jiffies."
   * It basically returns # of ticks per second.
   */
  private val ticksPerSecond = Os.sysconf(OsConstants._SC_CLK_TCK)

  fun cpuUsage(interval: Duration = 1L.seconds): Flow<Percentage> = flow {
    // Tracking variables
    var lastCpuTimeSec = 0.0
    var lastProcessTimeSec = 0.0

    while (currentCoroutineContext().isActive) {
      try {
        // opens /proc/self/stat file per iteration to read the latest value.
        BufferedReader(FileReader("/proc/self/stat")).use { reader ->
          // Read CPU data
          // Ref... Section 1.8 in https://www.kernel.org/doc/Documentation/filesystems/proc.txt and manpage of proc
          val cpuData = reader.readLine()?.split(REGEX_FOR_STAT, limit = REGEX_SPLIT_LIMIT)
          if (cpuData != null && cpuData.size > STAT_CSTIME_INDEX) {
            // Parse CPU time from /proc/self/stat
            // Fields: utime(13), stime(14), cutime(15), cstime(16)
            val cpuTimeTicks = cpuData[STAT_UTIME_INDEX].toDouble() +
              cpuData[STAT_STIME_INDEX].toDouble() +
              cpuData[STAT_CUTIME_INDEX].toDouble() +
              cpuData[STAT_CSTIME_INDEX].toDouble()
            val currentCpuTimeSec = cpuTimeTicks / ticksPerSecond
            val currentProcessTimeSec = SystemClock.elapsedRealtime().millisToSeconds()

            // Calculate usage ratio (need at least one previous reading)
            if (lastCpuTimeSec > 0 && lastProcessTimeSec > 0) {
              // Relative usage for this application during the interval
              val cpuTimeDeltaSec = currentCpuTimeSec - lastCpuTimeSec
              val processTimeDeltaSec = currentProcessTimeSec - lastProcessTimeSec

              @FloatRange(from = 0.0, to = 1.0)
              val cpuUsageRatio =
                (cpuTimeDeltaSec / processTimeDeltaSec) / numCpuCores

              emit(Percentage.ofClamped(cpuUsageRatio))
            }

            // Update previous values for next iteration
            lastCpuTimeSec = currentCpuTimeSec
            lastProcessTimeSec = currentProcessTimeSec
          }
        }
      } catch (e: FileNotFoundException) {
        Logger.w("Could not open '/proc/self/stat'", e)
      } catch (e: IOException) {
        Logger.w("Failed reading CPU data", e)
      } catch (e: NumberFormatException) {
        Logger.w("Failed parsing CPU data", e)
      }
      delay(interval)
    }
  }.flowOn(Dispatchers.IO)
}
