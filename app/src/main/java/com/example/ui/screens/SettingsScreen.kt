package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.data.model.UserProfile
import com.example.data.repository.StreamRepository
import com.example.ui.theme.AppAccent
import com.example.ui.theme.Dimens
import com.example.util.ShareUtils
import kotlinx.coroutines.launch
import com.example.data.auth.AuthManager
import com.example.data.auth.SignInResult
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.Logout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TELEGRAM_JOIN_URL = "https://t.me/+8pRE58uYZZIzYjJl"
private const val PREFS = "url_stream_prefs"

// ---------- language (English / Hindi) for this page ----------
private val LocalHi = compositionLocalOf { false }

@Composable
private fun tr(en: String, hi: String): String = if (LocalHi.current) hi else en

// ---------- glass helpers (kept in this file so only one file changes) ----------
@Composable
private fun Modifier.glass(shape: Shape, strength: Float = 1f): Modifier {
  val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
  val top = if (light) 0.78f else 0.16f
  val bottom = if (light) 0.50f else 0.06f
  val edge = if (light) Color.Black else Color.White
  return this
    .clip(shape)
    .background(
      Brush.linearGradient(
        listOf(Color.White.copy(alpha = top * strength), Color.White.copy(alpha = bottom * strength))
      )
    )
    .border(
      1.dp,
      Brush.linearGradient(
        listOf(edge.copy(alpha = if (light) 0.10f else 0.30f), edge.copy(alpha = if (light) 0.03f else 0.05f))
      ),
      shape
    )
}

@Composable
private fun AuroraPage(content: @Composable BoxScope.() -> Unit) {
  val primary = MaterialTheme.colorScheme.primary
  val bg = MaterialTheme.colorScheme.background
  val light = bg.luminance() > 0.5f
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(bg)
      .drawBehind {
        val r = size.width * 0.95f
        drawRect(
          Brush.radialGradient(
            listOf(primary.copy(alpha = if (light) 0.22f else 0.30f), Color.Transparent),
            center = Offset(size.width * 0.95f, size.height * 0.06f),
            radius = r
          )
        )
        drawRect(
          Brush.radialGradient(
            listOf(Color(0xFF3B82F6).copy(alpha = if (light) 0.14f else 0.18f), Color.Transparent),
            center = Offset(size.width * 0.02f, size.height * 0.62f),
            radius = r
          )
        )
      },
    content = content
  )
}

private fun sdCardDir(context: Context): java.io.File? = try {
  context.getExternalFilesDirs(Environment.DIRECTORY_MOVIES)
    .drop(1)
    .firstOrNull { it != null && Environment.isExternalStorageRemovable(it) }
} catch (_: Exception) {
  null
}

/** One lazy item whose children are stacked top to bottom. */
private fun LazyListScope.block(content: @Composable ColumnScope.() -> Unit) {
  item { Column(modifier = Modifier.fillMaxWidth(), content = content) }
}

