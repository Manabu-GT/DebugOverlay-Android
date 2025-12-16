package com.ms.square.debugoverlay.internal.data.source

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.model.AppExitInfo
import com.ms.square.debugoverlay.internal.data.model.AppExitReason
import com.ms.square.debugoverlay.internal.data.model.ProcessImportance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

private const val MAX_EXIT_RESULTS = 15

/**
 * Data source for retrieving app exit history using [ApplicationExitInfo] API.
 *
 * Requires Android 11 (API 30) or above. Returns empty list on older devices.
 */
internal class AppExitDataSource(private val context: Context, scope: CoroutineScope) {

  /**
   * Returns true if the device supports the ApplicationExitInfo API.
   */
  val isSupported: Boolean
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

  /**
   * Flow of app exit history, emitted once on subscription.
   * Runs on IO dispatcher to avoid blocking main thread during trace reading.
   */
  val appExitInfos: Flow<List<AppExitInfo>> = flow {
    emit(queryAppExitInfos())
  }.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Lazily, emptyList())

  /**
   * Returns a snapshot of app exit history for bug reports.
   * Always queries directly to ensure fresh data.
   */
  suspend fun queryAppExitInfosSnapshot(): List<AppExitInfo> = withContext(Dispatchers.IO) {
    queryAppExitInfos()
  }

  private fun queryAppExitInfos(): List<AppExitInfo> {
    if (!isSupported) {
      return emptyList()
    }

    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return activityManager
      .getHistoricalProcessExitReasons(
        context.packageName,
        0, // PID (0 for all PIDs)
        MAX_EXIT_RESULTS
      )
      .map { it.toAppExitInfo() }
  }

  @Suppress("NewApi") // Checked via isSupported
  private fun ApplicationExitInfo.toAppExitInfo() = AppExitInfo(
    id = timestamp xor processName.hashCode().toLong(),
    reason = AppExitReason.fromValue(reason),
    timestampMs = timestamp,
    description = description,
    processName = processName,
    pssKb = pss,
    rssKb = rss,
    importance = ProcessImportance.fromValue(importance),
    trace = readTrace()
  )

  @Suppress("NewApi") // Checked via isSupported
  private fun ApplicationExitInfo.readTrace(): String? {
    val stream = traceInputStream
    // On API 31+, native crash tombstones are returned as protobuf binary format
    // which cannot be displayed as readable text.
    if (reason == ApplicationExitInfo.REASON_CRASH_NATIVE &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
      return if (stream != null) {
        runCatching {
          stream.close() // Close the stream we won't use
        }
        NATIVE_CRASH_TOMBSTONE_MESSAGE
      } else {
        null
      }
    }
    return runCatching {
      stream?.bufferedReader()?.use { it.readText() }
    }.getOrElse { e ->
      Logger.w("ApplicationExitInfo - readTrace() failed", e)
      null
    }
  }

  companion object {
    private const val NATIVE_CRASH_TOMBSTONE_MESSAGE =
      "Native crash tombstone (binary protobuf format). Check logcat for stack trace details."
  }
}
