package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
  @PrimaryKey val id: String, // mediaId or mediaId_season_ep
  val mediaId: String,
  val title: String,
  val poster: String,
  val streamUrl: String,
  val isSeries: Boolean,
  val season: Int = 1,
  val episodeNumber: Int = 1,
  val episodeTitle: String = "",
  val lastPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
  @PrimaryKey val mediaId: String,
  val title: String,
  val poster: String,
  val banner: String,
  val category: String,
  val year: Int,
  val rating: Double,
  val streamUrl: String,
  val isSeries: Boolean,
  val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
  @PrimaryKey val mediaId: String,
  val title: String,
  val poster: String,
  val banner: String,
  val category: String,
  val year: Int,
  val rating: Double,
  val streamUrl: String,
  val isSeries: Boolean,
  val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadItemEntity(
  @PrimaryKey val id: String,
  val mediaId: String,
  val title: String,
  val poster: String,
  val streamUrl: String,
  val localFilePath: String,
  val totalBytes: Long,
  val downloadedBytes: Long,
  val status: String, // DOWNLOADING, COMPLETED, PAUSED, FAILED
  val progressPercent: Int = 0,
  val downloadId: Long = -1L,
  val createdAt: Long = System.currentTimeMillis()
)
