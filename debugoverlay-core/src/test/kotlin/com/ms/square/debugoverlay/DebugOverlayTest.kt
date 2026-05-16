package com.ms.square.debugoverlay

import android.content.Context
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.internal.bugreport.IntentShareExporter
import com.ms.square.debugoverlay.internal.ui.DebugPanelActivity
import com.ms.square.debugoverlay.model.BugReport
import com.ms.square.debugoverlay.model.ExportResult
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.io.OutputStream

@RunWith(RobolectricTestRunner::class)
class DebugOverlayTest {

  @After
  fun tearDown() {
    DebugOverlay.configure {
      overlayMode = OverlayMode.FullMetrics()
      networkRequestSource = NoOpNetworkRequestSource
      customLogSource = null
      bugReportExporter = IntentShareExporter
      maxLogcatEntries = DebugOverlay.Config.DEFAULT_MAX_LOGCAT_ENTRIES
    }
    DebugOverlay.bugReportContributors.clear()
  }

  @Test
  fun `Config has correct defaults`() {
    val config = DebugOverlay.Config()

    assertThat(config.overlayMode).isEqualTo(OverlayMode.FullMetrics())
    assertThat(config.networkRequestSource).isEqualTo(NoOpNetworkRequestSource)
    assertThat(config.customLogSource).isNull()
    assertThat(config.bugReportExporter).isSameInstanceAs(IntentShareExporter)
    assertThat(config.maxLogcatEntries).isEqualTo(DebugOverlay.Config.DEFAULT_MAX_LOGCAT_ENTRIES)
  }

  @Test
  fun `configure updates maxLogcatEntries`() {
    DebugOverlay.configure {
      maxLogcatEntries = 1000
    }

    assertThat(DebugOverlay.config.maxLogcatEntries).isEqualTo(1000)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `configure throws when maxLogcatEntries is zero`() {
    DebugOverlay.configure {
      maxLogcatEntries = 0
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `configure throws when maxLogcatEntries is negative`() {
    DebugOverlay.configure {
      maxLogcatEntries = -1
    }
  }

  @Test
  fun `configure updates config properties`() {
    val networkSource = TestNetworkRequestSource()
    val logSource = TestLogSource()

    DebugOverlay.configure {
      overlayMode = OverlayMode.BugReporterOnly
      networkRequestSource = networkSource
      customLogSource = logSource
    }

    assertThat(DebugOverlay.config.overlayMode).isEqualTo(OverlayMode.BugReporterOnly)
    assertThat(DebugOverlay.config.networkRequestSource).isSameInstanceAs(networkSource)
    assertThat(DebugOverlay.config.customLogSource).isSameInstanceAs(logSource)
  }

  @Test
  fun `configure updates bugReportExporter`() {
    val customExporter = object : BugReportExporter {
      override suspend fun export(context: Context, report: BugReport): ExportResult = ExportResult.Success
    }

    DebugOverlay.configure {
      bugReportExporter = customExporter
    }

    assertThat(DebugOverlay.config.bugReportExporter).isSameInstanceAs(customExporter)
  }

  @Test
  fun `configure preserves unchanged properties from previous call`() {
    val networkSource = TestNetworkRequestSource()
    DebugOverlay.configure {
      networkRequestSource = networkSource
    }

    DebugOverlay.configure {
      overlayMode = OverlayMode.BugReporterOnly
    }

    assertThat(DebugOverlay.config.overlayMode).isEqualTo(OverlayMode.BugReporterOnly)
    assertThat(DebugOverlay.config.networkRequestSource).isSameInstanceAs(networkSource)
  }

  @Test
  fun `configure sets custom tabs on FullMetrics preserving order`() {
    val tab1 = DebugTab(title = "Tab 1") {}
    val tab2 = DebugTab(title = "Tab 2") {}

    DebugOverlay.configure {
      overlayMode = OverlayMode.FullMetrics(customTabs = listOf(tab1, tab2))
    }

    val mode = DebugOverlay.config.overlayMode as OverlayMode.FullMetrics
    assertThat(mode.customTabs).containsExactly(tab1, tab2).inOrder()
  }

  @Test
  fun `configure sets custom tabs on Hidden preserving order`() {
    val tab1 = DebugTab(title = "Tab 1") {}
    val tab2 = DebugTab(title = "Tab 2") {}

    DebugOverlay.configure {
      overlayMode = OverlayMode.Hidden(customTabs = listOf(tab1, tab2))
    }

    val mode = DebugOverlay.config.overlayMode as OverlayMode.Hidden
    assertThat(mode.customTabs).containsExactly(tab1, tab2).inOrder()
  }

  @Test
  fun `openPanel launches DebugPanelActivity with NEW_TASK flag`() {
    val context = RuntimeEnvironment.getApplication()

    DebugOverlay.openPanel(context)

    val started = shadowOf(context).nextStartedActivity
    assertThat(started.component?.className).isEqualTo(DebugPanelActivity::class.java.name)
    assertThat(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
      .isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK)
  }

  @Test
  fun `addBugReportContributor adds contributors to list`() {
    val contributor1 = TestBugReportContributor("test1.txt")
    val contributor2 = TestBugReportContributor("test2.txt")

    DebugOverlay.addBugReportContributor(contributor1)
    DebugOverlay.addBugReportContributor(contributor2)

    assertThat(DebugOverlay.bugReportContributors).containsExactly(contributor1, contributor2)
  }

  @Test
  fun `addBugReportContributor ignores duplicate filenames case-insensitively`() {
    val contributor = TestBugReportContributor("test.txt")
    val upperCaseDuplicate = TestBugReportContributor("TEST.txt")
    val mixedCaseDuplicate = TestBugReportContributor("Test.TXT")

    DebugOverlay.addBugReportContributor(contributor)
    DebugOverlay.addBugReportContributor(upperCaseDuplicate) // case-insensitive duplicate ignored
    DebugOverlay.addBugReportContributor(mixedCaseDuplicate) // case-insensitive duplicate ignored

    assertThat(DebugOverlay.bugReportContributors).containsExactly(contributor)
  }

  @Test
  fun `addBugReportContributor rejects invalid filenames`() {
    val pathSeparator = TestBugReportContributor("foo/bar.txt")
    val blank = TestBugReportContributor("")
    val startsWithDot = TestBugReportContributor(".hidden")

    DebugOverlay.addBugReportContributor(pathSeparator)
    DebugOverlay.addBugReportContributor(blank)
    DebugOverlay.addBugReportContributor(startsWithDot)

    assertThat(DebugOverlay.bugReportContributors).isEmpty()
  }

  private class TestNetworkRequestSource : NetworkRequestSource {
    override val requests: Flow<List<NetworkRequest>> = emptyFlow()
  }

  private class TestLogSource : LogSource {
    override val sourceName: String = "Test"
    override val logs: Flow<List<LogEntry>> = emptyFlow()
  }

  private class TestBugReportContributor(override val filename: String) : BugReportDataContributor {
    override fun writeTo(outputStream: OutputStream) {
      // No-op for testing
    }
  }
}
