package com.ms.square.debugoverlay.internal.bugreport.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val MAX_BADGE_COUNT = 9

/**
 * Badge showing the number of saved drafts.
 *
 * Display logic:
 * - 1 draft: dot only (no text)
 * - 2-9 drafts: count as text
 * - 10+ drafts: "9+"
 *
 * Uses M3 error color per convention for "items requiring attention".
 *
 * @param draftCount Number of drafts to display
 * @param modifier Modifier for positioning (use offset to fine-tune position)
 */
@Composable
internal fun DraftCountBadge(draftCount: Int, modifier: Modifier = Modifier) {
  Badge(
    // Default offset for top-right corner positioning
    modifier = modifier.offset(x = 1.dp, y = (-1).dp),
    // Use error color for "items requiring attention" per M3 badge convention
    containerColor = MaterialTheme.colorScheme.error,
    contentColor = MaterialTheme.colorScheme.onError
  ) {
    val badgeText = when {
      draftCount == 1 -> null // Dot only
      draftCount > MAX_BADGE_COUNT -> "${MAX_BADGE_COUNT}+"
      else -> draftCount.toString()
    }
    badgeText?.let { Text(it) }
  }
}
