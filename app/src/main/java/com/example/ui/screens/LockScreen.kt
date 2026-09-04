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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricHelper
import com.example.ui.MainViewModel
import com.example.ui.components.AegisPinKeypad
import com.example.ui.components.AegisSecurityBadge
import com.example.ui.components.SecurityBadgeStatus
import com.example.ui.theme.AegisBackground
import com.example.ui.theme.AegisBorder
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisRose
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary

@Composable
fun LockScreen(
    viewModel: MainViewModel,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryInput by remember { mutableStateOf("") }

    val biometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }
    val biometricEnabled = config?.biometricEnabled == true && biometricAvailable

    fun triggerBiometricPrompt() {
        val activity = context as? FragmentActivity ?: return
        BiometricHelper.authenticate(
            activity = activity,
            onSuccess = {
                viewModel.unlockWithBiometrics {
                    onUnlocked()
                }
            },
            onError = { _ -> }
        )
    }

    // Auto-trigger biometric on open if configured
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            triggerBiometricPrompt()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AegisBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AegisSurfaceElevated)
                        .border(1.5.dp, AegisCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Project Aegis Security Shield",
                        tint = AegisCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "PRIVATE VAULT Ω",
                    color = AegisTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Project Aegis — Authenticated Session Required",
                    color = AegisTextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                AegisSecurityBadge(
                    text = "AES-256-GCM HARDWARE PROTECTED",
                    status = SecurityBadgeStatus.CYBER
                )
            }

            // Error Banner
            AnimatedVisibility(visible = authError != null) {
                if (authError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AegisRose.copy(alpha = 0.15f))
                            .border(1.dp, AegisRose.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = authError!!,
                            color = AegisRose,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Keypad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                AegisPinKeypad(
                    pinLength = 6,
                    currentPinLength = enteredPin.length,
                    onDigitClick = { digit ->
                        if (enteredPin.length < 6) {
                            val nextPin = enteredPin + digit
                            enteredPin = nextPin
                            viewModel.clearAuthError()

                            // Auto-submit when length is 6 or min 4
                            if (nextPin.length == 6) {
                                viewModel.unlockWithPin(nextPin) {
                                    onUnlocked()
                                }
                            }
                        }
                    },
                    onDeleteClick = {
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                            viewModel.clearAuthError()
                        }
                    },
                    onBiometricClick = if (biometricAvailable) {
                        { triggerBiometricPrompt() }
                    } else null
                )

                if (enteredPin.length in 4..5) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.unlockWithPin(enteredPin) {
                                onUnlocked()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("submit_pin_button")
                    ) {
                        Text(
                            text = "Unlock Vault",
                            color = Color(0xFF042F3D),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Recovery Link
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showRecoveryDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = AegisTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Emergency Recovery Code",
                        color = AegisTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Recovery Dialog
        if (showRecoveryDialog) {
            AlertDialog(
                onDismissRequest = { showRecoveryDialog = false },
                containerColor = AegisSurfaceElevated,
                title = {
                    Text(
                        text = "Cryptographic Recovery",
                        color = AegisTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter the 24-character recovery code generated during vault initialization (e.g. AEGIS-XXXX-XXXX-XXXX-XXXX):",
                            color = AegisTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = recoveryInput,
                            onValueChange = { recoveryInput = it },
                            placeholder = { Text("AEGIS-....", color = AegisTextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisCyan,
                                unfocusedBorderColor = AegisBorder,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("recovery_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.unlockWithRecoveryCode(recoveryInput) {
                                showRecoveryDialog = false
                                onUnlocked()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisCyan)
                    ) {
                        Text("Verify & Unlock", color = Color(0xFF042F3D), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRecoveryDialog = false }) {
                        Text("Cancel", color = AegisTextSecondary)
                    }
                }
            )
        }
    }
}
