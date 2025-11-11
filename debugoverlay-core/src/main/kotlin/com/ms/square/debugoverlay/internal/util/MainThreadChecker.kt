package com.ms.square.debugoverlay.internal.util

import android.os.Looper

private val isMainThread: Boolean get() = Looper.getMainLooper().thread === Thread.currentThread()

internal fun checkMainThread() {
  check(isMainThread) {
    "Should be called from the main thread, not ${Thread.currentThread()}"
  }
}
