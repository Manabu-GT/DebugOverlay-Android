package com.ms.square.debugoverlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ms.square.debugoverlay.internal.ui.DebugOverlayPanel

internal class OverlayViewManager(
  private val context: Context,
  private val config: DebugOverlay.Config,
) {
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var rootView: ViewGroup? = null
    private var overlayPermissionRequested = false

    fun showDebugSystemOverlay() {
        if (config.isAllowSystemLayer && rootView == null) {
            if (!canDrawOnSystemLayer(context, getWindowTypeForOverlay(true))) {
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

            val params = createLayoutParams(config.isAllowSystemLayer, null)
            windowManager.addView(rootView, params)
        }
    }

    fun hideDebugSystemOverlay() {
        if (config.isAllowSystemLayer && rootView != null) {
            windowManager.removeView(rootView)
            rootView = null
        }
    }

    fun isSystemOverlayShown(): Boolean = rootView != null

    fun isOverlayPermissionRequested(): Boolean = overlayPermissionRequested

    fun createAttachStateChangeListener(): OverlayViewAttachStateChangeListener {
        return OverlayViewAttachStateChangeListener()
    }

    @SuppressLint("WrongConstant")
    private fun createLayoutParams(
      allowSystemLayer: Boolean,
      windowToken: IBinder?,
    ): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            if (windowToken != null) {
                token = windowToken
            }
            type = getWindowTypeForOverlay(allowSystemLayer)
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.BOTTOM or Gravity.START
        }
    }

    private fun createRoot(): ViewGroup {
        return ComposeView(context).apply {
            // Create and attach a synthetic lifecycle for the overlay
            val lifecycleOwner = OverlayLifecycleOwner()
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            // Start the lifecycle
            lifecycleOwner.onCreate()

            setContent {
                DebugOverlayPanel(
                  onClick = {
                    //TODO: Navigate to detailed performance screen
                  }
                )
            }

            // Move lifecycle to STARTED/RESUMED when the view is attached
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
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
    }

    inner class OverlayViewAttachStateChangeListener : View.OnAttachStateChangeListener {
        private var _rootView: ViewGroup? = null

        fun onActivityResumed() {
//            _rootView?.let { root ->
//              if (overlayModules.isNotEmpty()) {
//                // force-update recreated views with the latest data
//                for (overlayModule in overlayModules) {
//                  overlayModule.notifyObservers()
//                }
//              }
//            }
        }

        override fun onViewAttachedToWindow(v: View) {
            if (DebugOverlay.DEBUG) {
                Log.i(TAG, "onViewAttachedToWindow")
            }
            _rootView = createRoot()
            windowManager.addView(
                _rootView,
                createLayoutParams(config.isAllowSystemLayer, v.windowToken)
            )
        }

        override fun onViewDetachedFromWindow(v: View) {
            if (DebugOverlay.DEBUG) {
                Log.i(TAG, "onViewDetachedFromWindow")
            }
            _rootView?.let { windowManager.removeViewImmediate(it) }
            v.removeOnAttachStateChangeListener(this)
        }
    }

    companion object {
        private const val TAG = "OverlayViewManager"

        @JvmStatic
        fun requestDrawOnSystemLayerPermission(context: Context) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
              "package:${context.packageName}".toUri()
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }

        @JvmStatic
        fun canDrawOnSystemLayer(context: Context, systemWindowType: Int): Boolean {
            return if (isSystemLayer(systemWindowType)) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        @JvmStatic
        fun isSystemLayer(windowType: Int): Boolean {
            return windowType >= WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW
        }

        @JvmStatic
        fun getWindowTypeForOverlay(allowSystemLayer: Boolean): Int {
            return if (allowSystemLayer) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                // make layout of the window happens as that of a top-level window, not as a child of its container
                WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
            }
        }
    }
}
