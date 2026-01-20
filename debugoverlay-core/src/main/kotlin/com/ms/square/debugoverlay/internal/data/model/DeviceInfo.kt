package com.ms.square.debugoverlay.internal.data.model

/**
 * Comprehensive device information collected without requiring dangerous permissions.
 * Useful for bug reports, diagnostics, and device compatibility analysis.
 */
internal data class DeviceInfo(
  val hardware: HardwareInfo,
  val battery: BatteryInfo,
  val system: SystemInfo,
  val network: NetworkInfo,
)

/**
 * Hardware information including device specs, CPU, display, and storage.
 */
internal data class HardwareInfo(
  // Device
  val manufacturer: String,
  val model: String,
  val brand: String,

  // CPU
  val cpuArchitecture: String,
  val cpuCores: Int,
  val supportedAbis: List<String>,

  // Display
  val screenSizeCategory: String, // "small", "normal", "large", "xlarge"
  val screenDensity: String, // "ldpi", "mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"
  val screenResolution: String, // "1080x2400"
  val currentRefreshRate: Float, // Hz (e.g., 60.0, 90.0, 120.0)
  val maxRefreshRate: Float, // Hz (e.g., 60.0, 90.0, 120.0)
  val openGlVersion: String, // e.g., "3.2"

  // Memory & Storage
  val totalRam: Long, // bytes
  val availableRam: Long, // bytes (dynamic, snapshot at collection time)
  val totalStorage: Long, // bytes
  val availableStorage: Long, // bytes (dynamic, snapshot at collection time)

  // Hardware features
  val hardwareFeature: HardwareFeature,
)

/**
 * Hardware features.
 */
internal data class HardwareFeature(
  val hasNfc: Boolean,
  val hasBluetooth: Boolean,
  val hasCamera: Boolean,
  val hasFingerprint: Boolean,
)

/**
 * Battery information.
 */
internal data class BatteryInfo(
  val level: Int, // percentage (0-100)
  val status: String, // "Charging", "Discharging", "Full", "Not Charging"
)

/**
 * System and build information.
 */
internal data class SystemInfo(
  // Android
  val androidVersion: String,
  val apiLevel: Int,
  val securityPatch: String,

  // Build
  val buildId: String,
  val buildType: String, // "user", "userdebug", "eng"
  val buildTags: String, // "release-keys", "test-keys", "dev-keys"
  val bootloader: String,
  val fingerprint: String,
  val kernelVersion: String,
  val uptimeMs: Long,

  // Security & Services
  val isRooted: Boolean,
  val isEmulator: Boolean,
  val playServicesVersion: String?, // null if not available (e.g., "24.45.33")
  val playServicesVersionCode: Long?, // null if not available (e.g., 244533000)

  // Localization
  val locale: String, // e.g., "en_US"
  val language: String, // e.g., "English (United States)"
  val timeZone: String, // e.g., "America/Los_Angeles (PST)"
)

/**
 * Network connection information (requires ACCESS_NETWORK_STATE normal permission).
 */
internal data class NetworkInfo(
  val type: NetworkType, // WIFI, CELLULAR, NONE
  val isConnected: Boolean,
) {
  companion object {
    val NONE = NetworkInfo(NetworkType.NONE, false)
  }
}

/**
 * Network connection type.
 */
internal enum class NetworkType {
  WIFI,
  CELLULAR,
  ETHERNET,
  VPN,
  NONE,
}
