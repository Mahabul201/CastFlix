package com.example.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.Episode
import com.example.data.model.MediaItem
import com.example.data.model.VideoQuality
import com.example.data.model.buildQualityOptions
import com.example.data.model.pickQuality
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max

/** Pages of the right-hand settings panel. */
/** Lets the half-screen player pass its running ExoPlayer to the fullscreen player (no restart). */
object PlayerHandoff {
  @Volatile var player: ExoPlayer? = null
}

private const val PAGE_MAIN = "main"
private const val PAGE_QUALITY = "quality"
private const val PAGE_AUDIO = "audio"
private const val PAGE_CAPTIONS = "captions"
private const val PAGE_SPEED = "speed"
private const val PAGE_EPISODES = "episodes"

private val PanelBackground = Color(0xFF212121)
private val SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun VideoPlayerScreen(
  media: MediaItem,
  initialEpisode: Episode? = null,
  initialPositionMs: Long = 0L,
  onBack: () -> Unit,
  onSaveProgress: (positionMs: Long, durationMs: Long, episode: Episode?) -> Unit,
  preferredQuality: String? = null,
  onQualityChosen: (String) -> Unit = {}
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

  var currentEpisode by remember { mutableStateOf(initialEpisode ?: media.episodes.firstOrNull()) }

  // Quality (480p / 720p / 1080p)
  var selectedQuality by remember { mutableStateOf(preferredQuality) }
  val qualityOptions = remember(media, currentEpisode) {
    val ep = currentEpisode
    if (ep != null) buildQualityOptions(ep.qualities, ep.streamUrl)
    else buildQualityOptions(media.qualities, media.streamUrl)
  }
  val activeQuality = pickQuality(qualityOptions, selectedQuality)
  val activeStreamUrl = activeQuality?.url ?: (currentEpisode?.streamUrl ?: media.streamUrl)

  var isPlaying by remember { mutableStateOf(true) }
  var isBuffering by remember { mutableStateOf(true) }
  var currentPosition by remember { mutableLongStateOf(initialPositionMs) }
  var totalDuration by remember { mutableLongStateOf(0L) }
  var controlsVisible by remember { mutableStateOf(true) }
  var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
  var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

  // Scrubbing the seek bar
  var scrubbing by remember { mutableStateOf(false) }
  var scrubFraction by remember { mutableFloatStateOf(0f) }

  // Screen lock
  var isLocked by remember { mutableStateOf(false) }
  var unlockButtonVisible by remember { mutableStateOf(false) }

  // Settings side panel (null = closed)
  var settingsPage by remember { mutableStateOf<String?>(null) }
  var tracks by remember { mutableStateOf(Tracks.EMPTY) }

  // Gestures HUD feedback
  var brightnessLevel by remember { mutableFloatStateOf(0.7f) }
  var volumeLevel by remember { mutableFloatStateOf(0.7f) }
  var showBrightnessHud by remember { mutableStateOf(false) }
  var showVolumeHud by remember { mutableStateOf(false) }
  var doubleTapFeedbackText by remember { mutableStateOf<String?>(null) }
  var hintText by remember { mutableStateOf<String?>(null) }

  val coroutineScope = rememberCoroutineScope()

  // Initialize ExoPlayer
  val exoPlayer = remember {
    PlayerHandoff.player?.also { PlayerHandoff.player = null } ?: ExoPlayer.Builder(
      context,
      androidx.media3.exoplayer.DefaultRenderersFactory(context)
        .setEnableDecoderFallback(true)
        .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
    ).build().apply {
      playWhenReady = true
      repeatMode = Player.REPEAT_MODE_OFF
      // Jump to the nearest keyframe: skips respond instantly instead of stalling/ignoring taps
      setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
    }
  }

  // Load stream: resume on first load, keep position when only the quality changes
  // When the player was handed over from the watch page the stream is already loaded
  val handedUrl = remember { exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty() }
  var loadedUrl by remember { mutableStateOf(handedUrl) }
  var loadedKey by remember { mutableStateOf(if (handedUrl.isNotBlank()) (currentEpisode?.id ?: media.id) else "") }
  var firstLoadDone by remember { mutableStateOf(handedUrl.isNotBlank()) }
  LaunchedEffect(Unit) {
    if (handedUrl.isNotBlank()) {
      isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING
      isPlaying = exoPlayer.isPlaying
      totalDuration = max(0L, exoPlayer.duration)
      currentPosition = max(0L, exoPlayer.currentPosition)
      tracks = exoPlayer.currentTracks
      exoPlayer.play()
    }
  }
  LaunchedEffect(activeStreamUrl) {
    if (activeStreamUrl.isNotBlank() && activeStreamUrl != loadedUrl) {
      val contentKey = currentEpisode?.id ?: media.id
      val startAt = when {
        loadedKey == contentKey && loadedUrl.isNotBlank() -> max(0L, exoPlayer.currentPosition)
        !firstLoadDone -> initialPositionMs
        else -> 0L
      }
      loadedUrl = activeStreamUrl
      loadedKey = contentKey
      firstLoadDone = true
      exoPlayer.setMediaItem(Media3Item.fromUri(Uri.parse(activeStreamUrl)), startAt)
      exoPlayer.prepare()
      exoPlayer.play()
    }
  }

  // Never overwrite a good resume point with "0:00" when the user leaves right away
  fun saveNow() {
    val pos = exoPlayer.currentPosition
    if (pos > 3000L) {
      onSaveProgress(pos, max(0L, exoPlayer.duration), currentEpisode)
    }
  }

  fun seekBy(deltaMs: Long) {
    // Recover if the player dropped to idle (e.g. after a network error)
    if (exoPlayer.playbackState == Player.STATE_IDLE) exoPlayer.prepare()
    val d = exoPlayer.duration
    val base = if (exoPlayer.currentPosition >= 0) exoPlayer.currentPosition else currentPosition
    var target = max(0L, base + deltaMs)
    // Don't seek onto the very last frame (that makes the player end/stall)
    if (d > 0) target = target.coerceAtMost(max(0L, d - 500L))
    exoPlayer.seekTo(target)
    currentPosition = target
  }

  // Rapid repeat taps after a double-tap keep skipping (like YouTube)
  val lastSkip = remember { longArrayOf(0L, 0L) } // [time, direction]

  fun selectTrack(type: Int, group: Tracks.Group?, index: Int) {
    val builder = exoPlayer.trackSelectionParameters.buildUpon()
    if (group == null) {
      builder.setTrackTypeDisabled(type, true)
    } else {
      builder.setTrackTypeDisabled(type, false)
      builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index))
    }
    exoPlayer.trackSelectionParameters = builder.build()
  }

  fun openInExternalPlayer() {
    val uri = Uri.parse(activeStreamUrl)
    if (uri.scheme.isNullOrBlank()) {
      Toast.makeText(context, "This file can't be opened in another app", Toast.LENGTH_SHORT).show()
      return
    }
    try {
      exoPlayer.pause()
      val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/*")
      context.startActivity(Intent.createChooser(view, "Open with"))
    } catch (e: Exception) {
      Toast.makeText(context, "No app found to open this video", Toast.LENGTH_SHORT).show()
    }
  }

  fun enterPip() {
    val act = activity
    if (act == null || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
      hintText = "Picture-in-picture not supported"
      return
    }
    try {
      controlsVisible = false
      act.enterPictureInPictureMode(
        PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
      )
    } catch (e: Exception) {
      hintText = "Picture-in-picture not available"
    }
  }

  // Back handling
  BackHandler {
    val page = settingsPage
    when {
      page != null && page != PAGE_MAIN -> settingsPage = PAGE_MAIN
      page != null -> settingsPage = null
      isLocked -> unlockButtonVisible = true
      else -> {
        saveNow()
        onBack()
      }
    }
  }

  // Auto-save every 10s while playing
  LaunchedEffect(exoPlayer) {
    while (true) {
      delay(10_000)
      if (exoPlayer.isPlaying) saveNow()
    }
  }

  // Auto-hide controls
  LaunchedEffect(controlsVisible, isPlaying, scrubbing) {
    if (controlsVisible && isPlaying && !scrubbing) {
      delay(4500)
      controlsVisible = false
    }
  }

  // Auto-hide the unlock button while the screen is locked
  LaunchedEffect(isLocked, unlockButtonVisible) {
    if (isLocked && unlockButtonVisible) {
      delay(3000)
      unlockButtonVisible = false
    }
  }

  // Short hint pill ("Locked", "Zoom", …)
  LaunchedEffect(hintText) {
    if (hintText != null) {
      delay(1200)
      hintText = null
    }
  }

  // Progress ticker
  LaunchedEffect(exoPlayer) {
    while (true) {
      if (exoPlayer.isPlaying && !scrubbing) {
        currentPosition = exoPlayer.currentPosition
        totalDuration = max(0L, exoPlayer.duration)
        isPlaying = true
      }
      delay(500)
    }
  }

  // Lifecycle
  DisposableEffect(Unit) {
    val listener = object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        isBuffering = playbackState == Player.STATE_BUFFERING
        if (playbackState == Player.STATE_READY) {
          totalDuration = max(0L, exoPlayer.duration)
        }
        if (playbackState == Player.STATE_ENDED) {
          // If series, autoplay next episode if available
          val nextEp = media.episodes.find {
            currentEpisode != null && it.episodeNumber == currentEpisode!!.episodeNumber + 1 && it.season == currentEpisode!!.season
          }
          if (nextEp != null) {
            currentEpisode = nextEp
          }
        }
      }

      override fun onIsPlayingChanged(playing: Boolean) {
        isPlaying = playing
      }

      override fun onTracksChanged(newTracks: Tracks) {
        tracks = newTracks
      }
    }
    exoPlayer.addListener(listener)

    // Keep screen on while playing
    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // True full screen: landscape, hide status + navigation bars, draw into the notch area
    val window = activity?.window
    val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    insetsController?.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController?.hide(WindowInsetsCompat.Type.systemBars())
    if (window != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
      window.attributes = window.attributes.apply {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      }
    }

    onDispose {
      insetsController?.show(WindowInsetsCompat.Type.systemBars())
      if (window != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
          layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }
      }
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      saveNow()
      exoPlayer.removeListener(listener)
      exoPlayer.release()
    }
  }

  val panelWidth = (LocalConfiguration.current.screenWidthDp * 0.42f).dp
  val prevEpisode = media.episodes.find {
    currentEpisode != null && it.episodeNumber == currentEpisode!!.episodeNumber - 1 && it.season == currentEpisode!!.season
  }
  val nextEpisode = media.episodes.find {
    currentEpisode != null && it.episodeNumber == currentEpisode!!.episodeNumber + 1 && it.season == currentEpisode!!.season
  }

  // Video on the left, settings panel (when open) on the right — like a normal player
  Row(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .background(Color.Black)
    ) {
      // Media3 PlayerView
      AndroidView(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectTapGestures(
              onTap = { tapOffset ->
                val quickRepeat = !isLocked && settingsPage == null &&
                  lastSkip[1] != 0L && System.currentTimeMillis() - lastSkip[0] < 700L
                if (quickRepeat) {
                  val dir = lastSkip[1].toInt()
                  seekBy(dir * 10_000L)
                  lastSkip[0] = System.currentTimeMillis()
                  doubleTapFeedbackText = if (dir > 0) "+10s" else "-10s"
                  coroutineScope.launch {
                    delay(1000)
                    doubleTapFeedbackText = null
                  }
                  return@detectTapGestures
                }
                when {
                  settingsPage != null -> settingsPage = null
                  isLocked -> unlockButtonVisible = !unlockButtonVisible
                  else -> controlsVisible = !controlsVisible
                }
              },
              onDoubleTap = { offset ->
                if (isLocked || settingsPage != null) return@detectTapGestures
                val screenWidth = size.width
                if (offset.x < screenWidth * 0.4f) {
                  seekBy(-10_000)
                  lastSkip[0] = System.currentTimeMillis(); lastSkip[1] = -1L
                  doubleTapFeedbackText = "-10s"
                  coroutineScope.launch {
                    delay(1000)
                    doubleTapFeedbackText = null
                  }
                } else if (offset.x > screenWidth * 0.6f) {
                  seekBy(10_000)
                  lastSkip[0] = System.currentTimeMillis(); lastSkip[1] = 1L
                  doubleTapFeedbackText = "+10s"
                  coroutineScope.launch {
                    delay(1000)
                    doubleTapFeedbackText = null
                  }
                } else {
                  if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                }
              }
            )
          }
          .pointerInput(Unit) {
            detectDragGestures(
              onDragEnd = {
                showBrightnessHud = false
                showVolumeHud = false
              },
              onDragCancel = {
                showBrightnessHud = false
                showVolumeHud = false
              },
              onDrag = { change, dragAmount ->
                if (isLocked) return@detectDragGestures
                change.consume()
                val screenWidth = size.width
                val dragDelta = -dragAmount.y / 600f

                if (change.position.x < screenWidth * 0.4f) {
                  // Left side: Brightness
                  brightnessLevel = (brightnessLevel + dragDelta).coerceIn(0.05f, 1.0f)
                  activity?.window?.attributes = activity?.window?.attributes?.apply {
                    screenBrightness = brightnessLevel
                  }
                  showBrightnessHud = true
                } else if (change.position.x > screenWidth * 0.6f) {
                  // Right side: Volume
                  volumeLevel = (volumeLevel + dragDelta).coerceIn(0.0f, 1.0f)
                  val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                  audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    (volumeLevel * maxVolume).toInt(),
                    0
                  )
                  showVolumeHud = true
                }
              }
            )
          },
        factory = { ctx ->
          PlayerView(ctx).apply {
            player = exoPlayer
            useController = false
            this.resizeMode = resizeMode
            layoutParams = FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
          }
        },
        update = { view ->
          view.resizeMode = resizeMode
        }
      )

      // Double tap feedback
      if (doubleTapFeedbackText != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier.padding(16.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (doubleTapFeedbackText == "-10s") Icons.Default.FastRewind else Icons.Default.FastForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = doubleTapFeedbackText ?: "",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // Brightness / volume HUD
      if (showBrightnessHud) {
        HudBox(Modifier.align(Alignment.CenterStart).padding(start = 32.dp), Icons.Default.BrightnessMedium, brightnessLevel)
      }
      if (showVolumeHud) {
        HudBox(Modifier.align(Alignment.CenterEnd).padding(end = 32.dp), Icons.Default.VolumeUp, volumeLevel)
      }

      // Short hint pill
      if (hintText != null) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = Color.Black.copy(alpha = 0.75f),
          modifier = Modifier
            .align(Alignment.TopCenter)
            .displayCutoutPadding()
            .padding(top = 16.dp)
        ) {
          Text(
            text = hintText ?: "",
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
          )
        }
      }

      // Loading indicator
      if (isBuffering && !(controlsVisible && !isLocked && settingsPage == null)) {
        CircularProgressIndicator(
          modifier = Modifier
            .align(Alignment.Center)
            .size(52.dp),
          color = Color.White,
          strokeWidth = 4.dp
        )
      }

      // Unlock button (only while the screen is locked)
      if (isLocked && unlockButtonVisible) {
        Surface(
          shape = CircleShape,
          color = Color.Black.copy(alpha = 0.6f),
          modifier = Modifier
            .align(Alignment.CenterStart)
            .displayCutoutPadding()
            .padding(start = 24.dp)
            .size(52.dp)
            .clickable {
              isLocked = false
              unlockButtonVisible = false
              controlsVisible = true
              hintText = "Unlocked"
            }
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.LockOpen, contentDescription = "Unlock", tint = Color.White)
          }
        }
      }

      // ===== Controls overlay =====
      FadeVisibility(
        visible = controlsVisible && !isLocked && settingsPage == null
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .displayCutoutPadding()
        ) {
          // Top: lock (+ back and title)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.TopCenter)
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = {
                saveNow()
                onBack()
              },
              modifier = Modifier.testTag("player_back_button")
            ) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
              )
            }
            IconButton(
              onClick = {
                isLocked = true
                controlsVisible = false
                unlockButtonVisible = true
                hintText = "Screen locked"
              }
            ) {
              Icon(
                Icons.Default.Lock,
                contentDescription = "Lock controls",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (media.isSeries) "Episodes" else "Screen Cast",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              currentEpisode?.let { ep ->
                if (media.isSeries) {
                  Text(
                    text = "S${ep.season} E${ep.episodeNumber} • ${ep.title}",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
            IconButton(onClick = { openInExternalPlayer() }) {
              Icon(
                Icons.Default.OpenInNew,
                contentDescription = "Open in another app",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
              )
            }
          }

          // Center: -10s | play / pause | +10s
          Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            DarkRoundButton(Icons.Default.Replay10, "Back 10 seconds", 52.dp, 30.dp) { seekBy(-10_000) }
            Box(
              modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A0F1E))
                .border(4.dp, Color(0xFF2196F3), CircleShape)
                .clickable {
                  if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                }
                .testTag("player_play_pause_button"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(44.dp)
              )
            }
            DarkRoundButton(Icons.Default.Forward10, "Forward 10 seconds", 52.dp, 30.dp) { seekBy(10_000) }
          }

          // Bottom: time | seek bar | duration, then the control row
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.BottomCenter)
              .padding(horizontal = 12.dp)
              .padding(bottom = 4.dp)
          ) {
            val fraction = if (totalDuration > 0) {
              (currentPosition.toFloat() / totalDuration).coerceIn(0f, 1f)
            } else 0f
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                formatTimeMs(currentPosition),
                color = Color.White,
                fontSize = 13.sp
              )
              Slider(
                value = if (scrubbing) scrubFraction else fraction,
                onValueChange = { ratio ->
                  scrubbing = true
                  scrubFraction = ratio
                  currentPosition = (ratio * totalDuration).toLong()
                },
                onValueChangeFinished = {
                  exoPlayer.seekTo((scrubFraction * totalDuration).toLong())
                  scrubbing = false
                },
                colors = SliderDefaults.colors(
                  thumbColor = Color.White,
                  activeTrackColor = Color.White,
                  inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                  .weight(1f)
                  .height(24.dp)
                  .padding(horizontal = 12.dp)
              )
              Text(
                formatTimeMs(totalDuration),
                color = Color.White,
                fontSize = 13.sp
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Left: previous | play / pause | next
              BareControl(
                Icons.Default.SkipPrevious, "Previous episode",
                enabled = prevEpisode != null
              ) { prevEpisode?.let { currentEpisode = it } }
              BareControl(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (isPlaying) "Pause" else "Play"
              ) {
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
              }
              BareControl(
                Icons.Default.SkipNext, "Next episode",
                enabled = nextEpisode != null
              ) { nextEpisode?.let { currentEpisode = it } }

              Spacer(modifier = Modifier.weight(1f))

              // Right: fit | speed | settings | picture-in-picture
              BareControl(Icons.Default.AspectRatio, "Resize video") {
                resizeMode = when (resizeMode) {
                  AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                  AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                  else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                hintText = when (resizeMode) {
                  AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit"
                  AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom (crop)"
                  else -> "Stretch"
                }
              }
              Box(
                modifier = Modifier
                  .size(width = 56.dp, height = 44.dp)
                  .clickable {
                    val i = SPEEDS.indexOfFirst { it == playbackSpeed }
                    playbackSpeed = SPEEDS[(i + 1) % SPEEDS.size]
                    exoPlayer.setPlaybackSpeed(playbackSpeed)
                    hintText = "Speed ${speedLabel(playbackSpeed)}"
                  },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.Speed,
                  contentDescription = "Playback speed",
                  tint = Color.White,
                  modifier = Modifier.size(26.dp)
                )
                Text(
                  text = speedLabel(playbackSpeed),
                  color = Color(0xFF2196F3),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 4.dp)
                )
              }
              BareControl(Icons.Default.Settings, "Settings") {
                settingsPage = PAGE_MAIN
                controlsVisible = false
              }
              BareControl(Icons.Default.PictureInPictureAlt, "Picture in picture") { enterPip() }
            }
          }
        }
      }
    }

    // ===== Settings side panel =====
    val page = settingsPage
    if (page != null) {
      SettingsPanel(
        modifier = Modifier
          .width(panelWidth)
          .fillMaxHeight()
          .background(
            Brush.verticalGradient(listOf(Color(0xD9202028), Color(0xCC101016)))
          )
          .border(1.dp, Color.White.copy(alpha = 0.12f))
          .displayCutoutPadding(),
        page = page,
        onNavigate = { settingsPage = it },
        qualityOptions = qualityOptions,
        activeQualityLabel = activeQuality?.label,
        onSelectQuality = { option ->
          selectedQuality = option.label
          onQualityChosen(option.label)
          settingsPage = PAGE_MAIN
        },
        tracks = tracks,
        onSelectTrack = { type, group, index ->
          selectTrack(type, group, index)
          settingsPage = PAGE_MAIN
        },
        playbackSpeed = playbackSpeed,
        onSelectSpeed = { spd ->
          playbackSpeed = spd
          exoPlayer.setPlaybackSpeed(spd)
          settingsPage = PAGE_MAIN
        },
        episodes = if (media.isSeries) media.episodes else emptyList(),
        currentEpisodeId = currentEpisode?.id,
        onSelectEpisode = { ep ->
          currentEpisode = ep
          settingsPage = null
        },
        onSkipIntro = {
          seekBy(85_000)
          settingsPage = null
        }
      )
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Small building blocks
// ---------------------------------------------------------------------------------------------

@Composable
private fun HudBox(modifier: Modifier, icon: ImageVector, level: Float) {
  Box(modifier = modifier) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = Color.Black.copy(alpha = 0.75f)
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text("${(level * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
      }
    }
  }
}

@Composable
private fun PlayerIconButton(
  icon: ImageVector,
  description: String,
  box: androidx.compose.ui.unit.Dp,
  iconSize: androidx.compose.ui.unit.Dp,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  IconButton(
    onClick = onClick,
    modifier = modifier
      .size(box)
      .clip(CircleShape)
      .background(Color.White.copy(alpha = 0.14f))
      .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
  ) {
    Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(iconSize))
  }
}

private fun speedLabel(speed: Float): String =
  if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x"

@Composable
private fun DarkRoundButton(
  icon: ImageVector,
  description: String,
  box: androidx.compose.ui.unit.Dp,
  iconSize: androidx.compose.ui.unit.Dp,
  onClick: () -> Unit
) {
  IconButton(
    onClick = onClick,
    modifier = Modifier
      .size(box)
      .clip(CircleShape)
      .background(Color(0xFF0A0F1E).copy(alpha = 0.85f))
  ) {
    Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(iconSize))
  }
}

@Composable
private fun BareControl(
  icon: ImageVector,
  description: String,
  enabled: Boolean = true,
  onClick: () -> Unit
) {
  IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp)) {
    Icon(
      icon,
      contentDescription = description,
      tint = if (enabled) Color.White else Color.White.copy(alpha = 0.35f),
      modifier = Modifier.size(26.dp)
    )
  }
}

@Composable
private fun SmallControl(icon: ImageVector, description: String, onClick: () -> Unit) {
  IconButton(
    onClick = onClick,
    modifier = Modifier
      .padding(start = 6.dp)
      .size(30.dp)
      .clip(CircleShape)
      .background(Color.White.copy(alpha = 0.14f))
      .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
  ) {
    Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(17.dp))
  }
}

// ---------------------------------------------------------------------------------------------
// Settings panel (Quality / Audio / Captions / Speed / Episodes)
// ---------------------------------------------------------------------------------------------

private fun trackName(format: Format, index: Int): String {
  val lang = format.language
    ?.takeIf { it != "und" }
    ?.let { Locale.forLanguageTag(it).displayLanguage }
    ?.takeIf { it.isNotBlank() }
  return format.label ?: lang ?: "Track ${index + 1}"
}

private val LANG_FALLBACK = mapOf(
  "hin" to "Hindi", "eng" to "English", "tam" to "Tamil", "tel" to "Telugu",
  "mal" to "Malayalam", "kan" to "Kannada", "ben" to "Bengali", "urd" to "Urdu",
  "jpn" to "Japanese", "kor" to "Korean", "chi" to "Chinese", "zho" to "Chinese",
  "spa" to "Spanish", "fra" to "French", "fre" to "French", "ger" to "German",
  "deu" to "German", "rus" to "Russian", "ara" to "Arabic", "por" to "Portuguese",
  "ita" to "Italian", "tur" to "Turkish", "mar" to "Marathi", "pan" to "Punjabi"
)

private fun audioTrackName(format: Format, index: Int): String {
  val code = format.language?.takeIf { it != "und" && it.isNotBlank() }
  val lang = code?.let {
    LANG_FALLBACK[it.lowercase()]
      ?: Locale.forLanguageTag(it).displayLanguage.takeIf { d -> d.isNotBlank() && !d.equals(it, true) }
  }
  return format.label?.takeIf { it.isNotBlank() } ?: lang ?: code ?: "Audio ${index + 1}"
}

private fun codecName(format: Format): String = when {
  format.sampleMimeType == null -> ""
  format.sampleMimeType!!.contains("eac3") -> "E-AC3"
  format.sampleMimeType!!.contains("ac3") -> "AC3"
  format.sampleMimeType!!.contains("dts") || format.sampleMimeType!!.contains("vnd.dts") -> "DTS"
  format.sampleMimeType!!.contains("mp4a") || format.sampleMimeType!!.contains("aac") -> "AAC"
  format.sampleMimeType!!.contains("opus") -> "Opus"
  format.sampleMimeType!!.contains("mpeg") -> "MP3"
  format.sampleMimeType!!.contains("flac") -> "FLAC"
  format.sampleMimeType!!.contains("truehd") -> "TrueHD"
  else -> ""
}

private fun channelsName(count: Int): String = when (count) {
  1 -> "Mono"
  2 -> "Stereo"
  Format.NO_VALUE -> ""
  else -> "${count}ch"
}

private fun speedLabel(speed: Float): String = if (speed == 1.0f) "Normal" else "${speed}x"

@OptIn(UnstableApi::class)
@Composable
private fun SettingsPanel(
  modifier: Modifier,
  page: String,
  onNavigate: (String?) -> Unit,
  qualityOptions: List<VideoQuality>,
  activeQualityLabel: String?,
  onSelectQuality: (VideoQuality) -> Unit,
  tracks: Tracks,
  onSelectTrack: (type: Int, group: Tracks.Group?, index: Int) -> Unit,
  playbackSpeed: Float,
  onSelectSpeed: (Float) -> Unit,
  episodes: List<Episode>,
  currentEpisodeId: String?,
  onSelectEpisode: (Episode) -> Unit,
  onSkipIntro: () -> Unit
) {
  val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
  val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }

  // Summary text for the main page
  var audioSummary = "Default"
  audioGroups.forEach { g ->
    for (i in 0 until g.length) {
      if (g.isTrackSelected(i)) {
        val f = g.getTrackFormat(i)
        audioSummary = listOf(audioTrackName(f, i), channelsName(f.channelCount))
          .filter { it.isNotBlank() }
          .joinToString(", ")
      }
    }
  }
  var captionSummary = "Off"
  textGroups.forEach { g ->
    for (i in 0 until g.length) {
      if (g.isTrackSelected(i)) captionSummary = trackName(g.getTrackFormat(i), i)
    }
  }
  val captionsOff = captionSummary == "Off"

  val title = when (page) {
    PAGE_QUALITY -> "Quality"
    PAGE_AUDIO -> "Audio"
    PAGE_CAPTIONS -> "Captions"
    PAGE_SPEED -> "Speed"
    PAGE_EPISODES -> "Episodes"
    else -> "Settings"
  }

  Column(modifier = modifier) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = if (page == PAGE_MAIN) 14.dp else 4.dp, end = 4.dp, top = 4.dp, bottom = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (page != PAGE_MAIN) {
        IconButton(onClick = { onNavigate(PAGE_MAIN) }) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
      }
      Text(
        text = title,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f)
      )
      IconButton(onClick = { onNavigate(null) }) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(28.dp))
      }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      when (page) {
        PAGE_QUALITY -> {
          items(qualityOptions) { option ->
            OptionRow(option.label, option.label == activeQualityLabel) { onSelectQuality(option) }
          }
        }

        PAGE_AUDIO -> {
          // Every audio track in the file (MKV keeps each language in its own group)
          val audioTracks = audioGroups.flatMap { g -> (0 until g.length).map { g to it } }
          if (audioTracks.isEmpty()) {
            item { OptionRow("Default audio", true) { onNavigate(PAGE_MAIN) } }
          } else {
            items(audioTracks.size) { n ->
              val (group, i) = audioTracks[n]
              val f = group.getTrackFormat(i)
              val supported = group.isTrackSupported(i)
              val name = listOf(
                audioTrackName(f, n),
                codecName(f),
                channelsName(f.channelCount),
                if (supported) "" else "not supported"
              ).filter { it.isNotBlank() }.joinToString(", ")
              OptionRow(name, group.isTrackSelected(i)) { onSelectTrack(C.TRACK_TYPE_AUDIO, group, i) }
            }
          }
        }

        PAGE_CAPTIONS -> {
          item { OptionRow("Off", captionsOff) { onSelectTrack(C.TRACK_TYPE_TEXT, null, 0) } }
          textGroups.forEach { group ->
            items((0 until group.length).toList()) { i ->
              OptionRow(trackName(group.getTrackFormat(i), i), group.isTrackSelected(i)) {
                onSelectTrack(C.TRACK_TYPE_TEXT, group, i)
              }
            }
          }
        }

        PAGE_SPEED -> {
          items(SPEEDS) { spd ->
            OptionRow(speedLabel(spd), playbackSpeed == spd) { onSelectSpeed(spd) }
          }
        }

        PAGE_EPISODES -> {
          items(episodes) { ep ->
            OptionRow("S${ep.season} E${ep.episodeNumber} • ${ep.title}", ep.id == currentEpisodeId) {
              onSelectEpisode(ep)
            }
          }
        }

        else -> {
          item {
            SettingsRow(Icons.Default.Hd, "Quality", activeQualityLabel ?: "Auto") { onNavigate(PAGE_QUALITY) }
          }
          item {
            SettingsRow(Icons.Default.VolumeUp, "Audio", audioSummary) { onNavigate(PAGE_AUDIO) }
          }
          item {
            SettingsRow(Icons.Default.ClosedCaption, "Captions", captionSummary) { onNavigate(PAGE_CAPTIONS) }
          }
          item {
            SettingsRow(Icons.Default.Speed, "Speed", speedLabel(playbackSpeed)) { onNavigate(PAGE_SPEED) }
          }
          if (episodes.isNotEmpty()) {
            item {
              val current = episodes.firstOrNull { it.id == currentEpisodeId }
              SettingsRow(
                Icons.Default.ViewList,
                "Episodes",
                current?.let { "S${it.season} E${it.episodeNumber}" } ?: "${episodes.size} episodes"
              ) { onNavigate(PAGE_EPISODES) }
            }
          }
          item {
            SettingsRow(Icons.Default.FastForward, "Skip intro", "+85 seconds", showChevron = false) { onSkipIntro() }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  showChevron: Boolean = true,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(title, color = Color.White, fontSize = 13.sp)
      Text(
        subtitle,
        color = Color.White.copy(alpha = 0.55f),
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    if (showChevron) {
      Icon(
        Icons.Default.ChevronRight,
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.55f),
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
      if (selected) {
        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
      }
    }
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      label,
      color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
      fontSize = 13.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

fun formatTimeMs(millis: Long): String {
  if (millis <= 0) return "00:00"
  val totalSeconds = millis / 1000
  val seconds = totalSeconds % 60
  val minutes = (totalSeconds / 60) % 60
  val hours = totalSeconds / 3600
  return if (hours > 0) {
    String.format("%d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format("%02d:%02d", minutes, seconds)
  }
}

/** Top-level wrapper so AnimatedVisibility resolves without an outer Row/Column receiver. */
@Composable
private fun FadeVisibility(
  visible: Boolean,
  content: @Composable AnimatedVisibilityScope.() -> Unit
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut(),
    content = content
  )
}
