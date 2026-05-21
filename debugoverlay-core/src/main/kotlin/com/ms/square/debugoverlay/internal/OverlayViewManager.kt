package com.ms.square.debugoverlay.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.OverlayMode
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.bugreport.ActivityProvider
import com.ms.square.debugoverlay.internal.bugreport.ui.BugReportActivity
import com.ms.square.debugoverlay.internal.bugreport.ui.DraggableBugReporterFab
import com.ms.square.debugoverlay.internal.data.model.ThermalState
import com.ms.square.debugoverlay.internal.data.source.DebugOverlayPanelDataSourceImpl
import com.ms.square.debugoverlay.internal.data.source.OverlayPreferences
import com.ms.square.debugoverlay.internal.data.source.SharedPreferencesOverlayPreferences
import com.ms.square.debugoverlay.internal.ui.DebugPanelActivity
import com.ms.square.debugoverlay.internal.ui.DraggableOverlayPanel
import com.ms.square.debugoverlay.internal.util.findActivityOrNull
import com.ms.square.debugoverlay.internal.util.isDarkTheme
import curtains.Curtains
import curtains.OnRootViewsChangedListener
import curtains.phoneWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.lang.ref.WeakReference

private const val OVERLAY_UPDATE_DEBOUNCE_MS = 100L

