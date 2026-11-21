package com.saveflow.downloader.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saveflow.downloader.AppContainer
import com.saveflow.downloader.domain.models.AppSettings
import com.saveflow.downloader.domain.models.DownloadStats
import com.saveflow.downloader.domain.models.MediaType
import com.saveflow.downloader.domain.models.ThemeMode
import com.saveflow.downloader.utils.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    private val app: Application = c.app
    private val repo = c.settingsRepository

    val settings: StateFlow<AppSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val stats: StateFlow<DownloadStats> = c.downloadRepository.completed.map { list ->
        DownloadStats(
            total = list.size,
            totalBytes = list.sumOf { it.sizeOnDisk },
            videos = list.count { it.type == MediaType.VIDEO },
            images = list.count { it.type == MediaType.IMAGE }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadStats())

    private val _cacheBytes = MutableStateFlow(0L)
    val cacheBytes: StateFlow<Long> = _cacheBytes.asStateFlow()

    private val _freeBytes = MutableStateFlow(0L)
    val freeBytes: StateFlow<Long> = _freeBytes.asStateFlow()

    val versionName: String = runCatching {
        app.packageManager.getPackageInfo(app.packageName, 0).versionName
    }.getOrNull() ?: "1.0"

    init { refreshStorage() }

    fun refreshStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            _cacheBytes.value = StorageUtils.cacheSize(app)
            _freeBytes.value = StorageUtils.freeBytes()
        }
    }

    fun setQuality(q: String) = viewModelScope.launch { repo.setQuality(q) }
    fun setClipboard(on: Boolean) = viewModelScope.launch { repo.setClipboardDetection(on) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repo.setTheme(mode) }
    fun setFolder(name: String) = viewModelScope.launch { if (name.isNotBlank()) repo.setFolder(name) }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            StorageUtils.clearCache(app)
            withContext(Dispatchers.Main) { refreshStorage() }
        }
    }

    /** Removes history entries only; files already saved in the gallery are kept. */
    fun clearHistory() = viewModelScope.launch { c.downloadRepository.clearHistory() }
}
