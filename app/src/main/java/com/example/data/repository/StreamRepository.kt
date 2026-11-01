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

  suspend fun fetchHomeData(): HomeResponse = withContext(Dispatchers.IO) {
    try {
      val remoteHome = apiService.getHome()
      if (remoteHome.movies.isNotEmpty() || remoteHome.series.isNotEmpty()) {
        _movies.value = remoteHome.movies
        _series.value = remoteHome.series
        _anime.value = remoteHome.anime
        _tabItems.value = remoteHome.tabItems
        return@withContext remoteHome
      }
    } catch (_: Exception) {
      // Fallback seamlessly to offline rich seed catalog
    }

    // Default Seed Catalog Home
    HomeResponse(
      featured = (_series.value + _movies.value).shuffled().take(4),
      recentlyAdded = (_movies.value.take(3) + _series.value.take(2)).shuffled(),
      movies = _movies.value,
      series = _series.value,
      categories = SeedData.categories
    )
  }

  suspend fun searchMedia(query: String, filter: String): List<MediaItem> = withContext(Dispatchers.IO) {
    if (query.isBlank()) {
      val combined = (_movies.value + _series.value + _anime.value).shuffled()
      return@withContext when (filter.lowercase()) {
        "movies" -> combined.filter { !it.isSeries }
        "web series", "series" -> combined.filter { it.isSeries && !it.isAnime }
        "anime" -> combined.filter { it.isAnime }
        else -> combined
      }
    }

    try {
      val results = apiService.search(query, if (filter == "All") null else filter)
      if (results.isNotEmpty()) return@withContext results
    } catch (_: Exception) {
      // Local search fallback
    }

    val q = query.trim().lowercase()
    val all = _movies.value + _series.value + _anime.value
    all.filter { item ->
      val matchesQuery = item.title.lowercase().contains(q) ||
        item.description.lowercase().contains(q) ||
        item.category.lowercase().contains(q)
      val matchesFilter = when (filter.lowercase()) {
        "movies" -> !item.isSeries
        "web series", "series" -> item.isSeries && !item.isAnime
        "anime" -> item.isAnime
        else -> true
      }
      matchesQuery && matchesFilter
    }
  }

  suspend fun getRandomMix(): List<MediaItem> = withContext(Dispatchers.IO) {
    (_movies.value + _series.value + _anime.value).shuffled().take(6)
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
  suspend fun getLastProgress(mediaId: String): WatchHistoryEntity? =����������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������
