package com.example.ui.screens

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Notifications
import com.example.ui.theme.Dimens
import com.example.ui.theme.glass
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.ui.components.AdultLockDialog
import com.example.util.AdultLock
import com.example.util.withoutAdult
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.material.icons.filled.Mic
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
  onVoiceSearch: (String) -> Unit = {},
  updateAvailable: Boolean = false,
  onSeeAllClick: (title: String, kind: String) -> Unit
) {
  // Start from what is already loaded, so coming back to Home never shows the spinner again
  var homeData by remember { mutableStateOf<HomeResponse?>(repository.cachedHome()) }
  var selectedCategory by remember { mutableStateOf("All") }
  var selectedHomeTab by rememberSaveable { mutableStateOf("Trending") }
  var showAdultLock by remember { mutableStateOf(false) }
  // Leaving the 18+ folder locks it again; a locked 18+ folder is never restored/opened directly
  LaunchedEffect(selectedHomeTab) {
    if (!AdultLock.isTab(selectedHomeTab)) AdultLock.lock()
    else if (!AdultLock.unlocked) selectedHomeTab = "Trending"
  }
  if (showAdultLock) {
    AdultLockDialog(
      repository = repository,
      onDismiss = { showAdultLock = false },
      onUnlocked = {
        showAdultLock = false
        selectedHomeTab = AdultLock.TAB
      }
    )
  }
  androidx.activity.compose.BackHandler(enabled = selectedHomeTab != "Trending") { selectedHomeTab = "Trending" }
  val historyList by repository.watchHistory.collectAsState(initial = emptyList())
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  var isRefreshing by remember { mutableStateOf(false) }

  // Load once when the app opens. Later visits reuse the data; it only refreshes quietly
  // in the background when it is older than 15 minutes, or when the user pulls down.
  LaunchedEffect(Unit) {
    if (homeData == null || repository.isHomeStale()) {
      homeData = repository.fetchHomeData()
    }
  }

  // Unread admin notifications for the bell badge
  var unreadCount by remember { mutableStateOf(0) }
  LaunchedEffect(Unit) {
    val lastRead = repository.getLastReadNotificationAt()
    val list = repository.fetchNotifications()
    unreadCount = if (lastRead < 0L) 0 else list.count { it.createdAt > lastRead }
  }

  // New admin notification -> one big popup on the Trending page, closed with ( X )
  var popupNotification by remember { mutableStateOf<com.example.data.model.AppNotification?>(null) }
  LaunchedEffect(Unit) {
    try {
      if (!repository.getSettings().notificationsEnabled) return@LaunchedEffect
      val list = repository.fetchNotifications()
      val newest = list.maxByOrNull { it.createdAt } ?: return@LaunchedEffect
      val lastPopup = repository.getLastPopupNotificationAt()
      val recent = System.currentTimeMillis() - newest.createdAt < 3L * 24 * 60 * 60 * 1000
      if (newest.createdAt > lastPopup && (lastPopup >= 0L || recent)) {
        popupNotification = newest
      } else if (lastPopup < 0L) {
        repository.setLastPopupNotificationAt(newest.createdAt)
      }
    } catch (_: Exception) {
    }
  }
  popupNotification?.let { n ->
    if (selectedHomeTab == "Trending") {
      com.example.ui.components.NotificationPopup(
        item = n,
        onClose = {
          repository.setLastPopupNotificationAt(n.createdAt)
          popupNotification = null
        }
      )
    }
  }

  // While 18+ is locked, 18+ titles are hidden from every row (Trending, Movies, ...)
  val adultUnlocked = AdultLock.unlocked
  val data = remember(homeData, adultUnlocked) { if (adultUnlocked) homeData else homeData?.withoutAdult() }

  // Header (search bar + tabs) floats over the big banner; measure it so the banner reaches under it
  var headerHeightDp by remember { mutableStateOf(110.dp) }
  val density = androidx.compose.ui.platform.LocalDensity.current

  // Everything the server sent (movies + series + anime + trending + admin tab items)
  val allHomeItems = remember(data) {
    if (data == null) emptyList()
    else (data.trending + data.movies + data.series + data.anime + data.tabItems.values.flatten())
      .distinctBy { it.id }
  }
  val homeMovies = remember(data) {
    if (data == null) emptyList()
    else (data.movies + data.tabItems["Movies"].orEmpty()).distinctBy { it.id }
  }
  val homeSeries = remember(data) {
    if (data == null) emptyList()
    else (data.series + data.tabItems["Webseries"].orEmpty()).distinctBy { it.id }
  }

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
    Column(modifier = Modifier.fillMaxSize()) {
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
            // App logo
            androidx.compose.foundation.Image(
              painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo),
              contentDescription = "CastFlix",
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .testTag("home_logo")
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Search button (opens the Search tab)
            Row(
              modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .glass(RoundedCornerShape(21.dp))
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

            // Voice search button (next to the search bar)
            val startVoice = com.example.util.rememberVoiceSearch { onVoiceSearch(it) }
            Surface(
              shape = CircleShape,
              color = Color.Transparent,
              modifier = Modifier
                .size(42.dp)
                .glass(CircleShape)
                .clickable { startVoice() }
                .testTag("home_voice_button")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  Icons.Default.Mic,
                  contentDescription = "Voice search",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Notification bell with unread badge
            Box {
              Surface(
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                  .size(42.dp)
                  .glass(CircleShape)
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

        }

        // Banner area: the tab names float on top of the big banner
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        Column(
          modifier = Modifier
            .align(Alignment.TopStart)
            .zIndex(1f)
            .fillMaxWidth()
            .onSizeChanged { headerHeightDp = with(density) { it.height.toDp() } }
            .background(
              Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.25f), Color.Transparent)
              )
            )
            .padding(horizontal = Dimens.ScreenHorizontal)
            .padding(top = 2.dp, bottom = 8.dp)
        ) {
          // Scrollable tab bar: Universe, Trending, Movies, Webseries, ... (admin tags items into these)
          val homeTabs = listOf(
            "Universe", "Trending", "Movies", "Webseries", "Hollywood", "Bollywood",
            "Anime", "English", "Cartoon", "KDrama", "Chinese", "South Indian", "18+"
          )
          LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            items(homeTabs) { label ->
              val adultLocked = AdultLock.isTab(label) && !AdultLock.unlocked
              HomeTypeButton(label = label, selected = selectedHomeTab == label, locked = adultLocked) {
                if (adultLocked) showAdultLock = true else selectedHomeTab = label
              }
            }
          }
        }

    if (selectedHomeTab != "Trending") {
      val tabList = remember(data, selectedHomeTab) {
        val base = when (selectedHomeTab) {
          "Movies" -> data?.movies
          "Webseries" -> data?.series
          "Anime" -> data?.anime
          else -> null
        }.orEmpty()
        (base + data?.tabItems?.get(selectedHomeTab).orEmpty()).distinctBy { it.id }
      }
      androidx.compose.runtime.key(selectedHomeTab) {
        com.example.ui.components.TabPageContent(
          title = selectedHomeTab,
          items = tabList,
          onPlayMedia = { onOpenDetails(it) },
          modifier = Modifier.fillMaxSize(),
          bannerHeight = 310.dp + headerHeightDp,
          repository = repository
        )
      }
    } else
    if (data == null) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
    } else
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("home_screen_scroll"),
      contentPadding = PaddingValues(bottom = Dimens.ScreenBottom)
    ) {
      // 1. TOP BANNER CAROUSEL
      item {
        // Banner: 2 trending movies + 2 trending web series (most viewed this week, then admin
        // "Trending" tag, then rating), alternating movie / series
        val featuredList = remember(data) {
          val views = data?.weekViews.orEmpty()
          val adminTrending = ((data?.trending ?: emptyList()) + (data?.tabItems?.get("Trending") ?: emptyList()))
            .map { it.id }.toSet()
          fun top(list: List<MediaItem>, n: Int) = list.sortedWith(
            compareByDescending<MediaItem> { views[it.id] ?: 0 }
              .thenByDescending { it.id in adminTrending }
              .thenByDescending { it.rating }
          ).take(n)
          val topMovies = top(allHomeItems.filter { !it.isSeries }, 2)
          val topSeries = top(allHomeItems.filter { it.isSeries && !it.isAnime }, 2)
          val mixed = buildList {
            for (i in 0 until maxOf(topMovies.size, topSeries.size)) {
              topMovies.getOrNull(i)?.let { add(it) }
              topSeries.getOrNull(i)?.let { add(it) }
            }
          }
          if (mixed.isNotEmpty()) mixed
          else {
            val f = data?.featured.orEmpty()
            if (f.isNotEmpty()) f else allHomeItems.sortedByDescending { it.createdAt }.take(5)
          }
        }
        if (featuredList.isNotEmpty()) {
          val pagerState = rememberPagerState(pageCount = { featuredList.size })

          // Auto-sliding every 5 seconds
          LaunchedEffect(pagerState, featuredList.size) {
            while (featuredList.size > 1) {
              delay(5000)
              if (pagerState.isScrollInProgress) continue
              val nextPage = (pagerState.currentPage + 1) % featuredList.size
              pagerState.animateScrollToPage(nextPage)
            }
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(310.dp + headerHeightDp)
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

                    val inList by repository.isWatchlistFlow(item.id).collectAsState(initial = false)
                    OutlinedButton(
                      onClick = {
                        coroutineScope.launch {
                          repository.toggleWatchlist(item, inList)
                          android.widget.Toast.makeText(
                            context,
                            if (inList) "Removed from My List" else "Added to My List",
                            android.widget.Toast.LENGTH_SHORT
                          ).show()
                        }
                      },
                      shape = RoundedCornerShape(24.dp),
                      contentPadding = PaddingValues(horizontal = 12.dp),
                      modifier = Modifier.weight(1f).glass(RoundedCornerShape(24.dp), fill = 0.18f).testTag("banner_mylist_button"),
                      border = null,
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                      Icon(if (inList) Icons.Default.Check else Icons.Default.Add, contentDescription = null)
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(if (inList) "In My List" else "My List", maxLines = 1, softWrap = false)
                    }

                    Surface(
                      shape = CircleShape,
                      color = Color.Transparent,
                      modifier = Modifier
                        .size(40.dp)
                        .glass(CircleShape, fill = 0.18f)
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
        } else {
          Spacer(modifier = Modifier.height(headerHeightDp))
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

      // 2a. AD STRIP (Monetag In-Page Push), under Recently Played
      if (com.example.ads.AdConfig.ADS_ENABLED && com.example.ads.AdConfig.HOME_AD_ENABLED &&
        com.example.ads.AdConfig.INPAGE_ZONE_ID != 0L
      ) {
        item {
          com.example.ads.AdWebView(
            scriptSrc = com.example.ads.AdConfig.INPAGE_SCRIPT_SRC,
            zoneId = com.example.ads.AdConfig.INPAGE_ZONE_ID,
            modifier = Modifier
              .fillMaxWidth()
              .height(com.example.ads.AdConfig.HOME_AD_HEIGHT_DP.dp)
              .padding(horizontal = Dimens.ScreenHorizontal)
          )
          Spacer(modifier = Modifier.height(12.dp))
        }
      }

      // 2b. TRENDING NOW: most watched today; the ranking restarts every day (server counts per day)
      item {
        val trendingItems = remember(data) {
          val adminTrending = ((data?.trending ?: emptyList()) + (data?.tabItems?.get("Trending") ?: emptyList()))
            .map { it.id }.toSet()
          allHomeItems.sortedWith(
            compareByDescending<MediaItem> { data?.todayViews?.get(it.id) ?: 0 }
              .thenByDescending { it.id in adminTrending }
              .thenByDescending { it.rating }
          ).take(15)
        }
        if (trendingItems.isNotEmpty()) {
          val trendingState = androidx.compose.foundation.lazy.rememberLazyListState()
          LaunchedEffect(trendingItems.size) {
            while (trendingItems.size > 1) {
              delay(3500)
              if (!trendingState.isScrollInProgress) {
                val next = (trendingState.firstVisibleItemIndex + 1) % trendingItems.size
                trendingState.animateScrollToItem(next)
              }
            }
          }
          SectionHeader(title = "Trending Now")
          LazyRow(
            state = trendingState,
            contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(trendingItems) { media ->
              MediaPosterCard(media = media, onClick = { onOpenDetails(media) })
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }
      }

      // 3. CATEGORIES: genre chips that filter every row below
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
              val chipShape = RoundedCornerShape(14.dp)
              Box(
                modifier = Modifier
                  .then(
                    if (isSelected) Modifier.clip(chipShape).background(MaterialTheme.colorScheme.primary)
                    else Modifier.glass(chipShape, fill = 0.12f)
                  )
                  .clickable { selectedCategory = cat }
                  .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  cat,
                  color = Color.White,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              }
            }
          }
        }
      }

      // ---- Everything below follows the selected category chip; empty rows are hidden ----
      val weekViews = data?.weekViews.orEmpty()
      val animeAll = (data?.anime.orEmpty() + data?.tabItems?.get("Anime").orEmpty()).distinctBy { it.id }
      val kdramaAll = data?.tabItems?.get("KDrama").orEmpty().distinctBy { it.id }
      fun cat(list: List<MediaItem>) =
        if (selectedCategory == "All") list else list.filter { it.category.equals(selectedCategory, ignoreCase = true) }
      fun newest(list: List<MediaItem>) = cat(list).sortedByDescending { it.createdAt }.take(20)
      fun mostViewed(list: List<MediaItem>) = rankByViews(cat(list), weekViews)

      // 4. RECENTLY ADDED (movies + series + anime)
      posterRowItem("Recently Added", newest(allHomeItems), onOpenDetails = onOpenDetails)

      // 5. MOVIES: recently added movies, trending this week, regional
      posterRowItem(
        "Movies", newest(homeMovies),
        actionLabel = "See All",
        onAction = { onSeeAllClick("All Movies", "movies") },
        onOpenDetails = onOpenDetails
      )
      posterRowItem("Trending Movies · This Week", mostViewed(homeMovies), onOpenDetails = onOpenDetails)
      regionalRowItem(
        title = "Regional Movies",
        regions = REGIONS,
        source = { region -> cat(data?.tabItems?.get(region).orEmpty().filter { !it.isSeries }) },
        onOpenDetails = onOpenDetails
      )

      // 6. WEB SERIES
      groupTitleItem("Web Series")
      posterRowItem(
        "Recently Added", newest(homeSeries),
        actionLabel = "See All",
        onAction = { onSeeAllClick("All Web Series", "series") },
        onOpenDetails = onOpenDetails
      )
      posterRowItem("Most Viewed This Week", mostViewed(homeSeries), onOpenDetails = onOpenDetails)
      regionalRowItem(
        title = "Regional",
        regions = REGIONS,
        source = { region -> cat(data?.tabItems?.get(region).orEmpty().filter { it.isSeries && !it.isAnime }) },
        onOpenDetails = onOpenDetails
      )

      // 7. ANIME: recently added, most viewed this week, new episodes
      groupTitleItem("Anime")
      posterRowItem(
        "Recently Added", newest(animeAll),
        actionLabel = "See All",
        onAction = { onSeeAllClick("All Anime", "anime") },
        onOpenDetails = onOpenDetails
      )
      posterRowItem("Most Viewed This Week", mostViewed(animeAll), onOpenDetails = onOpenDetails)
      posterRowItem(
        "New Episodes",
        cat(animeAll)
          .filter { (data?.episodeUpdated?.get(it.id) ?: 0L) > 0L }
          .sortedByDescending { data?.episodeUpdated?.get(it.id) ?: 0L }
          .take(20),
        onOpenDetails = onOpenDetails
      )

      // 7b. ENGLISH folder: English movies, web series and anime (after Anime, before K-Drama)
      val englishAll = data?.tabItems?.get("English").orEmpty().distinctBy { it.id }
      groupTitleItem("English", visible = cat(englishAll).isNotEmpty())
      posterRowItem(
        "English Movies", newest(englishAll.filter { !it.isSeries && !it.isAnime }),
        actionLabel = "See All",
        onAction = { onSeeAllClick("English", "tab:English") },
        onOpenDetails = onOpenDetails
      )
      posterRowItem("English Web Series", newest(englishAll.filter { it.isSeries && !it.isAnime }), onOpenDetails = onOpenDetails)
      posterRowItem("English Anime", newest(englishAll.filter { it.isAnime }), onOpenDetails = onOpenDetails)

      // 8. K-DRAMA
      groupTitleItem("K-Drama")
      posterRowItem("Recently Added", newest(kdramaAll), onOpenDetails = onOpenDetails)
      posterRowItem("Most Viewed This Week", mostViewed(kdramaAll), onOpenDetails = onOpenDetails)

      // 9. CARTOON (its own folder, with its ad)
      val cartoonAll = data?.tabItems?.get("Cartoon").orEmpty().distinctBy { it.id }
      groupTitleItem("Cartoon", visible = cat(cartoonAll).isNotEmpty())
      posterRowItem("Recently Added", newest(cartoonAll), onOpenDetails = onOpenDetails)
      posterRowItem("Most Viewed This Week", mostViewed(cartoonAll), onOpenDetails = onOpenDetails)

      // 10. SOUTH INDIAN (its own folder, with its ad)
      val southAll = data?.tabItems?.get("South Indian").orEmpty().distinctBy { it.id }
      groupTitleItem("South Indian", visible = cat(southAll).isNotEmpty())
      posterRowItem("Recently Added", newest(southAll), onOpenDetails = onOpenDetails)
      posterRowItem("Most Viewed This Week", mostViewed(southAll), onOpenDetails = onOpenDetails)

      item { Spacer(modifier = Modifier.height(24.dp)) }
    }
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
  selected: Boolean,
  modifier: Modifier = Modifier,
  locked: Boolean = false,
  onClick: () -> Unit
) {
  // Plain text tab: selected one is bold + accent underline
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .testTag("home_type_$label")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
      Text(
        text = label,
        color = if (selected) MaterialTheme.colorScheme.onBackground
        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        fontSize = 17.sp,
        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false
      )
      if (locked) {
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = "Locked",
          tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
          modifier = Modifier.size(14.dp)
        )
      }
    }
    Box(
      modifier = Modifier
        .height(3.dp)
        .width(if (selected) 24.dp else 0.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(MaterialTheme.colorScheme.primary)
    )
  }
}

