package com.ms.square.debugoverlay.internal.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.ms.square.debugoverlay.internal.Logger
import java.io.File

/**
 * Best effort check for a debug/QA environment.
 * Returns true if the device appears to be rooted or is a custom engineering build.
 */
internal object DeviceRootDetector {

  fun isRooted(context: Context): Boolean = checkBuildTags() || checkRootFiles() || checkRootPackages(context)

  private fun checkBuildTags(): Boolean {
    val buildTags = Build.TAGS
    return buildTags != null && buildTags.contains("test-keys")
  }

  private fun checkRootFiles(): Boolean {
    val paths = arrayOf(
      "/system/app/Superuser.apk",
      "/sbin/su",
      "/system/bin/su",
      "/system/xbin/su",
      "/data/local/xbin/su",
      "/data/local/bin/su",
      "/system/sd/xbin/su",
      "/system/bin/failsafe/su",
      "/data/local/su",
      "/su/bin/su"
    )
    return paths.any {
      runCatching { File(it).exists() }.getOrElse { e ->
        Logger.w("checkRootFiles failed", e)
        false
      }
    }
  }

  @Suppress("TooGenericExceptionCaught")
  private fun checkRootPackages(context: Context): Boolean {
    val rootPackages = arrayOf(
      "com.topjohnwu.magisk",
      "eu.chainfire.supersu",
      "com.noshufou.android.su",
      "com.koushikdutta.superuser",
      "com.zachspenner.zbksu",
      "com.thirdparty.superuser",
      "com.yellowes.su"
    )

    return rootPackages.any { packageName ->
      try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
      } catch (_: PackageManager.NameNotFoundException) {
        // Expected case - not installed, no logging needed
        false
      } catch (e: Exception) {
        Logger.w("checkRootPackages failed", e)
        false
      }
    }
  }
}
