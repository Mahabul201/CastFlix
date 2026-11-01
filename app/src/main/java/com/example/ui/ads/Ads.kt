package com.example.ui.ads

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * AdMob ads for CastFlix.
 *
 * !! Replace the TEST ids below with your own AdMob ids before publishing, and put your
 * AdMob APP id in AndroidManifest.xml (com.google.android.gms.ads.APPLICATION_ID).
 * Using test ids is safe; clicking your own live ads can get the AdMob account banned.
 */
object AdConfig {
  const val ENABLED = false
  const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"        // TEST banner
  const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"  // TEST interstitial

  /** Show an interstitial on every Nth video open, and never more often than this. */
  const val INTERSTITIAL_EVERY_N_PLAYS = 2
  const val INTERSTITIAL_MIN_GAP_MS = 3 * 60 * 1000L
}

object Ads {
  private var initialized = false
  private var interstitial: InterstitialAd? = null
  private var loading = false
  private var playCount = 0
  private var lastShown = 0L

  fun init(context: Context) {
    if (!AdConfig.ENABLED || initialized) return
    initialized = true
    MobileAds.initialize(context.applicationContext) {}
    loadInterstitial(context.applicationContext)
  }

  private fun loadInterstitial(context: Context) {
    if (!AdConfig.ENABLED || loading || interstitial != null) return
    loading = true
    InterstitialAd.load(
      context, AdConfig.INTERSTITIAL_ID, AdRequest.Builder().build(),
      object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) {
          interstitial = ad
          loading = false
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
          interstitial = null
          loading = false
        }
      }
    )
  }

  /**
   * Call when a video is opened. Shows the interstitial when the frequency rules allow it
   * and one is ready; [onClosed] is always called (immediately if nothing is shown).
   * [onShown] is called right before the ad appears so the player can be paused.
   */
  fun maybeShowInterstitial(activity: Activity, onShown: () -> Unit = {}, onClosed: () -> Unit = {}) {
    if (!AdConfig.ENABLED) return onClosed()
    playCount++
    val now = System.currentTimeMillis()
    val ad = interstitial
    val due = playCount % AdConfig.INTERSTITIAL_EVERY_N_PLAYS == 0 &&
      now - lastShown >= AdConfig.INTERSTITIAL_MIN_GAP_MS
    if (ad == null || !due) {
      loadInterstitial(activity)
      return onClosed()
    }
    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() {
        interstitial = null
        lastShown = System.currentTimeMillis()
        loadInterstitial(activity)
        onClosed()
      }

      override fun onAdFailedToShowFullScreenContent(error: AdError) {
        interstitial = null
        loadInterstitial(activity)
        onClosed()
      }
    }
    onShown()
    ad.show(activity)
  }
}

/** Adaptive banner. Collapses to nothing if the ad fails to load. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
  if (!AdConfig.ENABLED) return
  val context = LocalContext.current
  val adView = remember {
    AdView(context).apply {
      val width = (context.resources.displayMetrics.run { widthPixels / density } - 32).toInt()
      setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width))
      adUnitId = AdConfig.BANNER_ID
      loadAd(AdRequest.Builder().build())
    }
  }
  AndroidView(
    modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    factory = { adView }
  )
}
