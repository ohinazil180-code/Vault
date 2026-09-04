package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.AegisGlassCard
import com.example.ui.components.AegisPinKeypad
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
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmingPin by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var recoveryConfirmed by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf<String?>(null) }

    val totalSteps = 4

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
            // Header Progress
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AegisSecurityBadge(
                        text = "PROJECT AEGIS SETUP",
                        status = SecurityBadgeStatus.CYBER
                    )
                    Text(
                        text = "Step $step of $totalSteps",
                        color = AegisTextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Progress bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..totalSteps) {
                        val active = i <= step
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (active) AegisCyan else AegisBorder)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step Content
            when (step) {
                1 -> {
                    // Step 1: Philosophy & Identity
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(AegisSurfaceElevated)
                                .border(2.dp, AegisCyan.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = AegisCyan,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "PRIVATE VAULT Ω",
                            color = AegisTextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Project Aegis Security Platform",
                            color = AegisCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        AegisGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "Core Architecture Principles",
                                    color = AegisTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                PrincipleItem("Local-First & Offline", "No remote authentication, zero mandatory cloud accounts, 100% device sovereignty.")
                                PrincipleItem("Authenticated Ciphers", "All data encrypted with AES-256-GCM and unique 96-bit random IVs.")
                                PrincipleItem("Hardware Root Key", "Protected by Android KeyStore hardware-backed secure element (TEE).")
                            }
                        }
                    }
                }

                2 -> {
                    // Step 2: Critical Warning
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(AegisSurfaceElevated)
                                .border(2.dp, AegisAmber.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AegisAmber,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Zero-Knowledge Warning",
                            color = AegisTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Because Project Aegis employs true client-side cryptography, there is no backdoor, no company server, and no password reset email.",
                            color = AegisTextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        AegisGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = AegisAmber.copy(alpha = 0.4f)
                        ) {
                            Column {
                                Text(
                                    text = "Permanent Loss Warning",
                                    color = AegisAmber,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "If you forget both your primary PIN and your recovery code, your encrypted files and secrets are mathematically unrecoverable. Please store your recovery code in a secure physical location.",
                                    color = AegisTextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }

                3 -> {
                    // Step 3: PIN Setup
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (!isConfirmingPin) "Create Primary PIN" else "Confirm Your PIN",
                            color = AegisTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (!isConfirmingPin) "Enter a 4 to 6 digit security PIN" else "Re-enter the PIN to confirm",
                            color = AegisTextSecondary,
                            fontSize = 14.sp
                        )

                        if (pinError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = pinError!!,
                                color = AegisRose,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val currentPin = if (!isConfirmingPin) enteredPin else confirmPin

                        AegisPinKeypad(
                            pinLength = 6,
                            currentPinLength = currentPin.length,
                            onDigitClick = { digit ->
                                if (currentPin.length < 6) {
                                    if (!isConfirmingPin) {
                                        enteredPin += digit
                                    } else {
                                        confirmPin += digit
                                    }
                                }
                            },
                            onDeleteClick = {
                                if (!isConfirmingPin && enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                } else if (isConfirmingPin && confirmPin.isNotEmpty()) {
                                    confirmPin = confirmPin.dropLast(1)
                                }
                            }
                        )
                    }
                }

                4 -> {
                    // Step 4: Vault Initialized & Recovery Code
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(AegisSurfaceElevated)
                                .border(2.dp, AegisEmerald, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = AegisEmerald,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Vault Key Initialized",
                            color = AegisTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Save your cryptographic emergency recovery key:",
                            color = AegisTextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // Display recovery code
                        AegisGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = AegisCyan
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = generatedCode ?: "GENERATING...",
                                    color = AegisCyan,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "256-bit PBKDF2 Master Key Recovery Credential",
                                    color = AegisTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { recoveryConfirmed = !recoveryConfirmed }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = recoveryConfirmed,
                                onCheckedChange = { recoveryConfirmed = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AegisCyan,
                                    uncheckedColor = AegisBorderGlow
                                ),
                                modifier = Modifier.testTag("confirm_recovery_checkbox")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "I understand that without my PIN and Recovery Code, vault contents cannot be restored.",
                                color = AegisTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        when (step) {
                            1 -> step = 2
                            2 -> step = 3
                            3 -> {
                                if (!isConfirmingPin) {
                                    if (enteredPin.length < 4) {
                                        pinError = "PIN must be at least 4 digits"
                                    } else {
                                        pinError = null
                                        isConfirmingPin = true
                                    }
                                } else {
                                    if (confirmPin != enteredPin) {
                                        pinError = "PINs do not match. Please re-enter."
                                        confirmPin = ""
                                    } else {
                                        pinError = null
                                        // Initialize vault
                                        viewModel.setupNewVault(enteredPin) {
                                            generatedCode = viewModel.generatedRecoveryCode.value
                                            step = 4
                                        }
                                    }
                                }
                            }
                            4 -> {
                                if (recoveryConfirmed) {
                                    viewModel.clearGeneratedRecoveryCode()
                                    onComplete()
                                }
                            }
                        }
                    },
                    enabled = when (step) {
                        1, 2 -> true
                        3 -> if (!isConfirmingPin) enteredPin.length >= 4 else confirmPin.length >= 4
                        4 -> recoveryConfirmed
                        else -> true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisCyan,
                        contentColor = Color(0xFF042F3D),
                        disabledContainerColor = AegisSurfaceElevated,
                        disabledContentColor = AegisTextMuted
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("onboarding_action_button")
                ) {
                    Text(
                        text = when (step) {
                            1 -> "Continue to Architecture Overview"
                            2 -> "I Acknowledge, Proceed to PIN Setup"
                            3 -> if (!isConfirmingPin) "Next: Confirm PIN" else "Initialize Encrypted Vault"
                            4 -> "Enter Secure Vault"
                            else -> "Next"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PrincipleItem(title: String, description: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(AegisCyan)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = AegisTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = AegisTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
