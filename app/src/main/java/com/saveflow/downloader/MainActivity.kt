package com.saveflow.downloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.saveflow.downloader.ui.SaveFlowRoot
import com.saveflow.downloader.utils.UrlUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent { SaveFlowRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** A link shared from another app is handed to the home screen; it is never fetched automatically. */
    private fun handleIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val url = UrlUtils.extractUrl(intent.getStringExtra(Intent.EXTRA_TEXT)) ?: return
        (application as SaveFlowApp).container.session.incomingLink.value = IncomingLink(url, autoStart = false)
    }
}
