package com.canni.runpod.ui.templates

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.dto.Template
import com.canni.runpod.data.api.dto.TemplateRequest
import com.canni.runpod.data.repo.TemplateRepository
import com.canni.runpod.ui.components.EnvEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class TemplatePortEntry(
    val port: String = "",
    val protocol: String = "http",
)

@HiltViewModel
class TemplateFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val templateRepository: TemplateRepository,
) : ViewModel() {

    val isEdit: Boolean = savedStateHandle.get<String>("templateId") != null
    private val templateId: String = savedStateHandle.get<String>("templateId").orEmpty()

    data class UiState(
        val template: Template? = null,
        val isLoading: Boolean = false,

        val name: String = "",
        val isServerless: Boolean = false,
        val category: String = "NVIDIA",
        val image: String = "",
        val args: String = "",
        val disk: String = "50",
        val hasVolume: Boolean = false,
        val volumeSize: String = "20",
        val volumePath: String = "/workspace",
        val portEntries: List<TemplatePortEntry> = emptyList(),
        val envEntries: List<EnvEntry> = emptyList(),
        val isPublic: Boolean = false,
        val startSsh: Boolean = true,
        val startJupyter: Boolean = true,
        val allowedCuda: String = "",

        val envEditorVisible: Boolean = false,
        val isSubmitting: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        if (isEdit) {
            load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { templateRepository.get(templateId) }
                .onSuccess { t ->
                    val networkMount = t.mounts?.network?.firstOrNull()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            template = t,
                            name = t.name,
                            isServerless = t.serverless ?: false,
                            category = t.category ?: "NVIDIA",
                            image = t.image,
                            args = t.args.orEmpty(),
                            disk = (t.disk ?: 50).toString(),
                            hasVolume = t.mounts?.persistent != null || !t.mounts?.network.isNullOrEmpty(),
                            volumeSize = (t.mounts?.persistent?.size ?: networkMount?.let { 0 } ?: 20)
                                .takeIf { it > 0 }?.toString() ?: "20",
                            volumePath = t.mounts?.persistent?.path
                                ?: networkMount?.path
                                ?: "/workspace",
                            portEntries = (t.ports ?: emptyList()).map { parsePortSpec(it) },
                            envEntries = (t.env ?: emptyMap()).map { e -> EnvEntry(e.key, e.value) },
                            isPublic = t.public ?: false,
                            startSsh = t.startSsh ?: true,
                            startJupyter = t.startJupyter ?: true,
                            allowedCuda = (t.allowedCudaVersions ?: emptyList()).joinToString(", "),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "Failed to load template")
                    }
                }
        }
    }

    private fun parsePortSpec(spec: String): TemplatePortEntry {
        val parts = spec.split("/")
        return TemplatePortEntry(
            port = parts.getOrElse(0) { "" },
            protocol = parts.getOrElse(1) { "http" },
        )
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, error = null) }

    fun onImageChange(value: String) = _state.update { it.copy(image = value, error = null) }

    fun onArgsChange(value: String) = _state.update { it.copy(args = value, error = null) }

    fun onDiskChange(value: String) = _state.update { it.copy(disk = value, error = null) }

    fun setServerless(value: Boolean) {
        if (isEdit) return
        _state.update { it.copy(isServerless = value, error = null) }
    }

    fun setCategory(value: String) = _state.update { it.copy(category = value, error = null) }

    fun setHasVolume(value: Boolean) = _state.update { it.copy(hasVolume = value, error = null) }

    fun onVolumeSizeChange(value: String) = _state.update { it.copy(volumeSize = value, error = null) }

    fun onVolumePathChange(value: String) = _state.update { it.copy(volumePath = value, error = null) }

    fun setPublic(value: Boolean) = _state.update { it.copy(isPublic = value, error = null) }

    fun setStartSsh(value: Boolean) = _state.update { it.copy(startSsh = value, error = null) }

    fun setStartJupyter(value: Boolean) = _state.update { it.copy(startJupyter = value, error = null) }

    fun onAllowedCudaChange(value: String) = _state.update { it.copy(allowedCuda = value, error = null) }

    fun addPortEntry() = _state.update {
        it.copy(portEntries = it.portEntries + TemplatePortEntry())
    }

    fun removePortEntry(index: Int) = _state.update {
        it.copy(portEntries = it.portEntries.filterIndexed { i, _ -> i != index })
    }

    fun onPortChange(index: Int, value: String) = _state.update {
        it.copy(portEntries = it.portEntries.mapIndexed { i, e -> if (i == index) e.copy(port = value) else e })
    }

    fun onPortProtocolChange(index: Int, protocol: String) = _state.update {
        it.copy(portEntries = it.portEntries.mapIndexed { i, e -> if (i == index) e.copy(protocol = protocol) else e })
    }

    fun openEnvEditor() = _state.update { it.copy(envEditorVisible = true) }

    fun closeEnvEditor() = _state.update { it.copy(envEditorVisible = false) }

    fun addEnvEntry() = _state.update {
        it.copy(envEntries = it.envEntries + EnvEntry())
    }

    fun removeEnvEntry(index: Int) = _state.update {
        it.copy(envEntries = it.envEntries.filterIndexed { i, _ -> i != index })
    }

    fun onEnvKeyChange(index: Int, key: String) = _state.update {
        it.copy(envEntries = it.envEntries.mapIndexed { i, e -> if (i == index) e.copy(key = key) else e })
    }

    fun onEnvValueChange(index: Int, value: String) = _state.update {
        it.copy(envEntries = it.envEntries.mapIndexed { i, e -> if (i == index) e.copy(value = value) else e })
    }

    private fun fail(message: String) {
        _state.update { it.copy(error = message) }
    }

    private fun buildMounts(st: UiState): JsonElement? {
        val original = st.template?.mounts
        val hadVolume = original?.persistent != null || !original?.network.isNullOrEmpty()
        return when {
            st.hasVolume -> {
                val size = st.volumeSize.trim().toIntOrNull() ?: 20
                buildJsonObject {
                    put(
                        "persistent",
                        buildJsonObject {
                            put("size", size)
                            put("path", st.volumePath.trim().ifBlank { "/workspace" })
                        },
                    )
                }
            }

            isEdit && hadVolume -> buildJsonObject {
                put("persistent", JsonNull)
                put("network", JsonNull)
            }

            else -> null
        }
    }

    fun submit(onDone: () -> Unit) {
        val st = _state.value
        if (st.isSubmitting) return
        if (st.name.isBlank()) {
            fail("Name is required.")
            return
        }
        if (st.image.isBlank()) {
            fail("Container image is required.")
            return
        }
        val disk = st.disk.trim().toIntOrNull()
        if (disk != null && disk < 1) {
            fail("Disk must be at least 1 GB.")
            return
        }
        if (st.hasVolume) {
            val size = st.volumeSize.trim().toIntOrNull()
            if (size == null || size < 10) {
                fail("Persistent volume must be at least 10 GB.")
                return
            }
        }
        val ports = st.portEntries
            .filter { it.port.isNotBlank() }
            .map { "${it.port.trim()}/${it.protocol}" }
        val env = st.envEntries
            .filter { it.key.isNotBlank() }
            .associate { it.key.trim() to it.value }
        val cuda = st.allowedCuda
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val request = TemplateRequest(
            name = st.name.trim(),
            image = st.image.trim(),
            args = st.args.trim().ifBlank { null },
            disk = disk,
            env = env.ifEmpty { null },
            ports = ports.ifEmpty { null },
            mounts = buildMounts(st),
            serverless = st.isServerless,
            public = st.isPublic,
            category = st.category,
            startSsh = if (isEdit) null else st.startSsh,
            startJupyter = if (isEdit) null else st.startJupyter,
            allowedCudaVersions = cuda,
        )

        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            runCatching {
                if (isEdit) templateRepository.update(templateId, request)
                else templateRepository.create(request)
            }
                .onSuccess {
                    onDone()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isSubmitting = false, error = e.message ?: "Failed to save template")
                    }
                }
        }
    }
}
