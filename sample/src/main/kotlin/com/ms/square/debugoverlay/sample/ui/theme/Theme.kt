package com.ms.square.debugoverlay.sample.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
  primary = androidx.compose.ui.graphics.Color(0xFF2196F3),
  onPrimary = androidx.compose.ui.graphics.Color.White,
  primaryContainer = androidx.compose.ui.graphics.Color(0xFFBBDEFB),
  onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF0D47A1),
  secondary = androidx.compose.ui.graphics.Color(0xFF03A9F4),
  onSecondary = androidx.compose.ui.graphics.Color.White,
  secondaryContainer = androidx.compose.ui.graphics.Color(0xFFB3E5FC),
  onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF01579B),
  tertiary = androidx.compose.ui.graphics.Color(0xFF00BCD4),
  onTertiary = androidx.compose.ui.graphics.Color.White,
  error = androidx.compose.ui.graphics.Color(0xFFB00020),
  onError = androidx.compose.ui.graphics.Color.White,
  background = androidx.compose.ui.graphics.Color(0xFFFAFAFA),
  onBackground = androidx.compose.ui.graphics.Color(0xFF212121),
  surface = androidx.compose.ui.graphics.Color.White,
  onSurface = androidx.compose.ui.graphics.Color(0xFF212121),
  surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
  onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF757575)
)

private val DarkColorScheme = darkColorScheme(
  primary = androidx.compose.ui.graphics.Color(0xFF64B5F6),
  onPrimary = androidx.compose.ui.graphics.Color(0xFF0D47A1),
  primaryContainer = androidx.compose.ui.graphics.Color(0xFF1976D2),
  onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE3F2FD),
  secondary = androidx.compose.ui.graphics.Color(0xFF4FC3F7),
  onSecondary = androidx.compose.ui.graphics.Color(0xFF01579B),
  secondaryContainer = androidx.compose.ui.graphics.Color(0xFF0277BD),
  onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFE1F5FE),
  tertiary = androidx.compose.ui.graphics.Color(0xFF4DD0E1),
  onTertiary = androidx.compose.ui.graphics.Color(0xFF006064),
  error = androidx.compose.ui.graphics.Color(0xFFCF6679),
  onError = androidx.compose.ui.graphics.Color(0xFF370B1E),
  background = androidx.compose.ui.graphics.Color(0xFF121212),
  onBackground = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
  surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
  onSurface = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
  surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2C2C2C),
  onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFBDBDBD)
)

/**
 * AppTheme for the DebugOverlay sample app, which shows the RSS feed of Android Weekly.
 * Supports light and dark themes, including dynamic colors on Android 12+.
 *
 * @param darkTheme Whether to use dark theme. Defaults to system setting.
 * @param dynamicColor Whether to use dynamic colors (Android 12+). Defaults to true.
 * @param content The composable content to theme.
 */
@Composable
fun AppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    content = content
  )
}
