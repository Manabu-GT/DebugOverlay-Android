package com.ms.square.debugoverlay.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import com.ms.square.debugoverlay.DebugOverlay
import java.util.WeakHashMap

internal class ActivityOverlayViewManager(context: Context) : OverlayViewManager(context) {

  private var lifecycleOwner: OverlayLifecycleOwner? = null

  override fun showOverlay(windowToken: IBinder?) {
    hideOverlay()
    rootView = createRoot()
    // make layout of the window happens as that of a top-level window, not as a child of its container
    windowManager.addView(
      rootView,
      createLayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG, windowToken)
    )
  }

  override fun hideOverlay() {
    super.hideOverlay()
    lifecycleOwner = null
  }

  override fun createActivityLifecycleCallbacks(debugOverlay: DebugOverlay): Application.ActivityLifecycleCallbacks =
    ActivityLifecycleHandler()

  override fun setUpLifecycleOwnerOnComposeView(view: View, lifecycleOwner: OverlayLifecycleOwner) {
    this.lifecycleOwner = lifecycleOwner
  }

  inner class OverlayViewAttachStateChangeListener : View.OnAttachStateChangeListener {

    fun onActivityResumed() {
      Logger.d("OverlayViewAttachStateChangeListener-onActivityResumed")
      lifecycleOwner?.onStart()
      lifecycleOwner?.onResume()
    }

    fun onActivityStopped() {
      Logger.d("OverlayViewAttachStateChangeListener-onActivityStopped")
      lifecycleOwner?.onPause()
      lifecycleOwner?.onStop()
    }

    override fun onViewAttachedToWindow(v: View) {
      Logger.d("OverlayViewAttachStateChangeListener-onViewAttachedToWindow")
      showOverlay(v.windowToken)
    }

    override fun onViewDetachedFromWindow(v: View) {
      Logger.d("OverlayViewAttachStateChangeListener-onViewDetachedFromWindow")
      hideOverlay()
      v.removeOnAttachStateChangeListener(this)
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

    override fun onActivityResumed(activity: Activity) {
      super.onActivityResumed(activity)
      attachStateChangeListeners[activity]?.onActivityResumed()
    }

    override fun onActivityStopped(activity: Activity) {
      super.onActivityStopped(activity)
      attachStateChangeListeners[activity]?.onActivityStopped()
    }

    override fun onActivityDestroyed(activity: Activity) {
      super.onActivityDestroyed(activity)
      attachStateChangeListeners.remove(activity)
    }
  }
}
