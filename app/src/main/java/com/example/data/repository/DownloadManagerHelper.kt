package com.example.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadItemEntity
import com.example.data.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class StorageInfo(
  val totalBytes: Long,
  val freeBytes: Long,
  val usedBytes: Long,
  val appDownloadedBytes: Long
)

data class LocalVideoFile(
  val name: String,
  /** content:// uri (phone storage) or absolute file path (app downloads) */
  val path: String,
  val sizeBytes: Long,
  val formattedSize: String,
  val durationMs: Long = 0L,
  val folder: String = ""
) {
  val formattedDuration: String
    get() {
      if (durationMs <= 0L) return ""
      val totalSec = durationMs / 1000
      val h = totalSec / 3600
      val m = (totalSec % 3600) / 60
      val sec = totalSec % 60
      return if (h > 0) String.format("%d:%02d:%02d", h, m, sec) else String.format("%d:%02d", m, sec)
    }
}

class DownloadManagerHelper(private val context: Context) {
  private val database = AppDatabase.getDatabase(context)
  private val downloadDao = database.downloadDao()
  private val scope = CoroutineScope(Dispatchers.IO)
  private val activeJobs = mutableMapOf<String, Job>()

  val allDownloads: Flow<List<DownloadItemEntity>> = downloadDao.getAllDownloads()

  /** Only one file downloads at a time; the rest wait as PENDING. */
  private val queue = kotlinx.coroutines.sync.Semaphore(1)

  private val httpClient = okhttp3.OkHttpClient.Builder()
    .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
    .build()

  suspend fun startDownload(media: MediaItem, customUrl: String? = null) = withContext(Dispatchers.IO) {
    val downloadUrl = customUrl ?: media.streamUrl
    val downloadId = "dl_${media.id}_${System.currentTimeMillis()}"
    val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
    val targetFile = File(downloadDir, "${media.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")}.mp4")

