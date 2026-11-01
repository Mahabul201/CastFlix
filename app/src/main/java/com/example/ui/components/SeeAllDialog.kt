package com.example.ui.components

import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.ui.theme.Dimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeAllScreen(
  title: String,
  items: List<MediaItem>,
  onBack: () -> Unit,
  onPlayMedia: (MediaItem) -> Unit
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(title, fontWeight = FontWeight.Bold)
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.onBackground
        )
      )
    }
  ) { paddingValues ->
    if (items.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center
      ) {
        Text(
          "Nothing here yet",
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      return@Scaffold
    }
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 130.dp),
      contentPadding = PaddingValues(
        start = Dimens.ScreenHorizontal,
        end = Dimens.ScreenHorizontal,
        top = paddingValues.calculateTopPadding() + 8.dp,
        bottom = Dimens.ScreenBottom
      ),
      horizontalArrangement = Arrangement.spacedBy(Dimens.GridGap),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()
    ) {
      items(items) { media ->
        MediaPosterCard(
          media = media,
          onClick = { onPlayMedia(media) },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}
