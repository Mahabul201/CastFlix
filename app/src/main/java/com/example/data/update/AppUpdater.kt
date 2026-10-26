package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/** Checks the version published from the admin panel and installs the new APK. */
object AppUpdater {

  /** True when [info] is a newer build than the one that is installed. */
  fun isNewer(info: AppUpdateInfo?): Boolean =
    info != null && info.versionCode > BuildConfig.VERSION_CODE && info.apkUrl.isNotBlank()

  /**
   * Downloads the APK into the app's own storage and reports progress (0..100).
   * Returns the file, or null when the download failed.
   */
  suspend fun download(
    context: Context,
    info: AppUpdateInfo,
    onProgress: (Int) -> Unit
  ): File? = withContext(Dispatchers.IO) {
    try {
      val dir = File(context.getExternalFilesDir(null) ?: context.cacheDir, "updates").apply { mkdirs() }
      dir.listFiles()?.forEach { it.delete() }
      val target = File(dir, "CastFlix-${info.versionName.ifBlank { info.versionCode.toString() }}.apk")

      val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
      client.newCall(Request.Builder().url(info.apkUrl).build()).execute().use { response ->
        if (!response.isSuccessful) return@withContext null
        val body = response.body ?: return@withContext null
        val total = body.contentLength()
        FileOutputStream(target).use { out ->
          body.byteStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var done = 0L
            var lastPercent = -1
            while (true) {
              val read = input.read(buffer)
              if (read < 0) break
              out.write(buffer, 0, read)
              done += read
              if (total > 0) {
                val percent = ((done * 100) / total).toInt()
                if (percent != lastPercent) {
                  lastPercent = percent
                  onProgress(percent)
                }
              }
            }
          }
        }
      }
      if (target.length() > 0L) target else null
    } catch (_: Exception) {
      null
    }
  }

  /** Android asks once for "install unknown apps" permission; returns false if it is still missing. */
  fun canInstall(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

  fun openInstallPermissionSettings(context: Context) {
    val intent = Intent(
      Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
      Uri.parse("package:${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { context.startActivity(intent) } catch (_: Exception) {}
  }

  /** Opens Android's installer for the downloaded APK. */
  fun install(context: Context, apk: File): Boolean {
    return try {
      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    } catch (_: Exception) {
      false
    }
  }

  /** Fallback: let the browser download the APK. */
  fun openInBrowser(context: Context, url: String) {
    try {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {}
  }
}
