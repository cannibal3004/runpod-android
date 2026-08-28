package com.canni.runpod.ui.serverless

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import com.canni.runpod.data.api.dto.EndpointWorker
import com.canni.runpod.data.api.dto.Release
import com.canni.runpod.data.api.dto.ServerlessEndpoint
import com.canni.runpod.ui.common.formatUptime
import com.canni.runpod.ui.common.formatUtc
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REFRESH_MS = 15_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerlessDetailScreen(
    endpointId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenWorkerLogs: (String) -> Unit,
    viewModel: ServerlessDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(endpointId) {
        viewModel.load()
        launch {
            while (true) {
                delay(REFRESH_MS)
                viewModel.load(silent = true)
            }
        }
    }

    LaunchedEffect(state.endpointGone) {
        if (state.endpointGone) {
            delay(2_000)
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
                title = { Text(state.endpoint?.name?.ifBlank { null } ?: "Endpoint") },
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
            state.isLoading -> androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null && state.endpoint == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { viewModel.load() }) { Text("Retry") }
            }

            else -> state.endpoint?.let { endpoint ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.error != null) {
                        item(key = "error_banner") {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Refresh failed: ${state.error}",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    item(key = "urls") {
                        UrlCard(
                            endpoint = endpoint,
                            onCopy = { url -> clipboard.setText(AnnotatedString(url)) },
                            onOpen = { url ->
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            },
                        )
                    }
                    item(key = "workers") {
                        WorkersCard(
                            workers = state.workers,
                            onOpenLogs = onOpenWorkerLogs,
                        )
                    }
                    item(key = "config") {
                        ConfigCard(endpoint = endpoint)
                    }
                    item(key = "releases") {
                        ReleasesCard(releases = state.releases)
                    }
                }
            }
        }
    }

    if (state.confirmDelete) {
        val endpoint = state.endpoint
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Delete endpoint?") },
            text = {
                Text(
                    "\"${endpoint?.name ?: endpointId}\" will be permanently deleted. " +
                        "Any running workers are stopped and the public URL stops resolving.",
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

@Composable
private fun UrlCard(
    endpoint: ServerlessEndpoint,
    onCopy: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    val urls = endpoint.requestUrls
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Public URL",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (urls == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No request URLs reported.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                val entries = buildList {
                    urls.base?.let { add("Base" to it) }
                    urls.run?.let { add("Run (async)" to it) }
                    urls.runSync?.let { add("Run (sync)" to it) }
                    urls.health?.let { add("Health" to it) }
                }
                entries.forEach { (label, url) ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onCopy(url) }) {
                            Text(
                                text = "Copy",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = { onOpen(url) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open $label",
                            )
                        }
                    }
                }
                if (entries.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No request URLs reported.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkersCard(
    workers: com.canni.runpod.data.api.dto.ListEndpointWorkersResponse?,
    onOpenLogs: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Workers",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val summary = workers?.summary
            if (summary != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${summary.total} total  ·  ${summary.running} running  ·  " +
                        "${summary.idle} idle  ·  ${summary.initializing} starting  ·  " +
                        "${summary.throttled} throttled  ·  ${summary.unhealthy} unhealthy",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val list = workers?.workers.orEmpty()
            if (list.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No workers allocated. Requests scale workers up on demand.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                list.forEach { worker ->
                    Spacer(Modifier.height(10.dp))
                    WorkerRow(worker = worker, onClick = { onOpenLogs(worker.id) })
                }
            }
        }
    }
}

@Composable
private fun WorkerRow(
    worker: EndpointWorker,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkerStatusChip(status = worker.status)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            val stale = worker.isStale == true
            Text(
                text = buildString {
                    append(worker.gpuTypeId ?: "CPU")
                    append("  ·  ${worker.id}")
                    if (stale) append("  ·  stale config")
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val details = buildString {
                worker.dataCenterId?.let { append(it) }
                worker.uptimeSeconds?.let {
                    if (isNotEmpty()) append("  ·  ")
                    append("up ${formatUptime(it.toLong())}")
                }
            }
            if (details.isNotBlank()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = "logs",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun WorkerStatusChip(status: String?) {
    val color = when (status?.uppercase()) {
        "RUNNING" -> Color(0xFF4CAF50)
        "IDLE" -> MaterialTheme.colorScheme.primary
        "INITIALIZING" -> Color(0xFFFFC107)
        "THROTTLED" -> Color(0xFFFF9800)
        "UNHEALTHY" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = status ?: "UNKNOWN",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun ConfigCard(endpoint: ServerlessEndpoint) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            val workers = endpoint.workers
            val rows = buildList {
                endpoint.image?.takeIf { it.isNotBlank() }?.let { add("Image" to it) }
                endpoint.args?.takeIf { it.isNotBlank() }?.let { add("Args" to it) }
                endpoint.disk?.let { add("Disk" to "$it GB") }
                endpoint.env?.takeIf { it.isNotEmpty() }?.let { add("Environment" to "${it.size} variables") }
                endpoint.ports?.takeIf { it.isNotEmpty() }?.let { add("Ports" to it.joinToString(", ")) }
                add("Workers" to "${workers?.min ?: 0}–${workers?.max ?: 0}" +
                    (workers?.idleTimeout?.let { " (idle timeout ${it}s)" } ?: ""))
                endpoint.scalingLabel?.let { add("Scaling" to it) }
                endpoint.timeout?.let { add("Timeout" to "${it} ms") }
                endpoint.dataCenterIds?.takeIf { it.isNotEmpty() }?.let { add("Data centers" to it.joinToString(", ")) }
                endpoint.flashboot?.takeIf { it != "OFF" }?.let { add("FlashBoot" to it) }
                endpoint.networkVolumes?.takeIf { it.isNotEmpty() }?.let { add("Volumes" to it.joinToString(", ")) }
                endpoint.createdAt?.let { add("Created" to formatUtc(it)) }
            }
            rows.forEach { (label, value) ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(110.dp),
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleasesCard(
    releases: com.canni.runpod.data.api.dto.ListEndpointReleasesResponse?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Releases",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val rollout = releases?.rollout
            if (rollout != null && rollout.inProgress == true) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Rollout in progress: ${rollout.workersOnLatest ?: 0}/${rollout.workersTotal ?: 0} workers on latest",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            val list = releases?.releases.orEmpty()
            if (list.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No releases recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                list.take(5).forEach { release ->
                    Spacer(Modifier.height(10.dp))
                    ReleaseRow(release = release)
                }
            }
        }
    }
}

@Composable
private fun ReleaseRow(release: Release) {
    val header = buildString {
        append("v${release.version ?: "?"}")
        append("  ·  ${release.source ?: "?"}")
        append("  ·  ${release.workerCount ?: 0} workers")
        release.createdAt?.let { append("  ·  ${formatUtc(it)}") }
    }
    Text(
        text = header,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    release.diff.take(4).forEach { diff ->
        val old = diff.oldText
        val new = diff.newText
        if (old.isBlank() && new.isBlank()) return@forEach
        Text(
            text = "  ${diff.field ?: "?"}: $old → $new",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (release.diff.size > 4) {
        Text(
            text = "  +${release.diff.size - 4} more changes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
