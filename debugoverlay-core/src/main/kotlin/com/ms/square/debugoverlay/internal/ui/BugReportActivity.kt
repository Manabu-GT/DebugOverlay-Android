package com.ms.square.debugoverlay.internal.ui

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.bugreport.BugReportMetadata
import com.ms.square.debugoverlay.internal.bugreport.BugReportResult
import com.ms.square.debugoverlay.internal.bugreport.IntentShareExporter
import com.ms.square.debugoverlay.internal.util.isDarkTheme
import kotlinx.coroutines.launch
import java.io.File

internal const val INTENT_EXTRA_CAPTURE_FOLDER = "capture_folder_path"
private const val BUNDLE_KEY_CAPTURE_FOLDER = "capture_folder"

/**
 * Activity for displaying the bug report metadata dialog.
 *
 * This activity is needed because the FAB runs in a WindowManager overlay
 * which doesn't have an Activity context required for Compose dialogs.
 *
 * Flow:
 * 1. FAB captures data to folder via [captureToFolder]
 * 2. FAB starts this activity with folder path
 * 3. This activity loads screenshot preview and shows the metadata dialog
 * 4. User submits → ZIP is created → shared via Intent
 * 5. Activity finishes and cleans up the capture folder (on success only)
 *
 * TODO: Folder cleanup will be handled in draft management feature.
 *       For now, cancelled captures remain in cache until system clears it.
 */
internal class BugReportActivity : ComponentActivity() {

  private var captureFolder: File? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Tag the DecorView so UI hierarchy scan can filter it out
    window.decorView.setTag(R.id.debugoverlay_window_marker, true)

    // Restore folder path from savedInstanceState (process death) or Intent
    val folderPath = savedInstanceState?.getString(BUNDLE_KEY_CAPTURE_FOLDER)
      ?: intent.getStringExtra(INTENT_EXTRA_CAPTURE_FOLDER)
    if (folderPath == null) {
      finish()
      return
    }
    captureFolder = File(folderPath)

    setContent { BugReportScreen(captureFolder = captureFolder) }
  }

  @Composable
  private fun BugReportScreen(captureFolder: File?) {
    val isDarkTheme = LocalConfiguration.current.isDarkTheme()
    val snackbarHostState = remember { SnackbarHostState() }
    var screenshot by remember { mutableStateOf<Bitmap?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Load screenshot preview from folder
    LaunchedEffect(captureFolder) {
      captureFolder?.let { folder ->
        screenshot = DebugOverlay.bugReportGenerator.loadScreenshotPreview(folder)
      }
    }

    MaterialTheme(
      colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        BugReportMetadataDialog(
          screenshot = screenshot,
          isSubmitting = isSubmitting,
          onConfirm = { metadata ->
            handleConfirm(
              metadata = metadata,
              snackbarHostState = snackbarHostState,
              onSubmitStart = { isSubmitting = true },
              onSubmitEnd = { isSubmitting = false }
            )
          },
          onDismiss = {
            // TODO: Folder cleanup on dismiss will be handled in draft management feature
            finish()
          }
        )

        // Snackbar host at bottom for error messages
        SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
        ) { snackbarData ->
          Snackbar(
            snackbarData = snackbarData,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
          )
        }
      }
    }
  }

  private fun handleConfirm(
    metadata: BugReportMetadata?,
    snackbarHostState: SnackbarHostState,
    onSubmitStart: () -> Unit,
    onSubmitEnd: () -> Unit,
  ) {
    lifecycleScope.launch {
      onSubmitStart()
      val folder = captureFolder
      if (folder == null) {
        snackbarHostState.showSnackbar(getString(R.string.debugoverlay_bug_report_error))
        onSubmitEnd()
        return@launch
      }

      when (val result = DebugOverlay.bugReportGenerator.createReportFromFolder(folder, metadata)) {
        is BugReportResult.Success -> {
          val exported = IntentShareExporter(this@BugReportActivity).export(result.zipFile)
          if (exported) {
            DebugOverlay.bugReportGenerator.deleteCaptureFolder(folder)
            finish()
          } else {
            snackbarHostState.showSnackbar(getString(R.string.debugoverlay_share_bug_report_error))
            onSubmitEnd()
          }
        }
        is BugReportResult.Error -> {
          snackbarHostState.showSnackbar(getString(R.string.debugoverlay_bug_report_error))
          onSubmitEnd()
        }
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    captureFolder?.absolutePath?.let {
      outState.putString(BUNDLE_KEY_CAPTURE_FOLDER, it)
    }
  }
}
