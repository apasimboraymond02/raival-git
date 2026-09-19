package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

var isDarkTheme by mutableStateOf(true)

val RaivalPrimary: Color get() = RaivalColorTokens.brand
val RaivalSecondary: Color get() = RaivalColorTokens.brand
val RaivalElectricPurple: Color get() = RaivalColorTokens.accentRed
val RaivalBrightLime: Color get() = RaivalColorTokens.accentGold
val RaivalBackground: Color get() = RaivalColorTokens.surfaceContainerLow
val RaivalSurface: Color get() = RaivalColorTokens.surfaceContainer
val RaivalSurfaceLight: Color get() = RaivalColorTokens.surfaceContainerHigh
val RaivalAccent: Color get() = RaivalColorTokens.accentGold
val RaivalSuccess: Color get() = RaivalColorTokens.brand
val RaivalError: Color get() = RaivalColorTokens.error
val RaivalTextPrimary: Color get() = RaivalColorTokens.textPrimary
val RaivalTextSecondary: Color get() = RaivalColorTokens.textMuted