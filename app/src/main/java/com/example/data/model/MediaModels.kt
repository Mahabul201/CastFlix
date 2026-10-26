package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaItem(
  @Json(name = "id") val id: String,
  @Json(name = "title") val title: String,
  @Json(name = "description") val description: String,
  @Json(name = "poster") val poster: String,
  @Json(name = "banner") val banner: String,
  @Json(name = "category") val category: String,
  @Json(name = "year") val year: Int,
  @Json(name = "rating") val rating: Double,
  @Json(name = "duration") val duration: String = "120m",
  @Json(name = "streamUrl") val streamUrl: String = "",
  @Json(name = "isSeries") val isSeries: Boolean = false,
  @Json(name = "episodes") val episodes: List<Episode> = emptyList(),
  @Json(name = "createdAt") val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class Episode(
  @Json(name = "id") val id: String,
  @Json(name = "seriesId") val seriesId: String,
  @Json(name = "season") val season: Int,
  @Json(name = "episodeNumber") val episodeNumber: Int,
  @Json(name = "title") val title: String,
  @Json(name = "thumbnail") val thumbnail: String,
  @Json(name = "streamUrl") val streamUrl: String,
  @Json(name = "duration") val duration: String = "45m"
)

@JsonClass(generateAdapter = true)
data class HomeResponse(
  @Json(name = "featured") val featured: List<MediaItem>,
  @Json(name = "recentlyAdded") val recentlyAdded: List<MediaItem>,
  @Json(name = "movies") val movies: List<MediaItem>,
  @Json(name = "series") val series: List<MediaItem>,
  @Json(name = "categories") val categories: List<String>
)

data class UserProfile(
  val name: String = "Movie Fan",
  val username: String = "streamer_pro",
  val avatarIndex: Int = 0
)

data class StreamSettings(
  val themeMode: String = "DARK",
  val accentColor: String = "CINEMA_RED",
  val viewMode: String = "GRID",
  val defaultSpeed: Float = 1.0f,
  val autoplayNext: Boolean = true,
  val skipIntro: Boolean = true,
  val keepScreenOn: Boolean = true,
  val backgroundPlay: Boolean = false,
  val volumeBoost: Boolean = false,
  val defaultAspectRatio: String = "FIT",
  val pipMode: Boolean = true,
  val wifiOnlyDownload: Boolean = false,
  val maxParallelDownloads: Int = 2,
  val subtitleSize: String = "MEDIUM",
  val subtitleColor: String = "WHITE",
  val parentalLock: Boolean = false,
  val parentalPin: String = "0000",
  val backendUrl: String = "https://gastric-rasla-evamusic-01582837.koyeb.app"
)
