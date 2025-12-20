package com.ms.square.debugoverlay.internal.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.bugreport.BugReportMetadata

/**
 * Dialog for entering bug report metadata (title and optional description).
 *
 * Shows a screenshot preview at the top (or error placeholder if capture failed),
 * followed by optional title and description fields.
 * Users can leave fields empty to generate a report without metadata.
 *
 * @param screenshot The captured screenshot to display as preview (null if capture failed)
 * @param isSubmitting When true, buttons are disabled to prevent double-tap
 * @param onConfirm Called with the metadata when user confirms (null if both fields empty)
 * @param onDismiss Called when dialog is cancelled
 */
@Suppress("LongMethod")
@Composable
internal fun BugReportMetadataDialog(
  screenshot: Bitmap?,
  isSubmitting: Boolean = false,
  onConfirm: (BugReportMetadata?) -> Unit,
  onDismiss: () -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var showFullScreenPreview by remember { mutableStateOf(false) }
  val defaultTitle = stringResource(R.string.debugoverlay_bug_report_default_title)

  AlertDialog(
    onDismissRequest = { if (!isSubmitting) onDismiss() },
    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    title = { Text(text = stringResource(R.string.debugoverlay_bug_report_metadata_title)) },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        // Screenshot preview or error placeholder
        if (screenshot != null) {
          ScreenshotPreview(
            bitmap = screenshot,
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
        MetadataFormContent(title, { title = it }, description, { description = it })
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val metadata = if (title.isBlank() && description.isBlank()) {
            null
          } else {
            BugReportMetadata(title = title.trim().ifBlank { defaultTitle }, description = description.trim())
          }
          onConfirm(metadata)
        },
        enabled = !isSubmitting
      ) {
        Text(stringResource(R.string.debugoverlay_bug_report_submit))
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
  if (showFullScreenPreview && screenshot != null) {
    FullScreenImageDialog(
      bitmap = screenshot,
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
private fun ScreenshotPreview(bitmap: Bitmap, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
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
private fun FullScreenImageDialog(bitmap: Bitmap, onDismiss: () -> Unit) {
  val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
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
