package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = HighDensityContainerSecondary,
    secondary = HighDensityContainerTertiary,
    tertiary = HighDensityTrack,
    background = HighDensityTextDark,
    surface = Color(0xFF2B2930),
    onPrimary = HighDensityOnContainer,
    onSecondary = HighDensityOnContainerTertiary,
    onBackground = HighDensityBackground,
    onSurface = HighDensityBackground,
    outline = HighDensityOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = HighDensityPrimary,
    onPrimary = HighDensityOnPrimary,
    secondary = HighDensityPrimary,
    background = HighDensityBackground,
    surface = HighDensityContainer,
    onBackground = HighDensityTextDark,
    onSurface = HighDensityTextDark,
    surfaceVariant = HighDensityContainerTertiary,
    onSurfaceVariant = HighDensityOnContainerTertiary,
    outline = HighDensityOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamicColor by default to guarantee the High Density theme colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
