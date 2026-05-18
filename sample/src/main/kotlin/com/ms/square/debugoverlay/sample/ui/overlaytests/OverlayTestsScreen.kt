package com.ms.square.debugoverlay.sample.ui.overlaytests

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.OverlayMode
import com.ms.square.debugoverlay.sample.SecondActivity

/**
 * Main screen for overlay test scenarios.
 * Displays categorized test scenarios to validate overlay z-order behavior.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OverlayTestsScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val activity = context as? Activity
  val fragmentActivity = context as? FragmentActivity

  // State for Compose dialogs
  var showComposeDialog by rememberSaveable { mutableStateOf(false) }
  var showComposeBottomSheet by rememberSaveable { mutableStateOf(false) }
  var isFullscreen by rememberSaveable { mutableStateOf(false) }
  var isOverlayHidden by rememberSaveable { mutableStateOf(false) }
  // Matches DebugOverlaySetup.kt default; both this toggle and the Hide/Show Overlay toggle
  // honor it so flipping visibility never silently changes the thermal preference.
  var isThermalOn by rememberSaveable { mutableStateOf(true) }

  // Reset fullscreen mode when leaving the screen
  DisposableEffect(Unit) {
    onDispose {
      if (isFullscreen) {
        activity?.let { setFullscreenMode(it, false) }
      }
    }
  }

  // Compose dialogs
  if (showComposeDialog) {
    ComposeAlertDialog(onDismiss = { showComposeDialog = false })
  }

  if (showComposeBottomSheet) {
    ComposeModalBottomSheet(onDismiss = { showComposeBottomSheet = false })
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Overlay Tests") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      )
    },
    modifier = modifier
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Section: View System Dialogs
      item {
        SectionHeader("View System Dialogs")
      }

      item {
        TestScenarioCard(
          title = "AlertDialog",
          description = "Standard Android AlertDialog.Builder",
          icon = Icons.Default.ChatBubble,
          onClick = { showAlertDialog(context) },
          modifier = Modifier.fillMaxWidth()
        )
      }

      item {
        TestScenarioCard(
          title = "BottomSheetDialog",
          description = "View-based BottomSheetDialog",
          icon = Icons.Default.VerticalAlignBottom,
          onClick = { showBottomSheetDialog(context) },
          modifier = Modifier.fillMaxWidth()
        )
      }

      item {
        TestScenarioCard(
          title = "DialogFragment",
          description = "DialogFragment with Compose content",
          icon = Icons.Default.ViewAgenda,
          onClick = {
            fragmentActivity?.supportFragmentManager?.let { fm ->
              showDialogFragment(fm)
            }
          },
          modifier = Modifier.fillMaxWidth()
        )
      }

      // Section: Compose Dialogs
      item {
        SectionHeader("Compose Dialogs")
      }

      item {
        TestScenarioCard(
          title = "Compose Dialog",
          description = "Material 3 AlertDialog composable",
          icon = Icons.Default.ChatBubble,
          onClick = { showComposeDialog = true },
          modifier = Modifier.fillMaxWidth()
        )
      }

      item {
        TestScenarioCard(
          title = "ModalBottomSheet",
          description = "Material 3 ModalBottomSheet composable",
          icon = Icons.Default.Layers,
          onClick = { showComposeBottomSheet = true },
          modifier = Modifier.fillMaxWidth()
        )
      }

      // Section: Activity Scenarios
      item {
        SectionHeader("Activity Scenarios")
      }

      item {
        TestScenarioCard(
          title = "Multiple Activities",
          description = "Launch a second activity",
          icon = Icons.AutoMirrored.Filled.OpenInNew,
          onClick = { SecondActivity.launch(context) },
          modifier = Modifier.fillMaxWidth()
        )
      }

      item {
        TestScenarioCard(
          title = if (isFullscreen) "Exit Fullscreen" else "Fullscreen Mode",
          description = if (isFullscreen) "Show system bars" else "Hide system bars (immersive mode)",
          icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
          onClick = {
            activity?.let {
              isFullscreen = !isFullscreen
              setFullscreenMode(it, isFullscreen)
            }
          },
          modifier = Modifier.fillMaxWidth()
        )
      }

      // Section: Programmatic API
      item {
        SectionHeader("Programmatic API")
      }

      item {
        // Sample-only toggle: configures with empty customTabs, so the SharedPrefs tab
        // disappears for the rest of the session. Production code should re-pass its tabs.
        TestScenarioCard(
          title = if (isOverlayHidden) "Show Overlay" else "Hide Overlay",
          description = if (isOverlayHidden) {
            "Switch back to FullMetrics — overlay reappears"
          } else {
            "Switch to OverlayMode.Hidden — overlay disappears, panel still works via openPanel"
          },
          icon = if (isOverlayHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
          onClick = {
            val nextHidden = !isOverlayHidden
            DebugOverlay.configure {
              overlayMode = if (nextHidden) OverlayMode.Hidden() else OverlayMode.FullMetrics(showThermal = isThermalOn)
            }
            isOverlayHidden = nextHidden
          },
          modifier = Modifier.fillMaxWidth()
        )
      }

      item {
        TestScenarioCard(
          title = if (isThermalOn) "Hide Thermal Row" else "Show Thermal Row",
          description = "Toggle the thermal-status row in the compact overlay (Android 11+ only)",
          icon = Icons.Default.Thermostat,
          onClick = {
            val nextThermalOn = !isThermalOn
            isThermalOn = nextThermalOn
            // Only reconfigure when the overlay is currently shown — if hidden, the preference
            // is recorded locally and applied next time the user shows the overlay.
            if (!isOverlayHidden) {
              DebugOverlay.configure {
                overlayMode = OverlayMode.FullMetrics(showThermal = nextThermalOn)
              }
            }
          },
          modifier = Modifier.fillMaxWidth()
        )
      }

      item {
        TestScenarioCard(
          title = "Open Debug Panel",
          description = "Launch the panel via DebugOverlay.openPanel() — works in any OverlayMode, including Hidden",
          icon = Icons.AutoMirrored.Filled.OpenInNew,
          onClick = { DebugOverlay.openPanel(context) },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier
      .padding(top = 8.dp, bottom = 4.dp)
      .semantics { heading() }
  )
}

/**
 * Sets immersive fullscreen mode for the given activity.
 * Tests overlay visibility when system bars are hidden.
 *
 * @param activity The activity to set fullscreen mode on
 * @param enable Whether to enable or disable fullscreen mode
 */
private fun setFullscreenMode(activity: Activity, enable: Boolean) {
  val window = activity.window
  val insetsController = WindowCompat.getInsetsController(window, window.decorView)

  if (enable) {
    // Hide system bars for immersive mode
    insetsController.apply {
      hide(WindowInsetsCompat.Type.systemBars())
      systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
  } else {
    // Show system bars
    insetsController.show(WindowInsetsCompat.Type.systemBars())
  }
}
