package com.canni.runpod.ui.secrets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.dto.Secret
import com.canni.runpod.data.repo.SecretRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SecretsViewModel @Inject constructor(
    private val secretRepository: SecretRepository,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val loadError: String? = null,
        val secrets: List<Secret> = emptyList(),

        val showCreateForm: Boolean = false,
        val createName: String = "",
        val createValue: String = "",
        val createDescription: String = "",
        val isCreating: Boolean = false,
        val createError: String? = null,

        val deleteSecret: Secret? = null,
        val busy: Boolean = false,
        val dialogError: String? = null,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            runCatching { secretRepository.listSecrets() }
                .onSuccess { secrets ->
                    _state.update { it.copy(isLoading = false, secrets = secrets) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, loadError = e.message ?: "Failed to load secrets.") }
                }
        }
    }

    fun openCreateForm() {
        _state.update { it.copy(showCreateForm = true, createError = null) }
    }

    fun closeCreateForm() {
        _state.update {
            it.copy(
                showCreateForm = false,
                createName = "",
                createValue = "",
                createDescription = "",
                createError = null,
            )
        }
    }

    fun onCreateNameChange(value: String) {
        _state.update { it.copy(createName = value, createError = null) }
    }

    fun onCreateValueChange(value: String) {
        _state.update { it.copy(createValue = value, createError = null) }
    }

    fun onCreateDescriptionChange(value: String) {
        _state.update { it.copy(createDescription = value, createError = null) }
    }

    fun createSecret() {
        val s = _state.value
        val name = s.createName.trim()
        val value = s.createValue
        val error = when {
            name.isEmpty() -> "Enter a name, e.g. openai_api_key."
            name.any { it.isWhitespace() } -> "Names cannot contain spaces."
            value.isEmpty() -> "Enter the secret value."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(createError = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, createError = null) }
            runCatching {
                secretRepository.createSecret(
                    name = name,
                    value = value,
                    description = s.createDescription.trim().ifBlank { null },
                )
            }
                .onSuccess { secret ->
                    _state.update {
                        it.copy(
                            showCreateForm = false,
                            isCreating = false,
                            createName = "",
                            createValue = "",
                            createDescription = "",
                            message = "Created ${secret.reference}",
                        )
                    }
                    load()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isCreating = false, createError = e.message ?: "Failed to create secret.")
                    }
                }
        }
    }

    fun showDelete(secret: Secret) {
        _state.update { it.copy(deleteSecret = secret, dialogError = null) }
    }

    fun dismissDelete() {
        _state.update { it.copy(deleteSecret = null, dialogError = null) }
    }

    fun confirmDelete() {
        val secret = _state.value.deleteSecret ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, dialogError = null) }
            runCatching { secretRepository.deleteSecret(secret.id) }
                .onSuccess {
                    _state.update { it.copy(deleteSecret = null, busy = false, message = "Secret \"${secret.name}\" deleted.") }
                    load()
                }
                .onFailure { e ->
                    _state.update { it.copy(busy = false, dialogError = e.message ?: "Delete failed.") }
                }
        }
    }

    fun copyReference(secret: Secret) {
        _state.update { it.copy(message = "Copied ${secret.reference}") }
    }

    fun onSnackbarShown() {
        _state.update { it.copy(message = null) }
    }
}
