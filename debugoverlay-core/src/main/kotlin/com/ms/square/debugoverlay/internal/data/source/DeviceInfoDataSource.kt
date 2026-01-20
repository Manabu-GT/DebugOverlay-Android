package com.ms.square.debugoverlay.internal.data.source

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.data.model.BatteryInfo
import com.ms.square.debugoverlay.internal.data.model.DeviceInfo
import com.ms.square.debugoverlay.internal.data.model.HardwareFeature
import com.ms.square.debugoverlay.internal.data.model.HardwareInfo
import com.ms.square.debugoverlay.internal.data.model.NetworkInfo
import com.ms.square.debugoverlay.internal.data.model.NetworkType
import com.ms.square.debugoverlay.internal.data.model.SystemInfo
import com.ms.square.debugoverlay.internal.util.DeviceRootDetector
import com.ms.square.debugoverlay.internal.util.currentRefreshRate
import com.ms.square.debugoverlay.internal.util.defaultDisplay
import com.ms.square.debugoverlay.internal.util.maxSupportedFps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.seconds

/**
 * Data source for collecting comprehensive device information without requiring dangerous permissions.
 * Static device properties are cached via lazy initialization; dynamic values (battery, network,
 * available memory, locale, uptime) are queried fresh on each call to [collectDeviceInfo].
 */
@Suppress("TooManyFunctions")
internal class DeviceInfoDataSource(private val context: Context, scope: CoroutineScope) {

  // ===== Truly Immutable (safe to cache forever) =====

  // this returns configured (not necessarily online) cores
  private val cpuCores by lazy {
    runCatching { Os.sysconf(OsConstants._SC_NPROCESSORS_CONF).toInt() }
      .getOrDefault(Runtime.getRuntime().availableProcessors())
  }

  private val supportedAbis: List<String> by lazy { Build.SUPPORTED_ABIS.toList() }

  private val hardwareFeatures by lazy {
    HardwareFeature(
      hasNfc = hasSystemFeature(PackageManager.FEATURE_NFC),
      hasBluetooth = hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
      hasCamera = hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
      hasFingerprint = hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    )
  }

  // ===== Immutable Per Boot (safe to cache) =====

  private val totalRam by lazy { queryMemoryInfo()?.totalMem ?: 0L }
  private val totalStorage by lazy { queryTotalStorage() }
  private val openGlVersion by lazy { queryOpenGlVersion() }
  private val kernelVersion by lazy {
    runCatching { System.getProperty("os.version") }.getOrNull() ?: "unknown"
  }
  private val playServicesInfo by lazy { queryPlayServicesVersion() }
  private val isEmulator by lazy { detectEmulator() }
  private val isRooted by lazy { DeviceRootDetector.isRooted(context) }

  // ===== Display Info (cache the Display reference and max refresh rate) =====

  private val defaultDisplay by lazy { context.defaultDisplay() }

  private val maxRefreshRate by lazy {
    defaultDisplay.maxSupportedFps
  }

