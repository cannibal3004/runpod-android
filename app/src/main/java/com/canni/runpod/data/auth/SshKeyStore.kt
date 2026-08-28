package com.canni.runpod.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1TaggedObject
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.RSAKeyParameters
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.internal.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64

@Singleton
class SshKeyStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private data class Key(val pem: String, val line: String, val label: String)

    @Volatile
    private var generatedCache: Key? = null

    @Volatile
    private var importedCache: Key? = null

    val activeSource: String
        get() = if (prefs.getString(KEY_SOURCE, SOURCE_GENERATED) == SOURCE_IMPORTED) SOURCE_IMPORTED else SOURCE_GENERATED

    val activeLabel: String
        get() = if (activeSource == SOURCE_IMPORTED) imported().label else GENERATED_LABEL

    /** Private key content of the active key, usable as-is by `ssh -i`. */
    val privateKeyPem: String
        get() = active().pem

    /** authorized_keys-style line for PUT /v2/account/ssh-keys. */
    val publicKeyLine: String
        get() = active().line

    /** Validates [pem] as an SSH private key, derives its public key line, and makes it the active key. */
    fun importKey(pem: String, label: String) {
        val text = pem.trim()
        if (text.isEmpty()) throw IllegalArgumentException("The selected file is empty.")
        val line = derivePublicLine(text, label)
        prefs.edit()
            .putString(KEY_IMPORTED_PEM, text)
            .putString(KEY_IMPORTED_LINE, line)
            .putString(KEY_IMPORTED_LABEL, label)
            .putString(KEY_SOURCE, SOURCE_IMPORTED)
            .apply()
        importedCache = Key(text, line, label)
    }

    fun useGeneratedKey() {
        prefs.edit().putString(KEY_SOURCE, SOURCE_GENERATED).apply()
    }

    private fun active(): Key = if (activeSource == SOURCE_IMPORTED) imported() else generated()

    private fun generated(): Key {
        generatedCache?.let { return it }
        val pem = prefs.getString(KEY_PRIVATE, null)
        val line = prefs.getString(KEY_PUBLIC, null)
        if (pem != null && line != null) {
            val normalizedPem = normalizeEd25519Pem(pem)
            val result = Key(normalizedPem, line, GENERATED_LABEL)
            if (normalizedPem != pem) {
                prefs.edit().putString(KEY_PRIVATE, normalizedPem).apply()
            }
            generatedCache = result
            return result
        }
        val result = generate()
        prefs.edit()
            .putString(KEY_PRIVATE, result.pem)
            .putString(KEY_PUBLIC, result.line)
            .apply()
        generatedCache = result
        return result
    }

    private fun imported(): Key {
        importedCache?.let { return it }
        val pem = prefs.getString(KEY_IMPORTED_PEM, null)
        val line = prefs.getString(KEY_IMPORTED_LINE, null)
        if (pem == null || line == null) throw IllegalStateException("No imported key available.")
        val result = Key(pem, line, prefs.getString(KEY_IMPORTED_LABEL, "imported") ?: "imported")
        importedCache = result
        return result
    }

    private fun generate(): Key {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val pair = generator.generateKeyPair()
        val privateKey = pair.private as Ed25519PrivateKeyParameters
        val publicKey = pair.public as Ed25519PublicKeyParameters

        val seed = privateKey.getEncoded()
        val pem = toPem(ed25519Pkcs8Der(seed))
        val line = toAuthorizedLine(privateKey, GENERATED_LABEL)

        return Key(pem, line, GENERATED_LABEL)
    }

    // Ed25519 PKCS#8 in the OpenSSL convention (version 0, double-wrapped
    // octet string). This is what JDK/Android Ed25519 KeyFactories expect;
    // BC's PrivateKeyInfo(algId, DEROctetString) produces exactly this.
    private fun ed25519Pkcs8Der(seed: ByteArray): ByteArray =
        PrivateKeyInfo(AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), DEROctetString(seed)).encoded

    /**
     * Normalizes a stored generated key to the OpenSSL-convention PKCS#8.
     * Handles both the original 48-byte form and the RFC 5958 46-byte form
     * (version 1, single-wrapped) produced by a brief regression. Idempotent;
     * the key pair is unchanged, so the RunPod registration stays valid.
     */
    private fun normalizeEd25519Pem(pem: String): String {
        return try {
            val der = Base64.getDecoder().decode(
                pem.lineSequence()
                    .filterNot { it.startsWith("-----") }
                    .joinToString("")
                    .trim(),
            )
            val pkcs8 = PrivateKeyInfo.getInstance(der)
            if (pkcs8.privateKeyAlgorithm.algorithm != EdECObjectIdentifiers.id_Ed25519) return pem
            val octets = pkcs8.privateKey.octets
            val seed = when {
                octets.size == 32 -> octets
                octets.size == 34 && octets[0] == 0x04.toByte() && octets[1] == 0x20.toByte() ->
                    octets.copyOfRange(2, 34)
                else -> return pem
            }
            toPem(ed25519Pkcs8Der(seed))
        } catch (_: Exception) {
            pem
        }
    }

    private fun toPem(der: ByteArray): String = buildString {
        append("-----BEGIN PRIVATE KEY-----\n")
        Base64.getEncoder().encodeToString(der).chunked(64).forEach {
            append(it)
            append('\n')
        }
        append("-----END PRIVATE KEY-----\n")
    }

    // --- Public key derivation for imported keys ---

    private fun derivePublicLine(pem: String, comment: String): String {
        val params: AsymmetricKeyParameter
        if (pem.contains("BEGIN OPENSSH PRIVATE KEY")) {
            val start = pem.indexOf("-----BEGIN OPENSSH PRIVATE KEY-----") + BEGIN_OPENSSH.length
            val end = pem.indexOf("-----END OPENSSH PRIVATE KEY-----")
            if (start < BEGIN_OPENSSH.length || end < start) {
                throw IllegalArgumentException("Malformed OpenSSH key file.")
            }
            val b64 = pem.substring(start, end).replace(Regex("\\s"), "")
            val payload = try {
                Base64.getDecoder().decode(b64)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Malformed OpenSSH key file.")
            }
            if (payload.size > 15 && payload[14] == 0.toByte() &&
                String(payload, 0, 14, Charsets.US_ASCII) == "openssh-key-v1"
            ) {
                var off = 15
                val clen = ByteBuffer.wrap(payload, off, 4).int
                off += 4
                val cipher = String(payload, off, clen, Charsets.US_ASCII)
                if (cipher != "none") {
                    throw IllegalArgumentException(
                        "Passphrase-protected keys are not supported. " +
                            "Remove the passphrase first (ssh-keygen -p) and try again.",
                    )
                }
            }
            params = try {
                OpenSSHPrivateKeyUtil.parsePrivateKeyBlob(payload)
            } catch (e: Exception) {
                throw IllegalArgumentException("Could not parse OpenSSH private key.")
            } ?: throw IllegalArgumentException("Could not parse OpenSSH private key.")
        } else {
            val reader = PemReader(StringReader(pem))
            var found: AsymmetricKeyParameter? = null
            var obj: PemObject?
            try {
                obj = reader.readPemObject()
            } catch (e: Exception) {
                obj = null
            }
            while (obj != null) {
                if (found == null) {
                    try {
                        found = paramsFromPemObject(obj)
                    } catch (e: Exception) {
                        // Not a supported block; keep scanning.
                    }
                }
                try {
                    obj = reader.readPemObject()
                } catch (e: Exception) {
                    obj = null
                }
            }
            params = found
                ?: throw IllegalArgumentException(
                    "No SSH-capable private key found. Supported formats: OpenSSH, PKCS#8, PKCS#1 (RSA/EC).",
                )
        }
        return toAuthorizedLine(params, comment)
    }

    private fun paramsFromPemObject(obj: PemObject): AsymmetricKeyParameter? {
        val type = obj.type
        val content = obj.content
        return when {
            type.equals("PRIVATE KEY", ignoreCase = true) ->
                PrivateKeyFactory.createKey(PrivateKeyInfo.getInstance(content))

            type.equals("RSA PRIVATE KEY", ignoreCase = true) ->
                PrivateKeyFactory.createKey(
                    PrivateKeyInfo(
                        AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption),
                        DEROctetString(content),
                    ),
                )

            type.equals("EC PRIVATE KEY", ignoreCase = true) -> {
                val seq = ASN1Sequence.getInstance(content)
                val d = BigInteger(1, ASN1OctetString.getInstance(seq.getObjectAt(1)).octets)
                val oidObject = if (seq.size() >= 3) {
                    ASN1ObjectIdentifier.getInstance(
                        ASN1TaggedObject.getInstance(seq.getObjectAt(2)).parseExplicitBaseObject(),
                    )
                } else {
                    return null
                }
                val x9: X9ECParameters = SECNamedCurves.getByOID(oidObject) ?: return null
                ECPrivateKeyParameters(d, ECDomainParameters(x9))
            }

            else -> null
        }
    }

    private fun toAuthorizedLine(params: AsymmetricKeyParameter, comment: String): String {
        val wire = OpenSSHPublicKeyUtil.encodePublicKey(toPublic(params))
        val nameLen = ByteBuffer.wrap(wire, 0, 4).int
        val name = String(wire, 4, nameLen, Charsets.US_ASCII)
        return "$name ${Base64.getEncoder().encodeToString(wire)} $comment"
    }

    private fun toPublic(params: AsymmetricKeyParameter): AsymmetricKeyParameter = when (params) {
        is Ed25519PrivateKeyParameters -> params.generatePublicKey()
        is RSAPrivateCrtKeyParameters -> RSAKeyParameters(false, params.modulus, params.publicExponent)
        is ECPrivateKeyParameters -> ECPublicKeyParameters(
            params.parameters.g.multiply(params.d).normalize(),
            params.parameters,
        )
        else -> throw IllegalArgumentException("Unsupported key type: ${params.javaClass.simpleName}")
    }

    companion object {
        private const val PREFS_FILE = "runpod_ssh"
        private const val KEY_PRIVATE = "private_key_pem"
        private const val KEY_PUBLIC = "public_key_line"
        private const val KEY_IMPORTED_PEM = "imported_key_pem"
        private const val KEY_IMPORTED_LINE = "imported_key_line"
        private const val KEY_IMPORTED_LABEL = "imported_key_label"
        private const val KEY_SOURCE = "active_key_source"
        const val SOURCE_GENERATED = "generated"
        const val SOURCE_IMPORTED = "imported"
        const val GENERATED_LABEL = "runpod-android"
        private const val BEGIN_OPENSSH = "-----BEGIN OPENSSH PRIVATE KEY-----"
    }
}
