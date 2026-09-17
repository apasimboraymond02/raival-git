package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object ShapeTokens {
    val card = RoundedCornerShape(12.dp)
    val cardMedium = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(16.dp)
    val dialog = RoundedCornerShape(24.dp)
    val sheet = RoundedCornerShape(28.dp)
    val chip = RoundedCornerShape(50)
    val bottomBar = RoundedCornerShape(16.dp)
}

object IconTokens {
    val xs = 16.dp
    val sm = 20.dp
    val md = 24.dp
    val lg = 32.dp
    val xl = 40.dp
}

object ElevationTokens {
    val flat = 0.dp
    val low = 2.dp
    val medium = 4.dp
    val high = 8.dp
    val overlay = 16.dp
}

object HeightTokens {
    val chip = 32.dp
    val button = 48.dp
    val input = 56.dp
    val topBar = 56.dp
}