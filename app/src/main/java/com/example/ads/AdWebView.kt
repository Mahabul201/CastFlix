package com.example.ads

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Shows one Monetag website tag (In-Page Push, Vignette Banner, ...) inside a small WebView.
 * Give it a fixed size with [modifier]. Nothing is drawn when ads are off or the zone id is 0.
 * Taps on the ad open in the phone's browser.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdWebView(
  scriptSrc: String,
  zoneId: Long,
  modifier: Modifier = Modifier
) {
  if (!AdConfig.ADS_ENABLED || zoneId == 0L) return

  val html = remember(scriptSrc, zoneId) {
    """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <style>html,body{margin:0;padding:0;background:transparent;overflow:hidden}</style>
    </head>
    <body>
      <script>(function(s){s.dataset.zone='$zoneId',s.src='$scriptSrc'})([document.documentElement, document.body].filter(Boolean).pop().appendChild(document.createElement('script')))</script>
    </body>
    </html>
    """.trimIndent()
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      WebView(ctx).apply {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        webViewClient = object : WebViewClient() {
          override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            // A tap on the ad: open the advertiser page in the browser, not inside the app
            if (request.hasGesture()) {
              try {
                ctx.startActivity(
                  Intent(Intent.ACTION_VIEW, request.url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
              } catch (_: Exception) {
              }
              return true
            }
            return false
          }
        }
        loadDataWithBaseURL("https://castflix.app/", html, "text/html", "utf-8", null)
      }
    },
    onRelease = { it.destroy() }
  )
}
