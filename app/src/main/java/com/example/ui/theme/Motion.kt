package com.example.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Shared motion language for RAIVAL. Keeps every animation in the app on the
 * same timing/easing so transitions feel cohesive and responsive.
 */
object MotionTokens {
    const val microMs = 120
    const val quickMs = 220
    const val baseMs = 420
    const val slowMs = 700

    val springy: AnimationSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val springSoft: AnimationSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    val eased: AnimationSpec<Float> =
        tween(durationMillis = baseMs, easing = FastOutSlowInEasing)
    val easedSoft: AnimationSpec<Float> =
        tween(durationMillis = slowMs, easing = FastOutSlowInEasing)
}

/** Soft padded haptics for light taps vs. heavy actions. */
val RaivalHapticTick = HapticFeedbackType.TextHandleMove
val RaivalHapticHeavy = HapticFeedbackType.LongPress

/** Bolder neon under-glow. Applies a colored soft shadow matching the rounded shape. */
fun Modifier.raivalGlow(
    color: Color,
    shape: Shape,
    elevation: Dp = 12.dp,
    alpha: Float = 1f
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = color.copy(alpha = 0.5f * alpha),
    spotColor = color.copy(alpha = 0.7f * alpha)
)

/** Continuous breathing glow around any surface — the "amped" alternative to a static border. */
@Composable
fun RaivalBreathingGlow(
    color: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    baseElevation: Dp = 6.dp,
    pulseElevation: Dp = 18.dp,
    durationMs: Int = 1500,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "raivalBreathingGlow")
    val ratio by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowRatio"
    )
    val elevation = baseElevation + (pulseElevation - baseElevation) * ratio
    Box(
        modifier = modifier.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = color.copy(alpha = 0.5f),
            spotColor = color.copy(alpha = 0.65f)
        )
    ) {
        content()
    }
}

/**
 * Staggered entrance for list sections. Content fades in, slides up and pops
 * from slightly smaller scale, delayed by [index] for a cascading reveal.
 */
@Composable
fun RaivalStaggerEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    baseDelayMs: Int = 60,
    staggerMs: Int = 90,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay((baseDelayMs + index * staggerMs).toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(MotionTokens.baseMs, easing = FastOutSlowInEasing),
        label = "staggerAlpha"
    )
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 28f,
        animationSpec = tween(MotionTokens.baseMs, easing = FastOutSlowInEasing),
        label = "staggerY"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.96f,
        animationSpec = MotionTokens.springy,
        label = "staggerScale"
    )
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = translateY
            scaleX = scale
            scaleY = scale
        }
    ) {
        content()
    }
}

/** Pulsing "LIVE/online" indicator dot with a soft glow halo. */
@Composable
fun RaivalPulsingDot(
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    pulseDurationMs: Int = 1100,
) {
    val transition = rememberInfiniteTransition(label = "raivalPulsingDot")
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            tween(pulseDurationMs, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "dotScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            tween(pulseDurationMs, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = CircleShape,
            ambientColor = color,
            spotColor = color
        )
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .background(color = color, shape = CircleShape)
        )
    }
}