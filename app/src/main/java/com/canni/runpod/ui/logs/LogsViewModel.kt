package com.canni.runpod.ui.logs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.logs.LogEvent
import com.canni.runpod.data.logs.LogStreamer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LogsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val streamer: LogStreamer,
) : ViewModel() {

    enum class SourceFilter(val label: String, val param: String?) {
        ALL("All", null),
        CONTAINER("Container", "container"),
        SYSTEM("System", "system"),
    }

    data class LogLine(
        val source: String,
        val text: String,
        val time: String?,
    )

    sealed interface Status {
        object Connecting : Status
        object Streaming : Status
        object Reconnecting : Status
        data class Error(val message: String) : Status
    }

    data class UiState(
        val lines: List<LogLine> = emptyList(),
        val status: Status = Status.Connecting,
        val sourceFilter: SourceFilter = SourceFilter.ALL,
        val autoScroll: Boolean = true,
    )

    private val podId: String = savedStateHandle.get<String>("podId").orEmpty()
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var stopped = false
    private var reconnectAttempt = 0
    private var lastSeenTs: String? = null
    private var suppressReplay = false

    init {
        connect(userInitiated = true)
    }

    fun setSourceFilter(filter: SourceFilter) {
        if (filter == _state.value.sourceFilter) return
        _state.update { it.copy(sourceFilter = filter) }
        connect(userInitiated = true)
    }

    fun setAutoScroll(value: Boolean) {
        _state.update { it.copy(autoScroll = value) }
    }

    fun clear() {
        _state.update { it.copy(lines = emptyList()) }
    }

    fun retry() {
        reconnectAttempt = 0
        connect(userInitiated = true)
    }

    override fun onCleared() {
        stopped = true
        streamer.stop()
    }

    private fun connect(userInitiated: Boolean = false) {
        if (stopped || podId.isEmpty()) return
        _state.update { st ->
            st.copy(
                status = if (userInitiated) Status.Connecting else Status.Reconnecting,
                lines = if (userInitiated) emptyList() else st.lines,
            )
        }
        if (!userInitiated) suppressReplay = true
        streamer.stream(
            podId = podId,
            source = _state.value.sourceFilter.param,
            tail = 100,
            listener = object : LogStreamer.Listener {
                override fun onOpen() {
                    if (stopped) return
                    _state.update { it.copy(status = Status.Streaming) }
                }

                override fun onEvent(event: LogEvent) {
                    if (stopped) return
                    val ts = event.ts
                    if (suppressReplay) {
                        val last = lastSeenTs
                        if (last != null && (ts == null || ts <= last)) return
                        suppressReplay = false
                    }
                    if (ts != null) {
                        val last = lastSeenTs
                        if (last == null || ts > last) lastSeenTs = ts
                    }
                    reconnectAttempt = 0
                    val line = LogLine(
                        source = event.source ?: "container",
                        text = event.line ?: "",
                        time = event.ts?.let { ts ->
                            if (ts.length >= 19) ts.substring(11, 19) else ts
                        },
                    )
                    _state.update { st ->
                        st.copy(
                            lines = (st.lines + line).takeLast(MAX_LINES),
                            status = Status.Streaming,
                        )
                    }
                }

                override fun onFailure(message: String, code: Int?) {
                    if (stopped) return
                    if (code == 401 || code == 403 || code == 404) {
                        _state.update { st -> st.copy(status = Status.Error(message)) }
                    } else {
                        scheduleReconnect()
                    }
                }

                override fun onClosed() {
                    if (!stopped) scheduleReconnect()
                }
            },
        )
    }

    private fun scheduleReconnect() {
        reconnectAttempt += 1
        _state.update { it.copy(status = Status.Reconnecting) }
        val backoff = minOf(1_000L * (1L shl (reconnectAttempt - 1).coerceIn(0, 30)), 30_000L)
        viewModelScope.launch {
            delay(backoff)
            connect()
        }
    }

    companion object {
        private const val MAX_LINES = 2_000
    }
}
