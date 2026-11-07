package com.example.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Returns a function that opens Google's voice recognition screen.
 * The spoken text comes back in [onResult].
 */
@Composable
fun rememberVoiceSearch(onResult: (String) -> Unit): () -> Unit {
  val context = LocalContext.current
  val latest = rememberUpdatedState(onResult)
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
    if (res.resultCode == Activity.RESULT_OK) {
      val text = res.data
        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        ?.firstOrNull()
        ?.trim()
        .orEmpty()
      if (text.isNotEmpty()) latest.value(text)
    }
  }
  return {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_PROMPT, "Movie ya series ka naam bolo")
    }
    try {
      launcher.launch(intent)
    } catch (e: ActivityNotFoundException) {
      Toast.makeText(context, "Is phone me voice search available nahi hai", Toast.LENGTH_SHORT).show()
    }
  }
}