@Composable
fun SettingsScreen(
  repository: StreamRepository,
  updateAvailable: Boolean = false,
  onCheckUpdate: () -> Unit = {},
  onPlayMedia: (com.example.data.model.MediaItem, Long) -> Unit = { _, _ -> },
  onOpenHistory: () -> Unit = {},
  onOpenNotifications: () -> Unit = {}
) {
  val profileState = rememberProfileState(repository)
  val context = LocalContext.current
  val settings by repository.settings.collectAsState()
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()
  val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

  // Settings kept in prefs so no other file has to change
  var language by remember { mutableStateOf(prefs.getString("language", "EN") ?: "EN") }
  var downloadLocation by remember { mutableStateOf(prefs.getString("downloadLocation", "PHONE") ?: "PHONE") }
  var mobileDataSaver by remember { mutableStateOf(prefs.getBoolean("mobileDataSaver", false)) }
  var quality by remember { mutableStateOf(repository.getPreferredQuality() ?: "720p") }
  val sdAvailable = remember { sdCardDir(context) != null }

  var showMyList by remember { mutableStateOf(false) }
  var showAbout by remember { mutableStateOf(false) }
  var showClearHistoryDialog by remember { mutableStateOf(false) }
  var showClearCacheDialog by remember { mutableStateOf(false) }
  var showPinDialog by remember { mutableStateOf(false) }
  var showUnlockDialog by remember { mutableStateOf(false) }
  var pinInput by remember { mutableStateOf("") }
  var unlockInput by remember { mutableStateOf("") }

  val lastUpdated = remember {
    try {
      val t = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
      SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(t))
    } catch (_: Exception) {
      "-"
    }
  }

  CompositionLocalProvider(LocalHi provides (language == "HI")) {
    AuroraPage {
      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = Dimens.ScreenHorizontal)
          .testTag("settings_screen"),
        contentPadding = PaddingValues(bottom = Dimens.ScreenBottom),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        block {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            tr("Settings", "सेटिंग्स"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(10.dp))
        }

        // ===== Profile (the only box) =====
        item { GlassProfileCard(repository, profileState) }

        // ===== Profile / Notification / My List =====
        block {
          NavRow(
            icon = Icons.Default.Person,
            title = tr("Profile", "प्रोफ़ाइल"),
            subtitle = tr("Change name and avatar", "नाम और फोटो बदलें"),
            onClick = {
              profileState.isEditing = true
              scope.launch { listState.animateScrollToItem(1) }
            }
          )
          NavRow(
            icon = Icons.Default.Notifications,
            title = tr("Notification", "नोटिफिकेशन"),
            subtitle = tr("Open your notification inbox", "अपने नोटिफिकेशन देखें"),
            onClick = onOpenNotifications
          )
          ToggleRow(
            icon = Icons.Default.Notifications,
            title = tr("Push notifications", "पुश नोटिफिकेशन"),
            subtitle = tr("New releases and news", "नई रिलीज़ और खबरें"),
            checked = settings.notificationsEnabled,
            onChange = { repository.updateSettings(settings.copy(notificationsEnabled = it)) }
          )
          NavRow(
            icon = Icons.Default.Bookmark,
            title = tr("My List", "मेरी लिस्ट"),
            subtitle = tr("Favorites and watchlist", "पसंदीदा और वॉचलिस्ट"),
            onClick = { showMyList = !showMyList },
            trailing = {
              Icon(
                Icons.Default.ExpandMore, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (showMyList) 180f else 0f)
              )
            }
          )
        }
        if (showMyList) profileSavedItems(repository, profileState, onPlayMedia)

        // ===== Theme =====
        block {
          SectionLabel(tr("Theme", "थीम"), Icons.Default.Palette)
          ChipRow(
            options = listOf(
              "DARK" to tr("Dark", "डार्क"),
              "LIGHT" to tr("Light", "लाइट"),
              "AMOLED" to "AMOLED"
            ),
            selected = settings.themeMode,
            onSelect = { repository.updateSettings(settings.copy(themeMode = it)) }
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AppAccent.values().forEach { accent ->
              val sel = settings.accentColor == accent.name
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(accent.color)
                  .border(if (sel) 2.dp else 0.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                  .clickable { repository.updateSettings(settings.copy(accentColor = accent.name)) },
                contentAlignment = Alignment.Center
              ) {
                if (sel) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
              }
            }
          }
        }

        // ===== Video player control =====
        block {
          SectionLabel(tr("Video player control", "वीडियो प्लेयर कंट्रोल"), Icons.Default.PlayCircle)
          ToggleRow(
            icon = Icons.Default.PlayCircle,
            title = tr("Auto play next episodes", "अगला एपिसोड अपने आप चलाएँ"),
            subtitle = tr("Start the next episode when one ends", "एपिसोड खत्म होते ही अगला शुरू हो"),
            checked = settings.autoplayNext,
            onChange = { repository.updateSettings(settings.copy(autoplayNext = it)) }
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            tr("Default video quality", "डिफ़ॉल्ट वीडियो क्वालिटी"),
            fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
          )
          ChipRow(
            options = listOf("480p" to "480p", "720p" to "720p", "1080p" to "1080p"),
            selected = quality,
            onSelect = {
              quality = it
              repository.setPreferredQuality(it)
            }
          )
          Spacer(modifier = Modifier.height(6.dp))
          ToggleRow(
            icon = Icons.Default.PlayCircle,
            title = tr("Keep screen on", "स्क्रीन चालू रखें"),
            subtitle = tr("Screen stays awake while a video plays", "वीडियो चलते समय स्क्रीन बंद न हो"),
            checked = settings.keepScreenOn,
            onChange = { repository.updateSettings(settings.copy(keepScreenOn = it)) }
          )
          ToggleRow(
            icon = Icons.Default.PlayCircle,
            title = tr("Auto activate miniplayer", "मिनी प्लेयर अपने आप चालू करें"),
            subtitle = tr("Small window when you press Home", "Home दबाने पर छोटी विंडो में चले"),
            checked = settings.pipMode,
            onChange = { repository.updateSettings(settings.copy(pipMode = it)) }
          )
          ToggleRow(
            icon = Icons.Default.PlayCircle,
            title = tr("Background audio play", "बैकग्राउंड ऑडियो"),
            subtitle = tr("Keep playing when the screen is off", "स्क्रीन बंद होने पर भी चलता रहे"),
            checked = settings.backgroundPlay,
            onChange = { repository.updateSettings(settings.copy(backgroundPlay = it)) }
          )
        }

        // ===== Download settings =====
        block {
          SectionLabel(tr("Download", "डाउनलोड"), Icons.Default.Download)
          Text(
            tr("Set location", "लोकेशन चुनें"),
            fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
          )
          if (sdAvailable) {
            ChipRow(
              options = listOf(
                "PHONE" to tr("Phone storage", "फोन स्टोरेज"),
                "SD" to tr("SD card", "SD कार्ड")
              ),
              selected = downloadLocation,
              onSelect = {
                downloadLocation = it
                prefs.edit().putString("downloadLocation", it).apply()
              }
            )
          } else {
            Text(
              tr("Phone storage (no SD card found)", "फोन स्टोरेज (SD कार्ड नहीं मिला)"),
              fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(start = 2.dp)
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          ToggleRow(
            icon = Icons.Default.DataSaverOn,
            title = tr("Mobile data", "मोबाइल डेटा"),
            subtitle = tr("Don't download big files on mobile data", "मोबाइल डेटा पर बड़ी फ़ाइलें डाउनलोड न करें"),
            checked = mobileDataSaver,
            onChange = {
              mobileDataSaver = it
              prefs.edit().putBoolean("mobileDataSaver", it).apply()
            }
          )
        }

        // ===== Privacy & parental lock =====
        block {
          SectionLabel(tr("Privacy & parental lock", "प्राइवेसी और पैरेंटल लॉक"), Icons.Default.Lock)
          NavRow(
            icon = Icons.Default.History,
            title = tr("Watch history", "वॉच हिस्ट्री"),
            subtitle = tr("See what you watched", "आपने क्या देखा"),
            onClick = onOpenHistory
          )
          NavRow(
            icon = Icons.Default.History,
            title = tr("Clear watch history", "वॉच हिस्ट्री साफ़ करें"),
            subtitle = tr("Remove all resume progress", "सारी प्रोग्रेस हटाएँ"),
            tint = MaterialTheme.colorScheme.error,
            onClick = { showClearHistoryDialog = true }
          )
          NavRow(
            icon = Icons.Default.DeleteSweep,
            title = tr("Clear cache", "कैश साफ़ करें"),
            subtitle = tr("Free temporary images and buffers", "अस्थायी फ़ाइलें हटाएँ"),
            onClick = { showClearCacheDialog = true }
          )
          ToggleRow(
            icon = Icons.Default.Lock,
            title = tr("Parental PIN lock", "पैरेंटल PIN लॉक"),
            subtitle = if (settings.parentalLock) tr("PIN is active", "PIN चालू है")
            else tr("Protect with a 4-digit code", "4 अंकों के कोड से सुरक्षित करें"),
            checked = settings.parentalLock,
            onChange = { checked ->
              if (checked) { pinInput = ""; showPinDialog = true }
              else { unlockInput = ""; showUnlockDialog = true }
            }
          )
        }

        // ===== Language =====
        block {
          SectionLabel(tr("App language", "ऐप की भाषा"), Icons.Default.Language)
          ChipRow(
            options = listOf("EN" to "English", "HI" to "हिन्दी"),
            selected = language,
            onSelect = {
              language = it
              prefs.edit().putString("language", it).apply()
            }
          )
        }

        // ===== Community & app =====
        block {
          SectionLabel(tr("Community", "कम्युनिटी"), Icons.Default.Send)
          NavRow(
            icon = Icons.Default.Send,
            title = tr("Join Telegram", "टेलीग्राम से जुड़ें"),
            subtitle = tr("News, requests and updates", "खबरें, रिक्वेस्ट और अपडेट"),
            tint = Color(0xFF229ED9),
            onClick = {
              try {
                context.startActivity(
                  Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_JOIN_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
              } catch (e: Exception) {
                Toast.makeText(context, "Unable to open Telegram", Toast.LENGTH_SHORT).show()
              }
            }
          )
          NavRow(
            icon = Icons.Default.Share,
            title = tr("Share app", "ऐप शेयर करें"),
            subtitle = tr("Tell your friends about CastFlix", "दोस्तों को CastFlix बताएँ"),
            onClick = { ShareUtils.shareApp(context) }
          )
        }

        block {
          SectionLabel(tr("App", "ऐप"), Icons.Default.SystemUpdate)
          NavRow(
            icon = Icons.Default.SystemUpdate,
            title = if (updateAvailable) tr("Update available", "नया अपडेट उपलब्ध है")
            else tr("Check for updates", "अपडेट जाँचें"),
            subtitle = tr("Version", "वर्ज़न") + " ${BuildConfig.VERSION_NAME} • " +
              tr("Last updated", "आखिरी अपडेट") + ": $lastUpdated",
            onClick = onCheckUpdate
          )
          ToggleRow(
            icon = Icons.Default.SystemUpdate,
            title = tr("Check for updates on launch", "ऐप खुलते ही अपडेट जाँचें"),
            subtitle = tr("Prompt when a new version is published", "नया वर्ज़न आने पर बताएँ"),
            checked = settings.autoCheckUpdate,
            onChange = { repository.updateSettings(settings.copy(autoCheckUpdate = it)) }
          )
          NavRow(
            icon = Icons.Default.Info,
            title = tr("About us", "हमारे बारे में"),
            subtitle = "CastFlix",
            onClick = { showAbout = true }
          )
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }

    // ---------- dialogs ----------
    val dialogColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    val pinLengthMsg = tr("PIN must be 4 digits", "PIN 4 अंकों का होना चाहिए")
    val wrongPinMsg = tr("Wrong PIN", "गलत PIN")

    if (showAbout) {
      AlertDialog(
        onDismissRequest = { showAbout = false },
        containerColor = dialogColor,
        title = { Text(tr("About us", "हमारे बारे में")) },
        text = {
          Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text("CastFlix • v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              tr(
                "CastFlix is a simple streaming app for movie, web series and anime lovers. " +
                  "Watch your favourites in one place with quality choice, resume where you stopped, " +
                  "downloads and a smooth player.\n\n" +
                  "We keep adding new titles and features. Join our Telegram to request a title " +
                  "or share feedback.\n\nThank you for being part of CastFlix.",
                "CastFlix मूवी, वेब सीरीज़ और एनिमे पसंद करने वालों के लिए एक आसान स्ट्रीमिंग ऐप है। " +
                  "अपनी पसंदीदा चीज़ें एक ही जगह देखें: क्वालिटी चुनें, जहाँ छोड़ा वहीं से फिर चलाएँ, " +
                  "डाउनलोड करें और स्मूथ प्लेयर का मज़ा लें।\n\n" +
                  "हम लगातार नए टाइटल और फीचर जोड़ते रहते हैं। कोई टाइटल चाहिए या सुझाव देना हो " +
                  "तो हमारे टेलीग्राम से जुड़ें।\n\nCastFlix का हिस्सा बनने के लिए धन्यवाद।"
              ),
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        confirmButton = { TextButton(onClick = { showAbout = false }) { Text(tr("Close", "बंद करें")) } }
      )
    }

    if (showClearHistoryDialog) {
      AlertDialog(
        onDismissRequest = { showClearHistoryDialog = false },
        containerColor = dialogColor,
        title = { Text(tr("Clear watch history?", "वॉच हिस्ट्री साफ़ करें?")) },
        text = { Text(tr("All saved playback positions will be erased.", "सारी सेव की हुई प्रोग्रेस मिट जाएगी।")) },
        confirmButton = {
          Button(
            onClick = {
              scope.launch {
                repository.clearHistory()
                showClearHistoryDialog = false
                Toast.makeText(context, "OK", Toast.LENGTH_SHORT).show()
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
          ) { Text(tr("Clear", "साफ़ करें")) }
        },
        dismissButton = { TextButton(onClick = { showClearHistoryDialog = false }) { Text(tr("Cancel", "रद्द करें")) } }
      )
    }

    if (showClearCacheDialog) {
      AlertDialog(
        onDismissRequest = { showClearCacheDialog = false },
        containerColor = dialogColor,
        title = { Text(tr("Clear cache?", "कैश साफ़ करें?")) },
        text = { Text(tr("Temporary images and buffers will be deleted.", "अस्थायी इमेज और बफर हट जाएँगे।")) },
        confirmButton = {
          Button(onClick = {
            context.cacheDir.deleteRecursively()
            showClearCacheDialog = false
            Toast.makeText(context, "OK", Toast.LENGTH_SHORT).show()
          }) { Text(tr("Clear cache", "कैश साफ़ करें")) }
        },
        dismissButton = { TextButton(onClick = { showClearCacheDialog = false }) { Text(tr("Cancel", "रद्द करें")) } }
      )
    }

    if (showPinDialog) {
      PinDialog(
        title = tr("Set parental PIN", "पैरेंटल PIN सेट करें"),
        value = pinInput,
        onValue = { pinInput = it },
        onDismiss = { showPinDialog = false },
        onConfirm = {
          if (pinInput.length == 4) {
            repository.updateSettings(settings.copy(parentalLock = true, parentalPin = pinInput))
            showPinDialog = false
          } else {
            Toast.makeText(context, pinLengthMsg, Toast.LENGTH_SHORT).show()
          }
        }
      )
    }

    if (showUnlockDialog) {
      PinDialog(
        title = tr("Enter PIN to turn off", "बंद करने के लिए PIN डालें"),
        value = unlockInput,
        onValue = { unlockInput = it },
        onDismiss = { showUnlockDialog = false },
        onConfirm = {
          if (unlockInput == settings.parentalPin) {
            repository.updateSettings(settings.copy(parentalLock = false))
            showUnlockDialog = false
          } else {
            unlockInput = ""
            Toast.makeText(context, wrongPinMsg, Toast.LENGTH_SHORT).show()
          }
        }
      )
    }
  }
}

// ======================= pieces =======================

@Composable
private fun PinDialog(
  title: String,
  value: String,
  onValue: (String) -> Unit,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
    title = { Text(title) },
    text = {
      OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) onValue(it) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
      )
    },
    confirmButton = { Button(onClick = onConfirm) { Text(tr("OK", "ठीक है")) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "रद्द करें")) } }
  )
}

