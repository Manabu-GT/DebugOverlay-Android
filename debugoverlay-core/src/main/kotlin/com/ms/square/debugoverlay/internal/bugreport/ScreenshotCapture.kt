package com.ms.square.debugoverlay.internal.bugreport

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import com.ms.square.debugoverlay.internal.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Utility for capturing screenshots of app activities.
 *
 * - API 26+: Uses PixelCopy for accurate hardware-accelerated capture
 * - API 24-25: Falls back to View.draw() which may not capture some hardware layers
 *
 * Screenshot failures are handled gracefully - the bug report will still be generated
 * with a placeholder image if capture fails.
 */
internal object ScreenshotCapture {

  private const val MAX_DIMENSION = 1920
  private const val PIXEL_COPY_TIMEOUT_MS = 5000L
  private val mainHandler = Handler(Looper.getMainLooper())

  /**
   * Captures a screenshot of the given activity's window.
   *
   * @param activity The activity to capture
   * @return The captured bitmap, or null if capture failed
   */
  suspend fun capture(activity: Activity): Bitmap? {
    if (activity.isFinishing || activity.isDestroyed) {
      Logger.w("Cannot capture screenshot: activity is finishing or destroyed")
      return null
    }

    return runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        captureWithPixelCopy(activity.window)
      } else {
        captureWithCanvasOnMain(activity.window)
      }
    }.getOrElse { e ->
      if (e is CancellationException) throw e
      Logger.w("Screenshot capture failed", e)
      null
    }
  }

  /**
   * API 26+ capture using PixelCopy for accurate hardware-accelerated content.
   */
  @RequiresApi(Build.VERSION_CODES.O)
  private suspend fun captureWithPixelCopy(window: Window): Bitmap? {
    val decorView = window.decorView
    val width = decorView.width
    val height = decorView.height

    if (width <= 0 || height <= 0) {
      Logger.w("Invalid window dimensions: ${width}x$height")
      return null
    }

    val result = withTimeoutOrNull(PIXEL_COPY_TIMEOUT_MS) {
      suspendCancellableCoroutine { cont ->
        val (scaledWidth, scaledHeight) = calculateScaledDimensions(width, height)
        val bitmap = createBitmap(scaledWidth, scaledHeight)

        // Note: Don't use invokeOnCancellation to recycle bitmap - PixelCopy may still
        // be writing to it. Instead, recycle in the callback when cont.isActive == false.
        PixelCopy.request(
          window,
          bitmap,
          { copyResult ->
            // Coroutine may have been cancelled (timeout or external cancellation)
            if (!cont.isActive) {
              bitmap.recycle()
              return@request
            }

            if (copyResult == PixelCopy.SUCCESS) {
              cont.resume(Result.success(bitmap))
            } else {
              Logger.w("PixelCopy failed with result code: $copyResult")
              bitmap.recycle()
              cont.resume(Result.failure(RuntimeException("PixelCopy failed: $copyResult")))
            }
          },
          mainHandler
        )
      }
    }

    return when {
      result == null -> {
        Logger.w("PixelCopy timed out after ${PIXEL_COPY_TIMEOUT_MS}ms")
        null
      }
      else -> result.getOrNull() // Failure already logged in callback
    }
  }

  /**
   * Ensures captureWithCanvas runs on the main thread.
   * View.draw() must run on the main thread to avoid crashes or inconsistent output.
   */
  private suspend fun captureWithCanvasOnMain(window: Window): Bitmap? {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      return captureWithCanvas(window)
    }

    return suspendCancellableCoroutine { cont ->
      mainHandler.post {
        if (!cont.isActive) return@post
        val bitmap = runCatching {
          captureWithCanvas(window)
        }.getOrElse { e ->
          Logger.w("Canvas capture failed", e)
          null
        }
        // resume() is ignored if cancelled between isActive check and here
        cont.resume(bitmap)
      }
    }
  }

  /**
   * API 24-25 fallback using View.draw() to Canvas.
   * May not capture some hardware-accelerated layers accurately.
   * Must be called on the main thread.
   */
  private fun captureWithCanvas(window: Window): Bitmap? {
    val decorView = window.decorView
    val width = decorView.width
    val height = decorView.height

    if (width <= 0 || height <= 0) {
      Logger.w("Invalid window dimensions: ${width}x$height")
      return null
    }

    val (scaledWidth, scaledHeight) = calculateScaledDimensions(width, height)
    val bitmap = createBitmap(scaledWidth, scaledHeight)
    val canvas = Canvas(bitmap)

    // Scale canvas if dimensions differ
    if (scaledWidth != width || scaledHeight != height) {
      val scale = scaledWidth.toFloat() / width
      canvas.scale(scale, scale)
    }

    decorView.draw(canvas)
    return bitmap
  }

  private fun calculateScaledDimensions(width: Int, height: Int): Pair<Int, Int> {
    val scale = minOf(
      if (width > MAX_DIMENSION) MAX_DIMENSION.toFloat() / width else 1f,
      if (height > MAX_DIMENSION) MAX_DIMENSION.toFloat() / height else 1f
    )
    val scaledWidth = (width * scale).toInt().coerceAtLeast(1)
    val scaledHeight = (height * scale).toInt().coerceAtLeast(1)
    return scaledWidth to scaledHeight
  }
}
