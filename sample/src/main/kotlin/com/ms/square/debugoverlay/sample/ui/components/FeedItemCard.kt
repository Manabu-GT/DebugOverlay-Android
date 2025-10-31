package com.ms.square.debugoverlay.sample.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.sample.data.model.FeedItem

/**
 * Reusable card component for displaying a RSS feed item.
 *
 * @param item The [FeedItem] to display
 * @param onClick Callback invoked when the card is clicked
 * @param modifier Optional modifier for the card
 */
@Composable
fun FeedItemCard(item: FeedItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Text(
        text = item.title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
      )

      Text(
        text = item.publishDate,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }
}
