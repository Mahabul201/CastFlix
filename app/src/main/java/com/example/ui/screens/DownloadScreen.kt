package com.example.ui.screens

import androidx.compose.foundation.layout.statusBarsPadding
import com.example.ui.theme.Dimens
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.DownloadItemEntity
import com.example.data.model.MediaItem
import com.example.data.repository.DownloadManagerHelper
import com.example.data.repository.LocalVideoFile
import com.example.data.repository.StreamRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
  repository: StreamRepository,
  downloadHelper: DownloadManagerHelper,
  onPlayMedia: (MediaItem, Long) -> Unit
) {
  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("Downloading", "Downloaded", "Local Files")
  val coroutineScope = rememberCoroutineScope()

  val allDownloads by downloadHelper.allDownloads.collectAsState(initial = emptyList())
  val downloadingItems = allDownloads.filter { it.status == "DOWNLOADING" || it.status == "PAUSED" }
  val downloadedItems = allDownloads.filter { it.status == "COMPLETED" }

  var storageInfo by remember { mutableStateOf(downloadHelper.getStorageInfo()) }
  var localVideos by remember { mutableStateOf(downloadHelper.getLocalFiles()) }

  val settings by repository.settings.collectAsState()
  var wifiOnly by remember { mutableStateOf(settings.wifiOnlyDownload) }

  // Activity Result for picking any local video file directly
  val videoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      val directMedia = MediaItem(
        id = "local_${System.currentTimeMillis()}",
        title = it.lastPathSegment ?: "Local Video File",
        description = "Playing local video from device storage",
        poster = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&q=80",
        banner = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1200&q=80",
        category = "Local File",
        year = 2025,
        rating = 8.0,
        streamUrl = it.toString(),
        isSeries = false
      )
      onPlayMedia(directMedia, 0L)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .testTag("download_screen")
  ) {
    // Top Storage and Wi-Fi Control Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.ScreenHorizontal, vertical = Dimens.ScreenTop),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              "Storage Overview",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            val usedMb = downloadHelper.formatFileSize(storageInfo.appDownloadedBytes)
            val freeGb = downloadHelper.formatFileSize(storageInfo.freeBytes)
            Text(
              "Downloads: $usedMb • Free: $freeGb",
              fontSize = 12.sp,
              color = Color.LightGray
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Wifi, contentDescription = "Wi-Fi Only", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Switch(
              checked = wifiOnly,
              onCheckedChange = { checked ->
                wifiOnly = checked
                repository.updateSettings(settings.copy(wifiOnlyDownload = checked))
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val progress = if (storageInfo.totalBytes > 0) {
          (storageInfo.usedBytes.toFloat() / storageInfo.totalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0.5f

        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = MaterialTheme.colorScheme.primary,
          trackColor = Color.DarkGray
        )
      }
    }

    // Tabs
    PrimaryTabRow(
      selectedTabIndex = selectedTabIndex,
      containerColor = MaterialTheme.colorScheme.background,
      contentColor = MaterialTheme.colorScheme.primary
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedTabIndex == index,
          onClick = { selectedTabIndex = index },
          text = {
            val count = when (index) {
              0 -> downloadingItems.size
              1 -> downloadedItems.size
              else -> localVideos.size
            }
            Text("$title ($count)", fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal)
          }
        )
      }
    }

    // Tab Contents
    when (selectedTabIndex) {
      0 -> DownloadingTab(
        items = downloadingItems,
        onPause = { coroutineScope.launch { downloadHelper.pauseDownload(it) } },
        onResume = { coroutineScope.launch { downloadHelper.resumeDownload(it) } },
        onDelete = { coroutineScope.launch { downloadHelper.deleteDownload(it) } }
      )
      1 -> DownloadedTab(
        items = downloadedItems,
        onPlay = { item ->
          val media = MediaItem(
            id = item.mediaId,
            title = item.title,
            description = "Downloaded offline media",
            poster = item.poster,
            banner = item.poster,
            category = "Offline",
            year = 2025,
            rating = 9.0,
            streamUrl = item.localFilePath.ifBlank { item.streamUrl },
            isSeries = false
          )
          onPlayMedia(media, 0L)
        },
        onDelete = { coroutineScope.launch { downloadHelper.deleteDownload(it) } }
      )
      2 -> LocalFilesTab(
        files = localVideos,
        onRefresh = { localVideos = downloadHelper.getLocalFiles() },
        onBrowseFile = { videoPickerLauncher.launch("video/*") },
        onPlay = { file ->
          val media = MediaItem(
            id = "local_${file.name.hashCode()}",
            title = file.name,
            description = "Local media storage (${file.formattedSize})",
            poster = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&q=80",
            banner = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1200&q=80",
            category = "Local Video",
            year = 2025,
            rating = 8.5,
            streamUrl = file.path,
            isSeries = false
          )
          onPlayMedia(media, 0L)
        }
      )
    }
  }
}

