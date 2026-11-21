package com.saveflow.downloader.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saveflow.downloader.AppContainer
import com.saveflow.downloader.domain.models.DownloadRecord
import com.saveflow.downloader.domain.models.DownloadStats
import com.saveflow.downloader.domain.models.MediaType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(private val c: AppContainer) : ViewModel() {
    private val repo = c.downloadRepository
    private val lazily = SharingStarted.WhileSubscribed(5_000)

    val active: StateFlow<List<DownloadRecord>> = repo.active.stateIn(viewModelScope, lazily, emptyList())
    val completed: StateFlow<List<DownloadRecord>> = repo.completed.stateIn(viewModelScope, lazily, emptyList())
    val speeds: StateFlow<Map<Long, Long>> = repo.speeds
    val stats: StateFlow<DownloadStats> = repo.completed.map { list ->
        DownloadStats(
            total = list.size,
            totalBytes = list.sumOf { it.sizeOnDisk },
            videos = list.count { it.type == MediaType.VIDEO },
            images = list.count { it.type == MediaType.IMAGE }
        )
    }.stateIn(viewModelScope, lazily, DownloadStats())

    fun pause(id: Long) = repo.pause(id)
    fun resume(id: Long) = repo.resume(id)
    fun retry(id: Long) = repo.retry(id)
    fun cancel(id: Long) = repo.cancel(id)
    fun delete(record: DownloadRecord) = viewModelScope.launch { repo.deleteCompleted(record) }
}
