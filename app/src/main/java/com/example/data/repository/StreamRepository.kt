package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchlistEntity
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
      val combined = (_movies.value + _series.value).shuffled()
      return@withContext when (filter.lowercase()) {
        "movies" -> combined.filter { !it.isSeries }
        "web series", "series" -> combined.filter { it.isSeries }
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
    val all = _movies.value + _series.value
    all.filter { item ->
      val matchesQuery = item.title.lowercase().contains(q) ||
        item.description.lowercase().contains(q) ||
        item.category.lowercase().contains(q)
      val matchesFilter = when (filter.lowercase()) {
        "movies" -> !item.isSeries
        "web series", "series" -> item.isSeries
        else -> true
      }
      matchesQuery && matchesFilter
    }
  }

  suspend fun getRandomMix(): List<MediaItem> = withContext(Dispatchers.IO) {
    (_movies.value + _series.value).shuffled().take(6)
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
    (_movies.value + _series.value).find { it.id == id }
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
