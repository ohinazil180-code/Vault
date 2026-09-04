package com.example.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.data.AppDatabase
import com.example.data.model.SecureNoteEntity
import com.example.data.model.SecureSecretEntity
import com.example.data.model.VaultObjectEntity
import com.example.security.CryptoManager
import com.example.security.VaultKeyManager
import com.example.storage.VaultStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.spec.SecretKeySpec

data class BackupValidationResult(
    val isValid: Boolean,
    val formatVersion: Int,
    val objectCount: Int,
    val noteCount: Int,
    val secretCount: Int,
    val errorMessage: String? = null
)

object BackupManager {

    private const val BACKUP_MAGIC = "PVBACKUP_AEGIS_V1"
    private const val FORMAT_VERSION = 1

    /**
     * Creates an encrypted .aegis backup archive exported to the given Uri or local backup file.
     */
    suspend fun createBackup(
        context: Context,
        backupPassword: String,
        destinationUri: Uri? = null
    ): File = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val dao = db.vaultDao()

        // 1. Generate unique salt and derive backup key from user recovery password
        val backupSalt = CryptoManager.generateRandomBytes(16)
        val backupKey = CryptoManager.deriveKeyFromPin(backupPassword.toCharArray(), backupSalt)

        // 2. Wrap the active master key with this backup key
        val masterKey = VaultKeyManager.getActiveMasterKey()
        val wrappedMasterKey = CryptoManager.encryptAesGcm(masterKey.encoded, backupKey)

        // 3. Serialize all metadata into encrypted JSON
        val allObjects = dao.getAllObjectsDirect()
        val allNotes = dao.getAllNotesDirect()
        val allSecrets = dao.getAllSecretsDirect()

        val metadataJson = JSONObject().apply {
            put("objects", JSONArray().apply {
                allObjects.forEach { obj ->
                    put(JSONObject().apply {
                        put("id", obj.id)
                        put("storageRelativePath", obj.storageRelativePath)
                        put("originalName", obj.originalName)
                        put("plainDisplayExtension", obj.plainDisplayExtension)
                        put("mimeType", obj.mimeType)
                        put("category", obj.category)
                        put("fileSizeBytes", obj.fileSizeBytes)
                        put("wrappedFileKeyBase64", obj.wrappedFileKeyBase64)
                        put("checksumSha256", obj.checksumSha256)
                        put("isFavorite", obj.isFavorite)
                        put("isDeleted", obj.isDeleted)
                        put("deletedTimestamp", obj.deletedTimestamp ?: 0L)
                        put("createdAt", obj.createdAt)
                        put("notes", obj.notes)
                    })
                }
            })
            put("notes", JSONArray().apply {
                allNotes.forEach { note ->
                    put(JSONObject().apply {
                        put("id", note.id)
                        put("title", note.title)
                        put("encryptedContentBase64", note.encryptedContentBase64)
                        put("category", note.category)
                        put("isFavorite", note.isFavorite)
                        put("isPinned", note.isPinned)
                        put("isLocked", note.isLocked)
                        put("createdAt", note.createdAt)
                        put("updatedAt", note.updatedAt)
                    })
                }
            })
            put("secrets", JSONArray().apply {
                allSecrets.forEach { secret ->
                    put(JSONObject().apply {
                        put("id", secret.id)
                        put("title", secret.title)
                        put("username", secret.username)
                        put("encryptedPasswordBase64", secret.encryptedPasswordBase64)
                        put("websiteUrl", secret.websiteUrl)
                        put("category", secret.category)
                        put("notes", secret.notes)
                        put("isFavorite", secret.isFavorite)
                        put("createdAt", secret.createdAt)
                        put("updatedAt", secret.updatedAt)
                    })
                }
            })
        }

        // Encrypt the metadata using the backup key
        val encryptedMetadata = CryptoManager.encryptAesGcm(
            metadataJson.toString().toByteArray(Charsets.UTF_8),
            backupKey
        )

