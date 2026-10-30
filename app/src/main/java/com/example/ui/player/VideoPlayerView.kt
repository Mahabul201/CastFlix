package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.Episode
import com.example.data.model.MediaItem
import com.example.data.model.buildQualityOptions
import com.example.data.model.hasQualityChoice
import com.example.data.model.pickQuality
import com.example.ui.components.QualityPickerDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

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
  var showQualitySheet by remember { mutableStateOf(false) }
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
  var isFullscreen by remember { mutableStateOf(true) }
  var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

  // Gestures HUD feedback
  var brightnessLevel by remember { mutableFloatStateOf(0.7f) }
  var volumeLevel by remember { mutableFloatStateOf(0.7f) }
  var showBrightnessHud by remember { mutableStateOf(false) }
  var showVolumeHud by remember { mutableStateOf(false) }
  var doubleTapFeedbackText by remember { mutableStateOf<String?>(null) }

  // Sheets
  var showSpeedSheet by remember { mutableStateOf(false) }
  var showEpisodeSheet by remember { mutableStateOf(false) }
  var showTrackSheet by remember { mutableStateOf(false) }
  var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

  val coroutineScope = rememberCoroutineScope()

  // Initialize ExoPlayer
  val exoPlayer = remember {
    ExoPlayer.Builder(context).build().apply {
      playWhenReady = true
      repeatMode = Player.REPEAT_MODE_OFF
    }
  }

  // Load stream: resume on first load, keep position when only the quality changes
  var loadedUrl by remember { mutableStateOf("") }
  var loadedKey by remember { mutableStateOf("") }
  var firstLoadDone by remember { mutableStateOf(false) }
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

  // Back handling
  BackHandler {
    saveNow()
    onBack()
  }

  // Auto-save every 10s while playing
  LaunchedEffect(exoPlayer) {
    while (true) {
      delay(10_000)
      if (exoPlayer.isPlaying) saveNow()
    }
  }

  // Auto-hide controls
  LaunchedEffect(controlsVisible, isPlaying) {
    if (controlsVisible && isPlaying) {
      delay(4500)
      controlsVisible = false
    }
  }

  // Progress ticker & auto save
  LaunchedEffect(exoPlayer) {
    while (true) {
      if (exoPlayer.isPlaying) {
        currentPosition = exoPlayer.currentPosition
        totalDuration = max(0L, exoPlayer.duration)
        isPlaying = true
      }
      delay(500)
    }
  }

  // Lifecycle Dispose
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

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    // Media3 PlayerView with AspectRatio
    AndroidView(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
          detectTapGestures(
            onTap = {
              controlsVisible = !controlsVisible
            },
            onDoubleTap = { offset ->
              val screenWidth = size.width
              if (offset.x < screenWidth * 0.4f) {
                // Seek back 10s
                val newPos = max(0L, exoPlayer.currentPosition - 10000)
                exoPlayer.seekTo(newPos)
                currentPosition = newPos
                doubleTapFeedbackText = "-10s"
                coroutineScope.launch {
                  delay(1000)
                  doubleTapFeedbackText = null
                }
              } else if (offset.x > screenWidth * 0.6f) {
                // Seek forward 10s
                val newPos = min(exoPlayer.duration, exoPlayer.currentPosition + 10000)
                exoPlayer.seekTo(newPos)
                currentPosition = newPos
                doubleTapFeedbackText = "+10s"
                coroutineScope.launch {
                  delay(1000)
                  doubleTapFeedbackText = null
                }
              } else {
                // Toggle play/pause
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

    // Double tap ripple visual feedback
    if (doubleTapFeedbackText != null) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
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

    // Gestures HUD (Brightness / Volume)
    if (showBrightnessHud) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 32.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color.Black.copy(alpha = 0.75f)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.BrightnessMedium, contentDescription = "Brightness", tint = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("${(brightnessLevel * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
          }
        }
      }
    }

    if (showVolumeHud) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 32.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color.Black.copy(alpha = 0.75f)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("${(volumeLevel * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
          }
        }
      }
    }

    // Loading indicator
    if (isBuffering) {
      CircularProgressIndicator(
        modifier = Modifier
          .align(Alignment.Center)
          .size(52.dp),
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 4.dp
      )
    }

    // Controls Overlay
    AnimatedVisibility(
      visible = controlsVisible,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .displayCutoutPadding()
          .background(
            Brush.verticalGradient(
              listOf(
                Color.Black.copy(alpha = 0.75f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.85f)
              )
            )
          )
      ) {
        // Top Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .align(Alignment.TopCenter),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = {
              saveNow()
              onBack()
            },
            modifier = Modifier.testTag("player_back_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = media.title,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (currentEpisode != null) {
              Text(
                text = currentEpisode!!.title,
                color = Color.LightGray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          // Quality button (only when more than one quality exists)
          if (hasQualityChoice(qualityOptions) && activeQuality != null) {
            Text(
              text = activeQuality.label,
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .clickable { showQualitySheet = true }
                .padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }

          // Speed button
          IconButton(onClick = { showSpeedSheet = true }) {
            Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White)
          }

          // Track / Subtitle selector
          IconButton(onClick = { showTrackSheet = true }) {
            Icon(Icons.Default.ClosedCaption, contentDescription = "Tracks", tint = Color.White)
          }

          // Episodes button (if series)
          if (media.isSeries && media.episodes.isNotEmpty()) {
            IconButton(onClick = { showEpisodeSheet = true }) {
              Icon(Icons.Default.ViewList, contentDescription = "Episodes", tint = Color.White)
            }
          }

          // Fullscreen toggle
          IconButton(
            onClick = {
              isFullscreen = !isFullscreen
              activity?.requestedOrientation = if (isFullscreen) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
              } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
              }
            },
            modifier = Modifier.testTag("player_fullscreen_button")
          ) {
            Icon(
              imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
              contentDescription = "Fullscreen",
              tint = Color.White
            )
          }
        }

        // Center Controls: Rewind 10s, Play/Pause, Fast Forward 10s
        Row(
          modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth(0.7f),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = {
              val newPos = max(0L, exoPlayer.currentPosition - 10000)
              exoPlayer.seekTo(newPos)
              currentPosition = newPos
            },
            modifier = Modifier.size(54.dp)
          ) {
            Icon(
              Icons.Default.Replay10,
              contentDescription = "Rewind 10s",
              tint = Color.White,
              modifier = Modifier.size(38.dp)
            )
          }

          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .size(68.dp)
              .clickable {
                if (exoPlayer.isPlaying) {
                  exoPlayer.pause()
                } else {
                  exoPlayer.play()
                }
              }
              .testTag("player_play_pause_button")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(42.dp)
              )
            }
          }

          // Next Episode if series, else skip forward 10s
          val nextEpisode = media.episodes.find {
            currentEpisode != null && it.episodeNumber == currentEpisode!!.episodeNumber + 1 && it.season == currentEpisode!!.season
          }
          if (nextEpisode != null) {
            IconButton(
              onClick = { currentEpisode = nextEpisode },
              modifier = Modifier.size(54.dp)
            ) {
              Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next Episode",
                tint = Color.White,
                modifier = Modifier.size(38.dp)
              )
            }
          } else {
            IconButton(
              onClick = {
                val newPos = min(exoPlayer.duration, exoPlayer.currentPosition + 10000)
                exoPlayer.seekTo(newPos)
                currentPosition = newPos
              },
              modifier = Modifier.size(54.dp)
            ) {
              Icon(
                Icons.Default.FastForward,
                contentDescription = "Forward 10s",
                tint = Color.White,
                modifier = Modifier.size(38.dp)
              )
            }
          }
        }

        // Bottom Controls: Timeline scrubber, duration, aspect ratio toggle
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .align(Alignment.BottomCenter)
        ) {
          // Time labels
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              formatTimeMs(currentPosition),
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              formatTimeMs(totalDuration),
              color = Color.LightGray,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          // Seek Slider
          Slider(
            value = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f,
            onValueChange = { ratio ->
              val targetMs = (ratio * totalDuration).toLong()
              currentPosition = targetMs
              exoPlayer.seekTo(targetMs)
            },
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
          )

          // Extra row buttons: Skip Intro, Aspect Ratio, Speed Indicator
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Skip Intro button (85 seconds)
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = Color.White.copy(alpha = 0.15f),
              modifier = Modifier.clickable {
                val newPos = min(exoPlayer.duration, exoPlayer.currentPosition + 85000)
                exoPlayer.seekTo(newPos)
                currentPosition = newPos
              }
            ) {
              Text(
                text = "Skip Intro (+85s)",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }

            // Aspect ratio toggle
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = Color.White.copy(alpha = 0.15f),
              modifier = Modifier.clickable {
                resizeMode = when (resizeMode) {
                  AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                  AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                  else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
              }
            ) {
              Text(
                text = when (resizeMode) {
                  AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit 16:9"
                  AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Crop Zoom"
                  else -> "Stretch Fill"
                },
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }

            // Speed display
            Text(
              text = "${playbackSpeed}x",
              color = MaterialTheme.colorScheme.primary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    if (showQualitySheet) {
      QualityPickerDialog(
        title = "Video quality",
        options = qualityOptions,
        selectedLabel = activeQuality?.label,
        onSelect = { option ->
          selectedQuality = option.label
          onQualityChosen(option.label)
          showQualitySheet = false
        },
        onDismiss = { showQualitySheet = false }
      )
    }

    // Speed Selector Sheet
    if (showSpeedSheet) {
      ModalBottomSheet(
        onDismissRequest = { showSpeedSheet = false },
        sheetState = rememberModalBottomSheetState()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            "Playback Speed",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(12.dp))
          val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
          speeds.forEach { spd ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  playbackSpeed = spd
                  exoPlayer.setPlaybackSpeed(spd)
                  showSpeedSheet = false
                }
                .padding(vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("${spd}x", fontWeight = if (playbackSpeed == spd) FontWeight.Bold else FontWeight.Normal)
              if (playbackSpeed == spd) {
                Text("Active", color = MaterialTheme.colorScheme.primary)
              }
            }
          }
        }
      }
    }

    // Episode List Sheet
    if (showEpisodeSheet && media.isSeries) {
      ModalBottomSheet(
        onDismissRequest = { showEpisodeSheet = false },
        sheetState = rememberModalBottomSheetState()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
        ) {
          Text(
            "Episodes - ${media.title}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(12.dp))
          LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
            items(media.episodes) { ep ->
              val isCurrent = currentEpisode?.id == ep.id
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    currentEpisode = ep
                    showEpisodeSheet = false
                  }
                  .padding(vertical = 4.dp)
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = if (isCurrent) Icons.Default.PlayArrow else Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      ep.title,
                      fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                      color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text("${ep.duration} • Season ${ep.season}", fontSize = 12.sp, color = Color.Gray)
                  }
                }
              }
            }
          }
        }
      }
    }

    // Tracks / Audio / Subtitle Sheet
    if (showTrackSheet) {
      ModalBottomSheet(
        onDismissRequest = { showTrackSheet = false },
        sheetState = rememberModalBottomSheetState()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            "Audio & Subtitles",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(12.dp))

          Text("Audio Track", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Default Audio (Stereo/Surround)")
            Text("Selected", color = MaterialTheme.colorScheme.primary)
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text("Subtitles", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("English [CC]")
            Text("Available", color = Color.Gray)
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Off")
            Text("Active", color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
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
