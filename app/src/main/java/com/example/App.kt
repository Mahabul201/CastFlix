package com.example

import android.app.Application
import co.notix.interstitial.NotixInterstitial
import com.example.ads.AdConfig
import com.example.ads.AdManager

class App : Application() {
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
