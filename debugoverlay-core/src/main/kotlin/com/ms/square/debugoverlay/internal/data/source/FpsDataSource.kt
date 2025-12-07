package com.ms.square.debugoverlay.internal.data.source

import android.content.Context
import android.view.Choreographer
import com.ms.square.debugoverlay.internal.util.defaultDisplay
import com.ms.square.debugoverlay.internal.util.maxSupportedFps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FpsDataSource(context: Context) {

  private val defaultDisplay = context.defaultDisplay()

  val currentTargetFps: Float
    get() = defaultDisplay.refreshRate

  val maxSupportedFps: Float by lazy {
    defaultDisplay.maxSupportedFps
  }

  fun fps(interval: Duration = 1L.seconds): Flow<Float> = callbackFlow {
    val callback = object : Choreographer.FrameCallback {
      var frameCount: Long = 0
      var startFrameTimeNanos: Long = 0

      override fun doFrame(frameTimeNanos: Long) {
        if (startFrameTimeNanos > 0) {
          frameCount++
          val elapsedTimeNanos = frameTimeNanos - startFrameTimeNanos
          if (elapsedTimeNanos >= interval.inWholeNanoseconds) {
            val fps = frameCount / TimeUnit.NANOSECONDS.toSeconds(elapsedTimeNanos).toFloat()
            trySend(fps.coerceIn(0f, maxSupportedFps))
            startFrameTimeNanos = frameTimeNanos
            frameCount = 0
          }
        } else {
          startFrameTimeNanos = frameTimeNanos
        }
        Choreographer.getInstance().postFrameCallback(this)
      }
    }

    Choreographer.getInstance().postFrameCallback(callback)

    // Keep the Flow active until collection is cancelled or the channel is closed
    awaitClose {
      Choreographer.getInstance().removeFrameCallback(callback)
    }
  }.flowOn(Dispatchers.Main)
}
