package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun URLStreamTheme(
  themeMode: AppThemeMode = AppThemeMode.DARK,
  accent: AppAccent = AppAccent.CINEMA_RED,
  content: @Composable () -> Unit,
) {
  val primaryColor = accent.color

  val colorScheme = when (themeMode) {
    AppThemeMode.AMOLED -> darkColorScheme(
      primary = primaryColor,
      onPrimary = Color.White,
      primaryContainer = primaryColor.copy(alpha = 0.2f),
      onPrimaryContainer = primaryColor,
      secondary = primaryColor,
      onSecondary = Color.White,
      background = BackgroundAmoled,
      onBackground = Color(0xFFF8FAFC),
      surface = SurfaceAmoled,
      onSurface = Color(0xFFF8FAFC),
      surfaceVariant = SurfaceAmoledVariant,
      onSurfaceVariant = Color(0xFF94A3B8),
      outline = Color(0xFF27273A)
    )
    AppThemeMode.LIGHT -> lightColorScheme(
      primary = primaryColor,
      onPrimary = Color.White,
      primaryContainer = primaryColor.copy(alpha = 0.15f),
      onPrimaryContainer = primaryColor,
      secondary = primaryColor,
      onSecondary = Color.White,
      background = BackgroundLight,
      onBackground = OnSurfaceLight,
      surface = SurfaceLight,
      onSurface = OnSurfaceLight,
      surfaceVariant = SurfaceLightVariant,
      onSurfaceVariant = OnSurfaceLightVariant,
      outline = Color(0xFFCBD5E1)
    )
    AppThemeMode.DARK -> darkColorScheme(
      primary = primaryColor,
      onPrimary = Color.White,
      primaryContainer = primaryColor.copy(alpha = 0.25f),
      onPrimaryContainer = Color.White,
      secondary = primaryColor,
      onSecondary = Color.White,
      background = BackgroundDark,
      onBackground = OnSurfaceDark,
      surface = SurfaceDark,
      onSurface = OnSurfaceDark,
      surfaceVariant = SurfaceDarkVariant,
      onSurfaceVariant = OnSurfaceDarkVariant,
      outline = Color(0xFF2D324D)
    )
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
