package com.example.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object RaivalColorTokens {
    // Brand
    val brand = Color(0xFF1ED760)
    val brandDim = Color(0xFF169C46)
    val brandOn = Color.Black

    // Surfaces — layered depth hierarchy (background → elevated)
    val surfaceContainerLowest = Color(0xFF0B0B0C)
    val surfaceContainerLow = Color(0xFF121212)
    val surfaceContainer = Color(0xFF1A1A1B)
    val surfaceContainerHigh = Color(0xFF222224)
    val surfaceContainerHighest = Color(0xFF2B2B2D)

    // Accents (semantic)
    val accentGold = Color(0xFFFFD700)
    val accentRed = Color(0xFFFF1744)
    val accentBlue = Color(0xFF00E5FF)
    val error = Color(0xFFE91429)

    // Text
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFE6E6E6)
    val textMuted = Color(0xFFB3B3B3)

    // Outlines
    val outline = Color(0xFF3A3A3C)
    val outlineVariant = Color(0xFF2E2E30)
}

fun raivalColorScheme() = darkColorScheme(
    primary = RaivalColorTokens.brand,
    onPrimary = RaivalColorTokens.brandOn,
    primaryContainer = RaivalColorTokens.brand.copy(alpha = 0.16f),
    onPrimaryContainer = RaivalColorTokens.brand,

    secondary = RaivalColorTokens.accentGold,
    onSecondary = Color.Black,
    secondaryContainer = RaivalColorTokens.accentGold.copy(alpha = 0.16f),
    onSecondaryContainer = RaivalColorTokens.accentGold,

    tertiary = RaivalColorTokens.accentBlue,
    onTertiary = Color.Black,
    tertiaryContainer = RaivalColorTokens.accentBlue.copy(alpha = 0.16f),
    onTertiaryContainer = RaivalColorTokens.accentBlue,

    background = RaivalColorTokens.surfaceContainerLow,
    onBackground = RaivalColorTokens.textPrimary,
    surface = RaivalColorTokens.surfaceContainer,
    onSurface = RaivalColorTokens.textPrimary,
    surfaceVariant = RaivalColorTokens.surfaceContainerHigh,
    onSurfaceVariant = RaivalColorTokens.textMuted,
    surfaceContainerLowest = RaivalColorTokens.surfaceContainerLowest,
    surfaceContainerLow = RaivalColorTokens.surfaceContainerLow,
    surfaceContainer = RaivalColorTokens.surfaceContainer,
    surfaceContainerHigh = RaivalColorTokens.surfaceContainerHigh,
    surfaceContainerHighest = RaivalColorTokens.surfaceContainerHighest,
    error = RaivalColorTokens.error,
    onError = Color.White,
    errorContainer = RaivalColorTokens.error.copy(alpha = 0.16f),
    onErrorContainer = RaivalColorTokens.error,
    outline = RaivalColorTokens.outline,
    outlineVariant = RaivalColorTokens.outlineVariant,
    surfaceTint = RaivalColorTokens.brand,
    inverseSurface = RaivalColorTokens.textPrimary,
    inverseOnSurface = Color(0xFF121212),
    inversePrimary = Color(0xFF121212),
    scrim = Color(0x99000000)
)

val RaivalShapes = Shapes(
    small = ShapeTokens.chip,
    medium = ShapeTokens.card,
    large = ShapeTokens.dialog
)