package com.saveflow.downloader.domain.usecases

import android.webkit.MimeTypeMap
import com.saveflow.downloader.data.api.ApiException
import com.saveflow.downloader.data.repository.MediaRepository
import com.saveflow.downloader.data.repository.SettingsRepository
import com.saveflow.downloader.domain.models.MediaAsset
import com.saveflow.downloader.domain.models.MediaInfo
import com.saveflow.downloader.domain.models.MediaType
import com.saveflow.downloader.domain.models.Platform
import com.saveflow.downloader.utils.UrlUtils
import kotlinx.coroutines.flow.first

/** Validates user input, then resolves it through the backend. Throws [ApiException] with a user-facing message. */
class FetchMediaUseCase(
    private val media: MediaRepository,
    private val settings: SettingsRepository
) {
    suspend operator fun invoke(rawInput: String, quality: String? = null): MediaInfo {
        val url = UrlUtils.extractUrl(rawInput) ?: rawInput.trim()
        if (!UrlUtils.isValid(url)) {
            throw ApiException(ApiException.Kind.INVALID, "Enter a valid link starting with http:// or https://")
        }
        val q = quality ?: settings.settings.first().quality
        return media.resolve(url, q)
    }

    /** Wraps a direct video file URL found in the browser; no API call involved. */
    fun directMedia(directUrl: String, pageUrl: String): MediaInfo {
        val urlExt = directUrl.substringBefore('?').substringAfterLast('.', "").lowercase()
        val mimeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(urlExt)?.takeIf { it.startsWith("video/") }
        val mime = mimeFromExt ?: "video/mp4"
        val ext = if (mimeFromExt != null) urlExt else "mp4"
        val asset = MediaAsset(
            id = "0", type = MediaType.VIDEO, downloadUrl = directUrl, thumbnailUrl = null,
            mimeType = mime, extension = ext, fileNameHint = null,
            width = null, height = null, durationMs = null, sizeBytes = null
        )
        return MediaInfo(pageUrl, Platform.detect(pageUrl), null, listOf(asset), listOf("direct"), "direct")
    }
}
