package com.ms.square.debugoverlay.internal.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * NOTE:
 *  Scrollbars add value when:
 *   - Content is unstructured/dense
 *   - Two-dimensional scrolling exists
 *   - No visual cues for content bounds
 */

private enum class ScrollbarOrientation { Vertical, Horizontal }

private const val SCROLLBAR_THICKNESS_DP = 6
private const val SCROLLBAR_MIN_LENGTH_DP = 24
private const val SCROLLBAR_ALPHA = 0.5f
private const val SCROLLBAR_FADE_DURATION_MS = 300
private const val SCROLLBAR_HIDE_DELAY_MS = 1500L

@Composable
internal fun VerticalScrollbar(scrollState: ScrollState, modifier: Modifier = Modifier) {
  val alpha by rememberScrollbarAlpha(scrollState, "verticalScrollbarAlpha")

  if (scrollState.maxValue > 0) {
    val density = LocalDensity.current

    BoxWithConstraints(
      modifier = modifier
        .width(SCROLLBAR_THICKNESS_DP.dp)
        .alpha(alpha)
    ) {
      val minLengthPx = with(density) { SCROLLBAR_MIN_LENGTH_DP.dp.toPx() }
      val (thumbLength, thumbOffset) = calculateThumbMetrics(
        density = density,
        trackLengthPx = constraints.maxHeight.toFloat(),
        scrollState = scrollState,
        minLengthPx = minLengthPx
      )

      ScrollbarThumb(
        orientation = ScrollbarOrientation.Vertical,
        thumbLength = thumbLength,
        thumbOffset = thumbOffset
      )
    }
  }
}

@Composable
internal fun HorizontalScrollbar(scrollState: ScrollState, modifier: Modifier = Modifier) {
  val alpha by rememberScrollbarAlpha(scrollState, "horizontalScrollbarAlpha")

  if (scrollState.maxValue > 0) {
    val density = LocalDensity.current

    BoxWithConstraints(
      modifier = modifier
        .height(SCROLLBAR_THICKNESS_DP.dp)
        .alpha(alpha)
    ) {
      val minLengthPx = with(density) { SCROLLBAR_MIN_LENGTH_DP.dp.toPx() }
      val (thumbLength, thumbOffset) = calculateThumbMetrics(
        density = density,
        trackLengthPx = constraints.maxWidth.toFloat(),
        scrollState = scrollState,
        minLengthPx = minLengthPx
      )

      ScrollbarThumb(
        orientation = ScrollbarOrientation.Horizontal,
        thumbLength = thumbLength,
        thumbOffset = thumbOffset
      )
    }
  }
}

@Composable
private fun ScrollbarThumb(orientation: ScrollbarOrientation, thumbLength: Dp, thumbOffset: Dp) {
  val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SCROLLBAR_ALPHA)
  val shape = RoundedCornerShape(SCROLLBAR_THICKNESS_DP.dp / 2)
  val thickness = SCROLLBAR_THICKNESS_DP.dp

  val thumbModifier = when (orientation) {
    ScrollbarOrientation.Vertical ->
      Modifier
        .padding(top = thumbOffset)
        .width(thickness)
        .height(thumbLength)

    ScrollbarOrientation.Horizontal ->
      Modifier
        .padding(start = thumbOffset)
        .width(thumbLength)
        .height(thickness)
  }

  Box(modifier = thumbModifier.background(color, shape))
}

/**
 * Remembers and animates scrollbar alpha based on scroll activity.
 * Shows immediately on scroll, fades out after [SCROLLBAR_HIDE_DELAY_MS] of inactivity.
 * Starts visible for initial "flash" indicator.
 */
@Composable
private fun rememberScrollbarAlpha(scrollState: ScrollState, label: String): State<Float> {
  var isVisible by remember { mutableStateOf(true) }

  LaunchedEffect(scrollState.isScrollInProgress, scrollState.value) {
    if (scrollState.isScrollInProgress) {
      isVisible = true
    } else {
      delay(SCROLLBAR_HIDE_DELAY_MS)
      isVisible = false
    }
  }

  return animateFloatAsState(
    targetValue = if (isVisible) 1f else 0f,
    animationSpec = tween(durationMillis = SCROLLBAR_FADE_DURATION_MS),
    label = label
  )
}

/**
 * Calculates scrollbar thumb size and offset.
 *
 * @param density The density for px/dp conversions
 * @param trackLengthPx The length of the scrollbar track in pixels
 * @param scrollState The scroll state to derive position from
 * @param minLengthPx Minimum thumb length in pixels
 * @return Pair of (thumbLength, thumbOffset) in Dp
 */
private fun calculateThumbMetrics(
  density: Density,
  trackLengthPx: Float,
  scrollState: ScrollState,
  minLengthPx: Float,
): Pair<Dp, Dp> {
  val totalContentPx = scrollState.maxValue + scrollState.viewportSize

  val thumbFraction = (scrollState.viewportSize.toFloat() / totalContentPx)
    .coerceAtLeast(minLengthPx / trackLengthPx)
  val thumbLengthDp = with(density) { (trackLengthPx * thumbFraction).toDp() }

  val scrollFraction = scrollState.value.toFloat() / scrollState.maxValue
  val availableTravel = trackLengthPx * (1f - thumbFraction)
  val thumbOffsetDp = with(density) { (availableTravel * scrollFraction).toDp() }

  return thumbLengthDp to thumbOffsetDp
}
