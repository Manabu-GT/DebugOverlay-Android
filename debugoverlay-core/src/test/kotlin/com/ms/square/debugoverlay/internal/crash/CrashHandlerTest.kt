package com.ms.square.debugoverlay.internal.crash

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CrashHandlerTest {

  private val previousHandler = FakeUncaughtExceptionHandler()
  private var capturedWith: Pair<Thread, Throwable>? = null
  private var shouldThrowOnCapture = false

  private val handler = CrashHandler(
    previousHandler = previousHandler,
    captureCrash = { thread, throwable ->
      if (shouldThrowOnCapture) error("simulated capture failure")
      capturedWith = thread to throwable
    }
  )

  @Test
  fun `uncaughtException captures the crash and delegates to the previous handler`() {
    val thread = Thread.currentThread()
    val throwable = IllegalStateException("boom")

    handler.uncaughtException(thread, throwable)

    assertThat(capturedWith).isEqualTo(thread to throwable)
    assertThat(previousHandler.invokedWith).isEqualTo(thread to throwable)
  }

  @Test
  fun `uncaughtException delegates to previous handler even when capture throws`() {
    shouldThrowOnCapture = true

    handler.uncaughtException(Thread.currentThread(), RuntimeException("boom"))

    assertThat(previousHandler.invokedWith).isNotNull()
  }
}

private class FakeUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {
  var invokedWith: Pair<Thread, Throwable>? = null

  override fun uncaughtException(t: Thread, e: Throwable) {
    invokedWith = t to e
  }
}
