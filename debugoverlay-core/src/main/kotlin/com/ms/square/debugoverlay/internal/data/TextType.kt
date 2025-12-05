package com.ms.square.debugoverlay.internal.data

internal enum class TextType {
  JSON,
  HTML,
  XML,
  PLAIN,
  ;

  companion object Companion {

    /**
     * Detect text type from http content-type header and content.
     */
    fun from(body: String, contentType: String?): TextType {
      // Check content-type header first
      contentType?.lowercase()?.let { ct ->
        when {
          ct.contains("json") -> return JSON
          ct.contains("html") -> return HTML
          ct.contains("xml") -> return XML
        }
      }
      // Fallback: Detect from content
      val trimmed = body.trimStart()
      return when {
        trimmed.startsWith("{") || trimmed.startsWith("[") -> JSON
        trimmed.startsWith(
          "<!DOCTYPE html",
          ignoreCase = true
        ) || trimmed.startsWith("<html", ignoreCase = true) -> HTML
        trimmed.startsWith("<?xml") || trimmed.startsWith("<") -> XML
        else -> PLAIN
      }
    }
  }
}
