package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "vault_config")
data class VaultConfigEntity(
    @PrimaryKey val id: Int = 1,
    val isInitialized: Boolean = false,
    val saltBase64: String = "",
    val wrappedMasterKeyBase64: String = "",
    val recoverySaltBase64: String = "",
    val wrappedRecoveryKeyBase64: String = "",
    val biometricEnabled: Boolean = false,
    val autoLockTimeoutSeconds: Int = 60,
    val secureScreenEnabled: Boolean = true,
    val activityLogEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vault_objects",
    indices = [
        Index(value = ["isDeleted", "category"]),
        Index(value = ["checksumSha256"])
    ]
)
data class VaultObjectEntity(
    @PrimaryKey val id: String,
    val storageRelativePath: String,
    val originalName: String,
    val plainDisplayExtension: String,
    val mimeType: String,
    val category: String, // "PHOTO", "VIDEO", "DOCUMENT", "OTHER"
    val fileSizeBytes: Long,
    val wrappedFileKeyBase64: String,
    val checksumSha256: String,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "secure_notes",
    indices = [Index(value = ["isPinned", "updatedAt"])]
)
data class SecureNoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val encryptedContentBase64: String,
    val category: String = "GENERAL",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "secure_secrets",
    indices = [Index(value = ["category", "title"])]
)
data class SecureSecretEntity(
    @PrimaryKey val id: String,
    val title: String,
    val username: String,
    val encryptedPasswordBase64: String,
    val websiteUrl: String = "",
    val category: String = "LOGIN", // "LOGIN", "CARD", "API_KEY", "OTHER"
    val notes: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "activity_logs",
    indices = [Index(value = ["timestamp"])]
)
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
