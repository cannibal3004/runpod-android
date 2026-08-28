package com.canni.runpod.data.repo

import com.canni.runpod.data.api.dto.Pod
import com.canni.runpod.data.auth.SshKeyStore
import java.io.File
import java.security.KeyFactory
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.Ed25519KeyFactory
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyPairWrapper
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
 * Controls the RunPod web terminal (gotty on port 19123) over SSH, using the
 * same shell commands the console runs through its internal API. The proxy URL
 * includes a random 32-char path written to /root/gotty.hash when gotty starts.
 */
@Singleton
class WebTerminalRepository @Inject constructor(
    private val sshKeyStore: SshKeyStore,
) {

    data class State(
        val isRunning: Boolean,
        val isUnsupported: Boolean,
        val hash: String?,
    )

    suspend fun check(pod: Pod): State = withContext(Dispatchers.IO) {
        withClient(pod) { client ->
            val statusOut = exec(client, STATUS_COMMAND).trim()
            val hashOut = exec(client, HASH_COMMAND)
            val hash = hashOut.lineSequence()
                .map { it.trim().removePrefix("/").removeSuffix("/") }
                .firstOrNull { it.matches(HASH_PATTERN) }
            State(
                isRunning = statusOut.startsWith("running"),
                isUnsupported = statusOut.startsWith("unsupported"),
                hash = hash,
            )
        }
    }

    suspend fun start(pod: Pod) = withContext(Dispatchers.IO) {
        withClient(pod) { client ->
            exec(client, START_COMMAND)
        }
    }

    suspend fun stop(pod: Pod) = withContext(Dispatchers.IO) {
        withClient(pod) { client ->
            exec(client, STOP_COMMAND)
        }
    }

    /**
     * The random path only exists on gotty builds that honor
     * --random-url-length; otherwise the terminal is served at the root.
     */
    fun url(podId: String, hash: String?): String =
        if (hash.isNullOrBlank()) {
            "https://$podId-19123.proxy.runpod.net/"
        } else {
            "https://$podId-19123.proxy.runpod.net/$hash"
        }

    private inline fun <T> withClient(pod: Pod, block: (SSHClient) -> T): T {
        val (host, port, user) = parseSshTarget(pod)
            ?: throw IllegalStateException("This pod has no SSH access configured.")
        configureCryptoProvider()
        val config = DefaultConfig()
        config.keyExchangeFactories =
            config.keyExchangeFactories.filterNot { it.name.startsWith("curve25519") }
        val client = SSHClient(config)
        try {
            client.addHostKeyVerifier(PromiscuousVerifier())
            client.connectTimeout = CONNECT_TIMEOUT_MS
            client.connect(host, port)
            authenticate(client, user)
            return block(client)
        } finally {
            try {
                client.disconnect()
            } catch (_: Exception) {
            }
        }
    }

    private var providerConfigured = false

    /**
     * sshj resolves JCA algorithms through SecurityUtils. On Android the
     * default resolution hits AndroidKeyStore (which rejects non-keystore
     * keys), and some system images (e.g. HarmonyOS-based emulators) ship a
     * stub "BC" provider without Ed25519 while refusing to let apps replace
     * it. Register the app's full bcprov under a fresh name and pin the
     * first provider that can really generate an Ed25519 key.
     */
    private fun configureCryptoProvider() {
        if (providerConfigured) return
        providerConfigured = true
        addBouncyCastle()
        val testPkcs8 = ED25519_PKCS8_HEADER + ByteArray(32)
        for (p in Security.getProviders()) {
            if (p.name.equals("AndroidKeyStore", ignoreCase = true)) continue
            val result = runCatching {
                KeyFactory.getInstance("Ed25519", p.name)
                    .generatePrivate(PKCS8EncodedKeySpec(testPkcs8))
            }
            if (result.isSuccess) {
                SecurityUtils.setSecurityProvider(p.name)
                return
            }
        }
        SecurityUtils.setRegisterBouncyCastle(false)
    }

    private fun addBouncyCastle() {
        runCatching {
            Security.removeProvider("BC")
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /**
     * sshj 0.40's file parser handles RSA/EC PKCS#8 and OpenSSH keys, but not
     * Ed25519 in PKCS#8 (the format the app stores). For Ed25519 keys, build
     * the pair in memory: Ed25519KeyFactory takes the raw 32-byte seed and
     * 32-byte public key and wraps them into the DER encodings Android's
     * Ed25519 KeyFactory expects.
     */
    private fun authenticate(client: SSHClient, user: String) {
        val pem = sshKeyStore.privateKeyPem
        if (pem.contains("BEGIN PRIVATE KEY")) {
            parseEd25519Pem(pem)?.let { (seed, pub) ->
                val provider = KeyPairWrapper(
                    Ed25519KeyFactory.getPublicKey(pub),
                    Ed25519KeyFactory.getPrivateKey(seed),
                )
                client.authPublickey(user, provider)
                return
            }
        }
        val keyFile = File.createTempFile("runpod-ssh", ".pem")
        try {
            keyFile.writeText(pem)
            client.loadKeys(keyFile.absolutePath)
            client.authPublickey(user)
        } finally {
            runCatching { keyFile.delete() }
        }
    }

    private fun parseEd25519Pem(pem: String): Pair<ByteArray, ByteArray>? {
        return try {
            val der = Base64.getDecoder().decode(
                pem.lineSequence()
                    .filterNot { it.startsWith("-----") }
                    .joinToString("")
                    .trim(),
            )
            val pkcs8 = PrivateKeyInfo.getInstance(der)
            if (pkcs8.privateKeyAlgorithm.algorithm != ED25519_OID) return null
            val inner = pkcs8.privateKey.octets
            val seed = when {
                inner.size == 32 -> inner
                inner.size == 34 && inner[0] == 0x04.toByte() && inner[1] == 0x20.toByte() ->
                    inner.copyOfRange(2, 34)
                else -> return null
            }
            seed to Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded
        } catch (_: Exception) {
            null
        }
    }

    /**
     * RunPod's sshd ignores the exec command parameter: it only serves
     * PTY-allocated interactive shells. So start a real shell, wait for the
     * prompt, "type" the command with a marker appended, and harvest the
     * output between the PTY echo of the command and the marker line.
     * Canonical-mode PTYs drop input lines over 255 bytes, so callers must
     * keep commands short.
     */
    private fun exec(client: SSHClient, command: String): String {
        val session = client.startSession()
        try {
            // Wide PTY so the echoed command never line-wraps.
            session.allocatePTY("xterm", 1000, 50, 0, 0, emptyMap())
            val shell = session.startShell()
            try {
                val sb = StringBuilder()
                val reader = Thread {
                    val buf = ByteArray(8192)
                    try {
                        while (true) {
                            val n = shell.inputStream.read(buf)
                            if (n == -1) break
                            synchronized(sb) { sb.append(String(buf, 0, n)) }
                        }
                    } catch (_: Exception) {
                    }
                }
                reader.isDaemon = true
                reader.start()
                val deadline = System.currentTimeMillis() + CMD_TIMEOUT_S * 1000L
                fun snapshot(): String = synchronized(sb) { sb.toString() }
                fun waitUntil(pred: (String) -> Boolean): Boolean {
                    while (System.currentTimeMillis() < deadline) {
                        if (pred(snapshot())) return true
                        if (!shell.isOpen) break
                        Thread.sleep(100)
                    }
                    return pred(snapshot())
                }
                val prompt = Regex("root@\\S+:\\S*#")
                if (!waitUntil { stripEscapes(it).lineSequence().any { l -> prompt.containsMatchIn(l) } }) {
                    throw IllegalStateException("Shell prompt not reached: " + snapshot().take(200))
                }
                val marker = "WTMK${(System.nanoTime() % 100_000_000L)}"
                shell.outputStream.write("$command; echo $marker\n".toByteArray())
                shell.outputStream.flush()
                if (!waitUntil { stripEscapes(it).lineSequence().any { l -> l.trim() == marker } }) {
                    throw IllegalStateException("Command timed out: " + snapshot().take(200))
                }
                val lines = stripEscapes(snapshot()).lineSequence().toList()
                val mi = lines.indexOfLast { it.trim() == marker }
                if (mi < 0) throw IllegalStateException("Marker not found: " + snapshot().take(300))
                // The PTY echoes the typed command, and that echo line also
                // contains the marker; the command's output sits between the
                // echo line and the marker line.
                val echoIdx = lines.subList(0, mi).indexOfLast { it.contains(marker) }
                val start = if (echoIdx >= 0) {
                    echoIdx + 1
                } else {
                    val p = lines.subList(0, mi).indexOfLast { prompt.containsMatchIn(it) }
                    (p.coerceAtLeast(-1)) + 1
                }
                return lines.subList(start, mi).joinToString("\n")
            } finally {
                shell.close()
            }
        } finally {
            session.close()
        }
    }

    private val oscEscape = Regex("\\u001b\\][^\\u0007\\u001b]*(?:\\u0007|\\u001b\\\\)")
    private val csiEscape = Regex("\\u001b\\[[0-9;?]*[A-Za-z]")
    private val bareEscape = Regex("\\u001b")
    private val HASH_PATTERN = Regex("[A-Za-z0-9]{5,64}")

    private fun stripEscapes(s: String): String =
        s.replace(oscEscape, "").replace(csiEscape, "").replace(bareEscape, "")

    private fun parseSshTarget(pod: Pod): Triple<String, Int, String>? {
        val command = pod.ssh?.proxy?.command ?: pod.ssh?.direct?.command ?: return null
        return parseSshCommand(command)
    }

    private fun parseSshCommand(command: String): Triple<String, Int, String>? {
        val tokens = command.trim().split(Regex("\\s+"))
        if (tokens.firstOrNull() != "ssh") return null
        var port = 22
        var target: String? = null
        var i = 1
        while (i < tokens.size) {
            val t = tokens[i]
            when {
                t == "-p" && i + 1 < tokens.size -> port = tokens[++i].toIntOrNull() ?: 22
                (t == "-o" || t == "-i" || t == "-F") && !t.contains('=') && i + 1 < tokens.size -> i++
                t.startsWith("-") -> Unit
                else -> target = t
            }
            i++
        }
        val tgt = target ?: return null
        val at = tgt.lastIndexOf('@')
        if (at <= 0 || at == tgt.length - 1) return null
        return Triple(tgt.substring(at + 1), port, tgt.substring(0, at))
    }

    private companion object {
        val ED25519_OID = ASN1ObjectIdentifier("1.3.101.112")
        // OpenSSL/OpenSSH-convention Ed25519 PKCS#8 header (16 bytes),
        // followed by the 32-byte seed.
        val ED25519_PKCS8_HEADER = Base64.getDecoder().decode("MC4CAQEwBQYDK2VwBCIEIA")

        const val CONNECT_TIMEOUT_MS = 15_000
        const val CMD_TIMEOUT_S = 30

        // Commands are typed into an interactive PTY shell in canonical
        // mode, whose input line buffer is 255 bytes — every command (plus
        // the ~25-byte marker prefix) must stay well under that.
        const val STATUS_COMMAND =
            "ps -fC 'gotty' >/dev/null && echo 'running' || which gotty >/dev/null && " +
                "echo 'not running' || echo 'unsupported'"

        const val HASH_COMMAND =
            "sed -n 's#.*http[^ ]*://[^/]*/\\([A-Za-z0-9]\\{5,32\\}\\)/.*#\\1#p' " +
                "/root/gotty.log 2>/dev/null | head -n 1"

        const val START_COMMAND =
            "{ gotty --random-url-length 32 --ws-origin '.*' -p 19123 env TERM=xterm bash " +
                "> /root/gotty.log 2>&1 & } </dev/null >/dev/null 2>&1"

        const val STOP_COMMAND = "kill -9 `pidof gotty`"
    }
}
