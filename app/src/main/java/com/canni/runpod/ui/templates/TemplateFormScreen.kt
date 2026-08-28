package com.canni.runpod.ui.templates

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
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
import com.canni.runpod.ui.components.DropdownField
import com.canni.runpod.ui.components.DropdownOption
import com.canni.runpod.ui.components.EnvEntry
import com.canni.runpod.ui.components.EnvVarEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TemplateFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEdit) "Edit template" else "New template") },
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
                        onClick = { viewModel.submit(onSaved) },
                        enabled = !state.isSubmitting && !state.isLoading,
                    ) {
                        Text(if (viewModel.isEdit) "Save" else "Create")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

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

            if (!viewModel.isEdit) {
                item(key = "type") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = !state.isServerless,
                                onClick = { viewModel.setServerless(false) },
                                label = { Text("Pod") },
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = state.isServerless,
                                onClick = { viewModel.setServerless(true) },
                                label = { Text("Serverless") },
                            )
                        }
                        Text(
                            text = if (state.isServerless) {
                                "Reusable setup for serverless endpoint workers."
                            } else {
                                "Reusable setup for on-demand and persistent pods."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item(key = "category") {
                DropdownField(
                    label = "GPU category",
                    selected = DropdownOption(state.category, state.category),
                    options = listOf(
                        DropdownOption("NVIDIA", "NVIDIA"),
                        DropdownOption("CPU", "CPU"),
                        DropdownOption("AMD", "AMD"),
                    ),
                    onSelect = { viewModel.setCategory(it.value) },
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
                    }
                }
            }

            item(key = "volume") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Persistent volume",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = state.hasVolume,
                                onCheckedChange = viewModel::setHasVolume,
                            )
                        }
                        if (state.hasVolume) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = state.volumeSize,
                                    onValueChange = viewModel::onVolumeSizeChange,
                                    label = { Text("Size (GB)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = state.volumePath,
                                    onValueChange = viewModel::onVolumePathChange,
                                    label = { Text("Mount path") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.5f),
                                )
                            }
                        }
                    }
                }
            }

            item(key = "ports") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Ports",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    }
                }
            }

            item(key = "env") {
                OutlinedButton(
                    onClick = viewModel::openEnvEditor,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Environment variables (${state.envEntries.size})")
                }
            }

            item(key = "visibility") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Public template",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = "Visible to other RunPod users in the template catalog.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = state.isPublic,
                                onCheckedChange = viewModel::setPublic,
                            )
                        }
                    }
                }
            }

            item(key = "behavior") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Behavior",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Start SSH",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = "Inject PUBLIC_KEY env for SSH access at startup.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = state.startSsh,
                                onCheckedChange = viewModel::setStartSsh,
                                enabled = !viewModel.isEdit,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Start Jupyter",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = "Inject JUPYTER_PASSWORD env at startup.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = state.startJupyter,
                                onCheckedChange = viewModel::setStartJupyter,
                                enabled = !viewModel.isEdit,
                            )
                        }
                        OutlinedTextField(
                            value = state.allowedCuda,
                            onValueChange = viewModel::onAllowedCudaChange,
                            label = { Text("Allowed CUDA versions (comma-separated, e.g. 12.6, 12.8)") },
                            placeholder = { Text("Any version") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
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
