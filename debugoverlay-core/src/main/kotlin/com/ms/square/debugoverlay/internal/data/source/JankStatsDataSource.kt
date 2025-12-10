package com.ms.square.debugoverlay.internal.data.source

import android.app.Activity
import android.os.Build
import androidx.annotation.GuardedBy
import androidx.annotation.WorkerThread
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.FrameDataApi24
import androidx.metrics.performance.FrameDataApi31
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.EvictingQueue
import com.ms.square.debugoverlay.internal.data.Percentage
import com.ms.square.debugoverlay.internal.data.model.FrameInfo
import com.ms.square.debugoverlay.internal.data.model.JankStatsUiState
import com.ms.square.debugoverlay.internal.data.model.StateJankCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.WeakHashMap
import java.util.concurrent.TimeUnit
import kotlin.collections.ifEmpty

private const val MAX_FRAMES = 500
private const val RECENT_FRAMES_COUNT = 50
private const val TOP_STATES_COUNT = 5
private const val MAX_JANKY_FRAMES_DISPLAY = 20

internal class JankStatsDataSource {

  private val lock = Object()

  private val _state = MutableStateFlow(JankStatsUiState.EMPTY)
  val state: StateFlow<JankStatsUiState> = _state.asStateFlow()

  private val frames = EvictingQueue<FrameInfo>(MAX_FRAMES)
  private val recentJanks = EvictingQueue<Boolean>(RECENT_FRAMES_COUNT)

  @GuardedBy("lock")
  private var totalFrames = 0

  @GuardedBy("lock")
  private var jankyFrames = 0

  @GuardedBy("lock")
  private var totalDurationMs = 0L

  @GuardedBy("lock")
  private val stateCounters = mutableMapOf<String, Int>()

  // Track all live host app activities
  private val trackedActivities = WeakHashMap<Activity, JankStats>()

  /**
   * Start or resume tracking frames for the given Activity.
   * Multiple activities can be tracked simultaneously.
   */
  fun startOrResumeTracking(activity: Activity) {
    // Already tracking this activity
    if (trackedActivities.containsKey(activity)) {
      trackedActivities[activity]?.isTrackingEnabled = true
      return
    }

    runCatching {
      val jankStats = JankStats.createAndTrack(activity.window) { frameData ->
        processFrame(frameData)
      }
      trackedActivities[activity] = jankStats

      // Inject Activity name as default state
      PerformanceMetricsState.getHolderForHierarchy(activity.window.decorView)
        .state?.putState("Activity", activity.javaClass.simpleName)
    }.onFailure { e ->
      // JankStats may fail on some devices (e.g., missing window token)
      Logger.w("Failed to create JankStats for ${activity.javaClass.simpleName}", e)
    }
  }

  /**
   * Pause tracking for the given Activity (when it goes to background).
   */
  fun pauseTracking(activity: Activity) {
    trackedActivities[activity]?.isTrackingEnabled = false
  }

  /**
   * Stop tracking for the given Activity (when it's destroyed).
   */
  fun stopTracking(activity: Activity) {
    trackedActivities.remove(activity)?.isTrackingEnabled = false
  }

  /**
   * On Android version 7 (API level 24) and higher, this will be called on the non-UI thread.
   */
  @WorkerThread
  private fun processFrame(frameData: FrameData) {
    val durationUiMs = TimeUnit.NANOSECONDS.toMillis(frameData.frameDurationUiNanos)

    val info = FrameInfo(
      timestampMs = System.currentTimeMillis(),
      durationUiMs = durationUiMs,
      durationCpuMs = getCpuDurationMs(frameData),
      overrunMs = getOverrunMs(frameData),
      isJank = frameData.isJank,
      states = frameData.states.map { it.key to it.value }
    )

    synchronized(lock) {
      val evicted = frames.add(info)
      recentJanks.add(frameData.isJank)

      // Update state counters
      if (info.isJank) {
        val frameStates = info.frameStates
        frameStates.forEach { (key, value) ->
          val stateKey = if (key.isEmpty()) value else "$key=$value"
          stateCounters[stateKey] = (stateCounters[stateKey] ?: 0) + 1
        }
      }

      if (evicted != null && evicted.isJank) {
        val frameStates = evicted.frameStates
        frameStates.forEach { (key, value) ->
          val stateKey = if (key.isEmpty()) value else "$key=$value"
          val count = stateCounters[stateKey] ?: 0
          if (count > 1) {
            stateCounters[stateKey] = count - 1
          } else {
            stateCounters.remove(stateKey)
          }
        }
      }

      totalFrames++
      totalDurationMs += durationUiMs
      if (frameData.isJank) jankyFrames++

      updateState()
    }
  }

  /**
   * Updates the StateFlow with current statistics.
   * IMPORTANT: Must be called while holding [lock] to ensure consistent reads of
   * [totalFrames], [jankyFrames], and [totalDurationMs].
   */
  @GuardedBy("lock")
  private fun updateState() {
    val framesList = frames.toList()
    val recentJanksList = recentJanks.toList()
    val breakdown = computeStateBreakdown()

    _state.update {
      JankStatsUiState(
        totalFrames = totalFrames,
        jankyFrames = jankyFrames,
        jankPercentage = if (totalFrames > 0) {
          Percentage.ofClamped(jankyFrames.toFloat() / totalFrames)
        } else {
          Percentage.ZERO
        },
        avgFrameDurationMs = if (totalFrames > 0) {
          totalDurationMs / totalFrames
        } else {
          0L
        },
        recentFrameJanks = recentJanksList,
        stateBreakdown = breakdown,
        jankyFramesList = framesList.filter { it.isJank }.takeLast(MAX_JANKY_FRAMES_DISPLAY).reversed()
      )
    }
  }

  private fun computeStateBreakdown(): List<StateJankCount> = stateCounters
    .map { StateJankCount(it.key, it.value) }
    .sortedByDescending { it.count }
    .take(TOP_STATES_COUNT)

  private fun getCpuDurationMs(frameData: FrameData): Long? = if (frameData is FrameDataApi24) {
    TimeUnit.NANOSECONDS.toMillis(frameData.frameDurationCpuNanos)
  } else {
    null
  }

  private fun getOverrunMs(frameData: FrameData): Long? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
    frameData.isJank &&
    frameData is FrameDataApi31
  ) {
    val overrunNanos = frameData.frameOverrunNanos
    if (overrunNanos > 0) TimeUnit.NANOSECONDS.toMillis(overrunNanos) else null
  } else {
    null
  }

  private val FrameInfo.frameStates
    get() = states.ifEmpty { listOf("" to "(no state)") }
}
