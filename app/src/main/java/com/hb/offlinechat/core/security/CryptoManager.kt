package com.hb.offlinechat.core.security

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Application-level authenticated encryption.
 * Uses standard, vetted primitives only (AES-256-GCM, HKDF-HMAC-SHA256).
 * No custom cryptographic algorithms are implemented here.
 */
object CryptoManager {

    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private const val KEY_BYTES = 32

    /** HKDF: derive a 256-bit session key from an ECDH shared secret. */
    fun deriveSessionKey(sharedSecret: ByteArray, salt: ByteArray, info: ByteArray = "hb-chat-v1".toByteArray()): SecretKeySpec {
        // Extract
        val hmac = Mac.getInstance("HmacSHA256")
        hmac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = hmac.doFinal(sharedSecret)
        // Expand
        val expand = Mac.getInstance("HmacSHA256")
        expand.init(SecretKeySpec(prk, "HmacSHA256"))
        val key = expand.doFinal(info + byteArrayOf(0x01))
        require(key.size >= KEY_BYTES)
        return SecretKeySpec(key.copyOf(KEY_BYTES), "AES")
    }

    /** Authenticated encryption: output is Base64(IV || ciphertext || tag). */
    fun encrypt(plainText: String, key: SecretKey): String {
        val iv = ByteArray(IV_BYTES).also { Random.Default.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = iv + ciphertext
        return Base64.getEncoder().encodeToString(payload)
    }

    /** Decrypt and verify integrity. Returns null on tampering or wrong key. */
    fun decrypt(payloadBase64: String, key: SecretKey): String? {
        return try {
            val payload = Base64.getDecoder().decode(payloadBase64)
            if (payload.size <= IV_BYTES) return null
            val iv = payload.copyOfRange(0, IV_BYTES)
            val ciphertext = payload.copyOfRange(IV_BYTES, payload.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /** Short authentication code (6 digits) from both public keys, for QR/visual verification. */
    fun shortAuthenticationCode(localPublicKeyBase64: String, remotePublicKeyBase64: String): String {
        val joined = listOf(localPublicKeyBase64, remotePublicKeyBase64).sorted().joinToString("|").toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(joined)
        val value = ((digest[0].toInt() and 0xFF) shl 16) or
            ((digest[1].toInt() and 0xFF) shl 8) or
            (digest[2].toInt() and 0xFF)
        return (value % 1_000_000).toString().padStart(6, '0')
    }

    /** SHA-256 integrity hash for transferred files. */
    fun fileHash(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}

/** Sliding-window replay protection keyed by sender and timestamp. */
class ReplayProtection(private val windowMs: Long = 120_000L) {
    private val seen = mutableMapOf<String, Long>()

    @Synchronized
    fun isFresh(messageId: String, timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        if (timestamp < now - windowMs || timestamp > now + windowMs) return false
        if (seen.containsKey(messageId)) return false
        seen[messageId] = timestamp
        // Prune old entries
        seen.entries.removeIf { it.value < now - windowMs }
        return true
    }
}
