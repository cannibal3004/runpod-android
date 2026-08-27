package com.canni.runpod.data.repo

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import com.canni.runpod.data.api.ApiErrors
import com.canni.runpod.data.api.RunPodApi
import com.canni.runpod.data.api.dto.Pod
import com.canni.runpod.data.api.dto.UpdateSshKeysRequest
import com.canni.runpod.data.auth.SshKeyStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class TermuxSshRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: RunPodApi,
    private val sshKeyStore: SshKeyStore,
) {

    fun isTermuxInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun isRunCommandPermissionGranted(): Boolean =
        context.checkSelfPermission(TERMUX_RUN_COMMAND_PERMISSION) == PackageManager.PERMISSION_GRANTED

    suspend fun ensureKeyRegistered() {
        val line = sshKeyStore.publicKeyLine
        val newBlob = line.split(' ').getOrNull(1).orEmpty()
        val res = api.listSshKeys()
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        val existing = res.body()?.keys ?: emptyList()
        if (newBlob.isNotEmpty() && existing.any { it.split(' ').getOrNull(1) == newBlob }) return
        val put = api.replaceSshKeys(UpdateSshKeysRequest(existing + line))
        if (!put.isSuccessful) throw ApiErrors.fromResponse(put.code(), put.errorBody()?.string())
    }

    /** Sends an SSH session to Termux via the RUN_COMMAND service. Returns an error message, or null on success. */
    suspend fun openSsh(pod: Pod): String? {
        val endpoint = (pod.ssh?.direct ?: pod.ssh?.proxy)
            ?: return "This pod has no SSH access. Create it with SSH enabled."
        val command = endpoint.command
            ?: return "This pod has no SSH command available yet."
        val rest = command.removePrefix("ssh").trim()

        val keyB64 = withContext(Dispatchers.IO) {
            Base64.encodeToString(sshKeyStore.privateKeyPem.toByteArray(), Base64.NO_WRAP)
        }
        val script = "mkdir -p ~/.runpod/ssh && echo $keyB64 | base64 -d > ~/.runpod/ssh/id_runpod" +
            " && chmod 600 ~/.runpod/ssh/id_runpod" +
            " && { command -v ssh >/dev/null 2>&1 || pkg install -y openssh; }" +
            " && exec ssh -i ~/.runpod/ssh/id_runpod -o StrictHostKeyChecking=no $rest"

        val intent = Intent().apply {
            setClassName(TERMUX_PACKAGE, RUN_COMMAND_SERVICE)
            setAction(ACTION_RUN_COMMAND)
            putExtra(EXTRA_COMMAND_PATH, "\$PREFIX/bin/bash")
            putExtra(EXTRA_ARGUMENTS, arrayOf("-c", script))
            putExtra(EXTRA_BACKGROUND, false)
            putExtra(EXTRA_SESSION_ACTION, "0")
            putExtra(EXTRA_COMMAND_LABEL, "RunPod SSH: ${pod.name}")
            putExtra(EXTRA_COMMAND_DESCRIPTION, "SSH session to RunPod pod ${pod.name}")
        }

        if (context.packageManager.resolveService(intent, 0) == null)
            return "This Termux version is too old for external commands. Update Termux from F-Droid or GitHub."

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        } catch (e: SecurityException) {
            return "Termux blocked the request. Grant this app the 'Run commands in Termux environment' permission: App info → Permissions → Additional permissions."
        }

        try {
            val ui = Intent().apply {
                setClassName(TERMUX_PACKAGE, TERMUX_ACTIVITY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(ui)
        } catch (e: Exception) {
            // Session still runs in the background; the Termux notification provides access.
        }
        return null
    }

    companion object {
        const val TERMUX_RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
        private const val TERMUX_PACKAGE = "com.termux"
        private const val TERMUX_ACTIVITY = "com.termux.app.TermuxActivity"
        private const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
        private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
        private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        private const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
        private const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_LABEL"
        private const val EXTRA_COMMAND_DESCRIPTION = "com.termux.RUN_COMMAND_DESCRIPTION"
    }
}
