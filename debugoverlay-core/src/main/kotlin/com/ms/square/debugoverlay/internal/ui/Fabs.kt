package com.ms.square.debugoverlay.internal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ms.square.debugoverlay.core.R

@Composable
internal fun ScrollToBottomFab(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
  AnimatedVisibility(
    visible = visible,
    modifier = modifier,
    enter = fadeIn() + scaleIn(),
    exit = fadeOut() + scaleOut()
  ) {
    FloatingActionButton(
      onClick = onClick,
      containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
      Icon(
        imageVector = Icons.Default.ArrowDownward,
        contentDescription = stringResource(R.string.debugoverlay_scroll_to_bottom)
      )
    }
  }
}
