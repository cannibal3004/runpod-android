package com.canni.runpod.ui.settings

import androidx.lifecycle.ViewModel
import com.canni.runpod.data.auth.ApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val keyStore: ApiKeyStore,
) : ViewModel() {

    data class UiState(
        val maskedKey: String? = null,
        val showRemoveDialog: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState(maskedKey = mask(keyStore.apiKey)))
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onRemoveKeyClick() {
        if (_state.value.maskedKey == null) return
        _state.update { it.copy(showRemoveDialog = true) }
    }

    fun onDismissRemoveDialog() {
        _state.update { it.copy(showRemoveDialog = false) }
    }

    fun confirmRemoveKey() {
        keyStore.apiKey = ""
        _state.update { it.copy(maskedKey = null, showRemoveDialog = false) }
    }

    private fun mask(key: String): String? {
        if (key.isEmpty()) return null
        return if (key.length <= 8) {
            "••••"
        } else {
            "${key.take(4)}••••${key.takeLast(4)}"
        }
    }
}
