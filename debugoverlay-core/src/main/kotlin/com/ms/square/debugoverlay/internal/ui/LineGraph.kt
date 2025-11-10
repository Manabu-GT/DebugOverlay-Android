package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val DEFAULT_MAX_POINTS = 16
private const val DEFAULT_MIN_VALUE = 0f
private const val DEFAULT_MAX_VALUE = 100f

/**
 * minValue/maxValue is Nullable for auto-scaling the graph for unknown/unpredictable values.
 */
@Composable
internal fun LineGraph(
  data: List<Float>,
  color: Color,
  modifier: Modifier = Modifier,
  maxPoints: Int = DEFAULT_MAX_POINTS,
  strokeWidth: Dp = 2.dp,
  minValue: Float? = null,
  maxValue: Float? = null,
) {
  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .fillMaxHeight()
  ) {
    if (data.isEmpty()) return@Canvas

    val width = size.width
    val height = size.height

    // Calculate how many points we actually have
    val actualPoints = data.size.coerceAtMost(maxPoints)

    // Calculate the width ratio based on actual points vs max points
    val widthRatio = actualPoints.toFloat() / maxPoints
    val graphWidth = width * widthRatio

    // Start position (right-aligned)
    val startX = width - graphWidth

    // Calculate spacing between points
    val pointSpacing = if (actualPoints > 1) {
      graphWidth / (actualPoints - 1)
    } else {
      0f
    }

    // Take only the last 'maxPoints' data points
    val visibleData = data.takeLast(maxPoints)

    // Auto-calculate min/max if not provided
    val actualMin = minValue ?: visibleData.minOrNull() ?: DEFAULT_MIN_VALUE
    val actualMax = maxValue ?: visibleData.maxOrNull() ?: DEFAULT_MAX_VALUE
    val range = (actualMax - actualMin)
    if (range <= 0) {
      return@Canvas
    }

    // Build the path from end to start (right to left)
    val path = Path()
    visibleData.forEachIndexed { index, value ->
      // Normalize value to 0-1 range
      val normalizedValue = ((value - actualMin) / range).coerceIn(0f, 1f)

      // Calculate position
      val x = startX + (index * pointSpacing)
      val y = height - (normalizedValue * height)

      if (index == 0) {
        path.moveTo(x, y)
      } else {
        path.lineTo(x, y)
      }
    }

    // Draw the line
    drawPath(
      path = path,
      color = color,
      style = Stroke(
        width = strokeWidth.toPx(),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
      )
    )
  }
}
