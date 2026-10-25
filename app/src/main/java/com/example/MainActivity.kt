package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Episode
import com.example.data.model.MediaItem
import com.example.data.repository.DownloadManagerHelper
import com.example.data.repository.StreamRepository
import com.example.ui.components.DirectUrlStreamDialog
import com.example.ui.components.SeeAllScreen
import com.example.ui.player.VideoPlayerScreen
import com.example.ui.screens.DownloadScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AppAccent
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.URLStreamTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    val crashFile = java.io.File(filesDir, "crash.txt")
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      try {
        crashFile.writeText(android.util.Log.getStackTraceString(throwable))
      } catch (_: Throwable) {
      }
      defaultHandler?.uncaughtException(thread, throwable)
    }

    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    if (crashFile.exists()) {
      val report = try { crashFile.readText() } catch (_: Throwable) { "Unreadable crash report" }
      setContent {
        URLStreamTheme {
          CrashReportScreen(
            report = report,
            onCopy = {
              val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
              cm.setPrimaryClip(android.content.ClipData.newPlainText("crash", report))
            },
            onContinue = {
              crashFile.delete()
              recreate()
            }
          )
        }
      }
      return
    }

    val repository = StreamRepository(this)
    val downloadHelper = DownloadManagerHelper(this)

    setContent {
      val settings by repository.settings.collectAsState()

      val themeMode = remember(settings.themeMode) {
        when (settings.themeMode) {
          "AMOLED" -> AppThemeMode.AMOLED
          "LIGHT" -> AppThemeMode.LIGHT
          else -> AppThemeMode.DARK
        }
      }

      val accent = remember(settings.accentColor) {
        try {
          AppAccent.valueOf(settings.accentColor)
        } catch (_: Exception) {
          AppAccent.CINEMA_RED
        }
      }

      URLStreamTheme(themeMode = themeMode, accent = accent) {
        MainAppContent(
          repository = repository,
          downloadHelper = downloadHelper
        )
      }
    }
  }
}

enum class NavigationTab(
  val title: String,
  val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
  val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
  val testTag: String
) {
  HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
  SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search, "tab_search"),
  DOWNLOAD("Download", Icons.Filled.Download, Icons.Outlined.Download, "tab_download"),
  SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings"),
  PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "tab_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
  repository: StreamRepository,
  downloadHelper: DownloadManagerHelper
) {
  var currentTab by rememberSaveable { mutableStateOf(NavigationTab.HOME) }
  var activePlayerMedia by remember { mutableStateOf<MediaItem?>(null) }
  var activePlayerPosition by remember { mutableLongStateOf(0L) }
  var activePlayerEpisode by remember { mutableStateOf<Episode?>(null) }

  var showDirectUrlDialog by remember { mutableStateOf(false) }
  var seeAllTitle by remember { mutableStateOf<String?>(null) }
  var seeAllIsSeries by remember { mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()

  // Fullscreen Video Player Mode
  if (activePlayerMedia != null) {
    VideoPlayerScreen(
      media = activePlayerMedia!!,
      initialEpisode = activePlayerEpisode,
      initialPositionMs = activePlayerPosition,
      onBack = {
        activePlayerMedia = null
      },
      onSaveProgress = { pos, dur, ep ->
        coroutineScope.launch {
          activePlayerMedia?.let {
            repository.saveWatchProgress(it, pos, dur, ep)
          }
        }
      }
    )
    return
  }

  // See All Full View Mode
  if (seeAllTitle != null) {
    BackHandler { seeAllTitle = null }
    val movies by repository.movies.collectAsState()
    val series by repository.series.collectAsState()
    val items = if (seeAllIsSeries) series else movies

    SeeAllScreen(
      title = seeAllTitle!!,
      items = items,
      onBack = { seeAllTitle = null },
      onPlayMedia = { media ->
        activePlayerMedia = media
        activePlayerPosition = 0L
        activePlayerEpisode = media.episodes.firstOrNull()
      }
    )
    return
  }

  // Main 5-Tab Scaffolding
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      NavigationBar(
        modifier = Modifier
          .navigationBarsPadding()
          .testTag("bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
      ) {
        NavigationTab.values().forEach { tab ->
          val isSelected = currentTab == tab
          NavigationBarItem(
            modifier = Modifier.testTag(tab.testTag),
            selected = isSelected,
            onClick = { currentTab = tab },
            icon = {
              Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.title,
                modifier = Modifier.size(24.dp)
              )
            },
            label = {
              Text(
                text = tab.title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
          )
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      AnimatedContent(
        targetState = currentTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "TabContent"
      ) { tab ->
        when (tab) {
          NavigationTab.HOME -> HomeScreen(
            repository = repository,
            onPlayMedia = { media, pos ->
              activePlayerMedia = media
              activePlayerPosition = pos
              activePlayerEpisode = media.episodes.firstOrNull()
            },
            onOpenDirectUrlDialog = { showDirectUrlDialog = true },
            onSeeAllClick = { title, isSeries ->
              seeAllTitle = title
              seeAllIsSeries = isSeries
            }
          )
          NavigationTab.SEARCH -> SearchScreen(
            repository = repository,
            onPlayMedia = { media, pos ->
              activePlayerMedia = media
              activePlayerPosition = pos
              activePlayerEpisode = media.episodes.firstOrNull()
            }
          )
          NavigationTab.DOWNLOAD -> DownloadScreen(
            repository = repository,
            downloadHelper = downloadHelper,
            onPlayMedia = { media, pos ->
              activePlayerMedia = media
              activePlayerPosition = pos
              activePlayerEpisode = media.episodes.firstOrNull()
            }
          )
          NavigationTab.SETTINGS -> SettingsScreen(
            repository = repository
          )
          NavigationTab.PROFILE -> ProfileScreen(
            repository = repository,
            onPlayMedia = { media, pos ->
              activePlayerMedia = media
              activePlayerPosition = pos
              activePlayerEpisode = media.episodes.firstOrNull()
            }
          )
        }
      }
    }

    // Direct URL Dialog
    if (showDirectUrlDialog) {
      DirectUrlStreamDialog(
        onDismiss = { showDirectUrlDialog = false },
        onPlayUrl = { media ->
          activePlayerMedia = media
          activePlayerPosition = 0L
          activePlayerEpisode = null
        }
      )
    }
  }
}


@Composable
fun CrashReportScreen(report: String, onCopy: () -> Unit, onContinue: () -> Unit) {
  androidx.compose.foundation.layout.Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
      .systemBarsPadding()
  ) {
    Text(
      text = "The app crashed last time",
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )
    androidx.compose.foundation.layout.Row(modifier = Modifier.padding(vertical = 8.dp)) {
      androidx.compose.material3.Button(onClick = onCopy) { Text("Copy error") }
      androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
      androidx.compose.material3.OutlinedButton(onClick = onContinue) { Text("Clear and retry") }
    }
    Text(
      text = report,
      fontSize = 11.sp,
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier
        .weight(1f)
        .verticalScroll(androidx.compose.foundation.rememberScrollState())
    )
  }
}
