package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.repository.StreamRepository
import com.example.ui.theme.AppAccent
import com.example.ui.theme.Dimens
import com.example.util.ShareUtils
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
  repository: StreamRepository,
  updateAvailable: Boolean = false,
  onCheckUpdate: () -> Unit = {}
) {
  val context = LocalContext.current
  val settings by repository.settings.collectAsState()
  val coroutineScope = rememberCoroutineScope()

  var showClearHistoryDialog by remember { mutableStateOf(false) }
  var showClearCacheDialog by remember { mutableStateOf(false) }
  var showPinDialog by remember { mutableStateOf(false) }
  var pinInput by remember { mutableStateOf(settings.parentalPin) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .padding(horizontal = Dimens.ScreenHorizontal)
      .testTag("settings_screen"),
    contentPadding = PaddingValues(bottom = Dimens.ScreenBottom),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(Dimens.ScreenTop))
      Text(
        text = "Settings",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.height(10.dp))
    }

    // ---- App card: version + update + share ----
    item {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(48.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text("C", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
              }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("CastFlix", fontWeight = FontWeight.Bold, fontSize = 18.sp)
              Text(
                text = if (updateAvailable) "Version ${BuildConfig.VERSION_NAME} • Update available"
                else "Version ${BuildConfig.VERSION_NAME} • Up to date",
                fontSize = 12.sp,
                color = if (updateAvailable) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (updateAvailable) FontWeight.SemiBold else FontWeight.Normal
              )
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = onCheckUpdate,
              shape = RoundedCornerShape(20.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("settings_check_update")
            ) {
              Icon(Icons.Default.SystemUpdate, null, modifier = Modifier.size(16.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (updateAvailable) "Update now" else "Check for updates", fontSize = 12.sp, color = Color.White)
            }
            Button(
              onClick = { ShareUtils.shareApp(context) },
              shape = RoundedCornerShape(20.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("settings_share_app")
            ) {
              Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Share app", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
          }
        }
      }
    }

    // ---- Appearance ----
    item {
      SettingsSection(title = "Appearance", icon = Icons.Default.Palette) {
        Text("Theme", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("DARK" to "Dark", "AMOLED" to "AMOLED", "LIGHT" to "Light").forEach { (mode, label) ->
            val isSelected = settings.themeMode == mode
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .clickable { repository.updateSettings(settings.copy(themeMode = mode)) }
            ) {
              Text(
                text = label,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp)
              )
            }
          }
        }

        SettingsDivider()

        Text("Accent color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          AppAccent.values().forEach { accent ->
            val isSelected = settings.accentColor == accent.name
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accent.color)
                .clickable { repository.updateSettings(settings.copy(accentColor = accent.name)) },
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

        SettingsDivider()

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Catalog view", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("GRID" to "Grid", "LIST" to "List").forEach { (mode, label) ->
              val selected = settings.viewMode == mode
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { repository.updateSettings(settings.copy(viewMode = mode)) }
              ) {
                Text(
                  label,
                  color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
              }
            }
          }
        }
      }
    }

    // ---- Playback ----
    item {
      SettingsSection(title = "Playback", icon = Icons.Default.PlayCircle) {
        SettingToggleRow(
          title = "Autoplay next episode",
          subtitle = "Start the next episode when one ends",
          checked = settings.autoplayNext,
          onCheckedChange = { repository.updateSettings(settings.copy(autoplayNext = it)) }
        )
        SettingsDivider()
        SettingToggleRow(
          title = "Skip intro button",
          subtitle = "Show a +85s button to jump past the opening",
          checked = settings.skipIntro,
          onCheckedChange = { repository.updateSettings(settings.copy(skipIntro = it)) }
        )
        SettingsDivider()
        SettingToggleRow(
          title = "Keep screen on",
          subtitle = "Don't dim the screen while a video plays",
          checked = settings.keepScreenOn,
          onCheckedChange = { repository.updateSettings(settings.copy(keepScreenOn = it)) }
        )
        SettingsDivider()
        SettingToggleRow(
          title = "Picture-in-Picture",
          subtitle = "Mini player when you press Home",
          checked = settings.pipMode,
          onCheckedChange = { repository.updateSettings(settings.copy(pipMode = it)) }
        )
        SettingsDivider()
        SettingToggleRow(
          title = "Background audio",
          subtitle = "Keep playing with the screen off",
          checked = settings.backgroundPlay,
          onCheckedChange = { repository.updateSettings(settings.copy(backgroundPlay = it)) }
        )
        SettingsDivider()
        SettingToggleRow(
          title = "Volume boost",
          subtitle = "Allow volume above the normal maximum",
          checked = settings.volumeBoost,
          onCheckedChange = { repository.updateSettings(settings.copy(volumeBoost = it)) }
        )
      }
    }

    // ---- Downloads ----
    item {
      SettingsSection(title = "Downloads", icon = Icons.Default.Download) {
        SettingToggleRow(
          title = "Wi-Fi only downloads",
          subtitle = "Don't use mobile data for big files",
          checked = settings.wifiOnlyDownload,
          onCheckedChange = { repository.updateSettings(settings.copy(wifiOnlyDownload = it)) }
        )
        SettingsDivider()
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Parallel downloads", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
              "${settings.maxParallelDownloads} at a time",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1, 2, 3, 5).forEach { limit ->
              val isSelected = settings.maxParallelDownloads == limit
              Surface(
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .clickable { repository.updateSettings(settings.copy(maxParallelDownloads = limit)) }
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    "$limit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
    }

    // ---- Notifications & updates ----
    item {
      SettingsSection(title = "Notifications & updates", icon = Icons.Default.Notifications) {
        SettingToggleRow(
          title = "Push notifications",
          subtitle = "Get news and new releases sent from the admin",
          checked = settings.notificationsEnabled,
          onCheckedChange = { repository.updateSettings(settings.copy(notificationsEnabled = it)) }
        )
        SettingsDivider()
        SettingToggleRow(
          title = "Check for updates on launch",
          subtitle = "Show a prompt when a new version is published",
          checked = settings.autoCheckUpdate,
          onCheckedChange = { repository.updateSettings(settings.copy(autoCheckUpdate = it)) }
        )
      }
    }

    // ---- Privacy ----
    item {
      SettingsSection(title = "Privacy & parental lock", icon = Icons.Default.Lock) {
        SettingsActionRow(
          icon = Icons.Default.History,
          title = "Clear watch history",
          subtitle = "Remove all resume progress",
          tint = MaterialTheme.colorScheme.error,
          onClick = { showClearHistoryDialog = true }
        )
        SettingsDivider()
        SettingsActionRow(
          icon = Icons.Default.DeleteSweep,
          title = "Clear cache",
          subtitle = "Free temporary images and buffers",
          onClick = { showClearCacheDialog = true }
        )
        SettingsDivider()
        SettingToggleRow(
          title = "Parental PIN lock",
          subtitle = if (settings.parentalLock) "PIN is active" else "Protect mature titles with a 4-digit code",
          checked = settings.parentalLock,
          onCheckedChange = { checked ->
            if (checked) showPinDialog = true
            else repository.updateSettings(settings.copy(parentalLock = false))
          }
        )
      }
    }

    // ---- About ----
    item {
      SettingsSection(title = "About", icon = Icons.Default.Info) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Version", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
          Text(
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "CastFlix plays direct media links (m3u8, mp4, mkv) and videos stored on your phone.",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  // ---- Dialogs ----
  if (showClearHistoryDialog) {
    AlertDialog(
      onDismissRequest = { showClearHistoryDialog = false },
      title = { Text("Clear watch history?") },
      text = { Text("All saved playback positions will be permanently erased.") },
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
        ) { Text("Clear") }
      },
      dismissButton = { TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel") } }
    )
  }

  if (showClearCacheDialog) {
    AlertDialog(
      onDismissRequest = { showClearCacheDialog = false },
      title = { Text("Clear cache?") },
      text = { Text("Temporary images and stream buffers will be deleted to free space.") },
      confirmButton = {
        Button(onClick = {
          context.cacheDir.deleteRecursively()
          showClearCacheDialog = false
          Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
        }) { Text("Clear cache") }
      },
      dismissButton = { TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") } }
    )
  }

  if (showPinDialog) {
    AlertDialog(
      onDismissRequest = { showPinDialog = false },
      title = { Text("Set parental PIN") },
      text = {
        Column {
          Text("Enter a 4-digit code:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = pinInput,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          if (pinInput.length == 4) {
            repository.updateSettings(settings.copy(parentalLock = true, parentalPin = pinInput))
            showPinDialog = false
            Toast.makeText(context, "Parental PIN active", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
          }
        }) { Text("Enable") }
      },
      dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Cancel") } }
    )
  }
}

/** Rounded card with an icon + title above its content. */
@Composable
fun SettingsSection(
  title: String,
  icon: ImageVector,
  content: @Composable () -> Unit
) {
  Column(modifier = Modifier.padding(top = 8.dp)) {
    SettingsCategoryHeader(title = title, icon = icon)
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        content()
      }
    }
  }
}

@Composable
fun SettingsDivider() {
  Spacer(modifier = Modifier.height(10.dp))
  HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
  Spacer(modifier = Modifier.height(10.dp))
}

@Composable
fun SettingsActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  tint: Color = MaterialTheme.colorScheme.primary
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable { onClick() },
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Icon(
      Icons.Default.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun SettingsCategoryHeader(title: String, icon: ImageVector) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
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
      fontSize = 14.sp,
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
      Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
