package com.ms.square.debugoverlay.internal.util

/**
 * Case-insensitive content-type lookup. Headers are captured verbatim off the wire, so casing
 * varies by protocol (HTTP/2 lowercases header names, HTTP/1.1 typically capitalizes them);
 * an exact-key map lookup silently misses the latter.
 */
internal fun Map<String, String>.contentType(): String? =
  entries.firstOrNull { it.key.equals("content-type", ignoreCase = true) }?.value
