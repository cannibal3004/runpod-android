package com.canni.runpod.ui.storage

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canni.runpod.data.api.dto.NetworkVolume
import com.canni.runpod.data.api.dto.VolumeType
import com.canni.runpod.ui.components.DropdownField
import com.canni.runpod.ui.components.DropdownOption
import com.canni.runpod.ui.nav.MainScaffold
import com.canni.runpod.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    onNavigateTopLevel: (String) -> Unit,
    viewModel: StorageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.load()
    }

    if (state.showCreateForm) {
        CreateVolumeFormScreen(state = state, viewModel = viewModel)
        return
    }

    MainScaffold(
        title = "Storage",
        currentRoute = Routes.STORAGE,
        onNavigate = onNavigateTopLevel,
        snackbar = state.message,
        onSnackbarShown = viewModel::onSnackbarShown,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openCreateForm,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New volume") },
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.loadError != null) {
                    item(key = "error") {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Failed to load volumes: ${state.loadError}",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                if (state.volumes.isEmpty()) {
                    item(key = "empty") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 96.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "No network volumes",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Create one to attach to your pods.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                items(state.volumes, key = { it.id }) { volume ->
                    VolumeCard(
                        volume = volume,
                        onRename = { viewModel.showRename(volume) },
                        onResize = { viewModel.showResize(volume) },
                        onDelete = { viewModel.showDelete(volume) },
                    )
                }
            }
        }
    }

    state.renameVolume?.let { volume ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRename,
            title = { Text("Rename volume") },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.renameValue,
                        onValueChange = viewModel::onRenameChange,
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.dialogError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.dialogError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRename, enabled = !state.busy) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRename, enabled = !state.busy) {
                    Text("Cancel")
                }
            },
        )
    }

    state.resizeVolume?.let { volume ->
        AlertDialog(
            onDismissRequest = viewModel::dismissResize,
            title = { Text("Resize volume") },
            text = {
                Column {
                    Text(
                        text = "Currently ${(volume.size ?: 0)} GB. Storage can only grow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.resizeValue,
                        onValueChange = viewModel::onResizeChange,
                        label = { Text("New size (GB)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.dialogError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.dialogError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmResize, enabled = !state.busy) {
                    Text("Resize")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissResize, enabled = !state.busy) {
                    Text("Cancel")
                }
            },
        )
    }

    state.deleteVolume?.let { volume ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Delete volume?") },
            text = {
                Text(
                    text = "\"${volume.name}\" (${volume.size ?: 0} GB) will be permanently deleted. Pods using it will lose that storage.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete, enabled = !state.busy) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete, enabled = !state.busy) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun VolumeCard(
    volume: NetworkVolume,
    onRename: () -> Unit,
    onResize: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = volume.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${volume.size ?: "?"} GB · ${volume.dataCenter ?: "?"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            TypeChip(volume.type)
            Spacer(Modifier.width(4.dp))
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Actions for ${volume.name}")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Resize") },
                        onClick = {
                            menuExpanded = false
                            onResize()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeChip(type: VolumeType?) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when (type) {
            VolumeType.HIGH_PERFORMANCE -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            text = typeLabel(type),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun typeLabel(type: VolumeType?): String = when (type) {
    VolumeType.STANDARD -> "Standard"
    VolumeType.HIGH_PERFORMANCE -> "High performance"
    null -> "Volume"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateVolumeFormScreen(
    state: StorageViewModel.UiState,
    viewModel: StorageViewModel,
) {
    val volumeDcs = state.dataCenters.filter { it.networkVolumeTypes.orEmpty().isNotEmpty() }
    val selectedDc = volumeDcs.find { it.id == state.createDcId }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New volume") },
                navigationIcon = {
                    IconButton(onClick = viewModel::closeCreateForm) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = state.createName,
                onValueChange = viewModel::onCreateNameChange,
                label = { Text("Name") },
                placeholder = { Text("my-dataset") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.createSize,
                onValueChange = viewModel::onCreateSizeChange,
                label = { Text("Size (GB)") },
                placeholder = { Text("100") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            DropdownField(
                label = "Data center",
                selected = state.createDcId?.let { id ->
                    volumeDcs.find { it.id == id }?.let { DropdownOption(it.id, "${it.id} — ${it.name}") }
                },
                options = volumeDcs.map { DropdownOption(it.id, "${it.id} — ${it.name}") },
                onSelect = { viewModel.onCreateDcChange(it.value) },
                modifier = Modifier.fillMaxWidth(),
            )
            val supportedTypes = selectedDc?.networkVolumeTypes.orEmpty()
            if (supportedTypes.size > 1) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Storage tier",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    supportedTypes.forEach { type ->
                        FilterChip(
                            selected = state.createType == type,
                            onClick = { viewModel.onCreateTypeChange(type) },
                            label = { Text(typeLabel(type)) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "10–4096 GB. The full size is billed while the volume exists, and it can only be grown later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.createError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.createError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::createVolume,
                enabled = !state.isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (state.isCreating) "Creating…" else "Create volume")
            }
        }
    }
}
