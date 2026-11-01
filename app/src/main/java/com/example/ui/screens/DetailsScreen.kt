package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import com.example.ui.ads.Ads
import com.example.ui.player.StreamtapeEmbedPlayer
import com.example.ui.player.toStreamtapeEmbed
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Episode
import com.example.data.model.MediaItem
import com.example.data.model.ResumePoint
import com.example.data.model.VideoQuality
import com.example.data.model.buildQualityOptions
import com.example.data.model.hasQualityChoice
import com.example.data.model.pickQuality
import com.example.ui.components.QualityPickerDialog
import com.example.ui.theme.Dimens
import com.example.data.repository.DownloadManagerHelper
import com.example.data.repository.StreamRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Watch page: opens directly when a thumbnail is tapped.
 * Player on top (auto plays), info + action buttons + resources + "For you" below.
 */
@Composable
fun DetailsScreen(
  media: MediaItem,
  repository: StreamRepository,
  downloadHelper: DownloadManagerHelper,
  onBack: () -> Unit,
  onPlay: (MediaItem, Episode?, Long) -> Unit,
  onOpenMedia: (MediaItem) -> Unit = {},
  onSaveProgress: (MediaItem, Long, Long, Episode?) -> Unit = { _, _, _, _ -> },
  resume: ResumePoint? = null
) {
  // key() makes a fresh player + state whenever another thumbnail is opened from "For you"
  key(media.id) {
    WatchPage(
      media = media,
      repository = repository,
      downloadHelper = downloadHelper,
      onBack = onBack,
      onPlay = onPlay,
      onOpenMedia = onOpenMedia,
      onSaveProgress = onSaveProgress,
      resume = resume
    )
  }
}

