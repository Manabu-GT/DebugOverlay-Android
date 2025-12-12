package com.ms.square.debugoverlay.internal

/**
 * Marks declarations that are internal to DebugOverlay and should not be used
 * by external consumers. These APIs may change without notice.
 *
 * Extension modules within the DebugOverlay library group may use these APIs
 * by opting in with `@OptIn(InternalDebugOverlayApi::class)`.
 */
@RequiresOptIn(
  message = "This is internal DebugOverlay API. It may change without notice.",
  level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY
)
public annotation class InternalDebugOverlayApi
