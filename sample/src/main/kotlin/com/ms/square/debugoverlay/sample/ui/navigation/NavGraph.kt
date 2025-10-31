package com.ms.square.debugoverlay.sample.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ms.square.debugoverlay.sample.ui.feeditemdetail.FeedDetailScreen
import com.ms.square.debugoverlay.sample.ui.feedlist.FeedListScreen
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the sample app using Kotlin Serialization.
 */
sealed interface Route : NavKey {
  /**
   * Route for the feed list screen.
   */
  @Serializable
  data object FeedList : Route

  /**
   * Route for the feed item detail screen.
   *
   * @property feedItemId The ID of the feed item to display
   */
  @Serializable
  data class FeedItemDetail(val feedItemId: Int) : Route
}

/**
 * Main navigation graph for the sample app using Navigation 3 NavDisplay.
 *
 * @param backStack The Navigation 3 back stack
 * @param modifier Optional modifier for NavDisplay
 */
@Composable
fun NavGraph(backStack: NavBackStack<NavKey>, modifier: Modifier = Modifier) {
  NavDisplay(
    entryDecorators = listOf(
      // Add the default decorators for managing scenes and saving state
      rememberSaveableStateHolderNavEntryDecorator(),
      // Then add the view model store decorator
      rememberViewModelStoreNavEntryDecorator()
    ),
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    modifier = modifier,
    entryProvider = entryProvider {
      // FeedItem List Entry
      entry<Route.FeedList> {
        FeedListScreen(
          onFeedClick = { itemId ->
            backStack.add(Route.FeedItemDetail(itemId))
          }
        )
      }

      // FeedItem Detail Entry
      entry<Route.FeedItemDetail> { key ->
        FeedDetailScreen(
          itemId = key.feedItemId,
          onNavigateBack = {
            backStack.removeLastOrNull()
          }
        )
      }
    }
  )
}
