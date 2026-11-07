package com.example.data.auth

import android.util.Log
import com.example.data.local.FavoriteEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.UserProfile
import com.example.data.repository.StreamRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Cloud backup of Watchlist, Favorites and Profile (name / username / avatar) in Firestore,
 * under users/{uid}. Starts automatically when the user signs in with Google.
 * Local Room DB stays the source the UI reads from; this just mirrors it both ways.
 */
object CloudSync {
  private const val TAG = "CloudSync"
  private var started = false

  /** Human-readable sync state, shown in Settings under the account email. */
  val status = kotlinx.coroutines.flow.MutableStateFlow("")
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  fun start(repo: StreamRepository) {
    if (started) return
    started = true
    scope.launch {
      AuthManager.user.map { it?.uid }.distinctUntilChanged().collectLatest { uid ->
        if (uid == null) { status.value = ""; return@collectLatest }
        var attempt = 0
        while (true) {
          status.value = if (attempt == 0) "Connecting…" else "Retrying… (#$attempt)"
          try {
            syncForUser(repo, uid)
            break
          } catch (t: kotlinx.coroutines.CancellationException) {
            throw t
          } catch (t: Throwable) {
            Log.w(TAG, "Sync failed", t)
            val reason = t.message ?: t.javaClass.simpleName
            status.value = "Backup error: $reason\nChecking server…"
            status.value = "Backup error: $reason\nServer check: ${diagnose(uid)}"
            attempt++
            // Network may not be ready yet right after launch: keep trying with a growing pause
            kotlinx.coroutines.delay(minOf(30_000L, 4_000L * attempt))
          }
        }
      }
    }
  }

  private suspend fun syncForUser(repo: StreamRepository, uid: String) = coroutineScope {
    val db = FirebaseFirestore.getInstance()
    val userDoc = db.collection("users").document(uid)

    // ---- Profile: pull once, then keep pushing local edits ----
    val snap = userDoc.get().awaitTask()
    val local = repo.userProfile.value
    val isDefault = local.name == "Movie Fan" && local.username == "streamer_pro"
    if (snap.exists() && isDefault) {
      repo.updateUserProfile(
        UserProfile(
          name = snap.getString("name") ?: local.name,
          username = snap.getString("username") ?: local.username,
          avatarIndex = snap.getLong("avatarIndex")?.toInt() ?: local.avatarIndex
        )
      )
    } else if (!snap.exists() && isDefault) {
      AuthManager.user.value?.name?.let { repo.updateUserProfile(local.copy(name = it)) }
    }
    launch {
      repo.userProfile.collect { p ->
        userDoc.set(
          mapOf("name" to p.name, "username" to p.username, "avatarIndex" to p.avatarIndex),
          SetOptions.merge()
        ).awaitTask()
      }
    }

    // ---- Watchlist ----
    launch {
      mirror(
        col = userDoc.collection("watchlist"),
        local = repo.watchlist,
        idOf = { it.mediaId },
        toMap = { w ->
          mapOf(
            "mediaId" to w.mediaId, "title" to w.title, "poster" to w.poster, "banner" to w.banner,
            "category" to w.category, "year" to w.year, "rating" to w.rating,
            "streamUrl" to w.streamUrl, "isSeries" to w.isSeries, "addedAt" to w.addedAt
          )
        },
        fromDoc = { d ->
          WatchlistEntity(
            mediaId = d.getString("mediaId") ?: return@mirror null,
            title = d.getString("title") ?: "",
            poster = d.getString("poster") ?: "",
            banner = d.getString("banner") ?: "",
            category = d.getString("category") ?: "",
            year = d.getLong("year")?.toInt() ?: 0,
            rating = d.getDouble("rating") ?: 0.0,
            streamUrl = d.getString("streamUrl") ?: "",
            isSeries = d.getBoolean("isSeries") ?: false,
            addedAt = d.getLong("addedAt") ?: System.currentTimeMillis()
          )
        },
        insert = { repo.watchlistDao.insertWatchlist(it) }
      )
    }

    // ---- Favorites ----
    launch {
      mirror(
        col = userDoc.collection("favorites"),
        local = repo.favorites,
        idOf = { it.mediaId },
        toMap = { f ->
          mapOf(
            "mediaId" to f.mediaId, "title" to f.title, "poster" to f.poster, "banner" to f.banner,
            "category" to f.category, "year" to f.year, "rating" to f.rating,
            "streamUrl" to f.streamUrl, "isSeries" to f.isSeries, "addedAt" to f.addedAt
          )
        },
        fromDoc = { d ->
          FavoriteEntity(
            mediaId = d.getString("mediaId") ?: return@mirror null,
            title = d.getString("title") ?: "",
            poster = d.getString("poster") ?: "",
            banner = d.getString("banner") ?: "",
            category = d.getString("category") ?: "",
            year = d.getLong("year")?.toInt() ?: 0,
            rating = d.getDouble("rating") ?: 0.0,
            streamUrl = d.getString("streamUrl") ?: "",
            isSeries = d.getBoolean("isSeries") ?: false,
            addedAt = d.getLong("addedAt") ?: System.currentTimeMillis()
          )
        },
        insert = { repo.favoriteDao.insertFavorite(it) }
      )
    }
  }

