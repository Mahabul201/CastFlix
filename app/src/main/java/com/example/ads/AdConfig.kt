package com.example.ads

/**
 * Monetag ad settings. Sirf yahi file edit karni hai.
 *
 * Monetag dashboard -> In-App -> apna Android source -> Interstitial zone banao,
 * aur uska Zone ID niche daalo.
 */
object AdConfig {
  /** Sab ads ek saath band/chalu karne ka switch. */
  const val ADS_ENABLED = true

  /** Monetag dashboard se mila Interstitial Zone ID. 0L = ads off. */
  const val INTERSTITIAL_ZONE_ID = 0L

  /** Har kitne Play click par 1 interstitial dikhana hai (2 = har 2nd play). */
  const val SHOW_EVERY_N_PLAYS = 2

  /** Do ads ke beech minimum gap (milliseconds). 3 min = 180_000. */
  const val MIN_GAP_MS = 180_000L
}
