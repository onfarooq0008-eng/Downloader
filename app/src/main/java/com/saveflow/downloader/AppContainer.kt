package com.saveflow.downloader

import android.app.Application
import com.saveflow.downloader.data.api.ApiClient
import com.saveflow.downloader.data.api.DownloadApiService
import com.saveflow.downloader.data.database.AppDatabase
import com.saveflow.downloader.data.repository.DownloadRepository
import com.saveflow.downloader.data.repository.MediaRepository
import com.saveflow.downloader.data.repository.SettingsRepository
import com.saveflow.downloader.domain.models.MediaInfo
import com.saveflow.downloader.domain.usecases.FetchMediaUseCase
import com.saveflow.downloader.services.downloader.DownloadEngine
import com.saveflow.downloader.services.downloader.MediaStoreSaver
import com.saveflow.downloader.services.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow

/** State shared between screens that is not worth persisting. */
class SessionHolder {
    /** The media currently shown on the preview screen. */
    val media = MutableStateFlow<MediaInfo?>(null)

    /** A link handed to the home screen from outside (browser tab or the Android share sheet). */
    val incomingLink = MutableStateFlow<IncomingLink?>(null)
}

/** [autoStart] is true only when the user explicitly asked to download it (browser button). */
data class IncomingLink(val url: String, val autoStart: Boolean)

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
