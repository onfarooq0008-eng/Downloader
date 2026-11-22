package com.saveflow.downloader

import android.app.Application
import com.saveflow.downloader.data.api.ApiClient
import com.saveflow.downloader.data.api.DownloadApiService
import com.saveflow.downloader.data.database.AppDatabase
import com.saveflow.downloader.data.repository.DownloadRepository
import com.saveflow.downloader.data.repository.MediaRepository
import com.saveflow.downloader.data.repository.SettingsRepository
import com.saveflow.downloader.domain.usecases.FetchMediaUseCase
import com.saveflow.downloader.services.downloader.DownloadEngine
import com.saveflow.downloader.services.downloader.MediaStoreSaver
import com.saveflow.downloader.services.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow

/** State shared between screens that is not worth persisting. */
class SessionHolder {
    /** A link to download right away: from the Android share sheet or the browser's download button. */
    val incomingLink = MutableStateFlow<IncomingLink?>(null)
}

/**
 * [url] is sent to the downloader API. [fallbackUrl] is a direct video file URL found by the
 * browser, used only if the API cannot handle [url].
 */
data class IncomingLink(val url: String, val fallbackUrl: String? = null)

/** Hand-rolled dependency container: small app, no DI framework needed. */
class AppContainer(val app: Application) {
    val database: AppDatabase by lazy { AppDatabase.build(app) }
    val settingsRepository = SettingsRepository(app)
    val notifier = NotificationHelper(app)
    private val saver = MediaStoreSaver(app)

    val engine: DownloadEngine by lazy {
        DownloadEngine(app, database.downloadDao(), ApiClient.downloadClient, saver, notifier, settingsRepository)
    }
    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(database.downloadDao(), engine, saver)
    }
    val mediaRepository = MediaRepository(DownloadApiService())
    val fetchMedia = FetchMediaUseCase(mediaRepository, settingsRepository)
    val session = SessionHolder()
}
