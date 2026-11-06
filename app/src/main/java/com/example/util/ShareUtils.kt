package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.MediaItem
import com.example.data.remote.ApiConstants
import java.io.File

/** One place for every Share button in the app. Shares the whole app (APK file) + a caption. */
object ShareUtils {

  /** Link friends can open to download the newest CastFlix APK (fallback / caption). */
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

  /** Copies the installed APK into cache/updates (already allowed by file_paths.xml). */
  private fun apkUri(context: Context) = try {
    val dir = File(context.cacheDir, "updates").apply { mkdirs() }
    val out = File(dir, "CastFlix.apk")
    File(context.applicationInfo.sourceDir).inputStream().use { input ->
      out.outputStream().use { input.copyTo(it) }
    }
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", out)
  } catch (_: Exception) {
    null
  }

  private fun send(context: Context, chooserTitle: String, text: String) {
    val uri = apkUri(context)
    val intent = Intent(Intent.ACTION_SEND).apply {
      putExtra(Intent.EXTRA_TEXT, text)
      if (uri != null) {
        type = "application/vnd.android.package-archive"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      } else {
        type = "text/plain" // fallback: link only
      }
    }
    try {
      context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {}
  }
}
