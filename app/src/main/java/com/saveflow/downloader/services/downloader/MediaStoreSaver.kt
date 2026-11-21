package com.saveflow.downloader.services.downloader

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException

/** Publishes a finished file into the gallery (Movies/<folder> or Pictures/<folder>) via MediaStore. */
class MediaStoreSaver(private val context: Context) {

    @Suppress("DEPRECATION")
    fun save(src: File, displayName: String, mime: String, isVideo: Boolean, folder: String): Uri {
        val resolver = context.contentResolver
        val safeFolder = folder.ifBlank { "SaveFlow" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val rootDir = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = if (isVideo) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$rootDir/$safeFolder")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: throw IOException("Could not create a gallery entry")
            try {
                val out = resolver.openOutputStream(uri) ?: throw IOException("Could not open gallery file")
                out.use { o -> src.inputStream().use { it.copyTo(o) } }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return uri
            } catch (e: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                throw e
            }
        }

        // Android 7-9: write into the public directory (needs WRITE_EXTERNAL_STORAGE), then register it.
        val dir = File(Environment.getExternalStoragePublicDirectory(rootDir), safeFolder).apply { mkdirs() }
        var target = File(dir, displayName)
        var n = 1
        while (target.exists()) {
            target = File(dir, displayName.substringBeforeLast('.') + " ($n)." + displayName.substringAfterLast('.', ""))
            n++
        }
        src.copyTo(target)
        // Inserting the row registers the file with the gallery (no separate media scan, which would duplicate it).
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, target.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.DATA, target.absolutePath)
            put(MediaStore.MediaColumns.SIZE, target.length())
        }
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        return resolver.insert(collection, values) ?: Uri.fromFile(target)
    }

    fun delete(uri: Uri) {
        runCatching { context.contentResolver.delete(uri, null, null) }
    }
}
