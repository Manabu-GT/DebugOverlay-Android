package com.ms.square.debugoverlay.internal.data.source

import androidx.metrics.performance.FrameData
import androidx.metrics.performance.StateInfo
import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.internal.data.Percentage
import com.ms.square.debugoverlay.internal.data.model.JankStatsUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class FrameStatsProcessorTest {

  private val processor = FrameStatsProcessor()

  // ==========================================================================
  // Initial State
  // ==========================================================================

  @Test
  fun `initial state is empty`() {
    val state = processor.state.value

    assertThat(state).isEqualTo(JankStatsUiState.EMPTY)
  }

  // ==========================================================================
  // Frame Counting
  // ==========================================================================

  @Test
  fun `processFrame increments totalFrames`() {
    processFrame(createFrameData(isJank = false, durationMs = 16))

    assertThat(processor.state.value.totalFrames).isEqualTo(1)

    processFrame(createFrameData(isJank = false, durationMs = 16))

    assertThat(processor.state.value.totalFrames).isEqualTo(2)
  }

  @Test
  fun `processFrame increments jankyFrames only for jank frames`() {
    processFrame(createFrameData(isJank = true))
    processFrame(createFrameData(isJank = false))
    processFrame(createFrameData(isJank = true))

    assertThat(processor.state.value.totalFrames).isEqualTo(3)
    assertThat(processor.state.value.jankyFrames).isEqualTo(2)
  }

  // ==========================================================================
  // Jank Percentage Calculation
  // ==========================================================================

  @Test
  fun `jankPercentage is 0 when no janky frames`() {
    repeat(10) {
      processFrame(createFrameData(isJank = false))
    }

    assertThat(processor.state.value.jankPercentage).isEqualTo(Percentage.ZERO)
  }

  @Test
  fun `jankPercentage is 100% when all frames are janky`() {
    repeat(10) {
      processFrame(createFrameData(isJank = true))
    }

    assertThat(processor.state.value.jankPercentage).isEqualTo(Percentage.ofClamped(1.0f))
  }

  @Test
  fun `jankPercentage calculated correctly for mixed frames`() {
    // 3 janky out of 10 = 30%
    repeat(7) { processFrame(createFrameData(isJank = false)) }
    repeat(3) { processFrame(createFrameData(isJank = true)) }

    assertThat(processor.state.value.jankPercentage).isEqualTo(Percentage.ofClamped(0.3f))
  }

  // ==========================================================================
  // Average Frame Duration
  // ==========================================================================

  @Test
  fun `avgFrameDurationMs calculated correctly`() {
    processFrame(createFrameData(durationMs = 10))
    processFrame(createFrameData(durationMs = 20))
    processFrame(createFrameData(durationMs = 30))

    // (10 + 20 + 30) / 3 = 20ms
    assertThat(processor.state.value.avgFrameDurationMs).isEqualTo(20L)
  }

  // ==========================================================================
  // Recent Janks Tracking
  // ==========================================================================

  @Test
  fun `recentFrameJanks tracks recent frames`() {
    processFrame(createFrameData(isJank = true))
    processFrame(createFrameData(isJank = false))
    processFrame(createFrameData(isJank = true))

    assertThat(processor.state.value.recentFrameJanks).containsExactly(true, false, true).inOrder()
  }

  @Test
  fun `recentFrameJanks limited to 50 frames`() {
    // Add 60 frames
    repeat(60) { i ->
      processFrame(createFrameData(isJank = i % 2 == 0))
    }

    assertThat(processor.state.value.recentFrameJanks).hasSize(50)
  }

  // ==========================================================================
  // Janky Frames List
  // ==========================================================================

  @Test
  fun `jankyFramesList contains only janky frames`() {
    processFrame(createFrameData(isJank = true, durationMs = 20))
    processFrame(createFrameData(isJank = false, durationMs = 16))
    processFrame(createFrameData(isJank = true, durationMs = 25))

    val jankyFrames = processor.state.value.jankyFramesList
    assertThat(jankyFrames).hasSize(2)
    assertThat(jankyFrames.all { it.isJank }).isTrue()
  }

  @Test
  fun `jankyFramesList limited to 20 frames`() {
    // Add 30 janky frames
    repeat(30) {
      processFrame(createFrameData(isJank = true))
    }

    assertThat(processor.state.value.jankyFramesList).hasSize(20)
  }

  @Test
  fun `jankyFramesList is in reverse order (most recent first)`() {
    processFrame(createFrameData(isJank = true, durationMs = 10))
    processFrame(createFrameData(isJank = true, durationMs = 20))
    processFrame(createFrameData(isJank = true, durationMs = 30))

    val jankyFrames = processor.state.value.jankyFramesList
    // Most recent (30ms) should be first
    assertThat(jankyFrames.map { it.durationUiMs })
      .containsExactly(30L, 20L, 10L)
      .inOrder()
  }

  // ==========================================================================
  // State Counters (Jank Breakdown by State)
  // ==========================================================================

  @Test
  fun `state counters incremented for janky frames`() {
    val jankyWithState = createFrameData(
      isJank = true,
      states = listOf("Activity" to "MainActivity")
    )

    processFrame(jankyWithState)
    processFrame(jankyWithState)

    val breakdown = processor.state.value.stateBreakdown
    assertThat(breakdown).isNotEmpty()

    val activityState = breakdown.find { it.state == "Activity=MainActivity" }
    assertThat(activityState?.count).isEqualTo(2)
  }

  @Test
  fun `state counters not incremented for non-janky frames`() {
    val normalWithState = createFrameData(
      isJank = false,
      states = listOf("Activity" to "MainActivity")
    )

    processFrame(normalWithState)

    assertThat(processor.state.value.stateBreakdown).isEmpty()
  }

  @Test
  fun `state with empty key uses value only`() {
    val frameWithEmptyKey = createFrameData(
      isJank = true,
      states = listOf("" to "SomeValue")
    )

    processFrame(frameWithEmptyKey)

    val breakdown = processor.state.value.stateBreakdown
    assertThat(breakdown.first().state).isEqualTo("SomeValue")
  }

  @Test
  fun `frames with no states get fallback state`() {
    val frameWithNoStates = createFrameData(
      isJank = true,
      states = emptyList()
    )

    processFrame(frameWithNoStates)

    val breakdown = processor.state.value.stateBreakdown
    assertThat(breakdown).isNotEmpty()
    assertThat(breakdown.first().state).isEqualTo("(no state)")
  }

  @Test
  fun `state counters track multiple states per frame`() {
    val frameWithMultipleStates = createFrameData(
      isJank = true,
      states = listOf(
        "Activity" to "MainActivity",
        "Fragment" to "HomeFragment",
        "RecyclerView" to "Scrolling"
      )
    )

    processFrame(frameWithMultipleStates)

    val breakdown = processor.state.value.stateBreakdown
    assertThat(breakdown).hasSize(3)
    assertThat(breakdown.map { it.state }).containsExactly(
      "Activity=MainActivity",
      "Fragment=HomeFragment",
      "RecyclerView=Scrolling"
    )
    assertThat(breakdown.all { it.count == 1 }).isTrue()
  }

  @Test
  fun `state breakdown limited to top 5 by count`() {
    // Create 10 different states with varying counts
    repeat(10) { i ->
      val state = createFrameData(
        isJank = true,
        states = listOf("State" to "State$i")
      )
      // Add state i+1 times (State0=1, State1=2, ..., State9=10)
      repeat(i + 1) { processFrame(state) }
    }

    val breakdown = processor.state.value.stateBreakdown

    // Should only return top 5
    assertThat(breakdown).hasSize(5)
    // Should be sorted descending by count
    assertThat(breakdown[0].state).isEqualTo("State=State9")
    assertThat(breakdown[0].count).isEqualTo(10)
  }

  // ==========================================================================
  // Eviction Behavior
  // ==========================================================================

  @Test
  fun `totalFrames counter tracks all frames including evicted ones`() {
    // Add 510 frames (exceeds internal buffer capacity of 500)
    repeat(510) {
      processFrame(createFrameData(isJank = false, durationMs = 16))
    }

    // Counter tracks all processed frames, not just buffered ones
    assertThat(processor.state.value.totalFrames).isEqualTo(510)
  }

  @Test
  fun `state counter stable when same-state janky frame replaces evicted one`() {
    // Fill with 500 janky frames with same state
    val jankyFrame = createFrameData(
      isJank = true,
      states = listOf("Screen" to "LoadingScreen")
    )
    repeat(500) { processFrame(jankyFrame) }

    // Verify counter is at 500
    val beforeEviction = processor.state.value.stateBreakdown
      .find { it.state == "Screen=LoadingScreen" }
    assertThat(beforeEviction?.count).isEqualTo(500)

    // Add one more janky frame with same state (triggers eviction of oldest)
    processFrame(jankyFrame)

    // Counter should stay at 500 (one added, one evicted)
    val afterEviction = processor.state.value.stateBreakdown
      .find { it.state == "Screen=LoadingScreen" }
    assertThat(afterEviction?.count).isEqualTo(500)
  }

  @Test
  fun `state counters decremented correctly when evicting different state`() {
    // Fill with 500 janky frames
    val firstState = createFrameData(
      isJank = true,
      states = listOf("Screen" to "FirstScreen")
    )
    repeat(500) { processFrame(firstState) }

    // Add janky frame with different state (evicts oldest FirstScreen)
    val secondState = createFrameData(
      isJank = true,
      states = listOf("Screen" to "SecondScreen")
    )
    processFrame(secondState)

    val breakdown = processor.state.value.stateBreakdown
    val firstScreenCount = breakdown.find { it.state == "Screen=FirstScreen" }?.count
    val secondScreenCount = breakdown.find { it.state == "Screen=SecondScreen" }?.count

    assertThat(firstScreenCount).isEqualTo(499)
    assertThat(secondScreenCount).isEqualTo(1)
  }

  @Test
  fun `state counter decremented when janky frame pushed out by non-janky frames`() {
    // Add one janky frame to establish a state counter
    val jankyFrame = createFrameData(
      isJank = true,
      states = listOf("Screen" to "HomeScreen")
    )
    processFrame(jankyFrame)

    // Fill with 500 non-janky frames to push out the janky one
    repeat(500) {
      processFrame(createFrameData(isJank = false))
    }

    // When the janky frame is evicted, its state counter should be decremented
    val breakdown = processor.state.value.stateBreakdown
    assertThat(breakdown.find { it.state == "Screen=HomeScreen" }).isNull()
  }

  // ==========================================================================
  // Test Helpers
  // ==========================================================================

  /**
   * Advances the shadow clock past the throttle interval (1000ms) and processes a frame,
   * ensuring state is updated immediately for test assertions.
   */
  private fun processFrame(frameData: FrameData) {
    // Advance past STATE_UPDATE_INTERVAL_MS (1000ms) to bypass throttling
    ShadowSystemClock.advanceBy(Duration.ofMillis(1001))
    processor.processFrame(frameData)
  }

  private fun createFrameData(
    isJank: Boolean = false,
    durationMs: Long = 16,
    states: List<Pair<String, String>> = emptyList(),
  ): FrameData {
    val statesList = states.map { (key, value) -> StateInfo(key, value) }
    return FrameData(
      frameStartNanos = 0L,
      frameDurationUiNanos = TimeUnit.MILLISECONDS.toNanos(durationMs),
      isJank = isJank,
      states = statesList
    )
  }
}
