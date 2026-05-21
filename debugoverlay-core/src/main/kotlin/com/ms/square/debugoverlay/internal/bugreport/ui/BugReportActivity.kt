package com.ms.square.debugoverlay.internal.bugreport.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ms.square.debugoverlay.DebugOverlay
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.BugReportGenerator
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportResult
import com.ms.square.debugoverlay.internal.bugreport.model.UserInput
import com.ms.square.debugoverlay.internal.util.findActivityOrNull
import com.ms.square.debugoverlay.internal.util.isDarkTheme
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
import com.ms.square.debugoverlay.model.ExportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val INTENT_EXTRA_CAPTURE_FOLDER = "capture_folder_path"
private const val INTENT_EXTRA_SHOW_DRAFT_PICKER = "show_draft_picker"
private const val BUNDLE_KEY_CAPTURE_FOLDER = "capture_folder"

/**
 * Activity for bug report metadata dialog and draft picker.
 *
 * This activity is needed because the FAB runs in a WindowManager overlay
 * which doesn't have an Activity context required for Compose dialogs.
 *
 * Two modes:
 * 1. **Folder mode** (INTENT_EXTRA_CAPTURE_FOLDER): Shows metadata dialog for a specific folder
 * 2. **Draft picker mode** (INTENT_EXTRA_SHOW_DRAFT_PICKER): Shows bottom sheet to select draft or create new
 *
 * Flow for folder mode:
 * 1. FAB captures data to folder via captureToFolder
 * 2. FAB starts this activity with folder path
 * 3. This activity loads screenshot preview and shows the metadata dialog
 * 4. User submits → ZIP is created → shared via Intent → folder deleted
 * 5. User cancels → current input saved as draft → folder marked as draft
 *
 * Flow for draft picker mode:
 * 1. FAB taps with drafts → launches this activity with show_draft_picker=true
 * 2. Activity shows bottom sheet with drafts + "Create New" option
 * 3. User selects draft → switches to folder mode with that draft
 * 4. User selects "Create New" → captures new → switches to folder mode
 *
 * Draft management:
 * - On cancel: saves metadata.json to mark folder as a draft
 * - On submit: deletes folder after successful ZIP creation
 * - Eviction runs after save to prevent exceeding max drafts
 *
 * State Management Note:
 * Uses [rememberSaveable] for Compose state (title, description, mode) to eliminate manual
 * Bundle boilerplate while keeping [captureFolder] and [isSubmitted] as Activity fields
 * since they're accessed in Activity methods.
 */
internal class BugReportActivity : ComponentActivity() {

  // Activity fields for state accessed in non-Composable methods (handleDismiss, handleConfirm)
  private var captureFolder: File? = null
  private var isSubmitted = false

  // Initial values determined in onCreate, used by rememberSaveable
  private var initialShowDraftPicker = false
  private var initialTitle = ""
  private var initialDescription = ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Tag the DecorView so UI hierarchy scan can filter it out
    window.decorView.setTag(R.id.debugoverlay_window_marker, true)

    // Restore captureFolder from savedInstanceState (not using rememberSaveable since it's a File)
    savedInstanceState?.getString(BUNDLE_KEY_CAPTURE_FOLDER)?.let {
      captureFolder = File(it)
    }

    // Determine initial mode and folder from intent (first launch only)
    if (savedInstanceState == null) {
      when {
        intent.getBooleanExtra(INTENT_EXTRA_SHOW_DRAFT_PICKER, false) -> {
          initialShowDraftPicker = true
        }
        intent.hasExtra(INTENT_EXTRA_CAPTURE_FOLDER) -> {
          val folderPath = intent.getStringExtra(INTENT_EXTRA_CAPTURE_FOLDER)
          if (folderPath == null) {
            finish()
            return
          }
          captureFolder = File(folderPath)
          initialShowDraftPicker = false
        }
        else -> {
          finish()
          return
        }
      }
    }

