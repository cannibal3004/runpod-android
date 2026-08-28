package com.canni.runpod.ui.serverless

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.dto.CreateEndpointRequest
import com.canni.runpod.data.api.dto.CpuType
import com.canni.runpod.data.api.dto.DataCenter
import com.canni.runpod.data.api.dto.EndpointCpuConfig
import com.canni.runpod.data.api.dto.EndpointGpuConfig
import com.canni.runpod.data.api.dto.EndpointWorkers
import com.canni.runpod.data.api.dto.GpuType
import com.canni.runpod.data.api.dto.NetworkVolume
import com.canni.runpod.data.api.dto.ServerlessEndpoint
import com.canni.runpod.data.api.dto.Template
import com.canni.runpod.data.api.dto.UpdateEndpointRequest
import com.canni.runpod.data.repo.CatalogRepository
import com.canni.runpod.data.repo.NetworkVolumeRepository
import com.canni.runpod.data.repo.ServerlessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

enum class EndpointType {
    QUEUE,
    LOAD_BALANCER,
}

enum class ComputeKind {
    GPU,
    CPU,
}

enum class ScalingType {
    QUEUE_DELAY,
    REQUEST_COUNT,
}

data class GpuPoolOption(
    val id: String,
    val label: String,
)

data class PortEntry(
    val port: String = "",
    val protocol: String = "http",
)

