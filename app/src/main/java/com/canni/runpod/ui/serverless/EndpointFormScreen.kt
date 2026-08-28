package com.canni.runpod.ui.serverless

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canni.runpod.data.api.dto.CpuType
import com.canni.runpod.ui.components.DropdownField
import com.canni.runpod.ui.components.DropdownOption
import com.canni.runpod.ui.components.EnvEntry
import com.canni.runpod.ui.components.EnvVarEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndpointFormScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: EndpointFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEdit) "Edit endpoint" else "New endpoint") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    TextButton(
                        onClick = {
                            if (viewModel.isEdit) {
                                viewModel.submit { onBack() }
                            } else {
                                viewModel.submit { id -> onCreated(id) }
                            }
                        },
                        enabled = !state.isSubmitting,
                    ) {
                        Text(if (viewModel.isEdit) "Save" else "Create")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "name") {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.hubTitle?.let { title ->
                item(key = "hub_source") {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Based on hub repo: $title",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (!viewModel.isEdit) {
                item(key = "type") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = state.endpointType == EndpointType.QUEUE,
                            onClick = { viewModel.setEndpointType(EndpointType.QUEUE) },
                            label = { Text("Queue") },
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = state.endpointType == EndpointType.LOAD_BALANCER,
                            onClick = { viewModel.setEndpointType(EndpointType.LOAD_BALANCER) },
                            label = { Text("Load balancer") },
                        )
                    }
                }
            }

            item(key = "compute") {
                ComputeCard(
                    state = state,
                    isEdit = viewModel.isEdit,
                    onComputeKind = viewModel::setComputeKind,
                    onPool = viewModel::selectPool,
                    onGpuCount = viewModel::onGpuCountChange,
                    onCpu = viewModel::selectCpu,
                    onCpuVcpu = viewModel::onCpuVcpuChange,
                )
            }

            item(key = "container") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Container",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = state.image,
                            onValueChange = viewModel::onImageChange,
                            label = { Text("Image") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = state.args,
                            onValueChange = viewModel::onArgsChange,
                            label = { Text("Startup command (args)") },
                            minLines = 1,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = state.disk,
                            onValueChange = viewModel::onDiskChange,
                            label = { Text("Disk (GB)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        state.portEntries.forEachIndexed { index, port ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = port.port,
                                    onValueChange = { viewModel.onPortChange(index, it) },
                                    label = { Text("Port") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                PortProtocolPicker(
                                    protocol = port.protocol,
                                    onChange = { viewModel.onPortProtocolChange(index, it) },
                                )
                                IconButton(onClick = { viewModel.removePortEntry(index) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Remove port",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                        TextButton(onClick = viewModel::addPortEntry) {
                            Text("Add port")
                        }
                        OutlinedButton(
                            onClick = viewModel::openEnvEditor,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Environment variables (${state.envEntries.size})")
                        }
                    }
                }
            }

            item(key = "scaling") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Workers & scaling",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.workersMin,
                                onValueChange = viewModel::onWorkersMinChange,
                                label = { Text("Min workers") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = state.workersMax,
                                onValueChange = viewModel::onWorkersMaxChange,
                                label = { Text("Max workers") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (!state.isRequestCount) {
                            OutlinedTextField(
                                value = state.idleTimeout,
                                onValueChange = viewModel::onIdleTimeoutChange,
                                label = { Text("Idle timeout (seconds)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (state.endpointType == EndpointType.QUEUE) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = state.scalingType == ScalingType.QUEUE_DELAY,
                                    onClick = { viewModel.setScalingType(ScalingType.QUEUE_DELAY) },
                                    label = { Text("Queue delay") },
                                )
                                Spacer(Modifier.width(8.dp))
                                FilterChip(
                                    selected = state.scalingType == ScalingType.REQUEST_COUNT,
                                    onClick = { viewModel.setScalingType(ScalingType.REQUEST_COUNT) },
                                    label = { Text("Request count") },
                                )
                            }
                        }
                        if (state.isRequestCount) {
                            OutlinedTextField(
                                value = state.requestCount,
                                onValueChange = viewModel::onRequestCountChange,
                                label = { Text("Scale on in-flight requests") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            OutlinedTextField(
                                value = state.queueDelay,
                                onValueChange = viewModel::onQueueDelayChange,
                                label = { Text("Queue delay threshold (seconds)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            item(key = "advanced") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Advanced",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = viewModel::openDataCenterPicker,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (state.selectedDataCenterIds.isEmpty()) {
                                    "Data centers (any)"
                                } else {
                                    "Data centers (${state.selectedDataCenterIds.size} selected)"
                                },
                            )
                        }
                        OutlinedButton(
                            onClick = viewModel::openVolumePicker,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (state.selectedVolumeIds.isEmpty()) {
                                    "Network volumes (none)"
                                } else {
                                    "Network volumes (${state.selectedVolumeIds.size} attached)"
                                },
                            )
                        }
                        DropdownField(
                            label = "FlashBoot",
                            selected = DropdownOption(
                                state.flashboot,
                                when (state.flashboot) {
                                    "FLASHBOOT" -> "FlashBoot"
                                    "PRIORITY_FLASHBOOT" -> "Priority FlashBoot"
                                    else -> "Off"
                                },
                            ),
                            options = listOf(
                                DropdownOption("OFF", "Off"),
                                DropdownOption("FLASHBOOT", "FlashBoot"),
                                DropdownOption("PRIORITY_FLASHBOOT", "Priority FlashBoot"),
                            ),
                            onSelect = { viewModel.setFlashboot(it.value) },
                        )
                        OutlinedTextField(
                            value = state.timeout,
                            onValueChange = viewModel::onTimeoutChange,
                            label = { Text("Request timeout (ms)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (!viewModel.isEdit) {
                item(key = "template") {
                        DropdownField(
                            label = "Base on template (optional)",
                            selected = state.selectedTemplateId?.let { id ->
                                state.templates.find { t -> t.id == id }?.let { DropdownOption(it.id, it.name) }
                            } ?: DropdownOption("", "(none)"),
                            options = listOf(DropdownOption("", "(none)")) +
                                state.templates.map { DropdownOption(it.id, it.name) },
                            onSelect = { opt ->
                                viewModel.selectTemplate(
                                    if (opt.value.isEmpty()) null
                                    else state.templates.find { t -> t.id == opt.value },
                                )
                            },
                        )
                }
            }

            if (state.isLoadingCatalog) {
                item(key = "catalog_loading") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Loading catalog…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            state.error?.let { error ->
                item(key = "error") {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (state.envEditorVisible) {
        EnvVarEditor(
            entries = state.envEntries.map { EnvEntry(it.key, it.value) },
            onKeyChange = viewModel::onEnvKeyChange,
            onValueChange = viewModel::onEnvValueChange,
            onAdd = viewModel::addEnvEntry,
            onRemove = viewModel::removeEnvEntry,
            onBack = viewModel::closeEnvEditor,
        )
    }

    if (state.dataCenterPickerVisible) {
        MultiSelectPicker(
            title = "Data centers",
            options = state.dataCenters.map { DropdownOption(it.id, "${it.name} (${it.id})") },
            selected = state.selectedDataCenterIds,
            onToggle = viewModel::toggleDataCenter,
            onBack = viewModel::closeDataCenterPicker,
        )
    }

    if (state.volumePickerVisible) {
        MultiSelectPicker(
            title = "Network volumes",
            options = state.networkVolumes.map {
                DropdownOption(it.id, "${it.name} (${it.size ?: "?"} GB)")
            },
            selected = state.selectedVolumeIds,
            onToggle = viewModel::toggleVolume,
            onBack = viewModel::closeVolumePicker,
        )
    }
}

@Composable
private fun ComputeCard(
    state: EndpointFormViewModel.UiState,
    isEdit: Boolean,
    onComputeKind: (ComputeKind) -> Unit,
    onPool: (GpuPoolOption) -> Unit,
    onGpuCount: (String) -> Unit,
    onCpu: (CpuType) -> Unit,
    onCpuVcpu: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Compute",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = state.computeKind == ComputeKind.GPU,
                    onClick = { onComputeKind(ComputeKind.GPU) },
                    enabled = !isEdit,
                    label = { Text("GPU") },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.computeKind == ComputeKind.CPU,
                    onClick = { onComputeKind(ComputeKind.CPU) },
                    enabled = !isEdit,
                    label = { Text("CPU") },
                )
            }
            if (state.computeKind == ComputeKind.GPU) {
                DropdownField(
                    label = "GPU pool",
                    selected = state.selectedPoolId?.let { id ->
                        state.gpuPools.find { p -> p.id == id }?.let { DropdownOption(it.id, it.label) }
                    },
                    options = state.gpuPools.map { DropdownOption(it.id, it.label) },
                    onSelect = { opt ->
                        onPool(state.gpuPools.find { p -> p.id == opt.value } ?: GpuPoolOption(opt.value, opt.value))
                    },
                )
                OutlinedTextField(
                    value = state.gpuCount,
                    onValueChange = onGpuCount,
                    label = { Text("GPUs per worker") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                DropdownField(
                    label = "CPU flavor",
                    selected = state.selectedCpuId?.let { id ->
                        state.cpuTypes.find { f -> f.id == id }?.let {
                            DropdownOption(it.id, "${it.id} · ${it.name}")
                        }
                    },
                    options = state.cpuTypes.map { DropdownOption(it.id, "${it.id} · ${it.name}") },
                    onSelect = { opt ->
                        onCpu(state.cpuTypes.find { f -> f.id == opt.value } ?: CpuType(opt.value, opt.value))
                    },
                )
                OutlinedTextField(
                    value = state.cpuVcpu,
                    onValueChange = onCpuVcpu,
                    label = { Text("vCPUs (power of two)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                val flavor = state.cpuTypes.find { it.id == state.selectedCpuId }
                val vcpu = state.cpuVcpu.trim().toIntOrNull()
                if (flavor != null && vcpu != null && vcpu > 0) {
                    Text(
                        text = "≈ ${(flavor.ramGbPerVcpu * vcpu).toInt()} GB RAM",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PortProtocolPicker(protocol: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.width(88.dp),
        ) {
            Text(protocol)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Protocol", modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("http", "tcp", "udp").forEach { proto ->
                DropdownMenuItem(
                    text = { Text(proto) },
                    onClick = {
                        expanded = false
                        onChange(proto)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiSelectPicker(
    title: String,
    options: List<DropdownOption>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onBack) { Text("Done") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(options, key = { it.value }) { option ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = option.value in selected,
                            onCheckedChange = { onToggle(option.value) },
                        )
                    }
                }
            }
        }
    }
}
