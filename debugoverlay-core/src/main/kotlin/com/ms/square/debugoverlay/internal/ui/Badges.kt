package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.internal.util.httpMethodColor
import com.ms.square.debugoverlay.internal.util.httpStatusColor

/**
 * HTTP method badge (GET, POST, etc.)
 */
@Composable
internal fun MethodBadge(method: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    color = method.httpMethodColor,
    shape = MaterialTheme.shapes.extraSmall
  ) {
    Text(
      text = method,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      style = MaterialTheme.typography.labelSmall,
      color = Color.Black,
      fontWeight = FontWeight.Bold
    )
  }
}

/**
 * Status code badge with color coding.
 */
@Composable
internal fun StatusCodeBadge(statusCode: Int?, modifier: Modifier = Modifier) {
  Text(
    text = statusCode?.toString() ?: "ERR",
    modifier = modifier,
    style = MaterialTheme.typography.titleMedium,
    color = statusCode.httpStatusColor,
    fontWeight = FontWeight.Bold,
    fontFamily = FontFamily.Monospace
  )
}
