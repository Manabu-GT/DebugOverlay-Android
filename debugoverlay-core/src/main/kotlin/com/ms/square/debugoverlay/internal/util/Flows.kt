package com.ms.square.debugoverlay.internal.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.transform
import kotlin.time.Duration

internal fun <T> Flow<T>.throttleLatest(duration: Duration): Flow<T> = this
  .conflate()
  .transform {
    emit(it)
    delay(duration)
  }
