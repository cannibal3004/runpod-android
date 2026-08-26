package com.canni.runpod.ui.pod

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.dto.Pod
import com.canni.runpod.data.api.dto.PodAction
import com.canni.runpod.data.repo.PodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val podRepository: PodRepository,
) : ViewModel() {

    private val podId: String = savedStateHandle.get<String>("podId").orEmpty()

    data class UiState(
        val pod: Pod? = null,
        val isLoading: Boolean = true,
        val error: String? = null,
        val podGone: Boolean = false,
        val busyAction: PodAction? = null,
        val actionError: String? = null,
        val lastUpdated: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load(silent: Boolean = false) {
        if (podId.isEmpty()) {
            _state.update { it.copy(isLoading = false, error = "Missing pod id.") }
            return
        }
        if (!silent) _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { podRepository.getPod(podId) }
                .onSuccess { pod ->
                    _state.update {
                        it.copy(pod = pod, isLoading = false, lastUpdated = System.currentTimeMillis().toString())
                    }
                }
                .onFailure { e ->
                    _state.update {
                        if (silent && it.pod != null && e is com.canni.runpod.data.api.ApiError && e.code == 404) {
                            it.copy(isLoading = false, podGone = true)
                        } else {
                            it.copy(isLoading = false, error = e.message ?: "Failed to load pod")
                        }
                    }
                }
        }
    }

    fun act(action: PodAction) {
        if (_state.value.busyAction != null) return
        _state.update { it.copy(busyAction = action, actionError = null) }
        viewModelScope.launch {
            runCatching { podRepository.act(podId, action) }
                .onSuccess { pod ->
                    val gone = action == PodAction.terminate && pod.status == com.canni.runpod.data.api.dto.PodStatus.TERMINATED
                    _state.update {
                        it.copy(pod = pod, isLoading = false, busyAction = null, podGone = gone)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(busyAction = null, actionError = e.message ?: "Action failed")
                    }
                }
        }
    }

    fun clearActionError() {
        _state.update { it.copy(actionError = null) }
    }

    companion object {
        const val REFRESH_MS = 5_000L
    }
}
