package com.saveflow.downloader.ui.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saveflow.downloader.AppContainer
import com.saveflow.downloader.data.api.ApiConfig
import com.saveflow.downloader.data.api.ApiException
import com.saveflow.downloader.domain.models.MediaAsset
import com.saveflow.downloader.domain.models.MediaInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PreviewState(
    val info: MediaInfo? = null,
    val selected: Set<String> = emptySet(),
    val switchingQuality: Boolean = false,
    val message: String? = null
)

class PreviewViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(
        c.session.media.value.let { PreviewState(info = it, selected = it?.items?.map { a -> a.id }?.toSet().orEmpty()) }
    )
    val state: StateFlow<PreviewState> = _state.asStateFlow()

    fun toggle(id: String) = _state.update {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id)
    }

    fun selectAll(all: Boolean) = _state.update {
        it.copy(selected = if (all) it.info?.items?.map { a -> a.id }?.toSet().orEmpty() else emptySet())
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    /** Re-asks the backend for another quality; also remembered as the default. */
    fun setQuality(q: String) {
        val info = _state.value.info ?: return
        if (q.equals(info.quality, ignoreCase = true) || _state.value.switchingQuality) return
        viewModelScope.launch {
            _state.update { it.copy(switchingQuality = true) }
            try {
                val fresh = c.fetchMedia(info.sourceUrl, q)
                // Keep the full quality list we already learned about.
                val merged = fresh.copy(availableQualities = (info.availableQualities + fresh.availableQualities).distinct())
                c.session.media.value = merged
                if (q in ApiConfig.knownQualities) c.settingsRepository.setQuality(q)
                _state.update { it.copy(info = merged, selected = merged.items.map { a -> a.id }.toSet(), switchingQuality = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                _state.update { it.copy(switchingQuality = false, message = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(switchingQuality = false, message = "Couldn't switch quality.") }
            }
        }
    }

    /** Queues the given assets; returns how many were queued. */
    suspend fun download(assets: List<MediaAsset>): Int {
        val info = _state.value.info ?: return 0
        if (assets.isEmpty()) return 0
        return c.downloadRepository.enqueue(info, assets)
    }
}
