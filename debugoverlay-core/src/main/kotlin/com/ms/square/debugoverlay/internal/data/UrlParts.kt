package com.ms.square.debugoverlay.internal.data

import androidx.core.net.toUri

internal data class UrlParts(val scheme: String, val domain: String, val path: String, val query: String?) {
  val pathWithQuery: String
    get() = if (query != null) "$path?$query" else path

  companion object {
    fun from(url: String): UrlParts {
      val uri = url.toUri()
      return UrlParts(
        scheme = uri.scheme ?: "",
        domain = uri.host ?: "",
        path = uri.path?.ifEmpty { "/" } ?: "/",
        query = uri.query?.ifEmpty { null }
      )
    }
  }
}
