package com.hb.offlinechat.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.KeyAgreement

/**
 * Device identity backed by the Android Keystore.
 * The private key never leaves secure hardware; only the public key is shared.
 * The device ID is derived from the public key (random, app-generated, no PII).
 */
class DeviceIdentity(@Suppress("UNUSED_PARAMETER") private val context: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    val deviceId: String by lazy {
        val digest = MessageDigest.getInstance("SHA-256").digest(publicKeyEncoded)
        digest.take(8).joinToString("") { "%02x".format(it) }
    }

    val deviceName: String by lazy { "HB-" + deviceId.take(6).uppercase() }

    val publicKeyEncoded: ByteArray get() = keyPair.public.encoded

    val publicKeyBase64: String get() = Base64.getEncoder().encodeToString(publicKeyEncoded)

    private val keyPair: KeyPair by lazy { loadOrGenerate() }

    init {
        // Eagerly create the identity so first use has no latency.
        keyPair
    }

    private fun loadOrGenerate(): KeyPair {
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
        if (existing != null) return KeyPair(existing.certificate.publicKey, existing.privateKey)

        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
        // Keystore key agreement is available from API 31; older devices get sign/verify only
        // and fall back to QR/short-code verification for pairing.
        val purposes = KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                KeyProperties.PURPOSE_AGREE_KEY
            } else 0
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, purposes)
            .setKeySize(256)
            .build()
        generator.initialize(spec)
        return generator.generateKeyPair()
    }

    fun sign(data: ByteArray): ByteArray = Signature.getInstance("SHA256withECDSA").run {
        initSign(keyPair.private)
        update(data)
        sign()
    }

    fun verify(data: ByteArray, signatureBase64: String, peerPublicKeyBase64: String): Boolean =
        try {
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(decodePublicKey(peerPublicKeyBase64))
                update(data)
                verify(Base64.getDecoder().decode(signatureBase64))
            }
        } catch (e: Exception) {
            false
        }

    /** ECDH shared secret; requires Keystore key agreement support (API 31+). */
    fun deriveSharedSecret(peerPublicKeyBase64: String): ByteArray =
        KeyAgreement.getInstance("ECDH").run {
            init(keyPair.private)
            doPhase(decodePublicKey(peerPublicKeyBase64), true)
            generateSecret()
        }

    private fun decodePublicKey(base64: String): PublicKey =
        KeyFactory.getInstance("EC").generatePublic(
            X509EncodedKeySpec(Base64.getDecoder().decode(base64))
        )

    fun hasKey(): Boolean = keyStore.containsAlias(KEY_ALIAS)

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "hb_offline_chat_identity_v1"
    }
}
