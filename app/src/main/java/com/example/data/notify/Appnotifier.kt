package com.example.data.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.model.AppNotification
import com.example.data.repository.StreamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Shows the notifications an admin sends from the admin panel.
 * The app checks the server when it opens and every ~15 minutes in the background.
 */
object AppNotifier {
  private const val CHANNEL_ID = "admin_messages"
  private const val WORK_NAME = "admin_notifications"

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Announcements",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply { description = "News and new releases from CastFlix" }
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  /** Background check (Android's minimum period is 15 minutes). */
  fun schedule(context: Context) {
    val request = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
      .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
      .build()
    WorkManager.getInstance(context)
      .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
  }

  /**
   * Fetches notifications and shows the ones the user has not been told about yet.
   * The very first run only records the newest one, so a new install is not flooded
   * with old messages. Returns how many were shown.
   */
  suspend fun checkAndNotify(context: Context, repository: StreamRepository): Int {
    val list = repository.fetchNotifications()
    val lastSeen = repository.getLastSeenNotificationAt()

    if (lastSeen < 0L) {
      val baseline = list.maxOfOrNull { it.createdAt } ?: 0L
      repository.setLastSeenNotificationAt(baseline)
      if (repository.getLastReadNotificationAt() < 0L) repository.setLastReadNotificationAt(baseline)
      return 0
    }

    val fresh = list.filter { it.createdAt > lastSeen }.sortedBy { it.createdAt }
    if (fresh.isEmpty()) return 0
    if (repository.getSettings().notificationsEnabled) {
      fresh.forEach { show(context, it) }
    }
    repository.setLastSeenNotificationAt(fresh.last().createdAt)
    return fresh.size
  }

  private suspend fun show(context: Context, item: AppNotification) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
      PackageManager.PERMISSION_GRANTED
    ) {
      return // still visible in the in-app inbox
    }
    ensureChannel(context)

    val tapIntent = if (item.link.isNotBlank()) {
      Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
    } else {
      Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    val pending = PendingIntent.getActivity(
      context,
      item.id.hashCode(),
      tapIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_notify_chat)
      .setContentTitle(item.title)
      .setContentText(item.message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(item.message))
      .setContentIntent(pending)
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    if (item.imageUrl.isNotBlank()) {
      downloadBitmap(item.imageUrl)?.let { bmp ->
        builder.setLargeIcon(bmp)
        builder.setStyle(
          NotificationCompat.BigPictureStyle()
            .bigPicture(bmp)
            .setSummaryText(item.message)
        )
      }
    }

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(item.id.hashCode(), builder.build())
  }

  private suspend fun downloadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
      val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
      client.newCall(Request.Builder().url(url).build()).execute().use { response ->
        if (!response.isSuccessful) return@withContext null
        response.body?.byteStream()?.let { BitmapFactory.decodeStream(it) }
      }
    } catch (_: Exception) {
      null
    }
  }
}

class NotificationWorker(
  context: Context,
  params: WorkerParameters
) : CoroutineWorker(context, params) {
  override suspend fun doWork(): Result {
    return try {
      AppNotifier.checkAndNotify(applicationContext, StreamRepository(applicationContext))
      Result.success()
    } catch (_: Exception) {
      Result.retry()
    }
  }
}
