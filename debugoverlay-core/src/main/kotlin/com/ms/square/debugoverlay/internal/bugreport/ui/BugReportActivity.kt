package com.ms.square.debugoverlay.internal.bugreport.ui

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
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.IntentShareExporter
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportMetadata
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportResult
import com.ms.square.debugoverlay.internal.bugreport.model.validatedTitle
import com.ms.square.debugoverlay.internal.util.isDarkTheme
import kotlinx.coroutines.launch
import java.io.File

internal const val INTENT_EXTRA_CAPTURE_FOLDER = "capture_folder_path"
private const val BUNDLE_KEY_CAPTURE_FOLDER = "capture_folder"
private const val BUNDLE_KEY_TITLE = "title"
private const val BUNDLE_KEY_DESCRIPTION = "description"

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
 * 4. User submits → ZIP is created → shared via Intent → folder deleted
 * 5. User cancels → current input saved as draft → folder marked as draft
 *
 * Draft management:
 * - On cancel: saves user_input.json to mark folder as a draft
 * - On submit: deletes folder after successful ZIP creation
 * - Eviction runs after save to prevent exceeding max drafts
 */
internal class BugReportActivity : ComponentActivity() {

  private var captureFolder: File? = null
  private var isSubmitted = false

  // Hoisted state for title/description using mutableStateOf for Compose observability
  private var currentTitle by mutableStateOf("")
  private var currentDescription by mutableStateOf("")

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

    // Restore title/description from savedInstanceState (process death)
    currentTitle = savedInstanceState?.getString(BUNDLE_KEY_TITLE) ?: ""
    currentDescription = savedInstanceState?.getString(BUNDLE_KEY_DESCRIPTION) ?: ""

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
          title = currentTitle,
          onTitleChange = { currentTitle = it },
          description = currentDescription,
          onDescriptionChange = { currentDescription = it },
          isSubmitting = isSubmitting,
          onConfirm = { metadata ->
            handleConfirm(
              metadata = metadata,
              snackbarHostState = snackbarHostState,
              onSubmitStart = { isSubmitting = true },
              onSubmitEnd = { isSubmitting = false }
            )
          },
          onDismiss = { handleDismiss() }
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

  private fun handleDismiss() {
    if (isSubmitted) {
      // Already submitted successfully, just finish
      finish()
      return
    }

    // Save current input as draft before finishing
    val folder = captureFolder
    if (folder == null) {
      finish()
      return
    }

    lifecycleScope.launch {
      runCatching {
        val metadata = BugReportMetadata(
          title = currentTitle.trim(),
          description = currentDescription.trim()
        )
        DebugOverlay.bugReportGenerator.saveUserInputToDraft(folder, metadata)
        Logger.d("Saved draft on dismiss: ${folder.name}")
      }.onFailure {
        Logger.e("Failed to save draft on dismiss", it)
      }
      // finish() must be called inside coroutine after save completes
      finish()
    }
  }

  private fun handleConfirm(
    metadata: BugReportMetadata,
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

      // Use validatedTitle to ensure non-blank title for final submission
      val defaultTitle = getString(R.string.debugoverlay_bug_report_default_title)
      val validatedMetadata = BugReportMetadata(
        title = metadata.validatedTitle(defaultTitle),
        description = metadata.description
      )

      when (val result = DebugOverlay.bugReportGenerator.createReportFromFolder(folder, validatedMetadata)) {
        is BugReportResult.Success -> {
          val exported = IntentShareExporter(this@BugReportActivity).export(result.zipFile)
          if (exported) {
            isSubmitted = true // Prevent draft save on finish
            // Delete folder after successful share.
            // We can't know if it was actually shared successfully, but this is fine for now.
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
    outState.putString(BUNDLE_KEY_TITLE, currentTitle)
    outState.putString(BUNDLE_KEY_DESCRIPTION, currentDescription)
  }
}
