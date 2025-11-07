package com.ms.square.debugoverlay.internal.data

private const val MIN_PERCENTAGE_VALUE = 0f
private const val MAX_PERCENTAGE_VALUE = 100f

@JvmInline
internal value class Percentage private constructor(val value: Float) {

  companion object {
    fun ofClamped(value: Float): Percentage = Percentage(value.coerceIn(MIN_PERCENTAGE_VALUE, MAX_PERCENTAGE_VALUE))
    fun ofClamped(value: Double): Percentage = ofClamped(value.toFloat())
  }
}
