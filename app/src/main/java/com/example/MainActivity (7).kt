package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.border
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
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
import com.example.data.notify.AppNotifier
import com.example.ui.screens.NotificationsScreen
import com.example.data.model.ResumePoint
import com.example.data.repository.DownloadManagerHelper
import com.example.data.repository.StreamRepository
import com.example.ui.components.DirectUrlStreamDialog
import com.example.ui.components.UpdateDialog
import com.example.data.model.AppUpdateInfo
import com.example.data.update.AppUpdater
import com.example.ui.components.SeeAllScreen
import com.example.ui.components.TabPageScreen
import com.example.ui.player.VideoPlayerScreen
import com.example.ui.screens.DownloadScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AppAccent
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.URLStreamTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  /** Home button / app switch while a video is playing -> floating picture-in-picture window. */
  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    if (com.example.util.PipState.videoActive && com.example.util.PipState.pipEnabled &&
      android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
    ) {
      try {
        enterPictureInPictureMode(
          android.app.PictureInPictureParams.Builder()
            .setAspectRatio(android.util.Rational(16, 9))
            .build()
        )
      } catch (_: Exception) {
      }
    }
  }

  override fun onStop() {
    super.onStop()
    // Close the 18+ folder again when the app goes to the background (not on screen rotation)
    if (!isChangingConfigurations) com.example.util.AdultLock.lock()
  }

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
    com.example.data.auth.CloudSync.start(repository)
    val downloadHelper = DownloadManagerHelper(this)

    // Admin notifications: channel, background check, and permission on Android 13+
    AppNotifier.ensureChannel(this)
    AppNotifier.schedule(this)
    val notificationPermission = registerForActivityResult(
      androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
      androidx.core.content.ContextCompat.checkSelfPermission(
        this, android.Manifest.permission.POST_NOTIFICATIONS
      ) != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
      notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    setContent {
      val settings by repository.settings.collectAsState()
      com.example.util.PipState.pipEnabled = settings.pipMode

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
          AppAccent.ELECTRIC_BLUE
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
  SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
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
  var detailMedia by remember { mutableStateOf<MediaItem?>(null) }
  // Where the watch page should continue (history tap / back from fullscreen)
  var resumePoint by remember { mutableStateOf<ResumePoint?>(null) }
  var showHistory by remember { mutableStateOf(false) }
  var voiceQuery by remember { mutableStateOf<String?>(null) }
  var seeAllTitle by remember { mutableStateOf<String?>(null) }
  var seeAllKind by remember { mutableStateOf("movies") }
  var showNotifications by remember { mutableStateOf(false) }

  // Check for new admin notifications every time the app opens
  val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
  androidx.compose.runtime.LaunchedEffect(Unit) {
    AppNotifier.checkAndNotify(appContext, repository)
  }

  // Count this app open in the admin panel's user stats
  androidx.compose.runtime.LaunchedEffect(Unit) {
    repository.pingAppOpen()
  }

  val coroutineScope = rememberCoroutineScope()

  // App update (published from the admin panel)
  var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
  var showUpdateDialog by remember { mutableStateOf(false) }
  val appSettings by repository.settings.collectAsState()

  androidx.compose.runtime.LaunchedEffect(Unit) {
    if (!appSettings.autoCheckUpdate) return@LaunchedEffect
    val latest = repository.fetchUpdateInfo()
    if (AppUpdater.isNewer(latest)) {
      updateInfo = latest
      // A normal update is offered once per version; a forced one every launch
      if (latest!!.force || latest.versionCode != repository.getSkippedUpdateCode()) {
        showUpdateDialog = true
      }
    }
  }

  // Manual check from Settings
  fun checkForUpdateNow() {
    coroutineScope.launch {
      val latest = repository.fetchUpdateInfo()
      when {
        latest == null -> android.widget.Toast.makeText(
          appContext, "Couldn't reach the server", android.widget.Toast.LENGTH_SHORT
        ).show()
        AppUpdater.isNewer(latest) -> {
          updateInfo = latest
          showUpdateDialog = true
        }
        else -> {
          updateInfo = null
          android.widget.Toast.makeText(
            appContext, "You're on the latest version", android.widget.Toast.LENGTH_SHORT
          ).show()
        }
      }
    }
  }

  // Opens the watch page; it resumes from the saved position on its own
  fun openDetails(media: MediaItem, resume: ResumePoint? = null) {
    resumePoint = resume
    detailMedia = media
  }

  // Fullscreen Video Player Mode
  val playerMedia = activePlayerMedia
  if (playerMedia != null) {
    VideoPlayerScreen(
      media = playerMedia,
      initialEpisode = activePlayerEpisode,
      initialPositionMs = activePlayerPosition,
      onBack = {
        activePlayerMedia = null
      },
      onSaveProgress = { pos, dur, ep ->
        // Remember where we stopped so the watch page continues from here.
        // (playerMedia is captured on purpose: activePlayerMedia is already null when the
        // coroutine below runs after the player closes, which used to drop the save.)
        resumePoint = ResumePoint(
          mediaId = playerMedia.id,
          season = ep?.season,
          episodeNumber = ep?.episodeNumber,
          positionMs = pos,
          durationMs = dur
        )
        coroutineScope.launch {
          repository.saveWatchProgress(playerMedia, pos, dur, ep)
        }
      },
      preferredQuality = repository.getPreferredQuality(),
      onQualityChosen = { repository.setPreferredQuality(it) }
    )
    return
  }

  // Movie / series watch page (checked before History so Back returns to History)
  val watchMedia = detailMedia
  if (watchMedia != null) {
    BackHandler {
      detailMedia = null
      resumePoint = null
    }
    com.example.ui.screens.DetailsScreen(
      media = watchMedia,
      repository = repository,
      downloadHelper = downloadHelper,
      onBack = {
        detailMedia = null
        resumePoint = null
      },
      onPlay = { media, episode, pos ->
        com.example.ads.AdManager.onPlayClicked()
        activePlayerMedia = media
        activePlayerPosition = pos
        activePlayerEpisode = episode
      },
      onOpenMedia = { item -> openDetails(item) },
      onOpenDownloads = {
        detailMedia = null
        resumePoint = null
        currentTab = NavigationTab.DOWNLOAD
      },
      onSaveProgress = { m, pos, dur, ep ->
        coroutineScope.launch { repository.saveWatchProgress(m, pos, dur, ep) }
      },
      resume = resumePoint
    )
    return
  }

  // Notifications inbox
  if (showNotifications) {
    BackHandler { showNotifications = false }
    NotificationsScreen(
      repository = repository,
      onBack = { showNotifications = false },
      updateInfo = updateInfo,
      onUpdateClick = { showUpdateDialog = true }
    )
    if (showUpdateDialog) {
      updateInfo?.let { info ->
        UpdateDialog(info = info, onLater = {
          repository.setSkippedUpdateCode(info.versionCode)
          showUpdateDialog = false
        })
      }
    }
    return
  }

  // History page
  if (showHistory) {
    BackHandler { showHistory = false }
    com.example.ui.screens.HistoryScreen(
      repository = repository,
      onBack = { showHistory = false },
      onOpen = { media, resume -> openDetails(media, resume) }
    )
    return
  }

  // See All Full View Mode
  if (seeAllTitle != null) {
    BackHandler { seeAllTitle = null }
    val movies by repository.movies.collectAsState()
    val series by repository.series.collectAsState()
    val anime by repository.anime.collectAsState()
    val tabItems by repository.tabItems.collectAsState()
    val items = if (seeAllKind.startsWith("tab:")) {
      val tab = seeAllKind.removePrefix("tab:")
      val base = when (tab) {
        "Movies" -> movies
        "Webseries" -> series
        "Anime" -> anime
        else -> emptyList()
      }
      // Items tagged for this tab in the admin panel are merged in (an item can be in several tabs)
      (base + tabItems[tab].orEmpty()).distinctBy { it.id }
    } else when (seeAllKind) {
      "series" -> series
      "anime" -> anime
      else -> movies
    }

    if (seeAllKind.startsWith("tab:")) {
      // Home tab pages (Movies / Webseries / Anime / ...) look like the Home screen
      TabPageScreen(
        title = seeAllTitle!!,
        items = items,
        onBack = { seeAllTitle = null },
        onPlayMedia = { media -> openDetails(media) }
      )
    } else {
      SeeAllScreen(
        title = seeAllTitle!!,
        items = items,
        onBack = { seeAllTitle = null },
        onPlayMedia = { media -> openDetails(media) }
      )
    }
    return
  }

  // Main 5-Tab Scaffolding
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      // Floating "glass" pill: translucent, soft gradient border, content shows through
      val glassShape = RoundedCornerShape(32.dp)
      val surface = MaterialTheme.colorScheme.surface
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 12.dp, vertical = 12.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, glassShape, clip = false)
            .clip(glassShape)
            .background(
              Brush.verticalGradient(
                listOf(
                  surface.copy(alpha = 0.62f),
                  surface.copy(alpha = 0.42f)
                )
              )
            )
            .border(
              1.dp,
              Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f))
              ),
              glassShape
            )
            .testTag("bottom_navigation_bar")
        ) {
          NavigationBar(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0)
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
                    fontSize = 9.sp,
                    maxLines = 1,
                    softWrap = false,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                  unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                  indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                )
              )
            }
          }
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = paddingValues.calculateTopPadding())
    ) {
      AnimatedContent(
        targetState = currentTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "TabContent"
      ) { tab ->
        when (tab) {
          NavigationTab.HOME -> HomeScreen(
            repository = repository,
            onPlayMedia = { media, _ -> openDetails(media) },
            onOpenDetails = { media -> openDetails(media) },
            onOpenHistory = { showHistory = true },
            onOpenNotifications = { showNotifications = true },
            onOpenSearch = { currentTab = NavigationTab.SEARCH },
            onVoiceSearch = { q -> voiceQuery = q; currentTab = NavigationTab.SEARCH },
            updateAvailable = updateInfo != null,
            onSeeAllClick = { title, kind ->
              seeAllTitle = title
              seeAllKind = kind
            }
          )
          NavigationTab.SEARCH -> SearchScreen(
            repository = repository,
            onPlayMedia = { media, _ -> openDetails(media) },
            initialQuery = voiceQuery,
            onInitialQueryUsed = { voiceQuery = null }
          )
          NavigationTab.DOWNLOAD -> DownloadScreen(
            repository = repository,
            downloadHelper = downloadHelper,
            onOpenDetails = { media -> openDetails(media) },
            onPlayMedia = { media, pos ->
              activePlayerMedia = media
              activePlayerPosition = pos
              activePlayerEpisode = media.episodes.firstOrNull()
            }
          )
          NavigationTab.SETTINGS -> SettingsScreen(
            repository = repository,
            updateAvailable = updateInfo != null,
            onCheckUpdate = { checkForUpdateNow() },
            onPlayMedia = { media, _ -> openDetails(media) },
            onOpenHistory = { showHistory = true },
            onOpenNotifications = { showNotifications = true }
          )
        }
      }
    }
  }

  if (showUpdateDialog) {
    updateInfo?.let { info ->
      UpdateDialog(info = info, onLater = {
        repository.setSkippedUpdateCode(info.versionCode)
        showUpdateDialog = false
      })
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
