package com.canni.runpod.ui.templates

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.ApiError
import com.canni.runpod.data.api.dto.Template
import com.canni.runpod.data.repo.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TemplateDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val templateRepository: TemplateRepository,
) : ViewModel() {

    private val templateId: String = savedStateHandle.get<String>("templateId").orEmpty()

    data class UiState(
        val template: Template? = null,
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val templateGone: Boolean = false,
        val confirmDelete: Boolean = false,
        val deleteBusy: Boolean = false,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load(silent: Boolean = false) {
        if (templateId.isEmpty()) {
            _state.update { it.copy(isLoading = false, error = "Missing template id.") }
            return
        }
        if (!silent) {
            _state.update { it.copy(isLoading = true, error = null) }
        } else {
            _state.update { it.copy(isRefreshing = true) }
        }
        viewModelScope.launch {
            runCatching { templateRepository.get(templateId) }
                .onSuccess { template ->
                    _state.update {
                        it.copy(
                            template = template,
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        if (silent && it.template != null && e is ApiError && e.code == 404) {
                            it.copy(isLoading = false, isRefreshing = false, templateGone = true)
                        } else {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = e.message ?: "Failed to load template",
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
            runCatching { templateRepository.delete(templateId) }
                .onSuccess {
                    _state.update { it.copy(deleteBusy = false, templateGone = true) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(deleteBusy = false, message = e.message ?: "Failed to delete template")
                    }
                }
        }
    }
}
