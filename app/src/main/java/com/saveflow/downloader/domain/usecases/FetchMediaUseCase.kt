package com.saveflow.downloader.domain.usecases

import com.saveflow.downloader.data.api.ApiException
import com.saveflow.downloader.data.repository.MediaRepository
import com.saveflow.downloader.data.repository.SettingsRepository
import com.saveflow.downloader.domain.models.MediaInfo
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
}
