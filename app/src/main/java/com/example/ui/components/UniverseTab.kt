package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaItem
import com.example.data.model.UniverseInfo
import com.example.ui.theme.Dimens
import com.example.ui.theme.glass

/**
 * "Universe" tab of the Home screen.
 * Shows one folder card (cover photo + name) per universe, like Marvel Universe / DC Universe.
 * Tapping a folder opens it and lists every movie / series inside it in watch order.
 *
 * @param universes folders sent by the server (admin panel -> Universes)
 * @param allItems every title the app knows, used to turn a universe's item ids into titles
 * @param looseItems older titles tagged "Universe" that are not inside any folder
 * @param topPadding space for the floating tab bar at the top of Home
 */
@Composable
fun UniverseTabContent(
  universes: List<UniverseInfo>,
  allItems: List<MediaItem>,
  looseItems: List<MediaItem>,
  topPadding: Dp,
  onPlayMedia: (MediaItem) -> Unit,
  modifier: Modifier = Modifier
) {
  var openId by rememberSaveable { mutableStateOf<String?>(null) }
  val byId = remember(allItems) { allItems.associateBy { it.id } }
  val open = universes.firstOrNull { it.id == openId }

  // System back closes the folder first instead of leaving Home
  BackHandler(enabled = open != null) { openId = null }

  Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    if (open != null) {
      UniverseDetail(
        universe = open,
        items = open.itemIds.mapNotNull { byId[it] },
        topPadding = topPadding,
        onBack = { openId = null },
        onPlayMedia = onPlayMedia
      )
    } else if (universes.isEmpty() && looseItems.isEmpty()) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No universes yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().testTag("universe_grid"),
        contentPadding = PaddingValues(
          start = Dimens.ScreenHorizontal,
          end = Dimens.ScreenHorizontal,
          top = topPadding + 8.dp,
          bottom = Dimens.ScreenBottom
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        items(universes, key = { it.id }) { u ->
          UniverseCard(
            universe = u,
            count = u.itemIds.count { it in byId },
            onClick = { openId = u.id }
          )
        }
        if (looseItems.isNotEmpty()) {
          item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
              "More from the Universe",
              color = MaterialTheme.colorScheme.onBackground,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(top = 8.dp)
            )
          }
          items(looseItems, key = { "loose_" + it.id }) { media ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
              MediaPosterCard(media = media, onClick = { onPlayMedia(media) })
            }
          }
        }
      }
    }
  }
}

/** One folder: cover photo with the universe name and how many titles are inside. */
@Composable
private fun UniverseCard(universe: UniverseInfo, count: Int, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(0.72f)
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .clickable { onClick() }
      .testTag("universe_card")
  ) {
    AsyncImage(
      model = ImageRequest.Builder(LocalContext.current)
        .data(universe.poster.ifBlank { universe.banner })
        .crossfade(true)
        .build(),
      contentDescription = universe.name,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize()
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.92f))
          )
        )
    )
    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Text(
        text = universe.name,
        color = Color.White,
        fontSize = 17.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Folder, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
          text = if (count == 0) "Coming soon" else if (count == 1) "1 title" else "$count titles",
          color = Color(0xFFCBD5E1),
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

/** Inside a universe: wide banner with the name, then all its titles in watch order. */
@Composable
private fun UniverseDetail(
  universe: UniverseInfo,
  items: List<MediaItem>,
  topPadding: Dp,
  onBack: () -> Unit,
  onPlayMedia: (MediaItem) -> Unit
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 140.dp),
    modifier = Modifier.fillMaxSize().testTag("universe_detail"),
    contentPadding = PaddingValues(
      start = Dimens.ScreenHorizontal,
      end = Dimens.ScreenHorizontal,
      top = topPadding + 8.dp,
      bottom = Dimens.ScreenBottom
    ),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(210.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
      ) {
        AsyncImage(
          model = ImageRequest.Builder(LocalContext.current)
            .data(universe.banner.ifBlank { universe.poster })
            .crossfade(true)
            .build(),
          contentDescription = universe.name,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.92f))
              )
            )
        )
        Surface(
          shape = CircleShape,
          color = Color.Transparent,
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(10.dp)
            .size(38.dp)
            .glass(CircleShape, fill = 0.25f)
            .clickable { onBack() }
            .testTag("universe_back")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
          }
        }
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(14.dp)
        ) {
          Text(
            text = universe.name,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = if (items.size == 1) "1 title" else "${items.size} titles",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }
    }

    // Story of the universe (written in the admin panel)
    if (universe.description.isNotBlank()) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        var expanded by rememberSaveable(universe.id) { mutableStateOf(false) }
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable { expanded = !expanded }
            .padding(14.dp)
        ) {
          Text(
            text = "Story",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text = universe.description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis
          )
          if (universe.description.length > 160) {
            Text(
              text = if (expanded) "Show less" else "Read more",
              color = MaterialTheme.colorScheme.primary,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(top = 6.dp)
            )
          }
        }
      }
    }

    if (items.isEmpty()) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
          Text("Coming soon", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    } else {
      items(items, key = { it.id }) { media ->
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
          MediaPosterCard(media = media, onClick = { onPlayMedia(media) })
        }
      }
    }
  }
}
