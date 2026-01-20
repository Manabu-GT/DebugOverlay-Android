package com.ms.square.debugoverlay.internal.bugreport

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.ms.square.debugoverlay.internal.bugreport.model.AppInfo

/**
 * Provides information about the host application for bug reports.
 */
internal sealed interface AppInfoProvider {

  /**
   * Collects app information from the given context.
   * All queries are fast (cached by system) and require no permissions.
   */
  fun getAppInfo(context: Context): AppInfo
}

/**
 * Default implementation that queries PackageManager APIs.
 */
internal data object DefaultAppInfoProvider : AppInfoProvider {

  override fun getAppInfo(context: Context): AppInfo {
    val packageManager = context.packageManager
    val packageName = context.packageName
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val applicationInfo = context.applicationInfo
    val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val installerPackage = queryInstallerPackage(context)

    return AppInfo(
      packageName = packageName,
      versionName = packageInfo.versionName,
      versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
      targetSdkVersion = applicationInfo.targetSdkVersion,
      minSdkVersion = applicationInfo.minSdkVersion,
      isDebuggable = isDebuggable,
      installerStore = mapToInstallerStore(installerPackage),
      installerPackage = installerPackage,
      firstInstallTime = packageInfo.firstInstallTime,
      lastUpdateTime = packageInfo.lastUpdateTime
    )
  }

  private fun queryInstallerPackage(context: Context): String? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
    } else {
      @Suppress("DEPRECATION")
      context.packageManager.getInstallerPackageName(context.packageName)
    }
  }.getOrNull()

  /**
   * Maps installer package names to human-readable store names.
   */
  private fun mapToInstallerStore(installerPackage: String?): String = when (installerPackage) {
    // Major app stores
    "com.android.vending" -> "Google Play Store"
    "com.amazon.venezia" -> "Amazon Appstore"
    "com.sec.android.app.samsungapps" -> "Samsung Galaxy Store"
    "com.huawei.appmarket" -> "Huawei AppGallery"
    "com.xiaomi.mipicks" -> "Xiaomi GetApps"
    "com.oppo.market" -> "OPPO App Market"
    "com.vivo.appstore" -> "vivo App Store"
    // Development/system installs
    "com.android.shell" -> "ADB"
    "com.google.android.packageinstaller" -> "System Installer"
    "com.android.packageinstaller" -> "System Installer"
    null -> "Sideloaded"
    else -> "Unknown ($installerPackage)"
  }
}
