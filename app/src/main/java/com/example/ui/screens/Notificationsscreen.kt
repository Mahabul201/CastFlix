package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppNotification
import com.example.data.repository.StreamRepository
import com.example.ui.theme.Dimens

/** Inbox with everything the admin has sent. Opening it marks all as read. */
@Composable
fun NotificationsScreen(
  repository: StreamRepository,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  var items by remember { mutableStateOf<List<AppNotification>?>(null) }

  LaunchedEffect(Unit) {
    val list = repository.fetchNotifications()
    items = list
    list.maxOfOrNull { it.createdAt }?.let { repository.setLastReadNotificationAt(it) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.ScreenHorizontal - 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
      }
      Text(
        text = "Notifications",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    val list = items
    when {
      list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
      list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          "No notifications yet",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 15.sp
        )
      }
      else -> LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = Dimens.ScreenHorizontal,
          end = Dimens.ScreenHorizontal,
          top = 8.dp,
          bottom = Dimens.ScreenBottom
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)
      ) {
        items(list, key = { it.id }) { n ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .clickable(enabled = n.link.isNotBlank()) {
                try {
                  context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(n.link)))
                } catch (_: Exception) {
                }
              }
              .padding(14.dp)
          ) {
            if (n.imageUrl.isNotBlank()) {
              AsyncImage(
                model = n.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .fillMaxWidth()
                  .aspectRatio(16f / 9f)
                  .clip(RoundedCornerShape(8.dp))
              )
              Spacer(modifier = Modifier.height(10.dp))
            }
            Text(
              text = n.title,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = n.message,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = android.text.format.DateUtils.getRelativeTimeSpanString(n.createdAt).toString(),
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}
