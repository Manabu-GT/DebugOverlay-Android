package com.ms.square.debugoverlay.internal.data.source

import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.util.Log
import com.ms.square.debugoverlay.internal.data.Percentage
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val LOGTAG = "CpuDataSource"
private val REGEX_FOR_STAT = " +".toRegex()

internal class CpuDataSource {

    private val numCpuCores = Os.sysconf(OsConstants._SC_NPROCESSORS_CONF)
    private val ticksPerSecond = Os.sysconf(OsConstants._SC_CLK_TCK)

    fun cpuUsage(interval: Duration = 1L.seconds): Flow<Percentage> {
        return flow {
            var myProcessCpuReader: BufferedReader? = null

            // Tracking variables
            var processStartTimeSec = 0.0
            var lastCpuTimeSec = 0.0
            var lastProcessTimeSec = 0.0

            try {
                while (currentCoroutineContext().isActive) {
                    try {
                        // Open reader if needed
                        if (myProcessCpuReader == null) {
                            myProcessCpuReader = BufferedReader(FileReader("/proc/self/stat"))
                        }

                        // Read CPU data
                        val cpuData = myProcessCpuReader.readLine()?.split(REGEX_FOR_STAT, limit = 23)

                        if (cpuData != null && cpuData.size >= 22) {
                            // Parse CPU time from /proc/self/stat
                            // Fields: utime(13), stime(14), cutime(15), cstime(16)
                            val cpuTimeTicks = cpuData[13].toDouble() +
                                    cpuData[14].toDouble() +
                                    cpuData[15].toDouble() +
                                    cpuData[16].toDouble()
                            val currentCpuTimeSec = cpuTimeTicks / ticksPerSecond

                            // Set process start time once (won't change)
                            if (processStartTimeSec == 0.0) {
                                // starttime(21) - time process started after boot
                                processStartTimeSec = cpuData[21].toDouble() / ticksPerSecond
                            }

                            val currentProcessTimeSec = SystemClock.elapsedRealtime() / 1000.0

                            // Calculate usage percentages (need at least one previous reading)
                            if (processStartTimeSec > 0 && lastCpuTimeSec > 0 && lastProcessTimeSec > 0) {
                                // Relative usage during the interval
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

                        // Close and reopen reader for next iteration
                        myProcessCpuReader.close()
                        myProcessCpuReader = null

                    } catch (e: FileNotFoundException) {
                        Log.w(LOGTAG, "Could not open '/proc/self/stat'", e)
                    } catch (e: IOException) {
                        Log.w(LOGTAG, "Failed reading CPU data", e)
                    } catch (e: NumberFormatException) {
                        Log.w(LOGTAG, "Failed parsing CPU data", e)
                    } catch (e: Exception) {
                        Log.e(LOGTAG, "Error reading CPU usage", e)
                    }

                    delay(interval)
                }
            } finally {
                // Cleanup
                try {
                    myProcessCpuReader?.close()
                } catch (_: IOException) {
                    // Ignore
                }
            }
        }
    }
}
