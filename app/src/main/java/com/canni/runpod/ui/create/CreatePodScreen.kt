package com.canni.runpod.ui.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canni.runpod.data.api.dto.AvailabilityLevel
import com.canni.runpod.data.api.dto.Cloud
import com.canni.runpod.data.api.dto.GpuType
import com.canni.runpod.data.api.dto.NetworkVolume
import com.canni.runpod.data.api.dto.VolumeType
import com.canni.runpod.ui.components.DropdownField
import com.canni.runpod.ui.components.DropdownOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePodScreen(
    onBack: () -> Unit,
    onCreated: (podId: String) -> Unit,
    viewModel: CreatePodViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadCatalog()
    }

    if (state.pickerVisible) {
        GpuPickerScreen(
            state = state,
            viewModel = viewModel,
            onBack = viewModel::hideGpuPicker,
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New pod") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (!state.isCreating) scope.launch { viewModel.createPod(onCreated) }
                },
                icon = {
                    if (state.isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                },
                text = { Text(if (state.isCreating) "Creating…" else "Create pod") },
            )
        },
    ) { padding ->
        if (state.isLoadingCatalog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            ) {
                if (state.createError != null) {
                    item(key = "create_error") {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                        ) {
                            Text(
                                text = state.createError!!,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                if (state.catalogError != null) {
                    item(key = "catalog_error") {
                        Text(
                            text = "Catalog load failed: ${state.catalogError}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }

                item(key = "source") {
                    SectionHeader("Container")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.useTemplate,
                            onClick = { viewModel.onSourceChange(true) },
                            label = { Text("Template") },
                        )
                        FilterChip(
                            selected = !state.useTemplate,
                            onClick = { viewModel.onSourceChange(false) },
                            label = { Text("Custom image") },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.useTemplate) {
                        DropdownField(
                            label = "Template",
                            selected = state.templates.find { it.id == state.selectedTemplateId }?.let {
                                DropdownOption(it.id, it.name)
                            },
                            options = state.templates.map { DropdownOption(it.id, it.name) },
                            onSelect = { viewModel.onSelectTemplate(it.value) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        OutlinedTextField(
                            value = state.image,
                            onValueChange = viewModel::onImageChange,
                            label = { Text("Docker image") },
                            placeholder = { Text("runpod/pytorch:1.0.2-cu1281-torch280-ubuntu2404") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item(key = "name") {
                    SectionHeader("Name")
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Pod name") },
                        placeholder = { Text("my-training-pod") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item(key = "gpu") {
                    SectionHeader("GPU")
                    val gpu = state.gpuTypes.find { it.id == state.selectedGpuId }
                    OutlinedCard(
                        onClick = { viewModel.showGpuPicker() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                if (gpu != null) {
                                    Text(
                                        text = gpu.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${gpu.memory} GB VRAM · ${cloudLabel(state.cloud)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    Text(
                                        text = "Select a GPU type",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Browse, filter and compare",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (gpu != null) {
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "$%.2f/hr".format(gpuPrice(gpu, state.cloud)),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    if (gpu != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Count", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { viewModel.onGpuCountChange(-1) }) {
                                Text("−", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = state.gpuCount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.Center,
                            )
                            TextButton(onClick = { viewModel.onGpuCountChange(1) }) {
                                Text("+", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "≈ ${"%.2f".format(gpuPrice(gpu, state.cloud) * state.gpuCount)} USD/hr for ${state.gpuCount} GPU${if (state.gpuCount > 1) "s" else ""} on ${cloudLabel(state.cloud)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                item(key = "dc") {
                    SectionHeader("Data center")
                    val volumeLocked = state.dataStorage == CreatePodViewModel.DataStorage.NETWORK &&
                        state.selectedVolumeId != null
                    DropdownField(
                        label = if (volumeLocked) "Data center" else "Preferred data center",
                        selected = state.selectedDataCenterId?.let { id ->
                            state.dataCenters.find { it.id == id }?.let {
                                DropdownOption(it.id, "${it.id} — ${it.name}")
                            } ?: DropdownOption(id, id)
                        },
                        options = listOf(
                            DropdownOption("", "Any (scheduler decides)"),
                        ) + state.dataCenters.map {
                            DropdownOption(it.id, "${it.id} — ${it.name}")
                        },
                        onSelect = { viewModel.onSelectDataCenter(it.value.ifEmpty { null }) },
                        enabled = !volumeLocked,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (volumeLocked) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Locked to the selected volume's data center — pods must share the volume's location.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item(key = "storage") {
                    SectionHeader("Data storage")
                    val templateHasPersistent = state.selectedTemplateId
                        ?.let { id -> state.templates.find { it.id == id }?.mounts?.persistent != null }
                        ?: false
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CreatePodViewModel.DataStorage.entries.forEach { mode ->
                            FilterChip(
                                selected = state.dataStorage == mode,
                                onClick = { viewModel.onDataStorageChange(mode) },
                                label = { Text(storageLabel(mode)) },
                                enabled = !templateHasPersistent || mode == CreatePodViewModel.DataStorage.NONE,
                            )
                        }
                    }
                    when (state.dataStorage) {
                        CreatePodViewModel.DataStorage.NONE -> {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (templateHasPersistent) {
                                    "This template includes a persistent volume; no additional storage can be added."
                                } else {
                                    "Only the ephemeral container disk will be used."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        CreatePodViewModel.DataStorage.VOLUME_DISK -> {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.persistentSize,
                                onValueChange = viewModel::onPersistentSizeChange,
                                label = { Text("Volume disk size (GB)") },
                                placeholder = { Text("20") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.persistentPath,
                                onValueChange = viewModel::onPersistentPathChange,
                                label = { Text("Mount path in container") },
                                placeholder = { Text("/workspace") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Host-local persistent storage. Data survives pod restarts, but not a host failure.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        CreatePodViewModel.DataStorage.NETWORK -> {
                            val volume = state.networkVolumes.find { it.id == state.selectedVolumeId }
                            Spacer(Modifier.height(8.dp))
                            DropdownField(
                                label = "Volume",
                                selected = volume?.let { DropdownOption(it.id, volumeLabel(it)) },
                                options = listOf(
                                    DropdownOption("", "No volume"),
                                ) + state.networkVolumes.map { DropdownOption(it.id, volumeLabel(it)) },
                                onSelect = { viewModel.onSelectVolume(it.value.ifEmpty { null }) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            when {
                                state.volumesError != null -> {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Failed to load network volumes: ${state.volumesError}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                volume == null && state.networkVolumes.isEmpty() -> {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "No network volumes on your account yet. Create one from the Storage menu to attach it here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                volume != null -> {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = state.volumePath,
                                        onValueChange = viewModel::onVolumePathChange,
                                        label = { Text("Mount path in container") },
                                        placeholder = { Text("/runpod-volume") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = volumeMeta(volume, state.volumePath.trim().ifEmpty { "/runpod-volume" }),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "disk") {
                    SectionHeader("Disk & arguments")
                    OutlinedTextField(
                        value = state.disk,
                        onValueChange = viewModel::onDiskChange,
                        label = { Text("Container disk (GB)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.args,
                        onValueChange = viewModel::onArgsChange,
                        label = { Text("Entrypoint args (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item(key = "env") {
                    SectionHeader("Environment variables")
                    state.envEntries.forEachIndexed { index, entry ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = entry.key,
                                onValueChange = { viewModel.onEnvKeyChange(index, it) },
                                label = { Text("KEY") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = entry.value,
                                onValueChange = { viewModel.onEnvValueChange(index, it) },
                                label = { Text("value") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.showSecretPicker(index) }) {
                                        Icon(Icons.Filled.Lock, contentDescription = "Use a secret")
                                    }
                                },
                                modifier = Modifier.weight(1.4f),
                            )
                            IconButton(onClick = { viewModel.removeEnvEntry(index) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove variable")
                            }
                        }
                        if (index < state.envEntries.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.addEnvEntry() }) {
                        Text("+ Add variable")
                    }
                }

                item(key = "ports") {
                    SectionHeader("Exposed ports")
                    state.portEntries.forEachIndexed { index, entry ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = entry.port,
                                onValueChange = { viewModel.onPortChange(index, it) },
                                label = { Text("Port") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = entry.protocol == "http",
                                onClick = { viewModel.onPortProtocolChange(index, "http") },
                                label = { Text("http") },
                            )
                            Spacer(Modifier.width(4.dp))
                            FilterChip(
                                selected = entry.protocol == "tcp",
                                onClick = { viewModel.onPortProtocolChange(index, "tcp") },
                                label = { Text("tcp") },
                            )
                            IconButton(onClick = { viewModel.removePortEntry(index) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove port")
                            }
                        }
                        if (index < state.portEntries.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.addPortEntry() }) {
                        Text("+ Add port")
                    }
                }

                item(key = "toggles") {
                    SectionHeader("Startup")
                    ToggleRow(
                        title = "SSH access",
                        description = "Injects your registered SSH public key. Adds 22/tcp for direct SSH.",
                        checked = state.startSsh,
                        onChange = viewModel::onToggleSsh,
                    )
                    Spacer(Modifier.height(12.dp))
                    ToggleRow(
                        title = "JupyterLab",
                        description = "Generates a JupyterLab password. Adds 8888/http.",
                        checked = state.startJupyter,
                        onChange = viewModel::onToggleJupyter,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    state.secretPickerIndex?.let { index ->
        Dialog(onDismissRequest = viewModel::hideSecretPicker) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp,
                modifier = Modifier.width(340.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Use a secret", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Sets the value to the secret's reference.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    when {
                        state.secretsError != null -> Text(
                            text = "Failed to load secrets: ${state.secretsError}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        state.secrets.isEmpty() -> Text(
                            text = "No secrets on your account yet. Create one from the Secrets menu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        else -> Column(
                            Modifier
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            state.secrets.forEach { secret ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onSelectEnvSecret(index, secret) }
                                        .padding(vertical = 8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = secret.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (!secret.description.isNullOrBlank()) {
                                            Text(
                                                text = secret.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GpuPickerScreen(
    state: CreatePodViewModel.UiState,
    viewModel: CreatePodViewModel,
    onBack: () -> Unit,
) {
    val restrictDc = if (state.dataStorage == CreatePodViewModel.DataStorage.NETWORK) {
        state.selectedVolumeId?.let { id -> state.networkVolumes.find { it.id == id }?.dataCenter }
    } else {
        null
    }
    val gpus = filteredGpus(
        all = state.gpuTypes,
        cloud = state.cloud,
        query = state.gpuQuery,
        memory = state.gpuMemoryFilter,
        inStockOnly = state.gpuInStockOnly,
        sort = state.gpuSort,
        restrictDc = restrictDc,
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select GPU") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.cloud == Cloud.SECURE,
                    onClick = { viewModel.onCloudChange(Cloud.SECURE) },
                    label = { Text("Secure") },
                )
                FilterChip(
                    selected = state.cloud == Cloud.COMMUNITY,
                    onClick = { viewModel.onCloudChange(Cloud.COMMUNITY) },
                    label = { Text("Community") },
                )
            }
            OutlinedTextField(
                value = state.gpuQuery,
                onValueChange = viewModel::onGpuQueryChange,
                placeholder = { Text("Search GPUs") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "mem_any") {
                    FilterChip(
                        selected = state.gpuMemoryFilter == CreatePodViewModel.GpuMemoryFilter.ANY,
                        onClick = { viewModel.onGpuMemoryFilterChange(CreatePodViewModel.GpuMemoryFilter.ANY) },
                        label = { Text("Any VRAM") },
                    )
                }
                item(key = "mem_le24") {
                    FilterChip(
                        selected = state.gpuMemoryFilter == CreatePodViewModel.GpuMemoryFilter.LE_24,
                        onClick = { viewModel.onGpuMemoryFilterChange(CreatePodViewModel.GpuMemoryFilter.LE_24) },
                        label = { Text("≤ 24 GB") },
                    )
                }
                item(key = "mem_24_48") {
                    FilterChip(
                        selected = state.gpuMemoryFilter == CreatePodViewModel.GpuMemoryFilter.B_24_48,
                        onClick = { viewModel.onGpuMemoryFilterChange(CreatePodViewModel.GpuMemoryFilter.B_24_48) },
                        label = { Text("24–48 GB") },
                    )
                }
                item(key = "mem_48_96") {
                    FilterChip(
                        selected = state.gpuMemoryFilter == CreatePodViewModel.GpuMemoryFilter.B_48_96,
                        onClick = { viewModel.onGpuMemoryFilterChange(CreatePodViewModel.GpuMemoryFilter.B_48_96) },
                        label = { Text("48–96 GB") },
                    )
                }
                item(key = "mem_gt96") {
                    FilterChip(
                        selected = state.gpuMemoryFilter == CreatePodViewModel.GpuMemoryFilter.GT_96,
                        onClick = { viewModel.onGpuMemoryFilterChange(CreatePodViewModel.GpuMemoryFilter.GT_96) },
                        label = { Text("96+ GB") },
                    )
                }
                item(key = "stock") {
                    FilterChip(
                        selected = state.gpuInStockOnly,
                        onClick = { viewModel.onGpuInStockOnlyChange(!state.gpuInStockOnly) },
                        label = { Text("In stock") },
                    )
                }
                item(key = "sort_price") {
                    FilterChip(
                        selected = state.gpuSort == CreatePodViewModel.GpuSort.PRICE,
                        onClick = { viewModel.onGpuSortChange(CreatePodViewModel.GpuSort.PRICE) },
                        label = { Text("Price") },
                    )
                }
                item(key = "sort_name") {
                    FilterChip(
                        selected = state.gpuSort == CreatePodViewModel.GpuSort.NAME,
                        onClick = { viewModel.onGpuSortChange(CreatePodViewModel.GpuSort.NAME) },
                        label = { Text("Name") },
                    )
                }
                item(key = "sort_memory") {
                    FilterChip(
                        selected = state.gpuSort == CreatePodViewModel.GpuSort.MEMORY,
                        onClick = { viewModel.onGpuSortChange(CreatePodViewModel.GpuSort.MEMORY) },
                        label = { Text("VRAM") },
                    )
                }
            }
            Text(
                text = "${gpus.size} GPU type${if (gpus.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (gpus.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (restrictDc != null) {
                            "No GPU types are offered in $restrictDc."
                        } else {
                            "No GPUs match these filters"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(gpus, key = { it.id }) { gpu ->
                        GpuCard(
                            gpu = gpu,
                            cloud = state.cloud,
                            selected = gpu.id == state.selectedGpuId,
                            onClick = { viewModel.onSelectGpu(gpu.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GpuCard(
    gpu: GpuType,
    cloud: Cloud,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val available = gpu.availableOn(cloud)
    val maxCount = if (cloud == Cloud.SECURE) gpu.maxCount.secure else gpu.maxCount.community
    Card(
        onClick = { if (available) onClick() },
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (available) 1f else 0.45f),
            ) {
                Text(
                    text = gpu.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("${gpu.memory} GB VRAM")
                        gpu.manufacturer?.takeIf { m -> m.isNotBlank() }?.let { append(" · $it") }
                        if (maxCount > 1) append(" · up to ${maxCount}×")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                AvailabilityRow(
                    availability = gpu.availability,
                    available = available,
                    cloud = cloud,
                )
            }
            if (available) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "$%.2f/hr".format(gpuPrice(gpu, cloud)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailabilityRow(
    availability: AvailabilityLevel?,
    available: Boolean,
    cloud: Cloud,
) {
    val dotColor: Color
    val label: String
    when {
        !available -> {
            dotColor = Color(0xFF9E9E9E)
            label = "Not on ${cloudLabel(cloud)}"
        }
        availability == null -> {
            dotColor = Color(0xFFFFC107)
            label = "Availability unknown"
        }
        availability == AvailabilityLevel.NONE -> {
            dotColor = Color(0xFFEF5350)
            label = "No stock"
        }
        availability == AvailabilityLevel.LOW -> {
            dotColor = Color(0xFFFFA726)
            label = "Limited stock"
        }
        else -> {
            dotColor = Color(0xFF66BB6A)
            label = "Available"
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun filteredGpus(
    all: List<GpuType>,
    cloud: Cloud,
    query: String,
    memory: CreatePodViewModel.GpuMemoryFilter,
    inStockOnly: Boolean,
    sort: CreatePodViewModel.GpuSort,
    restrictDc: String? = null,
): List<GpuType> {
    var list = all
    if (restrictDc != null) {
        list = list.filter { gpu ->
            gpu.dataCenters?.any { it.id == restrictDc } ?: true
        }
    }
    val q = query.trim().lowercase()
    if (q.isNotEmpty()) {
        list = list.filter { it.name.lowercase().contains(q) || it.id.lowercase().contains(q) }
    }
    list = when (memory) {
        CreatePodViewModel.GpuMemoryFilter.ANY -> list
        CreatePodViewModel.GpuMemoryFilter.LE_24 -> list.filter { it.memory in 1..24 }
        CreatePodViewModel.GpuMemoryFilter.B_24_48 -> list.filter { it.memory in 25..48 }
        CreatePodViewModel.GpuMemoryFilter.B_48_96 -> list.filter { it.memory in 49..96 }
        CreatePodViewModel.GpuMemoryFilter.GT_96 -> list.filter { it.memory > 96 }
    }
    if (inStockOnly) {
        list = list.filter {
            it.availableOn(cloud) &&
                (it.availability == null || it.availability != AvailabilityLevel.NONE)
        }
    }
    val sorted = when (sort) {
        CreatePodViewModel.GpuSort.PRICE -> list.sortedBy { gpuPrice(it, cloud) }
        CreatePodViewModel.GpuSort.NAME -> list.sortedBy { it.name.lowercase() }
        CreatePodViewModel.GpuSort.MEMORY -> list.sortedByDescending { it.memory }
    }
    // Stable partition: GPUs not offered on the selected cloud sink to the bottom,
    // preserving the chosen sort order within each group.
    return sorted.sortedBy { !it.availableOn(cloud) }
}

private fun gpuPrice(gpu: GpuType, cloud: Cloud): Double =
    if (cloud == Cloud.SECURE) gpu.price.secure else gpu.price.community

private fun GpuType.availableOn(cloud: Cloud): Boolean =
    if (cloud == Cloud.SECURE) secure else community

private fun cloudLabel(cloud: Cloud): String =
    if (cloud == Cloud.SECURE) "Secure" else "Community"

private fun storageLabel(mode: CreatePodViewModel.DataStorage): String = when (mode) {
    CreatePodViewModel.DataStorage.NONE -> "None"
    CreatePodViewModel.DataStorage.VOLUME_DISK -> "Volume disk"
    CreatePodViewModel.DataStorage.NETWORK -> "Network volume"
}

private fun volumeLabel(v: NetworkVolume): String = buildString {
    append(v.name)
    v.size?.let { append(" · $it GB") }
    v.dataCenter?.let { append(" · $it") }
}

private fun volumeMeta(v: NetworkVolume, path: String): String = buildString {
    append(v.size ?: 0)
    append(" GB")
    v.dataCenter?.let { append(" · $it") }
    v.type?.let {
        append(" · ")
        append(if (it == VolumeType.HIGH_PERFORMANCE) "High performance" else "Standard")
    }
    append(" · mounted at $path")
}
