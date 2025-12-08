package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.util.copyToClipboard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import radiography.Radiography
import radiography.ScanScopes.AllWindowsScope
import radiography.ScannableView
import radiography.ViewStateRenderers.DefaultsNoPii

/**
 * UI tab showing the current view hierarchy using Radiography.
 *
 * Features:
 * - Displays full view hierarchy in monospace text
 * - Refresh button to rescan hierarchy
 * - Copy button to copy full hierarchy to clipboard
 * - Scrollable and selectable text
 *
 * @param modifier Modifier to be applied to the root layout.
 */
@Composable
internal fun UiTabContent(modifier: Modifier = Modifier) {
  var hierarchyOutput by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(true) }
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()

  fun refresh() {
    scope.launch {
      isLoading = true
      hierarchyOutput = runCatching {
        withContext(Dispatchers.Default) {
          Radiography.scan(
            viewStateRenderers = DefaultsNoPii,
            scanScope = excludeDebugPanelActivityScope
          )
        }
      }.getOrElse { e ->
        if (e is CancellationException) throw e
        "Failed to scan view hierarchy: ${e.message}"
      }
      isLoading = false
    }
  }

  LaunchedEffect(Unit) { refresh() }

  Column(modifier = modifier.fillMaxSize()) {
    // Toolbar with refresh and copy buttons
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.End
    ) {
      IconButton(onClick = { refresh() }) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = stringResource(R.string.debugoverlay_refresh)
        )
      }
      IconButton(
        onClick = {
          scope.copyToClipboard(clipboard, hierarchyOutput)
        }
      ) {
        Icon(
          imageVector = Icons.Default.ContentCopy,
          contentDescription = stringResource(R.string.debugoverlay_copy)
        )
      }
    }

    // Hierarchy output
    if (isLoading) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
    } else {
      HierarchyOutputDisplay(hierarchyOutput = hierarchyOutput)
    }
  }
}

@Composable
private fun HierarchyOutputDisplay(hierarchyOutput: String, modifier: Modifier = Modifier) {
  val verticalScrollState = rememberScrollState()
  val horizontalScrollState = rememberScrollState()

  Box(modifier = modifier.fillMaxSize()) {
    SelectionContainer {
      Text(
        text = hierarchyOutput,
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(verticalScrollState)
          .horizontalScroll(horizontalScrollState)
          .padding(16.dp),
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    VerticalScrollbar(
      scrollState = verticalScrollState,
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .fillMaxHeight()
        .padding(end = 2.dp, top = 2.dp, bottom = 2.dp)
    )

    HorizontalScrollbar(
      scrollState = horizontalScrollState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(start = 2.dp, end = 2.dp, bottom = 2.dp)
    )
  }
}

/**
 * A ScanScope that excludes windows marked with [R.id.debugoverlay_window_marker].
 */
private val excludeDebugPanelActivityScope = {
  AllWindowsScope.findRoots()
    .filter { scannableView ->
      val view = (scannableView as? ScannableView.AndroidView)?.view
      // only include if the view is not marked with the debug overlay window tag
      view?.getTag(R.id.debugoverlay_window_marker) != true
    }
    .toList()
}
