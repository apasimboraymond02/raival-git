package com.example.ui.screens

import androidx.compose.ui.graphics.Color

data class GameTheme(
    val accentColor: Color,
    val bgColor: Color
)

fun getGameTheme(game: String): GameTheme {
    val title = game.lowercase()
    return when {
        title.contains("dream league") || title.contains("dls") || title.contains("soccer") || title.contains("football") -> GameTheme(Color(0xFF4CAF50), Color(0xFF1B5E20))
        title.contains("fifa") || title.contains("fc 24") || title.contains("fc 25") || title.contains("fc mobile") -> GameTheme(Color(0xFF2196F3), Color(0xFF0D47A1))
        title.contains("pubg") || title.contains("free fire") || title.contains("cod") || title.contains("shooter") || title.contains("call of duty") -> GameTheme(Color(0xFFEF5350), Color(0xFFB71C1C))
        else -> GameTheme(Color(0xFFFFD600), Color(0xFFF57F17))
    }
}
