package com.canni.runpod.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.dto.Cloud
import com.canni.runpod.data.api.dto.CreatePodRequest
import com.canni.runpod.data.api.dto.CreateGpuConfig
import com.canni.runpod.data.api.dto.DataCenter
import com.canni.runpod.data.api.dto.GpuType
import com.canni.runpod.data.api.dto.Mounts
import com.canni.runpod.data.api.dto.NetworkMount
import com.canni.runpod.data.api.dto.NetworkVolume
import com.canni.runpod.data.api.dto.PersistentMount
import com.canni.runpod.data.api.dto.Secret
import com.canni.runpod.data.api.dto.Template
import com.canni.runpod.data.repo.CatalogRepository
import com.canni.runpod.data.repo.NetworkVolumeRepository
import com.canni.runpod.data.repo.PodRepository
import com.canni.runpod.data.repo.SecretRepository
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
class CreatePodViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val podRepository: PodRepository,
    private val networkVolumeRepository: NetworkVolumeRepository,
    private val secretRepository: SecretRepository,
) : ViewModel() {

    data class EnvEntry(
        val key: String = "",
        val value: String = "",
    )

    data class PortEntry(
        val port: String = "",
        val protocol: String = "http",
    )

    enum class GpuMemoryFilter {
        ANY,
        LE_24,
        B_24_48,
        B_48_96,
        GT_96,
    }

    enum class GpuSort {
        PRICE,
        NAME,
        MEMORY,
    }

    enum class DataStorage {
        NONE,
        VOLUME_DISK,
        NETWORK,
    }

    data class UiState(
        val gpuTypes: List<GpuType> = emptyList(),
        val dataCenters: List<DataCenter> = emptyList(),
        val templates: List<Template> = emptyList(),
        val networkVolumes: List<NetworkVolume> = emptyList(),
        val isLoadingCatalog: Boolean = true,
        val catalogError: String? = null,
        val volumesError: String? = null,
        val secrets: List<Secret> = emptyList(),
        val secretsError: String? = null,
        val secretPickerIndex: Int? = null,

        val useTemplate: Boolean = true,
        val selectedTemplateId: String? = null,
        val image: String = "",
        val name: String = "",
        val selectedGpuId: String? = null,
        val gpuCount: Int = 1,
        val cloud: Cloud = Cloud.SECURE,
        val pickerVisible: Boolean = false,
        val gpuQuery: String = "",
        val gpuMemoryFilter: GpuMemoryFilter = GpuMemoryFilter.ANY,
        val gpuInStockOnly: Boolean = false,
        val gpuSort: GpuSort = GpuSort.PRICE,
        val selectedDataCenterId: String? = null,
        val dataStorage: DataStorage = DataStorage.NONE,
        val selectedVolumeId: String? = null,
        val volumePath: String = "/runpod-volume",
        val persistentSize: String = "",
        val persistentPath: String = "/workspace",
        val disk: String = "50",
        val args: String = "",
        val envEntries: List<EnvEntry> = emptyList(),
        val portEntries: List<PortEntry> = emptyList(),
        val startSsh: Boolean = false,
        val startJupyter: Boolean = false,

        val isCreating: Boolean = false,
        val createError: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun loadCatalog() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingCatalog = true, catalogError = null) }
            coroutineScope {
                val gpus = async { runCatching { catalogRepository.listGpuTypes() } }
                val dcs = async { runCatching { catalogRepository.listDataCenters() } }
                val templates = async { runCatching { catalogRepository.listAllTemplates() } }
                val volumes = async { runCatching { networkVolumeRepository.listNetworkVolumes() } }
                val secrets = async { runCatching { secretRepository.listSecrets() } }
                val g = gpus.await()
                val d = dcs.await()
                val t = templates.await()
                val v = volumes.await()
                val sec = secrets.await()
                val error = listOfNotNull(
                    g.exceptionOrNull(),
                    d.exceptionOrNull(),
                    t.exceptionOrNull(),
                ).firstOrNull()?.message
                _state.update {
                    it.copy(
                        isLoadingCatalog = false,
                        gpuTypes = g.getOrNull() ?: emptyList(),
                        dataCenters = d.getOrNull() ?: emptyList(),
                        templates = t.getOrNull() ?: emptyList(),
                        networkVolumes = v.getOrNull() ?: emptyList(),
                        volumesError = v.exceptionOrNull()?.message,
                        secrets = sec.getOrNull() ?: emptyList(),
                        secretsError = sec.exceptionOrNull()?.message,
                        catalogError = error,
                    )
                }
            }
        }
    }

    fun onSourceChange(useTemplate: Boolean) {
        _state.update { it.copy(useTemplate = useTemplate, selectedTemplateId = null, createError = null) }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, createError = null) }

    fun onImageChange(value: String) = _state.update { it.copy(image = value, createError = null) }

    fun onDiskChange(value: String) = _state.update { it.copy(disk = value, createError = null) }

    fun onArgsChange(value: String) = _state.update { it.copy(args = value, createError = null) }

    fun onSelectTemplate(templateId: String) {
        val t = _state.value.templates.find { it.id == templateId } ?: return
        _state.update {
            it.copy(
                selectedTemplateId = templateId,
                image = t.image,
                dataStorage = if (t.mounts?.persistent != null) DataStorage.NONE else it.dataStorage,
                disk = (t.disk ?: 50).toString(),
                args = t.args.orEmpty(),
                envEntries = (t.env ?: emptyMap()).map { e -> EnvEntry(e.key, e.value) },
                portEntries = (t.ports ?: emptyList()).map { parsePortSpec(it) },
                startSsh = t.startSsh ?: false,
                startJupyter = t.startJupyter ?: false,
                createError = null,
            )
        }
    }

    fun onSelectGpu(gpuId: String) {
        val gpu = _state.value.gpuTypes.find { it.id == gpuId } ?: return
        val max = maxCountFor(gpu, _state.value.cloud)
        _state.update {
            it.copy(
                selectedGpuId = gpuId,
                gpuCount = it.gpuCount.coerceAtMost(max),
                pickerVisible = false,
                createError = null,
            )
        }
    }

    fun showGpuPicker() = _state.update { it.copy(pickerVisible = true) }

    fun hideGpuPicker() = _state.update { it.copy(pickerVisible = false) }

    fun onGpuQueryChange(value: String) = _state.update { it.copy(gpuQuery = value) }

    fun onGpuMemoryFilterChange(filter: GpuMemoryFilter) = _state.update { it.copy(gpuMemoryFilter = filter) }

    fun onGpuInStockOnlyChange(value: Boolean) = _state.update { it.copy(gpuInStockOnly = value) }

    fun onGpuSortChange(sort: GpuSort) = _state.update { it.copy(gpuSort = sort) }

    fun onCloudChange(cloud: Cloud) {
        val s = _state.value
        val gpu = s.gpuTypes.find { it.id == s.selectedGpuId }
        val max = gpu?.let { maxCountFor(it, cloud) } ?: Int.MAX_VALUE
        _state.update { it.copy(cloud = cloud, gpuCount = it.gpuCount.coerceAtMost(max), createError = null) }
    }

    fun onGpuCountChange(delta: Int) {
        val s = _state.value
        val gpu = s.gpuTypes.find { it.id == s.selectedGpuId } ?: return
        val max = maxCountFor(gpu, s.cloud)
        val next = (s.gpuCount + delta).coerceIn(1, max)
        if (next != s.gpuCount) _state.update { it.copy(gpuCount = next) }
    }

    fun onSelectDataCenter(dcId: String?) {
        val s = _state.value
        if (s.dataStorage == DataStorage.NETWORK && s.selectedVolumeId != null) return
        _state.update { it.copy(selectedDataCenterId = dcId, createError = null) }
    }

    fun onDataStorageChange(mode: DataStorage) {
        val s = _state.value
        if (s.dataStorage == mode) return
        _state.update { it.copy(dataStorage = mode, createError = null) }
        if (s.dataStorage == DataStorage.NETWORK && s.selectedVolumeId != null) {
            applyVolume(null)
        }
    }

    fun onSelectVolume(volumeId: String?) {
        _state.update { it.copy(dataStorage = DataStorage.NETWORK, createError = null) }
        applyVolume(volumeId)
    }

    private fun applyVolume(volumeId: String?) {
        val s = _state.value
        val volume = volumeId?.let { id -> s.networkVolumes.find { it.id == id } }
        val dc = volume?.dataCenter
        _state.update {
            val gpu = it.gpuTypes.find { g -> g.id == it.selectedGpuId }
            it.copy(
                selectedVolumeId = volumeId,
                selectedDataCenterId = dc ?: it.selectedDataCenterId,
                selectedGpuId = if (gpu != null && dc != null && !gpuInDataCenter(gpu, dc)) {
                    null
                } else {
                    it.selectedGpuId
                },
                createError = null,
            )
        }
    }

    fun onVolumePathChange(value: String) = _state.update { it.copy(volumePath = value, createError = null) }

    fun onPersistentSizeChange(value: String) = _state.update { it.copy(persistentSize = value, createError = null) }

    fun onPersistentPathChange(value: String) = _state.update { it.copy(persistentPath = value, createError = null) }

    fun onEnvKeyChange(index: Int, value: String) = updateEnv(index) { it.copy(key = value) }

    fun onEnvValueChange(index: Int, value: String) = updateEnv(index) { it.copy(value = value) }

    fun showSecretPicker(index: Int) {
        _state.update { it.copy(secretPickerIndex = index) }
    }

    fun hideSecretPicker() {
        _state.update { it.copy(secretPickerIndex = null) }
    }

    fun onSelectEnvSecret(index: Int, secret: Secret) {
        updateEnv(index) { it.copy(value = secret.reference) }
        _state.update { it.copy(secretPickerIndex = null) }
    }

    fun addEnvEntry() {
        _state.update { it.copy(envEntries = it.envEntries + EnvEntry()) }
    }

    fun removeEnvEntry(index: Int) {
        _state.update { it.copy(envEntries = it.envEntries.filterIndexed { i, _ -> i != index }) }
    }

    fun onPortChange(index: Int, value: String) = updatePort(index) { it.copy(port = value) }

    fun onPortProtocolChange(index: Int, protocol: String) = updatePort(index) { it.copy(protocol = protocol) }

    fun addPortEntry() {
        _state.update { it.copy(portEntries = it.portEntries + PortEntry()) }
    }

    fun removePortEntry(index: Int) {
        _state.update { it.copy(portEntries = it.portEntries.filterIndexed { i, _ -> i != index }) }
    }

    fun onToggleSsh(checked: Boolean) {
        _state.update {
            var ports = it.portEntries
            if (checked && ports.none { p -> p.port == "22" && p.protocol == "tcp" }) {
                ports = ports + PortEntry("22", "tcp")
            }
            it.copy(startSsh = checked, portEntries = ports)
        }
    }

    fun onToggleJupyter(checked: Boolean) {
        _state.update {
            var ports = it.portEntries
            if (checked && ports.none { p -> p.port == "8888" && p.protocol == "http" }) {
                ports = ports + PortEntry("8888", "http")
            }
            it.copy(startJupyter = checked, portEntries = ports)
        }
    }

    fun createPod(onDone: (podId: String) -> Unit) {
        val s = _state.value
        val name = s.name.trim()
        val gpu = s.gpuTypes.find { it.id == s.selectedGpuId }
        if (name.isEmpty()) return fail("Pod name is required.")
        if (s.useTemplate && s.selectedTemplateId == null) return fail("Pick a template.")
        if (!s.useTemplate && s.image.isBlank()) return fail("Container image is required.")
        if (gpu == null) return fail("Pick a GPU type.")
        val disk = s.disk.trim().toIntOrNull()
        if (disk == null || disk < 1) return fail("Disk size must be a positive number of GB.")
        for (p in s.portEntries) {
            if (p.port.isBlank()) continue
            val n = p.port.trim().toIntOrNull()
            if (n == null || n !in 1..65_535) return fail("Invalid port: ${p.port}")
        }

        val template = s.selectedTemplateId?.let { id -> s.templates.find { it.id == id } }
        val templateHasPersistent = template?.mounts?.persistent != null

        val volume = if (s.dataStorage == DataStorage.NETWORK) {
            s.selectedVolumeId?.let { id -> s.networkVolumes.find { it.id == id } }
        } else {
            null
        }
        val volumePath = s.volumePath.trim()
        if (volume != null) {
            if (volumePath.isEmpty() || !volumePath.startsWith("/")) {
                return fail("Mount path must be an absolute path, e.g. /runpod-volume.")
            }
            val pinnedDc = s.selectedDataCenterId
            val volDc = volume.dataCenter
            if (pinnedDc != null && volDc != null && volDc != pinnedDc) {
                return fail("Volume ${volume.name} lives in $volDc, but the pod is pinned to $pinnedDc. Pick a matching data center or another volume.")
            }
            if (templateHasPersistent) {
                return fail("Template ${template.name} includes a persistent volume, which is mutually exclusive with a network volume.")
            }
        }

        val persistent = if (s.dataStorage == DataStorage.VOLUME_DISK) {
            val size = s.persistentSize.trim().toIntOrNull()
            val path = s.persistentPath.trim()
            if (size == null || size < 10) {
                return fail("Volume disk size must be at least 10 GB.")
            }
            if (path.isEmpty() || !path.startsWith("/")) {
                return fail("Mount path must be an absolute path, e.g. /workspace.")
            }
            if (templateHasPersistent) {
                return fail("Template ${template.name} already defines a persistent volume; pick a different storage option.")
            }
            PersistentMount(size = size, path = path)
        } else {
            null
        }

        val request = CreatePodRequest(
            name = name,
            image = if (s.useTemplate) null else s.image.trim(),
            templateId = if (s.useTemplate) s.selectedTemplateId else null,
            gpu = CreateGpuConfig(gpu.id, s.gpuCount),
            cloud = s.cloud,
            dataCenterIds = s.selectedDataCenterId?.let { listOf(it) },
            mounts = when {
                volume != null -> Mounts(network = listOf(NetworkMount(volumeId = volume.id, path = volumePath)))
                persistent != null -> Mounts(persistent = persistent)
                else -> null
            },
            args = s.args.trim().ifEmpty { null },
            disk = disk,
            env = s.envEntries.filter { it.key.isNotBlank() }.associate { it.key.trim() to it.value },
            ports = s.portEntries.filter { it.port.isNotBlank() }.map { "${it.port.trim()}/${it.protocol}" },
            startSsh = s.startSsh,
            startJupyter = s.startJupyter,
        )

        _state.update { it.copy(isCreating = true, createError = null) }
        viewModelScope.launch {
            runCatching { podRepository.createPod(request) }
                .onSuccess { pod -> onDone(pod.id) }
                .onFailure { e ->
                    _state.update { it.copy(isCreating = false, createError = e.message ?: "Create failed") }
                }
        }
    }

    private fun updateEnv(index: Int, transform: (EnvEntry) -> EnvEntry) {
        _state.update {
            it.copy(envEntries = it.envEntries.mapIndexed { i, e -> if (i == index) transform(e) else e })
        }
    }

    private fun updatePort(index: Int, transform: (PortEntry) -> PortEntry) {
        _state.update {
            it.copy(portEntries = it.portEntries.mapIndexed { i, p -> if (i == index) transform(p) else p })
        }
    }

    private fun fail(message: String) {
        _state.update { it.copy(createError = message) }
    }

    private fun maxCountFor(gpu: GpuType, cloud: Cloud): Int =
        if (cloud == Cloud.SECURE) gpu.maxCount.secure else gpu.maxCount.community

    private fun gpuInDataCenter(gpu: GpuType, dcId: String): Boolean {
        val dcs = gpu.dataCenters ?: return true
        return dcs.any { it.id == dcId }
    }

    private fun parsePortSpec(spec: String): PortEntry {
        val idx = spec.indexOf('/')
        return if (idx > 0) {
            PortEntry(spec.substring(0, idx), spec.substring(idx + 1).ifBlank { "tcp" })
        } else {
            PortEntry(spec, "tcp")
        }
    }
}
