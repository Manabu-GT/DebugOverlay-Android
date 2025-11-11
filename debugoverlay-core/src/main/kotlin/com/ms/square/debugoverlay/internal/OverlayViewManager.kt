package com.ms.square.debugoverlay.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ms.square.debugoverlay.internal.data.source.DebugOverlayPanelDataSourceImpl
import com.ms.square.debugoverlay.internal.ui.DebugOverlayPanel
import kotlinx.coroutines.CoroutineScope
import java.util.WeakHashMap

internal class OverlayViewManager(private val application: Application, private val overlayScope: CoroutineScope) {
  private val windowManager: WindowManager =
    application.getSystemService(Context.WINDOW_SERVICE) as WindowManager

  private val activityLifecycleHandler = ActivityLifecycleHandler().also {
    application.registerActivityLifecycleCallbacks(it)
  }

  private val debugPanelDataSource by lazy { DebugOverlayPanelDataSourceImpl(application, overlayScope) }

  fun cleanUp() {
    activityLifecycleHandler.cleanUp()
    application.unregisterActivityLifecycleCallbacks(activityLifecycleHandler)
  }

  private fun createLayoutParams(windowToken: IBinder): WindowManager.LayoutParams =
    WindowManager.LayoutParams().apply {
      width = WindowManager.LayoutParams.WRAP_CONTENT
      height = WindowManager.LayoutParams.WRAP_CONTENT
      token = windowToken
      type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
      flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
      format = PixelFormat.TRANSLUCENT
      gravity = Gravity.BOTTOM or Gravity.START
    }

  private fun createOverlayRoot(): Pair<ViewGroup, OverlayLifecycleOwner> {
    val lifecycleOwner = OverlayLifecycleOwner()
    return ComposeView(application).apply {
      // Create and attach a synthetic lifecycle for the overlay
      // This is needed because:
      // 1. ComposeView requires a lifecycle to manage composition
      // 2. ComposeView uses collectAsStateWithLifecycle()
      // 3. The view is attached via WindowManager, not in activity hierarchy
      setViewTreeLifecycleOwner(lifecycleOwner)
      setViewTreeSavedStateRegistryOwner(lifecycleOwner)

      // Start the lifecycle, call onStart as well for the activity overlay case to work properly.
      lifecycleOwner.onCreate()
      lifecycleOwner.onStart()

      setContent {
        val metrics by debugPanelDataSource.debugOverlayPanelMetrics.collectAsStateWithLifecycle(initialValue = null)
        // Observe configuration changes for theme adaptation
        val isDarkTheme = LocalConfiguration.current.isDarkTheme()

        MaterialTheme(
          colorScheme = if (isDarkTheme) {
            darkColorScheme()
          } else {
            lightColorScheme()
          }
        ) {
          DebugOverlayPanel(
            metrics = metrics,
            onClick = {
              // Navigate to detailed performance screen
            }
          )
        }
      }
    } to lifecycleOwner
  }

  inner class ActivityLifecycleHandler : Application.ActivityLifecycleCallbacks {

    private val attachStateChangeListeners = WeakHashMap<Activity, OverlayViewAttachStateChangeListener>()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
      Logger.d("onCreate() called for ${activity.javaClass.simpleName}")
      OverlayViewAttachStateChangeListener().also {
        activity.window.decorView.addOnAttachStateChangeListener(it)
        attachStateChangeListeners.put(activity, it)
      }
    }

    override fun onActivityStarted(activity: Activity) {
      Logger.d("onStart() called for ${activity.javaClass.simpleName}")
      attachStateChangeListeners[activity]?.onActivityStarted()
    }

    override fun onActivityResumed(activity: Activity) {
      Logger.d("onResume() called for ${activity.javaClass.simpleName}")
      attachStateChangeListeners[activity]?.onActivityResumed()
    }

    override fun onActivityPaused(activity: Activity) {
      Logger.d("onPause() called for ${activity.javaClass.simpleName}")
      attachStateChangeListeners[activity]?.onActivityPaused()
    }

    override fun onActivityStopped(activity: Activity) {
      Logger.d("onStop() called for ${activity.javaClass.simpleName}")
      attachStateChangeListeners[activity]?.onActivityStopped()
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
      Logger.d("onSaveInstanceState() called for ${activity.javaClass.simpleName}")
    }

    override fun onActivityDestroyed(activity: Activity) {
      Logger.d("onDestroy() called for ${activity.javaClass.simpleName}")
      attachStateChangeListeners.remove(activity)?.also {
        activity.window.decorView.removeOnAttachStateChangeListener(it)
      }
    }

    fun cleanUp() {
      attachStateChangeListeners.values.forEach {
        it.hideOverlay()
      }
      attachStateChangeListeners.clear()
    }
  }

  inner class OverlayViewAttachStateChangeListener : View.OnAttachStateChangeListener {

    private var rootView: ViewGroup? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun onActivityStarted() {
      lifecycleOwner?.onStart()
    }

    fun onActivityResumed() {
      lifecycleOwner?.onResume()
    }

    fun onActivityPaused() {
      lifecycleOwner?.onPause()
    }

    fun onActivityStopped() {
      lifecycleOwner?.onStop()
    }

    override fun onViewAttachedToWindow(v: View) {
      showOverlay(v.windowToken)
    }

    override fun onViewDetachedFromWindow(v: View) {
      hideOverlay()
    }

    private fun showOverlay(windowToken: IBinder) {
      createOverlayRoot().also {
        rootView = it.first
        lifecycleOwner = it.second
      }
      // make layout of the window happens as that of a top-level window, not as a child of its container
      windowManager.addView(
        rootView,
        createLayoutParams(windowToken)
      )
    }

    fun hideOverlay() {
      rootView?.let {
        lifecycleOwner?.onDestroy()
        windowManager.removeView(it)
        rootView = null
        lifecycleOwner = null
      }
    }
  }
}

private fun Configuration.isDarkTheme(): Boolean =
  (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
