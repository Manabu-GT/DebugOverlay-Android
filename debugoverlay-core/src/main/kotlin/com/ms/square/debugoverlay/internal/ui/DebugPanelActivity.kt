package com.ms.square.debugoverlay.internal.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.LocalConfiguration
import com.ms.square.debugoverlay.internal.util.isDarkTheme

public class DebugPanelActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val isDarkTheme = LocalConfiguration.current.isDarkTheme()

      MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
      ) {
        DebugPanelBottomSheet(
          onDismiss = { finish() }
        )
      }
    }
  }
}
