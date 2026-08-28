package com.canni.runpod.ui.serverless

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.dto.ServerlessEndpoint
import com.canni.runpod.data.repo.ServerlessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ServerlessViewModel @Inject constructor(
    private val serverlessRepository: ServerlessRepository,
) : ViewModel() {

    data class UiState(
        val endpoints: List<ServerlessEndpoint> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load(initial: Boolean = false) {
        if (initial) {
            _state.update { it.copy(isLoading = true, error = null) }
        } else {
            _state.update { it.copy(isRefreshing = true) }
        }
        viewModelScope.launch {
            runCatching { serverlessRepository.listEndpoints() }
                .onSuccess { endpoints ->
                    _state.update {
                        it.copy(
                            endpoints = endpoints,
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = e.message ?: "Failed to load endpoints",
                        )
                    }
                }
        }
    }
}
