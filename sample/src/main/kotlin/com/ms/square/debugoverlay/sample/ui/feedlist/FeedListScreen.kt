package com.ms.square.debugoverlay.sample.ui.feedlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ms.square.debugoverlay.sample.data.model.FeedItem
import com.ms.square.debugoverlay.sample.ui.components.FeedItemCard

/**
 * Main screen displaying the list of RSS feed items.
 *
 * @param onFeedClick Callback invoked when a feed item is clicked
 * @param modifier Optional modifier for the screen
 * @param viewModel The ViewModel for this screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedListScreen(
  onFeedClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: FeedListViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Android Weekly") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      )
    },
    modifier = modifier
  ) { paddingValues ->
    when (val state = uiState) {
      is FeedListUiState.Loading -> {
        LoadingState(modifier = Modifier.padding(paddingValues))
      }

      is FeedListUiState.Success -> {
        PullToRefreshBox(
          isRefreshing = false,
          onRefresh = { viewModel.refresh() },
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
          FeedList(
            feedItems = state.feedItems,
            onFeedClick = onFeedClick
          )
        }
      }

      is FeedListUiState.Error -> {
        ErrorState(
          message = state.message,
          onRetry = { viewModel.refresh() },
          modifier = Modifier.padding(paddingValues)
        )
      }

      is FeedListUiState.Empty -> {
        EmptyState(modifier = Modifier.padding(paddingValues))
      }
    }
  }
}

@Composable
private fun FeedList(feedItems: List<FeedItem>, onFeedClick: (Int) -> Unit, modifier: Modifier = Modifier) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(
      items = feedItems,
      key = { feedItem -> feedItem.id }
    ) { feedItem ->
      FeedItemCard(
        item = feedItem,
        onClick = { onFeedClick(feedItem.id) }
      )
    }
  }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator()
  }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.padding(16.dp)
    ) {
      Text(
        text = "Error loading issues",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.error
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
      )
      Button(onClick = onRetry) {
        Text("Retry")
      }
    }
  }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = "No issues available",
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.padding(16.dp)
    )
  }
}
