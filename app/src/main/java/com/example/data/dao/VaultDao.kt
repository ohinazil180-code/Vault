package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ActivityLogEntity
import com.example.data.model.SecureNoteEntity
import com.example.data.model.SecureSecretEntity
import com.example.data.model.VaultConfigEntity
import com.example.data.model.VaultObjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    // --- Vault Config ---
    @Query("SELECT * FROM vault_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<VaultConfigEntity?>

    @Query("SELECT * FROM vault_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigDirect(): VaultConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: VaultConfigEntity)

    @Query("UPDATE vault_config SET autoLockTimeoutSeconds = :seconds WHERE id = 1")
    suspend fun updateAutoLock(seconds: Int)

    @Query("UPDATE vault_config SET biometricEnabled = :enabled WHERE id = 1")
    suspend fun updateBiometric(enabled: Boolean)

    @Query("UPDATE vault_config SET secureScreenEnabled = :enabled WHERE id = 1")
    suspend fun updateSecureScreen(enabled: Boolean)

    // --- Vault Objects ---
    @Query("SELECT * FROM vault_objects WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllActiveObjects(): Flow<List<VaultObjectEntity>>

    @Query("SELECT * FROM vault_objects WHERE isDeleted = 0 AND category = :category ORDER BY createdAt DESC")
    fun getObjectsByCategory(category: String): Flow<List<VaultObjectEntity>>

    @Query("SELECT * FROM vault_objects WHERE isDeleted = 1 ORDER BY deletedTimestamp DESC")
    fun getRecycleBinObjects(): Flow<List<VaultObjectEntity>>

    @Query("SELECT * FROM vault_objects WHERE id = :id LIMIT 1")
    suspend fun getObjectById(id: String): VaultObjectEntity?

    @Query("SELECT * FROM vault_objects")
    suspend fun getAllObjectsDirect(): List<VaultObjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObject(obj: VaultObjectEntity)

    @Update
    suspend fun updateObject(obj: VaultObjectEntity)

    @Query("UPDATE vault_objects SET isDeleted = :deleted, deletedTimestamp = :timestamp WHERE id = :id")
    suspend fun setDeleted(id: String, deleted: Boolean, timestamp: Long?)

    @Query("UPDATE vault_objects SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE vault_objects SET originalName = :newName WHERE id = :id")
    suspend fun renameObject(id: String, newName: String)

    @Query("DELETE FROM vault_objects WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM vault_objects WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()

    @Query("SELECT COUNT(*) FROM vault_objects WHERE isDeleted = 0")
    fun countActiveObjects(): Flow<Int>

    @Query("SELECT SUM(fileSizeBytes) FROM vault_objects WHERE isDeleted = 0")
    fun totalActiveSizeBytes(): Flow<Long?>

    @Query("SELECT * FROM vault_objects WHERE isDeleted = 0 AND checksumSha256 IN (SELECT checksumSha256 FROM vault_objects WHERE isDeleted = 0 GROUP BY checksumSha256 HAVING COUNT(*) > 1) ORDER BY checksumSha256")
    fun getDuplicates(): Flow<List<VaultObjectEntity>>

    // --- Secure Notes ---
    @Query("SELECT * FROM secure_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<SecureNoteEntity>>

    @Query("SELECT * FROM secure_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): SecureNoteEntity?

    @Query("SELECT * FROM secure_notes")
    suspend fun getAllNotesDirect(): List<SecureNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: SecureNoteEntity)

    @Update
    suspend fun updateNote(note: SecureNoteEntity)

    @Query("DELETE FROM secure_notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    @Query("SELECT COUNT(*) FROM secure_notes")
    fun countNotes(): Flow<Int>

    // --- Secure Secrets ---
    @Query("SELECT * FROM secure_secrets ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllSecrets(): Flow<List<SecureSecretEntity>>

    @Query("SELECT * FROM secure_secrets WHERE id = :id LIMIT 1")
    suspend fun getSecretById(id: String): SecureSecretEntity?

    @Query("SELECT * FROM secure_secrets")
    suspend fun getAllSecretsDirect(): List<SecureSecretEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecret(secret: SecureSecretEntity)

    @Update
    suspend fun updateSecret(secret: SecureSecretEntity)

    @Query("DELETE FROM secure_secrets WHERE id = :id")
    suspend fun deleteSecret(id: String)

    @Query("SELECT COUNT(*) FROM secure_secrets")
    fun countSecrets(): Flow<Int>

    // --- Activity Logs ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity)

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}
