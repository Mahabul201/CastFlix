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
import kotlinx.coroutines.delay
import com.example.ui.components.MediaPosterCard
import androidx.compose.foundation.lazy.LazyRow
import com.example.ui.theme.glass
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.example.data.repository.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
  repository: StreamRepository,
  downloadHelper: DownloadManagerHelper,
  onPlayMedia: (MediaItem, Long) -> Unit
) {
  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("CastFlix Download", "CastFlix Local File")
  val coroutineScope = rememberCoroutineScope()

  val allDownloads by downloadHelper.allDownloads.collectAsState(initial = emptyList())
  val downloadingItems = allDownloads.filter { it.status == "DOWNLOADING" || it.status == "PENDING" || it.status == "PAUSED" }
  val downloadedItems = allDownloads.filter { it.status == "COMPLETED" }

  var localVideos by remember { mutableStateOf<List<LocalVideoFile>>(emptyList()) }
  var localLoading by remember { mutableStateOf(false) }
  var hasPermission by remember { mutableStateOf(downloadHelper.hasLocalFilesPermission()) }
  var refreshKey by remember { mutableIntStateOf(0) }

  val settings by repository.settings.collectAsState()
  var wifiOnly by remember { mutableStateOf(settings.wifiOnlyDownload) }

  // "For You": a few random titles shown at the end of the page
  var forYou by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
  LaunchedEffect(Unit) {
    var tries = 0
    while (forYou.isEmpty() && tries < 5) {
      forYou = repository.getRandomMix()
      if (forYou.isEmpty()) delay(1500)
      tries++
    }
  }

  // Scan the phone for videos whenever the Local Files tab opens or is refreshed
  LaunchedEffect(selectedTabIndex, refreshKey, hasPermission) {
    if (selectedTabIndex == 1) {
      localLoading = true
      localVideos = withContext(Dispatchers.IO) { downloadHelper.getLocalFiles() }
      localLoading = false
    }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasPermission = granted
    if (granted) refreshKey++
  }
  val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
    android.Manifest.permission.READ_MEDIA_VIDEO
  } else {
    android.Manifest.permission.READ_EXTERNAL_STORAGE
  }

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
      .testTag("download_screen")
  ) {
    Spacer(modifier = Modifier.height(8.dp))

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
            Text(
              title,
              fontSize = 13.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
            )
          }
        )
      }
    }

    // Tab Contents
    when (selectedTabIndex) {
      0 -> DownloadingTab(
        items = downloadingItems,
        completed = downloadedItems,
        onPlayCompleted = { item ->
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
        forYou = forYou,
        onOpenMedia = { onPlayMedia(it, 0L) },
        onPause = { coroutineScope.launch { downloadHelper.pauseDownload(it) } },
        onResume = { coroutineScope.launch { downloadHelper.resumeDownload(it) } },
        onDelete = { coroutineScope.launch { downloadHelper.deleteDownload(it) } }
      )
      1 -> LocalFilesTab(
        files = localVideos,
        loading = localLoading,
        hasPermission = hasPermission,
        onRequestPermission = { permissionLauncher.launch(storagePermission) },
        onRefresh = { refreshKey++ },
        onBrowseFile = { videoPickerLauncher.launch("video/*") },
        onPlay = { file ->
          val media = MediaItem(
            id = "local_${file.path.hashCode()}",
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
  completed: List<DownloadItemEntity> = emptyList(),
  onPlayCompleted: (DownloadItemEntity) -> Unit = {},
  forYou: List<MediaItem> = emptyList(),
  onOpenMedia: (MediaItem) -> Unit = {},
  onPause: (String) -> Unit,
  onResume: (String) -> Unit,
  onDelete: (String) -> Unit
) {
  run {
    LazyColumn(
      contentPadding = PaddingValues(
        start = Dimens.ScreenHorizontal,
        end = Dimens.ScreenHorizontal,
        top = 4.dp,
        bottom = Dimens.ScreenBottom
      ),
      verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)
    ) {
      item { DownloadsAdStrip() }
      if (items.isEmpty() && completed.isEmpty()) {
        item { EmptyDownloadState(message = "No active downloads in progress", compact = true) }
      }
      items(items) { item ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AsyncImage(
              model = item.poster,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
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
                if (item.status == "PENDING") "Pending • waiting for its turn"
                else "${item.progressPercent}% • ${item.status}",
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
            Spacer(modifier = Modifier.width(4.dp))

            // Pause / Resume
            IconButton(
              modifier = Modifier.size(40.dp),
              onClick = {
                if (item.status == "DOWNLOADING" || item.status == "PENDING") onPause(item.id) else onResume(item.id)
              }
            ) {
              Icon(
                imageVector = if (item.status == "DOWNLOADING" || item.status == "PENDING") Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Toggle Pause/Resume",
                tint = Color.White
              )
            }

            // Delete
            IconButton(onClick = { onDelete(item.id) }, modifier = Modifier.size(40.dp)) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
            }
          }
        }
      }
      if (completed.isNotEmpty()) {
        item {
          Text(
            "Complete (${completed.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier
              .padding(top = 8.dp)
              .glass(RoundedCornerShape(14.dp), fill = 0.14f)
              .padding(horizontal = 14.dp, vertical = 7.dp)
          )
        }
        items(completed) { item ->
          CompleteCard(item = item, onPlay = { onPlayCompleted(item) }, onDelete = { onDelete(item.id) })
        }
      }
      item { ForYouSection(forYou, onOpenMedia) }
    }
  }
}

@Composable
fun DownloadedTab(
  items: List<DownloadItemEntity>,
  forYou: List<MediaItem> = emptyList(),
  onOpenMedia: (MediaItem) -> Unit = {},
  onPlay: (DownloadItemEntity) -> Unit,
  onDelete: (String) -> Unit
) {
  run {
    LazyColumn(
      contentPadding = PaddingValues(
        start = Dimens.ScreenHorizontal,
        end = Dimens.ScreenHorizontal,
        top = 4.dp,
        bottom = Dimens.ScreenBottom
      ),
      verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)
    ) {
      item { DownloadsAdStrip() }
      if (items.isEmpty()) {
        item { EmptyDownloadState(message = "No downloaded videos yet", compact = true) }
      }
      items(items) { item ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AsyncImage(
              model = item.poster,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
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
            Spacer(modifier = Modifier.width(4.dp))

            // Play offline
            Button(
              onClick = { onPlay(item) },
              shape = RoundedCornerShape(20.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Play", fontSize = 12.sp)
            }

            IconButton(onClick = { onDelete(item.id) }, modifier = Modifier.size(40.dp)) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
            }
          }
        }
      }
      item { ForYouSection(forYou, onOpenMedia) }
    }
  }
}

