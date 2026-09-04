package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AiDao
import com.example.data.dao.VaultDao
import com.example.data.model.ActivityLogEntity
import com.example.data.model.AiConversationEntity
import com.example.data.model.AiMessageEntity
import com.example.data.model.SecureNoteEntity
import com.example.data.model.SecureSecretEntity
import com.example.data.model.VaultConfigEntity
import com.example.data.model.VaultObjectEntity

@Database(
    entities = [
        VaultConfigEntity::class,
        VaultObjectEntity::class,
        SecureNoteEntity::class,
        SecureSecretEntity::class,
        ActivityLogEntity::class,
        AiConversationEntity::class,
        AiMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun aiDao(): AiDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aegis_vault_metadata.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
