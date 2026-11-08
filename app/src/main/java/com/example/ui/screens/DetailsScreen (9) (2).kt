package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
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
import com.example.ui.components.EPISODES_PER_ROW
import com.example.ui.components.EpisodeTileRow
import com.example.data.model.MediaItem
import com.example.data.model.ResumePoint
import com.example.data.model.VideoQuality
import com.example.data.model.buildQualityOptions
import com.example.data.model.hasQualityChoice
import com.example.data.model.pickQuality
import com.example.ui.components.QualityPickerDialog
import com.example.ui.theme.Dimens
import com.example.ui.player.CompactSlider
import com.example.ui.player.MiniPlayerState
import com.example.ui.player.currentBandwidthBps
import com.example.data.model.isAutoQuality
import com.example.data.model.pickAutoQuality
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
  resume: ResumePoint? = null,
  onOpenDownloads: () -> Unit = {}
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
      resume = resume,
      onOpenDownloads = onOpenDownloads
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
  resume: ResumePoint?,
  onOpenDownloads: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // Coming back from fullscreen with the back arrow: the same player, still playing
  val returnedPlayer = remember { com.example.ui.player.PlayerHandoff.takeReturned(media.id) }

  var current by remember { mutableStateOf(media) }
  var episodes by remember { mutableStateOf(media.episodes) }
  var currentEpisode by remember { mutableStateOf<Episode?>(null) }
  var startPositionMs by remember { mutableStateOf<Long?>(null) }
  var showInfo by remember { mutableStateOf(false) }
  // Multiple selection (series episodes)
  var selectionMode by remember { mutableStateOf(false) }
  var selectedIds by remember { mutableStateOf(setOf<String>()) }
  var showBatchQualityDialog by remember { mutableStateOf(false) }
  // Season picker (only shown when the series has more than one season)
  var selectedSeason by remember { mutableStateOf<Int?>(null) }

  // Count this open as one view (for Trending / Most viewed)
  LaunchedEffect(Unit) { repository.reportView(media.id) }

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

  val seasons = remember(episodes) { episodes.map { it.season }.distinct().sorted() }
  val activeSeasonNo = selectedSeason?.takeIf { it in seasons } ?: currentEpisode?.season ?: seasons.firstOrNull()
  val visibleEpisodes = remember(episodes, activeSeasonNo) {
    episodes.filter { it.season == activeSeasonNo }.sortedBy { it.episodeNumber }
  }
  // Follow the playing episode (auto next / resume) into its season
  LaunchedEffect(currentEpisode?.id) { currentEpisode?.season?.let { selectedSeason = it } }

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
  // Auto: follows the network speed (re-checked every few seconds). A fixed choice from the
  // fullscreen player settings is still respected.
  val autoMode = isAutoQuality(selectedQuality)
  var autoLabel by remember { mutableStateOf<String?>(null) }
  val initialAuto = remember(qualityOptions) {
    // Back from fullscreen: stay on the quality that is already playing
    val keepUrl = returnedPlayer?.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty()
    qualityOptions.firstOrNull { keepUrl.isNotBlank() && it.url == keepUrl }
      ?: pickAutoQuality(qualityOptions, currentBandwidthBps(context), null)
  }
  LaunchedEffect(qualityOptions, autoMode) {
    if (!autoMode || qualityOptions.size < 2) return@LaunchedEffect
    while (true) {
      delay(8_000)
      autoLabel = pickAutoQuality(
        qualityOptions, currentBandwidthBps(context), autoLabel ?: initialAuto?.label
      )?.label
    }
  }
  val activeQuality = if (autoMode) {
    qualityOptions.firstOrNull { it.label == autoLabel } ?: initialAuto
  } else {
    pickQuality(qualityOptions, selectedQuality)
  }
  val streamUrl = activeQuality?.url.orEmpty()
  val playable = current.copy(episodes = episodes)

  // ---- Player ----
  val exoPlayer = remember {
    returnedPlayer ?: com.example.ui.player.buildSharedPlayer(context).apply {
      // Loads in the background; nothing plays until the user taps "Click here to play"
      // (coming back from fullscreen / History continues straight away)
      playWhenReady = resume?.mediaId == media.id
    }
  }
  // Sound fix: never keep playing audio after the app is left
  com.example.ui.player.PauseWhenAppStops(exoPlayer)
  var isPlaying by remember { mutableStateOf(returnedPlayer?.isPlaying == true) }
  var wantsPlay by remember { mutableStateOf(returnedPlayer?.playWhenReady == true) }
  var userStarted by remember { mutableStateOf(resume?.mediaId == media.id || returnedPlayer != null) }
  LaunchedEffect(isPlaying) { if (isPlaying) userStarted = true }
  var isBuffering by remember {
    mutableStateOf(returnedPlayer == null || returnedPlayer.playbackState == Player.STATE_BUFFERING)
  }
  // false until the first video frame is on screen: the poster + "Click here to play" shows until then
  var firstFrame by remember { mutableStateOf(returnedPlayer != null && returnedPlayer.videoSize.width > 0) }
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

  LaunchedEffect(streamUrl, startPositionMs) {
    val start = startPositionMs
    if (start != null && streamUrl.isNotBlank() && streamUrl != loadedUrl) {
      val contentKey = currentEpisode?.id ?: current.id
      // The player came back from fullscreen already playing this exact video: keep it running
      val playingUri = exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty()
      if (returnedPlayer != null && !resumeApplied && playingUri == streamUrl &&
        exoPlayer.playbackState != Player.STATE_IDLE
      ) {
        loadedUrl = streamUrl
        loadedKey = contentKey
        resumeApplied = true
        return@LaunchedEffect
      }
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
      firstFrame = false
      errorText = null
      exoPlayer.setMediaItem(Media3Item.fromUri(Uri.parse(streamUrl)), seekTo)
      exoPlayer.prepare()
      resumeApplied = true
    }
  }

  LaunchedEffect(Unit) { MiniPlayerState.release() }
  var handedOver by remember { mutableStateOf(false) }

  // Home button while a video plays -> MainActivity moves it into picture-in-picture
  val inPip = com.example.ui.player.rememberIsInPip()
  // In-app picture-in-picture: the video keeps playing in a small floating window inside the app
  fun enterPipNow() {
    if (streamUrl.isBlank() || exoPlayer.currentMediaItem == null) {
      Toast.makeText(context, "Video is not ready yet", Toast.LENGTH_SHORT).show()
      return
    }
    handedOver = true
    MiniPlayerState.start(exoPlayer, latestMedia, if (current.isSeries) latestEpisode else null)
    onBack()
  }

  LaunchedEffect(isPlaying) { com.example.util.PipState.videoActive = isPlaying }
  DisposableEffect(Unit) { onDispose { com.example.util.PipState.videoActive = false } }

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

      override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        wantsPlay = playWhenReady
      }

      override fun onIsPlayingChanged(playing: Boolean) {
        isPlaying = playing
      }

      override fun onRenderedFirstFrame() {
        firstFrame = true
      }

      override fun onPlayerError(error: PlaybackException) {
        errorText = "Server error. Wait 10 minutes, it will be fixed automatically in 10 minutes."
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
      if (!handedOver) exoPlayer.release()
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
    activeDownload.status == "PENDING" -> "Pending"
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

  // ---- Multiple selection + download ----
  val batchOptions = remember(selectedIds, episodes) {
    episodes.filter { it.id in selectedIds }
      .flatMap { buildQualityOptions(it.qualities, it.streamUrl) }
      .distinctBy { it.label }
      .sortedBy { o -> o.label.filter { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }
  }

  fun downloadSelected(label: String?) {
    val picked = episodes.filter { it.id in selectedIds }
    selectionMode = false
    selectedIds = emptySet()
    scope.launch {
      var started = 0
      var skipped = 0
      for (ep in picked) {
        val opts = buildQualityOptions(ep.qualities, ep.streamUrl)
        val opt = pickQuality(opts, label ?: selectedQuality)
        val url = opt?.url.orEmpty()
        val existing = downloads.firstOrNull { it.mediaId == ep.id }
        if (url.isBlank() || url.contains(".m3u8", ignoreCase = true) ||
          (existing != null && existing.status != "FAILED")
        ) {
          skipped++
          continue
        }
        val suffix = if (hasQualityChoice(opts) && opt != null) " (${opt.label})" else ""
        val epName = ep.fileName.ifBlank { "${current.title} - ${ep.title}" }
        downloadHelper.startDownload(current.copy(id = ep.id, title = "$epName$suffix"), url)
        started++
        delay(250)
      }
      val msg = when {
        started == 0 -> "Nothing to download (already downloaded or no link)"
        skipped > 0 -> "$started downloads started, $skipped skipped"
        else -> "$started downloads started"
      }
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
  }

  fun onBatchDownloadClick() {
    if (selectedIds.isEmpty()) {
      Toast.makeText(context, "Select episodes first", Toast.LENGTH_SHORT).show()
    } else if (batchOptions.size > 1) {
      showBatchQualityDialog = true
    } else {
      downloadSelected(null)
    }
  }

  // ---- UI ----
  Box(modifier = Modifier.fillMaxSize()) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .auroraBackground()
  ) {
    // Player area
    Box(
      modifier = (if (inPip) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
        .background(Color.Black)
        .then(if (inPip) Modifier else Modifier.statusBarsPadding())
    ) {
      Box(
        modifier = if (inPip) Modifier.fillMaxSize() else Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f)
      ) {
        AndroidView(
          modifier = Modifier.fillMaxSize(),
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

        // Poster + "Click here to play" until the video has started (poster is cached after 1st load)
        if ((!userStarted || !firstFrame) && errorText == null) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black)
          ) {
            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(current.banner.ifBlank { current.poster })
                .crossfade(true)
                .build(),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
            Column(
              modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-26).dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              if (userStarted && isBuffering) {
                CircularProgressIndicator(
                  color = MaterialTheme.colorScheme.primary,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
              }
              if (!userStarted) Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color.Black.copy(alpha = 0.55f))
                  .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                  .clickable {
                    userStarted = true
                    exoPlayer.play()
                  }
                  .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Click here to play",
                  color = Color.White,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }

        // Back button
        if (!inPip) IconButton(
          onClick = onBack,
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(4.dp)
        ) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

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
        } else if (isBuffering && firstFrame) {
          Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(
              color = MaterialTheme.colorScheme.primary,
              strokeWidth = 2.dp,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Loading...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
          }
        }

        // Bottom control bar
        if (!inPip) Row(
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
              com.example.ui.player.togglePlayPause(exoPlayer)
            }
          ) {
            Icon(
              if (wantsPlay) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = "Play/Pause",
              tint = Color.White
            )
          }
          CompactSlider(
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
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 6.dp)
          )
          Text(
            text = "${formatTime(if (seeking) (seekValue * duration).toLong() else position)}/${formatTime(duration)}",
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 4.dp)
          )
          IconButton(onClick = { enterPipNow() }) {
            Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Picture in picture", tint = Color.White)
          }
          IconButton(
            onClick = {
              val ep = if (current.isSeries) currentEpisode else null
              // Hand the running player to the fullscreen screen: the video keeps playing, no reload
              if (exoPlayer.currentMediaItem != null) {
                handedOver = true
                com.example.ui.player.PlayerHandoff.player = exoPlayer
              }
              onPlay(playable, ep, max(0L, exoPlayer.currentPosition))
            }
          ) {
            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
          }
        }
      }
    }

    // Everything below the player scrolls (hidden in the small picture-in-picture window)
    if (!inPip) LazyVerticalGrid(
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
              fontSize = 16.sp,
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
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Icon(
                if (showInfo) Icons.Default.ExpandLess else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Star,
              contentDescription = null,
              tint = Color(0xFFFFC107),
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${current.rating}",
              fontSize = 11.sp,
              color = Color(0xFFFFC107)
            )
            Text(
              text = "  |  ${current.year}  |  ${current.category}" + if (current.isSeries) "  |  Series" else "",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          if (showInfo) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = current.description,
              fontSize = 12.sp,
              lineHeight = 17.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Action buttons
      item(span = { GridItemSpan(maxLineSpan) }) {
        Column(
          modifier = Modifier.padding(top = 2.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
            PillAction(
              icon = Icons.Default.Folder,
              label = if (downloads.isNotEmpty()) "Files (${downloads.size})" else "Files",
              modifier = Modifier.weight(1f)
            ) { onOpenDownloads() }
          }
        }
      }

      // Episodes heading (series only)
      if (current.isSeries) {
        item(span = { GridItemSpan(maxLineSpan) }) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Episodes",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.weight(1f)
            )
            if (episodes.size > 1) {
              Text(
                text = if (selectionMode) "Cancel" else "Select",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                  .glassSurface(RoundedCornerShape(16.dp))
                  .clickable {
                    selectionMode = !selectionMode
                    selectedIds = emptySet()
                  }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }
          }
        }
      }

      if (current.isSeries) {
        // Season picker chips
        if (seasons.size > 1) {
          item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              seasons.forEach { n ->
                val on = n == activeSeasonNo
                Text(
                  text = "Season $n",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  softWrap = false,
                  color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier
                    .glassSurface(RoundedCornerShape(16.dp))
                    .then(
                      if (on) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                      else Modifier
                    )
                    .clickable {
                      selectedSeason = n
                      selectedIds = emptySet()
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                )
              }
            }
          }
        }
        // Episodes as number tiles (01, 02, 03 ...) of the chosen season
        items(
          visibleEpisodes.chunked(EPISODES_PER_ROW),
          span = { GridItemSpan(maxLineSpan) }
        ) { rowEps ->
          EpisodeTileRow(
            episodes = rowEps,
            currentEpisodeId = currentEpisode?.id,
            selectionMode = selectionMode,
            selectedIds = selectedIds,
            downloadedIds = downloads.filter { it.status == "COMPLETED" }.map { it.mediaId }.toSet()
          ) { ep ->
            if (selectionMode) {
              selectedIds = if (ep.id in selectedIds) selectedIds - ep.id else selectedIds + ep.id
            } else if (currentEpisode?.id != ep.id) {
              userStarted = true
              exoPlayer.playWhenReady = true
              currentEpisode = ep
            }
          }
        }
      } else {
        item(span = { GridItemSpan(maxLineSpan) }) {
          ResourceCard(text = current.title, selected = true) {
            userStarted = true
            exoPlayer.seekTo(0L)
            exoPlayer.play()
          }
        }
      }

      // Ad strip under the title card
      if (com.example.ads.AdConfig.ADS_ENABLED && com.example.ads.AdConfig.DETAILS_AD_ENABLED &&
        com.example.ads.AdConfig.DETAILS_ZONE_ID != 0L
      ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
          com.example.ads.AdWebView(
            scriptSrc = com.example.ads.AdConfig.DETAILS_SCRIPT_SRC,
            zoneId = com.example.ads.AdConfig.DETAILS_ZONE_ID,
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 12.dp)
              .height(com.example.ads.AdConfig.STRIP_AD_HEIGHT_DP.dp)
          )
        }
      }

      // For you
      item(span = { GridItemSpan(maxLineSpan) }) {
        Column(modifier = Modifier.padding(top = 16.dp)) {
          Text(
            text = "For you",
            fontSize = 16.sp,
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
              fontSize = 9.sp,
              maxLines = 1,
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Text(
            text = item.title,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }
    }
  }

  // Floating glass bar while selecting episodes
  if (selectionMode) {
    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(horizontal = 12.dp, vertical = 12.dp)
        .fillMaxWidth()
        .glassSurface(RoundedCornerShape(28.dp), 1.4f)
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "${selectedIds.size} selected",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = if (visibleEpisodes.isNotEmpty() && selectedIds.size == visibleEpisodes.size) "Clear" else "Select all",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
          .glassSurface(RoundedCornerShape(16.dp))
          .clickable {
            selectedIds = if (selectedIds.size == visibleEpisodes.size) emptySet()
            else visibleEpisodes.map { it.id }.toSet()
          }
          .padding(horizontal = 12.dp, vertical = 7.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.primary)
          .clickable { onBatchDownloadClick() }
          .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
      }
    }
  }
  }

  if (showBatchQualityDialog) {
    QualityPickerDialog(
      title = "Download quality",
      options = batchOptions,
      selectedLabel = activeQuality?.label,
      onSelect = { option ->
        showBatchQualityDialog = false
        downloadSelected(option.label)
      },
      onDismiss = { showBatchQualityDialog = false }
    )
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
  val shape = RoundedCornerShape(20.dp)
  Row(
    modifier = modifier
      .glassSurface(shape)
      .then(
        if (highlighted) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
        else Modifier
      )
      .clickable(onClick = onClick)
      .padding(vertical = 9.dp, horizontal = 4.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      icon,
      contentDescription = null,
      tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.size(14.dp)
    )
    Spacer(modifier = Modifier.width(3.dp))
    Text(
      text = label,
      fontSize = 10.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun ResourceCard(
  text: String,
  selected: Boolean,
  selectionMode: Boolean = false,
  checked: Boolean = false,
  downloaded: Boolean = false,
  onClick: () -> Unit
) {
  val shape = RoundedCornerShape(12.dp)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .glassSurface(shape)
      .then(
        if (selected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
        else Modifier
      )
      .clickable(onClick = onClick)
      .padding(vertical = 11.dp, horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (selectionMode) {
      Icon(
        if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
        contentDescription = null,
        tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
    }
    Text(
      text = text,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = if (selectionMode) TextAlign.Start else TextAlign.Center,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f)
    )
    if (downloaded) {
      Spacer(modifier = Modifier.width(8.dp))
      Icon(
        Icons.Default.DownloadDone,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp)
      )
    }
  }
}

/** Frosted glass: translucent fill + thin edge. Turns into a white sheet on the Light theme. */
@Composable
private fun Modifier.glassSurface(shape: Shape, strength: Float = 1f): Modifier {
  val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
  val top = if (light) 0.78f else 0.14f
  val bottom = if (light) 0.50f else 0.05f
  val edge = if (light) Color.Black else Color.White
  return this
    .clip(shape)
    .background(
      Brush.linearGradient(
        listOf(Color.White.copy(alpha = top * strength), Color.White.copy(alpha = bottom * strength))
      )
    )
    .border(
      1.dp,
      Brush.linearGradient(
        listOf(edge.copy(alpha = if (light) 0.10f else 0.28f), edge.copy(alpha = if (light) 0.03f else 0.05f))
      ),
      shape
    )
}

/** Page background with soft colour glows so the glass has something to show through. */
@Composable
private fun Modifier.auroraBackground(): Modifier {
  val primary = MaterialTheme.colorScheme.primary
  val bg = MaterialTheme.colorScheme.background
  val light = bg.luminance() > 0.5f
  return this
    .background(bg)
    .drawBehind {
      val r = size.width * 0.95f
      drawRect(
        Brush.radialGradient(
          listOf(primary.copy(alpha = if (light) 0.20f else 0.28f), Color.Transparent),
          center = Offset(size.width * 0.9f, size.height * 0.30f),
          radius = r
        )
      )
      drawRect(
        Brush.radialGradient(
          listOf(Color(0xFF3B82F6).copy(alpha = if (light) 0.12f else 0.16f), Color.Transparent),
          center = Offset(size.width * 0.05f, size.height * 0.75f),
          radius = r
        )
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
