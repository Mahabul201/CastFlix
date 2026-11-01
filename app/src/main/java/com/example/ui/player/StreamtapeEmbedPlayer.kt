package com.example.ui.player

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.provider.Settings
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Turns a CastFlix stream link into the Streamtape EMBED link (the player that shows
 * Streamtape's ads). Returns null when the link is not a Streamtape / bot link,
 * so normal ExoPlayer playback is used instead.
 *
 *  https://<bot>/s/<id>, /se/<id>, /sv/<id>/name.mp4  ->  https://<bot>/se/<id>
 *  https://streamtape.com/v/<id>/name                  ->  https://streamtape.com/e/<id>
 */
fun toStreamtapeEmbed(url: String): String? {
  if (url.isBlank()) return null
  Regex("""^(https?://[^/]+)/(?:s|se|sv)/([A-Za-z0-9_-]+)""").find(url)?.let {
    return "${it.groupValues[1]}/se/${it.groupValues[2]}"
  }
  Regex("""^https?://(?:www\.)?(?:streamtape|strtape|stape)\.[a-z.]+/(?:v|e)/([A-Za-z0-9_-]+)""", RegexOption.IGNORE_CASE)
    .find(url)?.let { return "https://streamtape.com/e/${it.groupValues[1]}" }
  return null
}

/** Picture-in-picture for the whole player screen (the WebView keeps playing in the small window). */
fun enterStreamtapePip(activity: Activity) {
  if (Build.VERSION.SDK_INT >= 26) {
    try {
      activity.enterPictureInPictureMode(
        PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
      )
    } catch (_: Exception) {
    }
  }
}

/** Screen cast = Android's own screen mirroring (Smart View / Cast to TV). */
fun openCastSettings(context: Context) {
  try {
    context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  } catch (_: Exception) {
    Toast.makeText(context, "Open Cast / Smart View from your quick settings", Toast.LENGTH_LONG).show()
  }
}

@Composable
fun rememberIsInPip(): Boolean {
  val activity = LocalContext.current as? ComponentActivity
  var inPip by remember { mutableStateOf(false) }
  DisposableEffect(activity) {
    val listener = androidx.core.util.Consumer<PictureInPictureModeChangedInfo> {
      inPip = it.isInPictureInPictureMode
    }
    activity?.addOnPictureInPictureModeChangedListener(listener)
    onDispose { activity?.removeOnPictureInPictureModeChangedListener(listener) }
  }
  return inPip
}

private fun setImmersive(activity: Activity, on: Boolean) {
  val c = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
  if (on) {
    c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    c.hide(WindowInsetsCompat.Type.systemBars())
  } else {
    c.show(WindowInsetsCompat.Type.systemBars())
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StreamtapeEmbedPlayer(
  url: String,
  modifier: Modifier = Modifier,
  initialPositionMs: Long = 0L,
  onProgress: ((positionMs: Long, durationMs: Long) -> Unit)? = null
) {
  val context = LocalContext.current
  // The ad opens ONCE per video: the first tap on play goes to the ad, later taps just play.
  val adOpened = remember { booleanArrayOf(false) }
  val openAdOnce: (Uri) -> Unit = remember {
    { u ->
      if (!adOpened[0]) {
        adOpened[0] = true
        try {
          context.startActivity(Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
      }
    }
  }
  var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
  val webView = remember {
    WebView(context).apply {
      setBackgroundColor(android.graphics.Color.BLACK)
      layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        mediaPlaybackRequiresUserGesture = false
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(true)
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        userAgentString = userAgentString.replace("; wv", "")
      }
      CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

      webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, pageUrl: String?) {
          // Streamtape's own PiP / cast buttons need a desktop browser; ours are in the top bar
          view.evaluateJavascript(
            "(function(){var s=document.createElement('style');" +
              "s.innerHTML='[data-plyr=\"pip\"],[data-plyr=\"airplay\"]{display:none!important}';" +
              "document.head.appendChild(s);})()", null
          )
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
          val u = request.url
          val scheme = u.scheme.orEmpty()
          val host = u.host.orEmpty()
          val own = host.contains("streamtape") || host.contains("strtape") ||
            host.contains("stape") || host == Uri.parse(url).host
          if (scheme != "http" && scheme != "https") return true
          if (own) return false
          // Ad click-through: first one opens in the browser, the rest are ignored
          openAdOnce(u)
          return true
        }
      }

      var customView: View? = null
      webChromeClient = object : WebChromeClient() {
        // Streamtape ad popups -> open in the browser (this is what pays the host)
        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
          val tmp = WebView(view.context)
          tmp.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
              openAdOnce(r.url)
              v.destroy()
              return true
            }
          }
          (resultMsg.obj as WebView.WebViewTransport).webView = tmp
          resultMsg.sendToTarget()
          return true
        }

        // Fullscreen button inside the Streamtape player
        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
          val activity = context as? Activity ?: return
          customView?.let { (activity.window.decorView as FrameLayout).removeView(it) }
          customView = view
          fullscreenCallback = callback
          (activity.window.decorView as FrameLayout).addView(
            view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
          )
          activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
          setImmersive(activity, true)
        }

        override fun onHideCustomView() {
          val activity = context as? Activity ?: return
          customView?.let { (activity.window.decorView as FrameLayout).removeView(it) }
          customView = null
          fullscreenCallback = null
          activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
          setImmersive(activity, false)
        }
      }
    }
  }

  // Back button leaves fullscreen first
  BackHandler(enabled = fullscreenCallback != null) { fullscreenCallback?.onCustomViewHidden() }

  // Sound fix: stop audio when the app is left (not while in picture-in-picture)
  val lifecycleOwner = context as? LifecycleOwner
  DisposableEffect(lifecycleOwner, webView) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_STOP -> {
          webView.evaluateJavascript("document.querySelectorAll('video').forEach(function(v){v.pause()})", null)
          webView.onPause()
        }
        Lifecycle.Event.ON_START -> webView.onResume()
        else -> {}
      }
    }
    lifecycleOwner?.lifecycle?.addObserver(observer)
    onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
  }

  DisposableEffect(Unit) {
    onDispose {
      (context as? Activity)?.let {
        if (fullscreenCallback != null) {
          it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
          setImmersive(it, false)
        }
      }
      webView.stopLoading()
      webView.loadUrl("about:blank")
      webView.destroy()
    }
  }

  // Best effort: read the position from Streamtape's <video> to save progress / resume.
  // Silently does nothing if the page does not expose the video element.
  LaunchedEffect(webView, url) {
    var resumed = initialPositionMs <= 0L
    while (true) {
      delay(if (resumed) 10_000L else 1_500L)
      webView.evaluateJavascript(
        "(function(){var v=document.querySelector('video');" +
          (if (!resumed) "if(v&&v.duration>0&&v.currentTime<1){v.currentTime=${initialPositionMs / 1000.0};}" else "") +
          "return v&&v.duration>0?(v.currentTime+','+v.duration):'';})()"
      ) { raw ->
        val s = raw?.trim('"').orEmpty()
        val parts = s.split(',')
        if (parts.size == 2) {
          val pos = parts[0].toDoubleOrNull()
          val dur = parts[1].toDoubleOrNull()
          if (pos != null && dur != null) {
            resumed = true
            if (pos > 3.0) onProgress?.invoke((pos * 1000).toLong(), (dur * 1000).toLong())
          }
        }
      }
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { webView },
    update = {
      if (it.url != url && it.originalUrl != url) {
        adOpened[0] = false
        it.loadUrl(url)
      }
    }
  )
}
