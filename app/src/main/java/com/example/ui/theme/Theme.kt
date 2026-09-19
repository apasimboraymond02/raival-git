package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Force dark theme globally for esports brand look
    isDarkTheme = true

    MaterialTheme(
        colorScheme = raivalColorScheme(),
        typography = Typography,
        shapes = RaivalShapes,
        content = content
    )
}