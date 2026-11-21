package com.saveflow.downloader.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.saveflow.downloader.ui.components.PlatformIcon
import com.saveflow.downloader.domain.models.Platform
import com.saveflow.downloader.utils.ClipboardUtils
import com.saveflow.downloader.utils.FileActions
import com.saveflow.downloader.utils.UrlUtils

/**
 * Owns the WebView for the lifetime of the activity so page, history and scroll position
 * survive switching bottom-navigation tabs.
 */
class BrowserState(context: Context) {
    var url by mutableStateOf("")
    var progress by mutableFloatStateOf(0f)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)

    @SuppressLint("SetJavaScriptEnabled")
    val webView: WebView = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, pageUrl: String?, favicon: Bitmap?) = sync(view, pageUrl)
            override fun onPageFinished(view: WebView, pageUrl: String?) = sync(view, pageUrl)
            // Fires for single-page-app navigations (Instagram, X) that never reload the page.
            override fun doUpdateVisitedHistory(view: WebView, pageUrl: String?, isReload: Boolean) = sync(view, pageUrl)
        }
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                // Explicit receiver: inside WebView.apply, a bare `progress` would resolve to WebView.getProgress().
                this@BrowserState.progress = newProgress / 100f
            }
        }
        loadUrl(HOME)
    }

    private fun sync(view: WebView, pageUrl: String?) {
        pageUrl?.let { url = it }
        canGoBack = view.canGoBack()
        canGoForward = view.canGoForward()
    }

    fun load(input: String) = webView.loadUrl(UrlUtils.normalizeInput(input))

    fun destroy() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }

    companion object {
        const val HOME = "https://www.google.com"
    }
}

@Composable
fun BrowserScreen(
    state: BrowserState,
    isActive: Boolean,
    onSendToDownloader: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var address by remember { mutableStateOf(state.url) }
    // Keep the address bar in sync with navigation, without fighting the user while typing.
    LaunchedEffect(state.url) { address = state.url }

    BackHandler(enabled = isActive && state.canGoBack) { state.webView.goBack() }

    val detected = state.url.takeIf { UrlUtils.isMediaLink(it) }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Search or enter address") },
                trailingIcon = {
                    IconButton(onClick = {
                        ClipboardUtils.readText(context)?.let { address = it.trim() }
                    }) { Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste") }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { state.load(address) }),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { state.webView.goBack() }, enabled = state.canGoBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                }
                IconButton(onClick = { state.webView.goForward() }, enabled = state.canGoForward) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Forward")
                }
                IconButton(onClick = { state.webView.reload() }) { Icon(Icons.Rounded.Refresh, "Refresh") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { if (state.url.isNotBlank()) FileActions.shareText(context, state.url) }) {
                    Icon(Icons.Rounded.Share, "Share")
                }
            }
            if (state.progress in 0.01f..0.99f) {
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
            }
        }

        AnimatedVisibility(visible = detected != null) {
            detected?.let { url ->
                Surface(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlatformIcon(Platform.detect(url), size = 32.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Media link detected", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(UrlUtils.displayHost(url), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                        }
                        Button(onClick = { onSendToDownloader(url) }, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Rounded.Download, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Download")
                        }
                    }
                }
            }
        }

        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = {
                // The WebView may still be attached to a previous (disposed) container.
                (state.webView.parent as? ViewGroup)?.removeView(state.webView)
                state.webView
            }
        )
    }
}
