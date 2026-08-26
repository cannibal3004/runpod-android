package com.canni.runpod.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.dto.CreateNetworkVolumeRequest
import com.canni.runpod.data.api.dto.DataCenter
import com.canni.runpod.data.api.dto.NetworkVolume
import com.canni.runpod.data.api.dto.UpdateNetworkVolumeRequest
import com.canni.runpod.data.api.dto.VolumeType
import com.canni.runpod.data.repo.CatalogRepository
import com.canni.runpod.data.repo.NetworkVolumeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val volumeRepository: NetworkVolumeRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val loadError: String? = null,
        val volumes: List<NetworkVolume> = emptyList(),
        val dataCenters: List<DataCenter> = emptyList(),

        val showCreateForm: Boolean = false,
        val createName: String = "",
        val createSize: String = "",
        val createDcId: String? = null,
        val createType: VolumeType? = null,
        val isCreating: Boolean = false,
        val createError: String? = null,

        val renameVolume: NetworkVolume? = null,
        val renameValue: String = "",
        val resizeVolume: NetworkVolume? = null,
        val resizeValue: String = "",
        val deleteVolume: NetworkVolume? = null,
        val busy: Boolean = false,
        val dialogError: String? = null,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            coroutineScope {
                val vols = async { runCatching { volumeRepository.listNetworkVolumes() } }
                val dcs = async { runCatching { catalogRepository.listDataCenters() } }
                val v = vols.await()
                val d = dcs.await()
                _state.update {
                    it.copy(
                        isLoading = false,
                        volumes = v.getOrNull() ?: emptyList(),
                        dataCenters = d.getOrNull() ?: emptyList(),
                        loadError = v.exceptionOrNull()?.message,
                    )
                }
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
                createSize = "",
                createDcId = null,
                createType = null,
                createError = null,
            )
        }
    }

    fun onCreateNameChange(value: String) {
        _state.update { it.copy(createName = value, createError = null) }
    }

    fun onCreateSizeChange(value: String) {
        _state.update { it.copy(createSize = value, createError = null) }
    }

    fun onCreateDcChange(dcId: String) {
        val dc = _state.value.dataCenters.find { it.id == dcId }
        _state.update {
            it.copy(
                createDcId = dcId,
                createType = dc?.networkVolumeTypes?.firstOrNull(),
                createError = null,
            )
        }
    }

    fun onCreateTypeChange(type: VolumeType) {
        _state.update { it.copy(createType = type, createError = null) }
    }

    fun createVolume() {
        val s = _state.value
        val name = s.createName.trim()
        val size = s.createSize.trim().toIntOrNull()
        val dcId = s.createDcId
        val error = when {
            name.isEmpty() -> "Enter a name."
            size == null || size < MIN_SIZE_GB || size > MAX_SIZE_GB ->
                "Size must be between $MIN_SIZE_GB and $MAX_SIZE_GB GB."
            dcId == null -> "Pick a data center."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(createError = error) }
            return
        }
        val type = s.createType
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, createError = null) }
            runCatching {
                volumeRepository.createNetworkVolume(
                    CreateNetworkVolumeRequest(
                        name = name,
                        size = requireNotNull(size),
                        dataCenter = requireNotNull(dcId),
                        type = type,
                    ),
                )
            }
                .onSuccess {
                    _state.update {
                        it.copy(
                            showCreateForm = false,
                            isCreating = false,
                            createName = "",
                            createSize = "",
                            createDcId = null,
                            createType = null,
                            message = "Volume \"$name\" created.",
                        )
                    }
                    load()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isCreating = false, createError = e.message ?: "Failed to create volume.")
                    }
                }
        }
    }

    fun showRename(volume: NetworkVolume) {
        _state.update { it.copy(renameVolume = volume, renameValue = volume.name, dialogError = null) }
    }

    fun dismissRename() {
        _state.update { it.copy(renameVolume = null, dialogError = null) }
    }

    fun onRenameChange(value: String) {
        _state.update { it.copy(renameValue = value, dialogError = null) }
    }

    fun confirmRename() {
        val volume = _state.value.renameVolume ?: return
        val name = _state.value.renameValue.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(dialogError = "Enter a name.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, dialogError = null) }
            runCatching {
                volumeRepository.updateNetworkVolume(volume.id, UpdateNetworkVolumeRequest(name = name))
            }
                .onSuccess {
                    _state.update { it.copy(renameVolume = null, busy = false, message = "Renamed to \"$name\".") }
                    load()
                }
                .onFailure { e ->
                    _state.update { it.copy(busy = false, dialogError = e.message ?: "Rename failed.") }
                }
        }
    }

    fun showResize(volume: NetworkVolume) {
        _state.update {
            it.copy(
                resizeVolume = volume,
                resizeValue = (volume.size ?: MIN_SIZE_GB).toString(),
                dialogError = null,
            )
        }
    }

    fun dismissResize() {
        _state.update { it.copy(resizeVolume = null, dialogError = null) }
    }

    fun onResizeChange(value: String) {
        _state.update { it.copy(resizeValue = value, dialogError = null) }
    }

    fun confirmResize() {
        val volume = _state.value.resizeVolume ?: return
        val newSize = _state.value.resizeValue.trim().toIntOrNull()
        val current = volume.size ?: MIN_SIZE_GB
        val error = when {
            newSize == null -> "Enter a size in GB."
            newSize < MIN_SIZE_GB -> "Minimum size is $MIN_SIZE_GB GB."
            newSize > MAX_SIZE_GB -> "Maximum size is $MAX_SIZE_GB GB."
            newSize < current -> "Storage can only grow, not shrink."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(dialogError = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, dialogError = null) }
            runCatching {
                volumeRepository.updateNetworkVolume(volume.id, UpdateNetworkVolumeRequest(size = newSize))
            }
                .onSuccess {
                    _state.update { it.copy(resizeVolume = null, busy = false, message = "Resized to $newSize GB.") }
                    load()
                }
                .onFailure { e ->
                    _state.update { it.copy(busy = false, dialogError = e.message ?: "Resize failed.") }
                }
        }
    }

    fun showDelete(volume: NetworkVolume) {
        _state.update { it.copy(deleteVolume = volume, dialogError = null) }
    }

    fun dismissDelete() {
        _state.update { it.copy(deleteVolume = null, dialogError = null) }
    }

    fun confirmDelete() {
        val volume = _state.value.deleteVolume ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, dialogError = null) }
            runCatching { volumeRepository.deleteNetworkVolume(volume.id) }
                .onSuccess {
                    _state.update { it.copy(deleteVolume = null, busy = false, message = "Volume deleted.") }
                    load()
                }
                .onFailure { e ->
                    _state.update { it.copy(busy = false, dialogError = e.message ?: "Delete failed.") }
                }
        }
    }

    fun onSnackbarShown() {
        _state.update { it.copy(message = null) }
    }

    companion object {
        private const val MIN_SIZE_GB = 10
        private const val MAX_SIZE_GB = 4096
    }
}
