package com.ms.square.debugoverlay.sample

import android.app.Application
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.extension.okhttp.DebugOverlayNetworkInterceptor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SampleApplication : Application() {
  @Inject
  lateinit var debugOverlayNetworkInterceptor: DebugOverlayNetworkInterceptor

  override fun onCreate() {
    super.onCreate()
    DebugOverlay.config =
      DebugOverlay.config.copy(networkRequestTracker = debugOverlayNetworkInterceptor)
  }
}
