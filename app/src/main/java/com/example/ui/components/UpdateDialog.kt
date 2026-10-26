package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUpdateInfo
import com.example.data.update.AppUpdater
import kotlinx.coroutines.launch
import java.io.File

/**
 * "Update available" prompt. Downloads the APK inside the app (with a progress bar) and then
 * opens Android's installer. A forced update cannot be dismissed.
 */
@Composable
fun UpdateDialog(
  info: AppUpdateInfo,
  onLater: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var downloading by remember { mutableStateOf(false) }
  var progress by remember { mutableIntStateOf(0) }
  var apk by remember { mutableStateOf<File?>(null) }
  var error by remember { mutableStateOf<String?>(null) }

  fun install(file: File) {
    if (!AppUpdater.canInstall(context)) {
      Toast.makeText(context, "Allow CastFlix to install apps, then tap Install again", Toast.LENGTH_LONG).show()
      AppUpdater.openInstallPermissionSettings(context)
      return
    }
    if (!AppUpdater.install(context, file)) {
      error = "Couldn't open the installer."
    }
  }

  fun startDownload() {
    error = null
    downloading = true
    progress = 0
    scope.launch {
      val file = AppUpdater.download(context, info) { progress = it }
      downloading = false
      if (file == null) {
        error = "Download failed. Check your connection and try again."
      } else {
        apk = file
        install(file)
      }
    }
  }

  AlertDialog(
    onDismissRequest = { if (!info.force && !downloading) onLater() },
    title = {
      Text(
        "Update available" + if (info.versionName.isNotBlank()) " (v${info.versionName})" else "",
        fontSize = 18.sp
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
          modifier = Modifier
            .heightIn(max = 180.dp)
            .verticalScroll(rememberScrollState())
        ) {
          Text(
            text = info.changelog.ifBlank { "A new version of CastFlix is ready." },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        if (info.force) {
          Text(
            "This update is required to keep using the app.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
          )
        }
        if (downloading) {
          Spacer(modifier = Modifier.height(4.dp))
          LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
          )
          Text("Downloading… $progress%", fontSize = 12.sp)
        }
        error?.let {
          Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
      }
    },
    confirmButton = {
      Button(
        enabled = !downloading,
        onClick = {
          val ready = apk
          if (ready != null && ready.exists()) install(ready) else startDownload()
        },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
      ) {
        Text(if (apk != null) "Install" else if (error != null) "Retry" else "Update now")
      }
    },
    dismissButton = {
      Column {
        if (error != null) {
          TextButton(onClick = { AppUpdater.openInBrowser(context, info.apkUrl) }) {
            Text("Open in browser")
          }
        }
        if (!info.force && !downloading) {
          TextButton(onClick = onLater) { Text("Later") }
        }
      }
    }
  )
}
