package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem

@Composable
fun DirectUrlStreamDialog(
  onDismiss: () -> Unit,
  onPlayUrl: (MediaItem) -> Unit
) {
  var urlInput by remember { mutableStateOf("") }
  var titleInput by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Stream from Direct URL", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          "Enter any direct HLS stream (.m3u8), MP4, or MKV URL to stream immediately with hardware acceleration and gestures:",
          fontSize = 12.sp,
          color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = urlInput,
          onValueChange = {
            urlInput = it
            isError = false
          },
          label = { Text("Stream URL (m3u8 / mp4 / mkv)") },
          placeholder = { Text("https://example.com/live/stream.m3u8") },
          singleLine = true,
          isError = isError,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("direct_url_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = titleInput,
          onValueChange = { titleInput = it },
          label = { Text("Custom Title (Optional)") },
          placeholder = { Text("My Direct Stream") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text("Quick Test Samples:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .clickable {
                urlInput = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                titleInput = "Live HLS Test Stream"
              }
              .weight(1f)
          ) {
            Text(
              "HLS m3u8",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              color = MaterialTheme.colorScheme.primary
            )
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .clickable {
                urlInput = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                titleInput = "Big Buck Bunny 1080p"
              }
              .weight(1f)
          ) {
            Text(
              "MP4 Bunny",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              color = MaterialTheme.colorScheme.primary
            )
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .clickable {
                urlInput = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
                titleInput = "Tears of Steel HD"
              }
              .weight(1f)
          ) {
            Text(
              "Sci-Fi MP4",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (urlInput.isNotBlank()) {
            val media = MediaItem(
              id = "direct_${System.currentTimeMillis()}",
              title = titleInput.ifBlank { "Direct URL Stream" },
              description = "Direct stream URL: $urlInput",
              poster = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&q=80",
              banner = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1200&q=80",
              category = "Direct Stream",
              year = 2025,
              rating = 9.0,
              streamUrl = urlInput.trim(),
              isSeries = false
            )
            onPlayUrl(media)
            onDismiss()
          } else {
            isError = true
          }
        },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.testTag("dialog_play_button")
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Play Stream")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
