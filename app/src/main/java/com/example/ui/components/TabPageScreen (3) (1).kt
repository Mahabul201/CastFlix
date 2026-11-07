package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.model.MediaItem
import com.example.data.repository.StreamRepository
import com.example.util.ShareUtils
import kotlinx.coroutines.launch
import com.example.ui.screens.SectionHeader
import com.example.ui.theme.Dimens
import com.example.ui.theme.glass
import kotlinx.coroutines.delay

/** Names of the home tabs that can also be used as sub-sections (Hollywood, Bollywood, ...). */
private val SUB_SECTION_TABS = listOf(
  "Hollywood", "Bollywood", "English", "South Indian", "KDrama", "Chinese", "Anime", "Cartoon", "Webseries", "Movies", "18+"
)

/**
 * Page opened from the Home tabs (Movies / Webseries / Anime / Hollywood ...).
 * Same look as Home: big banner, "Recently Added", then one row per sub-section.
 */
@Composable
fun TabPageContent(
  title: String,
  items: List<MediaItem>,
  onPlayMedia: (MediaItem) -> Unit,
  modifier: Modifier = Modifier,
  bannerHeight: androidx.compose.ui.unit.Dp = 310.dp,
  repository: StreamRepository? = null
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val sorted = items.sortedByDescending { it.createdAt }
  val featured = sorted.take(5)

  // Sub-sections by admin tab tags (Hollywood, Bollywood, ...) - only items of this page
  val tagSections = SUB_SECTION_TABS
    .filter { !it.equals(title, ignoreCase = true) }
    .map { tag -> tag to sorted.filter { m -> m.tabs.any { it.equals(tag, ignoreCase = true) } } }
    .filter { it.second.isNotEmpty() && it.second.size < sorted.size }

  // Sub-sections by genre (Action, Comedy, ...) when there are enough titles
  val genreSections = sorted
    .groupBy { it.category.ifBlank { "Other" } }
    .filter { it.value.size >= 2 }
    .map { it.key to it.value }

  Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    if (items.isEmpty()) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Nothing here yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("tab_page_scroll"),
        contentPadding = PaddingValues(bottom = Dimens.ScreenBottom)
      ) {
        // 1. Big banner
        item {
          val pagerState = rememberPagerState(pageCount = { featured.size })
          LaunchedEffect(pagerState, featured.size) {
            while (featured.size > 1) {
              delay(5000)
              pagerState.animateScrollToPage((pagerState.currentPage + 1) % featured.size)
            }
          }
          Box(modifier = Modifier.fillMaxWidth().height(bannerHeight)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
              val item = featured[page]
              Box(modifier = Modifier.fillMaxSize().clickable { onPlayMedia(item) }) {
                AsyncImage(
                  model = ImageRequest.Builder(LocalContext.current)
                    .data(item.banner.ifBlank { item.poster })
                    .crossfade(true)
                    .build(),
                  contentDescription = item.title,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(
                      Brush.verticalGradient(
                        listOf(
                          Color.Black.copy(alpha = 0.5f),
                          Color.Transparent,
                          MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                          MaterialTheme.colorScheme.background
                        )
                      )
                    )
                )
                Column(
                  modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                      start = Dimens.ScreenHorizontal,
                      end = Dimens.ScreenHorizontal,
                      bottom = 30.dp
                    )
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
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                  ) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFB800), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                      "${item.rating} • ${item.year} • ${item.category}",
                      color = Color.LightGray,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Medium
                    )
                  }
                  if (item.description.isNotBlank()) {
                    Text(
                      text = item.description,
                      color = Color(0xFFCBD5E1),
                      fontSize = 12.sp,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.padding(bottom = 10.dp)
                    )
                  } else {
                    Spacer(Modifier.height(6.dp))
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Button(
                      onClick = { onPlayMedia(item) },
                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                      shape = RoundedCornerShape(24.dp),
                      contentPadding = PaddingValues(horizontal = 12.dp),
                      modifier = Modifier.weight(1f).testTag("tab_page_play")
                    ) {
                      Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                      Spacer(Modifier.width(6.dp))
                      Text("Play Now", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (repository != null) {
                      val inList by repository.isWatchlistFlow(item.id).collectAsState(initial = false)
                      OutlinedButton(
                        onClick = {
                          scope.launch {
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
                        modifier = Modifier.weight(1f).glass(RoundedCornerShape(24.dp), fill = 0.18f).testTag("tab_page_mylist"),
                        border = null,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                      ) {
                        Icon(if (inList) Icons.Default.Check else Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (inList) "In My List" else "My List", maxLines = 1, softWrap = false)
                      }
                    }

                    Surface(
                      shape = CircleShape,
                      color = Color.Transparent,
                      modifier = Modifier
                        .size(40.dp)
                        .glass(CircleShape, fill = 0.18f)
                        .clickable { ShareUtils.shareMedia(context, item) }
                        .testTag("tab_page_share")
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Share, "Share", tint = Color.White, modifier = Modifier.size(20.dp))
                      }
                    }
                  }
                }
              }
            }
            if (featured.size > 1) {
              Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                repeat(featured.size) { index ->
                  Box(
                    modifier = Modifier
                      .size(if (pagerState.currentPage == index) 10.dp else 6.dp)
                      .clip(CircleShape)
                      .background(
                        if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.4f)
                      )
                  )
                }
              }
            }
          }
        }

        // 2. Recently added
        item {
          SectionHeader(title = "Recently Added")
          PosterRow(sorted.take(20), onPlayMedia)
        }

        // Ad for this tab, under "Recently Added"
        val tabAd = if (com.example.ads.AdConfig.ADS_ENABLED && com.example.ads.AdConfig.HOME_ROW_ADS_ENABLED) {
          com.example.ads.AdConfig.tabAd(title)
        } else null
        if (tabAd != null && tabAd.first != 0L) {
          item {
            com.example.ads.AdWebView(
              scriptSrc = tabAd.second,
              zoneId = tabAd.first,
              modifier = Modifier
                .fillMaxWidth()
                .height(com.example.ads.AdConfig.STRIP_AD_HEIGHT_DP.dp)
                .padding(horizontal = Dimens.ScreenHorizontal)
            )
            Spacer(Modifier.height(12.dp))
          }
        }

        // 3. Hollywood / Bollywood / ... rows (only items of this page)
        tagSections.forEach { (name, list) ->
          item {
            SectionHeader(title = name)
            PosterRow(list, onPlayMedia)
          }
        }

        // 4. Genre rows
        genreSections.forEach { (name, list) ->
          item {
            SectionHeader(title = name)
            PosterRow(list, onPlayMedia)
          }
        }

        // 5. Everything
        item {
          SectionHeader(title = "All $title")
          PosterRow(sorted, onPlayMedia)
        }
      }
    }

  }
}

/** Full-screen version with a floating back button (used by "See all" style navigation). */
@Composable
fun TabPageScreen(
  title: String,
  items: List<MediaItem>,
  onBack: () -> Unit,
  onPlayMedia: (MediaItem) -> Unit
) {
  Box(modifier = Modifier.fillMaxSize()) {
    TabPageContent(title = title, items = items, onPlayMedia = onPlayMedia, bannerHeight = 380.dp)
    // Floating back button + page title over the banner
    Row(
      modifier = Modifier
        .statusBarsPadding()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier.size(40.dp).glass(CircleShape, fill = 0.18f).clickable { onBack() }.testTag("tab_page_back")
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }
      }
      Spacer(Modifier.width(12.dp))
      Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun PosterRow(list: List<MediaItem>, onPlayMedia: (MediaItem) -> Unit) {
  LazyRow(
    contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(list) { media ->
      MediaPosterCard(media = media, onClick = { onPlayMedia(media) })
    }
  }
  Spacer(Modifier.height(12.dp))
}
