package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.AegisGlassCard
import com.example.ui.components.AegisSecurityBadge
import com.example.ui.components.SecurityBadgeStatus
import com.example.ui.theme.AegisAmber
import com.example.ui.theme.AegisBackground
import com.example.ui.theme.AegisBorder
import com.example.ui.theme.AegisBorderGlow
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmerald
import com.example.ui.theme.AegisRose
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisSurfaceGlass
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary

@Composable
fun BackupScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current

    var activeTab by remember { mutableStateOf("CREATE") } // "CREATE" or "RESTORE"

    // Create Backup state
    var backupPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }
    var targetExportUri by remember { mutableStateOf<Uri?>(null) }

    // Restore Backup state
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var restorePassword by remember { mutableStateOf("") }
    var restoreError by remember { mutableStateOf<String?>(null) }
    var restoreSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    // Launcher for saving .aegis backup
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null && backupPassword.isNotBlank()) {
            viewModel.createBackup(backupPassword, uri) { success, msg ->
                if (success) {
                    Toast.makeText(context, "Encrypted backup exported successfully!", Toast.LENGTH_LONG).show()
                    backupPassword = ""
                    confirmPassword = ""
                    createError = null
                } else {
                    createError = msg
                }
            }
        }
    }

    // Launcher for selecting backup to restore
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedRestoreUri = uri
        restoreError = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AegisBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
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
                    text = "BACKUP & RESTORE",
                    color = AegisTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Proprietary PVBACKUP Container Format",
                    color = AegisTextMuted,
                    fontSize = 12.sp
                )
            }
            AegisSecurityBadge(text = "PVBACKUP v1", status = SecurityBadgeStatus.CYBER)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs: Create vs Restore
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("CREATE" to "Create Encrypted Archive", "RESTORE" to "Restore from Archive")
            for ((key, label) in tabs) {
                val isSelected = activeTab == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AegisCyan else AegisSurfaceElevated)
                        .border(1.dp, if (isSelected) AegisCyan else AegisBorder, RoundedCornerShape(10.dp))
                        .clickable { activeTab = key }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
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

        Spacer(modifier = Modifier.height(20.dp))

        if (activeTab == "CREATE") {
            // Create Backup Form
            AegisGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AegisCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = AegisCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Export Vault Archive (.aegis)",
                                color = AegisTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sealed with PBKDF2 + AES-256-GCM authenticated cipher",
                                color = AegisTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Set a dedicated backup passphrase. You will need this exact passphrase to restore your vault on any device.",
                        color = AegisTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Backup Passphrase", color = AegisTextMuted) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisCyan,
                            unfocusedBorderColor = AegisBorder,
                            focusedTextColor = AegisTextPrimary,
                            unfocusedTextColor = AegisTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("backup_password_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Passphrase", color = AegisTextMuted) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisCyan,
                            unfocusedBorderColor = AegisBorder,
                            focusedTextColor = AegisTextPrimary,
                            unfocusedTextColor = AegisTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("backup_confirm_password_input")
                    )

                    if (createError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = createError!!,
                            color = AegisRose,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (backupPassword.length < 6) {
                                createError = "Passphrase must be at least 6 characters."
                            } else if (backupPassword != confirmPassword) {
                                createError = "Passphrases do not match."
                            } else {
                                createError = null
                                val defaultName = "Aegis_Vault_Backup_${System.currentTimeMillis()}.aegis"
                                createDocLauncher.launch(defaultName)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_backup_button")
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFF042F3D))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Encrypted Archive", color = Color(0xFF042F3D), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Restore Backup Form
            AegisGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AegisEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = AegisEmerald, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Restore from .aegis Archive",
                                color = AegisTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pre-flight validation & transactional rollback",
                                color = AegisTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 1: Select Archive File
                    Button(
                        onClick = { openDocLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisSurfaceElevated),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, AegisBorder, RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, tint = AegisCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedRestoreUri != null) "Archive Selected" else "Select .aegis Backup File",
                            color = AegisTextPrimary
                        )
                    }

                    if (selectedRestoreUri != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = restorePassword,
                            onValueChange = { restorePassword = it },
                            label = { Text("Archive Decryption Passphrase", color = AegisTextMuted) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisEmerald,
                                unfocusedBorderColor = AegisBorder,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("restore_password_input")
                        )

                        if (restoreError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = restoreError!!,
                                color = AegisRose,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (restoreSuccessMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = restoreSuccessMessage!!,
                                color = AegisEmerald,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (restorePassword.isBlank()) {
                                    restoreError = "Please enter backup passphrase."
                                    return@Button
                                }
                                isRestoring = true
                                restoreError = null
                                viewModel.restoreBackup(selectedRestoreUri!!, restorePassword) { success, msg ->
                                    isRestoring = false
                                    if (success) {
                                        restoreSuccessMessage = msg
                                        restorePassword = ""
                                    } else {
                                        restoreError = msg
                                    }
                                }
                            },
                            enabled = !isRestoring,
                            colors = ButtonDefaults.buttonColors(containerColor = AegisEmerald),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("confirm_restore_button")
                        ) {
                            Text(
                                text = if (isRestoring) "Verifying & Restoring..." else "Validate & Restore Archive",
                                color = Color(0xFF022C22),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Security Notice
        AegisGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = AegisCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Backup Encryption Guarantee", color = AegisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Project Aegis backup containers do not contain any plaintext files or raw master keys. The backup key is derived independently using PBKDF2 (65,536 iterations), keeping your files authenticated and safe even if exported to external drives or untrusted cloud storage.",
                    color = AegisTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
