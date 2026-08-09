package com.ms.square.debugoverlay.internal.data

import android.app.Activity
import android.content.Context
import com.ms.square.debugoverlay.Clearable
import com.ms.square.debugoverlay.LogSource
import com.ms.square.debugoverlay.NetworkRequestSource
import com.ms.square.debugoverlay.NoOpNetworkRequestSource
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.bugreport.DefaultAppInfoProvider
import com.ms.square.debugoverlay.internal.bugreport.model.CustomLogSourceData
import com.ms.square.debugoverlay.internal.crash.CrashRecordInfo
import com.ms.square.debugoverlay.internal.crash.CrashRecordStorage
import com.ms.square.debugoverlay.internal.crash.DefaultCrashRecordStorage
import com.ms.square.debugoverlay.internal.crash.buildCrashRecord
import com.ms.square.debugoverlay.internal.data.model.AppExitInfo
import com.ms.square.debugoverlay.internal.data.model.DeviceInfo
import com.ms.square.debugoverlay.internal.data.model.JankStatsUiState
import com.ms.square.debugoverlay.internal.data.model.NetworkStats
import com.ms.square.debugoverlay.internal.data.source.AppExitDataSource
import com.ms.square.debugoverlay.internal.data.source.DeviceInfoDataSource
import com.ms.square.debugoverlay.internal.data.source.JankStatsDataSource
import com.ms.square.debugoverlay.internal.data.source.LogcatDataSource
import com.ms.square.debugoverlay.internal.data.source.NetStatsDataSource
import com.ms.square.debugoverlay.internal.util.runCatchingNonCancellation
import com.ms.square.debugoverlay.internal.util.throttleLatest
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/** Default name shown when a custom log source doesn't provide a source name. */
internal const val DEFAULT_CUSTOM_LOG_SOURCE_NAME = "Custom"

