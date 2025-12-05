package com.ms.square.debugoverlay.internal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val FULL_SCREEN_OFFSET = 1f
private const val HALF_SCREEN_OFFSET = 0.5f

/**
 * Generic detail navigation container with slide transitions.
 *
 * Handles:
 * - State-based conditional rendering (list vs detail)
 * - Slide-in/out transitions
 * - System back button handling
 *
 * @param selectedItem The currently selected item (null = show list, non-null = show detail)
 * @param onBack Callback to clear selection and return to list
 * @param listContent Composable to display the list screen
 * @param detailContent Composable to display the detail screen for the selected item
 */
@Composable
internal fun <T> DetailNavigation(
  selectedItem: T?,
  onBack: () -> Unit,
  listContent: @Composable () -> Unit,
  detailContent: @Composable (T) -> Unit,
  modifier: Modifier = Modifier,
) {
  // Handle system back button when detail screen is shown
  BackHandler(enabled = selectedItem != null) {
    onBack()
  }

  // State-based navigation with slide transitions
  AnimatedContent(
    targetState = selectedItem,
    transitionSpec = {
      if (targetState != null) {
        // Navigating to detail - slide in from right
        slideInHorizontally(initialOffsetX = { (it * FULL_SCREEN_OFFSET).toInt() }) togetherWith
          slideOutHorizontally(targetOffsetX = { (-it * HALF_SCREEN_OFFSET).toInt() })
      } else {
        // Back to list - slide in from left
        slideInHorizontally(initialOffsetX = { (-it * HALF_SCREEN_OFFSET).toInt() }) togetherWith
          slideOutHorizontally(targetOffsetX = { (it * FULL_SCREEN_OFFSET).toInt() })
      }
    },
    label = "detail_navigation",
    modifier = modifier
  ) { item ->
    if (item != null) {
      detailContent(item)
    } else {
      listContent()
    }
  }
}
