package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    WatchHistoryEntity::class,
    FavoriteEntity::class,
    WatchlistEntity::class,
    DownloadItemEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun historyDao(): HistoryDao
  abstract fun favoriteDao(): FavoriteDao
  abstract fun watchlistDao(): WatchlistDao
  abstract fun downloadDao(): DownloadDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "url_stream_database"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
