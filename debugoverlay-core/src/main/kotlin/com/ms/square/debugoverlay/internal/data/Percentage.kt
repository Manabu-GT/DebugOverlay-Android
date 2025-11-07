package com.ms.square.debugoverlay.internal.data

@JvmInline
internal value class Percentage private constructor(val value: Float) {

  companion object {
    fun ofClamped(value: Float): Percentage = Percentage(value.coerceIn(0f, 100f))
    fun ofClamped(value: Double): Percentage = ofClamped(value.toFloat())
  }
}
