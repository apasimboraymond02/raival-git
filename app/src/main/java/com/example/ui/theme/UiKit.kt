package com.example.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Consistent pressable card: tonal surface + fixed corner radius, scales down
 * while pressed for tactile feedback.
 */
@Composable
fun RaivalTapCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ShapeTokens.card,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderColor: Color? = null,
    elevation: Dp = ElevationTokens.low,
    glowColor: Color? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "RaivalTapCardScale"
    )

    val cardModifier = if (glowColor != null && isPressed && enabled) {
        // Bolder neon under-glow while pressed for instant tactile feedback
        modifier
            .scale(scale)
            .shadow(
                elevation = ElevationTokens.medium,
                shape = shape,
                ambientColor = glowColor.copy(alpha = 0.6f),
                spotColor = glowColor.copy(alpha = 0.8f)
            )
    } else {
        modifier.scale(scale)
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = borderColor?.let { BorderStroke(1.dp, it) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        bounded = true
                    ),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick
                )
        ) {
            content()
        }
    }
}

/** Standard tinted pill containing an icon — used for chips, quick actions, badges. */
@Composable
fun RaivalIconPill(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = IconTokens.md,
    pillColor: Color = tint.copy(alpha = 0.16f),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(ShapeTokens.chip)
            .background(pillColor)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.clip(ShapeTokens.chip)
        )
    }
}

/**
 * Neon statement card: colored glow under-glow + bright rim border, with a
 * springy press-scale when clickable. Optional — does not force the glow.
 */
@Composable
fun RaivalNeonCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: Shape = ShapeTokens.card,
    glowColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderAlpha: Float = 0.55f,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "RaivalNeonCardScale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isPressed) ElevationTokens.medium else ElevationTokens.low,
                shape = shape,
                ambientColor = glowColor.copy(alpha = 0.6f),
                spotColor = glowColor.copy(alpha = 0.8f)
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, glowColor.copy(alpha = borderAlpha))
    ) {
        if (onClick != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(
                            color = glowColor.copy(alpha = 0.25f),
                            bounded = true
                        ),
                        role = Role.Button,
                        onClick = onClick
                    )
            ) {
                content()
            }
        } else {
            content()
        }
    }
}