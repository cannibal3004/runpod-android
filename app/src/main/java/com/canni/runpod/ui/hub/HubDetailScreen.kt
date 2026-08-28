package com.canni.runpod.ui.hub

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubDetailScreen(
    listingId: String,
    onBack: () -> Unit,
    onCreateEndpoint: (String) -> Unit,
    viewModel: HubDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listing = state.listing
    val release = listing?.listedRelease

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listing?.title ?: "Hub repo", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading && listing == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null && listing == null -> Column(
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
                )
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                item(key = "header") {
                    Column {
                        val repo = listOfNotNull(listing?.repoOwner, listing?.repoName)
                            .filter { it.isNotBlank() }
                            .joinToString("/")
                        if (repo.isNotBlank()) {
                            Text(
                                text = repo,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        listing?.description?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            listing?.deploys?.let {
                                Text(
                                    text = "$it deploys",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            listing?.stars?.let {
                                Text(
                                    text = "$it stars",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            listing?.category?.takeIf { it.isNotBlank() }?.let {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(50),
                                ) {
                                    Text(
                                        text = it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                if (release != null) {
                    item(key = "release") {
                        Card {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = "Latest release",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Spacer(Modifier.height(8.dp))
                                release.name?.takeIf { it.isNotBlank() }?.let {
                                    Text(text = it, style = MaterialTheme.typography.titleMedium)
                                }
                                val meta = buildString {
                                    release.tagName?.takeIf { it.isNotBlank() }?.let { append("tag $it") }
                                    release.branch?.takeIf { it.isNotBlank() }?.let {
                                        if (isNotEmpty()) append("  ·  ")
                                        append("branch $it")
                                    }
                                    release.license?.takeIf { it.isNotBlank() }?.let {
                                        if (isNotEmpty()) append("  ·  ")
                                        append(it)
                                    }
                                    release.deploys?.let {
                                        if (isNotEmpty()) append("  ·  ")
                                        append("$it deploys")
                                    }
                                }
                                if (meta.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                release.build?.imageName?.takeIf { it.isNotBlank() }?.let {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                release.build?.let { build ->
                                    build.imageName?.takeIf { it.isNotBlank() }?.let { imageName ->
                                        Spacer(Modifier.height(8.dp))
                                        Button(onClick = { onCreateEndpoint(listingId) }) {
                                            Text("Create endpoint")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.envPreview.isNotEmpty()) {
                    item(key = "env") {
                        Card {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = "Environment variables",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Spacer(Modifier.height(8.dp))
                                state.envPreview.forEach { env ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = env.key,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                if (env.advanced) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                                        shape = RoundedCornerShape(50),
                                                    ) {
                                                        Text(
                                                            text = "advanced",
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${env.name} (${env.type})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            if (env.defaultValue.isNotBlank()) {
                                                Text(
                                                    text = "default: ${env.defaultValue}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                    if (env.description.isNotBlank()) {
                                        Text(
                                            text = env.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
