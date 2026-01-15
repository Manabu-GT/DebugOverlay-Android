package com.ms.square.debugoverlay.internal

import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // API 34 - compatible with Java 17
class OverlayLifecycleOwnerTest {

  private val owner = OverlayLifecycleOwner()

  @Test
  fun `initial state is INITIALIZED`() {
    assertThat(owner.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)
  }

  @Test
  fun `onCreate moves state to CREATED`() {
    owner.onCreate()

    assertThat(owner.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
  }

  @Test
  fun `savedStateRegistry is available after onCreate`() {
    owner.onCreate()

    // SavedStateRegistry should be accessible and not throw
    assertThat(owner.savedStateRegistry).isNotNull()
    assertThat(owner.savedStateRegistry.isRestored).isTrue()
  }

  @Test
  fun `onStart moves state to STARTED`() {
    owner.onCreate()

    owner.onStart()

    assertThat(owner.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
  }

  @Test
  fun `onResume moves state to RESUMED`() {
    owner.onCreate()
    owner.onStart()

    owner.onResume()

    assertThat(owner.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
  }

  @Test
  fun `onPause moves state back to STARTED`() {
    owner.onCreate()
    owner.onStart()
    owner.onResume()

    owner.onPause()

    assertThat(owner.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
  }

  @Test
  fun `onStop moves state back to CREATED`() {
    owner.onCreate()
    owner.onStart()
    owner.onResume()
    owner.onPause()

    owner.onStop()

    assertThat(owner.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
  }

  @Test
  fun `onDestroy moves state to DESTROYED`() {
    owner.onCreate()
    owner.onStart()
    owner.onResume()
    owner.onPause()
    owner.onStop()

    owner.onDestroy()

    assertThat(owner.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
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

  @Test
  fun `multiple observers receive same events`() {
    val observer1Events = mutableListOf<Lifecycle.Event>()
    val observer2Events = mutableListOf<Lifecycle.Event>()

    owner.lifecycle.addObserver(
      LifecycleEventObserver { _, event ->
        observer1Events.add(event)
      }
    )
    owner.lifecycle.addObserver(
      LifecycleEventObserver { _, event ->
        observer2Events.add(event)
      }
    )

    owner.onCreate()
    owner.onStart()

    assertThat(observer1Events).containsExactly(
      Lifecycle.Event.ON_CREATE,
      Lifecycle.Event.ON_START
    ).inOrder()
    assertThat(observer2Events).isEqualTo(observer1Events)
  }

  @Test
  fun `removed observer does not receive subsequent events`() {
    val receivedEvents = mutableListOf<Lifecycle.Event>()
    val observer = LifecycleEventObserver { _, event ->
      receivedEvents.add(event)
    }

    owner.lifecycle.addObserver(observer)
    owner.onCreate()
    owner.lifecycle.removeObserver(observer)
    owner.onStart()
    owner.onResume()

    // Observer should only have received ON_CREATE before removal
    assertThat(receivedEvents).containsExactly(Lifecycle.Event.ON_CREATE)
  }

  @Test
  fun `observer added after onCreate catches up to current state`() {
    owner.onCreate()

    val receivedEvents = mutableListOf<Lifecycle.Event>()
    owner.lifecycle.addObserver(
      LifecycleEventObserver { _, event ->
        receivedEvents.add(event)
      }
    )

    owner.onStart()

    // Observer catches up to current state, so receives ON_CREATE then ON_START
    assertThat(receivedEvents).containsExactly(
      Lifecycle.Event.ON_CREATE,
      Lifecycle.Event.ON_START
    ).inOrder()
  }
}
