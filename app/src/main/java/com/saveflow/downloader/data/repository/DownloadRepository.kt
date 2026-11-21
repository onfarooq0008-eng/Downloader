package com.saveflow.downloader.data.repository

import android.net.Uri
import com.saveflow.downloader.data.database.DownloadDao
import com.saveflow.downloader.data.database.DownloadEntity
import com.saveflow.downloader.domain.models.DownloadRecord
import com.saveflow.downloader.domain.models.DownloadStatus
import com.saveflow.downloader.domain.models.MediaAsset
import com.saveflow.downloader.domain.models.MediaInfo
import com.saveflow.downloader.services.downloader.DownloadEngine
import com.saveflow.downloader.services.downloader.MediaStoreSaver
import com.saveflow.downloader.utils.FileNames
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class DownloadRepository(
    private val dao: DownloadDao,
    private val engine: DownloadEngine,
    private val saver: MediaStoreSaver
) {
    val active: Flow<List<DownloadRecord>> = dao.observeActive().map { list -> list.map { it.toDomain() } }
    val completed: Flow<List<DownloadRecord>> = dao.observeCompleted().map { list -> list.map { it.toDomain() } }
    val speeds: StateFlow<Map<Long, Long>> = engine.speeds

    fun observe(id: Long): Flow<DownloadRecord?> = dao.observeById(id).map { it?.toDomain() }

    suspend fun enqueue(info: MediaInfo, assets: List<MediaAsset>): Int {
        val now = System.currentTimeMillis()
        assets.forEachIndexed { index, asset ->
            engine.enqueue(
                DownloadEntity(
                    fileName = FileNames.build(info.platform, asset, index),
                    sourceUrl = info.sourceUrl,
                    downloadUrl = asset.downloadUrl,
                    platform = info.platform.name,
                    type = asset.type,
                    status = DownloadStatus.QUEUED,
                    totalBytes = asset.sizeBytes ?: -1L,
                    downloadedBytes = 0L,
                    savedUri = null,
                    thumbnailUrl = asset.thumbnailUrl,
                    mimeType = asset.mimeType,
                    createdAt = now + index,
                    completedAt = null,
                    error = null
                )
            )
        }
        return assets.size
    }

    fun pause(id: Long) = engine.pause(id)
    fun resume(id: Long) = engine.resume(id)
    fun retry(id: Long) = engine.resume(id)
    fun cancel(id: Long) = engine.cancel(id)

    /** Removes the record and (optionally) the saved file from the gallery. */
    suspend fun deleteCompleted(record: DownloadRecord, deleteFile: Boolean = true) {
        if (deleteFile) record.savedUri?.let { saver.delete(Uri.parse(it)) }
        dao.delete(record.id)
    }

    suspend fun clearHistory() = dao.deleteCompleted()
}
