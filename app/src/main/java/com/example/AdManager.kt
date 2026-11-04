package com.example.ads

import android.os.SystemClock
import android.util.Log

/**
 * Interstitial ko kab dikhana hai ye yahi decide karta hai
 * (play count + minimum gap). Ad na mile to chupchap skip, app kabhi nahi atkegi.
 */
object AdManager {
  private const val TAG = "AdManager"

  /** App.kt me set hota hai jab Monetag SDK ready ho. */
  @Volatile var showAction: (() -> Unit)? = null

  private var playCount = 0
  private var lastShownAt = 0L

  /** Movie/episode Play dabane par call karo. */
  fun onPlayClicked() {
    if (!AdConfig.ADS_ENABLED) return
    val show = showAction ?: return

    playCount++
    if (playCount % AdConfig.SHOW_EVERY_N_PLAYS != 0) return

    val now = SystemClock.elapsedRealtime()
    if (lastShownAt != 0L && now - lastShownAt < AdConfig.MIN_GAP_MS) return

    try {
      show()
      lastShownAt = now
    } catch (t: Throwable) {
      Log.w(TAG, "Interstitial skipped: ${t.message}")
    }
  }
}
