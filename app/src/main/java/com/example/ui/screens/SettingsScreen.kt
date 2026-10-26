package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StreamSettings
import com.example.data.repository.StreamRepository
import com.example.ui.theme.AppAccent
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
  repository: StreamRepository
) {
  val context = LocalContext.current
  val settings by repository.settings.collectAsState()
  val coroutineScope = rememberCoroutineScope()

  var showClearHistoryDialog by remember { mutableStateOf(false) }
  var showClearCacheDialog by remember { mutableStateOf(false) }
  var showBackendDialog by remember { mutableStateOf(false) }
  var backendInput by remember { mutableStateOf(settings.backendUrl) }
  var showPinDialog by remember { mutableStateOf(false) }
  var pinInput by remember { mutableStateOf(settings.parentalPin) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
      .testTag("settings_screen")
  ) {
    item {
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "Settings",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.height(16.dp))
    }

    // 1. APPEARANCE
    item {
      SettingsCategoryHeader(title = "Appearance", icon = Icons.Default.Palette)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Theme Mode (Dark / AMOLED / Light)
          Text("Theme Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("DARK" to "Dark Cinema", "AMOLED" to "True AMOLED", "LIGHT" to "Light").forEach { (mode, label) ->
              val isSelected = settings.themeMode == mode
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                modifier = Modifier
                  .clickable {
                    repository.updateSettings(settings.copy(themeMode = mode))
                  }
                  .weight(1f)
              ) {
                Text(
                  text = label,
                  color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          Spacer(modifier = Modifier.height(16.dp))

          // Accent Color
          Text("Accent Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            AppAccent.values().forEach { accent ->
              val isSelected = settings.accentColor == accent.name
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(accent.color)
                  .clickable {
                    repository.updateSettings(settings.copy(accentColor = accent.name))
                  },
                contentAlignment = Alignment.Center
              ) {
                if (isSelected) {
                  Box(
                    modifier = Modifier
                      .size(14.dp)
                      .clip(CircleShape)
                      .background(Color.White)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          Spacer(modifier = Modifier.height(12.dp))

          // View Mode (Grid vs List)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Catalog View Style", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (settings.viewMode == "GRID") MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier.clickable { repository.updateSettings(settings.copy(viewMode = "GRID")) }
              ) {
                Text(
                  "Grid",
                  color = if (settings.viewMode == "GRID") Color.White else Color.Gray,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (settings.viewMode == "LIST") MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier.clickable { repository.updateSettings(settings.copy(viewMode = "LIST")) }
              ) {
                Text(
                  "List",
                  color = if (settings.viewMode == "LIST") Color.White else Color.Gray,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }

    // 2. AUDIO & SCREEN
    item {
      SettingsCategoryHeader(title = "Audio & Screen", icon = Icons.Default.VolumeUp)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          SettingToggleRow(
            title = "Volume Boost",
            subtitle = "Boost maximum volume levels above default Android thresholds",
            checked = settings.volumeBoost,
            onCheckedChange = { repository.updateSettings(settings.copy(volumeBoost = it)) }
          )
          Spacer(modifier = Modifier.height(12.dp))
          SettingToggleRow(
            title = "Background Audio Play",
            subtitle = "Keep audio streaming when screen is off or app minimized",
            checked = settings.backgroundPlay,
            onCheckedChange = { repository.updateSettings(settings.copy(backgroundPlay = it)) }
          )
          Spacer(modifier = Modifier.height(12.dp))
          SettingToggleRow(
            title = "Keep Screen On",
            subtitle = "Prevent screen from dimming or turning off during video playback",
            checked = settings.keepScreenOn,
            onCheckedChange = { repository.updateSettings(settings.copy(keepScreenOn = it)) }
          )
          Spacer(modifier = Modifier.height(12.dp))
          SettingToggleRow(
            title = "Picture-in-Picture (PiP)",
            subtitle = "Automatically switch to mini overlay player on home button",
            checked = settings.pipMode,
            onCheckedChange = { repository.updateSettings(settings.copy(pipMode = it)) }
          )
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }

    // 3. PLAYBACK
    item {
      SettingsCategoryHeader(title = "Playback Controls", icon = Icons.Default.PlayCircle)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          SettingToggleRow(
            title = "Autoplay Next Episode",
            subtitle = "Automatically start the next episode when series ends",
            checked = settings.autoplayNext,
            onCheckedChange = { repository.updateSettings(settings.copy(autoplayNext = it)) }
          )
          Spacer(modifier = Modifier.height(12.dp))
          SettingToggleRow(
            title = "Skip Intro Button",
            subtitle = "Show +85s fast forward button to skip opening theme songs",
            checked = settings.skipIntro,
            onCheckedChange = { repository.updateSettings(settings.copy(skipIntro = it)) }
          )
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }

    // 4. DOWNLOADS
    item {
      SettingsCategoryHeader(title = "Downloads", icon = Icons.Default.Download)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          SettingToggleRow(
            title = "Wi-Fi Only Downloads",
            subtitle = "Avoid downloading high-definition video files on cellular data",
            checked = settings.wifiOnlyDownload,
            onCheckedChange = { repository.updateSettings(settings.copy(wifiOnlyDownload = it)) }
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Max Parallel Downloads", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
              Text("Current: ${settings.maxParallelDownloads} concurrent tasks", fontSize = 12.sp, color = Color.Gray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(1, 2, 3, 5).forEach { limit ->
                val isSelected = settings.maxParallelDownloads == limit
                Surface(
                  shape = CircleShape,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                  modifier = Modifier
                    .size(32.dp)
                    .clickable { repository.updateSettings(settings.copy(maxParallelDownloads = limit)) }
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      "$limit",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) Color.White else Color.Gray
                    )
                  }
                }
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }

    // 5. PRIVACY & PARENTAL LOCK
    item {
      SettingsCategoryHeader(title = "Privacy & Parental Lock", icon = Icons.Default.Security)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showClearHistoryDialog = true }
              .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Clear Watch History", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
              Text("Remove all recently played progress logs", fontSize = 12.sp, color = Color.Gray)
            }
            Text("Clear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showClearCacheDialog = true }
              .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Clear Streaming Cache", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
              Text("Free temporary network image and video buffer chunks", fontSize = 12.sp, color = Color.Gray)
            }
            Text("Clear", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          Spacer(modifier = Modifier.height(10.dp))

          SettingToggleRow(
            title = "Parental PIN Lock",
            subtitle = if (settings.parentalLock) "PIN: •••• (Active)" else "Protect mature categories with a 4-digit code",
            checked = settings.parentalLock,
            onCheckedChange = { checked ->
              if (checked) {
                showPinDialog = true
              } else {
                repository.updateSettings(settings.copy(parentalLock = false))
              }
            }
          )
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }

    // 7. ABOUT
    item {
      SettingsCategoryHeader(title = "About", icon = Icons.Default.Info)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("App Version", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("1.0", color = Color.Gray, fontSize = 13.sp)
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "CastFlix plays direct media URLs (m3u8, mp4, mkv).",
            fontSize = 11.sp,
            color = Color.LightGray
          )
        }
      }
      Spacer(modifier = Modifier.height(96.dp))
    }
  }

  // Dialogs
  if (showClearHistoryDialog) {
    AlertDialog(
      onDismissRequest = { showClearHistoryDialog = false },
      title = { Text("Clear Watch History?") },
      text = { Text("All your recorded playback positions and resume progress will be permanently erased.") },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              repository.clearHistory()
              showClearHistoryDialog = false
              Toast.makeText(context, "Watch history cleared", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Clear")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearHistoryDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showClearCacheDialog) {
    AlertDialog(
      onDismissRequest = { showClearCacheDialog = false },
      title = { Text("Clear Cache?") },
      text = { Text("Temporary stream cache and thumbnail buffers will be deleted to free device storage.") },
      confirmButton = {
        Button(
          onClick = {
            context.cacheDir.deleteRecursively()
            showClearCacheDialog = false
            Toast.makeText(context, "Cache wiped successfully", Toast.LENGTH_SHORT).show()
          }
        ) {
          Text("Clear Cache")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearCacheDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showPinDialog) {
    AlertDialog(
      onDismissRequest = { showPinDialog = false },
      title = { Text("Set Parental PIN") },
      text = {
        Column {
          Text("Enter a 4-digit numeric code to protect mature streams:", fontSize = 12.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = pinInput,
            onValueChange = { if (it.length <= 4) pinInput = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (pinInput.length == 4) {
              repository.updateSettings(settings.copy(parentalLock = true, parentalPin = pinInput))
              showPinDialog = false
              Toast.makeText(context, "Parental PIN active", Toast.LENGTH_SHORT).show()
            } else {
              Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
            }
          }
        ) {
          Text("Enable")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPinDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun SettingsCategoryHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(bottom = 8.dp)
  ) {
    Icon(
      icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = title,
      fontWeight = FontWeight.Bold,
      fontSize = 15.sp,
      color = MaterialTheme.colorScheme.primary
    )
  }
}

@Composable
fun SettingToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      Text(subtitle, fontSize = 12.sp, color = Color.Gray)
    }
    Spacer(modifier = Modifier.width(12.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.primary,
        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
      )
    )
  }
}
