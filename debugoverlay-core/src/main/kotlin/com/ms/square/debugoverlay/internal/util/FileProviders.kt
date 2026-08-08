package com.ms.square.debugoverlay.internal.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

// Must stay in sync with the provider authority declared in this library's AndroidManifest.xml
// ("${applicationId}.debugoverlay.bugreport.provider").
private const val PROVIDER_AUTHORITY_SUFFIX = ".debugoverlay.bugreport.provider"

/**
 * Wraps [file] in a `content://` [Uri] served by DebugOverlay's own [FileProvider], for sharing
 * exports (bug report archives, crash logs) with other apps.
 *
 * [file] must live under one of the paths declared in `res/xml/debugoverlay_bugreport_paths.xml`,
 * or [FileProvider] throws [IllegalArgumentException].
 */
internal fun Context.debugOverlayFileUri(file: File): Uri =
  FileProvider.getUriForFile(this, "$packageName$PROVIDER_AUTHORITY_SUFFIX", file)
