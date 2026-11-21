package com.saveflow.downloader.services.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.saveflow.downloader.MainActivity
import com.saveflow.downloader.R

class NotificationHelper(private val context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CH_PROGRESS, "Download progress", NotificationManager.IMPORTANCE_LOW)
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_RESULT, "Download results", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /** Ongoing notification used by the foreground service. percent < 0 shows an indeterminate bar. */
    fun progress(activeCount: Int, percent: Int): Notification {
        val text = if (activeCount == 1) "Downloading 1 file" else "Downloading $activeCount files"
        return NotificationCompat.Builder(context, CH_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(text)
            .setContentText(if (percent >= 0) "$percent%" else "Starting…")
            .setProgress(100, percent.coerceAtLeast(0), percent < 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp())
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    @SuppressLint("MissingPermission")
    fun complete(id: Long, name: String, uri: Uri, mime: String) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        val view = PendingIntent.getActivity(
            context, id.toInt(),
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, CH_RESULT)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle("Download complete")
            .setContentText(name)
            .setContentIntent(view)
            .setAutoCancel(true)
            .build()
        nm.notify(RESULT_BASE + id.toInt(), n)
    }

    @SuppressLint("MissingPermission")
    fun failed(id: Long, name: String) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        val n = NotificationCompat.Builder(context, CH_RESULT)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle("Download failed")
            .setContentText("$name — open SaveFlow to retry")
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
        nm.notify(RESULT_BASE + id.toInt(), n)
    }

    companion object {
        const val CH_PROGRESS = "downloads_progress"
        const val CH_RESULT = "downloads_result"
        const val FOREGROUND_ID = 1
        private const val RESULT_BASE = 1000
    }
}
