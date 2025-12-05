package com.ms.square.debugoverlay.internal.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ms.square.debugoverlay.core.R

@Composable
internal fun BackButton(onClick: () -> Unit) {
  IconButton(onClick = onClick) {
    Icon(
      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
      contentDescription = stringResource(R.string.debugoverlay_back)
    )
  }
}