@HiltViewModel
class EndpointFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverlessRepository: ServerlessRepository,
    private val catalogRepository: CatalogRepository,
    private val networkVolumeRepository: NetworkVolumeRepository,
) : ViewModel() {

    val isEdit: Boolean = savedStateHandle.get<String>("endpointId") != null
    private val endpointId: String = savedStateHandle.get<String>("endpointId").orEmpty()

    data class EnvEntry(
        val key: String = "",
        val value: String = "",
    )

    data class UiState(
        val endpoint: ServerlessEndpoint? = null,
        val gpuPools: List<GpuPoolOption> = emptyList(),
        val cpuTypes: List<CpuType> = emptyList(),
        val dataCenters: List<DataCenter> = emptyList(),
        val networkVolumes: List<NetworkVolume> = emptyList(),
        val templates: List<Template> = emptyList(),
        val isLoadingCatalog: Boolean = true,
        val catalogError: String? = null,

        val name: String = "",
        val endpointType: EndpointType = EndpointType.QUEUE,
        val computeKind: ComputeKind = ComputeKind.GPU,
        val selectedPoolId: String? = null,
        val gpuCount: String = "1",
        val selectedCpuId: String? = null,
        val cpuVcpu: String = "4",
        val image: String = "",
        val args: String = "",
        val disk: String = "50",
        val envEntries: List<EnvEntry> = emptyList(),
        val portEntries: List<PortEntry> = emptyList(),
        val workersMin: String = "0",
        val workersMax: String = "3",
        val idleTimeout: String = "10",
        val scalingType: ScalingType = ScalingType.QUEUE_DELAY,
        val requestCount: String = "4",
        val queueDelay: String = "4",
        val selectedDataCenterIds: Set<String> = emptySet(),
        val selectedVolumeIds: Set<String> = emptySet(),
        val selectedTemplateId: String? = null,
        val flashboot: String = "OFF",
        val timeout: String = "300000",

        val envEditorVisible: Boolean = false,
        val dataCenterPickerVisible: Boolean = false,
        val volumePickerVisible: Boolean = false,

        val isSubmitting: Boolean = false,
        val error: String? = null,
    ) {
        val isRequestCount: Boolean
            get() = scalingType == ScalingType.REQUEST_COUNT || endpointType == EndpointType.LOAD_BALANCER
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        loadCatalog()
        if (isEdit) loadEndpoint()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingCatalog = true, catalogError = null) }
            coroutineScope {
                val gpus = async { runCatching { catalogRepository.listServerlessGpuTypes() } }
                val cpus = async { runCatching { catalogRepository.listCpuTypes() } }
                val dcs = async { runCatching { catalogRepository.listDataCenters() } }
                val volumes = async { runCatching { networkVolumeRepository.listNetworkVolumes() } }
                val templates = async {
                    runCatching { catalogRepository.listAllTemplates() }
                }
                val gpusList = gpus.await().getOrNull()
                val cpusList = cpus.await().getOrNull() ?: emptyList()
                val dcsList = dcs.await().getOrNull() ?: emptyList()
                val volumesList = volumes.await().getOrNull() ?: emptyList()
                val templatesList = templates.await().getOrNull() ?: emptyList()
                val pools = gpusList.orEmpty()
                    .filter { it.pool != null }
                    .groupBy { it.pool!! }
                    .map { (pool, types) ->
                        GpuPoolOption(
                            id = pool,
                            label = "$pool (${types.size} types)",
                        )
                    }
                    .sortedBy { it.id.lowercase() }
                val failed = listOf(gpus.await(), cpus.await(), dcs.await(), volumes.await(), templates.await())
                    .count { it.isFailure }
                _state.update {
                    it.copy(
                        gpuPools = pools,
                        cpuTypes = cpusList,
                        dataCenters = dcsList,
                        networkVolumes = volumesList,
                        templates = templatesList,
                        isLoadingCatalog = false,
                        catalogError = if (failed == 5) "Failed to load catalog" else null,
                    )
                }
            }
        }
    }

    private fun loadEndpoint() {
        viewModelScope.launch {
            runCatching { serverlessRepository.getEndpoint(endpointId) }
                .onSuccess { ep -> prefill(ep) }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message ?: "Failed to load endpoint") }
                }
        }
    }

    private fun prefill(ep: ServerlessEndpoint) {
        val gpu = ep.gpu
        val cpu = ep.cpu?.firstOrNull()
        _state.update {
            it.copy(
                endpoint = ep,
                name = ep.name,
                endpointType = if (ep.isLoadBalancer) EndpointType.LOAD_BALANCER else EndpointType.QUEUE,
                computeKind = if (gpu != null) ComputeKind.GPU else ComputeKind.CPU,
                selectedPoolId = gpu?.pools?.firstOrNull(),
                gpuCount = (gpu?.count ?: 1).toString(),
                selectedCpuId = cpu?.id,
                cpuVcpu = (cpu?.vcpuCount ?: 0).toString(),
                image = ep.image.orEmpty(),
                args = ep.args.orEmpty(),
                disk = ep.disk?.toString().orEmpty(),
                envEntries = ep.env?.map { (k, v) -> EnvEntry(k, v) } ?: emptyList(),
                portEntries = ep.ports?.map { p ->
                    val parts = p.split("/")
                    PortEntry(parts.firstOrNull().orEmpty(), parts.getOrNull(1) ?: "http")
                } ?: emptyList(),
                workersMin = ep.workers?.min?.toString().orEmpty(),
                workersMax = ep.workers?.max?.toString().orEmpty(),
                idleTimeout = ep.workers?.idleTimeout?.toString().orEmpty(),
                scalingType = if (ep.scalingLabel?.startsWith("REQUEST_COUNT") == true) {
                    ScalingType.REQUEST_COUNT
                } else {
                    ScalingType.QUEUE_DELAY
                },
                requestCount = (ep.scaling as? kotlinx.serialization.json.JsonObject)
                    ?.get("requestCount")
                    ?.let { (it as? JsonPrimitive)?.content }
                    .orEmpty(),
                queueDelay = (ep.scaling as? kotlinx.serialization.json.JsonObject)
                    ?.get("queueDelay")
                    ?.let { (it as? JsonPrimitive)?.content }
                    .orEmpty(),
                selectedDataCenterIds = ep.dataCenterIds?.toSet() ?: emptySet(),
                selectedVolumeIds = ep.networkVolumes?.toSet() ?: emptySet(),
                flashboot = ep.flashboot ?: "OFF",
                timeout = ep.timeout?.toString() ?: "300000",
            )
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v, error = null) }
    fun onImageChange(v: String) = _state.update { it.copy(image = v, error = null) }
    fun onArgsChange(v: String) = _state.update { it.copy(args = v, error = null) }
    fun onDiskChange(v: String) = _state.update { it.copy(disk = v, error = null) }
    fun onGpuCountChange(v: String) = _state.update { it.copy(gpuCount = v, error = null) }
    fun onCpuVcpuChange(v: String) = _state.update { it.copy(cpuVcpu = v, error = null) }
    fun onWorkersMinChange(v: String) = _state.update { it.copy(workersMin = v, error = null) }
    fun onWorkersMaxChange(v: String) = _state.update { it.copy(workersMax = v, error = null) }
    fun onIdleTimeoutChange(v: String) = _state.update { it.copy(idleTimeout = v, error = null) }
    fun onRequestCountChange(v: String) = _state.update { it.copy(requestCount = v, error = null) }
    fun onQueueDelayChange(v: String) = _state.update { it.copy(queueDelay = v, error = null) }
    fun onTimeoutChange(v: String) = _state.update { it.copy(timeout = v, error = null) }

    fun setEndpointType(t: EndpointType) {
        if (isEdit) return
        _state.update { st ->
            st.copy(
                endpointType = t,
                scalingType = if (t == EndpointType.LOAD_BALANCER) ScalingType.REQUEST_COUNT else st.scalingType,
                error = null,
            )
        }
    }

    fun setComputeKind(k: ComputeKind) {
        if (isEdit) return
        _state.update { it.copy(computeKind = k, error = null) }
    }

    fun setScalingType(t: ScalingType) {
        if (_state.value.endpointType == EndpointType.LOAD_BALANCER) return
        _state.update { it.copy(scalingType = t, error = null) }
    }

    fun selectPool(pool: GpuPoolOption) {
        _state.update { it.copy(selectedPoolId = pool.id, error = null) }
    }

    fun selectCpu(flavor: CpuType) {
        _state.update {
            it.copy(selectedCpuId = flavor.id, cpuVcpu = flavor.vcpu.min.toString(), error = null)
        }
    }

    fun setFlashboot(v: String) {
        _state.update { it.copy(flashboot = v, error = null) }
    }

    fun selectTemplate(template: Template?) {
        _state.update { st ->
            if (template == null) {
                st.copy(selectedTemplateId = null, error = null)
            } else {
                st.copy(
                    selectedTemplateId = template.id,
                    name = st.name.ifBlank { template.name },
                    image = st.image.ifBlank { template.image },
                    args = st.args.ifBlank { templateArgsToServerlessArgs(template.args) },
                    disk = st.disk.ifBlank { template.disk?.toString().orEmpty() },
                    envEntries = if (st.envEntries.isEmpty()) {
                        template.env?.map { (k, v) -> EnvEntry(k, v) } ?: emptyList()
                    } else {
                        st.envEntries
                    },
                    portEntries = if (st.portEntries.isEmpty()) {
                        template.ports?.map { p ->
                            val parts = p.split("/")
                            PortEntry(parts.firstOrNull().orEmpty(), parts.getOrNull(1) ?: "http")
                        } ?: emptyList()
                    } else {
                        st.portEntries
                    },
                    error = null,
                )
            }
        }
    }

    private fun templateArgsToServerlessArgs(args: String?): String {
        if (args.isNullOrBlank()) return ""
        return try {
            val el = Json.parseToJsonElement(args)
            val obj = (el as? JsonObject) ?: return args
            val cmd = (obj["cmd"] as? JsonArray) ?: return args
            cmd.joinToString(" ") { (it as? JsonPrimitive)?.content ?: "" }.trim()
        } catch (e: Exception) {
            args
        }
    }

    fun openEnvEditor() {
        _state.update { it.copy(envEditorVisible = true) }
    }

    fun closeEnvEditor() {
        _state.update { it.copy(envEditorVisible = false) }
    }

    fun openDataCenterPicker() {
        _state.update { it.copy(dataCenterPickerVisible = true) }
    }

    fun closeDataCenterPicker() {
        _state.update { it.copy(dataCenterPickerVisible = false) }
    }

    fun openVolumePicker() {
        _state.update { it.copy(volumePickerVisible = true) }
    }

    fun closeVolumePicker() {
        _state.update { it.copy(volumePickerVisible = false) }
    }

    fun toggleDataCenter(id: String) {
        _state.update { st ->
            val next = st.selectedDataCenterIds.toMutableSet()
            if (!next.add(id)) next.remove(id)
            st.copy(selectedDataCenterIds = next)
        }
    }

    fun toggleVolume(id: String) {
        _state.update { st ->
            val next = st.selectedVolumeIds.toMutableSet()
            if (!next.add(id)) next.remove(id)
            st.copy(selectedVolumeIds = next)
        }
    }

    fun onPortChange(index: Int, value: String) {
        _state.update { st ->
            val entries = st.portEntries.toMutableList()
            entries[index] = entries[index].copy(port = value)
            st.copy(portEntries = entries, error = null)
        }
    }

    fun onPortProtocolChange(index: Int, protocol: String) {
        _state.update { st ->
            val entries = st.portEntries.toMutableList()
            entries[index] = entries[index].copy(protocol = protocol)
            st.copy(portEntries = entries, error = null)
        }
    }

    fun addPortEntry() {
        _state.update { it.copy(portEntries = it.portEntries + PortEntry()) }
    }

    fun removePortEntry(index: Int) {
        _state.update { st ->
            val entries = st.portEntries.toMutableList()
            if (index in entries.indices) entries.removeAt(index)
            st.copy(portEntries = entries)
        }
    }

    fun onEnvKeyChange(index: Int, key: String) {
        _state.update { st ->
            val entries = st.envEntries.toMutableList()
            entries[index] = entries[index].copy(key = key)
            st.copy(envEntries = entries)
        }
    }

    fun onEnvValueChange(index: Int, value: String) {
        _state.update { st ->
            val entries = st.envEntries.toMutableList()
            entries[index] = entries[index].copy(value = value)
            st.copy(envEntries = entries)
        }
    }

    fun addEnvEntry() {
        _state.update { it.copy(envEntries = it.envEntries + EnvEntry()) }
    }

    fun removeEnvEntry(index: Int) {
        _state.update { st ->
            val entries = st.envEntries.toMutableList()
            if (index in entries.indices) entries.removeAt(index)
            st.copy(envEntries = entries)
        }
    }

    fun submit(onSuccess: (String) -> Unit) {
        val st = _state.value
        val validationError = validate(st)
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val diskValue = st.disk.trim().toIntOrNull()
            val envMap = st.envEntries
                .filter { it.key.isNotBlank() }
                .associate { it.key.trim() to it.value }
                .ifEmpty { null }
            val ports = st.portEntries
                .map { "${it.port.trim()}/${it.protocol.lowercase()}" }
                .filter { it != "/http" && it != "/tcp" && it != "/udp" }
                .ifEmpty { null }
            val gpu = if (st.computeKind == ComputeKind.GPU) {
                val pool = st.selectedPoolId
                if (pool == null) {
                    _state.update { it.copy(isSubmitting = false, error = "Select a GPU pool") }
                    return@launch
                }
                EndpointGpuConfig(
                    pools = listOf(pool),
                    count = st.gpuCount.trim().toIntOrNull() ?: 1,
                )
            } else {
                null
            }
            val cpu = if (st.computeKind == ComputeKind.CPU) {
                val flavorId = st.selectedCpuId
                if (flavorId == null) {
                    _state.update { it.copy(isSubmitting = false, error = "Select a CPU flavor") }
                    return@launch
                }
                listOf(EndpointCpuConfig(id = flavorId, vcpuCount = st.cpuVcpu.trim().toIntOrNull() ?: 2))
            } else {
                null
            }
            val workers = EndpointWorkers(
                min = st.workersMin.trim().toIntOrNull() ?: 0,
                max = st.workersMax.trim().toIntOrNull() ?: 3,
                idleTimeout = if (st.isRequestCount) null else st.idleTimeout.trim().toIntOrNull(),
            )
            val scaling = buildJsonObject {
                if (st.isRequestCount) {
                    put("type", JsonPrimitive("REQUEST_COUNT"))
                    put("requestCount", JsonPrimitive(st.requestCount.trim().toIntOrNull() ?: 4))
                } else {
                    put("type", JsonPrimitive("QUEUE_DELAY"))
                    put("queueDelay", JsonPrimitive(st.queueDelay.trim().toDoubleOrNull() ?: 4.0))
                }
            }
            val dataCenterIds = st.selectedDataCenterIds.ifEmpty { null }?.toList()
            val networkVolumes = st.selectedVolumeIds.ifEmpty { null }?.toList()
            val timeoutValue = st.timeout.trim().toIntOrNull()
            val flashbootValue = st.flashboot
            val argsValue = st.args.trim().ifBlank { null }
            val imageValue = st.image.trim().ifBlank { null }

            val result = if (isEdit) {
                runCatching {
                    serverlessRepository.updateEndpoint(
                        endpointId,
                        UpdateEndpointRequest(
                            name = st.name.trim(),
                            image = imageValue,
                            args = argsValue,
                            disk = diskValue,
                            env = envMap,
                            ports = ports,
                            gpu = gpu,
                            cpu = cpu,
                            workers = workers,
                            scaling = scaling,
                            dataCenterIds = dataCenterIds,
                            networkVolumes = networkVolumes,
                            timeout = timeoutValue,
                            flashboot = flashbootValue,
                        ),
                    )
                }
            } else {
                runCatching {
                    serverlessRepository.createEndpoint(
                        CreateEndpointRequest(
                            name = st.name.trim(),
                            type = if (st.endpointType == EndpointType.LOAD_BALANCER) "LOAD_BALANCER" else "QUEUE",
                            image = imageValue,
                            args = argsValue,
                            disk = diskValue,
                            env = envMap,
                            ports = ports,
                            gpu = gpu,
                            cpu = cpu,
                            workers = workers,
                            scaling = scaling,
                            dataCenterIds = dataCenterIds,
                            networkVolumes = networkVolumes,
                            templateId = st.selectedTemplateId,
                            timeout = timeoutValue,
                            flashboot = flashbootValue,
                        ),
                    )
                }
            }
            result
                .onSuccess { endpoint ->
                    onSuccess(endpoint.id)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isSubmitting = false, error = e.message ?: "Failed to save endpoint")
                    }
                }
        }
    }

    private fun validate(st: UiState): String? {
        if (st.name.isBlank()) return "Name is required"
        if (st.image.isBlank()) return "Image is required"
        val disk = st.disk.trim().toIntOrNull()
        if (disk == null || disk < 1) return "Disk must be at least 1 GB"
        if (st.computeKind == ComputeKind.GPU) {
            if (st.selectedPoolId == null) return "Select a GPU pool"
            val count = st.gpuCount.trim().toIntOrNull()
            if (count == null || count < 1) return "GPU count must be at least 1"
        } else {
            if (st.selectedCpuId == null) return "Select a CPU flavor"
            val vcpu = st.cpuVcpu.trim().toIntOrNull()
            val flavor = st.cpuTypes.find { it.id == st.selectedCpuId }
            if (vcpu == null || vcpu < (flavor?.vcpu?.min ?: 2)) return "Invalid vCPU count"
        }
        val min = st.workersMin.trim().toIntOrNull()
        val max = st.workersMax.trim().toIntOrNull()
        if (min == null || min < 0) return "Min workers must be 0 or more"
        if (max == null || max < min) return "Max workers must be at least the minimum"
        if (st.isRequestCount) {
            val rc = st.requestCount.trim().toIntOrNull()
            if (rc == null || rc < 1) return "Request count must be at least 1"
        } else {
            val qt = st.idleTimeout.trim().toIntOrNull()
            if (qt == null || qt < 1 || qt > 3600) return "Idle timeout must be 1-3600 seconds"
            val qd = st.queueDelay.trim().toDoubleOrNull()
            if (qd == null || qd < 0.5) return "Queue delay must be at least 0.5s"
        }
        val timeout = st.timeout.trim().toIntOrNull()
        if (timeout == null || timeout <= 0) return "Timeout must be a positive number of milliseconds"
        for (port in st.portEntries) {
            val p = port.port.trim().toIntOrNull()
            if (p == null || p < 1 || p > 65535) return "Port ${port.port} is not a valid port number"
        }
        return null
    }
}
