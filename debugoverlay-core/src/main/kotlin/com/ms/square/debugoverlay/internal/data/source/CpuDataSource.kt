package com.ms.square.debugoverlay.internal.data.source

import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.Percentage
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

internal class CpuDataSource {

  private val numCpuCores = Os.sysconf(OsConstants._SC_NPROCESSORS_CONF)

  /**
   * A way for user-space applications to find out the granularity of the system's software clock,
   * which is maintained by the kernel and measures time in units called "jiffies."
   * It basically returns # of ticks per second.
   */
  private val ticksPerSecond = Os.sysconf(OsConstants._SC_CLK_TCK)

  @Suppress("MagicNumber")
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
          val cpuData = reader.readLine()?.split(REGEX_FOR_STAT, limit = 23)

          if (cpuData != null && cpuData.size >= 22) {
            // Parse CPU time from /proc/self/stat
            // Fields: utime(13), stime(14), cutime(15), cstime(16)
            val cpuTimeTicks = cpuData[13].toDouble() +
              cpuData[14].toDouble() +
              cpuData[15].toDouble() +
              cpuData[16].toDouble()
            val currentCpuTimeSec = cpuTimeTicks / ticksPerSecond
            val currentProcessTimeSec = SystemClock.elapsedRealtime() / 1000.0

            // Calculate usage percentages (need at least one previous reading)
            if (lastCpuTimeSec > 0 && lastProcessTimeSec > 0) {
              // Relative usage percent for this application during the interval
              val cpuTimeDeltaSec = currentCpuTimeSec - lastCpuTimeSec
              val processTimeDeltaSec = currentProcessTimeSec - lastProcessTimeSec
              val intervalUsagePercent =
                (100.0 * (cpuTimeDeltaSec / processTimeDeltaSec)) / numCpuCores

              emit(Percentage.ofClamped(intervalUsagePercent))
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
