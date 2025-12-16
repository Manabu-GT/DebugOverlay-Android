package com.ms.square.debugoverlay.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
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
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.bugreport.ActivityProvider
import com.ms.square.debugoverlay.internal.data.source.DebugOverlayPanelDataSourceImpl
import com.ms.square.debugoverlay.internal.ui.DebugPanelActivity
import com.ms.square.debugoverlay.internal.ui.DraggableOverlayPanel
import com.ms.square.debugoverlay.internal.util.isDarkTheme
import kotlinx.coroutines.CoroutineScope
import java.lang.ref.WeakReference
import java.util.WeakHashMap

internal class OverlayViewManager(private val application: Application, private val overlayScope: CoroutineScope) :
  ActivityProvider {
  private val windowManager: WindowManager =
    application.getSystemService(Context.WINDOW_SERVICE) as WindowManager

  private val debugPanelDataSource by lazy { DebugOverlayPanelDataSourceImpl(application, overlayScope) }

  // Shared position state across all activities
  private var savedX: Int = 0
  private var savedY: Int = 0

  /**
   * The last app activity (excluding DebugPanelActivity) that was resumed.
   * Used for screenshot capture in bug reports.
   *
   * Uses WeakReference to avoid memory leaks if the activity is destroyed while referenced.
   * Cleared only on activity destroy, not on pause, so it remains available when
   * DebugPanelActivity is shown over the app activity.
   */
  @Volatile
  private var lastAppActivityRef: WeakReference<Activity>? = null

  // ActivityProvider implementation for bug report screenshot capture
  override val activity: Activity?
    get() = lastAppActivityRef?.get()

  init {
    application.registerActivityLifecycleCallbacks(ActivityLifecycleHandler())
  }

  private fun createLayoutParams(windowToken: IBinder): WindowManager.LayoutParams =
    WindowManager.LayoutParams().apply {
      width = WindowManager.LayoutParams.WRAP_CONTENT
      height = WindowManager.LayoutParams.WRAP_CONTENT
      token = windowToken
      // make layout of the window happens as that of a top-level window, not as a child of its container
      type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
      flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
      format = PixelFormat.TRANSLUCENT
      gravity = Gravity.TOP or Gravity.END
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        // disable the move window animation as not needed on Android 14+
        setCanPlayMoveAnimation(false)
      }
      x = savedX
      y = savedY
    }

  private fun createOverlayRoot(onPositionChanged: (Int, Int) -> Unit): Pair<ViewGroup, OverlayLifecycleOwner> {
    val lifecycleOwner = OverlayLifecycleOwner()
    return ComposeView(application).apply {
      // Create and attach a synthetic lifecycle for the overlay
      // This is needed because:
      // 1. ComposeView requires a lifecycle to manage composition
      // 2. ComposeView uses collectAsStateWithLifecycle()
      // 3. The view is attached via WindowManager, not in activity hierarchy
      setViewTreeLifecycleOwner(lifecycleOwner)
      setViewTreeSavedStateRegistryOwner(lifecycleOwner)

      // Start the lifecycle, call onStart as well for the activity overlay to start collecting data immediately.
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
          DraggableOverlayPanel(
            metrics = metrics,
            initialOffsetX = savedX.toFloat(),
            initialOffsetY = savedY.toFloat(),
            onPositionChanged = onPositionChanged,
            onClick = {
              val intent = Intent(application, DebugPanelActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              application.startActivity(intent)
            }
          )
        }
      }
      // Tag the ComposeView so UI hierarchy scan can filter it out
      setTag(R.id.debugoverlay_window_marker, true)
    } to lifecycleOwner
  }

  inner class ActivityLifecycleHandler : Application.ActivityLifecycleCallbacks {

    private val attachStateChangeListeners = WeakHashMap<Activity, OverlayViewAttachStateChangeListener>()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
      Logger.d("onCreate() called for ${activity.javaClass.simpleName}")
      // Don't create overlay for DebugPanelActivity - no need for it there
      if (activity !is DebugPanelActivity) {
        OverlayViewAttachStateChangeListener().also {
          activity.window.decorView.addOnAttachStateChangeListener(it)
          attachStateChangeListeners[activity] = it
        }
      }
    }

    override fun onActivityStarted(activity: Activity) {
      Logger.d("onStart() called for ${activity.javaClass.simpleName}")
    }

    override fun onActivityResumed(activity: Activity) {
      Logger.d("onResume() called for ${activity.javaClass.simpleName}")
      if (activity !is DebugPanelActivity) {
        lastAppActivityRef = WeakReference(activity)
        attachStateChangeListeners[activity]?.onActivityResumed()
        DebugOverlay.overlayDataRepository.startOrResumeJankStatsTracking(activity)
      }
    }

    override fun onActivityPaused(activity: Activity) {
      Logger.d("onPause() called for ${activity.javaClass.simpleName}")
      if (activity !is DebugPanelActivity) {
        attachStateChangeListeners[activity]?.onActivityPaused()
        DebugOverlay.overlayDataRepository.pauseJankStatsTracking(activity)
      }
    }

    override fun onActivityStopped(activity: Activity) {
      Logger.d("onStop() called for ${activity.javaClass.simpleName}")
      if (activity !is DebugPanelActivity) {
        attachStateChangeListeners[activity]?.onActivityStopped()
      }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
      Logger.d("onSaveInstanceState() called for ${activity.javaClass.simpleName}")
    }

    override fun onActivityDestroyed(activity: Activity) {
      Logger.d("onDestroy() called for ${activity.javaClass.simpleName}")
      if (activity !is DebugPanelActivity) {
        if (lastAppActivityRef?.get() === activity) {
          lastAppActivityRef = null
        }
        attachStateChangeListeners.remove(activity)
        DebugOverlay.overlayDataRepository.stopJankStatsTracking(activity)
      }
    }
  }

  inner class OverlayViewAttachStateChangeListener : View.OnAttachStateChangeListener {

    private var rootView: ViewGroup? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    fun onActivityResumed() {
      lifecycleOwner?.onResume()
      updatePosition(savedX, savedY)
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
      v.removeOnAttachStateChangeListener(this)
    }

    private fun showOverlay(windowToken: IBinder) {
      val params = createLayoutParams(windowToken)
      layoutParams = params

      createOverlayRoot(
        onPositionChanged = { x, y ->
          updatePosition(x, y)
        }
      ).also { (root, owner) ->
        rootView = root
        lifecycleOwner = owner
        windowManager.addView(root, params)
      }
    }

    private fun updatePosition(x: Int, y: Int) {
      savedX = x
      savedY = y
      val params = layoutParams ?: return
      val view = rootView?.takeIf { it.isAttachedToWindow } ?: return

      if (params.x != x || params.y != y) {
        params.x = x
        params.y = y
        windowManager.updateViewLayout(view, params)
      }
    }

    fun hideOverlay() {
      rootView?.let {
        lifecycleOwner?.onDestroy()
        // remove immediately so that WindowLeaked won't trigger
        windowManager.removeViewImmediate(it)
        rootView = null
        lifecycleOwner = null
        layoutParams = null
      }
    }
  }
}