@Composable
fun DownloadingTab(
  items: List<DownloadItemEntity>,
  onPause: (String) -> Unit,
  onResume: (String) -> Unit,
  onDelete: (String) -> Unit
) {
  if (items.isEmpty()) {
    EmptyDownloadState(message = "No active downloads in progress")
  } else {
    LazyColumn(
      contentPadding = PaddingValues(
        start = Dimens.ScreenHorizontal,
        end = Dimens.ScreenHorizontal,
        top = 4.dp,
        bottom = Dimens.ScreenBottom
      ),
      verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)
    ) {
      items(items) { item ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AsyncImage(
              model = item.poster,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                "${item.progressPercent}% • ${item.status}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.height(6.dp))
              LinearProgressIndicator(
                progress = { item.progressPercent / 100f },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary
              )
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Pause / Resume
            IconButton(
              onClick = {
                if (item.status == "DOWNLOADING") onPause(item.id) else onResume(item.id)
              }
            ) {
              Icon(
                imageVector = if (item.status == "DOWNLOADING") Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Toggle Pause/Resume",
                tint = Color.White
              )
            }

            // Delete
            IconButton(onClick = { onDelete(item.id) }) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
            }
          }
        }
      }
    }
  }
}

@Composable
fun DownloadedTab(
  items: List<DownloadItemEntity>,
  onPlay: (DownloadItemEntity) -> Unit,
  onDelete: (String) -> Unit
) {
  if (items.isEmpty()) {
    EmptyDownloadState(message = "No downloaded videos yet")
  } else {
    LazyColumn(
      contentPadding = PaddingValues(
        start = Dimens.ScreenHorizontal,
        end = Dimens.ScreenHorizontal,
        top = 4.dp,
        bottom = Dimens.ScreenBottom
      ),
      verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)
    ) {
      items(items) { item ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AsyncImage(
              model = item.poster,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text("Ready for offline play", fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Play offline
            Button(
              onClick = { onPlay(item) },
              shape = RoundedCornerShape(20.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Play", fontSize = 12.sp)
            }

            IconButton(onClick = { onDelete(item.id) }) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
            }
          }
        }
      }
    }
  }
}

@Composable
fun LocalFilesTab(
  files: List<LocalVideoFile>,
  onRefresh: () -> Unit,
  onBrowseFile: () -> Unit,
  onPlay: (LocalVideoFile) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = Dimens.ScreenHorizontal)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Button(
        onClick = onBrowseFile,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
      ) {
        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Browse Device Video")
      }

      IconButton(onClick = onRefresh) {
        Icon(Icons.Default.Refresh, contentDescription = "Scan Storage")
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(bottom = Dimens.ScreenBottom)
    ) {
      items(files) { file ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay(file) }
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  Icons.Default.VideoFile,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                file.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(file.formattedSize, fontSize = 11.sp, color = Color.Gray)
            }
            Button(
              onClick = { onPlay(file) },
              shape = RoundedCornerShape(20.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
              Text("Play", fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
fun EmptyDownloadState(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        Icons.Default.VideoFile,
        contentDescription = null,
        tint = Color.Gray.copy(alpha = 0.4f),
        modifier = Modifier.size(64.dp)
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(message, color = Color.Gray, fontSize = 14.sp)
    }
  }
}
