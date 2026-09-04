package com.example.security

import android.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

data class KeyWrappingResult(
    val saltBase64: String,
    val wrappedMasterKeyBase64: String,
    val recoveryCode: String,
    val recoverySaltBase64: String,
    val wrappedRecoveryKeyBase64: String
)

/**
 * Manages the layered key architecture:
 * Keystore KEK -> User PIN Derived Key -> Vault Master Key -> Per-File Keys.
 */
object VaultKeyManager {
    @Volatile
    private var activeMasterKey: SecretKey? = null

    fun isUnlocked(): Boolean = activeMasterKey != null

    fun getActiveMasterKey(): SecretKey {
        return activeMasterKey ?: throw IllegalStateException("Vault is locked")
    }

    /**
     * Initializes a new vault:
     * 1. Generates 256-bit Vault Master Key with SecureRandom.
     * 2. Derives PIN key using PBKDF2 and random 16-byte salt.
     * 3. Wraps Master Key with PIN key (AES-256-GCM).
     * 4. Envelopes wrapped key with Android Keystore hardware KEK.
     * 5. Generates optional recovery code and recovery envelope.
     */
    fun initializeVault(pin: String): KeyWrappingResult {
        val masterKeyBytes = CryptoManager.generateRandomBytes(32)
        val salt = CryptoManager.generateRandomBytes(16)
        val pinKey = CryptoManager.deriveKeyFromPin(pin.toCharArray(), salt)

        // Layer 1: Encrypt master key with PIN key
        val pinEncryptedMaster = CryptoManager.encryptAesGcm(masterKeyBytes, pinKey)

        // Layer 2: Envelope with Keystore root key
        val doubleWrappedMaster = KeystoreManager.wrapData(pinEncryptedMaster)

        // Generate Recovery Code (e.g. AEGIS-XXXX-XXXX-XXXX)
        val recoveryCode = generateRecoveryCode()
        val recoverySalt = CryptoManager.generateRandomBytes(16)
        val recoveryKey = CryptoManager.deriveKeyFromPin(recoveryCode.toCharArray(), recoverySalt)
        val recoveryEncryptedMaster = CryptoManager.encryptAesGcm(masterKeyBytes, recoveryKey)
        val doubleWrappedRecovery = KeystoreManager.wrapData(recoveryEncryptedMaster)

        // Set as active session key
        activeMasterKey = SecretKeySpec(masterKeyBytes.copyOf(), "AES")

        // Zero out intermediate buffers
        CryptoManager.wipe(masterKeyBytes)

        return KeyWrappingResult(
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            wrappedMasterKeyBase64 = Base64.encodeToString(doubleWrappedMaster, Base64.NO_WRAP),
            recoveryCode = recoveryCode,
            recoverySaltBase64 = Base64.encodeToString(recoverySalt, Base64.NO_WRAP),
            wrappedRecoveryKeyBase64 = Base64.encodeToString(doubleWrappedRecovery, Base64.NO_WRAP)
        )
    }

    /**
     * Unlocks the vault using the user's PIN.
     * Unwraps Layer 2 (Keystore) then Layer 1 (PIN Key) to restore the Master Key in memory.
     */
    fun unlockWithPin(pin: String, saltBase64: String, wrappedMasterKeyBase64: String): Boolean {
        return try {
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val doubleWrappedMaster = Base64.decode(wrappedMasterKeyBase64, Base64.NO_WRAP)

            // Unwrap Keystore
            val pinEncryptedMaster = KeystoreManager.unwrapData(doubleWrappedMaster)

            // Derive PIN key
            val pinKey = CryptoManager.deriveKeyFromPin(pin.toCharArray(), salt)

            // Decrypt Master Key
            val masterKeyBytes = CryptoManager.decryptAesGcm(pinEncryptedMaster, pinKey)
            activeMasterKey = SecretKeySpec(masterKeyBytes.copyOf(), "AES")
            CryptoManager.wipe(masterKeyBytes)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Unlocks the vault using the user-provided Recovery Code.
     */
    fun unlockWithRecoveryCode(
        recoveryCode: String,
        recoverySaltBase64: String,
        wrappedRecoveryKeyBase64: String
    ): Boolean {
        return try {
            val cleanCode = recoveryCode.trim().uppercase()
            val salt = Base64.decode(recoverySaltBase64, Base64.NO_WRAP)
            val doubleWrappedRecovery = Base64.decode(wrappedRecoveryKeyBase64, Base64.NO_WRAP)

            val recoveryEncryptedMaster = KeystoreManager.unwrapData(doubleWrappedRecovery)
            val recoveryKey = CryptoManager.deriveKeyFromPin(cleanCode.toCharArray(), salt)
            val masterKeyBytes = CryptoManager.decryptAesGcm(recoveryEncryptedMaster, recoveryKey)

            activeMasterKey = SecretKeySpec(masterKeyBytes.copyOf(), "AES")
            CryptoManager.wipe(masterKeyBytes)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Changes user PIN without re-encrypting existing vault files.
     * Unwraps active master key, generates new salt, re-wraps master key with new PIN key.
     */
    fun rotatePin(newPin: String): Pair<String, String> {
        val masterKey = getActiveMasterKey()
        val masterKeyBytes = masterKey.encoded

        val newSalt = CryptoManager.generateRandomBytes(16)
        val newPinKey = CryptoManager.deriveKeyFromPin(newPin.toCharArray(), newSalt)
        val pinEncryptedMaster = CryptoManager.encryptAesGcm(masterKeyBytes, newPinKey)
        val doubleWrappedMaster = KeystoreManager.wrapData(pinEncryptedMaster)

        return Pair(
            Base64.encodeToString(newSalt, Base64.NO_WRAP),
            Base64.encodeToString(doubleWrappedMaster, Base64.NO_WRAP)
        )
    }

    /**
     * Generates a fresh per-file encryption key and wraps it with the active Master Key.
     */
    fun createPerFileKey(): Pair<SecretKey, String> {
        val masterKey = getActiveMasterKey()
        val fileKeyBytes = CryptoManager.generateRandomBytes(32)
        val fileKey = SecretKeySpec(fileKeyBytes, "AES")

        val wrappedFileKey = CryptoManager.encryptAesGcm(fileKeyBytes, masterKey)
        CryptoManager.wipe(fileKeyBytes)

        return Pair(fileKey, Base64.encodeToString(wrappedFileKey, Base64.NO_WRAP))
    }

    /**
     * Unwraps a per-file key using the active Master Key.
     */
    fun unwrapPerFileKey(wrappedFileKeyBase64: String): SecretKey {
        val masterKey = getActiveMasterKey()
        val wrappedBytes = Base64.decode(wrappedFileKeyBase64, Base64.NO_WRAP)
        val fileKeyBytes = CryptoManager.decryptAesGcm(wrappedBytes, masterKey)
        val fileKey = SecretKeySpec(fileKeyBytes.copyOf(), "AES")
        CryptoManager.wipe(fileKeyBytes)
        return fileKey
    }

    fun lockVault() {
        activeMasterKey = null
    }

    private fun generateRecoveryCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val part1 = (1..4).map { chars.random() }.joinToString("")
        val part2 = (1..4).map { chars.random() }.joinToString("")
        val part3 = (1..4).map { chars.random() }.joinToString("")
        val part4 = (1..4).map { chars.random() }.joinToString("")
        return "AEGIS-$part1-$part2-$part3-$part4"
    }
}
