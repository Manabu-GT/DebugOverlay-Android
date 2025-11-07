package com.ms.square.debugoverlay.internal

import android.util.Log
import com.ms.square.debugoverlay.DebugOverlay

internal object Logger {

  private const val TAG = "[DebugOverlay]"

  fun e(message: String, throwable: Throwable) {
    Log.e(TAG, message, throwable)
  }

  fun w(message: String, throwable: Throwable) {
    Log.w(TAG, message, throwable)
  }

  fun i(message: String) {
    if (DebugOverlay.debug) {
      Log.i(TAG, message)
    }
  }

  fun d(message: String) {
    if (DebugOverlay.debug) {
      Log.d(TAG, message)
    }
  }
}
