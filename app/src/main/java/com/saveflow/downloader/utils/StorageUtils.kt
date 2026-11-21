package com.saveflow.downloader.utils

import android.content.Context
import android.os.Environment
import android.os.StatFs

object StorageUtils {
    fun cacheSize(context: Context): Long =
        context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun clearCache(context: Context) {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    @Suppress("DEPRECATION")
    fun freeBytes(): Long = runCatching {
        StatFs(Environment.getExternalStorageDirectory().path).availableBytes
    }.getOrDefault(0L)
}
