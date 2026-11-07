package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.AppNotification

/**
 * Big full-screen announcement that opens once on the Home > Trending page when the admin
 * sends a new notification. The ( X ) button in the corner closes it.
 */
@Composable
fun NotificationPopup(item: AppNotification, onClose: () -> Unit) {
  val context = LocalContext.current
  Dialog(
    onDismissRequest = onClose,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnClickOutside = false
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.92f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (item.imageUrl.isNotBlank()) {
          AsyncImage(
            model = item.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(4f / 5f)
              .clip(RoundedCornerShape(20.dp))
          )
          Spacer(modifier = Modifier.height(18.dp))
        }
        Text(
          text = item.title,
          color = Color.White,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = item.message,
          color = Color.White.copy(alpha = 0.85f),
          fontSize = 15.sp,
          textAlign = TextAlign.Center
        )
        if (item.link.isNotBlank()) {
          Spacer(modifier = Modifier.height(20.dp))
          Button(
            onClick = {
              try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link)))
              } catch (_: Exception) {
              }
              onClose()
            },
            shape = RoundedCornerShape(24.dp)
          ) {
            Text("Open", fontWeight = FontWeight.Bold)
          }
        }
      }

      // ( X ) close button
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .statusBarsPadding()
          .padding(14.dp)
          .size(40.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.18f))
          .clickable { onClose() }
          .testTag("notification_popup_close"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.Close,
          contentDescription = "Close",
          tint = Color.White,
          modifier = Modifier.size(22.dp)
        )
      }
    }
  }
}
