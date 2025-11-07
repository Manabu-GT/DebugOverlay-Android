package com.ms.square.debugoverlay.internal

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.internal.data.source.DebugOverlayPanelDataSourceImpl
import com.ms.square.debugoverlay.internal.ui.DebugOverlayPanel

internal abstract class OverlayViewManager(protected val context: Context) {
  protected val windowManager: WindowManager =
    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  protected var rootView: ViewGroup? = null

  abstract fun showOverlay(windowToken: IBinder? = null)

  open fun hideOverlay() {
    rootView?.let {
      windowManager.removeView(it)
      rootView = null
    }
  }

  fun isOverlayShown(): Boolean = rootView != null

  open fun isOverlayPermissionRequested(): Boolean = false

  abstract fun createActivityLifecycleCallbacks(debugOverlay: DebugOverlay): Application.ActivityLifecycleCallbacks

  open fun setUpLifecycleOwnerOnComposeView(view: View, lifecycleOwner: OverlayLifecycleOwner) {
    // Move lifecycle to STARTED/RESUMED when the view is attached
    view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View) {
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()
      }

      override fun onViewDetachedFromWindow(v: View) {
        lifecycleOwner.onPause()
        lifecycleOwner.onStop()
        lifecycleOwner.onDestroy()
      }
    })
  }

  protected fun createLayoutParams(windowType: Int, windowToken: IBinder? = null): WindowManager.LayoutParams =
    WindowManager.LayoutParams().apply {
      width = WindowManager.LayoutParams.WRAP_CONTENT
      height = WindowManager.LayoutParams.WRAP_CONTENT
      if (windowToken != null) {
        token = windowToken
      }
      type = windowType
      flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
      format = PixelFormat.TRANSLUCENT
      gravity = Gravity.BOTTOM or Gravity.START
    }

  protected fun createRoot(): ViewGroup = ComposeView(context).apply {
    // Create and attach a synthetic lifecycle for the overlay
    // This is needed REGARDLESS of system layer mode because:
    // 1. ComposeView requires a lifecycle to manage composition
    // 2. DebugOverlayPanel uses collectAsStateWithLifecycle()
    // 3. The view is attached via WindowManager, not in activity hierarchy
    val lifecycleOwner = OverlayLifecycleOwner()
    setViewTreeLifecycleOwner(lifecycleOwner)
    setViewTreeSavedStateRegistryOwner(lifecycleOwner)

    // Start the lifecycle
    lifecycleOwner.onCreate()
    lifecycleOwner.onStart()
    setUpLifecycleOwnerOnComposeView(this, lifecycleOwner)

    setContent {
      val debugOverlayPanelMetricsFlow = remember(context) {
        DebugOverlayPanelDataSourceImpl(context).debugOverlayPanelMetrics()
      }
      val metrics by debugOverlayPanelMetricsFlow.collectAsStateWithLifecycle(initialValue = null)

      MaterialTheme(
        colorScheme = if (context.isDarkTheme()) {
          darkColorScheme()
        } else {
          lightColorScheme()
        }
      ) {
        DebugOverlayPanel(
          metrics = metrics,
          onClick = {
            // TODO: Navigate to detailed performance screen
          }
        )
      }
    }
  }
}

private fun Context.isDarkTheme(): Boolean {
  return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
