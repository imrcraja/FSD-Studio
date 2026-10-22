package com.fsdetc.fsdstudio.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Selectable app themes (Settings → Theme). FSD_PURPLE is the default. */
enum class AppTheme { FSD_PURPLE, MIDNIGHT, LIGHT }

private val PurpleScheme: ColorScheme = darkColorScheme(
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

private val MidnightScheme: ColorScheme = darkColorScheme(
    primary = FsdAccent,
    onPrimary = Color(0xFF1B0B33),
    primaryContainer = FsdPrimary,
    onPrimaryContainer = FsdOnSurface,
    secondary = FsdSecondaryAccent,
    onSecondary = Color(0xFF0B1B2A),
    background = MidnightBackground,
    onBackground = FsdOnSurface,
    surface = MidnightSurface,
    onSurface = FsdOnSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = MidnightOnSurfaceMuted,
    error = FsdError,
    onError = Color(0xFF2A0A0A),
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = FsdPrimary,
    onPrimary = Color.White,
    primaryContainer = FsdPrimary,
    onPrimaryContainer = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = Color.White,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceMuted,
    error = LightError,
    onError = Color.White,
)

/**
 * The three palettes expose the same slots, and screens read colours from
 * [MaterialTheme.colorScheme] — the old Fsd* colour values live on as the
 * FSD_PURPLE palette above.
 */
@Composable
fun FsdStudioTheme(theme: AppTheme = AppTheme.FSD_PURPLE, content: @Composable () -> Unit) {
    val scheme = when (theme) {
        AppTheme.FSD_PURPLE -> PurpleScheme
        AppTheme.MIDNIGHT -> MidnightScheme
        AppTheme.LIGHT -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
