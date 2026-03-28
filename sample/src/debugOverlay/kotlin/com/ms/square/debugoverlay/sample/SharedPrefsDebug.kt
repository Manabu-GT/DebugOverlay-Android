package com.ms.square.debugoverlay.sample

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.BugReportDataContributor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.io.PrintWriter

private val SENSITIVE_KEYWORDS = listOf("token", "password", "secret", "key", "credential", "auth")

/** Data model for a SharedPreferences file. */
private data class PrefsFile(val name: String, val entries: List<Pair<String, String>>)

/** Custom debug panel tab content that displays all SharedPreferences. */
@Composable
internal fun SharedPrefsTabContent(context: Context) {
  val prefsData by produceState(emptyList()) {
    value = withContext(Dispatchers.IO) { readAllPrefs(context) }
  }

  if (prefsData.isEmpty()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("No SharedPreferences found", style = MaterialTheme.typography.bodyMedium)
    }
    return
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    items(prefsData) { prefsFile ->
      PrefsCard(prefsFile)
    }
  }
}

@Composable
private fun PrefsCard(prefsFile: PrefsFile) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    tonalElevation = 1.dp
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = prefsFile.name,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
      )
      prefsFile.entries.forEach { (key, value) ->
        PrefsRow(key, value)
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrefsRow(key: String, value: String) {
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = key,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(end = 12.dp)
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Medium,
      fontFamily = FontFamily.Monospace,
      textAlign = TextAlign.End,
      modifier = Modifier.weight(1f, fill = false)
    )
  }
}

private fun readAllPrefs(context: Context): List<PrefsFile> {
  val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
  val prefsFiles = prefsDir.listFiles { file -> file.extension == "xml" } ?: return emptyList()

  return prefsFiles.sortedBy { it.name }.map { file ->
    val prefsName = file.nameWithoutExtension
    val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    val entries = prefs.all.entries
      .filterNot { it.key.containsSensitiveKeyword() }
      .sortedBy { it.key }
      .map { (key, value) -> key to value.toString() }
    PrefsFile(prefsName, entries)
  }
}

/**
 * BugReportDataContributor that dumps SharedPreferences.
 * Filters out sensitive keys containing "token", "password", "secret", or "key".
 */
internal class SharedPreferencesContributor(private val context: Context) : BugReportDataContributor {

  override val filename = "shared_preferences.txt"

  override fun writeTo(outputStream: OutputStream) {
    PrintWriter(outputStream).use { writer ->
      val prefsData = readAllPrefs(context)
      if (prefsData.isEmpty()) {
        writer.println("No SharedPreferences files found")
        return@use
      }

      prefsData.forEach { prefsFile ->
        writer.println("=== ${prefsFile.name} ===")
        prefsFile.entries.forEach { (key, value) ->
          writer.println("  $key = $value")
        }
        writer.println()
      }
    }
  }
}

private fun String.containsSensitiveKeyword(): Boolean = SENSITIVE_KEYWORDS.any { this.contains(it, ignoreCase = true) }
