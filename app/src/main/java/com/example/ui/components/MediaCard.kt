package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.MediaItem

@Composable
fun MediaPosterCard(
  media: MediaItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .width(140.dp)
      .height(210.dp)
      .clickable { onClick() },
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
          .data(media.poster)
          .crossfade(true)
          .build(),
        contentDescription = media.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )

      // Gradient shadow at bottom
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(90.dp)
          .align(Alignment.BottomCenter)
          .background(
            Brush.verticalGradient(
              colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
            )
          )
      )

      // Series or Movie Badge
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (media.isSeries) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.7f),
        modifier = Modifier
          .padding(8.dp)
          .align(Alignment.TopStart)
      ) {
        Text(
          text = if (media.isSeries) "SERIES" else "MOVIE",
          color = Color.White,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
      }

      // Title & Year info at bottom
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(8.dp)
      ) {
        Text(
          text = media.title,
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFB800),
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "${media.rating}",
            color = Color.LightGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${media.year}",
            color = Color.LightGray,
            fontSize = 10.sp
          )
        }
      }
    }
  }
}

@Composable
fun RecentlyPlayedCard(
  history: WatchHistoryEntity,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val progress = if (history.durationMs > 0) {
    history.lastPositionMs.toFloat() / history.durationMs.toFloat()
  } else 0.35f

  Card(
    modifier = modifier
      .width(220.dp)
      .height(130.dp)
      .clickable { onClick() },
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
          .data(history.poster)
          .crossfade(true)
          .build(),
        contentDescription = history.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )

      // Dark overlay
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.45f))
      )

      // Play icon circle
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        modifier = Modifier.align(Alignment.Center)
      ) {
        Icon(
          Icons.Default.PlayArrow,
          contentDescription = "Resume",
          tint = Color.White,
          modifier = Modifier.padding(8.dp).size(20.dp)
        )
      }

      // Title & Progress Bar
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .background(Color.Black.copy(alpha = 0.75f))
          .padding(horizontal = 8.dp, vertical = 6.dp)
      ) {
        Text(
          text = history.title,
          color = Color.White,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (history.episodeTitle.isNotBlank()) {
          Text(
            text = history.episodeTitle,
            color = Color.LightGray,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
          progress = { progress.coerceIn(0f, 1f) },
          color = MaterialTheme.colorScheme.primary,
          trackColor = Color.DarkGray,
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
        )
      }
    }
  }
}
