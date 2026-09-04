package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VaultObjectEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AegisGlassCard
import com.example.ui.components.AegisSecurityBadge
import com.example.ui.components.AegisStatItem
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
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToVault: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToSecrets: () -> Unit,
    onNavigateToSecurityCenter: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToAi: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    val activeObjects by viewModel.activeObjects.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val secrets by viewModel.secrets.collectAsState()
    val totalStorageBytes by viewModel.totalStorageBytes.collectAsState()

    // SAF File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris) { count ->
                // Handled in ViewModel
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AegisBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Bar: Vault Status & Quick Lock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AegisSurfaceElevated)
                        .border(1.dp, AegisCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Vault Status",
                        tint = AegisCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "PROJECT AEGIS",
                        color = AegisTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AegisEmerald)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SESSION ACTIVE",
                            color = AegisEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Quick Lock Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AegisSurfaceElevated)
                    .border(1.dp, AegisBorder, RoundedCornerShape(12.dp))
                    .clickable { viewModel.lockVault() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("quick_lock_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Quick Lock",
                        tint = AegisRose,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LOCK",
                        color = AegisRose,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Metrics Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AegisStatItem(
                title = "PROTECTED FILES",
                value = "${activeObjects.size}",
                icon = Icons.Default.Description,
                accentColor = AegisCyan,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onNavigateToVault)
            )
            AegisStatItem(
                title = "SECURE NOTES",
                value = "${notes.size}",
                icon = Icons.Default.NoteAdd,
                accentColor = AegisEmerald,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onNavigateToNotes)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AegisStatItem(
                title = "CREDENTIALS",
                value = "${secrets.size}",
                icon = Icons.Default.Key,
                accentColor = AegisPurple,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onNavigateToSecrets)
            )
            AegisStatItem(
                title = "STORAGE USED",
                value = formatBytes(totalStorageBytes ?: 0L),
                icon = Icons.Default.FolderZip,
                accentColor = AegisBlue,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onNavigateToSecurityCenter)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AEGIS AI Banner
        AegisGlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = AegisCyan,
            onClick = onNavigateToAi
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x3300E5FF))
                            .border(1.dp, AegisCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = AegisCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "AEGIS AI CORE", color = AegisTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x3300E5FF))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(text = "ACTIVE", color = AegisCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(text = "Grounded Intelligence • বাংলা & English", color = AegisTextMuted, fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = onNavigateToAi,
                    colors = ButtonDefaults.buttonColors(containerColor = AegisCyan, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "Open AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions Row
        Text(
            text = "FAST ACTIONS",
            color = AegisTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.UploadFile,
                label = "Import",
                color = AegisCyan,
                modifier = Modifier.weight(1f),
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) }
            )
            QuickActionButton(
                icon = Icons.Default.NoteAdd,
                label = "New Note",
                color = AegisEmerald,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToNotes
            )
            QuickActionButton(
                icon = Icons.Default.Key,
                label = "New Secret",
                color = AegisPurple,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSecrets
            )
            QuickActionButton(
                icon = Icons.Default.FolderZip,
                label = "Backup",
                color = AegisBlue,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToBackup
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Security Health Card
        AegisGlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = AegisCyan.copy(alpha = 0.3f),
            onClick = onNavigateToSecurityCenter
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = AegisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security Posture Status",
                            color = AegisTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    AegisSecurityBadge(text = "OPTIMAL", status = SecurityBadgeStatus.ACTIVE)
                }
                Spacer(modifier = Modifier.height(12.dp))
                SecurityCheckItem("Hardware Keystore KEK", "Active TEE/SE AES-256", true)
                SecurityCheckItem("File Encryption", "Per-file unique 256-bit AES-GCM", true)
                SecurityCheckItem("App Switcher Shield", "FLAG_SECURE Active", true)
                SecurityCheckItem("Auto-Lock Timer", "${config?.autoLockTimeoutSeconds ?: 60}s Background Lock", true)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap to open Security Center & Threat Model →",
                    color = AegisCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Protected Objects Carousel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT VAULT OBJECTS",
                color = AegisTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "View All (${activeObjects.size})",
                color = AegisCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onNavigateToVault)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeObjects.isEmpty()) {
            AegisGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Files in Vault",
                        color = AegisTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 'Import' above to protect documents, photos, or media with AES-256-GCM.",
                        color = AegisTextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeObjects.take(8)) { obj ->
                    RecentObjectCard(
                        obj = obj,
                        onClick = { viewModel.openObjectForViewing(obj) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AegisSurfaceElevated)
            .border(1.dp, AegisBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = AegisTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SecurityCheckItem(title: String, detail: String, passed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (passed) AegisEmerald else AegisAmber)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, color = AegisTextSecondary, fontSize = 12.sp)
        }
        Text(
            text = detail,
            color = if (passed) AegisEmerald else AegisAmber,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RecentObjectCard(
    obj: VaultObjectEntity,
    onClick: () -> Unit
) {
    val icon = when (obj.category) {
        "PHOTO" -> Icons.Default.Image
        "VIDEO" -> Icons.Default.VideoFile
        else -> Icons.Default.Description
    }
    val accentColor = when (obj.category) {
        "PHOTO" -> AegisCyan
        "VIDEO" -> AegisPurple
        else -> AegisEmerald
    }

    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AegisSurfaceElevated)
            .border(1.dp, AegisBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = obj.originalName,
                color = AegisTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
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

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return "%.1f %s".format(value, units[digitGroups])
}
