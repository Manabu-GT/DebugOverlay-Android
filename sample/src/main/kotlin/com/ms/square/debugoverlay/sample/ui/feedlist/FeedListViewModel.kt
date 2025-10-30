package com.ms.square.debugoverlay.sample.ui.feedlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ms.square.debugoverlay.sample.data.model.FeedItem
import com.ms.square.debugoverlay.sample.data.repository.FeedRepository
import com.ms.square.debugoverlay.sample.data.repository.FeedRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * ViewModel for the issue list screen.
 */
class FeedListViewModel(private val repository: FeedRepository = FeedRepositoryProvider.repository) : ViewModel() {

  private val _uiState = MutableStateFlow<FeedListUiState>(FeedListUiState.Loading)
  val uiState: StateFlow<FeedListUiState> = _uiState.asStateFlow()

  init {
    loadIssues()
  }

  /**
   * Loads the list of issues from the repository.
   */
  fun loadIssues(refresh: Boolean = false) {
    _uiState.value = FeedListUiState.Loading

    repository.getFeedItems(refresh)
      .catch { exception ->
        Timber.e(exception, "Error loading feed items")
        _uiState.value = FeedListUiState.Error(
          exception.message ?: "Unknown error occurred"
        )
      }
      .onEach { result ->
        result.fold(
          onSuccess = { feedItems ->
            _uiState.value = if (feedItems.isEmpty()) {
              FeedListUiState.Empty
            } else {
              FeedListUiState.Success(feedItems)
            }
          },
          onFailure = { exception ->
            Timber.e(exception, "Error loading feed items")
            _uiState.value = FeedListUiState.Error(
              exception.message ?: "Unknown error occurred"
            )
          }
        )
      }.launchIn(viewModelScope)
  }

  /**
   * Refreshes the list of issues.
   */
  fun refresh() {
    loadIssues(refresh = true)
  }
}

/**
 * UI state for the feed list screen.
 */
sealed interface FeedListUiState {
  data object Loading : FeedListUiState
  data class Success(val feedItems: List<FeedItem>) : FeedListUiState
  data class Error(val message: String) : FeedListUiState
  data object Empty : FeedListUiState
}
