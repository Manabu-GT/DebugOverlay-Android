package com.ms.square.debugoverlay.internal.data.source

import android.app.Activity
import androidx.annotation.VisibleForTesting
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import com.ms.square.debugoverlay.internal.InternalDebugOverlayApi
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.model.JankStatsUiState
import kotlinx.coroutines.flow.StateFlow
import java.util.WeakHashMap

/**
 * Coordinates JankStats tracking across activity lifecycles.
 *
 * This is a thin integration layer over Android's JankStats API that manages
 * tracking instances per Activity. Frame processing logic is delegated to
 * [FrameStatsProcessor].
 */
@OptIn(InternalDebugOverlayApi::class)
internal class JankStatsDataSource(
  @VisibleForTesting
  private val processor: FrameStatsProcessor = FrameStatsProcessor(),
) {

  val state: StateFlow<JankStatsUiState> = processor.state

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
        processor.processFrame(frameData)
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
}
