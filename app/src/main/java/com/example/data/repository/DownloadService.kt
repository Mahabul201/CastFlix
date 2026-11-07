package com.example.data.repository

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * Keeps CastFlix alive while files are downloading, so downloads continue when the app is
 * closed / sent to the background / the screen is off. It stops itself when nothing is left.
 */
class DownloadService : Service() {
  private var wakeLock: PowerManager.WakeLock? = null
  private var wifiLock: WifiManager.WifiLock? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      releaseLocks()
      stopForegroundCompat()
      stopSelf()
      return START_NOT_STICKY
    }
    ensureChannel(this)
    val text = intent?.getStringExtra(EXTRA_TEXT) ?: "Downloading..."
    val notification = buildNotification(text)
    if (Build.VERSION.SDK_INT >= 29) {
      startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    } else {
      startForeground(NOTIF_ID, notification)
    }
    acquireLocks()
    return START_STICKY
  }

  private fun buildNotification(text: String): Notification {
    val open = packageManager.getLaunchIntentForPackage(packageName)
    val pi = if (open != null) {
      PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    } else null
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_sys_download)
      .setContentTitle("CastFlix")
      .setContentText(text)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setContentIntent(pi)
      .setProgress(0, 0, true)
      .build()
  }

  private fun acquireLocks() {
    try {
      if (wakeLock == null) {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CastFlix:download").apply {
          setReferenceCounted(false)
          acquire(6 * 60 * 60 * 1000L)
        }
      }
      if (wifiLock == null) {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "CastFlix:download").apply {
          setReferenceCounted(false)
          acquire()
        }
      }
    } catch (_: Exception) {
    }
  }

  private fun releaseLocks() {
    try { wakeLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
    try { wifiLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
    wakeLock = null
    wifiLock = null
  }

  private fun stopForegroundCompat() {
    if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
    else @Suppress("DEPRECATION") stopForeground(true)
  }

  override fun onDestroy() {
    releaseLocks()
    super.onDestroy()
  }

  companion object {
    const val CHANNEL_ID = "castflix_downloads"
    const val NOTIF_ID = 4711
    const val ACTION_STOP = "com.example.action.STOP_DOWNLOAD_SERVICE"
    const val EXTRA_TEXT = "text"

    fun ensureChannel(context: Context) {
      if (Build.VERSION.SDK_INT >= 26) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
          nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
          )
        }
      }
    }

    fun start(context: Context, text: String = "Downloading...") {
      try {
        val i = Intent(context, DownloadService::class.java).putExtra(EXTRA_TEXT, text)
        androidx.core.content.ContextCompat.startForegroundService(context, i)
      } catch (_: Exception) {
        // Background-start limits: downloads still run while the app is open
      }
    }

    fun stop(context: Context) {
      try {
        context.startService(Intent(context, DownloadService::class.java).setAction(ACTION_STOP))
      } catch (_: Exception) {
      }
    }
  }
}
