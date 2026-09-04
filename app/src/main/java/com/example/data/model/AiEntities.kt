package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_conversations",
    indices = [Index("updatedAt")]
)
data class AiConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mode: String = "CHAT", // CHAT, SEARCH, RESEARCH, EXPLAIN, SUMMARIZE, REWRITE, STUDY, DOCUMENT, VAULT
    val isTemporary: Boolean = false,
    val permissionLevel: Int = 0, // 0 = Chat only, 1 = Selected Text, 2 = Selected Doc, 3 = Selected Folder, 4 = Temp Vault
    val searchMode: String = "AUTO", // AUTO, ALWAYS, ASK, NEVER
    val languageMode: String = "AUTO", // AUTO, BN, EN
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ai_messages",
    foreignKeys = [
        ForeignKey(
            entity = AiConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId"), Index("createdAt")]
)
data class AiMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val model: String = "gemini-3.5-flash",
    val status: String = "COMPLETE", // COMPLETE, STREAMING, ERROR, CANCELLED
    val citationsJson: String? = null,
    val searchQueriesJson: String? = null,
    val researchStepsJson: String? = null,
    val attachedContextSummary: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AiCitationItem(
    val title: String,
    val url: String,
    val domain: String,
    val snippet: String = ""
)