@Composable
fun LocalFilesTab(
  files: List<LocalVideoFile>,
  loading: Boolean,
  hasPermission: Boolean,
  onRequestPermission: () -> Unit,
  onRefresh: () -> Unit,
  onBrowseFile: () -> Unit,
  onPlay: (LocalVideoFile) -> Unit
) {
  var query by remember { mutableStateOf("") }
  val shown = remember(files, query) {
    if (query.isBlank()) files else files.filter { it.name.contains(query.trim(), ignoreCase = true) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = Dimens.ScreenHorizontal)
  ) {
    Spacer(modifier = Modifier.height(10.dp))

    // Small filter field + actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier
          .weight(1f)
          .height(40.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Default.Search,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
          if (query.isEmpty()) {
            Text("Search ${files.size} videos", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
      IconButton(onClick = onRefresh) {
        Icon(Icons.Default.Refresh, contentDescription = "Scan storage")
      }
      IconButton(onClick = onBrowseFile) {
        Icon(Icons.Default.FolderOpen, contentDescription = "Pick a video file")
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (!hasPermission) {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            Icons.Default.VideoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text("Show all videos from your phone", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text(
            "Allow access to your videos to list everything stored on this phone. Files stay on your device.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp)
          )
          Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Text("Allow access", fontSize = 13.sp)
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
    }

    when {
      loading && files.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
      shown.isEmpty() -> EmptyDownloadState(
        message = if (query.isBlank()) "No videos found on this phone" else "No video matches \"$query\""
      )
      else -> LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = Dimens.ScreenBottom)
      ) {
        items(shown, key = { it.path }) { file ->
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onPlay(file) }
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  file.name,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  listOf(file.formattedSize, file.formattedDuration).filter { it.isNotBlank() }.joinToString(" • "),
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun EmptyDownloadState(message: String, compact: Boolean = false) {
  Box(
    modifier = (if (compact) Modifier.fillMaxWidth().height(170.dp) else Modifier.fillMaxSize())
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

/** "For You": random movie / series suggestions at the end of the page. */
@Composable
fun ForYouSection(items: List<MediaItem>, onOpen: (MediaItem) -> Unit) {
  if (items.isEmpty()) return
  Column(modifier = Modifier.padding(top = 16.dp)) {
    Text(
      "For You",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      items(items) { media ->
        MediaPosterCard(media = media, onClick = { onOpen(media) })
      }
    }
  }
}

/** A finished download, drawn as a glass card. */
@Composable
fun CompleteCard(
  item: DownloadItemEntity,
  onPlay: () -> Unit,
  onDelete: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .glass(RoundedCornerShape(14.dp), fill = 0.12f)
      .clickable { onPlay() }
      .padding(10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AsyncImage(
      model = item.poster,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp))
    )
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Spacer(modifier = Modifier.height(4.dp))
      Text("Ready for offline play", fontSize = 11.sp, color = Color.LightGray)
    }
    Button(
      onClick = onPlay,
      shape = RoundedCornerShape(20.dp),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
      Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text("Play", fontSize = 12.sp)
    }
    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
      Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
    }
  }
}

/** Ad strip at the top of the Downloads lists. */
@Composable
private fun DownloadsAdStrip() {
  if (com.example.ads.AdConfig.ADS_ENABLED && com.example.ads.AdConfig.DOWNLOAD_AD_ENABLED &&
    com.example.ads.AdConfig.DOWNLOAD_ZONE_ID != 0L
  ) {
    com.example.ads.AdWebView(
      scriptSrc = com.example.ads.AdConfig.DOWNLOAD_SCRIPT_SRC,
      zoneId = com.example.ads.AdConfig.DOWNLOAD_ZONE_ID,
      modifier = Modifier
        .fillMaxWidth()
        .height(com.example.ads.AdConfig.STRIP_AD_HEIGHT_DP.dp)
    )
  }
}
