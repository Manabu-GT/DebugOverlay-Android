package com.ms.square.debugoverlay.internal.util

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun CoroutineScope.copyToClipboard(clipboard: Clipboard, text: String, label: String = "") {
  launch {
    val clipEntry = ClipEntry(ClipData.newPlainText(label, text))
    clipboard.setClipEntry(clipEntry)
  }
}

/**
 * Builds the clipboard text off the main thread before copying. Use this overload when [text]
 * is expensive enough to risk jank (e.g. formatting an entire network transaction), instead of
 * computing it inline at the call site.
 */
internal fun CoroutineScope.copyToClipboard(clipboard: Clipboard, label: String = "", text: suspend () -> String) {
  launch {
    val resolved = withContext(Dispatchers.Default) { text() }
    val clipEntry = ClipEntry(ClipData.newPlainText(label, resolved))
    clipboard.setClipEntry(clipEntry)
  }
}
