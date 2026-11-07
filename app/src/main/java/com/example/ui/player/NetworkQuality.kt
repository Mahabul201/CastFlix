package com.example.ui.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter

/** Current download speed estimate in bits per second (ExoPlayer's own bandwidth meter). */
@OptIn(UnstableApi::class)
fun currentBandwidthBps(context: Context): Long =
  DefaultBandwidthMeter.getSingletonInstance(context.applicationContext).bitrateEstimate
