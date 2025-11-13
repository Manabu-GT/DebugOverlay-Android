package com.ms.square.debugoverlay.internal.util

private const val MILLIS_PER_SECOND = 1000.0

// Generic time conversion extensions
internal fun Long.millisToSeconds(): Double = this / MILLIS_PER_SECOND
