package com.saveflow.downloader.services.downloader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.webkit.WebSettings
import com.saveflow.downloader.data.api.ApiConfig
import androidx.core.content.ContextCompat
import com.saveflow.downloader.data.database.DownloadDao
import com.saveflow.downloader.data.database.DownloadEntity
import com.saveflow.downloader.data.repository.SettingsRepository
import com.saveflow.downloader.domain.models.DownloadStatus
import com.saveflow.downloader.services.notifications.NotificationHelper
import com.saveflow.downloader.utils.FileNames
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Runs downloads: up to [MAX_CONCURRENT] at once, resumable via HTTP Range, with automatic retry
 * (resuming from the partial file) on network errors. Finished files are copied into the gallery.
 */
class DownloadEngine(
    private val context: Context,
    private val dao: DownloadDao,
    private val client: OkHttpClient,
    private val saver: MediaStoreSaver,
    private val notifier: NotificationHelper,
    private val settings: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val limiter = Semaphore(MAX_CONCURRENT)
    private val jobs = ConcurrentHashMap<Long, Job>()
    private val partialDir by lazy { File(context.filesDir, "partial").apply { mkdirs() } }

    private val _speeds = MutableStateFlow<Map<Long, Long>>(emptyMap())
    /** Current speed per download id in bytes/second. */
    val speeds: StateFlow<Map<Long, Long>> = _speeds.asStateFlow()

    private class PermanentFailure(message: String) : IOException(message)

    init {
        scope.launch { dao.resetInterrupted() }
    }

    suspend fun enqueue(entity: DownloadEntity): Long {
        val id = dao.insert(entity)
        start(id)
        return id
    }

    fun resume(id: Long) {
        scope.launch {
            dao.setStatus(id, DownloadStatus.QUEUED, null)
            start(id)
        }
    }

    fun pause(id: Long) {
        scope.launch {
            jobs[id]?.cancelAndJoin()
            dao.setStatus(id, DownloadStatus.PAUSED, null)
        }
    }

    fun cancel(id: Long) {
        scope.launch {
            jobs[id]?.cancelAndJoin()
            partFile(id).delete()
            dao.delete(id)
        }
    }

    private fun start(id: Long) {
        if (jobs[id]?.isActive == true) return
        startService()
        val job = scope.launch(start = CoroutineStart.LAZY) { limiter.withPermit { execute(id) } }
        jobs[id] = job
        job.invokeOnCompletion {
            jobs.remove(id, job)
            _speeds.update { it - id }
        }
        job.start()
    }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, DownloadService::class.java))
        } catch (_: Exception) {
            // Background-start restrictions: the download still runs while the process is alive.
        }
    }

    private fun partFile(id: Long) = File(partialDir, "$id.part")

    private suspend fun execute(id: Long) {
        val entity = dao.get(id) ?: return
        dao.setStatus(id, DownloadStatus.DOWNLOADING, null)
        var attempt = 0
        while (true) {
            try {
                transfer(id, entity)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A cancelled call surfaces as IOException; treat it as cancellation, not failure.
                currentCoroutineContext().ensureActive()
                attempt++
                if (e is PermanentFailure || attempt > MAX_RETRIES) {
                    dao.setStatus(id, DownloadStatus.FAILED, e.message ?: "Download failed")
                    notifier.failed(id, entity.fileName)
                    return
                }
                delay(RETRY_BASE_MS * attempt) // the partial file is kept, so the retry resumes
            }
        }
    }

    private suspend fun transfer(id: Long, entity: DownloadEntity) {
        val part = partFile(id)
        var offset = if (part.exists()) part.length() else 0L

        val request = Request.Builder().url(entity.downloadUrl).apply {
            if (offset > 0) header("Range", "bytes=$offset-")
            // Direct file URLs found in the browser often reject requests without a page referer / browser UA.
            if (!entity.downloadUrl.startsWith(ApiConfig.BASE_URL)) {
                header("Referer", entity.sourceUrl)
                header("User-Agent", WebSettings.getDefaultUserAgent(context))
            }
        }.build()
        val call = client.newCall(request)
        val cancelHandle = currentCoroutineContext()[Job]!!.invokeOnCompletion { call.cancel() }

        var responseMime: String? = null
        try {
            call.execute().use { resp ->
                responseMime = resp.header("Content-Type")?.substringBefore(';')?.trim()?.lowercase()
                val alreadyComplete = resp.code == 416 && offset > 0
                if (!alreadyComplete) {
                    if (resp.code >= 500) throw IOException("Server error (HTTP ${resp.code})")
                    if (!resp.isSuccessful) throw PermanentFailure("Server returned HTTP ${resp.code}")
                    val body = resp.body ?: throw IOException("Empty response body")

                    val resumed = resp.code == 206 && offset > 0
                    if (!resumed) offset = 0
                    val length = body.contentLength()
                    val total = if (length >= 0) offset + length else entity.totalBytes.takeIf { it > 0 } ?: -1L

                    var downloaded = offset
                    var lastReport = SystemClock.elapsedRealtime()
                    var windowBytes = 0L
                    dao.setProgress(id, downloaded, total)

                    FileOutputStream(part, resumed).use { out ->
                        val input = body.byteStream()
                        val buffer = ByteArray(32 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n == -1) break
                            out.write(buffer, 0, n)
                            downloaded += n
                            windowBytes += n
                            currentCoroutineContext().ensureActive()
                            val now = SystemClock.elapsedRealtime()
                            if (now - lastReport >= 500) {
                                val speed = windowBytes * 1000 / (now - lastReport)
                                _speeds.update { it + (id to speed) }
                                dao.setProgress(id, downloaded, total)
                                lastReport = now
                                windowBytes = 0
                            }
                        }
                        out.flush()
                    }
                    if (length >= 0 && downloaded < total) throw IOException("Connection closed early")
                    dao.setProgress(id, downloaded, if (total > 0) total else downloaded)
                }
            }
        } finally {
            cancelHandle.dispose()
        }

        finish(id, entity, part, responseMime)
    }

    private suspend fun finish(id: Long, entity: DownloadEntity, part: File, responseMime: String?) {
        val mime = responseMime?.takeIf { it.startsWith("video/") || it.startsWith("image/") } ?: entity.mimeType
        val name = FileNames.withExtensionFor(entity.fileName, mime)
        val folder = settings.settings.first().folder
        val uri: Uri = try {
            saver.save(part, name, mime, mime.startsWith("video/"), folder)
        } catch (e: Exception) {
            throw PermanentFailure("Couldn't save to gallery: ${e.message ?: "unknown error"}")
        }
        val size = part.length()
        part.delete()
        dao.markCompleted(id, DownloadStatus.COMPLETED, uri.toString(), name, size, System.currentTimeMillis())
        notifier.complete(id, name, uri, mime)
    }

    companion object {
        const val MAX_CONCURRENT = 3
        private const val MAX_RETRIES = 3
        private const val RETRY_BASE_MS = 2_000L
    }
}
