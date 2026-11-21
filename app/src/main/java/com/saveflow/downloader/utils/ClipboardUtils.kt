package com.saveflow.downloader.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ClipboardUtils {
    /** Reads the clipboard text. Android 10+ only allows this while the app is focused; otherwise returns null. */
    fun readText(context: Context): String? {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return null
        val clip = runCatching { cm.primaryClip }.getOrNull() ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
    }

    fun copy(context: Context, label: String, text: String) {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}

object FileActions {
    fun open(context: Context, uriString: String?, mime: String) {
        val uri = uriString?.let(Uri::parse) ?: return toast(context, "File not found")
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure { toast(context, "No app can open this file") }
    }

    fun share(context: Context, uriString: String?, mime: String) {
        val uri = uriString?.let(Uri::parse) ?: return toast(context, "File not found")
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startChooser(context, send)
    }

    fun shareText(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startChooser(context, send)
    }

    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure { toast(context, "No app can open this link") }
    }

    private fun startChooser(context: Context, send: Intent) {
        runCatching {
            context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun toast(context: Context, message: String) =
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
