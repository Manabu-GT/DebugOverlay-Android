package com.ms.square.debugoverlay.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ms.square.debugoverlay.sample.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * A secondary activity for testing overlay behavior across multiple activities.
 * Validates that the debug overlay remains visible and functions correctly when
 * navigating between activities.
 */
@AndroidEntryPoint
class SecondActivity : ComponentActivity() {

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      AppTheme {
        Scaffold(
          topBar = {
            TopAppBar(
              title = { Text("Second Activity") },
              navigationIcon = {
                IconButton(onClick = { finish() }) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                  )
                }
              }
            )
          }
        ) { padding ->
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding)
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Text(
              text = "Multiple Activities Test",
              style = MaterialTheme.typography.headlineMedium,
              textAlign = TextAlign.Center
            )
            Text(
              text = "This is a second activity.\n\n" +
                "The debug overlay should remain visible and functional " +
                "across activity transitions.\n\n" +
                "Try dragging the overlay and interacting with it.",
              style = MaterialTheme.typography.bodyLarge,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(top = 16.dp)
            )
          }
        }
      }
    }
  }

  companion object {
    fun launch(context: Context) {
      context.startActivity(Intent(context, SecondActivity::class.java))
    }
  }
}
