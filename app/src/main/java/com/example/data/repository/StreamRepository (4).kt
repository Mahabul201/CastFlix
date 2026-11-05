package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.AppNotification
import com.example.data.model.AppUpdateInfo
import com.example.data.model.Episode
import com.example.data.model.HomeResponse
import com.example.data.model.MediaItem
import com.example.data.model.StreamSettings
import com.example.data.model.UserProfile
import com.example.data.remote.ApiConstants
import com.example.data.remote.StreamApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class StreamRepository(private val context: Context) {
  private val database = AppDatabase.getDatabase(context)
  private val historyDao = database.historyDao()
  private val favoriteDao = database.favoriteDao()
  private val watchlistDao = database.watchlistDao()
  val downloadDao = database.downloadDao()

  private val prefs: SharedPreferences =
    context.getSharedPreferences("url_stream_prefs", Context.MODE_PRIVATE)

  private var apiService = StreamApiService.create(loadSettings().backendUrl)

  // Local cache of media items
  private val _movies = MutableStateFlow<List<MediaItem>>(SeedData.sampleMovies)
  val movies = _movies.asStateFlow()

  private val _series = MutableStateFlow<List<MediaItem>>(SeedData.sampleSeries)
  val series = _series.asStateFlow()

  private val _anime = MutableStateFlow<List<MediaItem>>(emptyList())
  val anime = _anime.asStateFlow()

  // Items the admin tagged per home tab (Trending, Hollywood, ...). An item can be in several.
  private val _tabItems = MutableStateFlow<Map<String, List<MediaItem>>>(emptyMap())
  val tabItems = _tabItems.asStateFlow()

  val watchHistory: Flow<List<WatchHistoryEntity>> = historyDao.getAllHistory()
  val favorites: Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()
  val watchlist: Flow<List<WatchlistEntity>> = watchlistDao.getAllWatchlist()

  private val _settings = MutableStateFlow(loadSettings())
  val settings = _settings.asStateFlow()

  private val _userProfile = MutableStateFlow(loadUserProfile())
  val userProfile = _userProfile.asStateFlow()

  @Volatile private var lastGoodHome: HomeResponse? = null

  suspend fun fetchHomeData(): HomeResponse = withContext(Dispatchers.IO) {
    // The free server can be asleep: retry a few times before giving up
    repeat(3) { attempt ->
      try {
        val remoteHome = apiService.getHome()
        if (remoteHome.movies.isNotEmpty() || remoteHome.series.isNotEmpty() ||
          remoteHome.tabItems.values.any { it.isNotEmpty() }
        ) {
          _movies.value = remoteHome.movies
          _series.value = remoteHome.series
          _anime.value = remoteHome.anime
          _tabItems.value = remoteHome.tabItems
          lastGoodHome = remoteHome
          return@withContext remoteHome
        }
      } catch (_: Exception) {
        // try again
      }
      if (attempt < 2) kotlinx.coroutines.delay(1500L * (attempt + 1))
    }
    // Server not reachable: keep showing the last good data instead of replacing it
    lastGoodHome?.let { return@withContext it }

    // Default Seed Catalog Home
    HomeResponse(
      featured = (_series.value + _movies.value).shuffled().take(4),
      recentlyAdded = (_movies.value.take(3) + _series.value.take(2)).shuffled(),
      movies = _movies.value,
      series = _series.value,
      categories = SeedData.categories
    )
  }

  /** Every item we know about: movies, series, anime and the ones the admin put into home tabs. */
  private fun allKnownItems(): List<MediaItem> =
    (_movies.value + _series.value + _anime.value + _tabItems.value.values.flatten()).distinctBy { it.id }

  /** Filter chip on the Search page: All / Movies / Web Series / Anime or any home tab (Hollywood, Cartoon, ...). */
  private fun matchesSearchFilter(item: MediaItem, filter: String): Boolean {
    val f = filter.lowercase().replace(" ", "")
    return when (f) {
      "all" -> true
      "movies" -> !item.isSeries
      "webseries", "series" -> item.isSeries && !item.isAnime
      "anime" -> item.isAnime
      else -> item.tabs.any { it.lowercase().replace(" ", "") == f } ||
        _tabItems.value.entries.any { (k, list) -> k.lowercase().replace(" ", "") == f && list.any { it.id == item.id } }
    }
  }

  private fun isCoreFilter(filter: String) =
    filter.lowercase().replace(" ", "") in setOf("all", "movies", "webseries", "series", "anime")

  suspend fun searchMedia(query: String, filter: String): List<MediaItem> = withContext(Dispatchers.IO) {
    if (query.isBlank()) {
      return@withContext allKnownItems().filter { matchesSearchFilter(it, filter) }.shuffled()
    }

    try {
      // Custom tab filters are applied here on the phone; the server only knows the core ones
      val results = apiService.search(query, if (filter == "All" || !isCoreFilter(filter)) null else filter)
      val shown = if (isCoreFilter(filter)) results else results.filter { matchesSearchFilter(it, filter) }
      if (shown.isNotEmpty()) return@withContext shown
    } catch (_: Exception) {
      // Local search fallback
    }

    val q = query.trim().lowercase()
    allKnownItems().filter { item ->
      val matchesQuery = item.title.lowercase().contains(q) ||
        item.description.lowercase().contains(q) ||
        item.category.lowercase().contains(q)
      matchesQuery && matchesSearchFilter(item, filter)
    }
  }

  suspend fun getRandomMix(): List<MediaItem> = withContext(Dispatchers.IO) {
    allKnownItems().shuffled().take(6)
  }

  suspend fun getEpisodes(media: MediaItem): List<Episode> = withContext(Dispatchers.IO) {
    if (media.episodes.isNotEmpty()) return@withContext media.episodes
    try {
      apiService.getSeriesEpisodes(media.id)
    } catch (_: Exception) {
      emptyList()
    }
  }

  suspend fun getMediaById(id: String): MediaItem? = withContext(Dispatchers.IO) {
    (_movies.value + _series.value + _anime.value).find { it.id == id }
  }

  // Watch History
  suspend fun saveWatchProgress(
    media: MediaItem,
    positionMs: Long,
    durationMs: Long,
    episode: Episode? = null
  ) = withContext(Dispatchers.IO) {
    val historyId = if (episode != null) "${media.id}_s${episode.season}_e${episode.episodeNumber}" else media.id
    val entity = WatchHistoryEntity(
      id = historyId,
      mediaId = media.id,
      title = media.title,
      poster = episode?.thumbnail?.ifBlank { media.poster } ?: media.poster,
      streamUrl = episode?.streamUrl ?: media.streamUrl,
      isSeries = media.isSeries,
      season = episode?.season ?: 1,
      episodeNumber = episode?.episodeNumber ?: 1,
      episodeTitle = episode?.title ?: "",
      lastPositionMs = positionMs,
      durationMs = durationMs,
      updatedAt = System.currentTimeMillis()
    )
    historyDao.insertOrUpdate(entity)
  }

  suspend fun clearHistory() = withContext(Dispatchers.IO) {
    historyDao.clearAll()
  }

  suspend fun deleteHistoryItem(id: String) = withContext(Dispatchers.IO) {
    historyDao.deleteById(id)
  }

  suspend fun getTotalWatchTimeHours(): Double = withContext(Dispatchers.IO) {
    val totalMs = historyDao.getTotalWatchTimeMs() ?: 0L
    totalMs / (1000.0 * 60 * 60)
  }

  suspend fun getHistoryCount(): Int = withContext(Dispatchers.IO) {
    historyDao.getHistoryCount()
  }

  // Notifications sent from the admin panel
  suspend fun fetchNotifications(): List<AppNotification> = withContext(Dispatchers.IO) {
    try {
      apiService.getNotifications()
    } catch (_: Exception) {
      emptyList()
    }
  }

  /** Same as [fetchNotifications] but returns null when the server could not be reached. */
  suspend fun fetchNotificationsOrNull(): List<AppNotification>? = withContext(Dispatchers.IO) {
    try {
      apiService.getNotifications()
    } catch (_: Exception) {
      null
    }
  }

  /** Anonymous id for this install; created once and kept. */
  private fun deviceId(): String {
    prefs.getString("deviceId", null)?.let { return it }
    val id = java.util.UUID.randomUUID().toString().replace("-", "")
    prefs.edit().putString("deviceId", id).apply()
    return id
  }

  /** Reports one app open to the server (fails silently when offline). */
  suspend fun pingAppOpen() = withContext(Dispatchers.IO) {
    try {
      apiService.ping(
        mapOf("deviceId" to deviceId(), "appVersion" to com.example.BuildConfig.VERSION_CODE.toString())
      ).close()
    } catch (_: Exception) {
    }
  }

  /** Newest published app version, or null when the server could not be reached. */
  suspend fun fetchUpdateInfo(): AppUpdateInfo? = withContext(Dispatchers.IO) {
    try {
      apiService.getUpdate()
    } catch (_: Exception) {
      null
    }
  }

  /** Update version code the user chose to skip ("Later" on a non-forced update). */
  fun getSkippedUpdateCode(): Int = prefs.getInt("skippedUpdateCode", 0)

  fun setSkippedUpdateCode(code: Int) {
    prefs.edit().putInt("skippedUpdateCode", code).apply()
  }

  /** Time (ms) of the newest notification the user has already been told about. */
  fun getLastSeenNotificationAt(): Long = prefs.getLong("lastSeenNotificationAt", -1L)

  fun setLastSeenNotificationAt(timeMs: Long) {
    prefs.edit().putLong("lastSeenNotificationAt", timeMs).apply()
  }

  /** Time (ms) of the newest notification the user opened in the inbox (for the unread badge). */
  fun getLastReadNotificationAt(): Long = prefs.getLong("lastReadNotificationAt", -1L)

  fun setLastReadNotificationAt(timeMs: Long) {
    prefs.edit().putLong("lastReadNotificationAt", timeMs).apply()
  }

  // Preferred video quality (e.g. "720p"); null = not chosen yet
  fun getPreferredQuality(): String? = prefs.getString("preferredQuality", null)

  fun setPreferredQuality(label: String) {
    prefs.edit().putString("preferredQuality", label).apply()
  }

  // Latest saved progress for a title (for a series: the episode watched last)
  suspend fun getLastProgress(mediaId: String): WatchHistoryEntity? = withContext(Dispatchers.IO) {
    historyDao.getHistoryForMedia(mediaId)
  }

  // Favorites
  fun isFavoriteFlow(mediaId: String): Flow<Boolean> = favoriteDao.isFavoriteFlow(mediaId)

  suspend fun toggleFavorite(media: MediaItem) = withContext(Dispatchers.IO) {
    if (favoriteDao.isFavorite(media.id)) {
      favoriteDao.removeFavorite(media.id)
    } else {
      favoriteDao.insertFavorite(
        FavoriteEntity(
          mediaId = media.id,
          title = media.title,
          poster = media.poster,
          banner = media.banner,
          category = media.category,
          year = media.year,
          rating = media.rating,
          streamUrl = media.streamUrl,
          isSeries = media.isSeries
        )
      )
    }
  }

  // Watchlist
  fun isWatchlistFlow(mediaId: String): Flow<Boolean> = watchlistDao.isWatchlistFlow(mediaId)

  suspend fun toggleWatchlist(media: MediaItem, isAdded: Boolean) = withContext(Dispatchers.IO) {
    if (isAdded) {
      watchlistDao.removeWatchlist(media.id)
    } else {
      watchlistDao.insertWatchlist(
        WatchlistEntity(
          mediaId = media.id,
          title = media.title,
          poster = media.poster,
          banner = media.banner,
          category = media.category,
          year = media.year,
          rating = media.rating,
          streamUrl = media.streamUrl,
          isSeries = media.isSeries
        )
      )
    }
  }

  // Settings
  fun getSettings(): StreamSettings = _settings.value

  fun updateSettings(newSettings: StreamSettings) {
    _settings.value = newSettings
    saveSettings(newSettings)
    if (newSettings.backendUrl != apiService.javaClass.name) {
      apiService = StreamApiService.create(newSettings.backendUrl)
    }
  }

  private fun loadSettings(): StreamSettings {
    return StreamSettings(
      themeMode = prefs.getString("themeMode", "DARK") ?: "DARK",
      accentColor = prefs.getString("accentColor", "CINEMA_RED") ?: "CINEMA_RED",
      viewMode = prefs.getString("viewMode", "GRID") ?: "GRID",
      defaultSpeed = prefs.getFloat("defaultSpeed", 1.0f),
      autoplayNext = prefs.getBoolean("autoplayNext", true),
      skipIntro = prefs.getBoolean("skipIntro", true),
      keepScreenOn = prefs.getBoolean("keepScreenOn", true),
      backgroundPlay = prefs.getBoolean("backgroundPlay", false),
      volumeBoost = prefs.getBoolean("volumeBoost", false),
      defaultAspectRatio = prefs.getString("defaultAspectRatio", "FIT") ?: "FIT",
      pipMode = prefs.getBoolean("pipMode", true),
      wifiOnlyDownload = prefs.getBoolean("wifiOnlyDownload", false),
      maxParallelDownloads = prefs.getInt("maxParallelDownloads", 2),
      subtitleSize = prefs.getString("subtitleSize", "MEDIUM") ?: "MEDIUM",
      subtitleColor = prefs.getString("subtitleColor", "WHITE") ?: "WHITE",
      parentalLock = prefs.getBoolean("parentalLock", false),
      parentalPin = prefs.getString("parentalPin", "0000") ?: "0000",
      notificationsEnabled = prefs.getBoolean("notificationsEnabled", true),
      autoCheckUpdate = prefs.getBoolean("autoCheckUpdate", true),
      backendUrl = ApiConstants.DEFAULT_KOYEB_FQDN
    )
  }

  private fun saveSettings(s: StreamSettings) {
    prefs.edit()
      .putString("themeMode", s.themeMode)
      .putString("accentColor", s.accentColor)
      .putString("viewMode", s.viewMode)
      .putFloat("defaultSpeed", s.defaultSpeed)
      .putBoolean("autoplayNext", s.autoplayNext)
      .putBoolean("skipIntro", s.skipIntro)
      .putBoolean("keepScreenOn", s.keepScreenOn)
      .putBoolean("backgroundPlay", s.backgroundPlay)
      .putBoolean("volumeBoost", s.volumeBoost)
      .putString("defaultAspectRatio", s.defaultAspectRatio)
      .putBoolean("pipMode", s.pipMode)
      .putBoolean("wifiOnlyDownload", s.wifiOnlyDownload)
      .putInt("maxParallelDownloads", s.maxParallelDownloads)
      .putString("subtitleSize", s.subtitleSize)
      .putString("subtitleColor", s.subtitleColor)
      .putBoolean("parentalLock", s.parentalLock)
      .putString("parentalPin", s.parentalPin)
      .putBoolean("notificationsEnabled", s.notificationsEnabled)
      .putBoolean("autoCheckUpdate", s.autoCheckUpdate)
      .putString("backendUrl", s.backendUrl)
      .apply()
  }

  // Profile
  fun updateUserProfile(profile: UserProfile) {
    _userProfile.value = profile
    prefs.edit()
      .putString("profile_name", profile.name)
      .putString("profile_username", profile.username)
      .putInt("profile_avatar", profile.avatarIndex)
      .apply()
  }

  private fun loadUserProfile(): UserProfile {
    return UserProfile(
      name = prefs.getString("profile_name", "Movie Fan") ?: "Movie Fan",
      username = prefs.getString("profile_username", "streamer_pro") ?: "streamer_pro",
      avatarIndex = prefs.getInt("profile_avatar", 0)
    )
  }
}
