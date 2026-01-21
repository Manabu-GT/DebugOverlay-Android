package com.ms.square.debugoverlay.sample.ui.overlaytests

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Shows a standard Android AlertDialog using AlertDialog.Builder.
 * Tests overlay z-order with view-based alert dialogs.
 */
internal fun showAlertDialog(context: Context) {
  AlertDialog.Builder(context)
    .setTitle("View AlertDialog")
    .setMessage("This is a standard Android AlertDialog.\n\nThe debug overlay should appear above this dialog.")
    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
    .show()
}

/**
 * Shows a BottomSheetDialog with simple content.
 * Tests overlay z-order with view-based bottom sheets.
 */
@Suppress("MagicNumber")
internal fun showBottomSheetDialog(context: Context) {
  val dialog = BottomSheetDialog(context)

  val content = FrameLayout(context).apply {
    val padding = (24 * context.resources.displayMetrics.density).toInt()
    setPadding(padding, padding, padding, padding)

    addView(
      TextView(context).apply {
        text = "View BottomSheetDialog\n\n" +
          "This is a standard Android BottomSheetDialog.\n\n" +
          "The debug overlay should appear above this bottom sheet."
        textSize = 16f
        gravity = Gravity.CENTER
      }
    )

    minimumHeight = (200 * resources.displayMetrics.density).toInt()
  }

  dialog.setContentView(content)
  dialog.show()
}

/**
 * Shows a DialogFragment with Compose content.
 * Tests overlay z-order with fragment-based dialogs.
 */
internal fun showDialogFragment(fragmentManager: FragmentManager) {
  CustomDialogFragment().show(fragmentManager, "CustomDialogFragment")
}
