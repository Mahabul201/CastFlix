package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.Episode
import com.example.data.model.MediaItem
import com.example.data.repository.StreamRepository
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
  repository: StreamRepository,
  onBack: () -> Unit,
  onPlay: (MediaItem, Episode?, Long) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val history by repository.watchHistory.collectAsState(initial = emptyList())
  var showClearDialog by remember { mutableStateOf(false) }

  fun resume(h: WatchHistoryEntity) {
    scope.launch {
      val known = repository.getMediaById(h.mediaId)
      val media = known ?: MediaItem(
        id = h.mediaId,
        title = h.title,
        description = "",
        poster = h.poster,
        banner = h.poster,
        category = "",
        year = 2025,
        rating = 0.0,
        streamUrl = h.streamUrl,
        isSeries = h.isSeries
      )
      val episode = if (h.isSeries) {
        media.episodes.firstOrNull { it.season == h.season && it.episodeNumber == h.episodeNumber }
      } else {
        null
      }
      onPlay(media, episode, h.lastPositionMs)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
      }
      Text(
        text = "History",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.weight(1f)
      )
      if (history.isNotEmpty()) {
        TextButton(onClick = { showClearDialog = true }) {
          Text("Clear all", color = MaterialTheme.colorScheme.primary)
        }
      }
    }

    if (history.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = "Nothing watched yet",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 15.sp
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(history, key = { it.id }) { h ->
          val fraction = if (h.durationMs > 0L) {
            (h.lastPositionMs.toFloat() / h.durationMs.toFloat()).coerceIn(0f, 1f)
          } else {
            0f
          }
          val subtitle = if (h.isSeries) {
            "S${h.season} E${h.episodeNumber}" + if (h.episodeTitle.isNotBlank()) " • ${h.episodeTitle}" else ""
          } else {
            "Movie"
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .clickable { resume(h) }
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(h.poster)
                .crossfade(true)
                .build(),
              contentDescription = h.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .width(110.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = h.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = subtitle,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = android.text.format.DateUtils.getRelativeTimeSpanString(h.updatedAt).toString(),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(6.dp))
              LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(3.dp),
                color = MaterialTheme.colorScheme.primary
              )
            }
            IconButton(onClick = { scope.launch { repository.deleteHistoryItem(h.id) } }) {
              Icon(Icons.Default.Close, contentDescription = "Remove")
            }
          }
        }
      }
    }
  }

  if (showClearDialog) {
    AlertDialog(
      onDismissRequest = { showClearDialog = false },
      title = { Text("Clear history?") },
      text = { Text("All watch history and resume positions will be removed.") },
      confirmButton = {
        Button(
          onClick = {
            scope.launch { repository.clearHistory() }
            showClearDialog = false
          }
        ) {
          Text("Clear")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
