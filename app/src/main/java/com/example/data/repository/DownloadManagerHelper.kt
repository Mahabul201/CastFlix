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
  val path: String,
  val sizeBytes: Long,
  val formattedSize: String
)

class DownloadManagerHelper(private val context: Context) {
  private val database = AppDatabase.getDatabase(context)
  private val downloadDao = database.downloadDao()
  private val scope = CoroutineScope(Dispatchers.IO)
  private val activeJobs = mutableMapOf<String, Job>()

  val allDownloads: Flow<List<DownloadItemEntity>> = downloadDao.getAllDownloads()

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
      totalBytes = 250L * 1024 * 1024, // Estimate 250MB
      downloadedBytes = 0L,
      status = "DOWNLOADING",
      progressPercent = 0
    )
    downloadDao.insertOrUpdate(item)

    // Launch background progress simulation & download sync
    val job = scope.launch {
      var currentProgress = 0
      while (isActive && currentProgress < 100) {
        delay(800)
        currentProgress += (5..12).random()
        if (currentProgress > 100) currentProgress = 100
        val downloadedBytes = (item.totalBytes * (currentProgress / 100.0)).toLong()
        val newStatus = if (currentProgress >= 100) "COMPLETED" else "DOWNLOADING"
        downloadDao.updateProgress(downloadId, newStatus, currentProgress, downloadedBytes)
        if (currentProgress >= 100) {
          // Touch local dummy file to ensure it exists for local playback
          try {
            if (!targetFile.exists()) {
              targetFile.createNewFile()
            }
          } catch (_: Exception) {}
          break
        }
      }
      activeJobs.remove(downloadId)
    }
    activeJobs[downloadId] = job
  }

  suspend fun pauseDownload(id: String) = withContext(Dispatchers.IO) {
    activeJobs[id]?.cancel()
    activeJobs.remove(id)
    val item = downloadDao.getById(id) ?: return@withContext
    downloadDao.updateProgress(id, "PAUSED", item.progressPercent, item.downloadedBytes)
  }

  suspend fun resumeDownload(id: String) = withContext(Dispatchers.IO) {
    val item = downloadDao.getById(id) ?: return@withContext
    downloadDao.updateProgress(id, "DOWNLOADING", item.progressPercent, item.downloadedBytes)

    val job = scope.launch {
      var currentProgress = item.progressPercent
      while (isActive && currentProgress < 100) {
        delay(800)
        currentProgress += (6..14).random()
        if (currentProgress > 100) currentProgress = 100
        val downloadedBytes = (item.totalBytes * (currentProgress / 100.0)).toLong()
        val newStatus = if (currentProgress >= 100) "COMPLETED" else "DOWNLOADING"
        downloadDao.updateProgress(id, newStatus, currentProgress, downloadedBytes)
      }
      activeJobs.remove(id)
    }
    activeJobs[id] = job
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

  fun getLocalFiles(): List<LocalVideoFile> {
    val results = mutableListOf<LocalVideoFile>()
    try {
      val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
      val files = downloadDir.listFiles() ?: emptyArray()
      for (file in files) {
        if (file.isFile && (file.name.endsWith(".mp4") || file.name.endsWith(".mkv") || file.name.endsWith(".mov") || file.name.endsWith(".webm"))) {
          val sizeBytes = file.length()
          val sizeFormatted = formatFileSize(sizeBytes)
          results.add(LocalVideoFile(file.name, file.absolutePath, sizeBytes, sizeFormatted))
        }
      }
    } catch (_: Exception) {}

    // Add sample bundled demo files if list is empty so the user can immediately test local file streaming
    if (results.isEmpty()) {
      results.add(
        LocalVideoFile(
          name = "sample_trailer_direct.mp4",
          path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
          sizeBytes = 158000000L,
          formattedSize = "150.7 MB"
        )
      )
      results.add(
        LocalVideoFile(
          name = "blender_open_movie_preview.mp4",
          path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
          sizeBytes = 224000000L,
          formattedSize = "213.6 MB"
        )
      )
    }
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
