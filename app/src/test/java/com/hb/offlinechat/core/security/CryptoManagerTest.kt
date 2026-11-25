package com.hb.offlinechat.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

class CryptoManagerTest {

    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")

    @Test
    fun encryptDecryptRoundTrip() {
        val plain = "Hello HB Offline Chat 👋"
        val encrypted = CryptoManager.encrypt(plain, key)
        assertNotEquals(plain, encrypted)
        assertEquals(plain, CryptoManager.decrypt(encrypted, key))
    }

    @Test
    fun tamperedCiphertextIsRejected() {
        val encrypted = CryptoManager.encrypt("secret message", key)
        val bytes = Base64.getDecoder().decode(encrypted)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0xFF).toByte()
        val tampered = Base64.getEncoder().encodeToString(bytes)
        assertNull(CryptoManager.decrypt(tampered, key))
    }

    @Test
    fun wrongKeyCannotDecrypt() {
        val encrypted = CryptoManager.encrypt("secret", key)
        val wrongKey = SecretKeySpec(ByteArray(32) { (it + 1).toByte() }, "AES")
        assertNull(CryptoManager.decrypt(encrypted, wrongKey))
    }

    @Test
    fun shortAuthenticationCodeIsDeterministicAndSorted() {
        val a = CryptoManager.shortAuthenticationCode("keyA", "keyB")
        val b = CryptoManager.shortAuthenticationCode("keyB", "keyA")
        assertEquals(a, b)
        assertEquals(6, a.length)
        assertTrue(a.all { it.isDigit() })
    }

    @Test
    fun fileHashIsSha256Hex() {
        val hash = CryptoManager.fileHash("abc".toByteArray())
        assertEquals(64, hash.length)
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hash
        )
    }

    @Test
    fun hkdfDerivesStableKey() {
        val secret = "shared-secret".toByteArray()
        val salt = "salt".toByteArray()
        val k1 = CryptoManager.deriveSessionKey(secret, salt)
        val k2 = CryptoManager.deriveSessionKey(secret, salt)
        assertEquals(k1.encoded.asList(), k2.encoded.asList())
        assertEquals(32, k1.encoded.size)
    }
}
