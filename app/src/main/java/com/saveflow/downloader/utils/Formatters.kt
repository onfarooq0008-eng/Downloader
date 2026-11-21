package com.saveflow.downloader.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    fun bytes(b: Long): String {
        if (b <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var v = b.toDouble()
        var i = 0
        while (v >= 1024 && i < units.lastIndex) {
            v /= 1024
            i++
        }
        return if (i == 0) "$b B" else String.format(Locale.US, "%.1f %s", v, units[i])
    }

    fun speed(bytesPerSecond: Long): String = bytes(bytesPerSecond) + "/s"

    fun duration(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    fun date(ms: Long): String =
        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ms))

    fun qualityLabel(q: String): String = when (q.lowercase()) {
        "hd" -> "HD"
        "sd" -> "SD"
        else -> q.replaceFirstChar { it.uppercase() }
    }
}
