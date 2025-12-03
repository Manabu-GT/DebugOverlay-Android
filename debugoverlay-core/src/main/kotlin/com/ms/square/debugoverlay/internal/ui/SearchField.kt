package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.core.R

/**
 * A simple search field using OutlinedTextField.
 * Note: Material3's SearchBar component could be considered in the future for richer search UI.
 */
@Composable
internal fun SearchField(
  searchPlaceholder: String,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    value = searchQuery,
    onValueChange = onSearchQueryChanged,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    placeholder = {
      Text(
        text = searchPlaceholder,
        style = MaterialTheme.typography.bodyMedium
      )
    },
    leadingIcon = {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    },
    trailingIcon = {
      if (searchQuery.isNotEmpty()) {
        IconButton(onClick = { onSearchQueryChanged("") }) {
          Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = stringResource(R.string.debugoverlay_clear_search),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    singleLine = true,
    shape = RoundedCornerShape(12.dp),
    colors = TextFieldDefaults.colors(
      focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
      unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent
    )
  )
}