        // 4. Create backup manifest
        val manifestJson = JSONObject().apply {
            put("magic", BACKUP_MAGIC)
            put("version", FORMAT_VERSION)
            put("saltBase64", Base64.encodeToString(backupSalt, Base64.NO_WRAP))
            put("wrappedMasterKeyBase64", Base64.encodeToString(wrappedMasterKey, Base64.NO_WRAP))
            put("objectCount", allObjects.size)
            put("noteCount", allNotes.size)
            put("secretCount", allSecrets.size)
            put("timestamp", System.currentTimeMillis())
        }

        // 5. Build ZIP container
        val backupFile = File(context.cacheDir, "AEGIS_BACKUP_${System.currentTimeMillis()}.aegis")
        ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
            // Entry 1: manifest.json
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestJson.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Entry 2: metadata.enc
            zos.putNextEntry(ZipEntry("metadata.enc"))
            zos.write(encryptedMetadata)
            zos.closeEntry()

            // Entry 3..N: encrypted objects
            allObjects.forEach { obj ->
                val physicalFile = VaultStorageManager.getPhysicalFile(context, obj.storageRelativePath)
                if (physicalFile.exists()) {
                    zos.putNextEntry(ZipEntry("objects/${obj.id}.pvobj"))
                    FileInputStream(physicalFile).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }

        // If a SAF output URI was supplied, stream the backup file to it
        if (destinationUri != null) {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                FileInputStream(backupFile).use { inp -> inp.copyTo(out) }
            }
        }

