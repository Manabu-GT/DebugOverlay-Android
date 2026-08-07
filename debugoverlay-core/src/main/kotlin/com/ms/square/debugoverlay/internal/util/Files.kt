package com.ms.square.debugoverlay.internal.util

import com.ms.square.debugoverlay.internal.Logger
import java.io.File

/**
 * Ensures this file exists as a directory, creating it if necessary.
 * @throws IllegalStateException if the directory cannot be created and doesn't exist
 */
internal fun File.checkFolderExists() {
  check(mkdirs() || isDirectory) { "Failed to create a folder: $absolutePath" }
}

/**
 * Checks whether this file/folder is a direct child of [dir], resolving canonical paths
 * to guard against symlinks and ".." traversal.
 *
 * Intended as a safety check before destructive operations (e.g. delete) on a
 * caller-supplied path, to make sure it can't escape the directory it's expected to
 * live in.
 */
internal fun File.isDirectChildOf(dir: File): Boolean = runCatching {
  canonicalFile.parentFile == dir.canonicalFile
}.getOrElse { e ->
  Logger.w("Failed to resolve canonical path for safety check: ${e.javaClass.simpleName} - ${e.message}")
  false
}