@Suppress("TooManyFunctions")
internal class DebugOverlayDataRepository(
  private val context: Context,
  private val scope: CoroutineScope,
  initialLogcatMaxEntries: Int,
) {

  private val currentNetworkRequestSource = MutableStateFlow<NetworkRequestSource>(NoOpNetworkRequestSource)
  private val logcatDataSource = LogcatDataSource(scope, initialMaxEntries = initialLogcatMaxEntries)
  private val customLogSource = MutableStateFlow<LogSource?>(null)
  private val netStatsDataSource = NetStatsDataSource(scope)
  private val deviceInfoDataSource = DeviceInfoDataSource(context, scope)
  private val jankStatsDataSource = JankStatsDataSource()
  private val appExitDataSource = AppExitDataSource(context, scope)

  private val crashRecordStorage: CrashRecordStorage = DefaultCrashRecordStorage(context)

  init {
    scope.launch {
      try {
        awaitCancellation()
      } finally {
        // Note: netStatsDataSource self-cleans via stateIn + WhileSubscribed
        // The flow will automatically stop collecting when there are no active subscribers,
        // and resume when the UI resubscribes. No explicit cleanup needed.
        logcatDataSource.close()
      }
    }
  }

  // Logcat logs - always available (LogcatDataSource internally uses stateIn)
  val logcatLogs: Flow<List<LogEntry>> = logcatDataSource.logs

  // Custom source logs - empty list when no custom source is registered
  // Use hasCustomLogSource to determine if a source exists (e.g., for bug reports)
  @OptIn(ExperimentalCoroutinesApi::class)
  val customLogSourceLogs: StateFlow<List<LogEntry>> = customLogSource
    .flatMapLatest { source ->
      source?.logs
        ?.throttleLatest(500.milliseconds)
        ?.catch { e ->
          Logger.w("Custom log source error", e)
          emit(emptyList())
        }
        ?: flowOf(emptyList())
    }
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  // Custom source name (e.g., "Timber")
  val customLogSourceName: StateFlow<String?> = customLogSource
    .map { it?.sourceName }
    .stateIn(scope, SharingStarted.Eagerly, null)

  // Whether a custom log source is registered
  val hasCustomLogSource: StateFlow<Boolean> = customLogSource
    .map { it != null }
    .stateIn(scope, SharingStarted.Eagerly, false)

  val netStats: Flow<NetworkStats> = netStatsDataSource.stats
  val deviceInfo: Flow<DeviceInfo?> = deviceInfoDataSource.deviceInfo
  val jankStats: Flow<JankStatsUiState> = jankStatsDataSource.state

  val isAppExitSupported: Boolean
    get() = appExitDataSource.isSupported

  val appExitInfos: Flow<List<AppExitInfo>> = appExitDataSource.appExitInfos

  @OptIn(ExperimentalCoroutinesApi::class)
  val networkRequests: StateFlow<List<NetworkRequest>> = currentNetworkRequestSource
    .flatMapLatest { source -> source.requests }
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  // null means "not read from disk yet"
  private val _crashRecords = MutableStateFlow<List<CrashRecordInfo>?>(null)

  val crashRecords: StateFlow<List<CrashRecordInfo>?> = _crashRecords.asStateFlow()
    .onStart { refreshCrashRecords() }
    .stateIn(scope, SharingStarted.Lazily, null)

  // Drives the Crash tab's count badge.
  val crashRecordCount: StateFlow<Int> = crashRecords
    .map { it?.size ?: 0 }
    .stateIn(scope, SharingStarted.Lazily, 0)

  private suspend fun refreshCrashRecords() {
    _crashRecords.value = withContext(Dispatchers.IO) {
      runCatchingNonCancellation {
        crashRecordStorage.listCrashRecords()
      }.onFailure {
        Logger.e("Failed to refresh crash records", it)
      }.getOrDefault(emptyList())
    }
  }

  /**
   * Builds a crash record from the current in-memory snapshots and persists it.
   *
   * Non-suspending and dispatcher-free so [CrashHandler] can call it directly from the
   * crashing thread before delegating to the previous handler. The snapshots it reads are
   * kept private: assembling the record here is the only reason they exist.
   *
   * App info is queried here rather than cached up front: it's two PackageManager IPC calls,
   * cheap next to the file write below, and caching it would cost every app start for a read
   * that might be unnecessary. Guarded separately so a failure costs the app info
   * field, not the whole record.
   */
  fun writeCrashRecordSync(thread: Thread, throwable: Throwable) {
    crashRecordStorage.writeSync(
      buildCrashRecord(
        thread = thread,
        throwable = throwable,
        appInfo = runCatching { DefaultAppInfoProvider.getAppInfo(context) }.getOrNull(),
        logcatLogs = logcatDataSource.queryLogcatSnapshot(),
        customLogSourceData = customLogSourceName.value?.let { name ->
          CustomLogSourceData(customLogSourceLogs.value, name)
        },
        networkRequests = networkRequests.value
      )
    )
  }

  /**
   * Deletes a persisted crash record and re-syncs [crashRecords].
   */
  fun deleteCrashRecord(info: CrashRecordInfo) {
    scope.launch {
      crashRecordStorage.deleteCrashRecord(info)
      refreshCrashRecords()
    }
  }

  // Snapshot methods for bug reports (use cached value if available, otherwise query directly)
  fun queryLogcatSnapshot(): List<LogEntry> = logcatDataSource.queryLogcatSnapshot()
  suspend fun queryDeviceInfoSnapshot(): DeviceInfo = deviceInfoDataSource.queryDeviceInfoSnapshot()
  suspend fun queryAppExitInfosSnapshot(): List<AppExitInfo> = appExitDataSource.queryAppExitInfosSnapshot()

  fun setNetworkSource(source: NetworkRequestSource) {
    currentNetworkRequestSource.value = source
  }

  fun setCustomLogSource(source: LogSource?) {
    customLogSource.value = source
  }

  fun setLogcatMaxEntries(maxEntries: Int) {
    logcatDataSource.maxEntries = maxEntries
  }

  fun startOrResumeJankStatsTracking(activity: Activity) {
    jankStatsDataSource.startOrResumeTracking(activity)
  }

  fun pauseJankStatsTracking(activity: Activity) {
    jankStatsDataSource.pauseTracking(activity)
  }

  fun stopJankStatsTracking(activity: Activity) {
    jankStatsDataSource.stopTracking(activity)
  }

  /**
   * Clears accumulated entries from sources that opt into [Clearable]:
   * built-in logcat capture plus the current network and custom log sources
   * when they implement [Clearable]. Custom external sources that don't
   * implement [Clearable] are silently skipped.
   */
  fun clearAllLogs() {
    logcatDataSource.clear()
    (currentNetworkRequestSource.value as? Clearable)?.clear()
    (customLogSource.value as? Clearable)?.clear()
  }
}
