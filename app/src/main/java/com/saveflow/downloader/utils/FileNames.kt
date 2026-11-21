package com.saveflow.downloader.utils

import android.webkit.MimeTypeMap
import com.saveflow.downloader.domain.models.MediaAsset
import com.saveflow.downloader.domain.models.MediaType
import com.saveflow.downloader.domain.models.Platform
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileNames {
    fun build(platform: Platform, asset: MediaAsset, index: Int): String {
        val ext = asset.extension
            ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(asset.mimeType)
            ?: if (asset.type == MediaType.VIDEO) "mp4" else "jpg"
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val base = asset.fileNameHint?.substringBeforeLast('.')?.let(::sanitize)?.takeIf { it.isNotBlank() }
            ?: "${platform.label.substringBefore(' ')}_${stamp}_${index + 1}"
        return "$base.$ext"
    }

    fun sanitize(s: String): String = s.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").take(80)

    /** Makes sure the file extension matches the MIME type the server actually delivered. */
    fun withExtensionFor(name: String, mime: String): String {
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: return name
        return if (name.endsWith(".$ext", ignoreCase = true)) name else name.substringBeforeLast('.') + "." + ext
    }
}
