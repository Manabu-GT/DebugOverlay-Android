package com.ms.square.debugoverlay.internal.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

// Must stay in sync with the provider authority declared in this library's AndroidManifest.xml
// ("${applicationId}.debugoverlay.fileprovider").
private const val PROVIDER_AUTHORITY_SUFFIX = ".debugoverlay.fileprovider"

/**
 * Wraps [file] in a `content://` [Uri] served by DebugOverlay's own [FileProvider], for sharing
 * exports (bug report archives, crash logs) with other apps.
 *
 * [file] must live under one of the paths declared in `res/xml/debugoverlay_file_provider_paths.xml`,
 * or [FileProvider] throws [IllegalArgumentException].
 */
internal fun Context.debugOverlayFileUri(file: File): Uri =
  FileProvider.getUriForFile(this, "$packageName$PROVIDER_AUTHORITY_SUFFIX", file)
