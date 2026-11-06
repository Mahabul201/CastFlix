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

  // ---- Monetag website tags, shown inside the app through AdWebView ----

  /**
   * The website address the ad tags run under. Monetag only serves a zone on the website it was
   * created for, so put the SAME address you added in the Monetag dashboard (Websites list),
   * for example "https://your-app.koyeb.app/".
   */
  const val AD_SITE_URL = "https://gastric-rasla-evamusic-01582837.koyeb.app/"

  /** In-Page Push tag, shown as a strip on the Home screen under "Recently Played". 0L = off. */
  const val INPAGE_ZONE_ID = 12203169L
  const val INPAGE_SCRIPT_SRC = "https://nap5k.com/tag.min.js"
  const val HOME_AD_ENABLED = true
  const val HOME_AD_HEIGHT_DP = 120

  /** Vignette Banner tag, shown in the "Hydration break" popup when the video is paused. 0L = off. */
  const val VIGNETTE_ZONE_ID = 12203161L
  const val VIGNETTE_SCRIPT_SRC = "https://n6wxm.com/vignette.min.js"
  const val PAUSE_AD_ENABLED = true

  /** Seconds the video must stay paused before the popup appears. */
  const val PAUSE_AD_DELAY_MS = 1500L

  /** Minimum gap between two pause popups (milliseconds). 2 min = 120_000. */
  const val PAUSE_AD_MIN_GAP_MS = 120_000L

  /** Tag shown on the movie / series page, under the title card (above "For you"). 0L = off. */
  const val DETAILS_ZONE_ID = 12202723L
  const val DETAILS_SCRIPT_SRC = "https://nap5k.com/tag.min.js"
  const val DETAILS_AD_ENABLED = true

  /** Tag shown at the top of the Downloads page (Downloading and Downloaded tabs). 0L = off. */
  const val DOWNLOAD_ZONE_ID = 12203854L
  const val DOWNLOAD_SCRIPT_SRC = "https://nap5k.com/tag.min.js"
  const val DOWNLOAD_AD_ENABLED = true

  /** Height of the in-page ad strips (dp). */
  const val STRIP_AD_HEIGHT_DP = 120

  // ---- Ad on every Home tab page (Movies, Webseries, Anime, ...), under "Recently Added" ----
  const val HOME_ROW_ADS_ENABLED = true

  /** After the "Movies" row. */
  const val HOME_MOVIES_ZONE_ID = 12203927L
  const val HOME_MOVIES_SCRIPT_SRC = "https://nap5k.com/tag.min.js"

  /** After "Web Series > Recently Added". */
  const val HOME_SERIES_ZONE_ID = 12203920L
  const val HOME_SERIES_SCRIPT_SRC = "https://nap5k.com/tag.min.js"

  /** After "Anime > Recently Added". */
  const val HOME_ANIME_ZONE_ID = 12203924L
  const val HOME_ANIME_SCRIPT_SRC = "https://nap5k.com/tag.min.js"

  /** After "K-Drama > Recently Added". */
  const val HOME_KDRAMA_ZONE_ID = 12203925L
  const val HOME_KDRAMA_SCRIPT_SRC = "https://nap5k.com/tag.min.js"

  /** At the very bottom of Home (there is no Cartoon section yet). */
  const val HOME_CARTOON_ZONE_ID = 0L // no ad on the Cartoon tab
  const val HOME_CARTOON_SCRIPT_SRC = "https://nap5k.com/tag.min.js"

  /** After "Regional Movies" (the South Indian row sits in there). */
  const val HOME_SOUTH_ZONE_ID = 12203986L
  const val HOME_SOUTH_SCRIPT_SRC = "https://nap5k.com/tag.min.js"

  /**
   * The ad for a Home tab page, shown under its "Recently Added" row.
   * Returns (zone id, script address), or null when that tab has no ad (18+).
   */
  fun tabAd(tab: String): Pair<Long, String>? = when (tab) {
    "Movies" -> HOME_MOVIES_ZONE_ID to HOME_MOVIES_SCRIPT_SRC
    "Webseries" -> HOME_SERIES_ZONE_ID to HOME_SERIES_SCRIPT_SRC
    "Anime" -> HOME_ANIME_ZONE_ID to HOME_ANIME_SCRIPT_SRC
    "KDrama" -> HOME_KDRAMA_ZONE_ID to HOME_KDRAMA_SCRIPT_SRC
    "Cartoon" -> HOME_CARTOON_ZONE_ID to HOME_CARTOON_SCRIPT_SRC
    "South Indian" -> HOME_SOUTH_ZONE_ID to HOME_SOUTH_SCRIPT_SRC
    "18+" -> null
    // Hollywood, Bollywood, Chinese: use the In-Page Push tag
    else -> INPAGE_ZONE_ID to INPAGE_SCRIPT_SRC
  }
}
