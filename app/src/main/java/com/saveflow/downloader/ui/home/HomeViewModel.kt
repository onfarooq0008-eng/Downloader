package com.saveflow.downloader.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saveflow.downloader.AppContainer
import com.saveflow.downloader.data.api.ApiException
import com.saveflow.downloader.domain.models.DownloadRecord
import com.saveflow.downloader.domain.models.Platform
import com.saveflow.downloader.utils.UrlUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val input: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val clipSuggestion: String? = null
)

sealed interface HomeEvent {
    data object OpenPreview : HomeEvent
    data object ShowHome : HomeEvent
}

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val recent: StateFlow<List<DownloadRecord>> = c.downloadRepository.completed
        .map { it.take(6) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Links arriving from the share sheet or the browser tab.
        viewModelScope.launch {
            c.session.incomingLink.filterNotNull().collect { link ->
                c.session.incomingLink.value = null
                _state.update { it.copy(input = link.url, error = null, clipSuggestion = null) }
                _events.send(HomeEvent.ShowHome)
                if (link.autoStart) analyze()
            }
        }
    }

    fun onInput(value: String) = _state.update { it.copy(input = value, error = null) }

    fun clear() = _state.update { it.copy(input = "", error = null) }

    fun pasteFromClipboard(text: String?) {
        val url = UrlUtils.extractUrl(text) ?: text?.trim()
        if (url.isNullOrBlank()) {
            _state.update { it.copy(error = "Clipboard is empty") }
        } else {
            _state.update { it.copy(input = url, error = null, clipSuggestion = null) }
        }
    }

    /** Only suggests; never sends anything. Each URL is suggested once. */
    fun checkClipboard(text: String?) {
        viewModelScope.launch {
            if (!c.settingsRepository.settings.first().clipboardDetection) {
                _state.update { it.copy(clipSuggestion = null) }
                return@launch
            }
            val url = UrlUtils.extractUrl(text) ?: return@launch
            if (!UrlUtils.isValid(url) || Platform.detect(url) == Platform.OTHER) return@launch
            if (url == c.settingsRepository.lastSuggestedClip.first() || url == _state.value.input.trim()) return@launch
            _state.update { it.copy(clipSuggestion = url) }
            c.settingsRepository.setLastSuggestedClip(url)
        }
    }

    fun acceptSuggestion() {
        val url = _state.value.clipSuggestion ?: return
        _state.update { it.copy(input = url, clipSuggestion = null, error = null) }
    }

    fun dismissSuggestion() = _state.update { it.copy(clipSuggestion = null) }

    fun analyze() {
        val current = _state.value
        if (current.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val info = c.fetchMedia(current.input)
                c.session.media.value = info
                _state.update { it.copy(loading = false) }
                _events.send(HomeEvent.OpenPreview)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Unexpected error. Please try again.") }
            }
        }
    }
}
