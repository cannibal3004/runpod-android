package com.canni.runpod.ui.serverless

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.ApiError
import com.canni.runpod.data.api.dto.ListEndpointReleasesResponse
import com.canni.runpod.data.api.dto.ListEndpointWorkersResponse
import com.canni.runpod.data.api.dto.ServerlessEndpoint
import com.canni.runpod.data.repo.ServerlessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ServerlessDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverlessRepository: ServerlessRepository,
) : ViewModel() {

    private val endpointId: String = savedStateHandle.get<String>("endpointId").orEmpty()

    data class UiState(
        val endpoint: ServerlessEndpoint? = null,
        val workers: ListEndpointWorkersResponse? = null,
        val releases: ListEndpointReleasesResponse? = null,
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val endpointGone: Boolean = false,
        val confirmDelete: Boolean = false,
        val deleteBusy: Boolean = false,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load(silent: Boolean = false) {
        if (endpointId.isEmpty()) {
            _state.update { it.copy(isLoading = false, error = "Missing endpoint id.") }
            return
        }
        if (!silent) {
            _state.update { it.copy(isLoading = true, error = null) }
        } else {
            _state.update { it.copy(isRefreshing = true) }
        }
        viewModelScope.launch {
            val endpointDeferred = async { serverlessRepository.getEndpoint(endpointId) }
            val workersDeferred = async {
                runCatching { serverlessRepository.listWorkers(endpointId) }.getOrNull()
            }
            val releasesDeferred = async {
                runCatching { serverlessRepository.listReleases(endpointId) }.getOrNull()
            }
            try {
                val endpoint = endpointDeferred.await()
                _state.update {
                    it.copy(
                        endpoint = endpoint,
                        workers = workersDeferred.await(),
                        releases = releasesDeferred.await(),
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    if (silent && it.endpoint != null && e is ApiError && e.code == 404) {
                        it.copy(isLoading = false, isRefreshing = false, endpointGone = true)
                    } else {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = e.message ?: "Failed to load endpoint",
                        )
                    }
                }
            }
        }
    }

    fun showDelete() {
        _state.update { it.copy(confirmDelete = true) }
    }

    fun dismissDelete() {
        _state.update { it.copy(confirmDelete = false) }
    }

    fun confirmDelete() {
        _state.update { it.copy(confirmDelete = false, deleteBusy = true) }
        viewModelScope.launch {
            runCatching { serverlessRepository.deleteEndpoint(endpointId) }
                .onSuccess {
                    _state.update { it.copy(deleteBusy = false, endpointGone = true) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(deleteBusy = false, message = e.message ?: "Failed to delete endpoint")
                    }
                }
        }
    }

    fun onSnackbarShown() {
        _state.update { it.copy(message = null) }
    }
}