@Composable
private fun WatchPage(
  media: MediaItem,
  repository: StreamRepository,
  downloadHelper: DownloadManagerHelper,
  onBack: () -> Unit,
  onPlay: (MediaItem, Episode?, Long) -> Unit,
  onOpenMedia: (MediaItem) -> Unit,
  onSaveProgress: (MediaItem, Long, Long, Episode?) -> Unit,
  resume: ResumePoint?
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var current by remember { mutableStateOf(media) }
  var episodes by remember { mutableStateOf(media.episodes) }
  var currentEpisode by remember { mutableStateOf<Episode?>(null) }
  var startPositionMs by remember { mutableStateOf<Long?>(null) }
  var showInfo by remember { mutableStateOf(false) }

  // Load full media, episodes and resume point
  LaunchedEffect(Unit) {
    val full = repository.getMediaById(media.id)
    if (full != null) current = full
    var eps = current.episodes
    if (current.isSeries) {
      val loaded = repository.getEpisodes(current)
      if (loaded.isNotEmpty()) eps = loaded
    }
    episodes = eps
    // Resume point: an explicit one (history tap / back from fullscreen) wins over the database
    val explicit = resume?.takeIf { it.mediaId == media.id }
    val saved = if (explicit == null) repository.getLastProgress(media.id) else null
    val lastSeason = explicit?.season ?: saved?.season
    val lastEpisodeNo = explicit?.episodeNumber ?: saved?.episodeNumber
    val lastDur = explicit?.durationMs ?: saved?.durationMs ?: 0L
    val rawPos = explicit?.positionMs ?: saved?.lastPositionMs ?: 0L
    val hasLast = explicit != null || saved != null
    // Almost finished -> start over (or move to the next episode below)
    val finished = lastDur > 0L && rawPos >= (lastDur * 0.95).toLong()
    val lastPos = if (rawPos <= 5000L || finished) 0L else rawPos

    if (current.isSeries) {
      val lastEp = if (hasLast) {
        eps.firstOrNull { it.season == lastSeason && it.episodeNumber == lastEpisodeNo }
      } else {
        null
      }
      val nextEp = if (lastEp != null && finished) {
        eps.firstOrNull { it.season == lastEp.season && it.episodeNumber == lastEp.episodeNumber + 1 }
      } else {
        null
      }
      val target = nextEp ?: lastEp ?: eps.firstOrNull()
      currentEpisode = target
      // Only continue mid-episode when we are still on the episode that was being watched
      startPositionMs = if (target != null && target == lastEp) lastPos else 0L
    } else {
      startPositionMs = lastPos
    }
  }

  val inWatchlist by repository.isWatchlistFlow(media.id).collectAsState(initial = false)
  val downloads by downloadHelper.allDownloads.collectAsState(initial = emptyList())
  val allMovies by repository.movies.collectAsState()
  val allSeries by repository.series.collectAsState()

  val forYou = remember(allMovies, allSeries, media.id) {
    (allMovies + allSeries).filter { it.id != media.id }.shuffled()
  }

  // ---- Quality (480p / 720p / 1080p) ----
  var selectedQuality by remember { mutableStateOf(repository.getPreferredQuality()) }
  var showQualityDialog by remember { mutableStateOf(false) }
  var showDownloadQualityDialog by remember { mutableStateOf(false) }

  val qualityOptions = remember(current, currentEpisode) {
    val ep = currentEpisode
    if (current.isSeries) {
      buildQualityOptions(ep?.qualities.orEmpty(), ep?.streamUrl.orEmpty())
    } else {
      buildQualityOptions(current.qualities, current.streamUrl)
    }
  }
  val activeQuality = pickQuality(qualityOptions, selectedQuality)
  val streamUrl = activeQuality?.url.orEmpty()
  val playable = current.copy(episodes = episodes)
  val appSettings by repository.settings.collectAsState()
  val embedUrl = if (appSettings.streamtapeAds) toStreamtapeEmbed(streamUrl) else null

  // ---- Player ----
  val exoPlayer = remember {
    ExoPlayer.Builder(context).build().apply {
      playWhenReady = true
    }
  }
  // Interstitial ad when a video is opened (frequency-capped; skipped in Streamtape ad mode)
  LaunchedEffect(Unit) {
    val activity = context as? android.app.Activity
    if (activity != null && embedUrl == null) {
      Ads.maybeShowInterstitial(
        activity,
        onShown = { exoPlayer.pause() },
        onClosed = { if (embedUrl == null) exoPlayer.play() }
      )
    }
  }
  var isPlaying by remember { mutableStateOf(true) }
  var isBuffering by remember { mutableStateOf(true) }
  var errorText by remember { mutableStateOf<String?>(null) }
  var position by remember { mutableLongStateOf(0L) }
  var duration by remember { mutableLongStateOf(0L) }
  var seeking by remember { mutableStateOf(false) }
  var seekValue by remember { mutableStateOf(0f) }
  val latestMedia by rememberUpdatedState(playable)
  val latestEpisode by rememberUpdatedState(currentEpisode)
  var loadedUrl by remember { mutableStateOf("") }
  var loadedKey by remember { mutableStateOf("") }
  var resumeApplied by remember { mutableStateOf(false) }

  LaunchedEffect(streamUrl, startPositionMs, embedUrl) {
    val start = startPositionMs
    // In Streamtape ad mode the raw video is never loaded (that would skip their ads)
    if (embedUrl == null && start != null && streamUrl.isNotBlank() && streamUrl != loadedUrl) {
      val contentKey = currentEpisode?.id ?: current.id
      val seekTo = when {
        // Same title, different quality: keep the current position
        loadedKey == contentKey && loadedUrl.isNotBlank() -> max(0L, exoPlayer.currentPosition)
        // First load: continue where the user stopped
        !resumeApplied -> start
        // New episode
        else -> 0L
      }
      loadedUrl = streamUrl
      loadedKey = contentKey
      errorText = null
      exoPlayer.setMediaItem(Media3Item.fromUri(Uri.parse(streamUrl)), seekTo)
      exoPlayer.prepare()
      resumeApplied = true
      exoPlayer.play()
    }
  }

  // Save progress every 10s while playing, so nothing is lost if the app is closed
  LaunchedEffect(exoPlayer) {
    while (true) {
      delay(10_000)
      val pos = exoPlayer.currentPosition
      if (exoPlayer.isPlaying && pos > 3000L) {
        onSaveProgress(latestMedia, pos, max(0L, exoPlayer.duration), latestEpisode)
      }
    }
  }

  LaunchedEffect(exoPlayer) {
    while (true) {
      if (!seeking) {
        position = max(0L, exoPlayer.currentPosition)
      }
      val d = exoPlayer.duration
      duration = if (d > 0L) d else 0L
      delay(500)
    }
  }

  DisposableEffect(Unit) {
    val listener = object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        isBuffering = playbackState == Player.STATE_BUFFERING
        if (playbackState == Player.STATE_ENDED) {
          val ep = latestEpisode
          if (ep != null) {
            val next = latestMedia.episodes.firstOrNull {
              it.season == ep.season && it.episodeNumber == ep.episodeNumber + 1
            }
            if (next != null) currentEpisode = next
          }
        }
      }

      override fun onIsPlayingChanged(playing: Boolean) {
        isPlaying = playing
      }

      override fun onPlayerError(error: PlaybackException) {
        errorText = "Video play nahi ho raha. Link check karo."
        isBuffering = false
      }
    }
    exoPlayer.addListener(listener)
    val activity = context as? android.app.Activity
    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    onDispose {
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      val pos = exoPlayer.currentPosition
      if (pos > 3000L) {
        onSaveProgress(latestMedia, pos, max(0L, exoPlayer.duration), latestEpisode)
      }
      exoPlayer.removeListener(listener)
      exoPlayer.release()
    }
  }

  // ---- Download / share helpers ----
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

  val downloadKey = if (current.isSeries) currentEpisode?.id ?: current.id else media.id
  val activeDownload = downloads.firstOrNull { it.mediaId == downloadKey }
  val downloadLabel = when {
    activeDownload == null -> "Download"
    activeDownload.status == "COMPLETED" -> "Downloaded"
    activeDownload.status == "DOWNLOADING" -> "${activeDownload.progressPercent}%"
    activeDownload.status == "PAUSED" -> "Paused"
    else -> "Retry"
  }

  fun downloadWithQuality(option: VideoQuality?) {
    val ep = currentEpisode
    val label = option?.label?.takeIf { hasQualityChoice(qualityOptions) }
    val url = option?.url ?: streamUrl
    val suffix = if (label != null) " ($label)" else ""
    if (current.isSeries && ep != null) {
      val epName = ep.fileName.ifBlank { "${current.title} - ${ep.title}" }
      startDownloadFor(current.copy(id = ep.id, title = "$epName$suffix"), url)
    } else {
      startDownloadFor(current.copy(title = "${current.fileName.ifBlank { current.title }}$suffix"), url)
    }
  }

  fun onDownloadClick() {
    if (activeDownload == null || activeDownload.status == "FAILED") {
      if (hasQualityChoice(qualityOptions)) {
        showDownloadQualityDialog = true
      } else {
        downloadWithQuality(qualityOptions.firstOrNull())
      }
    } else if (activeDownload.status == "COMPLETED") {
      Toast.makeText(context, "Already downloaded. Open the Download tab to play it.", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "See the Download tab for progress", Toast.LENGTH_SHORT).show()
    }
  }

  fun shareMedia() {
    com.example.util.ShareUtils.shareMedia(context, current)
  }

  // ---- UI ----
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Player area
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color.Black)
        .statusBarsPadding()
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f)
      ) {
        if (embedUrl != null) {
          LaunchedEffect(embedUrl) { exoPlayer.pause() }
          StreamtapeEmbedPlayer(url = embedUrl, modifier = Modifier.fillMaxSize())
        }
        AndroidView(
          modifier = if (embedUrl != null) Modifier.size(0.dp) else Modifier.fillMaxSize(),
          factory = { ctx ->
            PlayerView(ctx).apply {
              player = exoPlayer
              useController = false
              layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
              )
            }
          }
        )

        // Back button
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(4.dp)
        ) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        if (embedUrl == null) {
        // Center status
        if (errorText != null) {
          Text(
            text = errorText.orEmpty(),
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .align(Alignment.Center)
              .padding(24.dp)
          )
        } else if (streamUrl.isBlank() && startPositionMs != null) {
          Text(
            text = "No video link available",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Center)
          )
        } else if (isBuffering) {
          Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(
              color = MaterialTheme.colorScheme.primary,
              strokeWidth = 3.dp,
              modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Loading...", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
          }
        }

        // Bottom control bar
        Row(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(
              Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
            )
            .padding(horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = {
              if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            }
          ) {
            Icon(
              if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = "Play/Pause",
              tint = Color.White
            )
          }
          Slider(
            value = if (seeking) seekValue else if (duration > 0L) position.toFloat() / duration.toFloat() else 0f,
            onValueChange = {
              seeking = true
              seekValue = it
            },
            onValueChangeFinished = {
              if (duration > 0L) exoPlayer.seekTo((seekValue * duration).toLong())
              seeking = false
            },
            enabled = duration > 0L,
            colors = SliderDefaults.colors(
              thumbColor = Color.White,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = Color.White.copy(alpha = 0.3f),
              disabledThumbColor = Color.White,
              disabledActiveTrackColor = Color.White.copy(alpha = 0.3f),
              disabledInactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.weight(1f)
          )
          Text(
            text = "${formatTime(if (seeking) (seekValue * duration).toLong() else position)}/${formatTime(duration)}",
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 4.dp)
          )
          if (hasQualityChoice(qualityOptions) && activeQuality != null) {
            Text(
              text = activeQuality.label,
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .clickable { showQualityDialog = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
          IconButton(
            onClick = {
              val ep = if (current.isSeries) currentEpisode else null
              onPlay(playable, ep, max(0L, exoPlayer.currentPosition))
            }
          ) {
            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
          }
        }
        }
      }
    }

    // Everything below the player scrolls
    LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding(),
      contentPadding = PaddingValues(
        start = Dimens.ScreenHorizontal,
        end = Dimens.ScreenHorizontal,
        top = Dimens.Section,
        bottom = Dimens.ScreenBottom
      ),
      horizontalArrangement = Arrangement.spacedBy(Dimens.GridGap),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Title + info
      item(span = { GridItemSpan(maxLineSpan) }) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = current.title,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Row(
              modifier = Modifier.clickable { showInfo = !showInfo },
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Info",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Icon(
                if (showInfo) Icons.Default.ExpandLess else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
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
              text = "${current.rating}",
              fontSize = 13.sp,
              color = Color(0xFFFFC107)
            )
            Text(
              text = "  |  ${current.year}  |  ${current.category}" + if (current.isSeries) "  |  Series" else "",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          if (showInfo) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = current.description,
              fontSize = 14.sp,
              lineHeight = 20.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Action buttons
      item(span = { GridItemSpan(maxLineSpan) }) {
        Row(
          modifier = Modifier.padding(top = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          PillAction(
            icon = if (inWatchlist) Icons.Default.Check else Icons.Default.Add,
            label = if (inWatchlist) "In list" else "Add to list",
            highlighted = inWatchlist,
            modifier = Modifier.weight(1f)
          ) {
            scope.launch { repository.toggleWatchlist(current, inWatchlist) }
          }
          PillAction(
            icon = Icons.Default.Share,
            label = "Share",
            modifier = Modifier.weight(1f)
          ) { shareMedia() }
          PillAction(
            icon = if (activeDownload?.status == "COMPLETED") Icons.Default.DownloadDone else Icons.Default.Download,
            label = downloadLabel,
            highlighted = true,
            modifier = Modifier.weight(1f)
          ) { onDownloadClick() }
        }
      }

      // Resources
      item(span = { GridItemSpan(maxLineSpan) }) {
        Row(
          modifier = Modifier.padding(top = 12.dp),
          verticalAlignment = Alignment.Bottom
        ) {
          Text(
            text = "Resources",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = if (current.isSeries) "${episodes.size} episodes" else "Direct stream",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (current.isSeries) {
        items(episodes, span = { GridItemSpan(maxLineSpan) }) { ep ->
          val selected = currentEpisode?.id == ep.id
          ResourceCard(
            text = "S${ep.season} E${ep.episodeNumber}  •  ${ep.title}",
            selected = selected
          ) {
            if (!selected) currentEpisode = ep
          }
        }
      } else {
        item(span = { GridItemSpan(maxLineSpan) }) {
          ResourceCard(text = current.title, selected = true) {
            exoPlayer.seekTo(0L)
            exoPlayer.play()
          }
        }
      }

      // For you
      item(span = { GridItemSpan(maxLineSpan) }) {
        Column(modifier = Modifier.padding(top = 16.dp)) {
          Text(
            text = "For you",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier = Modifier
              .width(48.dp)
              .height(3.dp)
              .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
          )
        }
      }

      items(forYou) { item ->
        Column(modifier = Modifier.clickable { onOpenMedia(item) }) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(2f / 3f)
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
          ) {
            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(item.poster)
                .crossfade(true)
                .build(),
              contentDescription = item.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
            Text(
              text = item.category,
              color = Color.White,
              fontSize = 10.sp,
              maxLines = 1,
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Text(
            text = item.title,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }
    }
  }

  if (showQualityDialog) {
    QualityPickerDialog(
      title = "Video quality",
      options = qualityOptions,
      selectedLabel = activeQuality?.label,
      onSelect = { option ->
        selectedQuality = option.label
        repository.setPreferredQuality(option.label)
        showQualityDialog = false
      },
      onDismiss = { showQualityDialog = false }
    )
  }

  if (showDownloadQualityDialog) {
    QualityPickerDialog(
      title = "Download quality",
      options = qualityOptions,
      selectedLabel = activeQuality?.label,
      onSelect = { option ->
        showDownloadQualityDialog = false
        downloadWithQuality(option)
      },
      onDismiss = { showDownloadQualityDialog = false }
    )
  }
}

@Composable
private fun PillAction(
  icon: ImageVector,
  label: String,
  modifier: Modifier = Modifier,
  highlighted: Boolean = false,
  onClick: () -> Unit
) {
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(24.dp))
      .background(
        if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surfaceVariant
      )
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp, horizontal = 8.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      fontSize = 13.sp,
      maxLines = 1,
      softWrap = false,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun ResourceCard(
  text: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surfaceVariant
      )
      .clickable(onClick = onClick)
      .padding(vertical = 16.dp, horizontal = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
  }
}

private fun formatTime(ms: Long): String {
  val totalSeconds = max(0L, ms) / 1000L
  val h = totalSeconds / 3600L
  val m = (totalSeconds % 3600L) / 60L
  val s = totalSeconds % 60L
  return if (h > 0L) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
