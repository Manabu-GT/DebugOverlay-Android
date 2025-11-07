package com.ms.square.debugoverlay.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.core.net.toUri
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.R

internal class SystemOverlayViewManager(context: Context) : OverlayViewManager(context) {

  private var overlayPermissionRequested = false

  override fun showOverlay(windowToken: IBinder?) {
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
