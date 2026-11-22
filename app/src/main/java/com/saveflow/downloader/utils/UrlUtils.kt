package com.saveflow.downloader.utils

import android.net.Uri
import com.saveflow.downloader.domain.models.Platform

object UrlUtils {
    private val urlRegex = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)

    /** Pulls the first http(s) link out of arbitrary text (e.g. a shared message). */
    fun extractUrl(text: String?): String? =
        text?.let { urlRegex.find(it)?.value?.trimEnd('.', ',', ')', ']', '"', '\'') }

    fun isValid(url: String): Boolean {
        if (url.any { it.isWhitespace() }) return false
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host ?: return false
        return (uri.scheme == "http" || uri.scheme == "https") && host.contains('.')
    }

    /** True for links that point at a specific post/video rather than a profile or home page. */
    fun isMediaLink(url: String): Boolean {
        val platform = Platform.detect(url)
        if (platform == Platform.OTHER) return false
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val segments = uri.pathSegments
        val host = uri.host?.lowercase().orEmpty()
        val isShort = platform.shortLinkHosts.any { host == it }
        return segments.size >= 2 || (segments.size == 1 && isShort)
    }

    private val videoExtensions = setOf("mp4", "webm", "mov", "m4v", "mkv", "3gp")

    /** True for an http(s) URL whose path ends in a common video file extension. */
    fun isVideoFileUrl(url: String): Boolean {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        val path = runCatching { Uri.parse(url).path }.getOrNull()?.lowercase() ?: return false
        return path.substringAfterLast('.', "") in videoExtensions
    }

    /** Turns address-bar input into a loadable URL (adds https:// or falls back to a web search). */
    fun normalizeInput(input: String): String {
        val t = input.trim()
        return when {
            t.isEmpty() -> "https://www.google.com"
            t.startsWith("http://") || t.startsWith("https://") -> t
            !t.contains(' ') && t.contains('.') -> "https://$t"
            else -> "https://www.google.com/search?q=" + Uri.encode(t)
        }
    }

    fun displayHost(url: String): String =
        runCatching { Uri.parse(url).host }.getOrNull()?.removePrefix("www.") ?: url
}
