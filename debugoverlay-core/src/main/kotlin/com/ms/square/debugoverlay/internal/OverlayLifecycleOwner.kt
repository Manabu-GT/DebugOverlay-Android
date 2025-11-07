package com.ms.square.debugoverlay.internal

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Synthetic lifecycle owner for ComposeView when used outside the Activity hierarchy.
 * This is necessary because ComposeView requires a LifecycleOwner to function properly.
 */
internal class OverlayLifecycleOwner :
  LifecycleOwner,
  SavedStateRegistryOwner {
  private val lifecycleRegistry = LifecycleRegistry(this)
  private val savedStateRegistryController = SavedStateRegistryController.create(this)

  override val lifecycle: Lifecycle
    get() = lifecycleRegistry

  override val savedStateRegistry: SavedStateRegistry
    get() = savedStateRegistryController.savedStateRegistry

  fun onCreate() {
    savedStateRegistryController.performRestore(null)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
  }

  fun onStart() {
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
  }

  fun onResume() {
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
  }

  fun onPause() {
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
  }

  fun onStop() {
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
  }

  fun onDestroy() {
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  }
}
