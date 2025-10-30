package com.ms.square.debugoverlay.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.navigation3.runtime.rememberNavBackStack
import com.ms.square.debugoverlay.sample.ui.navigation.NavGraph
import com.ms.square.debugoverlay.sample.ui.navigation.Route
import com.ms.square.debugoverlay.sample.ui.theme.AppTheme
import timber.log.Timber

/**
 * Main activity for the DebugOverlay sample app, which shows the RSS feed of Android Weekly.
 */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    Timber.d("onCreate() called")

    setContent {
      AppTheme {
        Surface {
          val backStack = rememberNavBackStack(Route.FeedList)
          NavGraph(backStack = backStack)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    Timber.d("onResume() called")
  }
}
