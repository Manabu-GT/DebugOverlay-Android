package com.ms.square.debugoverlay.sample.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import com.ms.square.debugoverlay.sample.ui.navigation.FeedNavDisplay
import com.ms.square.debugoverlay.sample.ui.navigation.Route
import com.ms.square.debugoverlay.sample.ui.overlaytests.OverlayTestsScreen

/**
 * Main screen composable that hosts the bottom navigation with two tabs:
 * - Feed: The Android Weekly RSS feed
 * - Overlay Tests: Dedicated test scenarios for overlay z-order validation
 */
@Composable
internal fun MainScreen() {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val feedBackStack = rememberNavBackStack(Route.FeedList)

  Scaffold(
    contentWindowInsets = WindowInsets(0),
    bottomBar = {
      NavigationBar {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.RssFeed, contentDescription = null) },
          label = { Text("Feed") }
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = { Icon(Icons.Default.Layers, contentDescription = null) },
          label = { Text("Overlay Tests") }
        )
      }
    }
  ) { padding ->
    when (selectedTab) {
      0 -> FeedNavDisplay(feedBackStack, Modifier.padding(padding))
      1 -> OverlayTestsScreen(modifier = Modifier.padding(padding))
    }
  }
}
