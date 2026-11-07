package com.example.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Thin coloured seek bar (3dp track, small thumb) used by the watch page and the fullscreen player.
 * The played part is a gradient of [accent], the rest is a soft white track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactSlider(
  value: Float,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  onValueChangeFinished: (() -> Unit)? = null,
  enabled: Boolean = true,
  accent: Color
) {
  val v = value.coerceIn(0f, 1f)
  Slider(
    value = v,
    onValueChange = onValueChange,
    onValueChangeFinished = onValueChangeFinished,
    enabled = enabled,
    modifier = modifier.height(20.dp),
    thumb = {
      Box(
        Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(if (enabled) Color.White else accent.copy(alpha = 0.6f))
      )
    },
    track = {
      Box(
        Modifier
          .fillMaxWidth()
          .height(3.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(Color.White.copy(alpha = 0.28f)),
        contentAlignment = Alignment.CenterStart
      ) {
        Box(
          Modifier
            .fillMaxWidth(v)
            .height(3.dp)
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.75f), accent)))
        )
      }
    }
  )
}
