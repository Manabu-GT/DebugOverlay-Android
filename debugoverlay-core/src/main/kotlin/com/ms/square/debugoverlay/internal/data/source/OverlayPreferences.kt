package com.ms.square.debugoverlay.internal.data.source

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persistence layer for debug overlay UI state.
 */
internal interface OverlayPreferences {
  /**
   * Get the last saved X position of the overlay panel.
   * @param default Default value if not previously saved
   * @return X coordinate (gravity END, so 0 = right edge)
   */
  fun getOverlayX(default: Int = 0): Int

  /**
   * Get the last saved Y position of the overlay panel.
   * @param default Default value if not previously saved
   * @return Y coordinate (gravity TOP, so 0 = top edge)
   */
  fun getOverlayY(default: Int = 0): Int

  /**
   * Save the overlay panel position.
   * Write should be asynchronous and should not block the calling thread.
   *
   * @param x X coordinate (gravity END)
   * @param y Y coordinate (gravity TOP)
   */
  fun saveOverlayPosition(x: Int, y: Int)
}

private const val PREFS_NAME = "debugoverlay_prefs"
private const val KEY_OVERLAY_X = "overlay_position_x"
private const val KEY_OVERLAY_Y = "overlay_position_y"

/**
 * SharedPreferences-based implementation of [OverlayPreferences].
 *
 * SharedPreferences reads are cached in memory after first load, so
 * subsequent reads are fast and safe on the main thread.
 */
internal class SharedPreferencesOverlayPreferences(context: Context) : OverlayPreferences {

  private val prefs: SharedPreferences = context.getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
  )

  override fun getOverlayX(default: Int): Int = prefs.getInt(KEY_OVERLAY_X, default)

  override fun getOverlayY(default: Int): Int = prefs.getInt(KEY_OVERLAY_Y, default)

  override fun saveOverlayPosition(x: Int, y: Int) {
    prefs.edit {
      putInt(KEY_OVERLAY_X, x)
      putInt(KEY_OVERLAY_Y, y)
    }
  }
}
