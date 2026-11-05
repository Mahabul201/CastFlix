package com.example.ui.screens

import androidx.compose.foundation.layout.statusBarsPadding
import com.example.ui.theme.Dimens
import androidx.compose.material.icons.filled.History
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.FavoriteEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.MediaItem
import com.example.data.model.UserProfile
import com.example.data.repository.StreamRepository
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import com.example.util.ShareUtils

val AVATAR_OPTIONS = listOf(
  "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&q=80",
  "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&q=80",
  "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&q=80",
  "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=300&q=80",
  "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=300&q=80",
  "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=300&q=80"
)

class ProfileState(
  val context: android.content.Context,
  val scope: kotlinx.coroutines.CoroutineScope,
  profileValue: UserProfile,
  favoritesValue: List<FavoriteEntity>,
  watchlistValue: List<WatchlistEntity>
) {
  var profile by mutableStateOf(profileValue)
  var favorites by mutableStateOf(favoritesValue)
  var watchlist by mutableStateOf(watchlistValue)
  var nameInput by mutableStateOf(profileValue.name)
  var usernameInput by mutableStateOf(profileValue.username)
  var selectedAvatarIndex by mutableIntStateOf(profileValue.avatarIndex)
  var isEditing by mutableStateOf(false)
  var totalWatchHours by mutableDoubleStateOf(0.0)
  var historyCount by mutableIntStateOf(0)
  var selectedTab by mutableIntStateOf(0) // 0: Favorites, 1: Watchlist
}

@Composable
fun rememberProfileState(repository: StreamRepository): ProfileState {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val profile by repository.userProfile.collectAsState()
  val favorites by repository.favorites.collectAsState(initial = emptyList())
  val watchlist by repository.watchlist.collectAsState(initial = emptyList())
  val state = remember { ProfileState(context, scope, profile, favorites, watchlist) }
  state.profile = profile
  state.favorites = favorites
  state.watchlist = watchlist
  LaunchedEffect(Unit) {
    state.totalWatchHours = repository.getTotalWatchTimeHours()
    state.historyCount = repository.getHistoryCount()
  }
  return state
}

