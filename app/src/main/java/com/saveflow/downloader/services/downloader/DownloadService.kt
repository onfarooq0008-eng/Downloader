package com.saveflow.downloader.services.downloader

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.saveflow.downloader.SaveFlowApp
import com.saveflow.downloader.domain.models.DownloadStatus
import com.saveflow.downloader.services.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps the process alive (foreground) while downloads are queued or running, shows aggregate progress,
 * and stops itself when everything is finished.
 */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val container = (application as SaveFlowApp).container
        val notifier = container.notifier
        ServiceCompat.startForeground(
            this,
            NotificationHelper.FOREGROUND_ID,
            notifier.progress(0, -1),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        scope.launch {
            container.database.downloadDao().observeRunning().collect { running ->
                if (running.isEmpty()) {
                    stopSelf()
                } else {
                    val known = running.filter { it.status == DownloadStatus.DOWNLOADING && it.totalBytes > 0 }
                    val percent = if (known.isEmpty()) -1 else
                        (known.sumOf { it.downloadedBytes } * 100 / known.sumOf { it.totalBytes }).toInt()
                    runCatching {
                        NotificationManagerCompat.from(this@DownloadService)
                            .notify(NotificationHelper.FOREGROUND_ID, notifier.progress(running.size, percent))
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
