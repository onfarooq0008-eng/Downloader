package com.saveflow.downloader.data.repository

import com.saveflow.downloader.data.api.DownloadApiService
import com.saveflow.downloader.domain.models.MediaInfo

class MediaRepository(private val api: DownloadApiService) {
    suspend fun resolve(sourceUrl: String, quality: String): MediaInfo = api.resolve(sourceUrl, quality)
}
