package com.example.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.core.content.edit  // KTX extension for SharedPreferences
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaivalViewModel
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random

// -----------------------------------------------------------------
// 1. ANIMATED STARTUP SPLASH SCREEN WITH GAMING CANVAS BACKGROUND
// -----------------------------------------------------------------
@Composable
fun EnhancedStartupSplashScreen(onTimeout: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "Splash Background")
    
    // Diagonal sliding lasers in canvas
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Laser Offset"
    )

    // Grid pulsing scale
    val gridOpacity by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Grid Pulse"
    )

// Logo scale and spring bounce
    var logoScale by remember { mutableFloatStateOf(0f) }
    var logoAlpha by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) { value, _ ->
            logoScale = value
            logoAlpha = value.coerceIn(0f, 1f)
        }
        
        delay(2500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RaivalBackground)
            .drawBehind {
                val width = size.width
                val height = size.height

                // Draw neon cyberpunk horizontal and vertical grid lines
                val gridSpacing = 60.dp.toPx()
                val gridColor = RaivalElectricPurple.copy(alpha = gridOpacity)
                
                // Vertical lines
                var x = 0f
                while (x < width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += gridSpacing
                }

                // Horizontal lines
                var y = 0f
                while (y < height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += gridSpacing
                }

                // Cyber diagonal laser beams
                drawLine(
                    color = RaivalPrimary.copy(alpha = 0.4f),
                    start = Offset(laserOffset, 0f),
                    end = Offset(laserOffset - 400f, height),
                    strokeWidth = 4.dp.toPx()
                )
                drawLine(
                    color = RaivalAccent.copy(alpha = 0.3f),
                    start = Offset(width - laserOffset, 0f),
                    end = Offset(width - laserOffset + 400f, height),
                    strokeWidth = 3.dp.toPx()
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Glowing circular backdrop for the logo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .graphicsLayer(
                        scaleX = logoScale,
                        scaleY = logoScale,
                        alpha = logoAlpha
                    )
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(RaivalElectricPurple.copy(alpha = 0.6f), Color.Transparent),
                                center = center,
                                radius = size.minDimension / 1.5f
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "Esports Arena",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(90.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "RAIVAL",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.White,
                modifier = Modifier.scale(logoScale)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "THE ULTIMATE MOBILE ESPORTS ARENA",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = RaivalPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(logoAlpha)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Gaming Radar Loading Spinner
            RadarProgressIndicator(
                color = RaivalPrimary,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

// Custom radar spinner utilizing infinite rotation & fade arcs
@Composable
fun RadarProgressIndicator(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "Radar Loading")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "Radar Angle"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        
        // Draw outer tech circle
        drawCircle(
            color = color.copy(alpha = 0.15f),
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw active rotating neon sweep
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

// -----------------------------------------------------------------
// 2. INTERACTIVE MULTI-PAGE ONBOARDING EXPERIENCE WITH CANVAS ART
// -----------------------------------------------------------------
data class OnboardingPageData(
    val title: String,
    val description: String,
    val illustrationType: String
)



@Composable
fun BracketOnboardingAnimation() {
    val transition = rememberInfiniteTransition(label = "Bracket Animation")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Bracket Progress"
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val w = size.width
        val h = size.height

        val p1 = Offset(w * 0.15f, h * 0.2f)
        val p2 = Offset(w * 0.15f, h * 0.5f)
        val p3 = Offset(w * 0.15f, h * 0.8f)

        val semi1 = Offset(w * 0.45f, h * 0.35f)
        val semi2 = Offset(w * 0.45f, h * 0.65f)

        val champion = Offset(w * 0.8f, h * 0.5f)

        // Connections Background
        val linkColor = RaivalSurfaceLight.copy(alpha = 0.5f)
        drawLine(color = linkColor, start = p1, end = Offset(w * 0.3f, p1.y), strokeWidth = 2.dp.toPx())
        drawLine(color = linkColor, start = p2, end = Offset(w * 0.3f, p2.y), strokeWidth = 2.dp.toPx())
        drawLine(color = linkColor, start = Offset(w * 0.3f, p1.y), end = Offset(w * 0.3f, p2.y), strokeWidth = 2.dp.toPx())
        drawLine(color = linkColor, start = Offset(w * 0.3f, h * 0.35f), end = semi1, strokeWidth = 2.dp.toPx())

        drawLine(color = linkColor, start = semi1, end = Offset(w * 0.62f, semi1.y), strokeWidth = 2.dp.toPx())
        drawLine(color = linkColor, start = semi2, end = Offset(w * 0.62f, semi2.y), strokeWidth = 2.dp.toPx())
        drawLine(color = linkColor, start = Offset(w * 0.62f, semi1.y), end = Offset(w * 0.62f, semi2.y), strokeWidth = 2.dp.toPx())
        drawLine(color = linkColor, start = Offset(w * 0.62f, h * 0.5f), end = champion, strokeWidth = 2.dp.toPx())

        // Animated neon drawing lines
        if (animProgress > 0.1f) {
            val drawProgress = kotlin.math.min(1f, (animProgress - 0.1f) * 4f)
            drawLine(
                color = RaivalPrimary,
                start = p1,
                end = p1 + (Offset(w * 0.3f, p1.y) - p1) * drawProgress,
                strokeWidth = 3.dp.toPx()
            )
        }
        if (animProgress > 0.4f) {
            val drawProgress = kotlin.math.min(1f, (animProgress - 0.4f) * 4f)
            drawLine(
                color = RaivalElectricPurple,
                start = Offset(w * 0.3f, h * 0.35f),
                end = Offset(w * 0.3f, h * 0.35f) + (semi1 - Offset(w * 0.3f, h * 0.35f)) * drawProgress,
                strokeWidth = 3.dp.toPx()
            )
        }
        if (animProgress > 0.7f) {
            val drawProgress = kotlin.math.min(1f, (animProgress - 0.7f) * 4f)
            drawLine(
                color = RaivalAccent,
                start = semi1,
                end = semi1 + (Offset(w * 0.62f, semi1.y) - semi1) * drawProgress,
                strokeWidth = 3.dp.toPx()
            )
            drawLine(
                color = RaivalSecondary,
                start = Offset(w * 0.62f, h * 0.5f),
                end = Offset(w * 0.62f, h * 0.5f) + (champion - Offset(w * 0.62f, h * 0.5f)) * drawProgress,
                strokeWidth = 4.dp.toPx()
            )
        }

        // Draw Node circles
        drawCircle(color = RaivalSurface, radius = 10.dp.toPx(), center = p1)
        drawCircle(color = RaivalSurface, radius = 10.dp.toPx(), center = p2)
        drawCircle(color = RaivalSurface, radius = 10.dp.toPx(), center = p3)

        drawCircle(color = if (animProgress > 0.3f) RaivalPrimary else RaivalSurface, radius = 14.dp.toPx(), center = semi1)
        drawCircle(color = if (animProgress > 0.5f) RaivalElectricPurple else RaivalSurface, radius = 14.dp.toPx(), center = semi2)

        // Champion glowing node
        val champPulse = 1f + 0.12f * sin(animProgress * Math.PI.toFloat() * 4f)
        drawCircle(
            color = if (animProgress > 0.8f) RaivalSecondary else RaivalSurface,
            radius = 18.dp.toPx() * champPulse,
            center = champion
        )
    }
}

@Composable
fun ChatOnboardingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "Chat Bubble")
    val count by infiniteTransition.animateValue(
        initialValue = 1,
        targetValue = 4,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Chat Steps"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(RaivalSurface, shape = RoundedCornerShape(16.dp))
            .border(1.dp, RaivalElectricPurple.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(RaivalBrightLime, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Casual Match Room #104", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text("3 online", color = RaivalTextSecondary, fontSize = 9.sp)
        }

        // Animated chat bubbles
        AnimatedVisibility(visible = count >= 1, enter = slideInHorizontally() + fadeIn()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Box(
                    modifier = Modifier
                        .background(RaivalSurfaceLight, shape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp))
                        .padding(10.dp)
                ) {
                    Text("Yo! Ready for the tournaments?", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        AnimatedVisibility(visible = count >= 2, enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .background(RaivalPrimary, shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp))
                        .padding(10.dp)
                ) {
                    Text("Always! DLS Mobile Cup starting in 5 mins", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedVisibility(visible = count >= 3, enter = slideInHorizontally() + fadeIn()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Box(
                    modifier = Modifier
                        .background(RaivalSurfaceLight, shape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp))
                        .padding(10.dp)
                ) {
                    Text("Awesome. Let's make it a double payout challenge!", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun CoinEarningOnboardingAnimation() {
    val transition = rememberInfiniteTransition(label = "Coin Progress")
    val levelProgress by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Level Progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Rotating golden coin
        val coinRotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Coin Rotation"
        )

        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = null,
            tint = RaivalSecondary,
            modifier = Modifier
                .size(72.dp)
                .rotate(coinRotation)
        )

        // Custom level badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(RaivalSurface, shape = RoundedCornerShape(12.dp))
                .border(1.dp, RaivalSecondary.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "LEVEL 1",
                color = RaivalSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            LinearProgressIndicator(
                progress = levelProgress,
                color = RaivalSecondary,
                trackColor = RaivalSurfaceLight,
                modifier = Modifier
                    .width(100.dp)
                    .height(6.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "LEVEL 2",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Earning multiplier activated: 1.5x XP!",
            color = RaivalBrightLime,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WelcomeOnboardingAnimation() {
    val transition = rememberInfiniteTransition(label = "Welcome Pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse Scale"
    )

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(RaivalPrimary.copy(alpha = 0.25f), Color.Transparent),
                    radius = size.width / 2f
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(pulseScale)
        ) {
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                tint = RaivalPrimary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "RAIVAL",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                text = "ARENA OF CHAMPIONS",
                color = RaivalAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(viewModel: RaivalViewModel, onFinished: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var currentPage by remember { mutableIntStateOf(0) }
    var usernameInput by remember { mutableStateOf(currentUser?.username ?: "") }
    var selectedGames by remember { mutableStateOf(setOf("eFootball", "FC Mobile", "DLS")) }
    var selectedTime by remember { mutableStateOf("Evening") }
    var skillLevel by remember { mutableStateOf("Intermediate") }

    // Username Checking States
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }

    // Social linking states
    var googleConnected by remember { mutableStateOf(currentUser?.socialFbConnected ?: false) }
    var appleConnected by remember { mutableStateOf(currentUser?.socialAppleConnected ?: false) }
    var connectingGoogle by remember { mutableStateOf(false) }
    var connectingApple by remember { mutableStateOf(false) }

// Welcome Bonus Chest Overlay
    var showBonusPopup by remember { mutableStateOf(false) }
    var popupProgress by remember { mutableIntStateOf(0) } // 0 = closed chest, 1 = open chest, 2 = coins awarded

    LaunchedEffect(usernameInput) {
        if (usernameInput.trim().length < 3) {
            usernameAvailable = null
            return@LaunchedEffect
        }
        isCheckingUsername = true
        delay(600)
        val exists = allUsers.any { it.username.equals(usernameInput.trim(), ignoreCase = true) && it.id != currentUser?.id }
        isCheckingUsername = false
        usernameAvailable = !exists
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RaivalBackground)
    ) {
        // Skip Button in Top-Right
        if (currentPage < 4) {
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Skip sets standard defaults and claim
                    scope.launch {
                        val user = viewModel.currentUser.value
                        if (user != null) {
                            val updated = user.copy(
                                welcomeBonusClaimed = true,
                                coinBalance = user.coinBalance + 500
                            )
                            viewModel.repository.saveUserProgress(updated)
                        }
                        onFinished()
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .testTag("onboarding_skip_btn")
            ) {
                Text(
                    text = "SKIP",
                    fontWeight = FontWeight.Bold,
                    color = RaivalPrimary,
                    fontSize = 13.sp
                )
            }
        }

        // Onboarding Pages Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Animated illustration view
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
                    },
                    label = "Onboarding Animation Frame"
                ) { targetPage ->
                    when (targetPage) {
                        0 -> WelcomeOnboardingAnimation()
                        1 -> BracketOnboardingAnimation()
                        2 -> ChatOnboardingAnimation()
                        3 -> CoinEarningOnboardingAnimation()
                        4 -> {
                            // Render game selection & profile customization fields
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Text(
                                        text = "🎮 PERSONALIZE PROFILE",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }

                                // Username Input Field
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("CHOOSE USERNAME", color = RaivalTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = usernameInput,
                                            onValueChange = { usernameInput = it },
                                            modifier = Modifier.fillMaxWidth().testTag("username_input"),
                                            placeholder = { Text("e.g. Accra_Champ", color = RaivalTextSecondary.copy(alpha = 0.5f)) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = RaivalPrimary,
                                                unfocusedBorderColor = RaivalSurfaceLight,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = RaivalSurface,
                                                unfocusedContainerColor = RaivalSurface
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (isCheckingUsername) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = RaivalPrimary)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Checking username...", color = RaivalTextSecondary, fontSize = 10.sp)
                                            }
                                        } else if (usernameAvailable != null) {
                                            if (usernameAvailable == true) {
                                                Text("✅ Username available!", color = RaivalBrightLime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Text("❌ Username already taken!", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Favorite Games Selection
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("FAVORITE GAMES (CHOOSE MULTIPLE)", color = RaivalTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val games = listOf("eFootball", "FC Mobile", "DLS", "CODM", "PUBG")
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            games.forEach { game ->
                                                val isSelected = selectedGames.contains(game)
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = if (isSelected) RaivalPrimary else RaivalSurface,
                                                            shape = RoundedCornerShape(18.dp)
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isSelected) RaivalPrimary else RaivalSurfaceLight,
                                                            shape = RoundedCornerShape(18.dp)
                                                        )
                                                        .clickable {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            selectedGames = if (isSelected) selectedGames - game else selectedGames + game
                                                        }
                                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = game,
                                                            color = if (isSelected) Color.Black else Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        if (isSelected) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Connect Google/Apple
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("CONNECT SOCIAL CHANNELS FOR SECURE PAYOUTS", color = RaivalTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (!googleConnected && !connectingGoogle) {
                                                        scope.launch {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            connectingGoogle = true
                                                            delay(1000)
                                                            connectingGoogle = false
                                                            googleConnected = true
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (googleConnected) RaivalBrightLime.copy(alpha = 0.2f) else RaivalSurface
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, if (googleConnected) RaivalBrightLime else RaivalSurfaceLight)
                                            ) {
                                                if (connectingGoogle) {
                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RaivalPrimary, strokeWidth = 2.dp)
                                                } else {
                                                    Icon(Icons.Default.Link, contentDescription = null, tint = if (googleConnected) RaivalBrightLime else Color.White, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(if (googleConnected) "Google Joined" else "Link Google", color = if (googleConnected) RaivalBrightLime else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    if (!appleConnected && !connectingApple) {
                                                        scope.launch {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            connectingApple = true
                                                            delay(1000)
                                                            connectingApple = false
                                                            appleConnected = true
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (appleConnected) RaivalBrightLime.copy(alpha = 0.2f) else RaivalSurface
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, if (appleConnected) RaivalBrightLime else RaivalSurfaceLight)
                                            ) {
                                                if (connectingApple) {
                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RaivalPrimary, strokeWidth = 2.dp)
                                                } else {
                                                    Icon(Icons.Default.Link, contentDescription = null, tint = if (appleConnected) RaivalBrightLime else Color.White, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(if (appleConnected) "Apple Linked" else "Link Apple", color = if (appleConnected) RaivalBrightLime else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Match Time selection & Skill slider
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("MATCH PREFERENCE", color = RaivalTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("Afternoon", "Evening", "Night").forEach { time ->
                                                val isSel = selectedTime == time
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .background(if (isSel) RaivalAccent else RaivalSurface, RoundedCornerShape(10.dp))
                                                        .border(1.dp, if (isSel) RaivalAccent else RaivalSurfaceLight, RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            selectedTime = time
                                                        }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(time, color = if (isSel) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("YOUR SKILL LEVEL: $skillLevel", color = RaivalTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("Beginner", "Intermediate", "Pro").forEach { lvl ->
                                                val isSel = skillLevel == lvl
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .background(if (isSel) RaivalPrimary else RaivalSurface, RoundedCornerShape(10.dp))
                                                        .border(1.dp, if (isSel) RaivalPrimary else RaivalSurfaceLight, RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            skillLevel = lvl
                                                        }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(lvl, color = if (isSel) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Description Details card
            if (currentPage < 4) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val titles = listOf(
                        "Welcome to Raival Arena",
                        "Compete in Automated Brackets",
                        "Real-time Chat & Socials",
                        "Win Coins & Multiply Payouts"
                    )
                    val descs = listOf(
                        "Ghana's premier online gaming hub for competitive eFootball, FC Mobile, DLS, and classic shooter tournaments.",
                        "Register in premium daily tournaments. Brackets process, record scores, and match results completely automatically!",
                        "Chat in real-time within tournament lobbies, match queues, casual zones, and build local gaming clans.",
                        "Dominate match challenges to claim real cash GHS payouts, unlock custom avatar profiles, and claim double multipliers."
                    )
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                        },
                        label = "Description Texts"
                    ) { targetPage ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = titles[targetPage],
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = descs[targetPage],
                                fontSize = 13.sp,
                                color = RaivalTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Bottom Navigation and Page Indicators
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (0..4).forEach { index ->
                        val isSelected = index == currentPage
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "Dot Width"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isSelected) RaivalPrimary else RaivalSurfaceLight,
                            label = "Dot Color"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth)
                                .background(dotColor, shape = CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentPage = index
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation Control Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    if (currentPage > 0) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentPage--
                            }
                        ) {
                            Text("BACK", color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(modifier = Modifier.size(1.dp))
                    }

                    // Next / Get Started Gradient Button
                    val isLastPage = currentPage == 4
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isLastPage) {
                                if (usernameAvailable == false) {
                                    // block if username invalid
                                } else {
                                    showBonusPopup = true
                                }
                            } else {
                                currentPage++
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .width(if (isLastPage) 170.dp else 120.dp)
                            .testTag("onboarding_next_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = if (isLastPage) listOf(RaivalElectricPurple, RaivalAccent) else listOf(RaivalPrimary, RaivalElectricPurple)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isLastPage) "FINISH & REWARD" else "NEXT",
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        // Welcome Bonus Chest / Gift Box Popup Overlay Dialog
        if (showBonusPopup) {
            Dialog(onDismissRequest = {}) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (popupProgress == 0) {
                            Text(
                                text = "🎁 UNLOCK WELCOME PACKAGE",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )

                            // Canvas drawing of a pulsing lockbox/gift chest
                            val transitionChest = rememberInfiniteTransition(label = "chest")
                            val chestScale by transitionChest.animateFloat(
                                initialValue = 0.95f,
                                targetValue = 1.05f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "chestScale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .scale(chestScale),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = null,
                                    tint = RaivalSecondary,
                                    modifier = Modifier.size(90.dp)
                                )
                            }

                            Text(
                                text = "Welcome to Raival! Open your starter gift bag containing 500 Gold Coins, Starter badge title, and a 2x Level multiplier.",
                                color = RaivalTextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    popupProgress = 1
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary)
                            ) {
                                Text("OPEN CHEST", color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        } else {
                            // Shower of gold coins text
                            Text(
                                text = "🎉 WELCOME BONUS UNLOCKED!",
                                color = RaivalBrightLime,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )

                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = RaivalSecondary,
                                modifier = Modifier.size(100.dp)
                            )

                            Text(
                                text = "+500 COINS ADDED!",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )

                            Text(
                                text = "Success! You are officially ready to compete in automated tournament matches and casual lobbies.",
                                color = RaivalTextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Save the final profile details in the background database
                                    scope.launch {
                                        val u = viewModel.currentUser.value
                                        if (u != null) {
                                            val updated = u.copy(
                                                username = if (usernameInput.trim().isNotEmpty()) usernameInput.trim() else u.username,
                                                preferredGame = selectedGames.firstOrNull() ?: u.preferredGame,
                                                coinBalance = u.coinBalance + 500,
                                                welcomeBonusClaimed = true,
                                                socialFbConnected = googleConnected,
                                                socialAppleConnected = appleConnected,
                                                completedMissions = if (u.completedMissions.isEmpty()) "welcome_bonus" else "${u.completedMissions},welcome_bonus"
                                            )
                                            viewModel.repository.saveUserProgress(updated)
                                        }
                                        showBonusPopup = false
                                        onFinished()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                            ) {
                                Text("COLLECT & ENTER LOBBY", color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// 3. CONFETTI CELEBRATION COMPOSE ANIMATOR
// -----------------------------------------------------------------
data class ConfettiParticle(
    val x: Float,
    var y: Float,
    val size: Float,
    val color: Color,
    val speed: Float,
    val angle: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiCelebrationOverlay(
    modifier: Modifier = Modifier,
    durationMs: Long = 4000,
    onFinished: () -> Unit = {}
) {
    var isRunning by remember { mutableStateOf(true) }
    val particles = remember {
        List(80) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = -50f,
                size = Random.nextFloat() * 12f + 8f,
                color = listOf(RaivalPrimary, RaivalElectricPurple, RaivalAccent, RaivalSecondary, RaivalBrightLime).random(),
                speed = Random.nextFloat() * 8f + 5f,
                angle = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 4f - 2f
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(durationMs)
        isRunning = false
        onFinished()
    }

    if (isRunning) {
        val infiniteTransition = rememberInfiniteTransition(label = "Confetti Wave")
        val ticker by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(16, easing = LinearEasing)),
            label = "Confetti Ticker"
        )

        Canvas(modifier = modifier.fillMaxSize()) {
            particles.forEach { p ->
                // Update position
                p.y += p.speed
                if (p.y > size.height) {
                    p.y = -20f
                }
                
                val currentX = (p.x * size.width) + sin(p.y / 30f) * 15f
                
                // Draw falling particle
                drawRoundRect(
                    color = p.color,
                    topLeft = Offset(currentX, p.y),
                    size = Size(p.size, p.size),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )
            }
        }
    }
}

// -----------------------------------------------------------------
// 4. FLOATING COIN ACCRETION ANIMATION (+10 COINS)
// -----------------------------------------------------------------
@Composable
fun FloatingCoinAnimation(
    trigger: Boolean,
    onAnimationEnd: () -> Unit
) {
    if (trigger) {
        var startAnim by remember { mutableStateOf(false) }
        
        LaunchedEffect(trigger) {
            startAnim = true
            delay(1500)
            onAnimationEnd()
        }

        val animProgress by animateFloatAsState(
            targetValue = if (startAnim) 1f else 0f,
            animationSpec = tween(1200, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)),
            label = "Coin Progress"
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = (-150 * animProgress).dp)
                    .alpha(1f - animProgress)
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = RaivalSecondary,
                    modifier = Modifier.size((40 + (20 * animProgress)).dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "+200 Coins",
                    color = RaivalBrightLime,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

// -----------------------------------------------------------------
// 5. MODERN REDESIGNED GAMING HOME TAB
// -----------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RedesignedHomeTab(
    viewModel: RaivalViewModel,
    currentUser: User,
    onTabSwitch: (Int) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenProgression: (Int) -> Unit,
    onOpenCommunity: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val tournaments by viewModel.tournaments.collectAsState()
    val upcomingTournaments = remember(tournaments) {
        tournaments.filter { !it.isHidden }.take(5)
    }

    var coinRefillTrigger by remember { mutableStateOf(false) }
    var successCelebration by remember { mutableStateOf(false) }

    // Mission states
    var mission1Claimed by remember { mutableStateOf(false) }
    var mission2Claimed by remember { mutableStateOf(false) }

    // Friends Online Dialog state
    var showFriendsDialog by remember { mutableStateOf(false) }

    // Guided Walkthrough state
    val hasSeenTour = remember { viewModel.sharedPrefs.getBoolean("has_seen_tour_v3", false) }
    var showTourStage by remember { mutableStateOf(if (!hasSeenTour) 1 else 0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RaivalBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. WELCOME HEADER WITH STUNNING BACKGROUND IMAGE
            item {
                RaivalStaggerEntrance(index = 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, RaivalPrimary.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
                ) {
                    // Background Image
                    Image(
                        painter = painterResource(id = R.drawable.bg_gaming_1),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alpha = 0.85f
                    )
                    // High-contrast translucent overlay for ideal text contrast
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.50f),
                                        RaivalSurface.copy(alpha = 0.25f)
                                    )
                                )
                            )
                    )

                    // Welcome Content
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Avatar with dynamic scale-down press
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(RaivalPrimary.copy(alpha = 0.2f))
                                .border(2.dp, RaivalPrimary, CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpenProfile()
                                 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.username.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome back, Champion!",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = currentUser.fullName,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Level/XP Progress Bar
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Lvl ${currentUser.level}",
                                    color = RaivalPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                LinearProgressIndicator(
                                    progress = (currentUser.xp % 100).toFloat() / 100f,
                                    color = RaivalPrimary,
                                    trackColor = RaivalSurface.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                        }

                        // Clickable Coin Balance Wallet display
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                                .border(1.dp, RaivalSecondary.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTabSwitch(3) // Wallet tab directly
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = RaivalSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${currentUser.coinBalance}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "GHS ${String.format(java.util.Locale.US, "%.2f", currentUser.balance)}",
                                color = RaivalBrightLime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                }
                }

            // 2. DAILY SUMMARY CARD (Gamification & Streak Tracking)
            item {
                RaivalStaggerEntrance(index = 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                    border = BorderStroke(1.dp, RaivalAccent.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = RaivalAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "TODAY'S GAMIFIED SUMMARY",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = "🔥 Day ${currentUser.winStreak + 1} Streak",
                                color = RaivalSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Matches", color = RaivalTextSecondary, fontSize = 10.sp)
                                Text("${currentUser.tournamentsPlayed} Played", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(RaivalSurfaceLight))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Won", color = RaivalTextSecondary, fontSize = 10.sp)
                                Text("${currentUser.wins} Wins", color = RaivalBrightLime, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(RaivalSurfaceLight))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("XP Bonus", color = RaivalTextSecondary, fontSize = 10.sp)
                                Text("+50 XP", color = RaivalPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                }
                }

            // 3. QUICK ACTIONS GRID (Revamped 4 Interactive Buttons)
            item {
                RaivalStaggerEntrance(index = 2) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "⚡ QUICK ACTIONS",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            title = "FIND MATCH",
                            subtitle = "Join Casual 1v1",
                            icon = Icons.Default.FlashOn,
                            color = RaivalPrimary,
                            backgroundImageRes = R.drawable.bg_gaming_1,
                            modifier = Modifier.weight(1f)
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTabSwitch(2) // Matches Tab
                        }

                        QuickActionCard(
                            title = "TOURNAMENTS",
                            subtitle = "Automated Cups",
                            icon = Icons.Default.EmojiEvents,
                            color = RaivalElectricPurple,
                            backgroundImageRes = R.drawable.bg_gaming_2,
                            modifier = Modifier.weight(1f)
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTabSwitch(1) // Tournaments Tab
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            title = "FRIENDS",
                            subtitle = "Challenge Buddy",
                            icon = Icons.Default.People,
                            color = RaivalSecondary,
                            backgroundImageRes = R.drawable.bg_gaming_3,
                            modifier = Modifier.weight(1f)
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showFriendsDialog = true
                        }

                        QuickActionCard(
                            title = "MY WALLET",
                            subtitle = "Manage Cash & Coins",
                            icon = Icons.Default.AccountBalanceWallet,
                            color = RaivalAccent,
                            backgroundImageRes = R.drawable.bg_gaming_4,
                            modifier = Modifier.weight(1f)
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTabSwitch(3) // Wallet Tab
                        }
                    }
                }
                }
                }

            // 4. SMART PERSONALIZED SUGGESTIONS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🤖 RECOMMENDED FOR YOU",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Suggestion 1: Game & Skill match
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(RaivalPrimary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Personalized Matchup starting soon",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Based on your preference for ${currentUser.preferredGame} and skill level.",
                                        color = RaivalTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Button(
                                    onClick = { onTabSwitch(1) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("JOIN", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = RaivalSurfaceLight.copy(alpha = 0.5f))

                            // Suggestion 2: Friends Online
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(RaivalBrightLime.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.size(8.dp).background(RaivalBrightLime, CircleShape))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Accra_United is Online",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Waiting in Casual Arena. Average match connection: 12 seconds.",
                                        color = RaivalTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Button(
                                    onClick = { onTabSwitch(2) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalBrightLime),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("PLAY", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // FEATURED TOURNAMENTS CAROUSEL
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 FEATURED TOURNAMENTS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "VIEW ALL",
                            color = RaivalPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { onTabSwitch(1) }
                        )
                    }

                    if (upcomingTournaments.isEmpty()) {
                        EmptyIllustrationState(
                            title = "No active tournaments",
                            description = "Our system is orchestrating the brackets. Check back shortly!"
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(upcomingTournaments) { tour ->
                                CardFlipItem(tour = tour) {
                                    onTabSwitch(1) // Open tournaments page
                                }
                            }
                        }
                    }
                }
            }

            // ACTIVE MATCHES COUNTDOWN TIMER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                    border = BorderStroke(1.dp, RaivalAccent.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(RaivalAccent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = RaivalAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Next Automated Brackets Launch",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Auto-filler bots and brackets lock immediately on launch",
                                color = RaivalTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Countdown clock
                        Box(
                            modifier = Modifier
                                .background(RaivalBackground, shape = RoundedCornerShape(8.dp))
                                .border(1.dp, RaivalAccent.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "28:42",
                                color = RaivalAccent,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // DAILY MISSION CHALLENGES
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🎯 DAILY CHALLENGES",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )

                    // Mission 1 Card
                    DailyMissionCard(
                        title = "Claim Welcome Reward Packet",
                        points = "+200 Coins",
                        progress = if (mission1Claimed) 1f else 0f,
                        isClaimed = mission1Claimed
                    ) {
                        if (!mission1Claimed) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.earnCoins(200)
                            mission1Claimed = true
                            coinRefillTrigger = true
                            successCelebration = true
                        }
                    }

                    // Mission 2 Card
                    DailyMissionCard(
                        title = "Practice with Training Bots",
                        points = "+100 Coins",
                        progress = if (mission2Claimed) 1f else 0.5f,
                        isClaimed = mission2Claimed
                    ) {
                        if (!mission2Claimed) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.earnCoins(100)
                            mission2Claimed = true
                            coinRefillTrigger = true
                            successCelebration = true
                        }
                    }
                }
            }

            // LOCAL COMMUNITY RECENT ACTIVITY FEED
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📢 TOURNAMENT LIVE BULLETINS",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )

                    ActivityFeedItem(
                        icon = Icons.Default.WorkspacePremium,
                        msg = "Ghanaian Champion **Kojo_Skills** claimed 500 GHS in DLS Mobile Arena!",
                        time = "2 mins ago"
                    )

                    ActivityFeedItem(
                        icon = Icons.Default.AutoAwesome,
                        msg = "Auto-Orchestrator successfully filled bracket slots for FC Mobile Cup.",
                        time = "10 mins ago"
                    )

                    ActivityFeedItem(
                        icon = Icons.Default.Campaign,
                        msg = "Admin created new automatic cup self-hosted match brackets.",
                        time = "1 hour ago"
                    )
                }
            }
        }

        // Friends Challenge Dialog
        if (showFriendsDialog) {
            Dialog(onDismissRequest = { showFriendsDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "👥 ACTIVE FRIENDS ONLINE",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )

                        val onlineFriends = listOf("Accra_United", "Tema_King", "Kojo_Skills", "Kumasi_Star")
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            onlineFriends.forEach { friend ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(RaivalSurfaceLight, shape = RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).background(RaivalBrightLime, CircleShape))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(friend, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showFriendsDialog = false
                                            Toast.makeText(context, "Challenge sent to $friend for 50 Coins!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("CHALLENGE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showFriendsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CLOSE", color = Color.White)
                        }
                    }
                }
            }
        }

        // STEP-BY-STEP GUIDED WALKTHROUGH TOUR OVERLAY
        if (showTourStage in 1..5) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .clickable(enabled = false) {} // block touches
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, RaivalPrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (showTourStage) {
                                    1 -> "🎮 PROFILE HEADER"
                                    2 -> "💰 WALLET PANEL"
                                    3 -> "🔥 DAILY SUMMARY"
                                    4 -> "⚡ QUICK ACTIONS"
                                    else -> "🤖 SMART SUGGESTIONS"
                                },
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = when (showTourStage) {
                                1 -> "Track your progress! Your Level, XP bar, and custom usernames are updated in real-time as you win tournaments."
                                2 -> "Manage your coins! Tap on the coin indicator to make instant deposits, check GHS payouts, and claim bonuses."
                                3 -> "Maintain your streak! Get daily multipliers, see your total match winnings, and check your active match streak counts."
                                4 -> "One-tap navigation! Launch Quick Matches, enter automated tournament brackets, challenge friends, or check your wallet immediately."
                                else -> "Personalized Matchmaking! Our AI matching engine filters tournament slots that perfectly suit your favorite game and skill level."
                            },
                            color = RaivalTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showTourStage = 0
                                    viewModel.sharedPrefs.edit().putBoolean("has_seen_tour_v3", true).apply()
                                }
                            ) {
                                Text("SKIP TOUR", color = RaivalTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (showTourStage == 5) {
                                        showTourStage = 0
                                        viewModel.sharedPrefs.edit().putBoolean("has_seen_tour_v3", true).apply()
                                        // Award completing tour milestone
                                        viewModel.earnCoins(10)
                                        successCelebration = true
                                        coinRefillTrigger = true
                                        Toast.makeText(context, "🏆 Achievement Unlocked: Getting Started! +10 Coins", Toast.LENGTH_LONG).show()
                                    } else {
                                        showTourStage++
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                            ) {
                                Text(if (showTourStage == 5) "FINISH" else "NEXT", color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        // Float Confetti celebration
        if (successCelebration) {
            ConfettiCelebrationOverlay {
                successCelebration = false
            }
        }

        // Float Flying "+10 Coins" text animation
        FloatingCoinAnimation(trigger = coinRefillTrigger) {
            coinRefillTrigger = false
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    backgroundImageRes: Int? = null,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "Action Scale"
    )

    Card(
        modifier = modifier
            .scale(scaleAnim)
            .shadow(
                elevation = if (isPressed) 14.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = color.copy(alpha = if (isPressed) 0.55f else 0f),
                spotColor = color.copy(alpha = if (isPressed) 0.75f else 0f)
            )
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } catch (e: Exception) {
                            // Cancelled if dragged away/scrolled
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = {
                        onClick()
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(115.dp)) {
            if (backgroundImageRes != null) {
                Image(
                    painter = painterResource(id = backgroundImageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    alpha = 0.85f
                )
                // Linear gradient overlay for perfect readability while maintaining background visibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.20f),
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )
            }
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (backgroundImageRes != null) Color.Black.copy(alpha = 0.4f)
                            else color.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = subtitle,
                        color = if (backgroundImageRes != null) Color.LightGray else RaivalTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CardFlipItem(
    tour: Tournament,
    onClick: () -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "Flip Animation"
    )

    Card(
        modifier = Modifier
            .width(260.dp)
            .height(150.dp)
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 12 * density
            }
            .clickable {
                isFlipped = !isFlipped
            },
        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (tour.isAutoHosted) RaivalPrimary.copy(alpha = 0.4f) else RaivalElectricPurple.copy(alpha = 0.3f))
    ) {
        if (rotationY <= 90f) {
            // Front details
            Box(modifier = Modifier.fillMaxSize()) {
                // Gradient backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(RaivalSurface, RaivalBackground.copy(alpha = 0.6f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = tour.game,
                                color = RaivalPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = tour.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Prize tag
                        Box(
                            modifier = Modifier
                                .background(RaivalAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(1.dp, RaivalAccent, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GHS ${tour.prize.toInt()}",
                                color = RaivalAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Players",
                                color = RaivalTextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${tour.players}/${tour.maxPlayers}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Entry Fee",
                                color = RaivalTextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "GHS ${tour.entryFee.toInt()}",
                                color = RaivalBrightLime,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // Back details
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.rotationY = 180f }
                    .background(RaivalSurfaceLight),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🤖 AUTO-HOSTED MATCH",
                        color = RaivalPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Instant bot filler & self-advancing brackets. Perfect for practicing your esports talent offline or online.",
                        color = Color.White,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("JOIN BRACKETS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun DailyMissionCard(
    title: String,
    points: String,
    progress: Float,
    isClaimed: Boolean,
    onClaim: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
        border = BorderStroke(1.dp, if (isClaimed) RaivalSuccess.copy(alpha = 0.3f) else RaivalPrimary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isClaimed) RaivalSuccess.copy(alpha = 0.15f) else RaivalPrimary.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isClaimed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isClaimed) RaivalSuccess else RaivalPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progress,
                    color = if (isClaimed) RaivalSuccess else RaivalPrimary,
                    trackColor = RaivalBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onClaim,
                enabled = !isClaimed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isClaimed) RaivalSurfaceLight else RaivalPrimary,
                    disabledContainerColor = RaivalSurfaceLight
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = if (isClaimed) "CLAIMED" else "CLAIM $points",
                    color = if (isClaimed) RaivalTextSecondary else Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun ActivityFeedItem(icon: ImageVector, msg: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RaivalSurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RaivalPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = msg,
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = time,
                color = RaivalTextSecondary,
                fontSize = 9.sp
            )
        }
    }
}

// -----------------------------------------------------------------
// 6. CUSTOM SKELETON LOADERS
// -----------------------------------------------------------------
@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    height: Float = 60f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Shimmer")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(RaivalSurfaceLight.copy(alpha = alphaAnim), RoundedCornerShape(12.dp))
    )
}

@Composable
fun TournamentListSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        repeat(5) {
            ShimmerPlaceholder(height = 110f)
        }
    }
}

@Composable
fun LeaderboardSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        repeat(8) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(RaivalSurfaceLight, CircleShape)
                )
                ShimmerPlaceholder(modifier = Modifier.weight(1f), height = 40f)
            }
        }
    }
}

// -----------------------------------------------------------------
// 7. HIGH POLISHED EMPTY AND ERROR STATES
// -----------------------------------------------------------------
@Composable
fun EmptyIllustrationState(
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            tint = RaivalTextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            color = RaivalTextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = actionLabel, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ErrorStateBanner(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = RaivalError.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, RaivalError.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.WifiOff, contentDescription = null, tint = RaivalError)
                Text(
                    text = "Connection Offline",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Text(
                text = message,
                color = RaivalTextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = RaivalError),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("RETRY CONNECTION", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
