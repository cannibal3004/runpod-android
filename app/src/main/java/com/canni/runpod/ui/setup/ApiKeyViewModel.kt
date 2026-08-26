package com.canni.runpod.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.auth.ApiKeyStore
import com.canni.runpod.data.repo.PodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ApiKeyViewModel @Inject constructor(
    private val keyStore: ApiKeyStore,
    private val podRepository: PodRepository,
) : ViewModel() {

    data class UiState(
        val key: String = "",
        val isSaving: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onKeyChange(value: String) {
        _state.update { it.copy(key = value, error = null) }
    }

    fun save(onSuccess: () -> Unit) {
        val key = _state.value.key.trim()
        if (key.isEmpty()) {
            _state.update { it.copy(error = "Enter your Runpod API key.") }
            return
        }
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            keyStore.apiKey = key
            runCatching { podRepository.listPods() }
                .onSuccess { onSuccess() }
                .onFailure { e ->
                    _state.update {
                        it.copy(isSaving = false, error = e.message ?: "Failed to connect.")
                    }
                }
        }
    }
}
