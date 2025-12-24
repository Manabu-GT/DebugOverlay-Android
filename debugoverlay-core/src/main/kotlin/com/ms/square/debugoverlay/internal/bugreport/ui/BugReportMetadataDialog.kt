package com.ms.square.debugoverlay.internal.bugreport.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportMetadata

/**
 * Dialog for entering bug report metadata (title and optional description).
 *
 * Shows a screenshot preview at the top (or error placeholder if capture failed),
 * followed by optional title and description fields.
 * Users can leave fields empty to generate a report without metadata.
 *
 * State is hoisted to the caller to enable auto-save on dismiss.
 *
 * @param screenshot The captured screenshot to display as preview (null if capture failed)
 * @param title Current title value (hoisted state)
 * @param onTitleChange Called when title changes
 * @param description Current description value (hoisted state)
 * @param onDescriptionChange Called when description changes
 * @param isSubmitting When true, buttons are disabled to prevent double-tap
 * @param onConfirm Called with the metadata when user confirms
 * @param onDismiss Called when dialog is cancelled (caller should save current values)
 */
@Suppress("LongMethod", "LongParameterList")
@Composable
internal fun BugReportMetadataDialog(
  screenshot: Bitmap?,
  title: String,
  onTitleChange: (String) -> Unit,
  description: String,
  onDescriptionChange: (String) -> Unit,
  isSubmitting: Boolean = false,
  onConfirm: (BugReportMetadata) -> Unit,
  onDismiss: () -> Unit,
) {
  val imageBitmap = remember(screenshot) { screenshot?.asImageBitmap() }
  var showFullScreenPreview by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = { if (!isSubmitting) onDismiss() },
    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    title = { Text(text = stringResource(R.string.debugoverlay_bug_report_metadata_title)) },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        // Screenshot preview or error placeholder
        if (imageBitmap != null) {
          ScreenshotPreview(
            imageBitmap = imageBitmap,
            onClick = { showFullScreenPreview = true },
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 200.dp)
          )
        } else {
          ScreenshotErrorPlaceholder(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 200.dp)
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Form fields
        MetadataFormContent(title, onTitleChange, description, onDescriptionChange)
      }
    },
    confirmButton = {
      Button(
        onClick = {
          // Always create metadata with current values (title can be blank, validatedTitle handles default)
          val metadata = BugReportMetadata(title = title.trim(), description = description.trim())
          onConfirm(metadata)
        },
        enabled = !isSubmitting
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary
          )
          Spacer(Modifier.width(8.dp))
        }
        Text(
          stringResource(
            if (isSubmitting) {
              R.string.debugoverlay_bug_report_submitting
            } else {
              R.string.debugoverlay_bug_report_submit
            }
          )
        )
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isSubmitting
      ) {
        Text(stringResource(R.string.debugoverlay_cancel))
      }
    }
  )

  // Fullscreen preview dialog (only if screenshot available)
  if (showFullScreenPreview && imageBitmap != null) {
    FullScreenImageDialog(
      imageBitmap = imageBitmap,
      onDismiss = { showFullScreenPreview = false }
    )
  }
}

@Composable
private fun MetadataFormContent(
  title: String,
  onTitleChange: (String) -> Unit,
  description: String,
  onDescriptionChange: (String) -> Unit,
) {
  Column {
    Text(
      text = stringResource(R.string.debugoverlay_bug_report_metadata_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
      value = title,
      onValueChange = onTitleChange,
      label = { Text(stringResource(R.string.debugoverlay_bug_report_title_label)) },
      placeholder = { Text(stringResource(R.string.debugoverlay_bug_report_title_hint)) },
      supportingText = { Text(stringResource(R.string.debugoverlay_bug_report_title_supporting)) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
      value = description,
      onValueChange = onDescriptionChange,
      label = { Text(stringResource(R.string.debugoverlay_bug_report_description_label)) },
      placeholder = { Text(stringResource(R.string.debugoverlay_bug_report_description_hint)) },
      minLines = 3,
      maxLines = 5,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
private fun ScreenshotPreview(imageBitmap: ImageBitmap, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val viewFullScreenshotLabel = stringResource(R.string.debugoverlay_bug_report_view_full_screenshot)
  Surface(
    modifier = modifier
      .clip(MaterialTheme.shapes.medium)
      .clickable(
        onClickLabel = viewFullScreenshotLabel,
        onClick = onClick
      )
      .semantics { role = Role.Button },
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = MaterialTheme.shapes.medium
  ) {
    Box(contentAlignment = Alignment.Center) {
      Image(
        bitmap = imageBitmap,
        contentDescription = stringResource(R.string.debugoverlay_bug_report_screenshot_description),
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxWidth()
      )
      // Expand indicator
      Surface(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = MaterialTheme.shapes.small
      ) {
        Icon(
          imageVector = Icons.Outlined.ZoomIn,
          contentDescription = null, // Decorative, action on parent
          modifier = Modifier.padding(4.dp),
          tint = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
private fun ScreenshotErrorPlaceholder(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.clip(MaterialTheme.shapes.medium),
    color = MaterialTheme.colorScheme.errorContainer,
    shape = MaterialTheme.shapes.medium
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.height(120.dp)
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
          imageVector = Icons.Outlined.BrokenImage,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
          text = stringResource(R.string.debugoverlay_bug_report_screenshot_failed),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onErrorContainer
        )
      }
    }
  }
}

@Composable
private fun FullScreenImageDialog(imageBitmap: ImageBitmap, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceDim,
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.debugoverlay_close))
      }
    },
    text = {
      Image(
        bitmap = imageBitmap,
        contentDescription = stringResource(R.string.debugoverlay_bug_report_screenshot_description),
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxWidth()
      )
    }
  )
}
