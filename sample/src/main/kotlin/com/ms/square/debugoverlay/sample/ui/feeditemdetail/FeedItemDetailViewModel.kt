package com.ms.square.debugoverlay.sample.ui.feeditemdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ms.square.debugoverlay.sample.data.model.FeedItem
import com.ms.square.debugoverlay.sample.data.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the feed detail screen.
 */
@HiltViewModel
class FeedDetailViewModel @Inject constructor(private val repository: FeedRepository) : ViewModel() {

  private val _uiState = MutableStateFlow<FeedDetailUiState>(FeedDetailUiState.Loading)
  val uiState: StateFlow<FeedDetailUiState> = _uiState.asStateFlow()

  /**
   * Loads the details of a specific feed item.
   *
   * @param itemId The ID of the feed item to load
   */
  fun loadFeed(itemId: Int) {
    _uiState.value = FeedDetailUiState.Loading

    repository.getFeedItemById(itemId)
      .catch { exception ->
        Timber.e(exception, "Error loading feed detail")
        _uiState.value = FeedDetailUiState.Error(
          exception.message ?: "Unknown error occurred"
        )
      }
      .onEach { result ->
        result.fold(
          onSuccess = { feed ->
            _uiState.value = if (feed != null) {
              FeedDetailUiState.Success(feed)
            } else {
              FeedDetailUiState.Error("Feed not found")
            }
          },
          onFailure = { exception ->
            Timber.e(exception, "Error loading feed detail")
            _uiState.value = FeedDetailUiState.Error(
              exception.message ?: "Unknown error occurred"
            )
          }
        )
      }
      .launchIn(viewModelScope)
  }
}

/**
 * UI state for the feed detail screen.
 */
sealed interface FeedDetailUiState {
  data object Loading : FeedDetailUiState
  data class Success(val feedItem: FeedItem) : FeedDetailUiState
  data class Error(val message: String) : FeedDetailUiState
}
