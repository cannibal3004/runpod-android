package com.canni.runpod.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.dto.Template
import com.canni.runpod.data.repo.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TemplateListViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
) : ViewModel() {

    data class UiState(
        val templates: List<Template> = emptyList(),
        val isLoading: Boolean = false,
        val isInitialLoad: Boolean = true,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadInternal()
        }
    }

    private suspend fun loadInternal() {
        _state.update { it.copy(isLoading = true, error = null) }
        runCatching { templateRepository.list() }
            .onSuccess { templates ->
                _state.update {
                    it.copy(
                        templates = templates,
                        isLoading = false,
                        isInitialLoad = false,
                    )
                }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isInitialLoad = false,
                        error = e.message ?: "Failed to load templates",
                    )
                }
            }
    }
}
