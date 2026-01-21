package com.ms.square.debugoverlay.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.ms.square.debugoverlay.sample.ui.MainScreen
import com.ms.square.debugoverlay.sample.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Main activity for the DebugOverlay sample app.
 * Hosts the bottom navigation with Feed (Android Weekly RSS) and Overlay Tests tabs.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    Timber.tag("MainActivity").d("onCreate() called")

    setContent {
      AppTheme {
        MainScreen()
      }
    }
  }

  override fun onResume() {
    super.onResume()
    Timber.tag("MainActivity").d("onResume() called")
  }
}
