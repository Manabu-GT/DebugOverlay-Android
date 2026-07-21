package com.ms.square.debugoverlay.internal.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.platform.LocalConfiguration
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.util.isDarkTheme

public class DebugPanelActivity : ComponentActivity() {

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Tag the DecorView so UI hierarchy scan can filter it out
    window.decorView.setTag(R.id.debugoverlay_window_marker, true)

    setContent {
      val isDarkTheme = LocalConfiguration.current.isDarkTheme()
      val windowSizeClass = calculateWindowSizeClass(this)
      val isCompactHeight = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact

      MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
      ) {
        DebugPanelDialog(
          onDismiss = { finish() },
          isCompactHeight = isCompactHeight
        )
      }
    }
  }
}
