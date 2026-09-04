package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.BuildConfig
import com.example.ui.MainViewModel
import com.example.ui.screens.AegisAiScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.FileViewerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SecurityCenterScreen
import com.example.ui.screens.SecretsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.AegisBackground
import com.example.ui.theme.AegisBorder
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisSurfaceGlass
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private var backgroundTimestamp: Long = 0L
    private var vmRef: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val mainViewModel: MainViewModel = viewModel()
                vmRef = mainViewModel

                val config by mainViewModel.config.collectAsState()
                val isInitialized by mainViewModel.isInitialized.collectAsState()
                val isUnlocked by mainViewModel.isUnlocked.collectAsState()
                val viewingObject by mainViewModel.viewingObject.collectAsState()

                // Threat T3 Mitigation: Block app switcher previews, screen recordings, screenshots
                // FLAG_SECURE is applied dynamically when enabled in Settings (and cleared in debug/streaming emulators so WebRTC displays properly)
                LaunchedEffect(config?.secureScreenEnabled) {
                    val enableSecure = config?.secureScreenEnabled == true && !BuildConfig.DEBUG
                    if (enableSecure) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                when {
                    !isInitialized -> {
                        OnboardingScreen(
                            viewModel = mainViewModel,
                            onComplete = {
                                // Handled in ViewModel
                            }
                        )
                    }
                    !isUnlocked -> {
                        LockScreen(
                            viewModel = mainViewModel,
                            onUnlocked = {
                                // Handled in ViewModel
                            }
                        )
                    }
                    viewingObject != null -> {
                        FileViewerScreen(
                            viewModel = mainViewModel,
                            onClose = {
                                mainViewModel.closeViewer()
                            }
                        )
                    }
                    else -> {
                        AegisMainScaffold(viewModel = mainViewModel)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundTimestamp = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundTimestamp > 0L) {
            val elapsed = System.currentTimeMillis() - backgroundTimestamp
            vmRef?.handleBackgroundTimeout(elapsed)
            backgroundTimestamp = 0L
        }
    }
}

@Composable
fun AegisMainScaffold(
    viewModel: MainViewModel
) {
    var currentTab by remember { mutableStateOf("HOME") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AegisBackground,
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            AegisNavigationBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AegisBackground)
        ) {
            when (currentTab) {
                "HOME" -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToVault = { currentTab = "VAULT" },
                        onNavigateToNotes = { currentTab = "NOTES" },
                        onNavigateToSecrets = { currentTab = "SECRETS" },
                        onNavigateToSecurityCenter = { currentTab = "SECURITY" },
                        onNavigateToBackup = { currentTab = "BACKUP" },
                        onNavigateToAi = { currentTab = "AI" }
                    )
                }
                "AI" -> {
                    AegisAiScreen(
                        viewModel = viewModel,
                        aiRepository = viewModel.aiRepository
                    )
                }
                "VAULT" -> {
                    VaultScreen(viewModel = viewModel)
                }
                "NOTES" -> {
                    NotesScreen(viewModel = viewModel)
                }
                "SECRETS" -> {
                    SecretsScreen(viewModel = viewModel)
                }
                "SECURITY" -> {
                    SecurityCenterScreen(viewModel = viewModel)
                }
                "BACKUP" -> {
                    BackupScreen(viewModel = viewModel)
                }
                "SETTINGS" -> {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AegisNavigationBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    val items = listOf(
        NavTabItem("HOME", "Home", Icons.Default.Home),
        NavTabItem("VAULT", "Vault", Icons.Default.Description),
        NavTabItem("AI", "AEGIS AI", Icons.Default.Shield),
        NavTabItem("NOTES", "Notes", Icons.Default.Note),
        NavTabItem("SECRETS", "Secrets", Icons.Default.Key),
        NavTabItem("SETTINGS", "Settings", Icons.Default.Settings)
    )

    NavigationBar(
        containerColor = AegisSurfaceGlass,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AegisBorder, RoundedCornerShape(0.dp))
            .testTag("aegis_bottom_nav")
    ) {
        for (item in items) {
            val selected = currentTab == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF042F3D),
                    selectedTextColor = AegisCyan,
                    indicatorColor = AegisCyan,
                    unselectedIconColor = AegisTextMuted,
                    unselectedTextColor = AegisTextMuted
                )
            )
        }
    }
}

data class NavTabItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
