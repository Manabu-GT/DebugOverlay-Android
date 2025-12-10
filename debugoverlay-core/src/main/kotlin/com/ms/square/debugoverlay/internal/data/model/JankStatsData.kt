package com.ms.square.debugoverlay.internal.data.model

import com.ms.square.debugoverlay.internal.data.Percentage

/**
 * Information about a single rendered frame.
 */
internal data class FrameInfo(
  val timestampMs: Long,
  // Duration of the frame
  val durationUiMs: Long,
  // the time spent in the non-GPU portions of the frame
  val durationCpuMs: Long?, // API 24+, null otherwise
  // the amount of time past the frame deadline that the frame took to complete.
  val overrunMs: Long?, // API 31+, null if not overrun
  val isJank: Boolean,
  val states: List<Pair<String, String>>,
)

/**
 * Jank count for a specific UI state.
 */
internal data class StateJankCount(val state: String, val count: Int)

/**
 * UI state for the JankStats tab.
 */
internal data class JankStatsUiState(
  val totalFrames: Int = 0,
  val jankyFrames: Int = 0,
  val jankPercentage: Percentage = Percentage.ZERO,
  val avgFrameDurationMs: Long = 0,
  val recentFrameJanks: List<Boolean> = emptyList(),
  val stateBreakdown: List<StateJankCount> = emptyList(),
  val jankyFramesList: List<FrameInfo> = emptyList(),
) {
  internal companion object {
    val EMPTY = JankStatsUiState()
  }
}
