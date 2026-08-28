package com.canni.runpod.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canni.runpod.data.api.dto.Template
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(
    templateId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCreatePod: () -> Unit,
    onCreateEndpoint: () -> Unit,
    viewModel: TemplateDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(templateId) {
        viewModel.load()
    }

    LaunchedEffect(state.templateGone) {
        if (state.templateGone) {
            onBack()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { scope.launch { snackbarHostState.showSnackbar(it) } }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.template?.name?.ifBlank { null } ?: "Template") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = viewModel::showDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null && state.template == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }

            else -> state.template?.let { template ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "type") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TypeChip(if (template.serverless == true) "Serverless" else "Pod")
                            template.category?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.width(6.dp))
                                TypeChip(it)
                            }
                            if (template.public == true) {
                                Spacer(Modifier.width(6.dp))
                                TypeChip("Public")
                            }
                        }
                    }

                    if (template.serverless == true) {
                        item(key = "create_endpoint") {
                            Button(
                                onClick = onCreateEndpoint,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Create endpoint from this template")
                            }
                        }
                    } else {
                        item(key = "create_pod") {
                            Button(
                                onClick = onCreatePod,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Create pod from this template")
                            }
                        }
                    }

                    item(key = "container") {
                        SectionCard("Container") {
                            InfoRow(
                                label = "Image",
                                value = template.image,
                                monospace = true,
                            )
                            template.args?.takeIf { it.isNotBlank() }?.let {
                                InfoRow(label = "Startup command", value = it, monospace = true)
                            }
                            template.disk?.let {
                                InfoRow(label = "Disk", value = "${it} GB")
                            }
                        }
                    }

                    item(key = "volume") {
                        val persistent = template.mounts?.persistent
                        val network = template.mounts?.network
                        if (persistent != null || !network.isNullOrEmpty()) {
                            SectionCard("Volume") {
                                persistent?.let {
                                    InfoRow(label = "Persistent size", value = "${it.size ?: "?"} GB")
                                    InfoRow(label = "Mount path", value = it.path ?: "", monospace = true)
                                }
                                network?.forEach { mount ->
                                    InfoRow(label = "Network volume", value = mount.volumeId ?: "")
                                    InfoRow(label = "Mount path", value = mount.path ?: "", monospace = true)
                                }
                            }
                        }
                    }

                    item(key = "ports") {
                        val ports = template.ports
                        if (!ports.isNullOrEmpty()) {
                            SectionCard("Ports") {
                                ports.forEach { port ->
                                    InfoRow(label = "", value = port, monospace = true)
                                }
                            }
                        }
                    }

                    item(key = "env") {
                        val env = template.env
                        if (!env.isNullOrEmpty()) {
                            SectionCard("Environment variables (${env.size})") {
                                env.forEach { (key, value) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = value.ifBlank { "(empty)" },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "flags") {
                        SectionCard("Behavior") {
                            InfoRow(label = "Start SSH", value = boolText(template.startSsh))
                            InfoRow(label = "Start Jupyter", value = boolText(template.startJupyter))
                            val cuda = template.allowedCudaVersions
                            InfoRow(
                                label = "Allowed CUDA",
                                value = if (cuda.isNullOrEmpty()) "Any version" else cuda.joinToString(", "),
                            )
                            InfoRow(label = "Template ID", value = template.id, monospace = true)
                        }
                    }
                }
            }
        }
    }

    if (state.confirmDelete) {
        val template = state.template
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Delete template?") },
            text = {
                Text(
                    "\"${template?.name ?: templateId}\" will be permanently deleted. " +
                        "Pods and endpoints already created from it are not affected.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    enabled = !state.deleteBusy,
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete, enabled = !state.deleteBusy) {
                    Text("Cancel")
                }
            },
        )
    }
}

private fun boolText(value: Boolean?): String = when (value) {
    true -> "On"
    false -> "Off"
    null -> "Default"
}

@Composable
private fun TypeChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    monospace: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .width(120.dp)
                    .padding(end = 8.dp),
            )
        }
        Text(
            text = value,
            style = if (monospace) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(1f),
        )
    }
}
