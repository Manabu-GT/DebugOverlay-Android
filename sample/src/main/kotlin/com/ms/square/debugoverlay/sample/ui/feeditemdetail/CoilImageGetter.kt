package com.ms.square.debugoverlay.sample.ui.feeditemdetail

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest

internal class CoilImageGetter(private val context: Context, private val callback: Drawable.Callback) :
  Html.ImageGetter {

  override fun getDrawable(source: String): Drawable? {
    if (source.endsWith(".gif")) {
      // just return an empty drawable for now if it's a gif..
      return BitmapDrawable()
    }
    val drawableWrapper = DrawableWrapper().apply {
      this.callback = this@CoilImageGetter.callback
    }
    val imageLoader = ImageLoader(context)
    imageLoader.enqueue(
      ImageRequest.Builder(context).data(source).apply {
        target { image ->
          val drawable = image.asDrawable(context.resources)
          drawableWrapper.drawable = drawable
          // use original image size
          drawableWrapper.bounds = Rect(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
          drawableWrapper.invalidateSelf()
        }
      }.build()
    )
    return drawableWrapper
  }
}

private class DrawableWrapper : Drawable() {
  var drawable: Drawable? = null

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    drawable?.bounds = bounds
  }

  override fun draw(canvas: Canvas) {
    drawable?.draw(canvas)
  }

  override fun setAlpha(alpha: Int) {
    drawable?.alpha = alpha
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    drawable?.colorFilter = colorFilter
  }

  @Deprecated("Deprecated in Java")
  override fun getOpacity(): Int = PixelFormat.UNKNOWN
}
