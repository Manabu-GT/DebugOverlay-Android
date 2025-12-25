package com.ms.square.debugoverlay.internal.util

import java.io.File

internal fun File.checkFolderExists() {
  check(mkdirs() || isDirectory) { "Failed to create a folder: $absolutePath" }
}
