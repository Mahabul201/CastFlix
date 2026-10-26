package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppNotification
import com.example.data.model.AppUpdateInfo
import com.example.data.repository.StreamRepository
import com.example.ui.theme.Dimens

/**
 * Inbox with everything the admin has sent (plus an "update available" card when a newer
 * version is published). New messages are highlighted; opening the screen marks them read.
 */
@Composable
fun NotificationsScreen(
  repository: StreamRepository,
  onBack: () -> Unit,
  updateInfo: AppUpdateInfo? = null,
  onUpdateClick: () -> Unit = {}
) {
  val context = LocalContext.current
  var items by remember { mutableStateOf<List<AppNotification>?>(null) }
  var failed by remember { mutableStateOf(false) }
  var reloadKey by remember { mutableIntStateOf(0) }
  // Time of the newest message the user had already read BEFORE opening this screen
  var readUpTo by remember { mutableLongStateOf(Long.MAX_VALUE) }

  LaunchedEffect(reloadKey) {
    failed = false
    val before = repository.getLastReadNotificationAt()
    val list = repository.fetchNotificationsOrNull()
    if (list == null) {
      failed = true
      if (items == null) items = emptyList()
    } else {
      readUpTo = if (before < 0L) Long.MAX_VALUE else before
      items = list.sortedByDescending { it.createdAt }
      list.maxOfOrNull { it.createdAt }?.let { repository.setLastReadNotificationAt(it) }
    }
  }

  val list = items
  val unread = list?.count { it.createdAt > readUpTo } ?: 0

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("notifications_screen")
  ) {
    // Top bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.ScreenHorizontal - 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Notifications",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        if (list != null && !failed) {
          Text(
            text = if (unread > 0) "$unread new" else "You're all caught up",
            fontSize = 12.sp,
            color = if (unread > 0) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
          )
        }
      }
      if (unread > 0) {
        IconButton(onClick = { readUpTo = Long.MAX_VALUE }) {
          Icon(Icons.Default.DoneAll, contentDescription = "Mark all as read")
        }
      }
      IconButton(onClick = { reloadKey++ }) {
        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
      }
    }

    when {
      list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }

      failed && list.isEmpty() && updateInfo == null -> StateMessage(
        icon = { Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(52.dp)) },
        title = "Couldn't load notifications",
        subtitle = "Check your internet connection and try again.",
        actionLabel = "Retry",
        onAction = { reloadKey++ }
      )

      list.isEmpty() && updateInfo == null -> StateMessage(
        icon = { Icon(Icons.Default.NotificationsNone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(52.dp)) },
        title = "No notifications yet",
        subtitle = "News and new releases from CastFlix will show up here."
      )

      else -> {
        val fresh = list.filter { it.createdAt > readUpTo }
        val earlier = list.filter { it.createdAt <= readUpTo }

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(
            start = Dimens.ScreenHorizontal,
            end = Dimens.ScreenHorizontal,
            top = 4.dp,
            bottom = Dimens.ScreenBottom
          ),
          verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)
        ) {
          if (updateInfo != null) {
            item(key = "update") { UpdateCard(updateInfo, onUpdateClick) }
          }
          if (failed) {
            item(key = "offline") {
              Text(
                "Showing nothing new — couldn't reach the server.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          if (fresh.isNotEmpty()) {
            item(key = "h_new") { GroupLabel("New") }
            items(fresh, key = { "n_" + it.id }) { n -> NotificationCard(n, true) { openLink(context, n.link) } }
          }
          if (earlier.isNotEmpty()) {
            item(key = "h_earlier") { GroupLabel(if (fresh.isNotEmpty()) "Earlier" else "All notifications") }
            items(earlier, key = { "e_" + it.id }) { n -> NotificationCard(n, false) { openLink(context, n.link) } }
          }
        }
      }
    }
  }
}

private fun openLink(context: android.content.Context, link: String) {
  if (link.isBlank()) return
  try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  } catch (_: Exception) {
  }
}

@Composable
private fun GroupLabel(text: String) {
  Text(
    text = text,
    fontSize = 13.sp,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 6.dp, start = 2.dp)
  )
}

@Composable
private fun UpdateCard(info: AppUpdateInfo, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
      .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
      .clickable { onClick() }
      .padding(14.dp)
      .testTag("notification_update_card"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp)) {
      Box(contentAlignment = Alignment.Center) {
        Icon(Icons.Default.SystemUpdate, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(22.dp))
      }
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "Update available${if (info.versionName.isNotBlank()) " — v${info.versionName}" else ""}",
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = info.changelog.ifBlank { "A new version of CastFlix is ready to install." },
        fontSize = 12.sp,
        maxLines = 2,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Text("Update", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
  }
}

@Composable
private fun NotificationCard(n: AppNotification, isNew: Boolean, onOpen: () -> Unit) {
  val shape = RoundedCornerShape(16.dp)
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .then(
        if (isNew) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), shape)
        else Modifier
      )
      .clickable(enabled = n.link.isNotBlank()) { onOpen() }
      .padding(14.dp)
  ) {
    Row(verticalAlignment = Alignment.Top) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = if (isNew) 0.25f else 0.12f),
        modifier = Modifier.size(38.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            Icons.Default.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = n.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
          )
          if (isNew) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
            )
          }
        }
        Text(
          text = android.text.format.DateUtils.getRelativeTimeSpanString(n.createdAt).toString(),
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    if (n.imageUrl.isNotBlank()) {
      Spacer(modifier = Modifier.height(10.dp))
      AsyncImage(
        model = n.imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f)
          .clip(RoundedCornerShape(10.dp))
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = n.message,
      fontSize = 14.sp,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    )

    if (n.link.isNotBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.OpenInNew, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Open link", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@Composable
private fun StateMessage(
  icon: @Composable () -> Unit,
  title: String,
  subtitle: String,
  actionLabel: String? = null,
  onAction: () -> Unit = {}
) {
  Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      icon()
      Spacer(modifier = Modifier.height(12.dp))
      Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        subtitle,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      if (actionLabel != null) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onAction,
          shape = RoundedCornerShape(20.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) { Text(actionLabel) }
      }
    }
  }
}
