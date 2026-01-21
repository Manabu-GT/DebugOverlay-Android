package com.ms.square.debugoverlay.sample.ui.overlaytests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Compose AlertDialog for testing overlay z-order.
 *
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
internal fun ComposeAlertDialog(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Compose Dialog") },
    text = {
      Text("This is a Compose AlertDialog.\n\nThe debug overlay should appear above this dialog.")
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("OK")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

/**
 * Compose ModalBottomSheet for testing overlay z-order.
 *
 * @param onDismiss Callback when the bottom sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeModalBottomSheet(onDismiss: () -> Unit) {
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = "Compose ModalBottomSheet",
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "This is a Compose ModalBottomSheet.\n\nThe debug overlay should appear above this bottom sheet.",
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(32.dp))
      TextButton(onClick = onDismiss) {
        Text("Dismiss")
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
