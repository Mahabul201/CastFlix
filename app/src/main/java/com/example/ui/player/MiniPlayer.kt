package com.example.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.Episode
import com.example.data.model.MediaItem
import com.example.data.model.ResumePoint
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

/** Where the in-app picture-in-picture window hands its video over to. */
object MiniPlayerState {
  var player by mutableStateOf<ExoPlayer?>(null)
  var media by mutableStateOf<MediaItem?>(null)
  var episode by mutableStateOf<Episode?>(null)

  /** Set when the user taps "expand": MainActivity reopens the watch page from this point. */
  var expandRequest by mutableStateOf<Pair<MediaItem, ResumePoint>?>(null)

  fun start(p: ExoPlayer, m: MediaItem, e: Episode?) {
    if (player !== p) release()
    player = p
    media = m
    episode = e
  }

  fun release() {
    player?.release()
    player = null
    media = null
    episode = null
  }
}

/**
 * In-app picture-in-picture: a small draggable video window that floats above the whole app,
 * so the user can keep browsing while the video plays. The app itself stays open.
 */
@Composable
fun InAppMiniPlayer(
  onSaveProgress: (MediaItem, Long, Long, Episode?) -> Unit
) {
  val player = MiniPlayerState.player
  val media = MiniPlayerState.media
  if (player == null || media == null) return
  val episode = MiniPlayerState.episode

  var offset by remember(player) { mutableStateOf(Offset.Zero) }
  var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
  var progress by remember(player) { mutableFloatStateOf(0f) }

  DisposableEffect(player) {
    val l = object : Player.Listener {
      override fun onIsPlayingChanged(playing: Boolean) {
        isPlaying = playing
      }
    }
    player.addListener(l)
    onDispose { player.removeListener(l) }
  }

  LaunchedEffect(player) {
    var sinceSave = 0
    while (true) {
      val d = player.duration
      progress = if (d > 0L) (player.currentPosition.toFloat() / d).coerceIn(0f, 1f) else 0f
      delay(500)
      sinceSave++
      if (sinceSave >= 20) {
        sinceSave = 0
        val pos = player.currentPosition
        if (player.isPlaying && pos > 3000L) onSaveProgress(media, pos, max(0L, player.duration), episode)
      }
    }
  }

  fun close() {
    val pos = player.currentPosition
    if (pos > 3000L) onSaveProgress(media, pos, max(0L, player.duration), episode)
    MiniPlayerState.release()
  }

  fun expand() {
    val pos = max(0L, player.currentPosition)
    val dur = max(0L, player.duration)
    MiniPlayerState.expandRequest = media to ResumePoint(
      mediaId = media.id,
      season = episode?.season,
      episodeNumber = episode?.episodeNumber,
      positionMs = pos,
      durationMs = dur
    )
    MiniPlayerState.release()
  }

  Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
    Box(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 12.dp, bottom = 96.dp)
        .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
        .size(width = 210.dp, height = 118.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color.Black)
        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        .pointerInput(Unit) {
          detectDragGestures { change, drag ->
            change.consume()
            offset += drag
          }
        }
    ) {
      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
          PlayerView(ctx).apply {
            this.player = player
            useController = false
          }
        }
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clickable { if (player.isPlaying) player.pause() else player.play() }
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.Black.copy(alpha = 0.35f))
          .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Default.OpenInFull, "Expand", tint = Color.White,
          modifier = Modifier.padding(6.dp).size(18.dp).clickable { expand() }
        )
        Box(modifier = Modifier.weight(1f))
        Icon(
          Icons.Default.Close, "Close", tint = Color.White,
          modifier = Modifier.padding(6.dp).size(18.dp).clickable { close() }
        )
      }
      if (!isPlaying) {
        Icon(
          Icons.Default.PlayArrow, "Play", tint = Color.White,
          modifier = Modifier
            .align(Alignment.Center)
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(6.dp)
        )
      }
      Box(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .fillMaxWidth(progress)
          .height(2.dp)
          .background(MaterialTheme.colorScheme.primary)
      )
    }
  }
}