  val deviceInfo: StateFlow<DeviceInfo?> = flow {
    while (currentCoroutineContext().isActive) {
      emit(collectDeviceInfo())
      delay(3.seconds)
    }
  }.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.WhileSubscribed(), null)

  /**
   * Returns a snapshot of device info for bug reports.
   * Uses cached value if available, otherwise queries directly.
   */
  suspend fun queryDeviceInfoSnapshot(): DeviceInfo {
    // Use cached value if already loaded (UI was viewed)
    deviceInfo.value?.let { return it }
    // Otherwise query directly
    return withContext(Dispatchers.IO) { collectDeviceInfo() }
  }

  private fun collectDeviceInfo(): DeviceInfo = DeviceInfo(
    hardware = HardwareInfo(
      // Immutable - use cached
      manufacturer = Build.MANUFACTURER,
      model = Build.MODEL,
      brand = Build.BRAND,
      cpuArchitecture = supportedAbis.firstOrNull() ?: "unknown",
      cpuCores = cpuCores,
      supportedAbis = supportedAbis,
      totalRam = totalRam,
      openGlVersion = openGlVersion,
      maxRefreshRate = maxRefreshRate,
      hardwareFeature = hardwareFeatures,

      // Config-dependent - queried fresh to reflect runtime changes (rotation, font scaling)
      // Could be cached with config change listener for efficiency in the future if needed.
      screenSizeCategory = computeScreenSizeCategory(),
      screenDensity = computeScreenDensity(),
      screenResolution = computeScreenResolution(),

      totalStorage = totalStorage, // Cached (immutable per boot)

      // Dynamic - always fresh
      currentRefreshRate = defaultDisplay.currentRefreshRate,
      availableRam = queryMemoryInfo()?.availMem ?: 0L,
      availableStorage = queryAvailableStorage()
    ),
    battery = queryBatteryInfo(), // Dynamic
    system = SystemInfo(
      // Mix of immutable (cached) and dynamic
      androidVersion = Build.VERSION.RELEASE,
      apiLevel = Build.VERSION.SDK_INT,
      securityPatch = Build.VERSION.SECURITY_PATCH,
      buildId = Build.ID,
      buildType = Build.TYPE,
      buildTags = Build.TAGS,
      bootloader = Build.BOOTLOADER,
      fingerprint = Build.FINGERPRINT,
      kernelVersion = kernelVersion,
      isRooted = isRooted,
      isEmulator = isEmulator,
      playServicesVersion = playServicesInfo?.first,
      playServicesVersionCode = playServicesInfo?.second,
      // Dynamic
      uptimeMs = SystemClock.elapsedRealtime(),
      locale = Locale.getDefault().toString(),
      language = Locale.getDefault().displayName,
      timeZone = queryTimeZoneInfo()
    ),
    network = queryNetworkInfo() // Dynamic
  )

  // ===== Hardware Information =====

  private fun computeScreenSizeCategory(): String {
    val screenLayout = context.resources.configuration.screenLayout
    val screenSize = screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    return when (screenSize) {
      Configuration.SCREENLAYOUT_SIZE_SMALL -> "small"
      Configuration.SCREENLAYOUT_SIZE_NORMAL -> "normal"
      Configuration.SCREENLAYOUT_SIZE_LARGE -> "large"
      Configuration.SCREENLAYOUT_SIZE_XLARGE -> "xlarge"
      else -> "undefined"
    }
  }

  @Suppress("MagicNumber") // Standard Android density bucket thresholds
  private fun computeScreenDensity(): String {
    val metrics = context.resources.displayMetrics
    val densityDpi = metrics.densityDpi
    val densityName = when {
      densityDpi <= 120 -> "ldpi"
      densityDpi <= 160 -> "mdpi"
      densityDpi <= 240 -> "hdpi"
      densityDpi <= 320 -> "xhdpi"
      densityDpi <= 480 -> "xxhdpi"
      densityDpi <= 640 -> "xxxhdpi"
      else -> "xxxhdpi+"
    }
    return "$densityDpi dpi ($densityName)"
  }

  private fun computeScreenResolution(): String {
    val metrics = context.resources.displayMetrics
    return "${metrics.widthPixels} × ${metrics.heightPixels}"
  }

  @Suppress("MagicNumber") // OpenGL ES version encoding: major in high 16 bits, minor in low 16 bits
  private fun queryOpenGlVersion(): String = runCatching {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val version = am.deviceConfigurationInfo.reqGlEsVersion
    "${version shr 16}.${version and 0xFFFF}"
  }.getOrElse { e ->
    Logger.w("openGlVersion query failed", e)
    "unknown"
  }

  private fun queryMemoryInfo(): ActivityManager.MemoryInfo? = runCatching {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
  }.getOrElse { e ->
    Logger.w("MemoryInfo query failed", e)
    null
  }

  private fun queryTotalStorage(): Long = runCatching {
    val dataDir = Environment.getDataDirectory()
    dataDir.totalSpace
  }.getOrElse { e ->
    Logger.w("TotalStorage query failed", e)
    0L
  }

  private fun queryAvailableStorage(): Long = runCatching {
    val dataDir = Environment.getDataDirectory()
    dataDir.usableSpace
  }.getOrElse { e ->
    Logger.w("AvailableStorage query failed", e)
    0L
  }

  // ===== Battery Information =====

  private fun queryBatteryInfo(): BatteryInfo = runCatching {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val rawLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: Int.MIN_VALUE
    // Note: getIntProperty returns Int.MIN_VALUE on devices without battery (TV, Automotive);
    // thus return 0 in such case.
    val level = if (rawLevel == Int.MIN_VALUE) 0 else rawLevel
    val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
    } else {
      BatteryManager.BATTERY_STATUS_UNKNOWN
    }
    BatteryInfo(level, status.batteryStatus)
  }.getOrElse { e ->
    Logger.w("BatteryInfo query failed", e)
    BatteryInfo(0, BatteryManager.BATTERY_STATUS_UNKNOWN.batteryStatus)
  }

  private val Int.batteryStatus: String
    get() = when (this) {
      BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
      BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
      BatteryManager.BATTERY_STATUS_FULL -> "Full"
      BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
      else -> "Unknown"
    }

  // ===== Hardware Features =====

  private fun hasSystemFeature(feature: String): Boolean = context.packageManager.hasSystemFeature(feature)

  // ===== System Information =====

  /**
   * This is a "best effort" detection that may have false positives.
   */
  private fun detectEmulator(): Boolean = (
    Build.FINGERPRINT.startsWith("generic") ||
      Build.FINGERPRINT.startsWith("unknown") ||
      Build.MODEL.contains("google_sdk") ||
      Build.MODEL.contains("Emulator") ||
      Build.MODEL.contains("Android SDK built for x86") ||
      Build.BOARD == "QC_Reference_Phone" ||
      Build.MANUFACTURER.contains("Genymotion") ||
      (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
      Build.PRODUCT == "google_sdk" ||
      Build.HARDWARE.contains("goldfish") ||
      Build.HARDWARE.contains("ranchu") ||
      Build.HARDWARE.contains("vbox86")
    )

  @Suppress("TooGenericExceptionCaught")
  private fun queryPlayServicesVersion(): Pair<String?, Long>? = try {
    val packageInfo = context.packageManager.getPackageInfo("com.google.android.gms", 0)
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      packageInfo.longVersionCode
    } else {
      @Suppress("DEPRECATION")
      packageInfo.versionCode.toLong()
    }
    packageInfo.versionName to versionCode
  } catch (_: PackageManager.NameNotFoundException) {
    // Expected case - Play Services not installed, no logging needed
    null
  } catch (e: Exception) {
    Logger.w("getPlayServicesVersion unexpected failure", e)
    null
  }

  private fun queryTimeZoneInfo(): String {
    val timeZone = TimeZone.getDefault()
    val displayName = timeZone.getDisplayName(false, TimeZone.SHORT)
    return "${timeZone.id} ($displayName)"
  }

  // ===== Network Information =====
  private fun queryNetworkInfo(): NetworkInfo {
    return runCatching {
      val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return@runCatching NetworkInfo.NONE

      val network = connectivityManager.activeNetwork ?: return@runCatching NetworkInfo.NONE
      val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@runCatching NetworkInfo.NONE

      val networkType = when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
        else -> NetworkType.NONE
      }
      val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

      NetworkInfo(networkType, isConnected)
    }.getOrElse { e ->
      Logger.w("NetworkInfo query failed", e)
      NetworkInfo.NONE
    }
  }
}
