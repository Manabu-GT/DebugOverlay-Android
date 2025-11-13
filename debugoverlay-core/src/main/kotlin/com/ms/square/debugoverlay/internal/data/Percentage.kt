package com.ms.square.debugoverlay.internal.data

import androidx.annotation.FloatRange

private const val MIN_PERCENTAGE_VALUE = 0f
private const val MAX_PERCENTAGE_VALUE = 100f

@JvmInline
internal value class Percentage private constructor(val value: Float) {

  companion object {
    fun ofClamped(@FloatRange(from = 0.0, to = 1.0) ratio: Float): Percentage =
      Percentage((ratio * MAX_PERCENTAGE_VALUE).coerceIn(MIN_PERCENTAGE_VALUE, MAX_PERCENTAGE_VALUE))
    fun ofClamped(@FloatRange(from = 0.0, to = 1.0) ratio: Double): Percentage = ofClamped(ratio.toFloat())
  }
}
