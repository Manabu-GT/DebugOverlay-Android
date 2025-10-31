package com.ms.square.debugoverlay.internal

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.ms.square.debugoverlay.DebugOverlay

/**
 * Content providers are loaded before the application class is created. [DebugOverlayInstaller] is
 * used to install [com.ms.square.debugoverlay.DebugOverlay] on application start.
 *
 * It automatically sets up the [com.ms.square.debugoverlay.DebugOverlay] code that runs in the main
 * app process.
 */
internal class DebugOverlayInstaller : ContentProvider() {

  override fun onCreate(): Boolean {
    // context here should be never null per the doc within onCreate()
    val application =
      context?.applicationContext as? Application ?: error("Can not cast the given context an Application")
    DebugOverlay.with(application).install()
    return true
  }

  override fun query(
    uri: Uri,
    projection: Array<out String?>?,
    selection: String?,
    selectionArgs: Array<out String?>?,
    sortOrder: String?,
  ): Cursor? = null

  override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String?>?): Int = 0

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String?>?): Int = 0

  override fun getType(uri: Uri): String? = null

  override fun insert(uri: Uri, values: ContentValues?): Uri? = null
}
