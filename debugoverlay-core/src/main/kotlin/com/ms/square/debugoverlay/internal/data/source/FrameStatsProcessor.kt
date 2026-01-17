package com.ms.square.debugoverlay.internal.data.source

import android.os.Build
import android.os.SystemClock
import androidx.annotation.GuardedBy
import androidx.annotation.WorkerThread
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.FrameDataApi24
import androidx.metrics.performance.FrameDataApi31
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import com.ms.square.debugoverlay.internal.data.EvictingQueue
import com.ms.square.debugoverlay.internal.data.Percentage
import com.ms.square.debugoverlay.internal.data.model.FrameInfo
import com.ms.square.debugoverlay.internal.data.model.JankStatsUiState
import com.ms.square.debugoverlay.internal.data.model.StateJankCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.TimeUnit

private const val MAX_FRAMES = 500
private const val RECENT_FRAMES_COUNT = 50
private const val TOP_STATES_COUNT = 5
private const val MAX_JANKY_FRAMES_DISPLAY = 20
private const val STATE_UPDATE_INTERVAL_MS = 1000L

/**
 * Processes frame timing data and maintains jank statistics.
 *
 * This is a pure data processing class with no Android lifecycle dependencies.
 * It receives [FrameData] from JankStats and maintains aggregated statistics
 * exposed via [state].
 */
@OptIn(InternalDebugOverlayApi::class)
internal class FrameStatsProcessor {

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

  @GuardedBy("lock")
  private var lastStateUpdateMs = 0L

  /**
   * Process a single frame's timing data.
   *
   * On Android 7+ (API 24+), this is called on a background HandlerThread by JankStats.
   * The method is thread-safe and updates [state] after processing.
   */
  @WorkerThread
  fun processFrame(frameData: FrameData) {
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

      // Update state counters for janky frames
      if (info.isJank) {
        val frameStates = info.frameStates
        frameStates.forEach { (key, value) ->
          val stateKey = if (key.isEmpty()) value else "$key=$value"
          stateCounters[stateKey] = (stateCounters[stateKey] ?: 0) + 1
        }
      }

      // Decrement state counters when janky frame is evicted
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

      // Throttle StateFlow emissions to ~1 update/sec to reduce UI churn and GC pressure
      val now = SystemClock.elapsedRealtime()
      if (now - lastStateUpdateMs >= STATE_UPDATE_INTERVAL_MS) {
        lastStateUpdateMs = now
        updateState()
      }
    }
  }

  /**
   * Updates the StateFlow with current statistics.
   * Must be called while holding [lock].
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
        jankyFramesList = framesList.asReversed().filter { it.isJank }.take(MAX_JANKY_FRAMES_DISPLAY)
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
