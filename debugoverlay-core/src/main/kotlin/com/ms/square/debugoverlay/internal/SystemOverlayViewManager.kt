package com.ms.square.debugoverlay.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.net.toUri
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.R
import kotlinx.coroutines.CoroutineScope

internal class SystemOverlayViewManager(context: Context, overlayScope: CoroutineScope) :
  OverlayViewManager(context, overlayScope) {

  private var rootView: ViewGroup? = null
  private var overlayPermissionRequested = false

  override fun showOverlay() {
    hideOverlay()
    if (!Settings.canDrawOverlays(context)) {
      Toast.makeText(
        context,
        R.string.debugoverlay_overlay_permission_prompt,
        Toast.LENGTH_LONG
      ).show()
      requestDrawOnSystemLayerPermission(context)
      overlayPermissionRequested = true
      return
    }
    overlayPermissionRequested = false

    rootView = createRoot()
    windowManager.addView(rootView, createLayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY))
  }

  override fun hideOverlay() {
    rootView?.let {
      windowManager.removeView(it)
      rootView = null
    }
  }

  override fun setUpLifecycleOwnerOnComposeView(view: View, lifecycleOwner: OverlayLifecycleOwner) {
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

  override fun isOverlayShown(): Boolean = rootView != null

  override fun isOverlayPermissionRequested(): Boolean = overlayPermissionRequested
  override fun createActivityLifecycleCallbacks(debugOverlay: DebugOverlay): Application.ActivityLifecycleCallbacks =
    ActivityLifecycleHandler(debugOverlay)

  inner class ActivityLifecycleHandler(private val debugOverlay: DebugOverlay) : ActivityLifecycleCallbacksAdapter() {

    private var numRunningActivities = 0

    override fun onActivityStarted(activity: Activity) {
      super.onActivityStarted(activity)
      incrementNumRunningActivities()
    }

    override fun onActivityResumed(activity: Activity) {
      super.onActivityResumed(activity)
      if (isOverlayPermissionRequested() && Settings.canDrawOverlays(activity)) {
        showOverlay()
        debugOverlay.overlayService?.updateNotification()
      }
    }

    override fun onActivityStopped(activity: Activity) {
      super.onActivityStopped(activity)
      decrementNumRunningActivities()
    }

    private fun incrementNumRunningActivities() {
      if (numRunningActivities == 0) {
        // App is in foreground
        if (debugOverlay.overlayService == null && debugOverlay.unBindRequestReceived) {
          // Service already un-bound by an explicit request, but restart here since it is now in foreground
          debugOverlay.startAndBindDebugOverlayService()
          debugOverlay.unBindRequestReceived = false
        }
      }
      numRunningActivities++
    }

    private fun decrementNumRunningActivities() {
      numRunningActivities--
      if (numRunningActivities <= 0) {
        numRunningActivities = 0
      }
    }
  }

  companion object {

    fun requestDrawOnSystemLayerPermission(context: Context) {
      val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        "package:${context.packageName}".toUri()
      ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      context.startActivity(intent)
    }
  }
}
