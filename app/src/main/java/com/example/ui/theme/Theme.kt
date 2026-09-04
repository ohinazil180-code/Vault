package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AegisDarkColorScheme = darkColorScheme(
    primary = AegisCyan,
    onPrimary = Color(0xFF042F3D),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = AegisBlue,
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = AegisEmerald,
    onTertiary = Color(0xFF022C22),
    background = AegisBackground,
    onBackground = AegisTextPrimary,
    surface = AegisSurface,
    onSurface = AegisTextPrimary,
    surfaceVariant = AegisSurfaceElevated,
    onSurfaceVariant = AegisTextSecondary,
    outline = AegisBorder,
    outlineVariant = AegisBorderGlow,
    error = AegisRose,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AegisDarkColorScheme,
        typography = Typography,
        content = content
    )
}

