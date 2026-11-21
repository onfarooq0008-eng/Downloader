package com.saveflow.downloader.domain.models

import android.net.Uri

/** Social platforms the app can recognise from a URL. Recognising a host does NOT mean the backend supports it. */
enum class Platform(
    val label: String,
    val short: String,
    val brand: Long,
    val hosts: List<String>,
    val shortLinkHosts: List<String> = emptyList()
) {
    INSTAGRAM("Instagram", "IG", 0xFFE1306C, listOf("instagram.com", "instagr.am")),
    FACEBOOK("Facebook", "f", 0xFF1877F2, listOf("facebook.com", "fb.com", "fb.watch"), listOf("fb.watch")),
    TIKTOK("TikTok", "TT", 0xFF2B2B2B, listOf("tiktok.com")),
    TWITTER("X / Twitter", "X", 0xFF2B2B2B, listOf("twitter.com", "x.com", "t.co"), listOf("t.co")),
    PINTEREST("Pinterest", "P", 0xFFE60023, listOf("pinterest.com", "pin.it"), listOf("pin.it")),
    REDDIT("Reddit", "R", 0xFFFF4500, listOf("reddit.com", "redd.it"), listOf("redd.it")),
    OTHER("Website", "•", 0xFF6B7280, emptyList());

    companion object {
        fun detect(url: String): Platform {
            val host = runCatching { Uri.parse(url).host }.getOrNull()?.lowercase() ?: return OTHER
            return entries.firstOrNull { p -> p.hosts.any { host == it || host.endsWith(".$it") } } ?: OTHER
        }
    }
}
