package com.ms.square.debugoverlay.internal.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ms.square.debugoverlay.internal.data.TextType
import com.ms.square.debugoverlay.internal.util.copyToClipboard
import com.ms.square.debugoverlay.internal.util.formatJsonIfPossible
import com.ms.square.debugoverlay.internal.util.formatTextSize

private const val COMPOSE_TEXT_MAX_SIZE = 10_000 // Use Compose Text
private const val TEXT_VIEW_MAX_SIZE = 500_000 // Use TextView, above this truncate

/**
 * Text preview with three-tier performance optimization for large texts.
 */
@Composable
internal fun TextPreview(text: String, textType: TextType) {
  // format if JSON
  val formatted = remember(text) {
    if (textType == TextType.JSON) {
      formatJsonIfPossible(text)
    } else {
      text
    }
  }
  when {
    // Tier 1: Small - Compose Text
    formatted.length < COMPOSE_TEXT_MAX_SIZE -> CompactTextPreview(formatted)
    // Tier 2: Medium/Large - TextView
    formatted.length < TEXT_VIEW_MAX_SIZE -> {
      TextViewTextPreview(formatted)
    }
    // Tier 3: Very Large - Truncate
    else -> TruncatedTextPreview(formatted)
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
    shape = MaterialTheme.shapes.medium,
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
 * Compose Text preview for short texts.
 */
@Composable
internal fun CompactTextPreview(body: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    tonalElevation = 1.dp
  ) {
    Text(
      text = body,
      modifier = Modifier.padding(16.dp),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      fontFamily = FontFamily.Monospace
    )
  }
}

/**
 * Truncated compose text preview for very large texts.
 */
@Composable
internal fun TruncatedTextPreview(text: String, modifier: Modifier = Modifier) {
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Warning message
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.small,
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
              COMPOSE_TEXT_MAX_SIZE
            )
          }.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onTertiaryContainer
        )
      }
    }

    // Truncated preview
    CompactTextPreview(body = text.take(COMPOSE_TEXT_MAX_SIZE))

    // Copy full body button
    Button(
      onClick = {
        scope.copyToClipboard(clipboard, text)
      },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Copy Full text (${formatTextSize(text.length)})")
    }
  }
}