@Composable
private fun SectionLabel(text: String, icon: ImageVector) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(start = 2.dp, top = 22.dp, bottom = 8.dp)
  ) {
    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(8.dp))
    Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
  }
}

@Composable
private fun GlassIcon(icon: ImageVector, tint: Color) {
  Box(
    modifier = Modifier
      .size(38.dp)
      .glass(CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
  }
}

@Composable
private fun RowText(title: String, subtitle: String?, modifier: Modifier, tint: Color? = null) {
  Column(modifier = modifier) {
    Text(
      title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
      color = tint ?: MaterialTheme.colorScheme.onSurface
    )
    if (!subtitle.isNullOrBlank()) {
      Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun NavRow(
  icon: ImageVector,
  title: String,
  subtitle: String?,
  onClick: () -> Unit,
  tint: Color = MaterialTheme.colorScheme.primary,
  trailing: @Composable () -> Unit = {
    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
  }
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    GlassIcon(icon, tint)
    Spacer(modifier = Modifier.width(12.dp))
    RowText(title, subtitle, Modifier.weight(1f), if (tint == MaterialTheme.colorScheme.error) tint else null)
    trailing()
  }
}

@Composable
private fun ToggleRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    GlassIcon(icon, MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.width(12.dp))
    RowText(title, subtitle, Modifier.weight(1f))
    Spacer(modifier = Modifier.width(8.dp))
    Switch(
      checked = checked,
      onCheckedChange = onChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = MaterialTheme.colorScheme.primary
      )
    )
  }
}

