package com.ms.square.debugoverlay.internal.util

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.copyToClipboard(clipboard: Clipboard, text: String, label: String = "") {
  launch {
    val clipEntry = ClipEntry(ClipData.newPlainText(label, text))
    clipboard.setClipEntry(clipEntry)
  }
}
