package com.ms.square.debugoverlay.internal.data.source

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.junit.Test

class LogcatDataSourceTest {

  /**
   * [LogcatDataSource.logs] is backed by `stateIn(SharingStarted.WhileSubscribed())`, so
   * constructing the data source never starts the `logcat` subprocess on its own —
   * only subscribing to [LogcatDataSource.logs] does. This keeps
   * [LogcatDataSource.snapshotEntriesSync] safe to exercise without spawning a process.
   */
  private val dataSource = LogcatDataSource(CoroutineScope(Job()), initialMaxEntries = 10)

  @Test
  fun `snapshotEntriesSync returns empty list before Logcat has ever been subscribed`() {
    assertThat(dataSource.snapshotEntriesSync()).isEmpty()
  }
}