  /** Cloud items are merged into local first (union), then every local change is mirrored to the cloud. */
  private suspend fun <T : Any> mirror(
    col: CollectionReference,
    local: Flow<List<T>>,
    idOf: (T) -> String,
    toMap: (T) -> Map<String, Any>,
    fromDoc: (DocumentSnapshot) -> T?,
    insert: suspend (T) -> Unit
  ) {
    val cloudDocs = col.get().awaitTask().documents
    cloudDocs.mapNotNull { fromDoc(it) }.forEach { insert(it) }
    val known = cloudDocs.map { it.id }.toMutableSet()
    status.value = "Cloud backup on ✓"

    local.collect { items ->
      val byDoc = items.associateBy { docId(idOf(it)) }
      val toAdd = byDoc.keys - known
      val toRemove = known - byDoc.keys
      if (toAdd.isEmpty() && toRemove.isEmpty()) return@collect
      val batch = col.firestore.batch()
      toAdd.forEach { id -> batch.set(col.document(id), toMap(byDoc.getValue(id))) }
      toRemove.forEach { id -> batch.delete(col.document(id)) }
      batch.commit().awaitTask()
      known.addAll(toAdd)
      known.removeAll(toRemove)
    }
  }

  /** Plain HTTPS call to Firestore (no gRPC) so we can see the real reason when the SDK says "offline". */
  private suspend fun diagnose(uid: String): String = kotlinx.coroutines.withContext(Dispatchers.IO) {
    try {
      val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        ?: return@withContext "not signed in"
      val token = user.getIdToken(false).awaitTask().token
      val project = com.google.firebase.FirebaseApp.getInstance().options.projectId
      val req = okhttp3.Request.Builder()
        .url("https://firestore.googleapis.com/v1/projects/$project/databases/(default)/documents/users/$uid")
        .header("Authorization", "Bearer $token")
        .build()
      okhttp3.OkHttpClient().newCall(req).execute().use { r ->
        val body = r.body?.string().orEmpty()
        val msg = try {
          org.json.JSONObject(body).getJSONObject("error").getString("message")
        } catch (_: Exception) { "" }
        val verdict = if (r.code == 200 || r.code == 404) " (server reachable)" else ""
        "HTTP ${r.code}$verdict $msg".take(220)
      }
    } catch (e: Exception) {
      "no connection: ${e.javaClass.simpleName} ${e.message ?: ""}".take(220)
    }
  }

  private fun docId(mediaId: String) = mediaId.replace('/', '_').ifBlank { "_" }

  private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
  }
}
