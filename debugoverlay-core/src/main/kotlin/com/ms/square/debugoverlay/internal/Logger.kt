package com.ms.square.debugoverlay.internal

import android.util.Log
import com.ms.square.debugoverlay.core.BuildConfig

internal object Logger {

  private const val TAG = "[DebugOverlay]"

  fun e(message: String, throwable: Throwable) {
    Log.e(TAG, message, throwable)
  }

  fun w(message: String, throwable: Throwable? = null) {
    Log.w(TAG, message, throwable)
  }

  fun i(message: String) {
    Log.i(TAG, message)
  }

  fun d(message: String) {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, message)
    }
  }
}
