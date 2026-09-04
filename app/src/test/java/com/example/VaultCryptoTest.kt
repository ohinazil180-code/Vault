package com.example

import com.example.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class VaultCryptoTest {

    @Test
    fun testAesGcmEncryptionDecryption() {
        val salt = CryptoManager.generateRandomBytes(32)
        val secretKey = CryptoManager.deriveKeyFromPin("123456".toCharArray(), salt)
        val originalText = "PROJECT_AEGIS_TOP_SECRET_PAYLOAD_2026"
        val plaintextBytes = originalText.toByteArray(StandardCharsets.UTF_8)

        val encryptedBytes = CryptoManager.encryptAesGcm(plaintextBytes, secretKey)
        assertNotNull(encryptedBytes)
        assertFalse(encryptedBytes.contentEquals(plaintextBytes))

        val decryptedBytes = CryptoManager.decryptAesGcm(encryptedBytes, secretKey)
        val decryptedText = String(decryptedBytes, StandardCharsets.UTF_8)
        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testPbkdf2Derivation() {
        val pin = "123456".toCharArray()
        val salt = CryptoManager.generateRandomBytes(32)

        val key1 = CryptoManager.deriveKeyFromPin(pin, salt)
        val key2 = CryptoManager.deriveKeyFromPin(pin, salt)

        assertEquals("AES", key1.algorithm)
        assertEquals(32, key1.encoded.size) // 256-bit key
        assertTrue(key1.encoded.contentEquals(key2.encoded))

        val wrongPin = "654321".toCharArray()
        val keyWrong = CryptoManager.deriveKeyFromPin(wrongPin, salt)
        assertFalse(key1.encoded.contentEquals(keyWrong.encoded))
    }

    @Test
    fun testSha256Checksum() {
        val data = "Aegis Integrity Test Data".toByteArray(StandardCharsets.UTF_8)
        val hash = CryptoManager.sha256(data)
        assertEquals(64, hash.length)

        val hash2 = CryptoManager.sha256(data)
        assertEquals(hash, hash2)
    }
}
