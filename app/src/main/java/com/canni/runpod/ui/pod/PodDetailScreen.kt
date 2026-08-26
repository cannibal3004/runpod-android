package com.canni.runpod.ui.pod

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canni.runpod.data.api.dto.Pod
import com.canni.runpod.data.api.dto.PodAction
import com.canni.runpod.ui.common.formatCostPerHour
import com.canni.runpod.ui.common.formatUtc
import com.canni.runpod.ui.common.formatUptime
import com.canni.runpod.ui.components.StatusChip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodDetailScreen(
    podId: String,
    onBack: () -> Unit,
    onOpenLogs: () -> Unit,
    viewModel: PodDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmAction by remember { mutableStateOf<PodAction?>(null) }

    LaunchedEffect(podId) {
        viewModel.load()
        launch {
            while (true) {
                delay(PodDetailViewModel.REFRESH_MS)
                viewModel.load(silent = true)
            }
        }
    }

    LaunchedEffect(state.podGone) {
        if (state.podGone) {
            delay(2_000)
            onBack()
        }
    }

    val pod = state.pod

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pod?.name ?: "Pod") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onOpenLogs) { Text("Logs") }
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

            state.podGone -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Pod terminated",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "It has been removed from your pods.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onBack) { Text("Back to pods") }
                }
            }

            state.error != null && pod == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.error ?: "Failed to load pod",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onBack) { Text("Go back") }
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pod?.let {
                    item(key = "status") { StatusRow(it) }
                    item(key = "actions") {
                        ActionRow(
                            pod = it,
                            busyAction = state.busyAction,
                            onAction = { confirmAction = it },
                        )
                    }
                    state.actionError?.let { msg ->
                        item(key = "action_error") {
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    it.runtime?.let { rt ->
                        if (rt.gpus?.isNotEmpty() == true || rt.cpu != null || rt.memory != null) {
                            item(key = "metrics") { MetricsCard(rt) }
                        }
                    }
                    item(key = "spec") { SpecCard(it) }
                    it.runtime?.ports?.takeIf { p -> p.isNotEmpty() }?.let { ports ->
                        item(key = "ports") { PortsCard(ports) }
                    }
                    it.ssh?.let { ssh ->
                        if (ssh.proxy?.command != null || ssh.direct?.command != null) {
                            item(key = "ssh") { SshCard(ssh) }
                        }
                    }
                    it.globalNetworking?.takeIf { g -> g.enabled == true }?.let { g ->
                        item(key = "global_net") {
                            SectionCard("Global networking") {
                                KvRow("IP", g.ip ?: "—")
                                KvRow("DNS", g.internalDns ?: "—")
                            }
                        }
                    }
                    it.env?.takeIf { e -> e.isNotEmpty() }?.let { env ->
                        item(key = "env") {
                            SectionCard("Environment variables") {
                                env.forEach { (k, v) -> KvRow(k, v) }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Confirm ${action.name}") },
            text = {
                Text(
                    when (action) {
                        PodAction.terminate ->
                            "This permanently terminates the pod. Host-local storage is destroyed. This cannot be undone."
                        PodAction.stop -> "The pod will be stopped. You can start it again later."
                        PodAction.restart -> "The container will be restarted."
                        PodAction.start -> "Start this pod?"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmAction = null
                        scope.launch { viewModel.act(action) }
                    },
                ) {
                    Text(
                        text = action.name,
                        color = if (action == PodAction.terminate) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StatusRow(pod: Pod) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(pod.status)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = formatCostPerHour(pod.cost),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
            val lines = buildList {
                pod.dataCenterId?.let { add(it) }
                pod.cloud?.let { add(if (it.name == "SECURE") "Secure cloud" else "Community cloud") }
                pod.cudaVersion?.let { add("CUDA $it") }
            }
            if (lines.isNotEmpty()) {
                Text(
                    text = lines.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (pod.locked == true) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Locked — stopping or resetting is disabled",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    pod: Pod,
    busyAction: PodAction?,
    onAction: (PodAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val busy = busyAction != null
        if (PodAction.start in pod.actions) {
            Button(
                onClick = { onAction(PodAction.start) },
                enabled = !busy,
                modifier = Modifier.weight(1f),
            ) {
                ActionLabel(PodAction.start, busyAction)
            }
        }
        if (PodAction.stop in pod.actions) {
            OutlinedButton(
                onClick = { onAction(PodAction.stop) },
                enabled = !busy,
                modifier = Modifier.weight(1f),
            ) {
                ActionLabel(PodAction.stop, busyAction)
            }
        }
        if (PodAction.restart in pod.actions) {
            OutlinedButton(
                onClick = { onAction(PodAction.restart) },
                enabled = !busy,
                modifier = Modifier.weight(1f),
            ) {
                ActionLabel(PodAction.restart, busyAction)
            }
        }
        if (PodAction.terminate in pod.actions) {
            Button(
                onClick = { onAction(PodAction.terminate) },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1.4f),
            ) {
                ActionLabel(PodAction.terminate, busyAction)
            }
        }
    }
}

@Composable
private fun ActionLabel(action: PodAction, busyAction: PodAction?) {
    if (busyAction == action) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(action.name)
        }
    } else {
        Text(action.name)
    }
}

@Composable
private fun MetricsCard(rt: com.canni.runpod.data.api.dto.PodRuntime) {
    SectionCard("Live metrics") {
        rt.cpu?.util?.let { KvRow("CPU", "$it%") }
        rt.memory?.util?.let { KvRow("Memory", "$it%") }
        rt.gpus?.forEachIndexed { i, g ->
            val label = if ((rt.gpus?.size ?: 0) > 1) "GPU ${i + 1}" else "GPU"
            KvRow(label, "${g.util ?: 0}%  ·  mem ${g.memoryUtil ?: 0}%")
        }
        KvRow("Uptime", formatUptime(rt.uptime))
    }
}

@Composable
private fun SpecCard(pod: Pod) {
    SectionCard("Details") {
        KvRow("Image", pod.image)
        if (!pod.args.isNullOrBlank()) KvRow("Args", pod.args)
        pod.disk?.let { KvRow("Disk", "${it} GB (ephemeral)") }
        pod.gpu?.id?.let { KvRow("GPU", "${pod.gpu?.count ?: 1}× $it") }
        pod.cpu?.let { KvRow("CPU", "${it.vcpuCount ?: "?"} vCPU · ${it.memory ?: "?"} GB RAM") }
        pod.mounts?.persistent?.let { KvRow("Storage", "Persistent ${it.size ?: "?"} GB @ ${it.path ?: "?"}") }
        pod.mounts?.network?.firstOrNull()?.let { KvRow("Storage", "Volume ${it.volumeId ?: "?"} @ ${it.path ?: "?"}") }
        if (!pod.ports.isNullOrEmpty()) KvRow("Ports", pod.ports?.joinToString(", ").orEmpty())
        KvRow("Created", formatUtc(pod.createdAt))
        KvRow("Started", formatUtc(pod.startedAt))
        pod.template?.let { KvRow("Template", it) }
    }
}

@Composable
private fun PortsCard(ports: List<com.canni.runpod.data.api.dto.PodRuntimePort>) {
    SectionCard("Port mappings") {
        ports.forEach { p ->
            val mapping = when {
                p.publicPort != null -> "${p.privatePort ?: "?"} → ${p.publicPort}"
                else -> "${p.privatePort ?: "?"}"
            }
            KvRow(
                label = "${mapping}${p.type?.let { " ($it" } ?: ""}${p.type?.let { ")" } ?: ""}",
                value = p.ip ?: "no public IP",
            )
        }
    }
}

@Composable
private fun SshCard(ssh: com.canni.runpod.data.api.dto.PodSsh) {
    val clipboard = LocalClipboardManager.current
    SectionCard("SSH") {
        ssh.proxy?.command?.let { cmd ->
            SshCommandRow("Proxy", cmd) { clipboard.setText(AnnotatedString(cmd)) }
        }
        ssh.direct?.command?.let { cmd ->
            SshCommandRow("Direct", cmd) { clipboard.setText(AnnotatedString(cmd)) }
        }
    }
}

@Composable
private fun SshCommandRow(label: String, command: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = command,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onCopy) {
            Text("Copy", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun KvRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
