package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.components.AegisGlassCard
import com.example.ui.components.AegisSecurityBadge
import com.example.ui.components.SecurityBadgeStatus
import com.example.ui.theme.AegisAmber
import com.example.ui.theme.AegisBackground
import com.example.ui.theme.AegisBorder
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisRose
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisSurfaceGlass
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Composable
fun FileViewerScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val viewingFile by viewModel.viewingDecryptedFile.collectAsState()
    val viewingObject by viewModel.viewingObject.collectAsState()

    var showExportWarningDialog by remember { mutableStateOf(false) }

    val file = viewingFile
    val obj = viewingObject

    // SAF CreateDocument launcher for secure export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(obj?.mimeType ?: "*/*")
    ) { uri ->
        if (uri != null && file != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(file).use { inp -> inp.copyTo(out) }
                }
                Toast.makeText(context, "Exported: ${obj?.originalName}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (file == null || obj == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(AegisBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("No decrypted object in active session", color = AegisTextMuted)
        }
        return
    }

    val isImage = obj.category == "PHOTO" || obj.mimeType.startsWith("image/")
    val isText = obj.mimeType.startsWith("text/") || obj.plainDisplayExtension in listOf("txt", "md", "json", "xml", "log", "csv")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AegisBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AegisSurfaceGlass)
                    .border(1.dp, AegisBorder, RoundedCornerShape(0.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        viewModel.closeViewer()
                        onClose()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AegisCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = obj.originalName,
                            color = AegisTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AegisSecurityBadge(text = "EPHEMERAL DECRYPTED", status = SecurityBadgeStatus.WARNING)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatBytes(obj.fileSizeBytes),
                                color = AegisTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = { showExportWarningDialog = true },
                        modifier = Modifier.testTag("export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export to storage",
                            tint = AegisCyan
                        )
                    }
                }
            }

            // Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isImage -> {
                        AsyncImage(
                            model = file,
                            contentDescription = obj.originalName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    isText -> {
                        val textContent = remember(file) {
                            try {
                                file.readText(Charsets.UTF_8).take(10000)
                            } catch (e: Exception) {
                                "[Unable to preview text encoding]"
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AegisSurfaceElevated)
                                .border(1.dp, AegisBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = textContent,
                                color = AegisTextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(AegisSurfaceElevated)
                                    .border(1.dp, AegisCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = AegisCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = obj.originalName,
                                color = AegisTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "MIME: ${obj.mimeType} (${formatBytes(obj.fileSizeBytes)})",
                                color = AegisTextMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, obj.mimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Open Protected File"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AegisCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Open in System Viewer", color = Color(0xFF042F3D), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Bottom scrub note
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AegisSurfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "🔒 Zero-Persistence: Ephemeral decrypted cache is automatically wiped upon closing or locking vault.",
                    color = AegisTextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Export Warning Dialog
        if (showExportWarningDialog) {
            AlertDialog(
                onDismissRequest = { showExportWarningDialog = false },
                containerColor = AegisSurfaceElevated,
                icon = {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AegisAmber)
                },
                title = {
                    Text("Export Plaintext Warning", color = AegisTextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        text = "Exporting will save a plaintext unencrypted copy of '${obj.originalName}' into your public device storage. The exported file will no longer be protected by Project Aegis cryptographic controls.",
                        color = AegisTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExportWarningDialog = false
                            exportLauncher.launch(obj.originalName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisAmber)
                    ) {
                        Text("Export to Storage", color = Color(0xFF261A00), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportWarningDialog = false }) {
                        Text("Cancel", color = AegisTextSecondary)
                    }
                }
            )
        }
    }
}
