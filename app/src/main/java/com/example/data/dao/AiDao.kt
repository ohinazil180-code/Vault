package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AiConversationEntity
import com.example.data.model.AiMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiDao {

    @Query("SELECT * FROM ai_conversations WHERE isTemporary = 0 ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<AiConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): AiConversationEntity?

    @Query("SELECT * FROM ai_conversations WHERE isTemporary = 0 AND title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchConversations(query: String): Flow<List<AiConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AiConversationEntity)

    @Update
    suspend fun updateConversation(conversation: AiConversationEntity)

    @Query("UPDATE ai_conversations SET title = :newTitle, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameConversation(id: String, newTitle: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ai_conversations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchConversation(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("DELETE FROM ai_conversations WHERE isTemporary = 1")
    suspend fun cleanupTemporaryConversations()

    @Query("DELETE FROM ai_conversations")
    suspend fun deleteAllConversations()

    // Messages
    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<AiMessageEntity>>

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesSnapshot(conversationId: String): List<AiMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessageEntity)

    @Update
    suspend fun updateMessage(message: AiMessageEntity)

    @Query("UPDATE ai_messages SET content = :content, status = :status, citationsJson = :citationsJson, searchQueriesJson = :searchQueriesJson WHERE id = :id")
    suspend fun updateMessageContent(id: String, content: String, status: String, citationsJson: String? = null, searchQueriesJson: String? = null)

    @Query("DELETE FROM ai_messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM ai_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)
}
