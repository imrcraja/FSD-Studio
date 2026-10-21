package com.fsdetc.fsdstudio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FsdColorScheme = darkColorScheme(
    primary = FsdAccent,
    onPrimary = Color(0xFF1B0B33),
    primaryContainer = FsdPrimary,
    onPrimaryContainer = FsdOnSurface,
    secondary = FsdSecondaryAccent,
    onSecondary = Color(0xFF0B1B2A),
    background = FsdBackground,
    onBackground = FsdOnSurface,
    surface = FsdSurface,
    onSurface = FsdOnSurface,
    surfaceVariant = FsdSurfaceVariant,
    onSurfaceVariant = FsdOnSurfaceMuted,
    error = FsdError,
    onError = Color(0xFF2A0A0A),
)

@Composable
fun FsdStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FsdColorScheme, content = content)
}
