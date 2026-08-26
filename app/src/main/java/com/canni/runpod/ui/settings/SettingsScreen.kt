package com.canni.runpod.ui.settings

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canni.runpod.ui.nav.MainScaffold
import com.canni.runpod.ui.nav.Routes

@Composable
fun SettingsScreen(
    onChangeKey: () -> Unit,
    onRemoveKey: () -> Unit,
    onNavigateTopLevel: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MainScaffold(
        title = "Settings",
        currentRoute = Routes.SETTINGS,
        onNavigate = onNavigateTopLevel,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item(key = "apikey") {
                SectionHeader("API key")
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "API key",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = state.maskedKey ?: "Not set",
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row {
                            Button(onClick = onChangeKey) {
                                Text("Change key")
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = viewModel::onRemoveKeyClick,
                                enabled = state.maskedKey != null,
                            ) {
                                Text(
                                    text = "Remove",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Stored encrypted on this device. Create keys at runpod.io under Account → API keys.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    if (state.showRemoveDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissRemoveDialog,
            title = { Text("Remove API key?") },
            text = { Text("You'll need to enter a key again to use the app.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmRemoveKey()
                        onRemoveKey()
                    },
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissRemoveDialog) {
                    Text("Cancel")
                }
            },
        )
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
            .padding(top = 4.dp, bottom = 8.dp),
    )
}
