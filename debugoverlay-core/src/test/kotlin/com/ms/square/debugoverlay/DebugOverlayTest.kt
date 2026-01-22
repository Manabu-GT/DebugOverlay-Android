package com.ms.square.debugoverlay

import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.OutputStream

@RunWith(RobolectricTestRunner::class)
class DebugOverlayTest {

  @After
  fun tearDown() {
    DebugOverlay.configure {
      overlayMode = OverlayMode.FullMetrics
      networkRequestSource = NoOpNetworkRequestSource
      customLogSource = null
    }
    DebugOverlay.bugReportContributors.clear()
  }

  @Test
  fun `Config has correct defaults`() {
    val config = DebugOverlay.Config()

    assertThat(config.overlayMode).isEqualTo(OverlayMode.FullMetrics)
    assertThat(config.networkRequestSource).isEqualTo(NoOpNetworkRequestSource)
    assertThat(config.customLogSource).isNull()
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
  fun `addBugReportContributor adds contributors to list`() {
    val contributor1 = TestBugReportContributor("test1.txt")
    val contributor2 = TestBugReportContributor("test2.txt")

    DebugOverlay.addBugReportContributor(contributor1)
    DebugOverlay.addBugReportContributor(contributor2)

    assertThat(DebugOverlay.bugReportContributors).containsExactly(contributor1, contributor2)
  }

  @Test
  fun `addBugReportContributor ignores duplicate filenames`() {
    val contributor = TestBugReportContributor("test.txt")
    val differentInstanceSameFilename = TestBugReportContributor("test.txt")

    DebugOverlay.addBugReportContributor(contributor)
    DebugOverlay.addBugReportContributor(differentInstanceSameFilename) // same filename ignored

    assertThat(DebugOverlay.bugReportContributors).containsExactly(contributor)
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