    val item = DownloadItemEntity(
      id = downloadId,
      mediaId = media.id,
      title = media.title,
      poster = media.poster,
      streamUrl = downloadUrl,
      localFilePath = targetFile.absolutePath,
      totalBytes = 0L,
      downloadedBytes = 0L,
      status = "PENDING",
      progressPercent = 0
    )
    downloadDao.insertOrUpdate(item)
    launchDownload(downloadId)
  }

  private fun launchDownload(id: String) {
    activeJobs[id]?.cancel()
    val job = scope.launch {
      queue.acquire()   // waits here (status PENDING) until the previous download is done
      try {
      val item = downloadDao.getById(id) ?: return@launch
      downloadDao.updateProgress(id, "DOWNLOADING", item.progressPercent, item.downloadedBytes)
      val file = File(item.localFilePath)
      try {
        if (item.streamUrl.contains(".m3u8", ignoreCase = true)) {
          // HLS playlists cannot be saved as a single file
          downloadDao.updateProgress(id, "FAILED", 0, 0L)
          return@launch
        }
        val existing = if (file.exists()) file.length() else 0L
        val builder = okhttp3.Request.Builder().url(item.streamUrl)
        if (existing > 0L) builder.header("Range", "bytes=$existing-")

        httpClient.newCall(builder.build()).execute().use { response ->
          if (response.code == 416) {
            downloadDao.updateProgress(id, "COMPLETED", 100, file.length())
            return@launch
          }
          if (!response.isSuccessful) {
            downloadDao.updateProgress(id, "FAILED", item.progressPercent, existing)
            return@launch
          }
          val body = response.body
          if (body == null) {
            downloadDao.updateProgress(id, "FAILED", item.progressPercent, existing)
            return@launch
          }
          val resumed = response.code == 206
          val start = if (resumed) existing else 0L
          val length = body.contentLength()
          val total = if (length > 0L) length + start else 0L
          if (total > 0L) downloadDao.updateTotal(id, total)

          java.io.FileOutputStream(file, resumed).use { out ->
            body.byteStream().use { input ->
              val buffer = ByteArray(64 * 1024)
              var downloaded = start
              var lastUpdate = 0L
              while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                out.write(buffer, 0, read)
                downloaded += read
                ensureActive()
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 500L) {
                  val percent = if (total > 0L) ((downloaded * 100) / total).toInt() else 0
                  downloadDao.updateProgress(id, "DOWNLOADING", percent, downloaded)
                  lastUpdate = now
                }
              }
            }
          }
          downloadDao.updateProgress(id, "COMPLETED", 100, file.length())
        }
      } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
      } catch (e: Exception) {
        val current = downloadDao.getById(id)
        downloadDao.updateProgress(id, "FAILED", current?.progressPercent ?: 0, current?.downloadedBytes ?: 0L)
      }
      } finally {
        queue.release()
      }
    }
    activeJobs[id] = job
  }

  suspend fun pauseDownload(id: String) = withContext(Dispatchers.IO) {
    activeJobs[id]?.cancel()
    activeJobs.remove(id)
    val item = downloadDao.getById(id) ?: return@withContext
    downloadDao.updateProgress(id, "PAUSED", item.progressPercent, item.downloadedBytes)
  }

  suspend fun resumeDownload(id: String) = withContext(Dispatchers.IO) {
    val item = downloadDao.getById(id) ?: return@withContext
    downloadDao.updateProgress(id, "PENDING", item.progressPercent, item.downloadedBytes)
    launchDownload(id)
  }

  suspend fun deleteDownload(id: String) = withContext(Dispatchers.IO) {
    activeJobs[id]?.cancel()
    activeJobs.remove(id)
    val item = downloadDao.getById(id)
    item?.let {
      try {
        val file = File(it.localFilePath)
        if (file.exists()) file.delete()
      } catch (_: Exception) {}
    }
    downloadDao.deleteById(id)
  }

  fun getStorageInfo(): StorageInfo {
    return try {
      val path = Environment.getDataDirectory()
      val stat = StatFs(path.path)
      val blockSize = stat.blockSizeLong
      val totalBlocks = stat.blockCountLong
      val availableBlocks = stat.availableBlocksLong

      val totalBytes = totalBlocks * blockSize
      val freeBytes = availableBlocks * blockSize
      val usedBytes = totalBytes - freeBytes

      val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
      val appDownloadedBytes = downloadDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()

      StorageInfo(
        totalBytes = totalBytes,
        freeBytes = freeBytes,
        usedBytes = usedBytes,
        appDownloadedBytes = appDownloadedBytes
      )
    } catch (_: Exception) {
      StorageInfo(
        totalBytes = 64L * 1024 * 1024 * 1024,
        freeBytes = 32L * 1024 * 1024 * 1024,
        usedBytes = 32L * 1024 * 1024 * 1024,
        appDownloadedBytes = 1024L * 1024 * 350
      )
    }
  }

  /** True when the app may read videos from the phone's storage. */
  fun hasLocalFilesPermission(): Boolean {
    val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      android.Manifest.permission.READ_MEDIA_VIDEO
    } else {
      android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
      android.content.pm.PackageManager.PERMISSION_GRANTED
  }

  /**
   * Every video on the phone (gallery, Downloads, WhatsApp, SD card...) through MediaStore,
   * newest first, plus the videos CastFlix downloaded itself.
   */
  fun getLocalFiles(): List<LocalVideoFile> {
    val results = mutableListOf<LocalVideoFile>()

    if (hasLocalFilesPermission()) {
      try {
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          android.provider.MediaStore.Video.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL)
        } else {
          android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
          android.provider.MediaStore.Video.Media._ID,
          android.provider.MediaStore.Video.Media.DISPLAY_NAME,
          android.provider.MediaStore.Video.Media.SIZE,
          android.provider.MediaStore.Video.Media.DURATION,
          android.provider.MediaStore.Video.Media.DATE_ADDED
        )
        context.contentResolver.query(
          collection,
          projection,
          null,
          null,
          "${android.provider.MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
          val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
          val nameCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DISPLAY_NAME)
          val sizeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.SIZE)
          val durCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DURATION)
          while (cursor.moveToNext()) {
            val size = cursor.getLong(sizeCol)
            if (size <= 0L) continue
            val uri = android.content.ContentUris.withAppendedId(collection, cursor.getLong(idCol))
            results.add(
              LocalVideoFile(
                name = cursor.getString(nameCol) ?: "Video",
                path = uri.toString(),
                sizeBytes = size,
                formattedSize = formatFileSize(size),
                durationMs = cursor.getLong(durCol)
              )
            )
          }
        }
      } catch (_: Exception) {
        // fall through: still show CastFlix's own downloads below
      }
    }

    // Videos CastFlix downloaded itself (readable without any permission)
    try {
      val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
      val known = results.map { it.name }.toSet()
      downloadDir.listFiles()?.forEach { file ->
        val ext = file.extension.lowercase()
        if (file.isFile && ext in setOf("mp4", "mkv", "mov", "webm", "avi", "3gp") && file.name !in known) {
          results.add(LocalVideoFile(file.name, file.absolutePath, file.length(), formatFileSize(file.length())))
        }
      }
    } catch (_: Exception) {}

    return results
  }

  fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb > 1024) {
      String.format("%.1f GB", mb / 1024.0)
    } else {
      String.format("%.1f MB", mb)
    }
  }
}
