package com.ms.square.debugoverlay.internal.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private val prettyJson = Json { prettyPrint = true }

/**
 * Pretty-prints [text] if it parses as valid JSON, otherwise returns it unchanged.
 */
internal fun formatJsonIfPossible(text: String): String = try {
  val element = Json.parseToJsonElement(text)
  prettyJson.encodeToString(JsonElement.serializer(), element)
} catch (_: Exception) {
  text
}
