package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
  @Query("SELECT * FROM watch_history ORDER BY updatedAt DESC")
  fun getAllHistory(): Flow<List<WatchHistoryEntity>>

  @Query("SELECT * FROM watch_history WHERE mediaId = :mediaId LIMIT 1")
  suspend fun getHistoryForMedia(mediaId: String): WatchHistoryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(history: WatchHistoryEntity)

  @Query("DELETE FROM watch_history WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM watch_history")
  suspend fun clearAll()

  @Query("SELECT COUNT(*) FROM watch_history")
  suspend fun getHistoryCount(): Int

  @Query("SELECT SUM(lastPositionMs) FROM watch_history")
  suspend fun getTotalWatchTimeMs(): Long?
}

@Dao
interface FavoriteDao {
  @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
  fun getAllFavorites(): Flow<List<FavoriteEntity>>

  @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
  fun isFavoriteFlow(mediaId: String): Flow<Boolean>

  @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
  suspend fun isFavorite(mediaId: String): Boolean

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFavorite(favorite: FavoriteEntity)

  @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
  suspend fun removeFavorite(mediaId: String)
}

@Dao
interface WatchlistDao {
  @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
  fun getAllWatchlist(): Flow<List<WatchlistEntity>>

  @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE mediaId = :mediaId)")
  fun isWatchlistFlow(mediaId: String): Flow<Boolean>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWatchlist(watchlist: WatchlistEntity)

  @Query("DELETE FROM watchlist WHERE mediaId = :mediaId")
  suspend fun removeWatchlist(mediaId: String)
}

@Dao
interface DownloadDao {
  @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
  fun getAllDownloads(): Flow<List<DownloadItemEntity>>

  @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
  suspend fun getById(id: String): DownloadItemEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(item: DownloadItemEntity)

  @Query("DELETE FROM downloads WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("UPDATE downloads SET status = :status, progressPercent = :percent, downloadedBytes = :downloaded WHERE id = :id")
  suspend fun updateProgress(id: String, status: String, percent: Int, downloaded: Long)

  @Query("UPDATE downloads SET totalBytes = :total WHERE id = :id")
  suspend fun updateTotal(id: String, total: Long)
}
