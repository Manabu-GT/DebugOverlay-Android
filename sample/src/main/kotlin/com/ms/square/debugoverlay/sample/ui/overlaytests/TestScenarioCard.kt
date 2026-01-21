package com.ms.square.debugoverlay.sample.ui.overlaytests

import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A reusable card component for displaying test scenario items.
 *
 * @param title The title of the test scenario
 * @param description A brief description of what the test does
 * @param icon The icon to display for the scenario
 * @param onClick Callback when the card is tapped
 * @param modifier Optional modifier
 */
@Composable
internal fun TestScenarioCard(
  title: String,
  description: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(onClick = onClick, modifier = modifier) {
    ListItem(
      headlineContent = { Text(title) },
      supportingContent = { Text(description) },
      leadingContent = {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = MaterialTheme.colorScheme.primary
        )
      }
    )
  }
}
