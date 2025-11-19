package com.ms.square.debugoverlay.internal.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ms.square.debugoverlay.internal.data.source.LogcatDataSource
import com.ms.square.debugoverlay.internal.util.isDarkTheme

public class DebugPanelActivity : ComponentActivity() {

  private val logcatDataSource by lazy { LogcatDataSource(lifecycleScope) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val isDarkTheme = LocalConfiguration.current.isDarkTheme()
      val logcatEntries by logcatDataSource.logs
        .collectAsStateWithLifecycle(initialValue = emptyList())

      MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
      ) {
        DebugPanelBottomSheet(
          logcatEntries = logcatEntries,
          onDismiss = { finish() }
        )
      }
    }
  }

  override fun onDestroy() {
    logcatDataSource.close()
    super.onDestroy()
  }
}
