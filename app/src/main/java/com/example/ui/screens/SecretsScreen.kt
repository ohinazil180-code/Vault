package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SecureSecretEntity
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.SecureRandom

@Composable
fun SecretsScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val secrets by viewModel.secrets.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    var editingSecret by remember { mutableStateOf<SecureSecretEntity?>(null) }
    var isCreatingSecret by remember { mutableStateOf(false) }
    var showPasswordGenerator by remember { mutableStateOf(false) }

    // Dialog state
    var titleInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var websiteInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("LOGIN") }
    var notesInput by remember { mutableStateOf("") }

    val filteredSecrets = secrets.filter { secret ->
        val matchesCat = (selectedCategory == "ALL" || secret.category == selectedCategory)
        val matchesSearch = secret.title.contains(searchQuery, ignoreCase = true) ||
                secret.username.contains(searchQuery, ignoreCase = true)
        matchesCat && matchesSearch
    }

    // Function to copy to clipboard with 30-second auto-purge
    fun copyWithAutoClear(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied. Clipboard will auto-clear in 30s.", Toast.LENGTH_SHORT).show()

        coroutineScope.launch {
            delay(30000)
            try {
                // Clear clipboard
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            } catch (_: Exception) {}
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

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SECRETS MANAGER",
                        color = AegisTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${filteredSecrets.size} credentials protected",
                        color = AegisTextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search logins & credentials...", color = AegisTextMuted, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = AegisPurple) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AegisPurple,
                    unfocusedBorderColor = AegisBorder,
                    focusedTextColor = AegisTextPrimary,
                    unfocusedTextColor = AegisTextPrimary
                ),
                modifier = Modifier.fillMaxWidth().testTag("secrets_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf("ALL", "LOGIN", "CARD", "API_KEY", "OTHER")
                for (cat in categories) {
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AegisPurple else AegisSurfaceElevated)
                            .border(1.dp, if (isSelected) AegisPurple else AegisBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else AegisTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secrets List
            if (filteredSecrets.isEmpty()) {
                AegisEmptyState(
                    title = "No Credentials Stored",
                    description = "Store website accounts, passwords, API tokens, or server keys with authenticated AES-256-GCM protection.",
                    icon = Icons.Default.Key,
                    actionText = "Add Credential",
                    onActionClick = {
                        titleInput = ""
                        usernameInput = ""
                        passwordInput = ""
                        websiteInput = ""
                        categoryInput = "LOGIN"
                        notesInput = ""
                        isCreatingSecret = true
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredSecrets, key = { it.id }) { secret ->
                        SecretItemCard(
                            secret = secret,
                            viewModel = viewModel,
                            onCopyPassword = { pass -> copyWithAutoClear("Password", pass) },
                            onCopyUsername = { user -> copyWithAutoClear("Username", user) },
                            onClick = {
                                editingSecret = secret
                                titleInput = secret.title
                                usernameInput = secret.username
                                websiteInput = secret.websiteUrl
                                categoryInput = secret.category
                                notesInput = secret.notes
                                passwordInput = "" // loaded in LaunchedEffect
                            },
                            onDelete = { viewModel.deleteSecret(secret.id) }
                        )
                    }
                }
            }
        }

        // Add Secret FAB
        FloatingActionButton(
            onClick = {
                titleInput = ""
                usernameInput = ""
                passwordInput = ""
                websiteInput = ""
                categoryInput = "LOGIN"
                notesInput = ""
                isCreatingSecret = true
            },
            containerColor = AegisPurple,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_secret_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Secret")
        }

        // Add / Edit Dialog
        val isOpen = isCreatingSecret || editingSecret != null
        if (isOpen) {
            LaunchedEffect(editingSecret) {
                if (editingSecret != null) {
                    passwordInput = viewModel.decryptSecretPassword(editingSecret!!.encryptedPasswordBase64)
                }
            }

            AlertDialog(
                onDismissRequest = {
                    isCreatingSecret = false
                    editingSecret = null
                },
                containerColor = AegisSurfaceElevated,
                title = {
                    Text(
                        text = if (isCreatingSecret) "Store New Credential" else "Edit Credential",
                        color = AegisTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Title (e.g. GitHub, ProtonMail)", color = AegisTextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisPurple,
                                unfocusedBorderColor = AegisBorder,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("secret_title_input")
                        )

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Username or Email", color = AegisTextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisPurple,
                                unfocusedBorderColor = AegisBorder,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("secret_username_input")
                        )

                        // Password field + generator button
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password", color = AegisTextMuted) },
                            trailingIcon = {
                                IconButton(onClick = { showPasswordGenerator = true }) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = "Generate strong password",
                                        tint = AegisCyan
                                    )
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisPurple,
                                unfocusedBorderColor = AegisBorder,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("secret_password_input")
                        )

                        OutlinedTextField(
                            value = websiteInput,
                            onValueChange = { websiteInput = it },
                            label = { Text("Website URL (Optional)", color = AegisTextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisPurple,
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
                            viewModel.saveSecret(
                                id = editingSecret?.id,
                                title = titleInput,
                                username = usernameInput,
                                passwordPlain = passwordInput,
                                websiteUrl = websiteInput,
                                category = categoryInput,
                                notes = notesInput
                            ) {
                                isCreatingSecret = false
                                editingSecret = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisPurple),
                        modifier = Modifier.testTag("save_secret_button")
                    ) {
                        Text("Save Credential", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        isCreatingSecret = false
                        editingSecret = null
                    }) {
                        Text("Cancel", color = AegisTextSecondary)
                    }
                }
            )
        }

        // Strong Password Generator Dialog
        if (showPasswordGenerator) {
            var genLength by remember { mutableFloatStateOf(16f) }
            var generatedSample by remember { mutableStateOf(generateSecurePassword(16)) }

            AlertDialog(
                onDismissRequest = { showPasswordGenerator = false },
                containerColor = AegisSurfaceElevated,
                title = {
                    Text("Password Generator", color = AegisTextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AegisBackground)
                                .border(1.dp, AegisCyan, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = generatedSample,
                                color = AegisCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Length: ${genLength.toInt()} characters",
                            color = AegisTextSecondary,
                            fontSize = 13.sp
                        )
                        Slider(
                            value = genLength,
                            onValueChange = {
                                genLength = it
                                generatedSample = generateSecurePassword(it.toInt())
                            },
                            valueRange = 8f..32f,
                            steps = 23,
                            colors = SliderDefaults.colors(thumbColor = AegisCyan, activeTrackColor = AegisCyan)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            passwordInput = generatedSample
                            showPasswordGenerator = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisCyan)
                    ) {
                        Text("Use Password", color = Color(0xFF042F3D), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPasswordGenerator = false }) {
                        Text("Cancel", color = AegisTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun SecretItemCard(
    secret: SecureSecretEntity,
    viewModel: MainViewModel,
    onCopyPassword: (String) -> Unit,
    onCopyUsername: (String) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var plainPassword by remember { mutableStateOf<String?>(null) }
    var isRevealed by remember { mutableStateOf(false) }

    LaunchedEffect(isRevealed) {
        if (isRevealed && plainPassword == null) {
            plainPassword = viewModel.decryptSecretPassword(secret.encryptedPasswordBase64)
        }
    }

    val itemScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AegisSurfaceGlass)
            .border(1.dp, AegisBorder, RoundedCornerShape(14.dp))
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AegisPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = AegisPurple, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = secret.title,
                            color = AegisTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (secret.username.isNotBlank()) {
                            Text(
                                text = secret.username,
                                color = AegisTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AegisTextMuted, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Password bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AegisSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRevealed) (plainPassword ?: "Decrypting...") else "••••••••••••",
                    color = if (isRevealed) AegisCyan else AegisTextMuted,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isRevealed = !isRevealed },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Reveal",
                            tint = AegisTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            itemScope.launch {
                                val pass = plainPassword ?: viewModel.decryptSecretPassword(secret.encryptedPasswordBase64)
                                onCopyPassword(pass)
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Password (Auto-clearing)",
                            tint = AegisCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun generateSecurePassword(length: Int): String {
    val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    val lower = "abcdefghijkmnopqrstuvwxyz"
    val digits = "23456789"
    val symbols = "!@#$%^&*()-_=+"
    val all = upper + lower + digits + symbols

    val random = SecureRandom()
    val password = StringBuilder()
    password.append(upper[random.nextInt(upper.length)])
    password.append(lower[random.nextInt(lower.length)])
    password.append(digits[random.nextInt(digits.length)])
    password.append(symbols[random.nextInt(symbols.length)])

    for (i in 4 until length) {
        password.append(all[random.nextInt(all.length)])
    }

    return password.toString().toList().shuffled(random).joinToString("")
}
