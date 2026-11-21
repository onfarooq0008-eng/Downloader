package com.saveflow.downloader.domain.models

import com.saveflow.downloader.data.api.ApiConfig

enum class MediaType { VIDEO, IMAGE }

enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** One downloadable file returned by the API. Optional fields are null when the API did not provide them. */
data class MediaAsset(
    val id: String,
    val type: MediaType,
    val downloadUrl: String,
    val thumbnailUrl: String?,
    val mimeType: String,
    val extension: String?,
    val fileNameHint: String?,
    val width: Int?,
    val height: Int?,
    val durationMs: Long?,
    val sizeBytes: Long?
)

/** Everything the preview screen needs about a resolved link. */
data class MediaInfo(
    val sourceUrl: String,
    val platform: Platform,
    val title: String?,
    val items: List<MediaAsset>,
    val availableQualities: List<String>,
    val quality: String
)

data class DownloadRecord(
    val id: Long,
    val fileName: String,
    val sourceUrl: String,
    val platform: Platform,
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
    /** 0f..1f, or -1f when the total size is unknown. */
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else -1f

    val sizeOnDisk: Long get() = if (totalBytes > 0) totalBytes else downloadedBytes
}

data class DownloadStats(
    val total: Int = 0,
    val totalBytes: Long = 0,
    val videos: Int = 0,
    val images: Int = 0
)

data class AppSettings(
    val quality: String = ApiConfig.DEFAULT_QUALITY,
    val clipboardDetection: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val folder: String = "SaveFlow"
)
