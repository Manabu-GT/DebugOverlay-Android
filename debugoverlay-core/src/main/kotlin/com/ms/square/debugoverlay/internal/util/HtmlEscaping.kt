package com.ms.square.debugoverlay.internal.util

private val HTML_ESCAPE_CHARS = charArrayOf('&', '<', '>', '"', '\'')

/** Extra capacity for escaped characters (e.g., '&' becomes '&amp;' = +4 chars) */
private const val ESCAPE_BUFFER_CAPACITY = 16

/**
 * Escapes this string for safe insertion into HTML text or attribute values.
 *
 * Note: This is not suitable for escaping JavaScript or CSS contexts.
 */
internal fun String.escapeHtml(): String {
  // Fast-path: return original instance if no escaping needed
  if (indexOfAny(HTML_ESCAPE_CHARS) == -1) return this

  val builder = StringBuilder(length + ESCAPE_BUFFER_CAPACITY)
  for (ch in this) {
    when (ch) {
      '&' -> builder.append("&amp;")
      '<' -> builder.append("&lt;")
      '>' -> builder.append("&gt;")
      '"' -> builder.append("&quot;")
      '\'' -> builder.append("&#39;")
      else -> builder.append(ch)
    }
  }
  return builder.toString()
}
