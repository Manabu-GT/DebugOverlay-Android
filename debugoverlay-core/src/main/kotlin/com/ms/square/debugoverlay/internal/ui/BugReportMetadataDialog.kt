package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.bugreport.BugReportMetadata

/**
 * Dialog for entering bug report metadata (title and optional description).
 *
 * Users can leave fields empty to generate a report without metadata.
 *
 * @param onConfirm Called with the metadata when user confirms (null if both fields empty)
 * @param onDismiss Called when dialog is cancelled
 */
@Composable
internal fun BugReportMetadataDialog(onConfirm: (BugReportMetadata?) -> Unit, onDismiss: () -> Unit) {
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  val defaultTitle = stringResource(R.string.debugoverlay_bug_report_default_title)

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    title = { Text(text = stringResource(R.string.debugoverlay_bug_report_metadata_title)) },
    text = { MetadataFormContent(title, { title = it }, description, { description = it }) },
    confirmButton = {
      Button(
        onClick = {
          val metadata = if (title.isBlank() && description.isBlank()) {
            null
          } else {
            BugReportMetadata(title = title.trim().ifBlank { defaultTitle }, description = description.trim())
          }
          onConfirm(metadata)
        }
      ) {
        Text(stringResource(R.string.debugoverlay_bug_report_submit))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.debugoverlay_cancel))
      }
    }
  )
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
