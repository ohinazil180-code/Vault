package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.model.VaultObjectEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AegisEmptyState
import com.example.ui.components.AegisGlassCard
import com.example.ui.components.AegisSecurityBadge
import com.example.ui.components.SecurityBadgeStatus
import com.example.ui.theme.AegisAmber
import com.example.ui.theme.AegisBackground
import com.example.ui.theme.AegisBlue
import com.example.ui.theme.AegisBorder
import com.example.ui.theme.AegisBorderGlow
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
fun VaultScreen(
    viewModel: MainViewModel
) {
    val activeObjects by viewModel.activeObjects.collectAsState()
    val recycleBinObjects by viewModel.recycleBinObjects.collectAsState()

    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(false) }

    var selectedDetailObject by remember { mutableStateOf<VaultObjectEntity?>(null) }
    var renameObjectTarget by remember { mutableStateOf<VaultObjectEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris) {}
        }
    }

    val displayObjects = if (selectedCategory == "BIN") {
        recycleBinObjects.filter {
            it.originalName.contains(searchQuery, ignoreCase = true)
        }
    } else {
        activeObjects.filter { obj ->
            val matchesCategory = when (selectedCategory) {
                "ALL" -> true
                "PHOTO" -> obj.category == "PHOTO"
                "VIDEO" -> obj.category == "VIDEO"
                "DOCUMENT" -> obj.category == "DOCUMENT"
                else -> obj.category == "OTHER"
            }
            matchesCategory && obj.originalName.contains(searchQuery, ignoreCase = true)
        }
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

            // Header & View Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ENCRYPTED VAULT",
                        color = AegisTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${displayObjects.size} objects in view",
                        color = AegisTextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier.testTag("toggle_view_mode")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Grid/List View",
                            tint = AegisCyan
                        )
                    }

                    if (selectedCategory == "BIN" && recycleBinObjects.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.emptyTrash() },
                            modifier = Modifier.testTag("empty_bin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Empty Bin",
                                tint = AegisRose
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search encrypted objects...", color = AegisTextMuted, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = AegisCyan)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AegisCyan,
                    unfocusedBorderColor = AegisBorder,
                    focusedTextColor = AegisTextPrimary,
                    unfocusedTextColor = AegisTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vault_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    "ALL" to "All",
                    "PHOTO" to "Photos",
                    "VIDEO" to "Videos",
                    "DOCUMENT" to "Docs",
                    "BIN" to "Bin (${recycleBinObjects.size})"
                )

                for ((key, label) in categories) {
                    val isSelected = selectedCategory == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AegisCyan else AegisSurfaceElevated)
                            .border(1.dp, if (isSelected) AegisCyan else AegisBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedCategory = key }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF042F3D) else AegisTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Object List / Grid
            if (displayObjects.isEmpty()) {
                AegisEmptyState(
                    title = if (selectedCategory == "BIN") "Recycle Bin Empty" else "No Files Found",
                    description = if (selectedCategory == "BIN")
                        "Deleted vault objects are kept in the Recycle Bin until permanently removed."
                    else
                        "Import files from device storage to protect them with per-file AES-256-GCM encryption.",
                    icon = Icons.Default.Description,
                    actionText = if (selectedCategory != "BIN") "Import Files Now" else null,
                    onActionClick = if (selectedCategory != "BIN") {
                        { filePickerLauncher.launch(arrayOf("*/*")) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(displayObjects, key = { it.id }) { obj ->
                            VaultGridItem(
                                obj = obj,
                                isBin = selectedCategory == "BIN",
                                onClick = {
                                    if (selectedCategory != "BIN") {
                                        viewModel.openObjectForViewing(obj)
                                    } else {
                                        selectedDetailObject = obj
                                    }
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(obj) },
                                onDetails = { selectedDetailObject = obj },
                                onRename = {
                                    renameObjectTarget = obj
                                    renameInput = obj.originalName
                                },
                                onDelete = { viewModel.moveToTrash(obj.id) },
                                onRestore = { viewModel.restoreFromTrash(obj.id) },
                                onPermanentDelete = { viewModel.permanentDelete(obj.id) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(displayObjects, key = { it.id }) { obj ->
                            VaultListItem(
                                obj = obj,
                                isBin = selectedCategory == "BIN",
                                onClick = {
                                    if (selectedCategory != "BIN") {
                                        viewModel.openObjectForViewing(obj)
                                    } else {
                                        selectedDetailObject = obj
                                    }
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(obj) },
                                onDetails = { selectedDetailObject = obj },
                                onRename = {
                                    renameObjectTarget = obj
                                    renameInput = obj.originalName
                                },
                                onDelete = { viewModel.moveToTrash(obj.id) },
                                onRestore = { viewModel.restoreFromTrash(obj.id) },
                                onPermanentDelete = { viewModel.permanentDelete(obj.id) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button for Import
        if (selectedCategory != "BIN") {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                containerColor = AegisCyan,
                contentColor = Color(0xFF042F3D),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("import_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Import Protected File")
            }
        }

        // Details Modal Dialog
        if (selectedDetailObject != null) {
            val obj = selectedDetailObject!!
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr = dateFormat.format(Date(obj.createdAt))

            AlertDialog(
                onDismissRequest = { selectedDetailObject = null },
                containerColor = AegisSurfaceElevated,
                title = {
                    Text(
                        text = "Cryptographic Object Details",
                        color = AegisTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow("Name", obj.originalName)
                        DetailRow("Category", obj.category)
                        DetailRow("MIME Type", obj.mimeType)
                        DetailRow("Ciphertext Size", formatBytes(obj.fileSizeBytes))
                        DetailRow("Import Date", dateStr)
                        DetailRow("Object UUID", obj.id.take(16) + "...")
                        DetailRow("Storage Shard", obj.storageRelativePath)
                        DetailRow("SHA-256 Fingerprint", obj.checksumSha256.take(16) + "...")
                        DetailRow("Cipher Spec", "AES-256-GCM / 96-bit IV")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedDetailObject = null },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisCyan)
                    ) {
                        Text("Close", color = Color(0xFF042F3D), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Rename Dialog
        if (renameObjectTarget != null) {
            AlertDialog(
                onDismissRequest = { renameObjectTarget = null },
                containerColor = AegisSurfaceElevated,
                title = {
                    Text(
                        text = "Rename Protected Object",
                        color = AegisTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Updating name only changes protected metadata. Ciphertext on disk remains untouched.",
                            color = AegisTextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisCyan,
                                unfocusedBorderColor = AegisBorder,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (renameInput.isNotBlank()) {
                                viewModel.renameObject(renameObjectTarget!!.id, renameInput.trim())
                            }
                            renameObjectTarget = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisCyan)
                    ) {
                        Text("Save", color = Color(0xFF042F3D), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameObjectTarget = null }) {
                        Text("Cancel", color = AegisTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun VaultListItem(
    obj: VaultObjectEntity,
    isBin: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDetails: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val icon = when (obj.category) {
        "PHOTO" -> Icons.Default.Image
        "VIDEO" -> Icons.Default.Description
        else -> Icons.Default.Description
    }
    val accentColor = when (obj.category) {
        "PHOTO" -> AegisCyan
        "VIDEO" -> AegisPurple
        else -> AegisEmerald
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AegisSurfaceGlass)
            .border(1.dp, AegisBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = obj.originalName,
                    color = AegisTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatBytes(obj.fileSizeBytes),
                        color = AegisTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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

            if (!isBin) {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (obj.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (obj.isFavorite) AegisAmber else AegisTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options", tint = AegisTextSecondary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(AegisSurfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Details & Integrity", color = AegisTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Info, null, tint = AegisCyan) },
                        onClick = {
                            showMenu = false
                            onDetails()
                        }
                    )
                    if (!isBin) {
                        DropdownMenuItem(
                            text = { Text("Rename", color = AegisTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null, tint = AegisTextSecondary) },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Bin", color = AegisRose) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = AegisRose) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Restore", color = AegisEmerald) },
                            leadingIcon = { Icon(Icons.Default.Restore, null, tint = AegisEmerald) },
                            onClick = {
                                showMenu = false
                                onRestore()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Permanently Erase", color = AegisRose) },
                            leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = AegisRose) },
                            onClick = {
                                showMenu = false
                                onPermanentDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultGridItem(
    obj: VaultObjectEntity,
    isBin: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDetails: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val icon = when (obj.category) {
        "PHOTO" -> Icons.Default.Image
        "VIDEO" -> Icons.Default.Description
        else -> Icons.Default.Description
    }
    val accentColor = when (obj.category) {
        "PHOTO" -> AegisCyan
        "VIDEO" -> AegisPurple
        else -> AegisEmerald
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AegisSurfaceGlass)
            .border(1.dp, AegisBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onDetails,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = AegisTextMuted, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = obj.originalName,
                color = AegisTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatBytes(obj.fileSizeBytes),
                color = AegisTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, color = AegisTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            color = AegisTextPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