@Composable
private fun ChipRow(
  options: List<Pair<String, String>>,
  selected: String,
  onSelect: (String) -> Unit
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    options.forEach { (key, label) ->
      val sel = key == selected
      val shape = RoundedCornerShape(20.dp)
      Box(
        modifier = Modifier
          .weight(1f)
          .then(
            if (sel) Modifier.clip(shape).background(MaterialTheme.colorScheme.primary)
            else Modifier.glass(shape)
          )
          .clickable { onSelect(key) }
          .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          label,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          maxLines = 1,
          color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
private fun GlassProfileCard(repository: StreamRepository, state: ProfileState) {
  val context = LocalContext.current
  val shape = RoundedCornerShape(24.dp)
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .glass(shape, strength = 1.15f)
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(78.dp)
        .clip(CircleShape)
        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
    ) {
      AsyncImage(
        model = state.authUser?.photoUrl ?: AVATAR_OPTIONS.getOrElse(state.selectedAvatarIndex) { AVATAR_OPTIONS[0] },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    }
    Spacer(modifier = Modifier.height(8.dp))

    if (state.isEditing) {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        items(AVATAR_OPTIONS.indices.toList()) { idx ->
          val sel = state.selectedAvatarIndex == idx
          Box(
            modifier = Modifier
              .size(50.dp)
              .clip(CircleShape)
              .border(if (sel) 3.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
              .clickable { state.selectedAvatarIndex = idx }
          ) {
            AsyncImage(
              model = AVATAR_OPTIONS[idx], contentDescription = null,
              contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      OutlinedTextField(
        value = state.nameInput, onValueChange = { state.nameInput = it },
        label = { Text(tr("Display name", "नाम")) }, singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = state.usernameInput, onValueChange = { state.usernameInput = it },
        label = { Text(tr("Username", "यूज़रनेम")) }, singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(12.dp))
      Button(
        onClick = {
          repository.updateUserProfile(
            UserProfile(
              name = state.nameInput.trim().ifBlank { "Movie Fan" },
              username = state.usernameInput.trim().replace(" ", "_").ifBlank { "streamer_pro" },
              avatarIndex = state.selectedAvatarIndex
            )
          )
          state.isEditing = false
          Toast.makeText(context, "OK", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
      ) {
        Icon(Icons.Default.Check, null, tint = Color.White)
        Spacer(modifier = Modifier.width(6.dp))
        Text(tr("Save changes", "सेव करें"), color = Color.White)
      }
    } else {
      Text(
        state.profile.name,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        "@${state.profile.username}",
        color = MaterialTheme.colorScheme.primary,
        fontSize = 13.sp, fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        StatPair(String.format("%.1fh", state.totalWatchHours), tr("Watched", "देखा"))
        StatPair("${state.historyCount}", tr("Played", "चलाया"))
        StatPair("${state.watchlist.size}", tr("Watchlist", "वॉचलिस्ट"))
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier
          .glass(RoundedCornerShape(20.dp))
          .clickable { state.isEditing = true }
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(tr("Edit profile", "प्रोफ़ाइल बदलें"), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
      }
      Spacer(modifier = Modifier.height(10.dp))
      val signedIn = state.authUser
      if (signedIn == null) {
        Button(
          enabled = !state.signingIn,
          onClick = {
            state.signingIn = true
            state.scope.launch {
              when (val r = AuthManager.signInWithGoogle(context)) {
                is SignInResult.Success -> {
                  if (state.profile.name == "Movie Fan") {
                    repository.updateUserProfile(state.profile.copy(name = r.user.name))
                  }
                  Toast.makeText(context, "Signed in as ${r.user.email}", Toast.LENGTH_SHORT).show()
                }
                is SignInResult.Error -> Toast.makeText(context, r.message, Toast.LENGTH_LONG).show()
                SignInResult.Cancelled -> Unit
              }
              state.signingIn = false
            }
          },
          shape = RoundedCornerShape(20.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1F1F1F)),
          contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
          modifier = Modifier.testTag("google_sign_in")
        ) {
          Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (state.signingIn) tr("Signing in…", "साइन इन हो रहा है…") else tr("Sign in with Google", "Google से साइन इन करें"),
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold
          )
        }
      } else {
        Text(signedIn.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { state.scope.launch { AuthManager.signOut(context) } }) {
          Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(tr("Sign out", "साइन आउट"), fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun StatPair(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
