package com.canni.runpod.ui.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.dto.HubListing
import com.canni.runpod.data.repo.HubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HubViewModel @Inject constructor(
    private val hubRepository: HubRepository,
) : ViewModel() {

    data class UiState(
        val query: String = "",
        val allListings: List<HubListing> = emptyList(),
        val listings: List<HubListing> = emptyList(),
        val isLoading: Boolean = false,
        val isInitialLoad: Boolean = true,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun setQuery(value: String) {
        _state.update { st ->
            st.copy(query = value, listings = filter(st.allListings, value))
        }
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadInternal()
        }
    }

    fun refresh() {
        load()
    }

    private fun filter(listings: List<HubListing>, query: String): List<HubListing> {
        val q = query.trim()
        if (q.isBlank()) return listings
        return listings.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.description?.contains(q, ignoreCase = true) == true
        }
    }

    private suspend fun loadInternal() {
        _state.update { it.copy(isLoading = true, error = null) }
        runCatching { hubRepository.listServerless() }
            .onSuccess { list ->
                _state.update { st ->
                    st.copy(
                        allListings = list,
                        listings = filter(list, st.query),
                        isLoading = false,
                        isInitialLoad = false,
                    )
                }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(isLoading = false, isInitialLoad = false, error = e.message)
                }
            }
    }
}