    setContent { BugReportActivityContent() }
  }

  @Composable
  private fun BugReportActivityContent() {
    val isDarkTheme = LocalConfiguration.current.isDarkTheme()

    // Use rememberSaveable for automatic state persistence across config changes
    var showDraftPicker by rememberSaveable { mutableStateOf(initialShowDraftPicker) }
    var currentTitle by rememberSaveable { mutableStateOf(initialTitle) }
    var currentDescription by rememberSaveable { mutableStateOf(initialDescription) }

    MaterialTheme(
      colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
      if (showDraftPicker) {
        DraftPickerScreen(
          onDraftSelected = { draft ->
            captureFolder = draft.folder
            // Restore saved title/description from draft
            currentTitle = draft.metadata.userInput?.title.orEmpty()
            currentDescription = draft.metadata.userInput?.description.orEmpty()
            showDraftPicker = false
          },
          onNewCaptureCreated = { folder ->
            captureFolder = folder
            // New capture - no saved title/description
            currentTitle = ""
            currentDescription = ""
            showDraftPicker = false
          },
          onDismiss = { finish() },
          onError = { /* Error handled by DraftPickerScreen snackbar */ }
        )
      } else {
        BugReportScreen(
          captureFolder = captureFolder,
          currentTitle = currentTitle,
          onTitleChange = { currentTitle = it },
          currentDescription = currentDescription,
          onDescriptionChange = { currentDescription = it },
          onDismiss = { handleDismiss(currentTitle, currentDescription) }
        )
      }
    }
  }

  @Composable
  private fun BugReportScreen(
    captureFolder: File?,
    currentTitle: String,
    onTitleChange: (String) -> Unit,
    currentDescription: String,
    onDescriptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
  ) {
    val snackbarHostState = remember { SnackbarHostState() }
    var screenshot by remember { mutableStateOf<Bitmap?>(null) }
    var isSubmitting by remember { mutableStateOf(false) } // Transient UI state

    // Load screenshot preview from folder
    LaunchedEffect(captureFolder) {
      captureFolder?.let { folder ->
        screenshot = DebugOverlay.bugReportGenerator.loadScreenshotPreview(folder)
      }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      BugReportMetadataDialog(
        screenshot = screenshot,
        title = currentTitle,
        onTitleChange = onTitleChange,
        description = currentDescription,
        onDescriptionChange = onDescriptionChange,
        isSubmitting = isSubmitting,
        onConfirm = { userInput ->
          handleConfirm(
            userInput = userInput,
            snackbarHostState = snackbarHostState,
            onSubmitStart = { isSubmitting = true },
            onSubmitEnd = { isSubmitting = false }
          )
        },
        onDismiss = onDismiss
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

  private fun handleDismiss(
    currentTitle: String,
    currentDescription: String,
    bugReportGenerator: BugReportGenerator = DebugOverlay.bugReportGenerator,
  ) {
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
      runCatchingNonCancellation {
        val userInput = UserInput(
          title = currentTitle.trim(),
          description = currentDescription.trim()
        )
        bugReportGenerator.saveUserInputToDraft(folder, userInput)
        Logger.d("Saved draft on dismiss: ${folder.name}")
      }.onFailure {
        Logger.e("Failed to save draft on dismiss", it)
      }
      // finish() must be called inside coroutine after save completes
      finish()
    }
  }

  private fun handleConfirm(
    userInput: UserInput,
    snackbarHostState: SnackbarHostState,
    onSubmitStart: () -> Unit,
    onSubmitEnd: () -> Unit,
    bugReportGenerator: BugReportGenerator = DebugOverlay.bugReportGenerator,
  ) {
    lifecycleScope.launch {
      onSubmitStart()
      val folder = captureFolder
      if (folder == null) {
        snackbarHostState.showSnackbar(getString(R.string.debugoverlay_bug_report_error))
        onSubmitEnd()
        return@launch
      }

      val result = bugReportGenerator.createReportFromFolder(
        captureFolder = folder,
        defaultTitle = getString(R.string.debugoverlay_bug_report_default_title),
        userInput = userInput
      )

      when (result) {
        is BugReportResult.Success -> {
          // Set early to prevent handleDismiss from saving a draft during export
          isSubmitted = true
          // Persist user input so the draft retains title/description regardless of export outcome
          bugReportGenerator.saveUserInputToDraft(folder, userInput)
          val exportResult = withContext(Dispatchers.IO) {
            runCatchingNonCancellation {
              DebugOverlay.config.bugReportExporter.export(this@BugReportActivity, result.report)
            }.getOrElse { e ->
              Logger.e("Bug report export failed", e)
              null
            }
          }
          when (exportResult) {
            is ExportResult.Initiated -> {
              // Outcome unknown (share sheet) — retain draft as SUBMITTED for re-sharing
              bugReportGenerator.markAsSubmitted(folder)
              finish()
            }
            is ExportResult.Success -> {
              // Confirmed delivery — clean up the folder
              bugReportGenerator.deleteCaptureFolder(folder)
              finish()
            }
            else -> {
              isSubmitted = false // Reset so dismiss can still save draft
              snackbarHostState.showSnackbar(getString(R.string.debugoverlay_share_bug_report_error))
              onSubmitEnd()
            }
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
    // Only save captureFolder manually; other state uses rememberSaveable
    captureFolder?.absolutePath?.let {
      outState.putString(BUNDLE_KEY_CAPTURE_FOLDER, it)
    }
  }

  companion object {
    fun launchWithDraftPicker(context: Context) {
      val intent = Intent(context, BugReportActivity::class.java).apply {
        if (context.findActivityOrNull() == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(INTENT_EXTRA_SHOW_DRAFT_PICKER, true)
      }
      context.startActivity(intent)
    }

    fun launchWithMetadataDialog(context: Context, bugCapturedFolderPath: String) {
      val intent = Intent(context, BugReportActivity::class.java).apply {
        if (context.findActivityOrNull() == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(INTENT_EXTRA_CAPTURE_FOLDER, bugCapturedFolderPath)
      }
      context.startActivity(intent)
    }
  }
}
