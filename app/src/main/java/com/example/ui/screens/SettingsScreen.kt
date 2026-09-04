package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.BiometricHelper
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
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()

    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var confirmNewPinInput by remember { mutableStateOf("") }
    var changePinError by remember { mutableStateOf<String?>(null) }

    val biometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }
    val biometricEnabled = config?.biometricEnabled == true
    val currentTimeout = config?.autoLockTimeoutSeconds ?: 60

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
                    text = "SETTINGS & SECURITY",
                    color = AegisTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Security Policies & Credential Rotation",
                    color = AegisTextMuted,
                    fontSize = 12.sp
                )
            }
            AegisSecurityBadge(text = "ZERO CLOUD", status = SecurityBadgeStatus.ACTIVE)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Auto-Lock Section
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
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = AegisCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Auto-Lock Session Inactivity", color = AegisTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Closes active session and purges master key from memory", color = AegisTextMuted, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val timeouts = listOf(
                    0 to "Never",
                    30 to "30s",
                    60 to "1m",
                    300 to "5m",
                    900 to "15m"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for ((sec, label) in timeouts) {
                        val isSelected = currentTimeout == sec
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AegisCyan else AegisSurfaceElevated)
                                .border(1.dp, if (isSelected) AegisCyan else AegisBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.setAutoLockTimeout(sec) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF042F3D) else AegisTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Biometric Unlock Toggle
        AegisGlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AegisEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = AegisEmerald, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Biometric Quick Unlock", color = AegisTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (biometricAvailable) "Fingerprint / Face Unlock supported" else "No biometric hardware detected",
                            color = AegisTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = biometricEnabled && biometricAvailable,
                    onCheckedChange = { enabled ->
                        if (biometricAvailable) {
                            viewModel.setBiometricEnabled(enabled)
                        } else {
                            Toast.makeText(context, "Biometrics not available on this device", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = biometricAvailable,
                    colors = SwitchDefaults.colors(checkedThumbColor = AegisEmerald, checkedTrackColor = Color(0x4410B981))
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Change Primary PIN
        AegisGlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                newPinInput = ""
                confirmNewPinInput = ""
                changePinError = null
                showChangePinDialog = true
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AegisAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = AegisAmber, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Rotate Primary PIN", color = AegisTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Re-wraps master key with new credential; no re-encryption needed", color = AegisTextMuted, fontSize = 11.sp)
                    }
                }
                Text(text = "Change →", color = AegisCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // App Switcher Protection Info (FLAG_SECURE)
        val secureScreenEnabled = config?.secureScreenEnabled == true
        AegisGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AegisCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = AegisCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "App Switcher Privacy", color = AegisTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Enforce Android FLAG_SECURE", color = AegisTextMuted, fontSize = 11.sp)
                        }
                    }

                    Switch(
                        checked = secureScreenEnabled,
                        onCheckedChange = { viewModel.setSecureScreenEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AegisCyan, checkedTrackColor = Color(0x4400E5FF))
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "When active on physical hardware, Android FLAG_SECURE blocks screenshots, task switcher previews, and external recorders. Leave disabled when using browser streaming previews.",
                    color = AegisTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // About / Architecture Summary
        AegisGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "PRIVATE VAULT Ω — PROJECT AEGIS",
                    color = AegisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Specification: Master Production Build Ω\n" +
                            "Algorithm: AES-256-GCM (128-bit tag, 96-bit random IV)\n" +
                            "KDF: PBKDF2WithHmacSHA256 (65,536 iterations)\n" +
                            "Key Store: AndroidKeyStore Hardware TEE KEK\n" +
                            "Storage: Deterministic Sharded Private Objects\n" +
                            "Database: Local SQLite / Room metadata\n" +
                            "Networking: ZERO network permissions declared",
                    color = AegisTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 17.sp
                )
            }
        }

        // Change PIN Dialog
        if (showChangePinDialog) {
            AlertDialog(
                onDismissRequest = { showChangePinDialog = false },
                containerColor = AegisSurfaceElevated,
                title = {
                    Text("Rotate Vault PIN", color = AegisTextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Enter a new 4 to 6 digit PIN. The master key will be re-wrapped using a newly generated cryptographic salt.",
                            color = AegisTextSecondary,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { newPinInput = it },
                            label = { Text("New PIN", color = AegisTextMuted) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisCyan,
                                unfocusedBorderColor = AegisBorder,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                        )

                        OutlinedTextField(
                            value = confirmNewPinInput,
                            onValueChange = { confirmNewPinInput = it },
                            label = { Text("Confirm New PIN", color = AegisTextMuted) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisCyan,
                                unfocusedBorderColor = AegisBorder,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("confirm_new_pin_input")
                        )

                        if (changePinError != null) {
                            Text(text = changePinError!!, color = AegisRose, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPinInput.length < 4) {
                                changePinError = "PIN must be at least 4 digits"
                            } else if (newPinInput != confirmNewPinInput) {
                                changePinError = "PINs do not match"
                            } else {
                                viewModel.changePin(newPinInput) { success ->
                                    if (success) {
                                        Toast.makeText(context, "PIN updated successfully!", Toast.LENGTH_SHORT).show()
                                        showChangePinDialog = false
                                    } else {
                                        changePinError = "Rotation failed: Vault must be unlocked"
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisCyan)
                    ) {
                        Text("Update PIN", color = Color(0xFF042F3D), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangePinDialog = false }) {
                        Text("Cancel", color = AegisTextSecondary)
                    }
                }
            )
        }
    }
}
