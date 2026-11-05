package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Frosted-glass look: soft translucent white fill + a thin light edge that is
 * brighter at the top-left. Works on every Android version (no blur needed).
 */
fun Modifier.glass(
  shape: Shape,
  fill: Float = 0.14f,
  border: Float = 0.30f
): Modifier = this
  .clip(shape)
  .background(
    Brush.linearGradient(
      listOf(Color.White.copy(alpha = fill + 0.06f), Color.White.copy(alpha = fill * 0.45f))
    )
  )
  .border(
    width = 1.dp,
    brush = Brush.linearGradient(
      listOf(Color.White.copy(alpha = border), Color.White.copy(alpha = border * 0.15f))
    ),
    shape = shape
  )
