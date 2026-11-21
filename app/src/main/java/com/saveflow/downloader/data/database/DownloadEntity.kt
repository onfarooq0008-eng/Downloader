package com.saveflow.downloader.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.saveflow.downloader.domain.models.DownloadRecord
import com.saveflow.downloader.domain.models.DownloadStatus
import com.saveflow.downloader.domain.models.MediaType
import com.saveflow.downloader.domain.models.Platform

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val sourceUrl: String,
    val downloadUrl: String,
    val platform: String,
    val type: MediaType,
    val status: DownloadStatus,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val savedUri: String?,
    val thumbnailUrl: String?,
    val mimeType: String,
    val createdAt: Long,
    val completedAt: Long?,
    val error: String?
) {
    fun toDomain() = DownloadRecord(
        id = id,
        fileName = fileName,
        sourceUrl = sourceUrl,
        platform = runCatching { Platform.valueOf(platform) }.getOrDefault(Platform.OTHER),
        type = type,
        status = status,
        totalBytes = totalBytes,
        downloadedBytes = downloadedBytes,
        savedUri = savedUri,
        thumbnailUrl = thumbnailUrl,
        mimeType = mimeType,
        createdAt = createdAt,
        completedAt = completedAt,
        error = error
    )
}

class Converters {
    @TypeConverter fun statusToString(s: DownloadStatus): String = s.name
    @TypeConverter fun stringToStatus(s: String): DownloadStatus = DownloadStatus.valueOf(s)
    @TypeConverter fun typeToString(t: MediaType): String = t.name
    @TypeConverter fun stringToType(s: String): MediaType = MediaType.valueOf(s)
}
