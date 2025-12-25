package com.ms.square.debugoverlay.internal.util

import java.io.File

/**
 * Ensures this file exists as a directory, creating it if necessary.
 * @throws IllegalStateException if the directory cannot be created and doesn't exist
 */
internal fun File.checkFolderExists() {
  check(mkdirs() || isDirectory) { "Failed to create a folder: $absolutePath" }
}
