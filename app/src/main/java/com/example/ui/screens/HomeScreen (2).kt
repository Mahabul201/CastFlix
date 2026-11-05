package com.example.ui.screens

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Notifications
import com.example.ui.theme.Dimens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.HomeResponse
import com.example.data.model.MediaItem
import com.example.data.repository.StreamRepository
import com.example.ui.components.MediaPosterCard
import com.example.ui.components.RecentlyPlayedCard
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.LocalFireDepartment
import com.example.util.ShareUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  repository: StreamRepository,
  onPlayMedia: (MediaItem, Long) -> Unit,
  onOpenDirectUrlDialog: () -> Unit = {},
  onOpenDetails: (MediaItem) -> Unit = {},
  onOpenHistory: () -> Unit = {},
  onOpenNotifications: () -> Unit = {},
  onOpenSearch: () -> Unit = {},
  updateAvailable: Boolean = false,
  onSeeAllClick: (title: String, kind: String) -> Unit
) {
  var homeData by remember { mutableStateOf<HomeResponse?>(null) }
  var selectedCategory by remember { mutableStateOf("All") }
  val historyList by repository.watchHistory.collectAsState(initial = emptyList())
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  var isRefreshing by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    homeData = repository.fetchHomeData()
  }

  // Unread admin notifications for the bell badge
  var unreadCount by remember { mutableStateOf(0) }
  LaunchedEffect(Unit) {
    val lastRead = repository.getLastReadNotificationAt()
    val list = repository.fetchNotifications()
    unreadCount = if (lastRead < 0L) 0 else list.count { it.createdAt > lastRead }
  }

  val data = homeData

  // Pull down on the home screen to reload everything from the server
  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = {
      coroutineScope.launch {
        isRefreshing = true
        homeData = repository.fetchHomeData()
        val lastRead = repository.getLastReadNotificationAt()
        val list = repository.fetchNotifications()
        unreadCount = if (lastRead < 0L) 0 else list.count { it.createdAt > lastRead }
        isRefreshing = false
      }
    },
    modifier = Modifier.fillMaxSize()
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("home_screen_scroll"),
      contentPadding = PaddingValues(bottom = Dimens.ScreenBottom)
    ) {
      // 0. HEADER: search button + bell, then Movies / Web Series / Anime buttons
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontal)
            .padding(top = 4.dp, bottom = 8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Search button (opens the Search tab)
            Row(
              modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onOpenSearch() }
                .padding(horizontal = 14.dp)
                .testTag("home_search_button"),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                "Search movies, series, anime…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Notification bell with unread badge
            Box {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                  .size(42.dp)
                  .clickable { onOpenNotifications() }
                  .testTag("home_notification_bell")
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }
              val badgeCount = unreadCount + if (updateAvailable) 1 else 0
              if (badgeCount > 0) {
                Box(
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Scrollable tab bar: Trending, Movies, Webseries, ... (admin tags items into these)
          val homeTabs = listOf(
            "Trending", "Movies", "Webseries", "Hollywood", "Bollywood",
            "Anime", "KDrama", "Chinese", "South Indian", "18+"
          )
          LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            items(homeTabs) { label ->
              HomeTypeButton(label = label) {
                onSeeAllClick(label, "tab:$label")
              }
            }
          }
        }
      }

      // 1. TOP BANNER CAROUSEL
      item {
        val featuredList = data?.featured ?: emptyList()
        if (featuredList.isNotEmpty()) {
          val pagerState = rememberPagerState(pageCount = { featuredList.size })

          // Auto-sliding every 5 seconds
          LaunchedEffect(pagerState) {
            while (true) {
              delay(5000)
              val nextPage = (pagerState.currentPage + 1) % featuredList.size
              pagerState.animateScrollToPage(nextPage)
            }
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(310.dp)
          ) {
            HorizontalPager(
              state = pagerState,
              modifier = Modifier.fillMaxSize()
            ) { page ->
              val item = featuredList[page]
              Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                  model = ImageRequest.Builder(LocalContext.current)
                    .data(item.banner.ifBlank { item.poster })
                    .crossfade(true)
                    .build(),
                  contentDescription = item.title,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )

                // Cinematic vignette gradient overlay
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(
                      Brush.verticalGradient(
                        colors = listOf(
                          Color.Black.copy(alpha = 0.5f),
                          Color.Transparent,
                          MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                          MaterialTheme.colorScheme.background
                        )
                      )
                    )
                )

                // Content details & Play button
                Column(
                  modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = Dimens.ScreenHorizontal, end = Dimens.ScreenHorizontal, top = 12.dp, bottom = 26.dp)
                ) {
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                  ) {
                    Text(
                      text = if (item.isSeries) "FEATURED SERIES" else "FEATURED MOVIE",
                      color = Color.White,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }

                  Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                  ) {
                    Icon(
                      Icons.Default.Star,
                      contentDescription = null,
                      tint = Color(0xFFFFB800),
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "${item.rating} • ${item.year} • ${item.category}",
                      color = Color.LightGray,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Medium
                    )
                  }

                  Text(
                    text = item.description,
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 10.dp)
                  )

                  // Action Buttons
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Button(
                      onClick = { onPlayMedia(item, 0L) },
                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                      shape = RoundedCornerShape(24.dp),
                      contentPadding = PaddingValues(horizontal = 12.dp),
                      modifier = Modifier.weight(1f).testTag("banner_play_button")
                    ) {
                      Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Play Now", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                      onClick = {
                        coroutineScope.launch {
                          repository.toggleWatchlist(item, false)
                        }
                      },
                      shape = RoundedCornerShape(24.dp),
                      contentPadding = PaddingValues(horizontal = 12.dp),
                      modifier = Modifier.weight(1f),
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                      Icon(Icons.Default.Add, contentDescription = null)
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("My List", maxLines = 1, softWrap = false)
                    }

                    Surface(
                      shape = CircleShape,
                      color = Color.Black.copy(alpha = 0.45f),
                      modifier = Modifier
                        .size(40.dp)
                        .clickable { ShareUtils.shareMedia(context, item) }
                        .testTag("banner_share_button")
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          Icons.Default.Share,
                          contentDescription = "Share",
                          tint = Color.White,
                          modifier = Modifier.size(20.dp)
                        )
                      }
                    }
                  }
                }
              }
            }

            // Pager dots indicator
            Row(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              repeat(featuredList.size) { index ->
                Box(
                  modifier = Modifier
                    .size(if (pagerState.currentPage == index) 10.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                      if (pagerState.currentPage == index)
                        MaterialTheme.colorScheme.primary
                      else
                        Color.White.copy(alpha = 0.4f)
                    )
                )
              }
            }
          }
        }
      }

      // 2. RECENTLY PLAYED / HISTORY (Horizontal List with progress bar)
      if (historyList.isNotEmpty()) {
        item {
          SectionHeader(
            title = "Recently Played",
            showAction = true,
            actionLabel = "History",
            onAction = onOpenHistory
          )
          LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(historyList) { history ->
              RecentlyPlayedCard(
                history = history,
                onClick = {
                  coroutineScope.launch {
                    val media = repository.getMediaById(history.mediaId)
                    if (media != null) {
                      onPlayMedia(media, history.lastPositionMs)
                    } else {
                      // Custom direct stream
                      val directMedia = MediaItem(
                        id = history.mediaId,
                        title = history.title,
                        description = "Direct stream from history",
                        poster = history.poster,
                        banner = history.poster,
                        category = "Direct Stream",
                        year = 2025,
                        rating = 8.5,
                        streamUrl = history.streamUrl,
                        isSeries = history.isSeries
                      )
                      onPlayMedia(directMedia, history.lastPositionMs)
                    }
                  }
                }
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }
      }

      // 3. CATEGORIES CHIPS
      item {
        val categories = data?.categories ?: listOf("All", "Action", "Sci-Fi", "Comedy", "Drama", "Thriller", "Romance")
        Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
          Text(
            text = "Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontal, vertical = 6.dp)
          )
          LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(categories) { cat ->
              val isSelected = selectedCategory == cat
              ElevatedFilterChip(
                selected = isSelected,
                onClick = { selectedCategory = cat },
                label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.elevatedFilterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = Color.White
                )
              )
            }
          }
        }
      }

      // 4. RECENTLY ADDED (Horizontal List)
      item {
        val recentlyAdded = (data?.recentlyAdded ?: emptyList()).sortedByDescending { it.createdAt }
        val filtered = if (selectedCategory == "All") recentlyAdded else recentlyAdded.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        val displayItems = if (filtered.isEmpty()) recentlyAdded else filtered

        if (displayItems.isNotEmpty()) {
          SectionHeader(title = "Recently Added")
          LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(displayItems) { media ->
              MediaPosterCard(media = media, onClick = { onOpenDetails(media) })
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }
      }

      // 5. MOVIES (Horizontal List with See All)
      item {
        val movies = data?.movies ?: emptyList()
        val filtered = if (selectedCategory == "All") movies else movies.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        val displayMovies = if (filtered.isEmpty()) movies else filtered

        SectionHeader(
          title = "Movies",
          showAction = true,
          actionLabel = "See All",
          onAction = { onSeeAllClick("All Movies", "movies") }
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(displayMovies) { movie ->
            MediaPosterCard(media = movie, onClick = { onOpenDetails(movie) })
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      // 6. WEB SERIES (Horizontal List with See All)
      item {
        val series = data?.series ?: emptyList()
        val filtered = if (selectedCategory == "All") series else series.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        val displaySeries = if (filtered.isEmpty()) series else filtered

        SectionHeader(
          title = "Web Series",
          showAction = true,
          actionLabel = "See All",
          onAction = { onSeeAllClick("All Web Series", "series") }
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(displaySeries) { item ->
            MediaPosterCard(media = item, onClick = { onOpenDetails(item) })
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      // 7. ANIME (own section, shown when the admin has added anime)
      item {
        val anime = data?.anime ?: emptyList()
        val filtered = if (selectedCategory == "All") anime else anime.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        val displayAnime = if (filtered.isEmpty()) anime else filtered

        if (displayAnime.isNotEmpty()) {
          SectionHeader(
            title = "Anime",
            showAction = true,
            actionLabel = "See All",
            onAction = { onSeeAllClick("All Anime", "anime") }
          )
          LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(displayAnime) { item ->
              MediaPosterCard(media = item, onClick = { onOpenDetails(item) })
            }
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun SectionHeader(
  title: String,
  showAction: Boolean = false,
  actionLabel: String = "See All",
  onAction: () -> Unit = {}
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = Dimens.ScreenHorizontal, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )
    if (showAction) {
      TextButton(onClick = onAction) {
        Text(
          text = actionLabel,
          color = MaterialTheme.colorScheme.primary,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

@Composable
private fun HomeTypeButton(
  label: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  // Plain text tab (no icon, no background)
  Text(
    text = label,
    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
    fontSize = 17.sp,
    fontWeight = FontWeight.SemiBold,
    maxLines = 1,
    softWrap = false,
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(vertical = 8.dp)
      .testTag("home_type_$label")
  )
}
