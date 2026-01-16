package com.ms.square.debugoverlay.internal

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OverlayLifecycleOwnerTest {

  private val owner = OverlayLifecycleOwner()

  @Test
  fun `initial state is INITIALIZED`() {
    assertThat(owner.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)
  }

  @Test
  fun `savedStateRegistry is available after onCreate`() {
    owner.onCreate()

    // SavedStateRegistry should be accessible and not throw
    assertThat(owner.savedStateRegistry).isNotNull()
    assertThat(owner.savedStateRegistry.isRestored).isTrue()
  }

  @Test
  fun `lifecycle observer receives events in order`() {
    val receivedEvents = mutableListOf<Lifecycle.Event>()

    owner.lifecycle.addObserver(
      LifecycleEventObserver { _, event ->
        receivedEvents.add(event)
      }
    )

    owner.onCreate()
    owner.onStart()
    owner.onResume()
    owner.onPause()
    owner.onStop()
    owner.onDestroy()

    assertThat(receivedEvents).containsExactly(
      Lifecycle.Event.ON_CREATE,
      Lifecycle.Event.ON_START,
      Lifecycle.Event.ON_RESUME,
      Lifecycle.Event.ON_PAUSE,
      Lifecycle.Event.ON_STOP,
      Lifecycle.Event.ON_DESTROY
    ).inOrder()
  }
}