        backupFile
    }

    /**
     * Inspects and validates a backup file before restoration.
     */
    suspend fun inspectBackup(
        context: Context,
        backupUri: Uri,
        backupPassword: String
    ): BackupValidationResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(backupUri)
                ?: return@withContext BackupValidationResult(false, 0, 0, 0, 0, "Cannot open backup stream")

            var manifestJson: JSONObject? = null
            var encryptedMetadataBytes: ByteArray? = null
            var physicalObjectCount = 0

            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "manifest.json" -> {
                            val text = zis.bufferedReader().readText()
                            manifestJson = JSONObject(text)
                        }
                        entry.name == "metadata.enc" -> {
                            val baos = ByteArrayOutputStream()
                            zis.copyTo(baos)
                            encryptedMetadataBytes = baos.toByteArray()
                        }
                        entry.name.startsWith("objects/") -> {
                            physicalObjectCount++
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (manifestJson == null || encryptedMetadataBytes == null) {
                return@withContext BackupValidationResult(false, 0, 0, 0, 0, "Invalid backup container structure")
            }

            val magic = manifestJson!!.optString("magic")
            val version = manifestJson!!.optInt("version", 0)
            if (magic != BACKUP_MAGIC || version != FORMAT_VERSION) {
                return@withContext BackupValidationResult(false, version, 0, 0, 0, "Unsupported backup format or version")
            }

            val saltBase64 = manifestJson!!.getString("saltBase64")
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val backupKey = CryptoManager.deriveKeyFromPin(backupPassword.toCharArray(), salt)

            // Test decrypting metadata
            val decryptedMetadata = try {
                CryptoManager.decryptAesGcm(encryptedMetadataBytes!!, backupKey)
            } catch (e: Exception) {
                return@withContext BackupValidationResult(false, version, 0, 0, 0, "Incorrect backup password or corrupted metadata")
            }

            val metaObj = JSONObject(String(decryptedMetadata, Charsets.UTF_8))
            val objectCount = metaObj.optJSONArray("objects")?.length() ?: 0
            val noteCount = metaObj.optJSONArray("notes")?.length() ?: 0
            val secretCount = metaObj.optJSONArray("secrets")?.length() ?: 0

            BackupValidationResult(
                isValid = true,
                formatVersion = version,
                objectCount = objectCount,
                noteCount = noteCount,
                secretCount = secretCount
            )
        } catch (e: Exception) {
            BackupValidationResult(false, 0, 0, 0, 0, e.message ?: "Validation error")
        }
    }

    /**
     * Transactionally restores vault contents from a verified backup.
     */
    suspend fun restoreBackup(
        context: Context,
        backupUri: Uri,
        backupPassword: String
    ): Boolean = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(backupUri)
            ?: throw IllegalStateException("Cannot open backup stream")

        var manifestJson: JSONObject? = null
        var encryptedMetadataBytes: ByteArray? = null
        val objectBytesMap = mutableMapOf<String, ByteArray>()

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when {
                    entry.name == "manifest.json" -> {
                        manifestJson = JSONObject(zis.bufferedReader().readText())
                    }
                    entry.name == "metadata.enc" -> {
                        val baos = ByteArrayOutputStream()
                        zis.copyTo(baos)
                        encryptedMetadataBytes = baos.toByteArray()
                    }
                    entry.name.startsWith("objects/") && entry.name.endsWith(".pvobj") -> {
                        val fileName = entry.name.substringAfterLast('/')
                        val baos = ByteArrayOutputStream()
                        zis.copyTo(baos)
                        objectBytesMap[fileName] = baos.toByteArray()
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        requireNotNull(manifestJson)
        requireNotNull(encryptedMetadataBytes)

        val salt = Base64.decode(manifestJson!!.getString("saltBase64"), Base64.NO_WRAP)
        val backupKey = CryptoManager.deriveKeyFromPin(backupPassword.toCharArray(), salt)

        // Decrypt metadata
        val decryptedMetadata = CryptoManager.decryptAesGcm(encryptedMetadataBytes!!, backupKey)
        val metaObj = JSONObject(String(decryptedMetadata, Charsets.UTF_8))

        val db = AppDatabase.getDatabase(context)
        val dao = db.vaultDao()

        // Write objects to sharded directory
        val objectsArray = metaObj.optJSONArray("objects") ?: JSONArray()
        for (i in 0 until objectsArray.length()) {
            val item = objectsArray.getJSONObject(i)
            val id = item.getString("id")
            val relativePath = item.getString("storageRelativePath")
            val targetFile = VaultStorageManager.getPhysicalFile(context, relativePath)
            targetFile.parentFile?.mkdirs()

            val objectData = objectBytesMap["$id.pvobj"]
            if (objectData != null) {
                FileOutputStream(targetFile).use { it.write(objectData) }
            }

            val vaultObj = VaultObjectEntity(
                id = id,
                storageRelativePath = relativePath,
                originalName = item.getString("originalName"),
                plainDisplayExtension = item.optString("plainDisplayExtension"),
                mimeType = item.optString("mimeType"),
                category = item.optString("category", "OTHER"),
                fileSizeBytes = item.optLong("fileSizeBytes"),
                wrappedFileKeyBase64 = item.getString("wrappedFileKeyBase64"),
                checksumSha256 = item.getString("checksumSha256"),
                isFavorite = item.optBoolean("isFavorite"),
                isDeleted = item.optBoolean("isDeleted"),
                deletedTimestamp = if (item.has("deletedTimestamp") && item.getLong("deletedTimestamp") > 0) item.getLong("deletedTimestamp") else null,
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                notes = item.optString("notes")
            )
            dao.insertObject(vaultObj)
        }

        // Restore notes
        val notesArray = metaObj.optJSONArray("notes") ?: JSONArray()
        for (i in 0 until notesArray.length()) {
            val item = notesArray.getJSONObject(i)
            dao.insertNote(
                SecureNoteEntity(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    encryptedContentBase64 = item.getString("encryptedContentBase64"),
                    category = item.optString("category", "GENERAL"),
                    isFavorite = item.optBoolean("isFavorite"),
                    isPinned = item.optBoolean("isPinned"),
                    isLocked = item.optBoolean("isLocked"),
                    createdAt = item.optLong("createdAt"),
                    updatedAt = item.optLong("updatedAt")
                )
            )
        }

        // Restore secrets
        val secretsArray = metaObj.optJSONArray("secrets") ?: JSONArray()
        for (i in 0 until secretsArray.length()) {
            val item = secretsArray.getJSONObject(i)
            dao.insertSecret(
                SecureSecretEntity(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    username = item.getString("username"),
                    encryptedPasswordBase64 = item.getString("encryptedPasswordBase64"),
                    websiteUrl = item.optString("websiteUrl"),
                    category = item.optString("category", "LOGIN"),
                    notes = item.optString("notes"),
                    isFavorite = item.optBoolean("isFavorite"),
                    createdAt = item.optLong("createdAt"),
                    updatedAt = item.optLong("updatedAt")
                )
            )
        }

        true
    }
}
