package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.data.AppDatabase
import com.example.data.model.ActivityLogEntity
import com.example.data.model.SecureNoteEntity
import com.example.data.model.SecureSecretEntity
import com.example.data.model.VaultConfigEntity
import com.example.data.model.VaultObjectEntity
import com.example.security.CryptoManager
import com.example.security.VaultKeyManager
import com.example.storage.VaultStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class VaultIntegrityReport(
    val status: String, // "HEALTHY", "WARNING", "REPAIRABLE"
    val totalDbRecords: Int,
    val verifiedDiskFiles: Int,
    val missingDiskFiles: List<String>,
    val orphanedDiskFiles: Int,
    val duplicateCount: Int
)

class VaultRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.vaultDao()

    val configFlow: Flow<VaultConfigEntity?> = dao.getConfig()
    val activeObjectsFlow: Flow<List<VaultObjectEntity>> = dao.getAllActiveObjects()
    val recycleBinObjectsFlow: Flow<List<VaultObjectEntity>> = dao.getRecycleBinObjects()
    val notesFlow: Flow<List<SecureNoteEntity>> = dao.getAllNotes()
    val secretsFlow: Flow<List<SecureSecretEntity>> = dao.getAllSecrets()
    val activityLogsFlow: Flow<List<ActivityLogEntity>> = dao.getRecentLogs(50)
    val totalStorageBytesFlow: Flow<Long?> = dao.totalActiveSizeBytes()
    val duplicatesFlow: Flow<List<VaultObjectEntity>> = dao.getDuplicates()

    suspend fun initializeVault(pin: String): String = withContext(Dispatchers.IO) {
        val result = VaultKeyManager.initializeVault(pin)
        val config = VaultConfigEntity(
            id = 1,
            isInitialized = true,
            saltBase64 = result.saltBase64,
            wrappedMasterKeyBase64 = result.wrappedMasterKeyBase64,
            recoverySaltBase64 = result.recoverySaltBase64,
            wrappedRecoveryKeyBase64 = result.wrappedRecoveryKeyBase64,
            biometricEnabled = false,
            autoLockTimeoutSeconds = 60,
            secureScreenEnabled = false,
            activityLogEnabled = true,
            createdAt = System.currentTimeMillis()
        )
        dao.saveConfig(config)
        logActivity("INITIALIZE", "Project Aegis vault initialized with AES-256-GCM hardware key binding")
        result.recoveryCode
    }

    suspend fun unlockWithPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        val config = dao.getConfigDirect() ?: return@withContext false
        val success = VaultKeyManager.unlockWithPin(pin, config.saltBase64, config.wrappedMasterKeyBase64)
        if (success) {
            logActivity("UNLOCK", "Vault unlocked via primary PIN")
        }
        success
    }

    suspend fun unlockWithRecoveryCode(code: String): Boolean = withContext(Dispatchers.IO) {
        val config = dao.getConfigDirect() ?: return@withContext false
        val success = VaultKeyManager.unlockWithRecoveryCode(code, config.recoverySaltBase64, config.wrappedRecoveryKeyBase64)
        if (success) {
            logActivity("RECOVERY_UNLOCK", "Vault unlocked via cryptographic recovery credential")
        }
        success
    }

    suspend fun changePin(newPin: String): Boolean = withContext(Dispatchers.IO) {
        if (!VaultKeyManager.isUnlocked()) return@withContext false
        val (newSalt, newWrappedMaster) = VaultKeyManager.rotatePin(newPin)
        val config = dao.getConfigDirect() ?: return@withContext false
        dao.saveConfig(
            config.copy(
                saltBase64 = newSalt,
                wrappedMasterKeyBase64 = newWrappedMaster
            )
        )
        logActivity("PIN_CHANGED", "Primary authentication credential rotated successfully")
        true
    }

    fun lockVault() {
        VaultKeyManager.lockVault()
        VaultStorageManager.clearDecryptedCache(context)
    }

    suspend fun updateAutoLock(seconds: Int) = withContext(Dispatchers.IO) {
        dao.updateAutoLock(seconds)
        logActivity("SETTINGS", "Auto-lock timeout set to ${seconds}s")
    }

    suspend fun updateBiometric(enabled: Boolean) = withContext(Dispatchers.IO) {
        dao.updateBiometric(enabled)
        logActivity("SETTINGS", if (enabled) "Biometric authentication enabled" else "Biometric authentication disabled")
    }

    suspend fun updateSecureScreen(enabled: Boolean) = withContext(Dispatchers.IO) {
        dao.updateSecureScreen(enabled)
    }

    // --- File Operations ---
    suspend fun importFiles(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var count = 0
        uris.forEach { uri ->
            try {
                val entity = VaultStorageManager.importFileFromUri(context, uri)
                dao.insertObject(entity)
                count++
            } catch (e: Exception) {
                // Keep vault state consistent on single file error
            }
        }
        if (count > 0) {
            logActivity("IMPORT", "Imported $count encrypted object(s) with per-file AES-GCM keys")
        }
        count
    }

    suspend fun toggleFavorite(id: String, current: Boolean) = withContext(Dispatchers.IO) {
        dao.setFavorite(id, !current)
    }

    suspend fun renameObject(id: String, newName: String) = withContext(Dispatchers.IO) {
        dao.renameObject(id, newName)
        logActivity("RENAME", "Object renamed")
    }

    suspend fun moveToRecycleBin(id: String) = withContext(Dispatchers.IO) {
        dao.setDeleted(id, true, System.currentTimeMillis())
        logActivity("TRASH", "Moved object to Recycle Bin")
    }

    suspend fun restoreFromRecycleBin(id: String) = withContext(Dispatchers.IO) {
        dao.setDeleted(id, false, null)
        logActivity("RESTORE", "Restored object from Recycle Bin")
    }

    suspend fun permanentDelete(id: String) = withContext(Dispatchers.IO) {
        val obj = dao.getObjectById(id)
        if (obj != null) {
            VaultStorageManager.deletePhysicalObject(context, obj.storageRelativePath)
            dao.deletePermanently(id)
            logActivity("DELETE", "Permanently destroyed encrypted object $id")
        }
    }

    suspend fun emptyRecycleBin() = withContext(Dispatchers.IO) {
        val trashed = dao.getRecycleBinObjects()
        // Delete all physical files in bin
        val allObjs = dao.getAllObjectsDirect().filter { it.isDeleted }
        allObjs.forEach { obj ->
            VaultStorageManager.deletePhysicalObject(context, obj.storageRelativePath)
        }
        dao.emptyRecycleBin()
        logActivity("EMPTY_BIN", "Emptied Recycle Bin (${allObjs.size} objects removed)")
    }

    suspend fun decryptObjectForViewing(obj: VaultObjectEntity): File = withContext(Dispatchers.IO) {
        VaultStorageManager.decryptObjectToCache(context, obj)
    }

    // --- Secure Notes ---
    suspend fun saveNote(
        id: String?,
        title: String,
        content: String,
        category: String,
        isPinned: Boolean
    ): String = withContext(Dispatchers.IO) {
        val masterKey = VaultKeyManager.getActiveMasterKey()
        val encryptedContent = CryptoManager.encryptAesGcm(content.toByteArray(Charsets.UTF_8), masterKey)
        val encryptedContentBase64 = Base64.encodeToString(encryptedContent, Base64.NO_WRAP)

        val noteId = id ?: UUID.randomUUID().toString()
        val note = SecureNoteEntity(
            id = noteId,
            title = title.ifBlank { "Untitled Note" },
            encryptedContentBase64 = encryptedContentBase64,
            category = category,
            isFavorite = false,
            isPinned = isPinned,
            isLocked = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertNote(note)
        logActivity("NOTE", if (id == null) "Created encrypted note" else "Updated encrypted note")
        noteId
    }

    suspend fun decryptNoteContent(encryptedContentBase64: String): String = withContext(Dispatchers.Default) {
        try {
            val masterKey = VaultKeyManager.getActiveMasterKey()
            val cipherBytes = Base64.decode(encryptedContentBase64, Base64.NO_WRAP)
            val decryptedBytes = CryptoManager.decryptAesGcm(cipherBytes, masterKey)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "[Decryption error: invalid key or corrupted payload]"
        }
    }

    suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        dao.deleteNote(id)
        logActivity("NOTE_DELETE", "Deleted encrypted note")
    }

    // --- Secure Secrets ---
    suspend fun saveSecret(
        id: String?,
        title: String,
        username: String,
        passwordPlain: String,
        websiteUrl: String,
        category: String,
        notes: String
    ): String = withContext(Dispatchers.IO) {
        val masterKey = VaultKeyManager.getActiveMasterKey()
        val encryptedPw = CryptoManager.encryptAesGcm(passwordPlain.toByteArray(Charsets.UTF_8), masterKey)
        val encryptedPwBase64 = Base64.encodeToString(encryptedPw, Base64.NO_WRAP)

        val secretId = id ?: UUID.randomUUID().toString()
        val secret = SecureSecretEntity(
            id = secretId,
            title = title.ifBlank { "Account" },
            username = username,
            encryptedPasswordBase64 = encryptedPwBase64,
            websiteUrl = websiteUrl,
            category = category,
            notes = notes,
            isFavorite = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertSecret(secret)
        logActivity("SECRET", if (id == null) "Stored new encrypted credential" else "Updated credential")
        secretId
    }

    suspend fun decryptSecretPassword(encryptedPasswordBase64: String): String = withContext(Dispatchers.Default) {
        try {
            val masterKey = VaultKeyManager.getActiveMasterKey()
            val cipherBytes = Base64.decode(encryptedPasswordBase64, Base64.NO_WRAP)
            val decryptedBytes = CryptoManager.decryptAesGcm(cipherBytes, masterKey)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "******"
        }
    }

    suspend fun deleteSecret(id: String) = withContext(Dispatchers.IO) {
        dao.deleteSecret(id)
        logActivity("SECRET_DELETE", "Deleted credential from secret manager")
    }

    // --- Security Center Integrity Check ---
    suspend fun runIntegrityCheck(): VaultIntegrityReport = withContext(Dispatchers.IO) {
        val allDbObjects = dao.getAllObjectsDirect()
        val missingFiles = mutableListOf<String>()
        var verifiedCount = 0

        allDbObjects.forEach { obj ->
            val file = VaultStorageManager.getPhysicalFile(context, obj.storageRelativePath)
            if (file.exists()) {
                verifiedCount++
            } else {
                missingFiles.add(obj.id)
            }
        }

        val duplicates = dao.getAllObjectsDirect().groupBy { it.checksumSha256 }.filter { it.value.size > 1 }
        val duplicateCount = duplicates.values.sumOf { it.size }

        val status = when {
            missingFiles.isNotEmpty() -> "WARNING"
            duplicateCount > 0 -> "HEALTHY_WITH_DUPLICATES"
            else -> "HEALTHY"
        }

        VaultIntegrityReport(
            status = status,
            totalDbRecords = allDbObjects.size,
            verifiedDiskFiles = verifiedCount,
            missingDiskFiles = missingFiles,
            orphanedDiskFiles = 0,
            duplicateCount = duplicateCount
        )
    }

    suspend fun clearActivityLogs() = withContext(Dispatchers.IO) {
        dao.clearLogs()
    }

    private suspend fun logActivity(action: String, description: String) {
        try {
            dao.insertLog(
                ActivityLogEntity(
                    action = action,
                    description = description,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
    }
}
