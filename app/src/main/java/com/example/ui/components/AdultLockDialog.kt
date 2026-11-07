package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.repository.StreamRepository
import com.example.util.AdultLock

/**
 * Shown when someone taps the 18+ folder. Big lock icon + PIN box.
 *  - PIN already set (Settings -> Parental PIN lock): asks for it.
 *  - No PIN yet: asks the user to create a 4-digit PIN (twice), then opens the folder.
 */
@Composable
fun AdultLockDialog(
  repository: StreamRepository,
  onDismiss: () -> Unit,
  onUnlocked: () -> Unit
) {
  val settings = remember { repository.getSettings() }
  val hasPin = settings.parentalLock && settings.parentalPin.length == 4

  var firstPin by remember { mutableStateOf("") }   // creating: the first entry
  var input by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  val confirming = !hasPin && firstPin.isNotEmpty()
  val focus = remember { FocusRequester() }

  LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

  fun submit(pin: String) {
    when {
      hasPin -> {
        if (pin == settings.parentalPin) {
          AdultLock.unlock()
          onUnlocked()
        } else {
          input = ""
          error = "Wrong PIN. Try again."
        }
      }
      !confirming -> {
        firstPin = pin
        input = ""
        error = null
      }
      pin == firstPin -> {
        // Same PIN as Settings -> Parental PIN lock
        repository.updateSettings(settings.copy(parentalLock = true, parentalPin = pin))
        AdultLock.unlock()
        onUnlocked()
      }
      else -> {
        firstPin = ""
        input = ""
        error = "PINs did not match. Start again."
      }
    }
  }

  val subtitle = when {
    hasPin -> "This folder is locked. Enter your 4-digit PIN to open it."
    confirming -> "Enter the same PIN again to confirm."
    else -> "This folder is locked. Create a 4-digit PIN to protect it."
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(76.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "Locked",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp)
          )
        }
        Spacer(Modifier.height(16.dp))
        Text(
          text = "18+ Locked",
          fontSize = 20.sp,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text = subtitle,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
          textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
          value = input,
          onValueChange = { v ->
            val digits = v.filter { it.isDigit() }.take(4)
            input = digits
            error = null
            if (digits.length == 4) submit(digits)
          },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          label = { Text(if (confirming) "Confirm PIN" else if (hasPin) "PIN" else "New PIN") },
          isError = error != null,
          supportingText = { error?.let { Text(it) } },
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    }
  }
}
