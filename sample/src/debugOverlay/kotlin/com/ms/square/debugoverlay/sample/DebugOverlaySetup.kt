package com.ms.square.debugoverlay.sample

import android.content.Context
import com.ms.square.debugoverlay.BugReportDataContributor
import com.ms.square.debugoverlay.DebugOverlay
import java.io.File
import java.io.PrintWriter

/**
 * Configures DebugOverlay with custom contributors.
 * This file is in the debugOverlay source set, shared by debug and releaseWithOverlay builds.
 */
object DebugOverlaySetup {
  fun init(context: Context) {
    DebugOverlay.addBugReportContributor(SharedPreferencesContributor(context.applicationContext))
  }
}

/**
 * Example BugReportDataContributor that dumps SharedPreferences.
 * Filters out sensitive keys containing "token", "password", "secret", or "key".
 */
private class SharedPreferencesContributor(private val context: Context) : BugReportDataContributor {

  override val filename = "shared_preferences.txt"

  override fun writeTo(outputStream: java.io.OutputStream) {
    PrintWriter(outputStream).use { writer ->
      val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
      val prefsFiles = prefsDir.listFiles { file -> file.extension == "xml" } ?: emptyArray()

      if (prefsFiles.isEmpty()) {
        writer.println("No SharedPreferences files found")
        return@use
      }

      prefsFiles.sortedBy { it.name }.forEach { file ->
        val prefsName = file.nameWithoutExtension
        writer.println("=== $prefsName ===")

        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.all.entries
          .filterNot { it.key.containsSensitiveKeyword() }
          .sortedBy { it.key }
          .forEach { (key, value) ->
            writer.println("  $key = $value")
          }
        writer.println()
      }
    }
  }

  private fun String.containsSensitiveKeyword(): Boolean =
    listOf("token", "password", "secret", "key", "credential", "auth")
      .any { this.contains(it, ignoreCase = true) }
}
