package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Default Cinema Dark Palette
val CinemaRed = Color(0xFFE50914)
val NeonCyan = Color(0xFF00E5FF)
val ElectricGold = Color(0xFFFFB800)
val EmeraldGreen = Color(0xFF00E676)
val RoyalPurple = Color(0xFFA855F7)

// Dark Theme Colors
val BackgroundDark = Color(0xFF0D0E15)
val SurfaceDark = Color(0xFF161824)
val SurfaceDarkVariant = Color(0xFF222538)
val OnSurfaceDark = Color(0xFFF1F5F9)
val OnSurfaceDarkVariant = Color(0xFF94A3B8)

// AMOLED True Black Colors
val BackgroundAmoled = Color(0xFF000000)
val SurfaceAmoled = Color(0xFF0A0A0E)
val SurfaceAmoledVariant = Color(0xFF14141A)

// Light Theme Colors
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceLightVariant = Color(0xFFE2E8F0)
val OnSurfaceLight = Color(0xFF0F172A)
val OnSurfaceLightVariant = Color(0xFF64748B)

// Accent Colors for User Customization
enum class AppAccent(val displayName: String, val color: Color, val hex: String) {
  CINEMA_RED("Cinema Red", CinemaRed, "#E50914"),
  NEON_CYAN("Neon Cyan", NeonCyan, "#00E5FF"),
  ELECTRIC_GOLD("Electric Gold", ElectricGold, "#FFB800"),
  EMERALD("Emerald Green", EmeraldGreen, "#00E676"),
  ROYAL_PURPLE("Royal Purple", RoyalPurple, "#A855F7")
}

enum class AppThemeMode(val displayName: String) {
  DARK("Dark (Cinema)"),
  AMOLED("AMOLED (Deep Black)"),
  LIGHT("Light")
}
