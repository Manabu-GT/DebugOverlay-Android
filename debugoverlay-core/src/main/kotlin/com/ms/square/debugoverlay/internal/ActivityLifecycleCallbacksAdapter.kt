package com.ms.square.debugoverlay.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle

internal abstract class ActivityLifecycleCallbacksAdapter : Application.ActivityLifecycleCallbacks {

  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    Logger.d("onCreate() called for ${activity.javaClass.simpleName}")
  }

  override fun onActivityStarted(activity: Activity) {
    Logger.d("onStart() called for ${activity.javaClass.simpleName}")
  }

  override fun onActivityResumed(activity: Activity) {
    Logger.d("onResume() called for ${activity.javaClass.simpleName}")
  }

  override fun onActivityPaused(activity: Activity) {
    Logger.d("onPause() called for ${activity.javaClass.simpleName}")
  }

  override fun onActivityStopped(activity: Activity) {
    Logger.d("onStop() called for ${activity.javaClass.simpleName}")
  }

  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    Logger.d("onSaveInstanceState() called for ${activity.javaClass.simpleName}")
  }

  override fun onActivityDestroyed(activity: Activity) {
    Logger.d("onDestroy() called for ${activity.javaClass.simpleName}")
  }
}
