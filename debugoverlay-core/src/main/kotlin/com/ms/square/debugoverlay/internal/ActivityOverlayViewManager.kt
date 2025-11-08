package com.ms.square.debugoverlay.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.ms.square.debugoverlay.DebugOverlay
import kotlinx.coroutines.CoroutineScope
import java.util.WeakHashMap

internal class ActivityOverlayViewManager(context: Context, overlayScope: CoroutineScope) : OverlayViewManager(context, overlayScope) {

  override fun createActivityLifecycleCallbacks(debugOverlay: DebugOverlay): Application.ActivityLifecycleCallbacks =
    ActivityLifecycleHandler()

  inner class OverlayViewAttachStateChangeListener() : View.OnAttachStateChangeListener {

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
      rootView = createRoot()
      lifecycleOwner = rootView?.findViewTreeLifecycleOwner() as? OverlayLifecycleOwner
      // make layout of the window happens as that of a top-level window, not as a child of its container
      windowManager.addView(
        rootView,
        createLayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG, windowToken)
      )
    }

    private fun hideOverlay() {
      rootView?.let {
        lifecycleOwner?.onDestroy()
        windowManager.removeView(it)
        rootView = null
        lifecycleOwner = null
      }
    }
  }

  inner class ActivityLifecycleHandler : ActivityLifecycleCallbacksAdapter() {
    private val attachStateChangeListeners: MutableMap<Activity, OverlayViewAttachStateChangeListener> = WeakHashMap()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
      super.onActivityCreated(activity, savedInstanceState)
      OverlayViewAttachStateChangeListener().also {
        activity.window.decorView.addOnAttachStateChangeListener(it)
        attachStateChangeListeners.put(activity, it)
      }
    }

    override fun onActivityStarted(activity: Activity) {
      super.onActivityStarted(activity)
      attachStateChangeListeners[activity]?.onActivityStarted()
    }

    override fun onActivityResumed(activity: Activity) {
      super.onActivityResumed(activity)
      attachStateChangeListeners[activity]?.onActivityResumed()
    }

    override fun onActivityPaused(activity: Activity) {
      super.onActivityPaused(activity)
      attachStateChangeListeners[activity]?.onActivityPaused()
    }

    override fun onActivityStopped(activity: Activity) {
      super.onActivityStopped(activity)
      attachStateChangeListeners[activity]?.onActivityStopped()
    }

    override fun onActivityDestroyed(activity: Activity) {
      super.onActivityDestroyed(activity)
      attachStateChangeListeners.remove(activity)?.also {
        activity.window.decorView.removeOnAttachStateChangeListener(it)
      }
    }
  }
}