private val REGIONS = listOf("Bollywood", "Hollywood", "South Indian")

/** Most viewed first (views counted by the server), then by rating, so rows are never empty. */
private fun rankByViews(list: List<MediaItem>, views: Map<String, Int>, limit: Int = 15): List<MediaItem> =
  list.sortedWith(
    compareByDescending<MediaItem> { views[it.id] ?: 0 }.thenByDescending { it.rating }
  ).take(limit)

/** A titled poster row. Nothing is added when [items] is empty. */
private fun androidx.compose.foundation.lazy.LazyListScope.posterRowItem(
  title: String,
  list: List<MediaItem>,
  actionLabel: String? = null,
  onAction: () -> Unit = {},
  onOpenDetails: (MediaItem) -> Unit
) {
  if (list.isEmpty()) return
  item {
    Column {
      SectionHeader(
        title = title,
        showAction = actionLabel != null,
        actionLabel = actionLabel ?: "See All",
        onAction = onAction
      )
      LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(list, key = { it.id }) { media ->
          MediaPosterCard(media = media, onClick = { onOpenDetails(media) })
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

/** A big heading that starts a group of rows (Web Series, Anime, ...). */
private fun androidx.compose.foundation.lazy.LazyListScope.groupTitleItem(title: String, visible: Boolean = true) {
  if (!visible) return
  item {
    Column(modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontal, vertical = 6.dp)) {
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.height(4.dp))
      Box(
        modifier = Modifier
          .width(40.dp)
          .height(3.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(MaterialTheme.colorScheme.primary)
      )
    }
  }
}

/** Row with Bollywood / Hollywood / South Indian chips. Hidden when all three are empty. */
private fun androidx.compose.foundation.lazy.LazyListScope.regionalRowItem(
  title: String,
  regions: List<String>,
  source: (String) -> List<MediaItem>,
  onOpenDetails: (MediaItem) -> Unit
) {
  if (regions.all { source(it).isEmpty() }) return
  item(key = "regional_$title") {
    var selected by rememberSaveable(title) { mutableStateOf(regions.firstOrNull { source(it).isNotEmpty() } ?: regions.first()) }
    val shown = source(selected).sortedByDescending { it.createdAt }
    Column {
      SectionHeader(title = title)
      LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(regions) { region ->
          val isSelected = selected == region
          val chipShape = RoundedCornerShape(14.dp)
          Box(
            modifier = Modifier
              .then(
                if (isSelected) Modifier.clip(chipShape).background(MaterialTheme.colorScheme.primary)
                else Modifier.glass(chipShape, fill = 0.12f)
              )
              .clickable { selected = region }
              .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              region,
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      if (shown.isEmpty()) {
        Text(
          text = "Nothing added in $selected yet",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 12.sp,
          modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontal, vertical = 8.dp)
        )
      } else {
        LazyRow(
          contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(shown, key = { it.id }) { media ->
            MediaPosterCard(media = media, onClick = { onOpenDetails(media) })
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
