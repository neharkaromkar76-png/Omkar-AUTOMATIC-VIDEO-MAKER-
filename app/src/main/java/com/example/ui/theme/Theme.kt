package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OmkarPrimary,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = OmkarSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF321A80),
    onSecondaryContainer = Color(0xFFE8DDFF),
    tertiary = OmkarAccent,
    onTertiary = Color(0xFF432C00),
    tertiaryContainer = Color(0xFF604100),
    onTertiaryContainer = Color(0xFFFFDEA8),
    background = OmkarDarkBackground,
    onBackground = TextPrimary,
    surface = OmkarSurface,
    onSurface = TextPrimary,
    surfaceVariant = OmkarSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = OmkarSurfaceBorder,
    error = OmkarError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Video editor defaults to premium dark mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
