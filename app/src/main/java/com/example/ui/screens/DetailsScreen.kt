package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.data.model.Episode
import com.example.data.model.MediaItem
import com.example.data.repository.DownloadManagerHelper
import com.example.data.repository.StreamRepository
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(
  media: MediaItem,
  repository: StreamRepository,
  downloadHelper: DownloadManagerHelper,
  onBack: () -> Unit,
  onPlay: (MediaItem, Episode?, Long) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var current by remember(media.id) { mutableStateOf(media) }
  var episodes by remember(media.id) { mutableStateOf(media.episodes) }

  LaunchedEffect(media.id) {
    val full = repository.getMediaById(media.id)
    if (full != null) current = full
    if (current.isSeries) {
      val loaded = repository.getEpisodes(current)
      if (loaded.isNotEmpty()) episodes = loaded
    }
  }

  val inWatchlist by repository.isWatchlistFlow(media.id).collectAsState(initial = false)
  val isFavorite by repository.isFavoriteFlow(media.id).collectAsState(initial = false)
  val downloads by downloadHelper.allDownloads.collectAsState(initial = emptyList())
  val history by repository.watchHistory.collectAsState(initial = emptyList())

  val lastWatched = history.firstOrNull { it.mediaId == media.id }
  val resumePos = if (lastWatched != null && lastWatched.lastPositionMs > 5000L) lastWatched.lastPositionMs else 0L
  val resumeEpisode = if (lastWatched != null) {
    episodes.firstOrNull { it.season == lastWatched.season && it.episodeNumber == lastWatched.episodeNumber }
  } else {
    null
  }

  val playable = current.copy(episodes = episodes)

  fun startDownloadFor(target: MediaItem, url: String) {
    if (url.isBlank()) {
      Toast.makeText(context, "No video link available", Toast.LENGTH_SHORT).show()
      return
    }
    if (url.contains(".m3u8", ignoreCase = true)) {
      Toast.makeText(context, "This is a live/HLS stream and cannot be downloaded", Toast.LENGTH_LONG).show()
      return
    }
    scope.launch {
      downloadHelper.startDownload(target, url)
      Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    }
  }

  val mainDownload = downloads.firstOrNull { it.mediaId == media.id }
  val downloadLabel = when {
    mainDownload == null -> "Download"
    mainDownload.status == "COMPLETED" -> "Downloaded"
    mainDownload.status == "DOWNLOADING" -> "Downloading ${mainDownload.progressPercent}%"
    mainDownload.status == "PAUSED" -> "Paused"
    else -> "Retry download"
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
    ) {
      // Hero image
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
        ) {
          AsyncImage(
            model = ImageRequest.Builder(context)
              .data(current.banner.ifBlank { current.poster })
              .crossfade(true)
              .build(),
            contentDescription = current.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    Color.Black.copy(alpha = 0.35f),
                    Color.Transparent,
                    MaterialTheme.colorScheme.background
                  )
                )
              )
          )
        }
      }

      // Title and info
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          Text(
            text = current.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Star,
              contentDescription = null,
              tint = Color(0xFFFFC107),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${current.rating} • ${current.year} • ${current.category} • ${if (current.isSeries) "Series" else current.duration}",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.height(16.dp))

          // Play + Download
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = {
                val ep = if (current.isSeries) (resumeEpisode ?: episodes.firstOrNull()) else null
                onPlay(playable, ep, resumePos)
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(24.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (resumePos > 0L) "Resume" else "Play",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
              )
            }
            if (!current.isSeries) {
              OutlinedButton(
                onClick = {
                  if (mainDownload == null || mainDownload.status == "FAILED") {
                    startDownloadFor(current, current.streamUrl)
                  } else if (mainDownload.status == "COMPLETED") {
                    Toast.makeText(context, "Already downloaded. Open the Download tab to play it.", Toast.LENGTH_SHORT).show()
                  } else {
                    Toast.makeText(context, "See the Download tab for progress", Toast.LENGTH_SHORT).show()
                  }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
              ) {
                Icon(
                  if (mainDownload?.status == "COMPLETED") Icons.Default.DownloadDone else Icons.Default.Download,
                  contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(downloadLabel, maxLines = 1, softWrap = false, fontSize = 13.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Watchlist + Favorite
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
              onClick = { scope.launch { repository.toggleWatchlist(current, inWatchlist) } },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(24.dp)
            ) {
              Icon(
                if (inWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = null
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (inWatchlist) "In Watchlist" else "Watchlist",
                maxLines = 1,
                softWrap = false,
                fontSize = 13.sp
              )
            }
            OutlinedButton(
              onClick = { scope.launch { repository.toggleFavorite(current) } },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(24.dp)
            ) {
              Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) Color(0xFFE50914) else MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isFavorite) "Favorite" else "Add Favorite",
                maxLines = 1,
                softWrap = false,
                fontSize = 13.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = current.description,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(20.dp))
        }
      }

      // Episodes (series only)
      if (current.isSeries) {
        item {
          Text(
            text = "Episodes (${episodes.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
          )
          if (episodes.isEmpty()) {
            Text(
              text = "No episodes added yet.",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 16.dp)
            )
          }
        }
        items(episodes) { ep ->
          val epDownload = downloads.firstOrNull { it.mediaId == ep.id }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onPlay(playable, ep, 0L) }
              .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(ep.thumbnail.ifBlank { current.poster })
                .crossfade(true)
                .build(),
              contentDescription = ep.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .width(120.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "S${ep.season} E${ep.episodeNumber}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = ep.title,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = ep.duration,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            IconButton(
              onClick = {
                if (epDownload == null || epDownload.status == "FAILED") {
                  startDownloadFor(current.copy(id = ep.id, title = "${current.title} - ${ep.title}"), ep.streamUrl)
                } else {
                  Toast.makeText(context, "See the Download tab", Toast.LENGTH_SHORT).show()
                }
              }
            ) {
              Icon(
                if (epDownload?.status == "COMPLETED") Icons.Default.DownloadDone else Icons.Default.Download,
                contentDescription = "Download episode"
              )
            }
          }
        }
      }
    }

    // Back button
    IconButton(
      onClick = onBack,
      modifier = Modifier
        .statusBarsPadding()
        .padding(8.dp)
        .align(Alignment.TopStart)
        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
    ) {
      Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
    }
  }
}
