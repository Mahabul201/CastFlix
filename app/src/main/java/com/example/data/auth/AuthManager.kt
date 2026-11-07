package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class AuthUser(
  val uid: String,
  val name: String,
  val email: String,
  val photoUrl: String?
)

sealed class SignInResult {
  data class Success(val user: AuthUser) : SignInResult()
  data object Cancelled : SignInResult()
  data class Error(val message: String) : SignInResult()
}

/** Google Sign-In via Credential Manager + Firebase Auth. Safe no-op when Firebase isn't configured. */
object AuthManager {
  private const val TAG = "AuthManager"

  private val _user = MutableStateFlow<AuthUser?>(null)
  val user: StateFlow<AuthUser?> = _user.asStateFlow()

  private var auth: FirebaseAuth? = null

  /** True only when google-services.json is present AND the web client ID is set. */
  val isConfigured: Boolean
    get() = auth != null && webClientId.isNotBlank()

  private val webClientId: String
    get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.takeUnless { it.startsWith("YOUR_") } ?: ""

  fun init(context: Context) {
    if (auth != null) return
    try {
      if (FirebaseApp.getApps(context).isEmpty() && FirebaseApp.initializeApp(context) == null) {
        Log.w(TAG, "Firebase not configured (google-services.json missing)")
        return
      }
      val fa = FirebaseAuth.getInstance()
      auth = fa
      fa.addAuthStateListener { _user.value = it.currentUser?.toAuthUser() }
      _user.value = fa.currentUser?.toAuthUser()
    } catch (t: Throwable) {
      Log.w(TAG, "Auth init failed: ${t.message}")
    }
  }

  suspend fun signInWithGoogle(context: Context): SignInResult {
    val fa = auth
    if (fa == null || webClientId.isBlank()) {
      return SignInResult.Error("Google Sign-In is not configured yet (google-services.json / web client ID).")
    }
    val activity = context.findActivity()
      ?: return SignInResult.Error("Sign-in needs an active screen.")
    return try {
      val option = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(false)
        .build()
      val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
      val credential = CredentialManager.create(activity).getCredential(activity, request).credential

      if (credential !is CustomCredential ||
        credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
      ) {
        return SignInResult.Error("Unexpected credential type.")
      }
      val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
      val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
      val result = suspendCancellableCoroutine { cont ->
        fa.signInWithCredential(firebaseCred)
          .addOnSuccessListener { cont.resume(it) }
          .addOnFailureListener { cont.resumeWithException(it) }
      }
      val u = result.user?.toAuthUser() ?: return SignInResult.Error("Sign-in failed.")
      _user.value = u
      SignInResult.Success(u)
    } catch (e: GetCredentialCancellationException) {
      SignInResult.Cancelled
    } catch (e: Exception) {
      Log.w(TAG, "Sign-in failed", e)
      SignInResult.Error(e.message ?: "Sign-in failed.")
    }
  }

  suspend fun signOut(context: Context) {
    auth?.signOut()
    _user.value = null
    try {
      CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    } catch (_: Exception) {
    }
  }

  private fun com.google.firebase.auth.FirebaseUser.toAuthUser() = AuthUser(
    uid = uid,
    name = displayName ?: email?.substringBefore('@') ?: "Movie Fan",
    email = email ?: "",
    photoUrl = photoUrl?.toString()
  )

  private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
      if (c is Activity) return c
      c = c.baseContext
    }
    return null
  }
}
