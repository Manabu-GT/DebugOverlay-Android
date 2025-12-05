package com.ms.square.debugoverlay.internal.ui

import android.content.ClipData
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.TextType
import com.ms.square.debugoverlay.internal.util.formatJsonAsHtml
import com.ms.square.debugoverlay.internal.util.formatPlainTextAsHtml
import com.ms.square.debugoverlay.internal.util.formatTextSize
import kotlinx.coroutines.launch

private const val SMALL_TEXT_THRESHOLD = 10_000 // Use Compose Text
private const val LARGE_TEXT_THRESHOLD = 500_000 // Use TextView, above this truncate

/**
 * Text preview with four-tier performance optimization for large texts.
 */
@Composable
internal fun TextPreview(text: String, textType: TextType, modifier: Modifier = Modifier) {
  when {
    // Tier 1: Small texts - Use Compose Text (best UX, integrated styling)
    text.length < SMALL_TEXT_THRESHOLD -> {
      CompactTextPreview(text, modifier)
    }
    // Tier 2: Structured data - Offer formatted view
    textType == TextType.JSON || textType == TextType.HTML -> {
      StructuredTextPreview(text, textType, modifier)
    }
    // Tier 3: Large plain text - Use native TextView (performant)
    text.length < LARGE_TEXT_THRESHOLD -> {
      TextViewTextPreview(text, modifier)
    }
    // Tier 4: Very large - Truncate (always instant)
    else -> {
      TruncatedTextPreview(text, modifier)
    }
  }
}

/**
 * Structured text data preview with raw/formatted toggle.
 */
@Composable
private fun StructuredTextPreview(text: String, textType: TextType, modifier: Modifier = Modifier) {
  var showFormatted by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxWidth()) {
    // Toggle buttons
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = { showFormatted = false },
        modifier = Modifier.weight(1f)
      ) {
        Text("Raw")
      }
      Button(
        onClick = { showFormatted = true },
        modifier = Modifier.weight(1f)
      ) {
        Text("Formatted")
      }
    }

    if (showFormatted) {
      WebViewTextPreview(text, textType)
    } else {
      TextViewTextPreview(text)
    }
  }
}

/**
 * Native TextView preview for large plain text (performant).
 */
@Suppress("MagicNumber")
@Composable
private fun TextViewTextPreview(text: String, modifier: Modifier = Modifier) {
  val textColor = MaterialTheme.colorScheme.onSurface

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    tonalElevation = 1.dp
  ) {
    AndroidView(
      factory = { context ->
        TextView(context).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          )
          textSize = 12f
          typeface = Typeface.MONOSPACE
          setTextIsSelectable(true)
          setTextColor(textColor.toArgb())
          setBackgroundColor(Color.TRANSPARENT)
        }
      },
      update = { textView ->
        textView.text = text
      },
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    )
  }
}

/**
 * WebView preview for formatted JSON/HTML text.
 */
@Composable
private fun WebViewTextPreview(text: String, textType: TextType, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    tonalElevation = 1.dp
  ) {
    AndroidView(
      factory = { context ->
        WebView(context).apply {
          settings.javaScriptEnabled = false // Security: No JS needed
          settings.builtInZoomControls = true
          settings.displayZoomControls = false
          settings.setSupportZoom(true)
          setBackgroundColor(Color.TRANSPARENT)
        }
      },
      update = { webView ->
        val html = when (textType) {
          TextType.JSON -> formatJsonAsHtml(text)
          // For now, just display the raw HTML for BodyType.HTML.
          // Could add a toggle to show rendered vs source
          else -> formatPlainTextAsHtml(text)
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
      },
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 200.dp, max = 600.dp)
    )
  }
}

/**
 * Compose Text preview for short texts.
 */
@Composable
internal fun CompactTextPreview(body: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    tonalElevation = 1.dp
  ) {
    Text(
      text = body,
      modifier = Modifier.padding(16.dp),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      fontFamily = FontFamily.Monospace,
      fontSize = 12.sp,
      lineHeight = 18.sp
    )
  }
}

/**
 * Truncated compose text preview for very large texts.
 */
@Composable
internal fun TruncatedTextPreview(text: String, modifier: Modifier = Modifier) {
  val clipboard = LocalClipboard.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Warning message
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
          text = "Text too large (${formatTextSize(text.length)}). Showing first ${
            formatTextSize(
              SMALL_TEXT_THRESHOLD
            )
          }.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onTertiaryContainer
        )
      }
    }

    // Truncated preview
    CompactTextPreview(body = text.take(SMALL_TEXT_THRESHOLD))

    // Copy full body button
    Button(
      onClick = {
        scope.launch {
          val clipboardLabel = context.getString(R.string.debugoverlay_clipboard_label)
          val clipEntry = ClipEntry(ClipData.newPlainText(clipboardLabel, text))
          clipboard.setClipEntry(clipEntry)
        }
      },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Copy Full text (${formatTextSize(text.length)})")
    }
  }
}
