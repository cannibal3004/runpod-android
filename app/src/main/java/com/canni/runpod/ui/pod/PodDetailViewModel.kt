package com.canni.runpod.ui.pod

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.dto.Pod
import com.canni.runpod.data.api.dto.PodAction
import com.canni.runpod.data.auth.SshKeyStore
import com.canni.runpod.data.repo.MigrationStore
import com.canni.runpod.data.repo.PodRepository
import com.canni.runpod.data.repo.TermuxSshRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val podRepository: PodRepository,
    private val termuxSshRepository: TermuxSshRepository,
    private val sshKeyStore: SshKeyStore,
    private val migrationStore: MigrationStore,
) : ViewModel() {

    private val podId: String = savedStateHandle.get<String>("podId").orEmpty()
    private var migrationPollJob: Job? = null

    fun isTermuxInstalled(): Boolean = termuxSshRepository.isTermuxInstalled()

    fun isRunCommandPermissionGranted(): Boolean = termuxSshRepository.isRunCommandPermissionGranted()

    data class MigrationUi(
        val migrationId: String,
        val status: String? = null,
        val progress: Float? = null,
        val message: String? = null,
        val targetPodId: String? = null,
    ) {
        val isTerminal: Boolean
            get() = status?.lowercase() in TERMINAL_STATUSES

        val isCompleted: Boolean
            get() = status?.lowercase() == "completed"
    }

    data class UiState(
        val pod: Pod? = null,
        val isLoading: Boolean = true,
        val error: String? = null,
        val podGone: Boolean = false,
        val busyAction: PodAction? = null,
        val actionError: String? = null,
        val migrationPromptError: String? = null,
        val migration: MigrationUi? = null,
        val migrationBusy: Boolean = false,
        val migrationDialogDismissed: Boolean = false,
        val termuxBusy: Boolean = false,
        val termuxError: String? = null,
        val termuxHint: String? = null,
        val keyLabel: String = SshKeyStore.GENERATED_LABEL,
        val keySource: String = SshKeyStore.SOURCE_GENERATED,
        val importBusy: Boolean = false,
        val importError: String? = null,
        val lastUpdated: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        _state.update {
            runCatching {
                it.copy(keyLabel = sshKeyStore.activeLabel, keySource = sshKeyStore.activeSource)
            }.getOrDefault(it)
        }
        val savedMigrationId = migrationStore.activeFor(podId)
        if (savedMigrationId != null) {
            _state.update { it.copy(migration = MigrationUi(migrationId = savedMigrationId)) }
            startMigrationPolling(savedMigrationId)
        }
    }

    override fun onCleared() {
        migrationPollJob?.cancel()
        super.onCleared()
    }

    fun load(silent: Boolean = false) {
        if (podId.isEmpty()) {
            _state.update { it.copy(isLoading = false, error = "Missing pod id.") }
            return
        }
        if (!silent) _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { podRepository.getPod(podId) }
                .onSuccess { pod ->
                    _state.update {
                        it.copy(pod = pod, isLoading = false, lastUpdated = System.currentTimeMillis().toString())
                    }
                }
                .onFailure { e ->
                    _state.update {
                        if (silent && it.pod != null && e is com.canni.runpod.data.api.ApiError && e.code == 404) {
                            it.copy(isLoading = false, podGone = true)
                        } else {
                            it.copy(isLoading = false, error = e.message ?: "Failed to load pod")
                        }
                    }
                }
        }
    }

    fun act(action: PodAction) {
        if (_state.value.busyAction != null) return
        _state.update { it.copy(busyAction = action, actionError = null) }
        viewModelScope.launch {
            runCatching { podRepository.act(podId, action) }
                .onSuccess { pod ->
                    val gone = action == PodAction.terminate && pod.status == com.canni.runpod.data.api.dto.PodStatus.TERMINATED
                    _state.update {
                        it.copy(pod = pod, isLoading = false, busyAction = null, podGone = gone)
                    }
                }
                .onFailure { e ->
                    val message = e.message ?: "Action failed"
                    val isGpuPod = _state.value.pod?.gpu != null
                    _state.update {
                        if (action == PodAction.start && isGpuPod) {
                            it.copy(busyAction = null, migrationPromptError = message)
                        } else {
                            it.copy(busyAction = null, actionError = message)
                        }
                    }
                }
        }
    }

    fun clearActionError() {
        _state.update { it.copy(actionError = null) }
    }

    fun dismissMigrationPrompt() {
        _state.update { it.copy(migrationPromptError = null) }
    }

    fun dismissMigrationDialog() {
        _state.update { it.copy(migrationDialogDismissed = true) }
    }

    fun clearMigration() {
        migrationPollJob?.cancel()
        _state.update {
            it.copy(
                migration = null,
                migrationDialogDismissed = false,
                migrationBusy = false,
                migrationPromptError = null,
            )
        }
    }

    fun startWithCpuOnly() {
        if (_state.value.migrationBusy || _state.value.migration != null) return
        _state.update { it.copy(migrationBusy = true) }
        viewModelScope.launch {
            attempt { podRepository.resumePodWithCpuOnly(podId) }
                .onSuccess {
                    _state.update { it.copy(migrationBusy = false, migrationPromptError = null) }
                    load(silent = true)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(migrationBusy = false, migrationPromptError = e.message ?: "Failed to start pod.")
                    }
                }
        }
    }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: Throwable) {
            Result.failure(e)
        }

    fun startMigration() {
        if (_state.value.migrationBusy || _state.value.migration != null) return
        _state.update { it.copy(migrationBusy = true, migrationDialogDismissed = false) }
        viewModelScope.launch {
            attempt { podRepository.migratePod(podId) }
                .onSuccess { migration ->
                    migrationStore.save(podId, migration.id)
                    attempt { podRepository.setLocked(podId, true) }
                    _state.update {
                        it.copy(
                            migrationBusy = false,
                            migration = MigrationUi(
                                migrationId = migration.id,
                                status = migration.status ?: "creating",
                                progress = 0f,
                                message = migration.message,
                            ),
                        )
                    }
                    startMigrationPolling(migration.id)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            migrationBusy = false,
                            migrationPromptError = e.message ?: "Failed to start migration.",
                        )
                    }
                }
        }
    }

    private fun startMigrationPolling(migrationId: String) {
        migrationPollJob?.cancel()
        migrationPollJob = viewModelScope.launch {
            var errors = 0
            while (true) {
                val fetched = attempt { podRepository.migrationStatus(migrationId) }
                val migration = fetched.getOrNull()
                if (fetched.isFailure || migration?.status.isNullOrBlank()) {
                    errors++
                    if (errors >= MAX_POLL_ERRORS) {
                        finishMigration(migration, "Lost connection to migration status. Check the pod list for the latest state.")
                        return@launch
                    }
                } else {
                    errors = 0
                    val status = migration.status.lowercase()
                    if (status in TERMINAL_STATUSES) {
                        finishMigration(migration, null)
                        return@launch
                    }
                    _state.update {
                        it.copy(
                            migration = it.migration?.copy(
                                status = migration.status,
                                progress = migration.progress?.toFloat(),
                                message = migration.message,
                                targetPodId = migration.targetPodId,
                            ),
                        )
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun finishMigration(
        migration: com.canni.runpod.data.api.dto.PodMigration?,
        errorMessage: String?,
    ) {
        migrationPollJob?.cancel()
        attempt { podRepository.setLocked(podId, false) }
        migrationStore.clear()
        _state.update {
            it.copy(
                migrationBusy = false,
                migrationDialogDismissed = false,
                migration = it.migration?.copy(
                    status = migration?.status ?: "failed",
                    progress = if (errorMessage == null) 1f else it.migration?.progress,
                    message = errorMessage ?: migration?.message,
                    targetPodId = migration?.targetPodId ?: it.migration?.targetPodId,
                ),
            )
        }
        load(silent = true)
    }

    fun openSshInTermux() {
        val s = _state.value
        val pod = s.pod ?: return
        if (s.termuxBusy) return
        _state.update { it.copy(termuxBusy = true, termuxError = null, termuxHint = null) }
        viewModelScope.launch {
            val keyResult = runCatching { termuxSshRepository.ensureKeyRegistered() }
            if (keyResult.isFailure) {
                _state.update {
                    it.copy(termuxBusy = false, termuxError = keyResult.exceptionOrNull()?.message ?: "Failed to register SSH key.")
                }
                return@launch
            }
            val openError = runCatching { termuxSshRepository.openSsh(pod) }.getOrNull()
            _state.update {
                it.copy(
                    termuxBusy = false,
                    termuxError = openError,
                    termuxHint = if (openError == null) ALLOW_EXTERNAL_APPS_HINT else null,
                )
            }
        }
    }

    fun clearTermuxError() {
        _state.update { it.copy(termuxError = null) }
    }

    fun importKey(uri: Uri, label: String) {
        if (_state.value.importBusy) return
        _state.update { it.copy(importBusy = true, importError = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes().decodeToString() }
                        ?: throw IOException("Could not read the selected file.")
                    sshKeyStore.importKey(text, label)
                }
            }
            result
                .onSuccess {
                    val registerError = runCatching { termuxSshRepository.ensureKeyRegistered() }
                        .exceptionOrNull()?.message
                    _state.update {
                        it.copy(
                            importBusy = false,
                            importError = null,
                            keyLabel = label,
                            keySource = SshKeyStore.SOURCE_IMPORTED,
                            termuxError = registerError?.let { e -> "Key imported, but RunPod registration failed: $e" },
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(importBusy = false, importError = e.message ?: "Could not import key.")
                    }
                }
        }
    }

    fun useGeneratedKey() {
        if (_state.value.keySource == SshKeyStore.SOURCE_GENERATED) return
        sshKeyStore.useGeneratedKey()
        _state.update {
            it.copy(
                keySource = SshKeyStore.SOURCE_GENERATED,
                keyLabel = SshKeyStore.GENERATED_LABEL,
                importError = null,
            )
        }
        viewModelScope.launch {
            val registerError = runCatching { termuxSshRepository.ensureKeyRegistered() }
                .exceptionOrNull()?.message
            _state.update {
                it.copy(termuxError = registerError?.let { e -> "Key restored, but RunPod registration failed: $e" })
            }
        }
    }

    companion object {
        const val REFRESH_MS = 5_000L
        private const val POLL_INTERVAL_MS = 5_000L
        private const val MAX_POLL_ERRORS = 3
        private val TERMINAL_STATUSES = setOf("completed", "failed", "cancelled")
        private const val ALLOW_EXTERNAL_APPS_HINT =
            "If Termux didn't open a session, check its notification. Termux requires allow-external-apps=true in ~/.termux/termux.properties."
    }
}
