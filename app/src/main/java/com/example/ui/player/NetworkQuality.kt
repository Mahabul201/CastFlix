package com.example.ui.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter

/** Current download speed estimate in bits per second (ExoPlayer's own bandwidth meter). */
@OptIn(UnstableApi::class)
fun currentBandwidthBps(context: Context): Long =
  DefaultBandwidthMeter.getSingletonInstance(context.applicationContext).bitrateEstimate

/**
 * The player used by both the watch page and the fullscreen player, so the running video can be
 * handed over from one to the other without restarting (same decoders, same seek behaviour).
 */
@OptIn(UnstableApi::class)
fun buildSharedPlayer(context: Context): androidx.media3.exoplayer.ExoPlayer =
  androidx.media3.exoplayer.ExoPlayer.Builder(
    context,
    androidx.media3.exoplayer.DefaultRenderersFactory(context)
      .setEnableDecoderFallback(true)
      .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
  ).build().apply {
    repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
    setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
  }
  
