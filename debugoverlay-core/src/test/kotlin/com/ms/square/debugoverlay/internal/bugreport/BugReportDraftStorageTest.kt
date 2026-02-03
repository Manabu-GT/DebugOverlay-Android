package com.ms.square.debugoverlay.internal.bugreport

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.internal.bugreport.model.AppInfo
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportSnapshot
import com.ms.square.debugoverlay.internal.bugreport.model.BugReportState
import com.ms.square.debugoverlay.internal.bugreport.model.UserInput
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BugReportDraftStorageTest {

  private val storage: DefaultBugReportDraftStorage =
    DefaultBugReportDraftStorage(
      context = RuntimeEnvironment.getApplication(),
      appInfoProvider = DefaultAppInfoProvider
    )

  private val testAppInfo = AppInfo(
    packageName = "com.test.app",
    versionName = "1.0.0",
    versionCode = 1,
    targetSdkVersion = 34,
    minSdkVersion = 21,
    isDebuggable = true,
    installerStore = "Unknown",
    installerPackage = null,
    firstInstallTime = 0L,
    lastUpdateTime = 0L
  )

  @Test
  fun `markAsSubmitted changes state to SUBMITTED`() = runTest {
    val folder = createDraft(capturedAt = 1000L)

    storage.markAsSubmitted(folder)

    storage.drafts.test {
      val drafts = awaitItem()
      assertThat(drafts).hasSize(1)
      assertThat(drafts.first().isSubmitted).isTrue()
      cancel()
    }
  }

  @Test
  fun `markAsSubmitted preserves existing metadata fields`() = runTest {
    val userInput = UserInput(title = "Test Bug", description = "Description")
    val folder = createDraft(capturedAt = 5000L, userInput = userInput)

    storage.markAsSubmitted(folder)

    storage.drafts.test {
      val drafts = awaitItem()
      assertThat(drafts).hasSize(1)
      val draft = drafts.first()
      assertThat(draft.isSubmitted).isTrue()
      assertThat(draft.capturedAt).isEqualTo(5000L)
      assertThat(draft.metadata.userInput).isEqualTo(userInput)
      cancel()
    }
  }

  @Test
  fun `drafts includes both DRAFT and SUBMITTED`() = runTest {
    createDraft(capturedAt = 2000L)
    createSubmittedDraft(capturedAt = 1000L)

    storage.drafts.test {
      val drafts = awaitItem()
      assertThat(drafts).hasSize(2)
      cancel()
    }
  }

  @Test
  fun `drafts excludes IN_PROGRESS`() = runTest {
    // saveSnapshot alone leaves the folder in IN_PROGRESS state
    storage.saveSnapshot(createMinimalSnapshot(capturedAt = 1000L))
    createDraft(capturedAt = 2000L)

    storage.drafts.test {
      val drafts = awaitItem()
      assertThat(drafts).hasSize(1)
      assertThat(drafts.first().metadata.state).isEqualTo(BugReportState.DRAFT)
      cancel()
    }
  }

  private fun createMinimalSnapshot(capturedAt: Long = System.currentTimeMillis()) = BugReportSnapshot(
    capturedAt = capturedAt,
    appInfo = testAppInfo,
    screenshot = null,
    deviceInfo = null,
    logcatLogs = emptyList(),
    customLogSourceData = null,
    networkRequests = emptyList(),
    jankStats = null,
    appExitInfos = emptyList(),
    uiHierarchy = null
  )

  private suspend fun createDraft(capturedAt: Long, userInput: UserInput = UserInput()): File {
    val folder = storage.saveSnapshot(createMinimalSnapshot(capturedAt))
    storage.saveUserInput(folder, userInput)
    return folder
  }

  private suspend fun createSubmittedDraft(capturedAt: Long, userInput: UserInput = UserInput()): File {
    val folder = createDraft(capturedAt, userInput)
    storage.markAsSubmitted(folder)
    return folder
  }
}
