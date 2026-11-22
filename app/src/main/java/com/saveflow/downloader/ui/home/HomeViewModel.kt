package com.saveflow.downloader.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saveflow.downloader.AppContainer
import com.saveflow.downloader.data.api.ApiException
import com.saveflow.downloader.domain.models.DownloadRecord
import com.saveflow.downloader.domain.models.MediaInfo
import com.saveflow.downloader.domain.models.Platform
import com.saveflow.downloader.utils.UrlUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    /** The link was resolved; the UI asks for permissions, then calls [HomeViewModel.enqueue]. */
    data class StartDownload(val info: MediaInfo) : HomeEvent
    data class Started(val count: Int) : HomeEvent
    data class Message(val text: String) : HomeEvent
}

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val repo = c.downloadRepository
    private val lazily = SharingStarted.WhileSubscribed(5_000)

    val recent: StateFlow<List<DownloadRecord>> = repo.completed
        .map { it.take(6) }
        .stateIn(viewModelScope, lazily, emptyList())
    val active: StateFlow<List<DownloadRecord>> = repo.active.stateIn(viewModelScope, lazily, emptyList())
    val speeds: StateFlow<Map<Long, Long>> = repo.speeds

    private var job: Job? = null

    init {
        // Links from the share sheet or the browser's download button start immediately.
        viewModelScope.launch {
            c.session.incomingLink.filterNotNull().collect { link ->
                c.session.incomingLink.value = null
                if (_state.value.loading) {
                    _events.send(HomeEvent.Message("Please wait — another link is still being processed."))
                } else {
                    start(link.url, link.fallbackUrl, manual = false)
                }
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

    /** The Download button: resolves the typed link and downloads it. */
    fun analyze() = start(_state.value.input, fallbackUrl = null, manual = true)

    fun cancel() {
        job?.cancel()
        _state.update { it.copy(loading = false) }
    }

    private fun start(rawInput: String, fallbackUrl: String?, manual: Boolean) {
        if (_state.value.loading) return
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val info = try {
                    c.fetchMedia(rawInput)
                } catch (e: ApiException) {
                    // The API couldn't handle the page; use the direct video file the browser found, if any.
                    if (fallbackUrl != null) c.fetchMedia.directMedia(fallbackUrl, rawInput) else throw e
                }
                _state.update { it.copy(loading = false, input = if (manual) "" else it.input) }
                _events.send(HomeEvent.StartDownload(info))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = (e as? ApiException)?.message ?: "Unexpected error. Please try again."
                _state.update { it.copy(loading = false, error = if (manual) message else null) }
                if (!manual) _events.send(HomeEvent.Message(message))
            }
        }
    }

    /** Queues every item of a resolved link (a carousel downloads all its media). */
    fun enqueue(info: MediaInfo) {
        viewModelScope.launch {
            val n = repo.enqueue(info, info.items)
            _events.send(HomeEvent.Started(n))
        }
    }

    fun pause(id: Long) = repo.pause(id)
    fun resume(id: Long) = repo.resume(id)
    fun retry(id: Long) = repo.retry(id)
    fun cancelDownload(id: Long) = repo.cancel(id)
}
