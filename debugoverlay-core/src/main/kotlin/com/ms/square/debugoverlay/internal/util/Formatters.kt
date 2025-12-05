package com.ms.square.debugoverlay.internal.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val BYTES_PER_KB = 1024L
private const val BYTES_PER_MB = 1024L * 1024L
private const val BYTES_PER_GB = 1024L * 1024L * 1024L

/**
 * Format bytes to human-readable string.
 */
internal fun formatBytes(bytes: Long?): String = when {
  bytes == null || bytes < 0 -> "—"
  bytes < BYTES_PER_KB -> "$bytes B"
  bytes < BYTES_PER_MB -> {
    val kb = bytes / BYTES_PER_KB.toDouble()
    @Suppress("MagicNumber")
    if (kb < 10) "%.2f KB".format(kb) else "%.1f KB".format(kb)
  }
  bytes < BYTES_PER_GB -> "%.1f MB".format(bytes / BYTES_PER_MB.toDouble())
  else -> "%.1f GB".format(bytes / BYTES_PER_GB.toDouble())
}

/**
 * Format text size to human-readable string.
 */
internal fun formatTextSize(length: Int): String = when {
  length < BYTES_PER_KB -> "$length chars"
  length < BYTES_PER_MB -> "${length / BYTES_PER_KB} KB"
  else -> "${"%.1f".format(length / (BYTES_PER_MB.toDouble()))} MB"
}

internal fun formatTimestamp(timestamp: Long): String {
  val date = Date(timestamp)
  val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
  return formatter.format(date)
}

/**
 * Format JSON as syntax-highlighted HTML.
 */
@Suppress("MaxLineLength") // HTML template
internal fun formatJsonAsHtml(json: String): String {
  val escaped = json
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

  // Simple syntax highlighting with regex
  val highlighted = escaped
    .replace(Regex("""(&quot;[^&]+&quot;)\s*:"""), """<span style="color: #9cdcfe;">$1</span>:""") // Keys
    .replace(Regex(""":\s*(&quot;[^&]+&quot;)"""), """: <span style="color: #ce9178;">$1</span>""") // String values
    .replace(Regex("""\b(\d+\.?\d*)\b"""), """<span style="color: #b5cea8;">$1</span>""") // Numbers
    .replace(Regex("""\b(true|false)\b"""), """<span style="color: #569cd6;">$1</span>""") // Booleans
    .replace(Regex("""\b(null)\b"""), """<span style="color: #808080;">$1</span>""") // Null

  return """
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style>
    body {
      font-family: 'Courier New', monospace;
      font-size: 12px;
      background: #1e1e1e;
      color: #d4d4d4;
      margin: 16px;
      padding: 0;
      overflow-wrap: break-word;
      word-wrap: break-word;
    }
    pre {
      white-space: pre-wrap;
      margin: 0;
      line-height: 1.5;
    }
  </style>
</head>
<body>
  <pre>$highlighted</pre>
</body>
</html>
  """.trimIndent()
}

/**
 * Format plain text as HTML.
 */
internal fun formatPlainTextAsHtml(text: String): String {
  val escaped = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

  return """
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style>
    body {
      font-family: 'Courier New', monospace;
      font-size: 12px;
      background: #1e1e1e;
      color: #d4d4d4;
      margin: 16px;
      padding: 0;
      overflow-wrap: break-word;
      word-wrap: break-word;
    }
    pre {
      white-space: pre-wrap;
      margin: 0;
      line-height: 1.5;
    }
  </style>
</head>
<body>
  <pre>$escaped</pre>
</body>
</html>
  """.trimIndent()
}
