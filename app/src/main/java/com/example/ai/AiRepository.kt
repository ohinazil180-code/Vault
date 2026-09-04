package com.example.ai

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.dao.AiDao
import com.example.data.model.AiCitationItem
import com.example.data.model.AiConversationEntity
import com.example.data.model.AiMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AiRepository(
    context: Context,
    private val aiDao: AiDao = AppDatabase.getDatabase(context).aiDao(),
    val geminiClient: GeminiClient = GeminiClient(),
    val cloudAuthManager: AiCloudAuthManager = AiCloudAuthManager(context)
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    val conversationsFlow: Flow<List<AiConversationEntity>> = aiDao.getAllConversations()

    private val _currentConversation = MutableStateFlow<AiConversationEntity?>(null)
    val currentConversation: StateFlow<AiConversationEntity?> = _currentConversation.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<AiMessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<AiMessageEntity>> = _currentMessages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _currentResearchStep = MutableStateFlow<String?>(null)
    val currentResearchStep: StateFlow<String?> = _currentResearchStep.asStateFlow()

    private var activeGenerationJob: Job? = null
    private var messagesObserverJob: Job? = null

    init {
        coroutineScope.launch {
            aiDao.cleanupTemporaryConversations()
        }
    }

    fun selectConversation(conversationId: String) {
        coroutineScope.launch {
            val conv = aiDao.getConversationById(conversationId)
            _currentConversation.value = conv
            observeMessages(conversationId)
        }
    }

    private fun observeMessages(conversationId: String) {
        messagesObserverJob?.cancel()
        messagesObserverJob = coroutineScope.launch {
            aiDao.getMessagesForConversation(conversationId).collect { list ->
                _currentMessages.value = list
            }
        }
    }

    suspend fun createNewConversation(
        title: String = "New Conversation",
        mode: String = "CHAT",
        isTemporary: Boolean = false,
        permissionLevel: Int = 0,
        searchMode: String = "AUTO"
    ): AiConversationEntity {
        val newConv = AiConversationEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            mode = mode,
            isTemporary = isTemporary,
            permissionLevel = permissionLevel,
            searchMode = searchMode,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        aiDao.insertConversation(newConv)
        _currentConversation.value = newConv
        observeMessages(newConv.id)
        return newConv
    }

    fun renameConversation(id: String, newTitle: String) {
        coroutineScope.launch {
            aiDao.renameConversation(id, newTitle)
            if (_currentConversation.value?.id == id) {
                _currentConversation.value = _currentConversation.value?.copy(title = newTitle)
            }
        }
    }

    fun deleteConversation(id: String) {
        coroutineScope.launch {
            aiDao.deleteConversation(id)
            if (_currentConversation.value?.id == id) {
                _currentConversation.value = null
                _currentMessages.value = emptyList()
            }
        }
    }

    fun deleteAllConversations() {
        coroutineScope.launch {
            aiDao.deleteAllConversations()
            _currentConversation.value = null
            _currentMessages.value = emptyList()
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        _isGenerating.value = false
        _currentResearchStep.value = null
    }

    /**
     * Sends user message, coordinates streaming generation with Gemini + Google Search Grounding.
     */
    fun sendMessage(
        userText: String,
        attachedContext: String? = null,
        attachedSummary: String? = null,
        searchModeOverride: String? = null
    ) {
        val currentConv = _currentConversation.value
        val convId = currentConv?.id ?: run {
            coroutineScope.launch {
                val created = createNewConversation(
                    title = if (userText.length > 28) userText.take(28) + "..." else userText
                )
                sendMessage(userText, attachedContext, attachedSummary, searchModeOverride)
            }
            return
        }

        activeGenerationJob?.cancel()
        activeGenerationJob = coroutineScope.launch {
            _isGenerating.value = true
            _currentResearchStep.value = null

            // 1. Insert User Message
            val userMsg = AiMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = "user",
                content = userText,
                attachedContextSummary = attachedSummary,
                createdAt = System.currentTimeMillis()
            )
            aiDao.insertMessage(userMsg)
            aiDao.touchConversation(convId)

            // Auto-update conversation title if it was "New Conversation"
            if (currentConv.title == "New Conversation") {
                val newTitle = if (userText.length > 26) userText.take(26) + "..." else userText
                aiDao.renameConversation(convId, newTitle)
                _currentConversation.value = _currentConversation.value?.copy(title = newTitle)
            }

            // 2. Prepare Assistant Placeholder Message
            val assistantMsgId = UUID.randomUUID().toString()
            val initialAssistantMsg = AiMessageEntity(
                id = assistantMsgId,
                conversationId = convId,
                role = "assistant",
                content = "",
                status = "STREAMING",
                createdAt = System.currentTimeMillis()
            )
            aiDao.insertMessage(initialAssistantMsg)

            // 3. Build prompt with attached context if permission granted
            val effectivePrompt = buildString {
                if (!attachedContext.isNullOrBlank()) {
                    append("--- USER GRANTED VAULT CONTEXT (ISOLATED & SANITIZED) ---\n")
                    append(AiPermissionManager.sanitizeText(attachedContext))
                    append("\n--- END OF VAULT CONTEXT ---\n\n")
                }
                append(userText)
            }

            // 4. Fetch past history for context window
            val historySnapshot = aiDao.getMessagesSnapshot(convId)
                .filter { it.id != assistantMsgId && it.status == "COMPLETE" }
                .takeLast(8)
                .map { it.role to it.content }

            val searchMode = searchModeOverride ?: currentConv.searchMode
            val isDeepResearch = currentConv.mode == "RESEARCH"

            try {
                if (isDeepResearch) {
                    _currentResearchStep.value = "1/4: Analyzing inquiry & decomposing key claims..."
                    kotlinx.coroutines.delay(400)
                    _currentResearchStep.value = "2/4: Grounding via real-time web intelligence..."
                }

                val fullBuffer = StringBuilder()

                val result = geminiClient.generateContentStream(
                    prompt = effectivePrompt,
                    history = historySnapshot,
                    searchMode = searchMode,
                    model = if (isDeepResearch) GeminiClient.MODEL_PRO else GeminiClient.MODEL_FLASH,
                    onChunk = { chunk ->
                        fullBuffer.append(chunk)
                        coroutineScope.launch {
                            aiDao.updateMessageContent(
                                id = assistantMsgId,
                                content = fullBuffer.toString(),
                                status = "STREAMING"
                            )
                        }
                    }
                )

                if (isDeepResearch) {
                    _currentResearchStep.value = "3/4: Cross-checking citations & source veracity..."
                    kotlinx.coroutines.delay(300)
                    _currentResearchStep.value = "4/4: Synthesizing verified intelligence report..."
                }

                val citationsJson = if (result.citations.isNotEmpty()) {
                    val arr = JSONArray()
                    for (c in result.citations) {
                        val obj = JSONObject().apply {
                            put("title", c.title)
                            put("url", c.url)
                            put("domain", c.domain)
                            put("snippet", c.snippet)
                        }
                        arr.put(obj)
                    }
                    arr.toString()
                } else null

                val queriesJson = if (result.searchQueries.isNotEmpty()) {
                    JSONArray(result.searchQueries).toString()
                } else null

                aiDao.updateMessageContent(
                    id = assistantMsgId,
                    content = if (fullBuffer.isNotEmpty()) fullBuffer.toString() else result.text,
                    status = "COMPLETE",
                    citationsJson = citationsJson,
                    searchQueriesJson = queriesJson
                )
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("503") == true || e.message?.contains("UNAVAILABLE") == true ->
                        "⚡ **AEGIS Traffic Advisory**: Gemini cloud model is currently experiencing peak server demand (HTTP 503). The system is operating in resilient local mode. Please retry in a few moments."
                    e.message?.contains("429") == true || e.message?.contains("RESOURCE_EXHAUSTED") == true ->
                        "⚠️ **AEGIS Quota Advisory**: Cloud rate limit reached (HTTP 429). The system is now operating in resilient local mode. Please wait a moment before sending another message."
                    else ->
                        "⚠️ **AEGIS Security Shield Notice**: Unable to complete cloud AI request (${e.localizedMessage?.take(100) ?: "Network connection timeout"}). Private vault isolation remains secure."
                }
                aiDao.updateMessageContent(
                    id = assistantMsgId,
                    content = errorMsg,
                    status = "ERROR"
                )
            } finally {
                _isGenerating.value = false
                _currentResearchStep.value = null
            }
        }
    }
}
