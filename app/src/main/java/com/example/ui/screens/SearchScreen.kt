package com.example.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.ui.theme.Dimens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.repository.StreamRepository
import com.example.ui.components.MediaPosterCard
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
  repository: StreamRepository,
  onPlayMedia: (MediaItem, Long) -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf("All") }
  var searchResults by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
  var isFocused by remember { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()

  val filters = listOf("All", "Movies", "Web Series", "Anime")

  LaunchedEffect(searchQuery, selectedFilter) {
    searchResults = repository.searchMedia(searchQuery, selectedFilter)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .padding(horizontal = Dimens.ScreenHorizontal)
      .testTag("search_screen")
  ) {
    Spacer(modifier = Modifier.height(6.dp))

    // Compact search bar (44dp instead of the old 56dp+ field)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .clip(RoundedCornerShape(22.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .border(
          width = 1.dp,
          color = if (isFocused) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
          shape = RoundedCornerShape(22.dp)
        )
        .padding(horizontal = 14.dp)
        .testTag("search_input_field"),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        Icons.Default.Search,
        contentDescription = "Search",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
        if (searchQuery.isEmpty()) {
          Text(
            "Search movies, series, anime…",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
          )
        }
        BasicTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          singleLine = true,
          textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
        )
      }
      if (searchQuery.isNotEmpty()) {
        Icon(
          Icons.Default.Clear,
          contentDescription = "Clear search",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .clickable { searchQuery = "" }
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Filter Chips (wrap instead of scrolling, so every chip is always visible)
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
      filters.forEach { filter ->
        val isSelected = selectedFilter == filter
        ElevatedFilterChip(
          selected = isSelected,
          onClick = { selectedFilter = filter },
          label = { Text(filter, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
          colors = FilterChipDefaults.elevatedFilterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Header label: "Search Results" or "Trending & Random Mix"
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (searchQuery.isBlank()) "Random Mix & Trending" else "Results (${searchResults.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )

      if (searchQuery.isBlank()) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.clickable {
            coroutineScope.launch {
              searchResults = repository.getRandomMix()
            }
          }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Shuffle", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Results Grid
    if (searchResults.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = Color.Gray.copy(alpha = 0.5f)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            if (searchQuery.isBlank()) "Nothing to show yet" else "No titles match '$searchQuery'",
            color = Color.Gray
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 130.dp),
        contentPadding = PaddingValues(bottom = Dimens.ScreenBottom),
        horizontalArrangement = Arrangement.spacedBy(Dimens.GridGap),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        items(searchResults) { media ->
          MediaPosterCard(
            media = media,
            onClick = { onPlayMedia(media, 0L) },
            modifier = Modifier.fillMaxWidth(),
            cardHeight = 185.dp
          )
        }
      }
    }
  }
}
