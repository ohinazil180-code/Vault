package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
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
fun SecurityCenterScreen(
    viewModel: MainViewModel
) {
    val activeObjects by viewModel.activeObjects.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val secrets by viewModel.secrets.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()
    val integrityReport by viewModel.integrityReport.collectAsState()
    val duplicates by viewModel.duplicates.collectAsState()

    var activeTab by remember { mutableStateOf("THREATS") } // "THREATS", "INTEGRITY", "LOGS"

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
                    text = "SECURITY CENTER",
                    color = AegisTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Project Aegis Threat Model & Storage Audit",
                    color = AegisTextMuted,
                    fontSize = 12.sp
                )
            }
            AegisSecurityBadge(text = "TEE VERIFIED", status = SecurityBadgeStatus.ACTIVE)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                "THREATS" to "Threat Model",
                "INTEGRITY" to "Storage & Audit",
                "LOGS" to "Activity Log (${activityLogs.size})"
            )
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

        when (activeTab) {
            "THREATS" -> {
                ThreatModelSection()
            }
            "INTEGRITY" -> {
                IntegritySection(viewModel, integrityReport, duplicates)
            }
            "LOGS" -> {
                ActivityLogsSection(viewModel, activityLogs)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ThreatModelSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "THREAT MITIGATION ARCHITECTURE (T1–T10)",
            color = AegisTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )

        val threats = listOf(
            ThreatItemData("T1: Physical Device Extraction", "Full disk dump or forensic acquisition of flash memory without user credentials.", "Mitigated: Android KeyStore hardware root key (TEE/SE) envelopes PBKDF2-derived master key with 65,536 iterations. Flash dumps yield unrecoverable ciphertext.", ThreatStatus.MITIGATED),
            ThreatItemData("T2: Forensic Storage Artifacts", "Recovering original filenames, metadata, and timestamps from filesystems.", "Mitigated: Sharded two-level UUID paths (/vault/objects/xx/yy/<uuid>.pvobj). Original names and MIME types exist exclusively inside encrypted metadata.", ThreatStatus.MITIGATED),
            ThreatItemData("T3: App Switcher Snooping", "Recent apps overview and screen recording capturing sensitive data.", "Mitigated: WindowManager.LayoutParams.FLAG_SECURE enforced across the application lifecycle. Previews and recordings are completely blocked.", ThreatStatus.MITIGATED),
            ThreatItemData("T4: Malicious OS Process", "Other applications attempting to read private vault files.", "Mitigated: Sandboxed internal app storage (Context.filesDir) inaccessible to other processes without root. Even with root, per-file AES-256-GCM keys remain encrypted.", ThreatStatus.MITIGATED),
            ThreatItemData("T5: Shoulder Surfing", "Visual observation during credential entry.", "Mitigated: Masked PIN dots, custom keypad without system accessibility logging, and one-tap instant Quick Lock.", ThreatStatus.MITIGATED),
            ThreatItemData("T6: Cloud Interception & Subpoenas", "Man-in-the-middle attacks or third-party server breaches.", "Mitigated: Strict Local-First architecture. Zero mandatory cloud accounts, zero analytics trackers, zero external sync endpoints.", ThreatStatus.MITIGATED),
            ThreatItemData("T7: Clipboard Data Leaks", "Background services sniffing copied passwords or notes.", "Mitigated: 30-second automated clipboard memory scrubbing timer. Plaintext is destroyed from clipboard automatically.", ThreatStatus.MITIGATED),
            ThreatItemData("T8: Memory & RAM Dumps", "Cold boot memory extraction or debug inspection.", "Mitigated: Ephemeral in-memory master key with explicit zeroization (Arrays.fill byte wipe) upon lock or app backgrounding.", ThreatStatus.MITIGATED),
            ThreatItemData("T9: Accidental Data Destruction", "Accidental deletion of critical notes or media.", "Mitigated: Two-phase deletion with isolated Recycle Bin container before cryptographic erasure.", ThreatStatus.MITIGATED),
            ThreatItemData("T10: Silent Storage Bit Rot", "Hardware degradation or storage corruption corrupting encrypted objects.", "Mitigated: SHA-256 authenticated integrity hashes calculated and verified on file import and restore operations.", ThreatStatus.MITIGATED)
        )

        for (threat in threats) {
            ThreatCard(threat)
        }
    }
}

enum class ThreatStatus { MITIGATED, EXPLAINED }

data class ThreatItemData(
    val title: String,
    val attackVector: String,
    val mitigation: String,
    val status: ThreatStatus
)

@Composable
private fun ThreatCard(item: ThreatItemData) {
    var expanded by remember { mutableStateOf(false) }

    AegisGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = AegisBorder,
        onClick = { expanded = !expanded }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AegisEmerald)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.title,
                        color = AegisTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AegisTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "Attack Vector:",
                        color = AegisAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.attackVector,
                        color = AegisTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Engineering Mitigation:",
                        color = AegisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.mitigation,
                        color = AegisTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun IntegritySection(
    viewModel: MainViewModel,
    report: com.example.data.repository.VaultIntegrityReport?,
    duplicates: List<com.example.data.model.VaultObjectEntity>
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AegisGlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = AegisCyan.copy(alpha = 0.4f)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vault Integrity Auditor",
                        color = AegisTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { viewModel.runIntegrityCheck() },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("run_audit_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF042F3D), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audit Now", color = Color(0xFF042F3D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (report != null) {
                    val statusColor = if (report.status == "HEALTHY") AegisEmerald else AegisAmber
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STATUS: ${report.status}",
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    AuditMetricRow("Database Records", "${report.totalDbRecords} registered")
                    AuditMetricRow("Verified Sharded Files", "${report.verifiedDiskFiles} present on flash")
                    AuditMetricRow("Missing Storage Files", "${report.missingDiskFiles.size} anomalies")
                    AuditMetricRow("Identical SHA-256 Duplicates", "${report.duplicateCount} objects")
                } else {
                    Text(
                        text = "Tap 'Audit Now' to verify physical ciphertext files on disk against internal database records and SHA-256 fingerprints.",
                        color = AegisTextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Duplicates Breakdown
        if (duplicates.isNotEmpty()) {
            AegisGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = AegisAmber.copy(alpha = 0.4f)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AegisAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Duplicate Content Detected (${duplicates.size})",
                            color = AegisAmber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The following files share identical SHA-256 hashes. You can safely remove duplicate entries in the Vault browser to recover storage space.",
                        color = AegisTextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    duplicates.take(5).forEach { dup ->
                        Text(
                            text = "• ${dup.originalName} (${formatBytes(dup.fileSizeBytes)})",
                            color = AegisTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AegisTextSecondary, fontSize = 12.sp)
        Text(text = value, color = AegisTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActivityLogsSection(
    viewModel: MainViewModel,
    logs: List<com.example.data.model.ActivityLogEntity>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOCAL AUDIT TRAIL (DEVICE ONLY)",
                color = AegisTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            if (logs.isNotEmpty()) {
                Text(
                    text = "Clear Logs",
                    color = AegisRose,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.clearActivityLogs() }
                )
            }
        }

        if (logs.isEmpty()) {
            AegisGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No audit events logged yet.",
                    color = AegisTextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            val dateFormat = SimpleDateFormat("HH:mm:ss (MMM d)", Locale.getDefault())
            for (log in logs) {
                val timeStr = dateFormat.format(Date(log.timestamp))
                AegisGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AegisSurfaceElevated)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = log.action,
                                        color = AegisCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = log.description,
                                    color = AegisTextPrimary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = timeStr,
                            color = AegisTextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
