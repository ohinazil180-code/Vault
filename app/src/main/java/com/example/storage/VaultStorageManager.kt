package com.example.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.VaultObjectEntity
import com.example.security.CryptoManager
import com.example.security.VaultKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object VaultStorageManager {

    private const val VAULT_DIR = "vault"
    private const val OBJECTS_DIR = "objects"
    private const val TEMP_DIR = "temp_staging"
    private const val DECRYPTED_CACHE_DIR = "decrypted"

    private fun getVaultObjectsBaseDir(context: Context): File {
        val dir = File(context.filesDir, "$VAULT_DIR/$OBJECTS_DIR")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getTempStagingDir(context: Context): File {
        val dir = File(context.cacheDir, TEMP_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getDecryptedCacheDir(context: Context): File {
        val dir = File(context.cacheDir, DECRYPTED_CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Resolves physical storage file location using two-level directory sharding:
     * e.g., /vault/objects/ab/cd/<uuid>.pvobj
     */
    fun getPhysicalFile(context: Context, relativePath: String): File {
        return File(context.filesDir, "$VAULT_DIR/$relativePath")
    }

    /**
     * Cleans up any decrypted cache files to prevent data lingering in plaintext.
     */
    fun clearDecryptedCache(context: Context) {
        try {
            val cacheDir = getDecryptedCacheDir(context)
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    /**
     * Import Pipeline:
     * Streams URI data -> Computes SHA-256 fingerprint -> Encrypts with unique per-file AES-256-GCM key ->
     * Writes to temp file -> Atomically moves to shard location -> Returns VaultObjectEntity.
     */
    suspend fun importFileFromUri(
        context: Context,
        uri: Uri,
        notes: String = ""
    ): VaultObjectEntity = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        // 1. Resolve original metadata
        var originalFileName = "import_${System.currentTimeMillis()}"
        var reportedSize: Long = 0
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) originalFileName = name
                }
                if (sizeIndex != -1) {
                    reportedSize = cursor.getLong(sizeIndex)
                }
            }
        }

        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val extension = originalFileName.substringAfterLast('.', "").lowercase()
        val category = determineCategory(mimeType, extension)

        // 2. Read plaintext into bounded memory or buffer
        val inputStream: InputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open source stream for: $uri")

        val rawBytes = inputStream.use { it.readBytes() }
        val actualSize = rawBytes.size.toLong()
        val checksumSha256 = CryptoManager.sha256(rawBytes)

        // 3. Create encryption context: unique per-file key + random IV
        val (perFileKey, wrappedFileKeyBase64) = VaultKeyManager.createPerFileKey()
        val encryptedData = CryptoManager.encryptAesGcm(rawBytes, perFileKey)
        CryptoManager.wipe(rawBytes)

        // 4. Staging to temporary file
        val objectId = UUID.randomUUID().toString()
        val tempFile = File(getTempStagingDir(context), "$objectId.tmp")
        FileOutputStream(tempFile).use { it.write(encryptedData) }

        // 5. Atomic placement into sharded directory: /vault/objects/aa/bb/<objectId>.pvobj
        val prefix1 = objectId.substring(0, 2)
        val prefix2 = objectId.substring(2, 4)
        val shardDir = File(getVaultObjectsBaseDir(context), "$prefix1/$prefix2")
        if (!shardDir.exists()) shardDir.mkdirs()

        val finalFile = File(shardDir, "$objectId.pvobj")
        if (!tempFile.renameTo(finalFile)) {
            FileOutputStream(finalFile).use { out ->
                FileInputStream(tempFile).use { inp -> inp.copyTo(out) }
            }
            tempFile.delete()
        }

        val relativePath = "$OBJECTS_DIR/$prefix1/$prefix2/$objectId.pvobj"

        VaultObjectEntity(
            id = objectId,
            storageRelativePath = relativePath,
            originalName = originalFileName,
            plainDisplayExtension = extension,
            mimeType = mimeType,
            category = category,
            fileSizeBytes = actualSize,
            wrappedFileKeyBase64 = wrappedFileKeyBase64,
            checksumSha256 = checksumSha256,
            isFavorite = false,
            isDeleted = false,
            deletedTimestamp = null,
            createdAt = System.currentTimeMillis(),
            notes = notes
        )
    }

    /**
     * Decrypts an encrypted vault object into a safe temporary cache file for viewer display.
     */
    suspend fun decryptObjectToCache(
        context: Context,
        obj: VaultObjectEntity
    ): File = withContext(Dispatchers.IO) {
        val physicalFile = getPhysicalFile(context, obj.storageRelativePath)
        require(physicalFile.exists()) { "Physical encrypted object not found on storage" }

        val encryptedBytes = physicalFile.readBytes()
        val perFileKey = VaultKeyManager.unwrapPerFileKey(obj.wrappedFileKeyBase64)
        val decryptedBytes = CryptoManager.decryptAesGcm(encryptedBytes, perFileKey)

        val ext = if (obj.plainDisplayExtension.isNotBlank()) ".${obj.plainDisplayExtension}" else ""
        val cacheFile = File(getDecryptedCacheDir(context), "view_${obj.id}$ext")
        FileOutputStream(cacheFile).use { it.write(decryptedBytes) }
        CryptoManager.wipe(decryptedBytes)

        cacheFile
    }

    /**
     * Decrypts raw byte content directly into memory (e.g. for small text/note/image rendering).
     */
    suspend fun decryptObjectBytes(
        context: Context,
        obj: VaultObjectEntity
    ): ByteArray = withContext(Dispatchers.IO) {
        val physicalFile = getPhysicalFile(context, obj.storageRelativePath)
        require(physicalFile.exists()) { "Physical encrypted object not found" }

        val encryptedBytes = physicalFile.readBytes()
        val perFileKey = VaultKeyManager.unwrapPerFileKey(obj.wrappedFileKeyBase64)
        CryptoManager.decryptAesGcm(encryptedBytes, perFileKey)
    }

    /**
     * Permanently deletes the physical ciphertext object from private storage.
     */
    fun deletePhysicalObject(context: Context, relativePath: String): Boolean {
        val physicalFile = getPhysicalFile(context, relativePath)
        return if (physicalFile.exists()) physicalFile.delete() else true
    }

    private fun determineCategory(mimeType: String, extension: String): String {
        return when {
            mimeType.startsWith("image/") || extension in listOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp") -> "PHOTO"
            mimeType.startsWith("video/") || extension in listOf("mp4", "mkv", "mov", "avi", "webm", "3gp") -> "VIDEO"
            mimeType.startsWith("text/") || mimeType.contains("pdf") || extension in listOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "md") -> "DOCUMENT"
            else -> "OTHER"
        }
    }
}
