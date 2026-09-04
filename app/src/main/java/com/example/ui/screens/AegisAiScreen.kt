package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiPermissionManager
import com.example.ai.AiRepository
import com.example.data.model.AiCitationItem
import com.example.data.model.AiConversationEntity
import com.example.data.model.AiMessageEntity
import com.example.data.model.SecureNoteEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AegisMarkdownView
import com.example.ui.components.AegisSourcesRow
import com.example.ui.components.AegisTypingIndicator
import com.example.ui.theme.AegisBackground
import com.example.ui.theme.AegisBorder
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AegisRed = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AegisAiScreen(
    viewModel: MainViewModel,
    aiRepository: AiRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val currentConversation by aiRepository.currentConversation.collectAsState()
    val messages by aiRepository.currentMessages.collectAsState()
    val isGenerating by aiRepository.isGenerating.collectAsState()
    val researchStep by aiRepository.currentResearchStep.collectAsState()
    val conversations by aiRepository.conversationsFlow.collectAsState(initial = emptyList())
    val notes by viewModel.notes.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf("AUTO") } // AUTO, ALWAYS, ASK, NEVER
    var isTemporaryChat by remember { mutableStateOf(false) }
    var selectedAiMode by remember { mutableStateOf("CHAT") }

    // Dialog & Sheet states
    var showHistorySheet by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<AiConversationEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    // Vault Attachment Pending State
    var attachedNoteTitle by remember { mutableStateOf<String?>(null) }
    var attachedNoteDecryptedText by remember { mutableStateOf<String?>(null) }
    var pendingDisclosure by remember { mutableStateOf<AiPermissionManager.Disclosure?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AegisBackground)
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showModeMenu = true }
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x3300E5FF))
                        .border(1.dp, AegisCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "AEGIS AI",
                        tint = AegisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AEGIS AI",
                            color = AegisTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x3300E5FF))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = selectedAiMode,
                                color = AegisCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = if (isTemporaryChat) "Incognito Ephemeral" else "Gemini 3.5 Grounded",
                        color = if (isTemporaryChat) Color(0xFFFFB74D) else AegisTextMuted,
                        fontSize = 11.sp
                    )
                }

                // AI Mode Dropdown Menu
                DropdownMenu(
                    expanded = showModeMenu,
                    onDismissRequest = { showModeMenu = false },
                    modifier = Modifier.background(AegisSurfaceElevated)
                ) {
                    val modes = listOf(
                        "CHAT" to "Normal Chat",
                        "SEARCH" to "Web Search Grounding",
                        "RESEARCH" to "Deep Research Mode",
                        "EXPLAIN" to "Explain Concept",
                        "SUMMARIZE" to "Summarize Content",
                        "REWRITE" to "Rewrite & Polish",
                        "STUDY" to "Study & Learn",
                        "VAULT" to "Vault Context Assistant"
                    )
                    for ((key, label) in modes) {
                        DropdownMenuItem(
                            text = { Text(label, color = if (selectedAiMode == key) AegisCyan else AegisTextPrimary) },
                            onClick = {
                                selectedAiMode = key
                                showModeMenu = false
                                coroutineScope.launch {
                                    aiRepository.createNewConversation(mode = key, isTemporary = isTemporaryChat, searchMode = searchMode)
                                }
                            }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Temporary / Ephemeral Chat toggle
                IconButton(
                    onClick = {
                        isTemporaryChat = !isTemporaryChat
                        coroutineScope.launch {
                            aiRepository.createNewConversation(
                                title = if (isTemporaryChat) "Incognito Session" else "New Chat",
                                mode = selectedAiMode,
                                isTemporary = isTemporaryChat,
                                searchMode = searchMode
                            )
                        }
                        Toast.makeText(
                            context,
                            if (isTemporaryChat) "Incognito Chat Enabled (Wiped on exit)" else "Standard Chat Active",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Temporary Chat",
                        tint = if (isTemporaryChat) Color(0xFFFFB74D) else AegisTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // History Button
                IconButton(
                    onClick = { showHistorySheet = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Chat History",
                        tint = AegisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Settings Button
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "AI Settings",
                        tint = AegisTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Ephemeral / Research Banner
        if (isTemporaryChat) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x33FFB74D))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🔒 Incognito Mode: Messages and tokens will vanish when you switch chats or close the app.",
                    color = Color(0xFFFFCC80),
                    fontSize = 11.sp
                )
            }
        } else if (selectedAiMode == "RESEARCH") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x2200E5FF))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🔬 Deep Research Workflow Active: Multi-step claim verification & web source cross-checking.",
                    color = AegisCyan,
                    fontSize = 11.sp
                )
            }
        }

        // --- CHAT MESSAGES AREA ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && !isGenerating) {
                // Empty state with quick prompts
                AegisAiEmptyState(
                    onSelectPrompt = { prompt, mode ->
                        selectedAiMode = mode
                        aiRepository.sendMessage(prompt, searchModeOverride = searchMode)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages) { msg ->
                        AegisChatMessageItem(
                            message = msg,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(msg.content))
                                Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                            },
                            onRegenerate = {
                                aiRepository.sendMessage(msg.content, searchModeOverride = searchMode)
                            }
                        )
                    }

                    if (isGenerating) {
                        item {
                            AegisTypingIndicator(researchStep = researchStep)
                        }
                    }
                }
            }
        }

        // --- ATTACHED CONTEXT PREVIEW (IF ATTACHED) ---
        if (attachedNoteTitle != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AegisSurfaceElevated)
                    .border(1.dp, Color(0x3300E5FF))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AegisCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Attached: $attachedNoteTitle (Level 1 Sanitized)",
                        color = AegisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = {
                        attachedNoteTitle = null
                        attachedNoteDecryptedText = null
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attachment",
                        tint = AegisTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // --- BOTTOM COMPOSER ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AegisSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Control row: Permission level badge + Search Mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Permission Level Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (attachedNoteTitle != null) AegisCyan else AegisTextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (attachedNoteTitle != null) "Level 1: Selected Note" else "Level 0: Isolated Chat",
                        color = if (attachedNoteTitle != null) AegisCyan else AegisTextMuted,
                        fontSize = 11.sp
                    )
                }

                // Web Search Mode Toggle Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x2200E5FF))
                        .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(6.dp))
                        .clickable {
                            searchMode = when (searchMode) {
                                "AUTO" -> "ALWAYS"
                                "ALWAYS" -> "NEVER"
                                "NEVER" -> "ASK"
                                else -> "AUTO"
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = AegisCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Web: $searchMode",
                            color = AegisCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment Button (+)
                IconButton(
                    onClick = { showAttachmentDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x2200E5FF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Attach Vault Note",
                        tint = AegisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Input Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Ask AEGIS AI or type in বাংলা...",
                            color = AegisTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AegisTextPrimary,
                        unfocusedTextColor = AegisTextPrimary,
                        focusedBorderColor = AegisCyan,
                        unfocusedBorderColor = AegisBorder,
                        cursorColor = AegisCyan
                    ),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send or Stop Button
                if (isGenerating) {
                    IconButton(
                        onClick = { aiRepository.stopGeneration() },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AegisRed.copy(alpha = 0.2f))
                            .border(1.dp, AegisRed, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop generation",
                            tint = AegisRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val text = inputText.trim()
                                val ctx = attachedNoteDecryptedText
                                val summary = if (attachedNoteTitle != null) "Note: '$attachedNoteTitle' (Level 1)" else null
                                inputText = ""
                                attachedNoteTitle = null
                                attachedNoteDecryptedText = null
                                aiRepository.sendMessage(
                                    userText = text,
                                    attachedContext = ctx,
                                    attachedSummary = summary,
                                    searchModeOverride = searchMode
                                )
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) AegisCyan else Color(0x3300E5FF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send message",
                            tint = if (inputText.isNotBlank()) Color.Black else AegisTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // --- VAULT ATTACHMENT PICKER DIALOG ---
    if (showAttachmentDialog) {
        AlertDialog(
            onDismissRequest = { showAttachmentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = AegisCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Attach Vault Note (Level 1)", color = AegisTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Select an encrypted note to temporarily share with AEGIS AI. Vault master keys and unselected files remain strictly isolated.",
                        color = AegisTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (notes.isEmpty()) {
                        Text(
                            text = "No notes found in your vault. Create a note in the Notes tab first.",
                            color = AegisTextMuted,
                            fontSize = 12.sp
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(notes) { note ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AegisSurfaceElevated)
                                        .border(1.dp, AegisBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            coroutineScope.launch {
                                                val decrypted = viewModel.decryptNote(note.encryptedContentBase64)
                                                pendingDisclosure = AiPermissionManager.createDisclosureForNote(note.title, decrypted)
                                                attachedNoteTitle = note.title
                                                attachedNoteDecryptedText = decrypted
                                                showAttachmentDialog = false
                                            }
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(text = note.title, color = AegisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(text = "Category: ${note.category}", color = AegisCyan, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAttachmentDialog = false }) {
                    Text("Cancel", color = AegisTextMuted)
                }
            },
            containerColor = Color(0xFF0F1823)
        )
    }

    // --- SECURITY DISCLOSURE CONFIRMATION DIALOG ---
    pendingDisclosure?.let { disclosure ->
        AlertDialog(
            onDismissRequest = {
                pendingDisclosure = null
                attachedNoteTitle = null
                attachedNoteDecryptedText = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(disclosure.title, color = AegisTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(text = disclosure.description, color = AegisTextPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF040A12))
                            .border(1.dp, AegisBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(text = disclosure.previewSnippet, color = AegisTextMuted, fontSize = 11.sp, maxLines = 4)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = disclosure.warning, color = Color(0xFFFFB74D), fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDisclosure = null
                        Toast.makeText(context, "Note attached with Level 1 permission", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisCyan, contentColor = Color.Black)
                ) {
                    Text("Authorize & Attach", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDisclosure = null
                        attachedNoteTitle = null
                        attachedNoteDecryptedText = null
                    }
                ) {
                    Text("Cancel", color = AegisTextMuted)
                }
            },
            containerColor = Color(0xFF0F1823)
        )
    }

    // --- CHAT HISTORY MODAL BOTTOM SHEET ---
    if (showHistorySheet) {
        val sheetState = rememberModalBottomSheetState()
        var historySearchQuery by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF0F1823)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Conversations History", color = AegisTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                aiRepository.createNewConversation()
                                showHistorySheet = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisCyan, contentColor = Color.Black)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Chat", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = historySearchQuery,
                    onValueChange = { historySearchQuery = it },
                    placeholder = { Text("Search conversations...", color = AegisTextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AegisTextPrimary,
                        unfocusedTextColor = AegisTextPrimary,
                        focusedBorderColor = AegisCyan,
                        unfocusedBorderColor = AegisBorder
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filteredConversations = conversations.filter {
                    it.title.contains(historySearchQuery, ignoreCase = true)
                }

                if (filteredConversations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No saved conversations.", color = AegisTextMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredConversations) { conv ->
                            val isCurrent = conv.id == currentConversation?.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) Color(0x3300E5FF) else AegisSurfaceElevated)
                                    .border(1.dp, if (isCurrent) AegisCyan else AegisBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        aiRepository.selectConversation(conv.id)
                                        showHistorySheet = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = conv.title,
                                            color = AegisTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(conv.updatedAt)),
                                            color = AegisTextMuted,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                renameText = conv.title
                                                showRenameDialog = conv
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename", tint = AegisCyan, modifier = Modifier.size(14.dp))
                                        }

                                        IconButton(
                                            onClick = { aiRepository.deleteConversation(conv.id) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AegisRed, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = {
                        aiRepository.deleteAllConversations()
                        showHistorySheet = false
                        Toast.makeText(context, "All conversations cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear All AI History", color = AegisRed, fontSize = 12.sp)
                }
            }
        }
    }

    // --- RENAME DIALOG ---
    showRenameDialog?.let { conv ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Chat", color = AegisTextPrimary, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AegisTextPrimary, unfocusedTextColor = AegisTextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            aiRepository.renameConversation(conv.id, renameText.trim())
                            showRenameDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisCyan, contentColor = Color.Black)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("Cancel", color = AegisTextMuted) }
            },
            containerColor = Color(0xFF0F1823)
        )
    }

    // --- AI SETTINGS & CLOUD AUTH DIALOG ---
    if (showSettingsDialog) {
        AegisAiSettingsDialog(
            aiRepository = aiRepository,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun AegisChatMessageItem(
    message: AiMessageEntity,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == "user"

    // Parse citations JSON if any
    val citations = remember(message.citationsJson) {
        if (message.citationsJson.isNullOrBlank()) emptyList()
        else {
            try {
                val arr = JSONArray(message.citationsJson)
                val list = mutableListOf<AiCitationItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        AiCitationItem(
                            title = obj.optString("title", ""),
                            url = obj.optString("url", ""),
                            domain = obj.optString("domain", "web"),
                            snippet = obj.optString("snippet", "")
                        )
                    )
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0x3300E5FF))
                    .border(1.dp, AegisCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = AegisCyan, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Attached context badge (if any)
            if (!message.attachedContextSummary.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x3300E5FF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = message.attachedContextSummary,
                        color = AegisCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) Color(0x3300E5FF) else AegisSurfaceElevated)
                    .border(1.dp, if (isUser) AegisCyan.copy(alpha = 0.5f) else AegisBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column {
                    if (message.status == "ERROR") {
                        Text(text = message.content, color = AegisRed, fontSize = 13.sp)
                    } else {
                        AegisMarkdownView(text = message.content)
                    }

                    // Sources Cards (Google Search Grounding)
                    if (citations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        AegisSourcesRow(citations = citations)
                    }
                }
            }

            // Action row under assistant message
            if (!isUser && message.content.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = AegisTextMuted, modifier = Modifier.size(12.dp))
                    }
                    IconButton(onClick = onRegenerate, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate", tint = AegisTextMuted, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AegisAiEmptyState(
    onSelectPrompt: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0x3300E5FF))
                .border(2.dp, AegisCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = AegisCyan, modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AEGIS AI CORE",
            color = AegisTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Isolated Privacy Boundary • Real-Time Web Intelligence • বাংলা & English",
            color = AegisCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Prompt Chips
        val promptChips = listOf(
            Triple("🔍 Web Intelligence", "What are the latest privacy and zero-knowledge breakthroughs?", "SEARCH"),
            Triple("🔬 Deep Research", "Perform research on Post-Quantum Cryptography standards (ML-KEM, Kyber).", "RESEARCH"),
            Triple("🇧🇩 বাংলায় জানুন", "ভল্টের অফলাইন AES-256-GCM এনক্রিপশন কীভাবে কাজ করে ব্যাখ্যা করুন।", "CHAT"),
            Triple("🛡️ Security Audit", "Explain the difference between Level 0 isolated chat and Level 1 note attachment.", "EXPLAIN")
        )

        for ((label, prompt, mode) in promptChips) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AegisSurfaceElevated)
                    .border(1.dp, AegisBorder, RoundedCornerShape(10.dp))
                    .clickable { onSelectPrompt(prompt, mode) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = label, color = AegisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = prompt, color = AegisTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = AegisCyan, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun AegisAiSettingsDialog(
    aiRepository: AiRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cloudAuth = aiRepository.cloudAuthManager
    val isLoggedIn by cloudAuth.isLoggedIn.collectAsState()
    val userEmail by cloudAuth.userEmail.collectAsState()
    val backendUrl by cloudAuth.backendUrl.collectAsState()
    val sessions by cloudAuth.sessions.collectAsState()

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var customUrlInput by remember { mutableStateOf(backendUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = AegisCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AEGIS AI Settings & Cloud", color = AegisTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(340.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(text = "Cloud Account System", color = AegisCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Separate from your vault encryption keys. Used for cross-device AI synchronization.",
                        color = AegisTextMuted,
                        fontSize = 11.sp
                    )
                }

                if (isLoggedIn) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AegisSurfaceElevated)
                                .border(1.dp, AegisBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(text = "Active Cloud Session", color = AegisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = userEmail ?: "Authenticated", color = AegisCyan, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        cloudAuth.logout()
                                        Toast.makeText(context, "Logged out from AEGIS Cloud", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AegisRed.copy(alpha = 0.2f), contentColor = AegisRed)
                                ) {
                                    Text("Log Out")
                                }
                            }
                        }
                    }

                    item {
                        Text(text = "Active Devices (${sessions.size})", color = AegisTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        for (s in sessions) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AegisSurfaceElevated)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = s.deviceName, color = AegisTextPrimary, fontSize = 11.sp)
                                        Text(text = s.sessionId, color = AegisTextMuted, fontSize = 9.sp)
                                    }
                                    TextButton(onClick = { cloudAuth.revokeSession(s.sessionId) }) {
                                        Text("Revoke", color = AegisRed, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email", color = AegisTextMuted, fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AegisTextPrimary, unfocusedTextColor = AegisTextPrimary)
                            )
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password (min 8 chars)", color = AegisTextMuted, fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AegisTextPrimary, unfocusedTextColor = AegisTextPrimary)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val res = cloudAuth.login(emailInput, passwordInput)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Logged in to AEGIS Cloud", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, res.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = AegisCyan, contentColor = Color.Black)
                                ) {
                                    Text("Login")
                                }
                                Button(
                                    onClick = {
                                        val res = cloudAuth.register(emailInput, passwordInput, "Agent")
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Registered & logged in", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, res.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF), contentColor = AegisCyan)
                                ) {
                                    Text("Register")
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "AEGIS Backend Endpoint", color = AegisCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = {
                            customUrlInput = it
                            cloudAuth.setBackendUrl(it)
                        },
                        label = { Text("API Base URL", color = AegisTextMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AegisTextPrimary, unfocusedTextColor = AegisTextPrimary)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AegisCyan, contentColor = Color.Black)) {
                Text("Done")
            }
        },
        containerColor = Color(0xFF0F1823)
    )
}
