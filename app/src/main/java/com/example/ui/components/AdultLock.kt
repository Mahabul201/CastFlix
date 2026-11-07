package com.example.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.model.HomeResponse
import com.example.data.model.MediaItem

/**
 * 18+ folder lock.
 *
 * - The folder stays locked until the correct PIN is entered (same PIN as Settings -> Parental PIN lock).
 * - While locked, 18+ titles are also hidden from Trending / Movies / Search "All" etc.
 * - The lock closes again when the app goes to the background or the user leaves the 18+ folder.
 */
object AdultLock {
  const val TAB = "18+"

  /** Compose state: screens recompose automatically when this changes. */
  var unlocked by mutableStateOf(false)
    private set

  fun unlock() { unlocked = true }
  fun lock() { unlocked = false }

  fun isTab(label: String): Boolean = label.replace(" ", "") == TAB

  fun isAdult(item: MediaItem): Boolean =
    item.tabs.any { isTab(it) } || isTab(item.category)
}

/** Same data, with every 18+ title removed (used while the folder is locked). */
fun HomeResponse.withoutAdult(): HomeResponse {
  val adultIds = tabItems.entries
    .filter { AdultLock.isTab(it.key) }
    .flatMap { it.value }
    .map { it.id }
    .toSet()
  fun List<MediaItem>.clean() = filterNot { AdultLock.isAdult(it) || it.id in adultIds }
  return copy(
    featured = featured.clean(),
    recentlyAdded = recentlyAdded.clean(),
    movies = movies.clean(),
    series = series.clean(),
    anime = anime.clean(),
    trending = trending.clean(),
    tabItems = tabItems
      .filterKeys { !AdultLock.isTab(it) }
      .mapValues { (_, list) -> list.clean() }
  )
}