/** Profile card, quick actions and watch statistics (shown at the top of Settings). */
fun LazyListScope.profileTopItems(
  repository: StreamRepository,
  state: ProfileState,
  onOpenHistory: () -> Unit,
  onOpenNotifications: () -> Unit
) {
  val context = state.context
    // Profile Card
    item {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                  MaterialTheme.colorScheme.surfaceVariant
                ),
                endY = 420f
              )
            )
            .padding(14.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Current Avatar with Glow
          Box(
            modifier = Modifier
              .size(76.dp)
              .clip(CircleShape)
              .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            val avatarUrl = AVATAR_OPTIONS.getOrElse(state.selectedAvatarIndex) { AVATAR_OPTIONS[0] }
            AsyncImage(
              model = avatarUrl,
              contentDescription = "Avatar",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (state.isEditing) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Text("Choose Avatar", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
              Spacer(modifier = Modifier.height(8.dp))
              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                items(AVATAR_OPTIONS.indices.toList()) { idx ->
                  val isSelected = state.selectedAvatarIndex == idx
                  Box(
                    modifier = Modifier
                      .size(50.dp)
                      .clip(CircleShape)
                      .border(
                        if (isSelected) 3.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        CircleShape
                      )
                      .clickable { state.selectedAvatarIndex = idx }
                  ) {
                    AsyncImage(
                      model = AVATAR_OPTIONS[idx],
                      contentDescription = null,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize()
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              OutlinedTextField(
                value = state.nameInput,
                onValueChange = { state.nameInput = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )

              Spacer(modifier = Modifier.height(8.dp))

              OutlinedTextField(
                value = state.usernameInput,
                onValueChange = { state.usernameInput = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )

              Spacer(modifier = Modifier.height(14.dp))

              Button(
                onClick = {
                  val updated = UserProfile(
                    name = state.nameInput.trim().ifBlank { "Movie Fan" },
                    username = state.usernameInput.trim().replace(" ", "_").ifBlank { "streamer_pro" },
                    avatarIndex = state.selectedAvatarIndex
                  )
                  repository.updateUserProfile(updated)
                  state.isEditing = false
                  Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
              ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Changes")
              }
            }
          } else {
            Text(
              text = state.profile.name,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "@${state.profile.username}",
              color = MaterialTheme.colorScheme.primary,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = { state.isEditing = true },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Profile", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Backend sync chip
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Saved on this device", fontSize = 11.sp, color = Color.LightGray)
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }

    // Quick actions
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        ProfileAction(
          label = "History",
          icon = Icons.Default.History,
          modifier = Modifier.weight(1f),
          onClick = onOpenHistory
        )
        ProfileAction(
          label = "Alerts",
          icon = Icons.Default.Notifications,
          modifier = Modifier.weight(1f),
          onClick = onOpenNotifications
        )
        ProfileAction(
          label = "Share app",
          icon = Icons.Default.Share,
          modifier = Modifier.weight(1f),
          onClick = { ShareUtils.shareApp(context) }
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
    }

    // Watch Stats Grid
    item {
      Text(
        text = "Watch Statistics",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        StatCard(
          title = "Watched",
          value = String.format("%.1fh", state.totalWatchHours),
          icon = Icons.Default.Schedule,
          modifier = Modifier.weight(1f)
        )
        StatCard(
          title = "Played",
          value = "${state.historyCount}",
          icon = Icons.Default.Movie,
          modifier = Modifier.weight(1f)
        )
        StatCard(
          title = "Watchlist",
          value = "${state.watchlist.size}",
          icon = Icons.Default.Bookmark,
          modifier = Modifier.weight(1f)
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
}

/** Favorites / Watchlist tabs and lists (shown at the end of Settings). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun LazyListScope.profileSavedItems(
  repository: StreamRepository,
  state: ProfileState,
  onPlayMedia: (MediaItem, Long) -> Unit
) {
  val coroutineScope = state.scope
    // Tabs: Favorites & Watchlist
    item {
      PrimaryTabRow(
        selectedTabIndex = state.selectedTab,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary
      ) {
        Tab(
          selected = state.selectedTab == 0,
          onClick = { state.selectedTab = 0 },
          text = { Text("Favorites (${state.favorites.size})", fontSize = 13.sp, maxLines = 1, softWrap = false, fontWeight = if (state.selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
        )
        Tab(
          selected = state.selectedTab == 1,
          onClick = { state.selectedTab = 1 },
          text = { Text("Watchlist (${state.watchlist.size})", fontSize = 13.sp, maxLines = 1, softWrap = false, fontWeight = if (state.selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
    }

    if (state.selectedTab == 0) {
      if (state.favorites.isEmpty()) {
        item {
          EmptyProfileState(message = "No favorite titles added yet", icon = Icons.Default.Favorite)
        }
      } else {
        items(state.favorites) { fav ->
          SavedItemRow(
            title = fav.title,
            poster = fav.poster,
            category = fav.category,
            year = fav.year,
            rating = fav.rating,
            onPlay = {
              val media = MediaItem(
                id = fav.mediaId,
                title = fav.title,
                description = "Favorite saved item",
                poster = fav.poster,
                banner = fav.banner,
                category = fav.category,
                year = fav.year,
                rating = fav.rating,
                streamUrl = fav.streamUrl,
                isSeries = fav.isSeries
              )
              onPlayMedia(media, 0L)
            },
            onDelete = {
              coroutineScope.launch {
                val media = repository.getMediaById(fav.mediaId) ?: MediaItem(
                  id = fav.mediaId,
                  title = fav.title,
                  description = "",
                  poster = fav.poster,
                  banner = fav.banner,
                  category = fav.category,
                  year = fav.year,
                  rating = fav.rating,
                  streamUrl = fav.streamUrl
                )
                repository.toggleFavorite(media)
              }
            }
          )
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    } else {
      if (state.watchlist.isEmpty()) {
        item {
          EmptyProfileState(message = "Your state.watchlist is empty", icon = Icons.Default.Bookmark)
        }
      } else {
        items(state.watchlist) { item ->
          SavedItemRow(
            title = item.title,
            poster = item.poster,
            category = item.category,
            year = item.year,
            rating = item.rating,
            onPlay = {
              val media = MediaItem(
                id = item.mediaId,
                title = item.title,
                description = "Watchlist item",
                poster = item.poster,
                banner = item.banner,
                category = item.category,
                year = item.year,
                rating = item.rating,
                streamUrl = item.streamUrl,
                isSeries = item.isSeries
              )
              onPlayMedia(media, 0L)
            },
            onDelete = {
              coroutineScope.launch {
                val media = repository.getMediaById(item.mediaId) ?: MediaItem(
                  id = item.mediaId,
                  title = item.title,
                  description = "",
                  poster = item.poster,
                  banner = item.banner,
                  category = item.category,
                  year = item.year,
                  rating = item.rating,
                  streamUrl = item.streamUrl
                )
                repository.toggleWatchlist(media, true)
              }
            }
          )
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    }
}

@Composable
private fun ProfileAction(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
      Spacer(modifier = Modifier.height(4.dp))
      Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
fun StatCard(
  title: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.height(6.dp))
      Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      Text(title, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
    }
  }
}

@Composable
fun SavedItemRow(
  title: String,
  poster: String,
  category: String,
  year: Int,
  rating: Double,
  onPlay: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onPlay() }
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = poster,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(54.dp)
          .clip(RoundedCornerShape(8.dp))
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$category • $year • ★$rating", fontSize = 11.sp, color = Color.Gray)
      }
      Button(
        onClick = onPlay,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
      ) {
        Text("Play", fontSize = 11.sp)
      }
      IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
      }
    }
  }
}

@Composable
fun EmptyProfileState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(icon, contentDescription = null, tint = Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
      Spacer(modifier = Modifier.height(8.dp))
      Text(message, color = Color.Gray, fontSize = 13.sp)
    }
  }
}
