package com.example

import android.app.Application
import co.notix.interstitial.NotixInterstitial
import com.example.ads.AdConfig
import com.example.ads.AdManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class App : Application(), ImageLoaderFactory {
  /**
   * Posters are saved in RAM and on the phone's cache folder. After a poster has loaded once,
   * it is never downloaded again (even if the server says "no-cache").
   */
  override fun newImageLoader(): ImageLoader =
    ImageLoader.Builder(this)
      .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
      .diskCache {
        DiskCache.Builder()
          .directory(cacheDir.resolve("poster_cache"))
          .maxSizeBytes(300L * 1024 * 1024)
          .build()
      }
      .respectCacheHeaders(false)
      .crossfade(true)
      .build()

  override fun onCreate() {
    super.onCreate()
    initMonetag()
    com.example.data.auth.AuthManager.init(this)
  }

  private fun initMonetag() {
    if (!AdConfig.ADS_ENABLED || AdConfig.INTERSTITIAL_ZONE_ID == 0L) return
    try {
      val loader = NotixInterstitial.createLoader(zoneId = AdConfig.INTERSTITIAL_ZONE_ID)
      loader.startLoading()
      AdManager.showAction = {
        loader.doOnNextAvailable { result ->
          result?.let { NotixInterstitial.show(it) }
        }
      }
    } catch (t: Throwable) {
      android.util.Log.w("App", "Monetag init failed: ${t.message}")
    }
  }
}
