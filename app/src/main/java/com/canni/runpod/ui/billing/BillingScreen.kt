package com.canni.runpod.ui.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canni.runpod.data.api.dto.AccountBalance
import com.canni.runpod.data.api.dto.BillingAmounts
import com.canni.runpod.data.api.dto.BillingRecord
import com.canni.runpod.ui.common.formatBucketLabel
import com.canni.runpod.ui.nav.MainScaffold
import com.canni.runpod.ui.nav.Routes
import com.canni.runpod.ui.pod.SectionCard
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    onNavigateTopLevel: (String) -> Unit,
    viewModel: BillingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MainScaffold(
        title = "Billing",
        currentRoute = Routes.BILLING,
        onNavigate = onNavigateTopLevel,
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "ranges") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        BillingViewModel.Range.entries.forEach { range ->
                            FilterChip(
                                selected = state.range == range,
                                onClick = { viewModel.setRange(range) },
                                label = { Text(range.label) },
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }

                item(key = "balance") {
                    BalanceCard(state.balance, state.balanceError)
                }

                if (state.error != null) {
                    item(key = "error") {
                        Text(
                            text = state.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                item(key = "summary") {
                    SummaryCard(state.totals, state.range)
                }

                if (state.records.isNotEmpty()) {
                    item(key = "chart") {
                        SpendChart(
                            records = state.records,
                            bucketSize = state.range.bucketSize,
                        )
                    }
                }

                item(key = "pods") {
                    PodSpendCard(
                        podSpends = state.podSpends,
                        total = state.podTotal,
                        range = state.range,
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: AccountBalance?, error: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = balance?.clientBalance?.let { "$%.2f USD".format(it) } ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            when {
                error != null -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Couldn't load balance: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                balance != null &&
                    (balance.currentSpendPerHr != null || balance.spendLimit != null) -> {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth()) {
                        BalanceStat(
                            label = "Spend / hr",
                            value = balance.currentSpendPerHr?.let { "$%.2f".format(it) } ?: "—",
                            modifier = Modifier.weight(1f),
                        )
                        BalanceStat(
                            label = "Spend limit",
                            value = balance.spendLimit?.let { "$%.0f /hr".format(it) } ?: "—",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryCard(totals: BillingAmounts?, range: BillingViewModel.Range) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Spend — last ${range.label}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = totals?.let { "$%.2f USD".format(it.totalAmount) } ?: "—",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            totals?.let { t ->
                Spacer(Modifier.height(12.dp))
                breakdownRow("Pods", t.podGpuAmount + t.podCpuAmount + t.podDiskAmount)
                breakdownRow("Serverless", t.serverlessGpuAmount + t.serverlessCpuAmount + t.serverlessDiskAmount + t.serverlessFeeAmount)
                breakdownRow("Storage", t.storageStandardAmount + t.storageHighPerformanceAmount)
                breakdownRow("Clusters", t.clusterGpuAmount + t.clusterDiskAmount + t.clusterNetworkingAmount)
                breakdownRow("Endpoints", t.endpointAmount)
            }
        }
    }
}

@Composable
private fun breakdownRow(label: String, amount: Double) {
    if (amount <= 0.0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$%.2f USD".format(amount),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SpendChart(
    records: List<BillingRecord>,
    bucketSize: String,
) {
    SectionCard(title = "Spend over time") {
        val maxAmount = max(records.maxOfOrNull { it.totalAmount } ?: 0.0, 0.0001)
        records.forEach { record ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatBucketLabel(record.startTime, bucketSize),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(64.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((record.totalAmount / maxAmount).toFloat().coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$%.2f".format(record.totalAmount),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(52.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun PodSpendCard(
    podSpends: List<BillingViewModel.PodSpend>,
    total: Double,
    range: BillingViewModel.Range,
) {
    var showAll by remember { mutableStateOf(false) }
    SectionCard(title = "By pod — last ${range.label}") {
        if (podSpends.isEmpty()) {
            Text(
                text = "No pod spend in this range.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$%.2f USD".format(total),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            val maxSpend = max(podSpends.maxOf { it.total }, 0.0001)
            val visible = if (showAll) podSpends else podSpends.take(MAX_POD_ROWS)
            visible.forEach { spend ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = spend.podId,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "$%.2f USD".format(spend.total),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((spend.total / maxSpend).toFloat().coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        )
                    }
                }
            }
            if (podSpends.size > MAX_POD_ROWS) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { showAll = !showAll }) {
                    Text(if (showAll) "Show less" else "Show all ${podSpends.size} pods")
                }
            }
        }
    }
}

private const val MAX_POD_ROWS = 8
