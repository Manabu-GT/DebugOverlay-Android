package com.ms.square.debugoverlay.internal.bugreport.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.bugreport.model.DraftInfo
import com.ms.square.debugoverlay.internal.bugreport.model.validatedTitle
import com.ms.square.debugoverlay.internal.util.formatRelativeTime

// Thumbnail dimensions (1:2 aspect ratio for portrait screenshots)
private val THUMBNAIL_WIDTH = 40.dp
private val THUMBNAIL_HEIGHT = 80.dp

/**
 * A list item displaying a saved bug report draft.
 *
 * Shows:
 * - Thumbnail of screenshot (or placeholder icon if unavailable)
 * - Title (falls back to "Bug Report" if blank)
 * - Relative timestamp ("2h ago")
 * - Delete button for accessibility
 *
 * The entire row is tappable to resume the draft.
 *
 * @param draft The draft information to display
 * @param thumbnail Optional screenshot thumbnail bitmap
 * @param onClick Called when the draft is tapped to resume
 * @param onDelete Called when the delete button is tapped
 * @param modifier Modifier for the list item
 */
@Composable
internal fun DraftListItem(
  draft: DraftInfo,
  thumbnail: Bitmap?,
  onClick: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val defaultTitle = stringResource(R.string.debugoverlay_bug_report_default_title)
  val title = remember(draft.metadata.userInput, defaultTitle) {
    draft.metadata.userInput.validatedTitle(defaultTitle)
  }
  val relativeTime = remember(draft.capturedAt) {
    formatRelativeTime(draft.capturedAt)
  }
  val draftDescription = stringResource(R.string.debugoverlay_draft_description, title, relativeTime)
  val deleteDescription = stringResource(R.string.debugoverlay_delete_draft)

  Row(
    modifier = modifier
      .defaultMinSize(minHeight = 56.dp) // M3 recommended list item height
      .semantics(mergeDescendants = true) {
        contentDescription = draftDescription
        role = Role.Button
      }
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Thumbnail
    DraftThumbnail(
      thumbnail = thumbnail,
      modifier = Modifier
        .size(width = THUMBNAIL_WIDTH, height = THUMBNAIL_HEIGHT)
        .clip(MaterialTheme.shapes.small)
    )

    Spacer(modifier = Modifier.width(16.dp))

    DraftTextContent(title = title, relativeTime = relativeTime, modifier = Modifier.weight(1f))

    // Delete button - clearAndSetSemantics isolates it from parent's merged semantics
    IconButton(
      onClick = onDelete,
      modifier = Modifier.clearAndSetSemantics {
        contentDescription = deleteDescription
        role = Role.Button
      }
    ) {
      Icon(
        imageVector = Icons.Outlined.Delete,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun DraftTextContent(title: String, relativeTime: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.clearAndSetSemantics { }) {
    Text(
      text = title,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = relativeTime,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun DraftThumbnail(thumbnail: Bitmap?, modifier: Modifier = Modifier) {
  val imageBitmap = remember(thumbnail) { thumbnail?.asImageBitmap() }

  if (imageBitmap != null) {
    Image(
      bitmap = imageBitmap,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = modifier
    )
  } else {
    // Placeholder when no screenshot available
    Box(
      modifier = modifier
        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.BugReport,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp)
      )
    }
  }
}
