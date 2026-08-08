package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val MAX_BADGE_COUNT = 9

/**
 * Badge showing a count of items needing attention (saved drafts, crash records).
 *
 * Display logic:
 * - 1 item: dot only (no text)
 * - 2-9 items: count as text
 * - 10+ items: "9+"
 *
 * Uses M3 error color per convention for "items requiring attention". Purely decorative —
 * callers own the accessible description on the element being badged, so the count isn't
 * announced as a stray digit.
 *
 * @param count Number of items to display; must be positive
 * @param modifier Modifier for positioning (use offset to fine-tune position)
 */
@Composable
internal fun CountBadge(count: Int, modifier: Modifier = Modifier) {
  require(count > 0) { "count must be positive, got: $count" }
  Badge(
    // Default offset for top-right corner positioning
    modifier = modifier.offset(x = 1.dp, y = (-1).dp),
    // Use error color for "items requiring attention" per M3 badge convention
    containerColor = MaterialTheme.colorScheme.error,
    contentColor = MaterialTheme.colorScheme.onError
  ) {
    val badgeText = when {
      count == 1 -> null // Dot only
      count > MAX_BADGE_COUNT -> "${MAX_BADGE_COUNT}+"
      else -> count.toString()
    }
    badgeText?.let { Text(it) }
  }
}
