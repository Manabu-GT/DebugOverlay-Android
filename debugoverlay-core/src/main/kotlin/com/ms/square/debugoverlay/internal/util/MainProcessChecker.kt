package com.ms.square.debugoverlay.internal.util

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build

/**
 * Returns true if the current process is the main process (matches the initial application pid)
 */
internal fun isMainProcess(application: Application): Boolean {
  val mainProcessName = application.packageName
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    return mainProcessName == Application.getProcessName()
  }
  val currentProcessName = getProcessName(application) ?: return true
  return mainProcessName == currentProcessName
}

/**
 * A fallback way to get the current process name on older android OSs, should get a
 * name like "com.package.name"(main process name) or "com.package.name:remote"
 */
private fun getProcessName(application: Application): String? {
  val myPid = android.os.Process.myPid()
  val am = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
  val infos = am.runningAppProcesses ?: return null

  return infos.firstOrNull { it.pid == myPid }?.processName
}
