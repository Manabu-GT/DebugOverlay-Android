package com.ms.square.debugoverlay.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle

internal abstract class ActivityLifecycleCallbacksAdapter : Application.ActivityLifecycleCallbacks {

  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    Logger.d("onCreate():${activity.javaClass.simpleName}")
  }

  override fun onActivityStarted(activity: Activity) {
    Logger.d("onStart():${activity.javaClass.simpleName}")
  }

  override fun onActivityResumed(activity: Activity) {
    Logger.d("onResume():${activity.javaClass.simpleName}")
  }

  override fun onActivityPaused(activity: Activity) {
    Logger.d("onPause():${activity.javaClass.simpleName}")
  }

  override fun onActivityStopped(activity: Activity) {
    Logger.d("onStop():${activity.javaClass.simpleName}")
  }

  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    Logger.d("onSaveInstanceState():${activity.javaClass.simpleName}")
  }

  override fun onActivityDestroyed(activity: Activity) {
    Logger.d("onDestroy():${activity.javaClass.simpleName}")
  }
}
