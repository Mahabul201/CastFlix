package com.example.ui.player

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? = when (this) {
  is LifecycleOwner -> this
  is ContextWrapper -> baseContext.findLifecycleOwner()
  else -> null
}

/**
 * Sound fix: pauses [player] when the app is no longer visible (screen off, app switched, or the
 * picture-in-picture window was closed), so audio never keeps playing on its own.
 * Skipped when "Background play" is turned on in Settings. Not triggered while the video is shown
 * in picture-in-picture, because the app is still visible then.
 */
@Composable
fun PauseWhenAppStops(player: ExoPlayer) {
  val context = LocalContext.current
  val owner = remember(context) { context.findLifecycleOwner() }
  DisposableEffect(owner, player) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP) {
        val keepPlaying = context
          .getSharedPreferences("url_stream_prefs", Context.MODE_PRIVATE)
          .getBoolean("backgroundPlay", false)
        if (!keepPlaying) player.pause()
      }
    }
    owner?.lifecycle?.addObserver(observer)
    onDispose { owner?.lifecycle?.removeObserver(observer) }
  }
}

/**
 * Play/pause button logic. Uses playWhenReady (what the user asked for), not isPlaying, so a tap
 * on Pause while the video is still buffering really pauses it instead of being ignored.
 */
fun togglePlayPause(p: Player) {
  if (p.playWhenReady && p.playbackState != Player.STATE_ENDED) {
    p.pause()
  } else {
    if (p.playbackState == Player.STATE_ENDED) p.seekTo(0L)
    p.play()
  }
}
