package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.MediaItem
import com.example.data.remote.ApiConstants

/** One place for every Share button in the app. */
object ShareUtils {

  /** Link friends can open to download the newest CastFlix APK. */
  fun appLink(): String = ApiConstants.DEFAULT_KOYEB_FQDN.trimEnd('/') + ApiConstants.SHARE_PATH

  fun shareApp(context: Context) {
    send(
      context,
      "Share CastFlix",
      "Watch movies, web series and anime on CastFlix!\nDownload the app: ${appLink()}"
    )
  }

  fun shareMedia(context: Context, media: MediaItem) {
    val kind = when {
      media.isAnime -> "anime"
      media.isSeries -> "web series"
      else -> "movie"
    }
    send(
      context,
      "Share ${media.title}",
      "Watch ${media.title} (${media.year}) — $kind rated ${media.rating} on CastFlix.\nGet the app: ${appLink()}"
    )
  }

  private fun send(context: Context, chooserTitle: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
      context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {}
  }
}
