package com.canni.runpod.ui.hub

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.dto.HubListing
import com.canni.runpod.data.api.dto.HubReleaseConfig
import com.canni.runpod.data.repo.HubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

@HiltViewModel
class HubDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val hubRepository: HubRepository,
    private val json: Json,
) : ViewModel() {

    private val listingId: String = savedStateHandle.get<String>("listingId").orEmpty()

    data class EnvPreview(
        val key: String,
        val name: String,
        val type: String,
        val defaultValue: String,
        val description: String,
        val advanced: Boolean,
    )

    data class UiState(
        val listing: HubListing? = null,
        val config: HubReleaseConfig? = null,
        val envPreview: List<EnvPreview> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadInternal()
        }
    }

    private suspend fun loadInternal() {
        _state.update { it.copy(isLoading = true, error = null) }
        runCatching { hubRepository.getListing(listingId) }
            .onSuccess { listing ->
                val release = listing.listedRelease
                val config = release?.config
                    ?.takeIf { it.isNotBlank() }
                    ?.let { raw ->
                        runCatching { json.decodeFromJsonElement<HubReleaseConfig>(json.parseToJsonElement(raw)) }
                            .getOrNull()
                    }
                val envPreview = config?.env?.map { env ->
                    val input = env.input
                    EnvPreview(
                        key = env.key,
                        name = input?.name ?: env.key,
                        type = input?.type ?: "string",
                        defaultValue = (input?.defaultValue as? JsonPrimitive)?.content ?: "",
                        description = input?.description.orEmpty(),
                        advanced = input?.advanced == true,
                    )
                }.orEmpty()
                _state.update {
                    it.copy(
                        listing = listing,
                        config = config,
                        envPreview = envPreview,
                        isLoading = false,
                    )
                }
            }
            .onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
    }
}