internal class OverlayViewManager(
  private val application: Application,
  private val overlayScope: CoroutineScope,
  initialOverlayMode: OverlayMode,
) : ActivityProvider {

  private val windowManager: WindowManager =
    application.getSystemService(Context.WINDOW_SERVICE) as WindowManager

  private val overlayPreferences: OverlayPreferences = SharedPreferencesOverlayPreferences(application)

  private val debugPanelDataSource by lazy { DebugOverlayPanelDataSourceImpl(application, overlayScope) }

  /** Observable overlay mode that triggers recomposition when changed. */
  internal val overlayMode = MutableStateFlow(initialOverlayMode)

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

  private var currentTargetWindowView: View? = null
  private var currentOverlayView: ViewGroup? = null
  private var currentLayoutParams: WindowManager.LayoutParams? = null
  private var currentLifecycleOwner: OverlayLifecycleOwner? = null

  private var pendingUpdate: Runnable? = null

  private val handler = Handler(Looper.getMainLooper())

  private val rootsChangedListener = OnRootViewsChangedListener { view, added ->
    // skip if it is currentOverlayView as no update is needed for it.
    if (view === currentOverlayView) {
      return@OnRootViewsChangedListener
    }
    // Hide overlay when its target window is removed to avoid window leaks when an app is dismissed
    if (view === currentTargetWindowView && !added) {
      hideOverlay()
    }
    pendingUpdate?.let { handler.removeCallbacks(it) }
    pendingUpdate = Runnable { updateOverlayAttachment() }.also {
      // If a new window is added or removed, check if we need to move the overlay
      // We process this on the next frame or immediately to ensure we have the latest state
      // Small debounce is added to minimize updates when window changes rapidly
      handler.postDelayed(it, OVERLAY_UPDATE_DEBOUNCE_MS)
    }
  }

  init {
    application.registerActivityLifecycleCallbacks(ActivityLifecycleHandler())

    // Use Curtains to track window changes and ensure overlay is always on top
    // Note: We don't remove this listener as OverlayViewManager is scoped to the application/singleton
    // and mimics the process lifecycle at the moment.
    Curtains.onRootViewsChangedListeners += rootsChangedListener

    // React to runtime mode changes from configure { overlayMode = ... }.
    // Without this, after a Hidden -> FullMetrics toggle, the overlay won't reappear
    // until the next Curtains event (since hideOverlay() may have already destroyed
    // the ComposeView that would have observed the StateFlow). drop(1) skips the
    // initial value — initial attach is handled by the Curtains listener.
    overlayMode.drop(1).distinctUntilChanged().onEach {
      updateOverlayAttachment()
    }.flowOn(Dispatchers.Main).launchIn(overlayScope)
  }

  private fun updateOverlayAttachment() {
    // Hidden mode: tear down any existing window and skip attachment work
    if (overlayMode.value is OverlayMode.Hidden) {
      hideOverlay()
      return
    }

    // if no better target, just return
    val targetWindowView = findBestTargetWindow() ?: return

    // If already attached to the correct target window, just ensure its lifecycle is resumed.
    if (currentOverlayView != null &&
      currentLayoutParams?.token == targetWindowView.windowToken &&
      currentTargetWindowView === targetWindowView
    ) {
      currentLifecycleOwner?.onResume()
      return
    }

    // Move to new window - save position first so it's preserved across window transitions
    savePosition()
    hideOverlay()
    currentTargetWindowView = targetWindowView
    showOverlay(targetWindowView.windowToken)
  }

  private fun findBestTargetWindow(): View? {
    // Get all root views (windows) in the app
    val rootViews = Curtains.rootViews

    // Find the topmost window that:
    // 1. Is not our overlay window itself
    // 2. Is not system window
    // 3. Is an Activity or Dialog window (has a token we can attach to)
    // 4. Is visible and has a valid window token
    // 5. Does not belong to a DebugOverlay activity (DebugPanelActivity, BugReportActivity)
    return rootViews.lastOrNull { view ->
      // Exclude our own overlay
      if (view.getTag(R.id.debugoverlay_window_marker) == true) return@lastOrNull false

      // Avoid system specific windows (>= 2000) as we can't attach to them.
      val type = (view.layoutParams as? WindowManager.LayoutParams)?.type ?: 0
      if (type >= FIRST_SYSTEM_WINDOW) return@lastOrNull false

      // Check if it is a phone window we can attach to and skip if not.
      if (view.phoneWindow == null) {
        return@lastOrNull false
      }

      // Exclude windows belonging to DebugOverlay's own activities
      if (view.findActivityOrNull()?.isDebugOverlayActivity() == true) return@lastOrNull false

      view.windowToken != null && view.isShown
    }
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
        // Note: Move animation on pre-Android 14 cannot be disabled via public API.
        // It only occurs during window reparenting (e.g., when dialogs open), so should not be critical.
        setCanPlayMoveAnimation(false)
      }
      x = overlayPreferences.getOverlayX()
      y = overlayPreferences.getOverlayY()
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
      // Always resume when creating, ActivityLifecycleHandler handles its update afterwards.
      lifecycleOwner.onResume()

      setContent {
        // Observe configuration changes for theme adaptation
        val isDarkTheme = LocalConfiguration.current.isDarkTheme()

        MaterialTheme(
          colorScheme = if (isDarkTheme) {
            darkColorScheme()
          } else {
            lightColorScheme()
          }
        ) {
          val currentOverlayMode by overlayMode.collectAsStateWithLifecycle()
          when (val mode = currentOverlayMode) {
            is OverlayMode.FullMetrics -> {
              val metrics by debugPanelDataSource.debugOverlayPanelMetrics.collectAsStateWithLifecycle(
                initialValue = null
              )
              val thermalFlow: Flow<ThermalState?> = remember(mode.showThermal) {
                if (mode.showThermal) debugPanelDataSource.thermalState else flowOf(null)
              }
              val thermalState by thermalFlow.collectAsStateWithLifecycle(initialValue = null)
              DraggableOverlayPanel(
                metrics = metrics,
                thermalState = thermalState,
                initialOffsetX = overlayPreferences.getOverlayX().toFloat(),
                initialOffsetY = overlayPreferences.getOverlayY().toFloat(),
                onPositionChanged = onPositionChanged,
                onClick = { DebugOverlay.openPanel(application) }
              )
            }
            OverlayMode.BugReporterOnly -> {
              DraggableBugReporterFab(
                initialOffsetX = overlayPreferences.getOverlayX().toFloat(),
                initialOffsetY = overlayPreferences.getOverlayY().toFloat(),
                onPositionChanged = onPositionChanged,
                onError = { errorMessage ->
                  Toast.makeText(application, errorMessage, Toast.LENGTH_SHORT).show()
                }
              )
            }
            // Transient state during the brief main-thread hop between overlayMode
            // flipping to Hidden and updateOverlayAttachment() running to tear the
            // window down.
            is OverlayMode.Hidden -> Unit
          }
        }
      }
      // Tag the ComposeView so UI hierarchy scan can filter it out
      setTag(R.id.debugoverlay_window_marker, true)
    } to lifecycleOwner
  }

  private fun showOverlay(windowToken: IBinder) {
    val params = createLayoutParams(windowToken)
    currentLayoutParams = params

    createOverlayRoot(
      onPositionChanged = { x, y ->
        updatePosition(x, y)
      }
    ).also { (root, owner) ->
      currentOverlayView = root
      currentLifecycleOwner = owner
      try {
        windowManager.addView(root, params)
      } catch (e: WindowManager.BadTokenException) {
        // Token might have become invalid in the meantime
        Logger.w("Failed to add overlay view: ${e.message}")
        hideOverlay()
      }
    }
  }

  private fun hideOverlay() {
    currentOverlayView?.let {
      currentLifecycleOwner?.onDestroy()
      try {
        if (it.isAttachedToWindow) {
          windowManager.removeViewImmediate(it)
        }
      } catch (e: IllegalStateException) {
        // Logs an error in case removing results in an unexpected state.
        Logger.w("hideOverlay-removeViewImmediate failed", e)
      }
      currentOverlayView = null
      currentLifecycleOwner = null
      currentLayoutParams = null
    }
    currentTargetWindowView = null
  }

  private fun updatePosition(x: Int, y: Int) {
    val params = currentLayoutParams ?: return
    val view = currentOverlayView?.takeIf { it.isAttachedToWindow } ?: return

    if (params.x != x || params.y != y) {
      params.x = x
      params.y = y
      windowManager.updateViewLayout(view, params)
    }
  }

  private fun savePosition() {
    currentLayoutParams?.let { params ->
      overlayPreferences.saveOverlayPosition(params.x, params.y)
    }
  }

  inner class ActivityLifecycleHandler : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
      Logger.d("onCreate() called for ${activity.javaClass.simpleName}")
    }

    override fun onActivityStarted(activity: Activity) {
      Logger.d("onStart() called for ${activity.javaClass.simpleName}")
    }

    override fun onActivityResumed(activity: Activity) {
      Logger.d("onResume() called for ${activity.javaClass.simpleName}")
      if (!activity.isDebugOverlayActivity()) {
        lastAppActivityRef = WeakReference(activity)
        if (isCurrentTarget(activity)) {
          currentLifecycleOwner?.onResume()
        }
        DebugOverlay.overlayDataRepository.startOrResumeJankStatsTracking(activity)
      }
    }

    override fun onActivityPaused(activity: Activity) {
      Logger.d("onPause() called for ${activity.javaClass.simpleName}")
      if (!activity.isDebugOverlayActivity()) {
        if (isCurrentTarget(activity)) {
          currentLifecycleOwner?.onPause()
        }
        DebugOverlay.overlayDataRepository.pauseJankStatsTracking(activity)
        // Save position on pause to avoid excessive writes during active drag gestures
        savePosition()
      }
    }

    override fun onActivityStopped(activity: Activity) {
      Logger.d("onStop() called for ${activity.javaClass.simpleName}")
      if (!activity.isDebugOverlayActivity()) {
        if (isCurrentTarget(activity)) {
          currentLifecycleOwner?.onStop()
        }
      }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
      Logger.d("onSaveInstanceState() called for ${activity.javaClass.simpleName}")
    }

    override fun onActivityDestroyed(activity: Activity) {
      Logger.d("onDestroy() called for ${activity.javaClass.simpleName}")
      if (!activity.isDebugOverlayActivity()) {
        if (lastAppActivityRef?.get() === activity) {
          lastAppActivityRef = null
        }
        DebugOverlay.overlayDataRepository.stopJankStatsTracking(activity)
      }
    }
    private fun isCurrentTarget(activity: Activity): Boolean =
      currentTargetWindowView?.findActivityOrNull() === activity
  }
}

/** Returns true for activities that are part of DebugOverlay's UI (not the app's UI). */
private fun Activity.isDebugOverlayActivity(): Boolean = this is DebugPanelActivity || this is BugReportActivity
