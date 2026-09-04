package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Standard JVM-backed AES-256-GCM and PBKDF2 authenticated cryptographic primitives.
 * Strictly adheres to zero unauthenticated ciphers, no fixed IVs, no hardcoded keys.
 */
object CryptoManager {
    private const val AES_KEY_SIZE_BITS = 256
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PBKDF2_ITERATIONS = 65536
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"

    private val secureRandom = SecureRandom()

    fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    /**
     * Derives a 256-bit symmetric key from user PIN and random salt using PBKDF2WithHmacSHA256.
     */
    fun deriveKeyFromPin(pin: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, AES_KEY_SIZE_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * AES-256-GCM authenticated encryption.
     * Generates a fresh 12-byte random IV for every encryption call.
     * Result format: IV (12 bytes) + Ciphertext with 16-byte GCM Auth Tag appended.
     */
    fun encryptAesGcm(plaintext: ByteArray, key: SecretKey, aad: ByteArray? = null): ByteArray {
        val iv = generateRandomBytes(GCM_IV_LENGTH_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        if (aad != null && aad.isNotEmpty()) {
            cipher.updateAAD(aad)
        }
        val ciphertextWithTag = cipher.doFinal(plaintext)
        return iv + ciphertextWithTag
    }

    /**
     * AES-256-GCM authenticated decryption.
     * Extracts IV, verifies GCM Tag, and decrypts ciphertext.
     */
    fun decryptAesGcm(ivAndCiphertext: ByteArray, key: SecretKey, aad: ByteArray? = null): ByteArray {
        require(ivAndCiphertext.size >= GCM_IV_LENGTH_BYTES + 16) { "Ciphertext too short" }
        val iv = ivAndCiphertext.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val ciphertext = ivAndCiphertext.copyOfRange(GCM_IV_LENGTH_BYTES, ivAndCiphertext.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        if (aad != null && aad.isNotEmpty()) {
            cipher.updateAAD(aad)
        }
        return cipher.doFinal(ciphertext)
    }

    /**
     * Computes SHA-256 checksum for safe data deduplication without exposing plaintext content.
     */
    fun sha256(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Securely clears sensitive byte arrays from memory when done.
     */
    fun wipe(bytes: ByteArray) {
        Arrays.fill(bytes, 0.toByte())
    }
}
