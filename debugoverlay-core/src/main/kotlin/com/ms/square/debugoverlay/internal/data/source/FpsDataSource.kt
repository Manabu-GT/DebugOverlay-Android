package com.ms.square.debugoverlay.internal.data.source

import android.view.Choreographer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FpsDataSource(private val displayDataSource: DisplayDataSource) {

  fun fps(interval: Duration = 1L.seconds) : Flow<Float> {
    return callbackFlow {
      var frameCount: Long = 0
      var startFrameTimeNanos: Long = 0

      val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
          if (startFrameTimeNanos > 0) {
            frameCount++
            val elapsedTimeNanos = frameTimeNanos - startFrameTimeNanos
            if (elapsedTimeNanos >= interval.inWholeNanoseconds) {
              val fps = frameCount / TimeUnit.NANOSECONDS.toSeconds(elapsedTimeNanos).toFloat()
              trySend(fps.coerceIn(0f, displayDataSource.maxSupportedRefreshRate.value))
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
    }
  }
}
