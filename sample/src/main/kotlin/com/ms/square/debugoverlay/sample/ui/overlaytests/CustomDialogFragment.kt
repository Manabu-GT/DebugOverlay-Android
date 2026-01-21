package com.ms.square.debugoverlay.sample.ui.overlaytests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.ms.square.debugoverlay.sample.ui.theme.AppTheme

/**
 * A DialogFragment that uses ComposeView for its content.
 * Tests overlay z-order with the Fragment dialog lifecycle and Compose interop.
 */
internal class CustomDialogFragment : DialogFragment() {

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
    ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AppTheme {
          Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Text(
                text = "DialogFragment",
                style = MaterialTheme.typography.headlineSmall
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "This is a DialogFragment with Compose content.\n\n" +
                  "The debug overlay should appear above this dialog.",
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(24.dp))
              TextButton(onClick = { dismiss() }) {
                Text("Dismiss")
              }
            }
          }
        }
      }
    }
}
