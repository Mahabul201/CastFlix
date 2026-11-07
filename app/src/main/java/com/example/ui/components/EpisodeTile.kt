package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Episode

/** How many episode tiles fit in one row. */
const val EPISODES_PER_ROW = 6

/**
 * One square episode tile: just the number (01, 02, 03 ...).
 * The playing / selected episode gets a teal-green gradient and glowing number. No premium badge.
 */
@Composable
fun EpisodeTile(
  number: Int,
  selected: Boolean,
  modifier: Modifier = Modifier,
  selectionMode: Boolean = false,
  checked: Boolean = false,
  downloaded: Boolean = false,
  onDark: Boolean = false,
  onClick: () -> Unit
) {
  val shape = RoundedCornerShape(12.dp)
  val highlight = if (selectionMode) checked else selected
  val idleFill = if (onDark) Color.White.copy(alpha = 0.10f)
  else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
  val label = "%02d".format(number)

  Box(
    modifier = modifier
      .aspectRatio(0.9f)
      .clip(shape)
      .background(
        if (highlight) Brush.linearGradient(listOf(Color(0xFF1B4F63), Color(0xFF1F4D2F)))
        else Brush.linearGradient(listOf(idleFill, idleFill))
      )
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    if (highlight) {
      Text(
        text = label,
        maxLines = 1,
        softWrap = false,
        style = TextStyle(
          brush = Brush.horizontalGradient(listOf(Color(0xFF35D6E8), Color(0xFF3DE08A))),
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
      )
    } else {
      Text(
        text = label,
        maxLines = 1,
        softWrap = false,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = if (onDark) Color.White.copy(alpha = 0.85f)
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
      )
    }
    if (selectionMode) {
      Icon(
        imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
        contentDescription = null,
        tint = if (checked) Color(0xFF3DE08A) else Color.Gray,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(4.dp)
          .size(14.dp)
      )
    }
    if (downloaded) {
      Icon(
        imageVector = Icons.Default.DownloadDone,
        contentDescription = null,
        tint = Color(0xFF3DE08A),
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(4.dp)
          .size(12.dp)
      )
    }
  }
}

/** One row of up to [EPISODES_PER_ROW] tiles. Short last rows are padded so tiles keep the same size. */
@Composable
fun EpisodeTileRow(
  episodes: List<Episode>,
  currentEpisodeId: String?,
  modifier: Modifier = Modifier,
  selectionMode: Boolean = false,
  selectedIds: Set<String> = emptySet(),
  downloadedIds: Set<String> = emptySet(),
  onDark: Boolean = false,
  onClick: (Episode) -> Unit
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    episodes.forEach { ep ->
      EpisodeTile(
        number = ep.episodeNumber,
        selected = ep.id == currentEpisodeId,
        modifier = Modifier.weight(1f),
        selectionMode = selectionMode,
        checked = ep.id in selectedIds,
        downloaded = ep.id in downloadedIds,
        onDark = onDark,
        onClick = { onClick(ep) }
      )
    }
    repeat((EPISODES_PER_ROW - episodes.size).coerceAtLeast(0)) {
      Spacer(modifier = Modifier.weight(1f))
    }
  }
}
