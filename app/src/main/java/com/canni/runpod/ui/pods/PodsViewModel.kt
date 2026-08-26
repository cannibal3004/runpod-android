package com.canni.runpod.ui.pods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.dto.Pod
import com.canni.runpod.data.repo.PodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class PodsViewModel @Inject constructor(
    private val podRepository: PodRepository,
) : ViewModel() {

    data class UiState(
        val pods: List<Pod> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val lastUpdated: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load(initial: Boolean = false, silent: Boolean = false) {
        if (initial) {
            _state.update { it.copy(isLoading = true, error = null) }
        } else if (!silent) {
            _state.update { it.copy(isRefreshing = true) }
        }
        viewModelScope.launch {
            runCatching { podRepository.listPods() }
                .onSuccess { pods ->
                    _state.update {
                        it.copy(
                            pods = pods,
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            lastUpdated = nowUtc(),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = e.message ?: "Failed to load pods",
                        )
                    }
                }
        }
    }

    fun autoRefreshLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(AUTO_REFRESH_MS)
                load(silent = true)
            }
        }
    }

    private fun nowUtc(): String =
        ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm:ss 'UTC'"))

    companion object {
        const val AUTO_REFRESH_MS = 15_000L
    }
}
