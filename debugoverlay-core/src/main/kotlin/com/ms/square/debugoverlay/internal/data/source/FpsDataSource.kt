package com.ms.square.debugoverlay.internal.data.source

import android.content.Context
import android.view.Choreographer
import com.ms.square.debugoverlay.internal.util.currentRefreshRate
import com.ms.square.debugoverlay.internal.util.defaultDisplay
import com.ms.square.debugoverlay.internal.util.maxSupportedFps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FpsDataSource(context: Context) {

  private val defaultDisplay = context.defaultDisplay()

  val currentTargetFps: Float
    get() = defaultDisplay.currentRefreshRate

  val maxSupportedFps: Float by lazy {
    defaultDisplay.maxSupportedFps
  }

  fun fps(interval: Duration = 1L.seconds): Flow<Float> = callbackFlow {
    val choreographer = Choreographer.getInstance()
    val calculator = FpsCalculator(
      intervalNanos = interval.inWholeNanoseconds,
      maxFps = maxSupportedFps
    )

    val callback = object : Choreographer.FrameCallback {
      override fun doFrame(frameTimeNanos: Long) {
        calculator.onFrame(frameTimeNanos)?.let { fps ->
          trySend(fps)
        }
        choreographer.postFrameCallback(this)
      }
    }

    choreographer.postFrameCallback(callback)

    // Keep the Flow active until collection is cancelled or the channel is closed
    awaitClose {
      choreographer.removeFrameCallback(callback)
    }
  }.flowOn(Dispatchers.Main.immediate)
}
