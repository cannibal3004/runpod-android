package com.canni.runpod.ui.pods

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canni.runpod.data.api.dto.Cloud
import com.canni.runpod.data.api.dto.Pod
import com.canni.runpod.data.api.dto.PodStatus
import com.canni.runpod.ui.common.formatCostPerHour
import com.canni.runpod.ui.common.formatUptime
import com.canni.runpod.ui.components.StatusChip
import com.canni.runpod.ui.nav.MainScaffold
import com.canni.runpod.ui.nav.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodsScreen(
    onPodClick: (String) -> Unit,
    onCreate: () -> Unit,
    onNavigateTopLevel: (String) -> Unit,
    viewModel: PodsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.load(initial = true)
        viewModel.autoRefreshLoop()
    }

    MainScaffold(
        title = "Pods",
        currentRoute = Routes.PODS,
        onNavigate = onNavigateTopLevel,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New pod") },
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

            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { scope.launch { viewModel.load() } },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    state.error != null && state.pods.isEmpty() -> ErrorContent(
                        message = state.error!!,
                        modifier = Modifier.padding(padding),
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.error != null && state.pods.isNotEmpty()) {
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
                        if (state.pods.isEmpty() && state.error == null) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 96.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = "No pods yet",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "Create one from the RunPod console and it will show up here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        items(state.pods, key = { it.id }) { pod ->
                            PodCard(pod = pod, onClick = { onPodClick(pod.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Check your API key and pull to refresh.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PodCard(
    pod: Pod,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pod.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(pod.status)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = pod.summaryLine(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (pod.image.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = pod.image,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatCostPerHour(pod.cost),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(16.dp))
                val runtime = pod.runtime
                if (pod.status == PodStatus.RUNNING && runtime != null) {
                    Text(
                        text = "CPU ${runtime.cpu?.util ?: 0}%  ·  GPU ${runtime.gpus?.firstOrNull()?.util ?: 0}%  ·  up ${formatUptime(runtime.uptime)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun Pod.summaryLine(): String {
    val parts = mutableListOf<String>()
    val gpu = gpu
    if (gpu?.id != null) {
        parts.add("${gpu.count ?: 1}× ${gpu.id}")
    } else if (cpu != null) {
        parts.add("CPU · ${cpu.vcpuCount ?: "?"} vCPU · ${cpu.memory ?: "?"} GB")
    }
    dataCenterId?.let { parts.add(it) }
    if (cloud == Cloud.COMMUNITY) parts.add("Community")
    return parts.joinToString("  ·  ")
}
