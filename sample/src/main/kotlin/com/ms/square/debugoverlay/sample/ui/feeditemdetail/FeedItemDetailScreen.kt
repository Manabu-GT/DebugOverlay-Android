package com.ms.square.debugoverlay.sample.ui.feeditemdetail

import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ms.square.debugoverlay.sample.data.model.FeedItem

/**
 * Screen displaying the details of a single feed item.
 *
 * @param itemId The ID of the feed item to display
 * @param onNavigateBack Callback invoked when the back button is clicked
 * @param modifier Optional modifier for the screen
 * @param viewModel The ViewModel for this screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedDetailScreen(
  itemId: Int,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: FeedDetailViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(itemId) {
    viewModel.loadFeed(itemId)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Issue Details") },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      )
    },
    modifier = modifier
  ) { paddingValues ->
    when (val state = uiState) {
      is FeedDetailUiState.Loading -> {
        LoadingState(modifier = Modifier.padding(paddingValues))
      }

      is FeedDetailUiState.Success -> {
        FeedDetailContent(
          feedItem = state.feedItem,
          modifier = Modifier.padding(paddingValues)
        )
      }

      is FeedDetailUiState.Error -> {
        ErrorState(
          message = state.message,
          modifier = Modifier.padding(paddingValues)
        )
      }
    }
  }
}

@Composable
private fun FeedDetailContent(feedItem: FeedItem, modifier: Modifier = Modifier) {
  val uriHandler = LocalUriHandler.current
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Clickable title
    Text(
      text = feedItem.title,
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.clickable { uriHandler.openUri(feedItem.url) }
    )

    Text(
      text = "Published: ${feedItem.publishDate}",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Render HTML content with clickable links
    if (feedItem.description.isNotEmpty()) {
      // Use AndroidView with TextView
      // since AnnotatedString passed to the Text composable cannot render images...
      AndroidView(
        factory = { context ->
          TextView(context).apply {
            movementMethod = LinkMovementMethod.getInstance()
          }
        },
        update = { textView ->
          textView.text = feedItem.description.parseAsHtml(
            flags = HtmlCompat.FROM_HTML_MODE_COMPACT,
            imageGetter = CoilImageGetter(
              textView.context,
              object : Drawable.Callback {
                override fun invalidateDrawable(who: Drawable) {
                  val spannable = textView.text as? Spannable
                  spannable?.getSpans<ImageSpan>()?.find { it.drawable == who }?.let {
                    // this will trigger SpanWatcher in TextView
                    spannable.setSpan(
                      it,
                      spannable.getSpanStart(it),
                      spannable.getSpanEnd(it),
                      spannable.getSpanFlags(it)
                    )
                  }
                }
                override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) = Unit
                override fun unscheduleDrawable(who: Drawable, what: Runnable) = Unit
              }
            )
          )
        }
      )
    }

    Button(
      onClick = { uriHandler.openUri(feedItem.url) },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("View Full Issue")
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
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
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
        text = "Error loading issue",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.error
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
      )
    }
  }
}
