package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SecureNoteEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AegisEmptyState
import com.example.ui.theme.AegisAmber
import com.example.ui.theme.AegisBackground
import com.example.ui.theme.AegisBorder
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmerald
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisRose
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisSurfaceGlass
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    viewModel: MainViewModel
) {
    val notes by viewModel.notes.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    var editingNote by remember { mutableStateOf<SecureNoteEntity?>(null) }
    var isCreatingNewNote by remember { mutableStateOf(false) }

    // Dialog state
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var noteCategory by remember { mutableStateOf("GENERAL") }
    var noteIsPinned by remember { mutableStateOf(false) }

    val filteredNotes = notes.filter { note ->
        val matchesCategory = (selectedCategory == "ALL" || note.category == selectedCategory)
        val matchesSearch = note.title.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AegisBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SECURE NOTES",
                        color = AegisTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${filteredNotes.size} encrypted note(s)",
                        color = AegisTextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search note titles...", color = AegisTextMuted, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = AegisEmerald) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AegisEmerald,
                    unfocusedBorderColor = AegisBorder,
                    focusedTextColor = AegisTextPrimary,
                    unfocusedTextColor = AegisTextPrimary
                ),
                modifier = Modifier.fillMaxWidth().testTag("notes_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf("ALL", "GENERAL", "WORK", "FINANCE", "PERSONAL")
                for (cat in categories) {
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AegisEmerald else AegisSurfaceElevated)
                            .border(1.dp, if (isSelected) AegisEmerald else AegisBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color(0xFF022C22) else AegisTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notes List
            if (filteredNotes.isEmpty()) {
                AegisEmptyState(
                    title = "No Secure Notes",
                    description = "Keep ideas, recovery phrases, or private documents encrypted with AES-256-GCM.",
                    icon = Icons.Default.Note,
                    actionText = "Create New Note",
                    onActionClick = {
                        noteTitle = ""
                        noteContent = ""
                        noteCategory = "GENERAL"
                        noteIsPinned = false
                        isCreatingNewNote = true
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            viewModel = viewModel,
                            onClick = {
                                editingNote = note
                                noteTitle = note.title
                                noteCategory = note.category
                                noteIsPinned = note.isPinned
                                // Decrypt content for editing
                                noteContent = "" // loaded in LaunchedEffect
                            },
                            onDelete = { viewModel.deleteNote(note.id) }
                        )
                    }
                }
            }
        }

        // Add Note FAB
        FloatingActionButton(
            onClick = {
                noteTitle = ""
                noteContent = ""
                noteCategory = "GENERAL"
                noteIsPinned = false
                isCreatingNewNote = true
            },
            containerColor = AegisEmerald,
            contentColor = Color(0xFF022C22),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_note_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Create Secure Note")
        }

        // Add / Edit Dialog
        val isOpen = isCreatingNewNote || editingNote != null
        if (isOpen) {
            LaunchedEffect(editingNote) {
                if (editingNote != null) {
                    noteContent = viewModel.decryptNote(editingNote!!.encryptedContentBase64)
                }
            }

            AlertDialog(
                onDismissRequest = {
                    isCreatingNewNote = false
                    editingNote = null
                },
                containerColor = AegisSurfaceElevated,
                title = {
                    Text(
                        text = if (isCreatingNewNote) "Create Secure Note" else "Edit Secure Note",
                        color = AegisTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = noteTitle,
                            onValueChange = { noteTitle = it },
                            label = { Text("Title", color = AegisTextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisEmerald,
                                unfocusedBorderColor = AegisBorder,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                        )

                        // Category selection
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val editCats = listOf("GENERAL", "WORK", "FINANCE", "PERSONAL")
                            for (cat in editCats) {
                                val isSelected = noteCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AegisEmerald else AegisBackground)
                                        .border(1.dp, if (isSelected) AegisEmerald else AegisBorder, RoundedCornerShape(8.dp))
                                        .clickable { noteCategory = cat }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (isSelected) Color(0xFF022C22) else AegisTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Pinned switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pin to top", color = AegisTextSecondary, fontSize = 13.sp)
                            Switch(
                                checked = noteIsPinned,
                                onCheckedChange = { noteIsPinned = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = AegisEmerald, checkedTrackColor = Color(0x4410B981))
                            )
                        }

                        // Content
                        OutlinedTextField(
                            value = noteContent,
                            onValueChange = { noteContent = it },
                            label = { Text("Encrypted Content", color = AegisTextMuted) },
                            minLines = 6,
                            maxLines = 10,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisEmerald,
                                unfocusedBorderColor = AegisBorder,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("note_content_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveNote(
                                id = editingNote?.id,
                                title = noteTitle,
                                content = noteContent,
                                category = noteCategory,
                                isPinned = noteIsPinned
                            ) {
                                isCreatingNewNote = false
                                editingNote = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisEmerald),
                        modifier = Modifier.testTag("save_note_button")
                    ) {
                        Text("Save Note", color = Color(0xFF022C22), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        isCreatingNewNote = false
                        editingNote = null
                    }) {
                        Text("Cancel", color = AegisTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun NoteCardItem(
    note: SecureNoteEntity,
    viewModel: MainViewModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var decryptedSnippet by remember { mutableStateOf("Decrypting...") }

    LaunchedEffect(note.encryptedContentBase64) {
        val plain = viewModel.decryptNote(note.encryptedContentBase64)
        decryptedSnippet = plain.take(80)
    }

    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(note.updatedAt))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AegisSurfaceGlass)
            .border(1.dp, if (note.isPinned) AegisEmerald.copy(alpha = 0.5f) else AegisBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = AegisEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = note.title,
                        color = AegisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete note",
                        tint = AegisTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = decryptedSnippet,
                color = AegisTextSecondary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AegisSurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = note.category,
                            color = AegisEmerald,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateStr,
                        color = AegisTextMuted,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AegisSurfaceElevated)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "AES-GCM",
                        color = AegisCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
