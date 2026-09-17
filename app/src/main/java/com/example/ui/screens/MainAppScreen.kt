
package com.example.ui.screens

import com.example.BuildConfig

import androidx.compose.animation.*

import androidx.compose.foundation.*

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

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.Path

import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.testTag

import androidx.compose.ui.platform.LocalHapticFeedback

import androidx.compose.ui.hapticfeedback.HapticFeedbackType

import androidx.compose.ui.text.font.FontFamily

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.PasswordVisualTransformation

import androidx.compose.ui.text.input.VisualTransformation

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.compose.ui.window.Dialog

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.data.model.*

import com.example.ui.theme.*

import com.example.ui.viewmodel.RaivalViewModel

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.layout.ContentScale

import androidx.compose.animation.core.*

import androidx.compose.ui.draw.alpha

import java.text.SimpleDateFormat

import java.util.*

import com.example.R

private object TournamentBannerResolver {
    private val bannerMap = mapOf(
        "bg_efootball_1" to R.drawable.bg_efootball_1,
        "bg_efootball_2" to R.drawable.bg_efootball_2,
        "bg_efootball_3" to R.drawable.bg_efootball_3,
        "dls_bg_new1" to R.drawable.dls_bg_new1,
        "dls_bg_new2" to R.drawable.dls_bg_new2,
        "dls_bg_new3" to R.drawable.dls_bg_new3,
        "dls_bg_new4" to R.drawable.dls_bg_new4,
        "fc_bg_new1" to R.drawable.fc_bg_new1,
        "fc_bg_new2" to R.drawable.fc_bg_new2,
        "fc_bg_new3" to R.drawable.fc_bg_new3,
        "fc_bg_new4" to R.drawable.fc_bg_new4,
        "bg_gaming_1" to R.drawable.bg_gaming_1,
        "bg_gaming_2" to R.drawable.bg_gaming_2,
        "bg_gaming_3" to R.drawable.bg_gaming_3,
        "bg_gaming_4" to R.drawable.bg_gaming_4
    )

    fun resolve(banner: String?): Int? {
        if (banner == null || banner.isBlank()) return null
        val key = banner.replace(".jpg", "").replace(".png", "")
        return bannerMap[key]
    }
}

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun MainAppScreen(
    viewModel: RaivalViewModel = viewModel(),
    innerPadding: PaddingValues = PaddingValues(0.dp)) {
    // Only show splash once per session; returning without closing skips splash
    var showLocalSplash by rememberSaveable { mutableStateOf(false) }
    val showOnboarding by viewModel.showOnboarding.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (showLocalSplash) {
            EnhancedStartupSplashScreen(
                onTimeout = { showLocalSplash = false }
            )
        }
else if (!isOnline && currentUser == null) {
            OfflineBlockerScreen(viewModel = viewModel)
        }
else if (showOnboarding) {
            OnboardingScreen(
                viewModel = viewModel,
                onFinished = { viewModel.setHasSeenOnboarding() }
            )
        }
else if (currentUser == null) {
            AuthScreen(viewModel)
        }
else {
            val isCloudPulling by viewModel.isCloudPulling.collectAsState()
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isOnline) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RaivalSecondary)
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Offline Mode Active",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OFFLINE MODE: PLAY CASUAL GAMES & TEST RETRO SOUNDS",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                if (isCloudPulling && isOnline) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF00E5FF).copy(alpha = 0.1f))
                            .padding(vertical = 6.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Syncing cloud data...",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                MainTabsScreen(viewModel = viewModel, initialUser = currentUser!!)
            }
        }
    }
}
// -------------------------------------------------------------
// OFFLINE BLOCKER SCREEN
// -------------------------------------------------------------

@Composable
fun OfflineBlockerScreen(viewModel: RaivalViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RaivalBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(RaivalError.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "No Network Connection",
                    tint = RaivalError,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "CONNECTION REQUIRED",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                border = BorderStroke(1.dp, RaivalError.copy(alpha = pulseAlpha * 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "RAIVAL is an online-only competitive platform. Active connection is required to authenticate, load tournament verifications, register for brackets, process MTN/Vodafone MoMo transactions, and prevent match disputes.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        color = RaivalTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = RaivalSecondary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "LISTENING FOR NETWORK RESTORATION...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = RaivalSecondary,
                            modifier = Modifier.alpha(pulseAlpha)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    viewModel.pullAllDataFromCloud()
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry Connection",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FORCE RETRY CONNECTION",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// AUTHENTICATION SCREEN
// -------------------------------------------------------------

@Composable
fun AuthScreen(viewModel: RaivalViewModel) {
    var isLoginMode by remember { mutableStateOf(true) }
    var logoTapCount by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {        
// Logo Section
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(RaivalPrimary.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    logoTapCount++
                    if (logoTapCount >= 5) {
                        email = ""
                        password = ""
                        logoTapCount = 0
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "Raival Logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(70.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "RAIVAL",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            ),
            color = RaivalPrimary
        )
        Text(
            text = "Ghana's Premier Mobile Esports Arena",
            style = MaterialTheme.typography.bodyMedium,
            color = RaivalTextSecondary,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
// Card Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RaivalSurface),
            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isLoginMode) "Welcome Back" else "Create Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isLoginMode) "Sign in to compete and win GHS prizes" else "Register to join active tournaments",
                    style = MaterialTheme.typography.bodySmall,
                    color = RaivalTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = RaivalError,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                if (successMessage.isNotEmpty()) {
                    Text(
                        text = successMessage,
                        color = RaivalSuccess,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }                
// Fields
                if (!isLoginMode) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = RaivalPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLabelColor = RaivalPrimary,
                            focusedIndicatorColor = RaivalPrimary
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = RaivalPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedLabelColor = RaivalPrimary,
                        focusedIndicatorColor = RaivalPrimary
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = RaivalPrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedLabelColor = RaivalPrimary,
                        focusedIndicatorColor = RaivalPrimary
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        errorMessage = ""
                        successMessage = ""
                        if (isLoginMode) {
                            viewModel.signInWithEmail(email, password) { success, msg ->
                                if (success) {
                                    successMessage = msg
                                }
else {
                                    errorMessage = msg
                                }
                            }
                        }
else {
                            viewModel.signUpWithEmail(email, password, username) { success, msg ->
                                if (success) {
                                    successMessage = msg
                                    isLoginMode = true
                                }
else {
                                    errorMessage = msg
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isLoginMode) "SIGN IN" else "CREATE ACCOUNT",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.continueAsGuest() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RaivalSecondary),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "CONTINUE AS GUEST",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { isLoginMode = !isLoginMode }) {
                    Text(
                        text = if (isLoginMode) "Don't have an account? Register" else "Already have an account? Sign In",
                        color = RaivalSecondary
                    )
                }
            }
        }
    }}
// -------------------------------------------------------------
// -------------------------------------------------------------
// MAIN TABBED APP SCREEN
// -------------------------------------------------------------
data class NavigationTabItem(
    val label: String,
    val icon: ImageVector,
    val index: Int,
    val badgeText: String? = null)

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun MainTabsScreen(viewModel: RaivalViewModel, initialUser: User) {
    val currentUserState by viewModel.currentUser.collectAsState()
    val currentUser = currentUserState ?: initialUser
    val activePlayingGameId by viewModel.activePlayingGameId.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val postponedEventState by viewModel.tournamentPostponedEvent.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(postponedEventState) {
        postponedEventState?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearTournamentPostponedEvent()
        }
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    var animDirection by remember { mutableIntStateOf(1) }
    var selectedTournamentForDetail by remember { mutableStateOf<Tournament?>(null) }
    var selectedUserForProfileDetail by remember { mutableStateOf<User?>(null) }
    var showProfileWalletInitially by remember { mutableStateOf(false) }
    
// Sub-screen states for consolidated tabs
    var showCommunityFromHome by remember { mutableStateOf(false) }
    var showTournamentCalendar by remember { mutableStateOf(false) }
    var showAdminFromProfile by remember { mutableStateOf(false) }
    
// Floating dialog states
    var showTopUpDialog by remember { mutableStateOf(false) }
    var topUpAmount by remember { mutableStateOf("20.0") }
    var topUpPhone by remember { mutableStateOf(currentUser.phone) }
    var topUpMethod by remember { mutableStateOf("MTN") }
    var showProgressionInfoDialog by remember { mutableStateOf(false) }
    var selectedProgressionTab by remember { mutableIntStateOf(0) }
 
// 0: Coins, 1: Skill Points, 2: XP
    var showProgressionHubFromMain by remember { mutableStateOf(false) }
    
// Automatic referral popup states
    var showReferralPromptOnLaunch by remember { mutableStateOf(true) }
    var promptReferralName by remember { mutableStateOf("") }
    var promptReferralPhone by remember { mutableStateOf("") }
    var promptReferralSuccessMsg by remember { mutableStateOf("") }
    var promptReferralErrorMsg by remember { mutableStateOf("") }
    var promptReferralProcessing by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            if (activePlayingGameId == null) {
                TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = RaivalPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RAIVAL",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White
                        )
                    }
                },
                actions = {                    
// Balance Chip
                    Row(
                        modifier = Modifier
                            .background(RaivalSurfaceLight, shape = ShapeTokens.chip)
                            .clickable { showTopUpDialog = true }
                            .padding(horizontal = Spacing.md, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Wallet",
                            tint = RaivalSecondary,
                            modifier = Modifier.size(IconTokens.xs)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = "${currentUser.coinBalance} Coins",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Top Up",
                            tint = RaivalPrimary,
                            modifier = Modifier.size(IconTokens.xs)
                        )
                    }
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        
// Refer & Earn Button Icon
                    IconButton(
                        onClick = {
                            promptReferralName = ""
                            promptReferralPhone = ""
                            promptReferralSuccessMsg = ""
                            promptReferralErrorMsg = ""
                            showReferralPromptOnLaunch = true
                        },
                        modifier = Modifier.testTag("top_bar_referral_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Refer & Earn",
                            tint = RaivalSecondary,
                            modifier = Modifier.size(IconTokens.md)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.xs))
                                        
// Profile Button Icon
                    IconButton(
                        onClick = { selectedUserForProfileDetail = currentUser },
                        modifier = Modifier.testTag("top_bar_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "My Profile",
                            tint = RaivalPrimary,
                            modifier = Modifier.size(IconTokens.md)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface)
            )
            }
        },
        bottomBar = {
            if (activePlayingGameId == null) {
                NavigationBar(
                    containerColor = RaivalSurface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    val haptic = LocalHapticFeedback.current
                    val navItems = listOf(
                        NavigationTabItem("Home", Icons.Default.Home, 0, null),
                        NavigationTabItem("Tournaments", Icons.Default.EmojiEvents, 1, "LIVE"),
                        NavigationTabItem("Matches", Icons.Default.SportsEsports, 2, null),
                        NavigationTabItem("Community", Icons.Default.Forum, 3, "NEW"),
                        NavigationTabItem("Marketplace", Icons.Default.Storefront, 4, "HOT")
                    )
                    for (navItem in navItems) {
                        val isSel: Boolean = selectedTab == navItem.index
                        NavigationBarItem(
                            modifier = if (isSel) {
                                Modifier.shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = RaivalPrimary.copy(alpha = 0.5f),
                                    spotColor = RaivalPrimary.copy(alpha = 0.6f)
                                )
                            } else Modifier,
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (navItem.badgeText != null) {
                                            Badge(
                                                containerColor = if (navItem.badgeText == "LIVE") RaivalPrimary else RaivalAccent,
                                                contentColor = if (navItem.badgeText == "LIVE") Color.Black else Color.White
                                            ) {
                                                if (navItem.badgeText == "LIVE") {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        RaivalPulsingDot(color = Color.White, dotSize = 5.dp)
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("LIVE", fontSize = 8.sp, fontWeight = FontWeight.Black)
                                                    }
                                                } else {
                                                    Text(navItem.badgeText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = navItem.icon,
                                        contentDescription = navItem.label,
                                        modifier = Modifier
                                            .scale(if (isSel) 1.15f else 1.0f)
                                            .size(IconTokens.md)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = navItem.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold
                                )
                            },
                            selected = isSel,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (navItem.index != selectedTab) {
                                    animDirection = if (navItem.index > selectedTab) 1 else -1
                                }
                                showProfileWalletInitially = false
                                selectedTab = navItem.index
                                selectedTournamentForDetail = null
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = RaivalPrimary,
                                indicatorColor = RaivalPrimary,
                                unselectedIconColor = RaivalTextSecondary,
                                unselectedTextColor = RaivalTextSecondary
                            )
                        )
                    }
                }
            }
        }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (activePlayingGameId != null) PaddingValues(0.dp) else innerPadding)
        ) {
            if (showProgressionHubFromMain) {
                RaivalProgressionHubScreen(
                    viewModel = viewModel,
                    user = currentUser,
                    onBack = { showProgressionHubFromMain = false }
                )
            }
else if (showCommunityFromHome) {
                CommunityScreen(
                    viewModel = viewModel,
                    currentUser = currentUser,
                    onTabSwitch = { tab -> if (tab == 3) { selectedUserForProfileDetail = currentUser; showProfileWalletInitially = true }
else { selectedTab = tab } },
                    onBack = { showCommunityFromHome = false }
                )
            }
else if (showTournamentCalendar) {
                TournamentCalendarScreen(viewModel, onBack = { showTournamentCalendar = false })
            }
else if (showAdminFromProfile) {
                AdminTab(viewModel) { showAdminFromProfile = false }
            }
else if (selectedUserForProfileDetail != null) {
                UserProfileScreen(
                    user = selectedUserForProfileDetail!!,
                    viewModel = viewModel,
                    isCurrentUser = selectedUserForProfileDetail?.id ?: 0 == currentUser.id,
                    onBack = { selectedUserForProfileDetail = null },
                    onStatClick = { tab ->
                        selectedProgressionTab = tab
                        showProgressionInfoDialog = true
                    },
                    onSelectUser = { selectedUserForProfileDetail = it }
                )
            }
else if (selectedTournamentForDetail != null) {
                TournamentDetailScreen(
                    initialTournament = selectedTournamentForDetail!!,
                    viewModel = viewModel,
                    currentUser = currentUser,
                    onBack = { selectedTournamentForDetail = null }
                )}
else {
                    if (!isOnline && selectedTab != 2 && selectedTab != 3 && selectedTab != 4) {
                        val pageTitle = when (selectedTab) {
                            0 -> "Home Dashboard"
                            1 -> "Tournaments & Brackets"
                            3 -> "Marketplace Store"
                            4 -> "Profile & Wallet"
                            else -> "Competitive Hub"
                        }
                        val pageIcon = when (selectedTab) {
                            0 -> Icons.Default.Home
                            1 -> Icons.Default.EmojiEvents
                            3 -> Icons.Default.Storefront
                            4 -> Icons.Default.AccountCircle
                            else -> Icons.Default.CloudOff
                        }
                        OfflineFeatureBlocker(
                            title = pageTitle,
                            icon = pageIcon,
                            onNavigateToCasual = { selectedTab = 2 },
                            onNavigateToProfile = { selectedUserForProfileDetail = currentUser }
                        )
                    }
else {
                        val tabDirection = animDirection
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                val entering = slideInHorizontally(
                                    animationSpec = tween(MotionTokens.baseMs, easing = FastOutSlowInEasing)
                                ) { w -> tabDirection * (w / 3) } + fadeIn(animationSpec = tween(MotionTokens.quickMs))
                                val exiting = slideOutHorizontally(
                                    animationSpec = tween(MotionTokens.baseMs, easing = FastOutSlowInEasing)
                                ) { w -> -tabDirection * (w / 4) } + fadeOut(animationSpec = tween(MotionTokens.quickMs))
                                entering togetherWith exiting
                            },
                            label = "MainTabContent"
                        ) { tab ->
                            when (tab) {
                                0 -> RedesignedHomeTab(
                                    viewModel = viewModel,
                                    currentUser = currentUser,
                                    onTabSwitch = { if (it == 3) { selectedUserForProfileDetail = currentUser; showProfileWalletInitially = true }
else { selectedTab = it } },
                                    onOpenProfile = { selectedUserForProfileDetail = currentUser },
                                    onOpenProgression = { tab -> selectedProgressionTab = tab; showProgressionInfoDialog = true },
                                    onOpenCommunity = { showCommunityFromHome = true }
                                )
                                1 -> TournamentsTab(
                                    viewModel = viewModel,
                                    onStatClick = { tab -> selectedProgressionTab = tab; showProgressionInfoDialog = true },
                                    onOpenCalendar = { showTournamentCalendar = true }
                                ) { selectedTournamentForDetail = it }
                                2 -> CasualGamesTab(viewModel, currentUser)
                                3 -> CommunityScreen(
                                    viewModel = viewModel,
                                    currentUser = currentUser,
                                    onTabSwitch = { if (it == 3) { selectedUserForProfileDetail = currentUser; showProfileWalletInitially = true }
else { selectedTab = it } },
                                    onBack = { selectedTab = 0 }
                                )
                                4 -> MarketplaceTabWithWallet(
                                    viewModel = viewModel,
                                    currentUser = currentUser,
                                    onOpenProfile = { selectedUserForProfileDetail = currentUser }
                                )
                            }
                        }
                    }
            }
        }
    }
    
// Top up / Deposit Dialog
    if (showTopUpDialog) {
        Dialog(onDismissRequest = { showTopUpDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💎 COIN REFILL HUB",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Claim coin bundles to register and play in exclusive esports tournaments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RaivalTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    if (currentUser.coinsClaimedFromTopUp) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = RaivalError.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, RaivalError.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("⚠️", fontSize = 20.sp)
                                Text(
                                    text = "You have already claimed your one-time coin refill! Refills are limited to once per account.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
else {                        
// Coin options list
                        val packages = listOf(
                            Triple("Daily Free", 200, "FREE CLAIM"),
                            Triple("Gamer Pack", 500, "LEVEL UP"),
                            Triple("Pro Bundle", 1500, "ELITE READY")
                        )
                        packages.forEach { pkg ->
                            val isSelected = topUpAmount == pkg.second.toString()
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { topUpAmount = pkg.second.toString() },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) RaivalPrimary.copy(alpha = 0.15f) else RaivalSurfaceLight
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) RaivalPrimary else RaivalPrimary.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(pkg.first, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("+${pkg.second} Coins", color = RaivalSecondary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(if (isSelected) RaivalPrimary else RaivalSurface, shape = RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            pkg.third,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) Color.Black else RaivalTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                      Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.End
                     ) {
                         TextButton(onClick = { showTopUpDialog = false }) {
                             Text("Cancel", color = RaivalError)
                         }
                         Spacer(modifier = Modifier.width(8.dp))
                         Button(
                             onClick = {
                                 if (currentUser.coinsClaimedFromTopUp) {
                                     return@Button
                                 }
                                 val amt = topUpAmount.toDoubleOrNull() ?: 200.0
                                 viewModel.depositWallet(amt, "COIN REFILL", currentUser.phone) {
                                     showTopUpDialog = false
                                 }
                             },
                             enabled = !currentUser.coinsClaimedFromTopUp,
                             colors = ButtonDefaults.buttonColors(
                                 containerColor = if (currentUser.coinsClaimedFromTopUp) Color.Gray else Color(0xFFFFD600),
                                 disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                             )
                         ) {
                             Text(
                                 text = if (currentUser.coinsClaimedFromTopUp) "CLAIMED" else "CLAIM COINS",
                                 color = Color.Black,
                                 fontWeight = FontWeight.Bold
                             )
                         }
                     }
                }
            }
        }
    }
    if (showProgressionInfoDialog) {
        Dialog(onDismissRequest = { showProgressionInfoDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {                    
// Title & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚀 REWARDS & PROGRESSION",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                        IconButton(onClick = { showProgressionInfoDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = RaivalTextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
// Tab Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RaivalSurfaceLight, shape = RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val tabs = listOf("💎 COINS", "🌟 SP", "⚡ XP")
                        tabs.forEachIndexed { idx, tabTitle ->
                            val isSelected = selectedProgressionTab == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) RaivalPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedProgressionTab = idx }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabTitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
// Tab Contents
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val referralCode = if (currentUser.referralCode.isEmpty()) "SK-REF${currentUser.id + 721}" else currentUser.referralCode
                        when (selectedProgressionTab) {
                            0 -> {                                
// --- COINS TAB ---
                                Text(
                                    text = "DLS Coins are used to join competitive bracket tournaments, purchase dynamic skins, animated handles, and win-streak shields.",
                                    fontSize = 12.sp,
                                    color = RaivalTextSecondary,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
// Referral Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "👥 REFER A FRIEND (GET +200 COINS)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RaivalSecondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Invite friends to join Raival! When they sign up, you instantly claim 200 free coins.",
                                            fontSize = 11.sp,
                                            color = RaivalTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                                                                
// Invite Code Display Box
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), shape = RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("YOUR INVITE CODE", fontSize = 8.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                                    Text(referralCode, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                                }
                                                Row {                                                    
// Copy Button
                                                    IconButton(
                                                        onClick = {
                                                            val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                            val clip = android.content.ClipData.newPlainText("Raival Referral Code", referralCode)
                                                            clipboardManager.setPrimaryClip(clip)
                                                                                                                        viewModel.rewardParticipation("referral_copy")
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = RaivalPrimary, modifier = Modifier.size(18.dp))
                                                    }                                                    
// Share Button
                                                    IconButton(
                                                        onClick = {
                                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                type = "text/plain"
                                                                putExtra(
                                                                    android.content.Intent.EXTRA_TEXT,
                                                                    "Hey! Come join me on Raival, the ultimate DLS & eFootball esports arena in Ghana! Use my invite code $referralCode to claim 200 free DLS coins instantly! Download here: https://raival.com/app"
                                                                )
                                                            }
                                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Invite"))
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = RaivalSecondary, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                
// Daily Missions Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "📅 CLAIM DAILY MISSIONS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RaivalPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Earn 50 to 150 coins every single day by completing casual play challenges & tasks.",
                                            fontSize = 11.sp,
                                            color = RaivalTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                showProgressionInfoDialog = false
                                                showProgressionHubFromMain = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("GO TO DAILY MISSION LIST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                
// Training Arena Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    border = BorderStroke(1.dp, RaivalAccent.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "🤖 CASUAL PRACTICE ARENA",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RaivalAccent
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Play zero-risk training simulator matches to sharpen your skills & claim daily play bonuses.",
                                            fontSize = 11.sp,
                                            color = RaivalTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                showProgressionInfoDialog = false
                                                selectedTab = 2
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RaivalAccent),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("PRACTICE BOT TRAINING NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                
// Coin Refill Box
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    border = BorderStroke(1.dp, RaivalSuccess.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "🎁 INSTANT FREE REFILL",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RaivalSuccess
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Running low? Claim free coin bundles instantly in our sandbox refill hub.",
                                            fontSize = 11.sp,
                                            color = RaivalTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                showProgressionInfoDialog = false
                                                showTopUpDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSuccess),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("OPEN COIN REFILL HUB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                            }
                            1 -> {                                
// --- SKILL POINTS TAB ---
                                Text(
                                    text = "Raival Points (SP) determine your matchmaking tier and division bracket eligibility. The stronger your Elo score, the higher your ranking!",
                                    fontSize = 12.sp,
                                    color = RaivalTextSecondary,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
// Division Structure Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🏆 RAIVAL GLOBAL DIVISION TIERS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RaivalSecondary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                                                                val divisions = listOf(
                                            Triple("🥉 BRONZE DIVISION", "100 - 399 SP", "Default starting tier for amateur players."),
                                            Triple("🥈 SILVER DIVISION", "400 - 699 SP", "Intermediate competitors with robust match experience."),
                                            Triple("🥇 GOLD DIVISION", "700 - 999 SP", "Expert gamers qualifying for high stakes leagues."),
                                            Triple("👑 ARENA CHAMPION", "1000+ SP", "Elite tier with glowing name labels and special badges.")
                                        )
                                        divisions.forEachIndexed { dIdx, dInfo ->
                                            val isActive = if (dIdx == 0) currentUser.raivalPoints < 400
                                                else if (dIdx == 1) currentUser.raivalPoints in 400..699
                                                else if (dIdx == 2) currentUser.raivalPoints in 700..999
                                                else currentUser.raivalPoints >= 1000
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        color = if (isActive) RaivalSecondary.copy(alpha = 0.1f) else Color.Transparent,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(dInfo.first, fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (isActive) RaivalSecondary else Color.White)
                                                        if (isActive) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(RaivalSecondary, shape = RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("ACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                            }
                                                        }
                                                    }
                                                    Text(dInfo.third, fontSize = 9.sp, color = RaivalTextSecondary)
                                                }
                                                Text(dInfo.second, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                            if (dIdx < divisions.size - 1) {
                                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
// Earn SP Info
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🚀 HOW TO MAXIMIZE YOUR SP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RaivalPrimary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("• Win official tournaments to claim up to +150 SP per victory.", fontSize = 11.sp, color = RaivalTextSecondary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("• Enter consecutive league tournaments to build streak bonuses.", fontSize = 11.sp, color = RaivalTextSecondary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("• Equip STREAK SHIELDS from the Progression Hub to prevent SP loss if you get defeated.", fontSize = 11.sp, color = RaivalTextSecondary)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                showProgressionInfoDialog = false
                                                selectedTab = 1
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("BROWSE BRACKETS & COMPETE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                            }
                            2 -> {                                
// --- XP TAB ---
                                Text(
                                    text = "Experience Points (XP) level up your Raival Profile, unlocking Ghanaian custom theme colors, professional walkout animation effects, and badges.",
                                    fontSize = 12.sp,
                                    color = RaivalTextSecondary,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
// Current XP Status Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    border = BorderStroke(1.dp, RaivalAccent.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("GAMER PROFILE", fontSize = 10.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                                Text("LEVEL ${currentUser.level}", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Black)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(RaivalAccent.copy(alpha = 0.15f), shape = CircleShape)
                                                    .padding(12.dp)
                                            ) {
                                                Icon(Icons.Default.Bolt, contentDescription = null, tint = RaivalAccent, modifier = Modifier.size(28.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val xpNeeded = currentUser.level * 1000
                                        val xpProgress = (currentUser.xp.toFloat() / xpNeeded.toFloat()).coerceIn(0f, 1f)
                                                                                Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("EXP: ${currentUser.xp} XP", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("NEXT LEVEL: $xpNeeded XP", fontSize = 11.sp, color = RaivalTextSecondary)
                                        }
                                                                                Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { xpProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = RaivalAccent,
                                            trackColor = Color.White.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
// Ways to level up list
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("📈 FAST-TRACK XP GAINS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RaivalSecondary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                                                                val waysToEarnXp = listOf(
                                            Pair("📅 Daily Missions", "Earn up to +300 XP per mission completion in the Progression Hub."),
                                            Pair("🤖 Practice Matches", "Earn guaranteed XP safely in the Bot training modes (zero risk)."),
                                            Pair("🏆 Official Brackets", "Completing official bracket matches awards experience points based on match performance!")
                                        )
                                        waysToEarnXp.forEach { way ->
                                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Text(way.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(way.second, fontSize = 10.sp, color = RaivalTextSecondary)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                                                                Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    showProgressionInfoDialog = false
                                                    selectedTab = 2
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = RaivalAccent),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("BOT PRACTICE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            }
                                                                                        Button(
                                                onClick = {
                                                    showProgressionInfoDialog = false
                                                    showProgressionHubFromMain = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("CLAIM QUESTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
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
    }    
// Modern, automatic and manual prompt for referral download system
    if (showReferralPromptOnLaunch) {
        Dialog(onDismissRequest = { showReferralPromptOnLaunch = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, RaivalSecondary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {                    
// Header section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = RaivalPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REFER & EARN COINS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White
                            )
                        }
                        IconButton(onClick = { showReferralPromptOnLaunch = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = RaivalTextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
// Description text
                    Text(
                        text = "Build Ghana's ultimate esports arena! Invite your friends outside the app (WhatsApp, Telegram, SMS, or Call) to download Raival.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                        text = "Get +20 Coins instantly on sending an invitation and a massive +200 Coins when they complete registration using your code!",
                        style = MaterialTheme.typography.bodySmall,
                        color = RaivalSecondary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    
// Unique Referral Code display
                    val refCode = if (currentUser.referralCode.isEmpty()) "SK-REF${currentUser.id + 721}" else currentUser.referralCode
                    val context = LocalContext.current
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "YOUR EXCLUSIVE REFERRAL CODE",
                                fontSize = 10.sp,
                                color = RaivalTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = refCode,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                ),
                                color = RaivalPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                                                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {                                
// Copy Code button
                                Button(
                                    onClick = {
                                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Raival Referral Code", refCode)
                                        clipboardManager.setPrimaryClip(clip)
                                        viewModel.rewardParticipation("referral_copy")
                                        promptReferralSuccessMsg = "Code copied! Send it to friends outside the app!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSurface),
                                    border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy Code", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
// Share Direct Link button
                                Button(
                                    onClick = {
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                android.content.Intent.EXTRA_TEXT,
                                                "Hey! Join me on Raival, Ghana's premier mobile esports platform! Use my code $refCode to earn rewards and compete. Download here: https://raival.com/app"
                                            )
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Download Link"))
                                        viewModel.rewardParticipation("referral")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share Link", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = RaivalPrimary.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
// Send Direct Download Invite Form
                    Text(
                        text = "📱 DIRECT SMS/WHATSAPP INVITE LINK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RaivalSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter a friend's details to instantly notify them about downloading the app. You'll claim a 20-coin direct reward!",
                        fontSize = 11.sp,
                        color = RaivalTextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = promptReferralName,
                        onValueChange = {
                            promptReferralName = it
                            promptReferralSuccessMsg = ""
                            promptReferralErrorMsg = ""
                        },
                        label = { Text("Friend's Full Name") },
                        placeholder = { Text("e.g. Kwame Mensah") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RaivalPrimary,
                            focusedLabelColor = RaivalPrimary,
                            unfocusedBorderColor = RaivalTextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("referral_prompt_name_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = promptReferralPhone,
                        onValueChange = {
                            promptReferralPhone = it
                            promptReferralSuccessMsg = ""
                            promptReferralErrorMsg = ""
                        },
                        label = { Text("Mobile Phone Number") },
                        placeholder = { Text("e.g. 0541234567") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RaivalPrimary,
                            focusedLabelColor = RaivalPrimary,
                            unfocusedBorderColor = RaivalTextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("referral_prompt_phone_input"),
                        singleLine = true
                    )
                    if (promptReferralSuccessMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = promptReferralSuccessMsg,
                            color = RaivalSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (promptReferralErrorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = promptReferralErrorMsg,
                            color = RaivalError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showReferralPromptOnLaunch = false },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Maybe Later", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (promptReferralName.isBlank()) {
                                    promptReferralErrorMsg = "Please enter your friend's name."
                                    return@Button
                                }
                                if (promptReferralPhone.isBlank()) {
                                    promptReferralErrorMsg = "Please enter your friend's mobile number."
                                    return@Button
                                }
                                promptReferralProcessing = true
                                promptReferralErrorMsg = ""
                                promptReferralSuccessMsg = ""
                                                                viewModel.rewardParticipation("referral")
                                promptReferralSuccessMsg = "✉️ Download invitation sent to $promptReferralName! +20 Coins credited successfully!"
                                promptReferralProcessing = false
                                promptReferralName = ""
                                promptReferralPhone = ""
                            },
                            enabled = !promptReferralProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary),
                            modifier = Modifier.weight(1.3f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (promptReferralProcessing) "Sending..." else "Send Invitation",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }}
// -------------------------------------------------------------
// TAB 1: TOURNAMENTS (DISCOVERY & FILTERS)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun TournamentsTab(
    viewModel: RaivalViewModel,
    onStatClick: (Int) -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onSelectTournament: (Tournament) -> Unit) {
    val tournaments by viewModel.filteredTournaments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userRegistrations by viewModel.userRegistrations.collectAsState()
    val allTournamentsList by viewModel.tournaments.collectAsState()
    val userMatches by remember(currentUser) {
        viewModel.getMatchesForUser(currentUser?.username ?: "")
    }.collectAsState(initial = emptyList())
    var showJoinedToursStatusDialog by remember { mutableStateOf(false) }
    
// Registration states for direct quick join from dashboard
    var quickJoinTournament by remember { mutableStateOf<Tournament?>(null) }
    var inGameName by remember { mutableStateOf("") }
    var teamRatingInput by remember { mutableStateOf("") }
    var accountTeamInput by remember { mutableStateOf("") }
    var mmPhone by remember { mutableStateOf("") }
    var mmMethod by remember { mutableStateOf("MTN") }
    var processingMsg by remember { mutableStateOf("") }
    var regStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    
// On quick join select, initialize fields
    LaunchedEffect(quickJoinTournament) {
        quickJoinTournament?.let {
            inGameName = currentUser?.username ?: ""
            teamRatingInput = ""
            accountTeamInput = ""
            mmPhone = currentUser?.phone ?: ""
            processingMsg = ""
            regStatus = null
        }
    }    
// Hoisted states for the guide carousel and importing
    var selectedFormatGuide by remember { mutableStateOf("EPL") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importCodeInput by remember { mutableStateOf("") }
    var importStatusMsg by remember { mutableStateOf("") }
    val filterChips = listOf(
        Pair("all", "All Tournaments"),
        Pair("featured", "🔥 Featured"),
        Pair("open", "🔓 Open Entry"),
        Pair("size_2", "⚡ 2 Members"),
        Pair("size_4", "⚡ 4 Members"),
        Pair("size_8", "⚡ 8 Members"),
        Pair("size_16", "⚡ 16 Members"),
        Pair("size_32", "⚡ 32 Members"),
        Pair("DLS", "⚽ DLS Mobile"),
        Pair("eFootball", "🎮 eFootball")
    )
LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RaivalBackground),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (BuildConfig.IS_DEMO_MODE) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFA500).copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Demo", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "⚠️ DEMO MODE: Tournament registration, coin transactions, and payments are disabled. All data is simulated.",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 2,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        // 1. Welcome and Gamer Stats Card
        currentUser?.let { user ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {                        
// Header greeting with badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "WELCOME BACK,",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                    color = RaivalTextSecondary
                                )
                                Text(
                                    text = user.fullName.uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }                                                        
// Gamer Loyalty Tier Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(RaivalSecondary, RaivalAccent)
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "⚡ ARENA CHAMP",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
// Stats row in card grid
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {                                
// Coin Balance Stat Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onStatClick(0) },
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("COINS", fontSize = 9.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("${user.coinBalance} COINS", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                
// Raival Points Stat Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onStatClick(1) },
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = RaivalSecondary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("SKILL POINTS", fontSize = 9.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("${user.raivalPoints} SP", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {                                
// XP Level Stat Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onStatClick(2) },
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = RaivalAccent, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("XP LEVEL", fontSize = 9.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("LEVEL ${user.level}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                
// Joined Tournaments Stat Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showJoinedToursStatusDialog = true },
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = RaivalAccent, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("JOINED", fontSize = 9.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("${userRegistrations.size} TOURS", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
// 2. Spotlight Arena Hero Banner
        if (searchQuery.isEmpty() && activeFilter == "all") {            
// Find a high-value or active tournament as our main featured candidate
            val featuredTournament = tournaments.maxByOrNull { it.prize } ?: tournaments.firstOrNull()
                        featuredTournament?.let { ft ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.bg_gaming_1),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = 0.85f
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.50f),
                                                Color.Black.copy(alpha = 0.30f)
                                            )
                                        )
                                    )
                            )
                                                        Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .background(RaivalSecondary.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "🔥 FEATURED SHOWDOWN",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RaivalSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = ft.title.uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val prizeCoins = if (ft.coinPrize > 0) ft.coinPrize else ft.prize.toInt()
                                    Text(
                                        text = "Join Accra's elite squad. Prize Pool: $prizeCoins Coins",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RaivalTextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.People, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${ft.players}/${ft.maxPlayers} Slots Filled",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                                                Spacer(modifier = Modifier.width(12.dp))
                                                                
// Visual Big Prize Circle + Button
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val bPrizeCoins = if (ft.coinPrize > 0) ft.coinPrize else ft.prize.toInt()
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(RaivalPrimary.copy(alpha = 0.15f), shape = CircleShape)
                                            .border(2.dp, RaivalPrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("PRIZE", fontSize = 8.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                            Text("$bPrizeCoins", fontSize = 12.sp, color = RaivalPrimary, fontWeight = FontWeight.Black)
                                        }
                                    }
                                                                        Spacer(modifier = Modifier.height(8.dp))
                                                                        val isJoined = userRegistrations.any { it.tournamentId == ft.id }
                                    Button(
                                        onClick = {
                                            if (isJoined) {
                                                onSelectTournament(ft)
                                            }
else {
                                                quickJoinTournament = ft
                                            }
                                        },
                                        modifier = Modifier.height(28.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isJoined) RaivalSuccess else RaivalPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isJoined) "ENTER" else "JOIN NOW",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
// 3. Search Bar with Calendar Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search tournaments...", color = RaivalTextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RaivalPrimary) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = RaivalSurface,
                        unfocusedContainerColor = RaivalSurface,
                        focusedIndicatorColor = RaivalPrimary,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                                
// Calendar Button
                IconButton(
                    onClick = onOpenCalendar,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(RaivalPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.3f)), shape = RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Open Tournament Calendar",
                        tint = RaivalPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
// 4. Filters Horizontal Row
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterChips, key = { it.first }) { pair ->
                    val id = pair.first
                    val label = pair.second
                    val isSelected = activeFilter == id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.activeFilter.value = id },
                        label = { Text(label, fontSize = 12.sp, color = if (isSelected) Color.Black else Color.White) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RaivalPrimary,
                            containerColor = RaivalSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            selected = isSelected,
                            enabled = true,
                            borderColor = if (isSelected) RaivalPrimary else RaivalPrimary.copy(alpha = 0.3f),
                            selectedBorderColor = RaivalPrimary,
                            borderWidth = 1.dp
                        )
                    )
                }
            }
        }
        
// 5. Themed Tournament Format Guide Carousel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏆 EXCLUSIVE ARENA LEAGUE STYLES",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = RaivalPrimary
                        )
                                                Text(
                            text = "NEW FORMATS",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Black),
                            color = RaivalSecondary,
                            modifier = Modifier
                                .background(RaivalSecondary.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
// Formats tabs selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val formats = listOf("EPL" to "EPL LEAGUE", "UCL" to "UCL GALA", "WC" to "WORLD CUP")
                        formats.forEach { (key, label) ->
                            val isSel = selectedFormatGuide == key
                            val activeColor = when (key) {
                                "EPL" -> Color(0xFFFFDF1B)
                                "UCL" -> Color(0xFFFFD700)
                                else -> Color(0xFFFFD700)
                            }
                            val activeBg = when (key) {
                                "EPL" -> Color(0xFF3F0A44)
                                "UCL" -> Color(0xFF5C0000)
                                else -> Color(0xFF0A4F35)
                            }
                                                        Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSel) activeBg else RaivalSurfaceLight,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSel) activeColor.copy(alpha = 0.5f) else Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedFormatGuide = key }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) activeColor else RaivalTextSecondary
                                )
                            }
                        }
                    }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
// Active Card details
                    when (selectedFormatGuide) {
                        "EPL" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MilitaryTech,
                                    contentDescription = null,
                                    tint = Color(0xFFFFDF1B),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "English Premier League (EPL) Style",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Round-Robin league format. Earn 3 pts for a WIN, 1 pt for a DRAW. Live points table tracks played, Goal Difference (GD), and points. The top of the table after all matches wins the trophy!",
                                        fontSize = 10.sp,
                                        color = RaivalTextSecondary,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                        "UCL" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Champions League (UCL) Style",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "High-intensity double-legged knockout play-offs! Standard aggregate score rule. In case of aggregate draw, the iconic AWAY GOALS count double! Build aggregate strategy for home & away games.",
                                        fontSize = 10.sp,
                                        color = RaivalTextSecondary,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                        "WC" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "FIFA World Cup Style",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Represent national teams. Group stage (Groups A-H) with top 2 qualifying for high-stakes single-elimination Round of 16. Tied matches go to 3 min Extra Time and 5-shot penalty shootouts!",
                                        fontSize = 10.sp,
                                        color = RaivalTextSecondary,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }        
// 6. LOCAL OFFLINE SYNC - IMPORT TOURNAMENT
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 Active Arenas",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                                TextButton(
                    onClick = {
                        showImportDialog = true
                        importCodeInput = ""
                        importStatusMsg = ""
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RaivalPrimary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }        
// 7. Tournament List Feed
        if (tournaments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = RaivalTextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No active tournaments found",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Try clearing filters or check back later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RaivalTextSecondary
                        )
                    }
                }
            }
        }
else {
            items(tournaments, key = { it.id }) { tournament ->
                val isRegistered = userRegistrations.any { it.tournamentId == tournament.id }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    TournamentCard(
                        tournament = tournament,
                        isRegistered = isRegistered,
                        onSelect = onSelectTournament,
                        onQuickJoin = { quickJoinTournament = it }
                    )
                }
            }
        }
    }    
// Frictionless Quick Join Dialog
    quickJoinTournament?.let { tournament ->
        Dialog(onDismissRequest = { quickJoinTournament = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {                    
// Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ SECURE ENTRY",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = RaivalPrimary
                        )
                        IconButton(onClick = { quickJoinTournament = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = RaivalTextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tournament.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
// Ledger of costs & validation
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val entryFeeCoins = if (tournament.coinEntryFee > 0) tournament.coinEntryFee else tournament.entryFee.toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Entry Fee:", color = RaivalTextSecondary, fontSize = 13.sp)
                                Text(
                                    text = if (entryFeeCoins > 0) "$entryFeeCoins Coins" else "FREE ENTRY",
                                    color = if (entryFeeCoins == 0) RaivalSuccess else RaivalSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        val hasBalance = (currentUser?.coinBalance ?: 0) >= entryFeeCoins
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Your Balance:", color = RaivalTextSecondary, fontSize = 13.sp)
                                Text(
                                    text = "${currentUser?.coinBalance ?: 0} Coins",
                                    color = if (hasBalance) Color.White else RaivalError,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            if (!hasBalance) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(RaivalError.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RaivalError, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Insufficient coin balance. Play matches or earn coins to top up.",
                                        color = RaivalError,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    if (processingMsg.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(color = RaivalPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = processingMsg, color = RaivalPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    if (regStatus != null) {
                        val isSuccess = regStatus?.first ?: false
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSuccess) RaivalSuccess.copy(alpha = 0.15f) else RaivalError.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isSuccess) RaivalSuccess else RaivalError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = regStatus?.second ?: "",
                                color = if (isSuccess) RaivalSuccess else RaivalError,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                quickJoinTournament = null
                                regStatus = null
                                processingMsg = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSuccess) RaivalSuccess else RaivalPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
else {                        
// Form
                        OutlinedTextField(
                            value = inGameName,
                            onValueChange = { inGameName = it },
                            label = { Text("Your Gamer Name (IGN)") },
                            placeholder = { Text("e.g. Messi10") },
                            supportingText = { Text("This is your username or team name in either eFootball, DLS, or FC Mobile", color = RaivalTextSecondary, fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.Gamepad, contentDescription = null, tint = RaivalPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = RaivalPrimary,
                                focusedIndicatorColor = RaivalPrimary
                            ),
                            singleLine = true
                        )
                        
// Determine game field label and helper text
                        val ratingLabel = when {
                            tournament.game.contains("FC Mobile", ignoreCase = true) || tournament.game.contains("FC", ignoreCase = true) || tournament.game.contains("FIFA", ignoreCase = true) -> "FC Mobile Team OVR"
                            tournament.game.contains("eFootball", ignoreCase = true) || tournament.game.contains("PES", ignoreCase = true) -> "eFootball Collective Strength"
                            else -> "DLS Team Strength"
                        }
                        val ratingPlaceholder = when {
                            tournament.game.contains("FC Mobile", ignoreCase = true) || tournament.game.contains("FC", ignoreCase = true) || tournament.game.contains("FIFA", ignoreCase = true) -> "e.g. 102"
                            tournament.game.contains("eFootball", ignoreCase = true) || tournament.game.contains("PES", ignoreCase = true) -> "e.g. 3100"
                            else -> "e.g. 85"
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = teamRatingInput,
                            onValueChange = { teamRatingInput = it.filter { char -> char.isDigit() } },
                            label = { Text(ratingLabel) },
                            placeholder = { Text(ratingPlaceholder) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = RaivalSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = RaivalSecondary,
                                focusedIndicatorColor = RaivalSecondary
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = accountTeamInput,
                            onValueChange = { accountTeamInput = it },
                            label = { Text("Built Account Team (Model Club)") },
                            placeholder = { Text("e.g. Barcelona, Brazil, Liverpool, Spain...") },
                            leadingIcon = { Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = RaivalSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = RaivalSecondary,
                                focusedIndicatorColor = RaivalSecondary
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Or tap a popular preset team:",
                            fontSize = 11.sp,
                            color = RaivalTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val popularTeams = listOf("Barcelona", "Brazil", "Spain", "Liverpool")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            popularTeams.forEach { team ->
                                val isSelected = accountTeamInput.equals(team, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) RaivalSecondary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) RaivalSecondary else Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { accountTeamInput = team }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = when (team) {
                                            "Barcelona" -> "Barcelona 🇪🇸"
                                            "Brazil" -> "Brazil 🇧🇷"
                                            "Spain" -> "Spain 🇪🇸"
                                            "Liverpool" -> "Liverpool 🏴󠁧󠁢󠁥󠁮󠁧󠁿"
                                            else -> team
                                        },
                                        color = if (isSelected) RaivalSecondary else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        val entryFeeCoins = if (tournament.coinEntryFee > 0) tournament.coinEntryFee else tournament.entryFee.toInt()
                        val hasBalance = (currentUser?.coinBalance ?: 0) >= entryFeeCoins
                        Button(
                            onClick = {
                                if (inGameName.trim().isEmpty()) {
                                    regStatus = Pair(false, "Please enter your Gamer Name.")
                                    return@Button
                                }
                                val ratingInt = teamRatingInput.toIntOrNull()
                                if (ratingInt == null || ratingInt <= 0) {
                                    regStatus = Pair(false, "Please enter a valid numeric $ratingLabel.")
                                    return@Button
                                }
                                if (accountTeamInput.trim().isEmpty()) {
                                    regStatus = Pair(false, "Please select or enter the Built Account Team.")
                                    return@Button
                                }
                                
// Specific range warnings/validations to make it realistic
                                val isValidRange = when {
                                    tournament.game.contains("FC Mobile", ignoreCase = true) || tournament.game.contains("FC", ignoreCase = true) || tournament.game.contains("FIFA", ignoreCase = true) -> ratingInt in 50..160
                                    tournament.game.contains("eFootball", ignoreCase = true) || tournament.game.contains("PES", ignoreCase = true) -> ratingInt in 1000..3500
                                    else -> ratingInt in 30..105 
// DLS
                                }
                                if (!isValidRange) {
                                    regStatus = Pair(false, "Please enter a realistic rating/strength for ${tournament.game}.")
                                    return@Button
                                }
                                processingMsg = "Securing tournament seat..."
                                viewModel.registerForTournament(
                                    tournamentId = tournament.id,
                                    inGameName = inGameName,
                                    paymentMethod = "COINS",
                                    phone = currentUser?.phone ?: "",
                                    teamRating = ratingInt,
                                    modelTeam = accountTeamInput.trim()
                                ) { success, msg ->
                                    processingMsg = ""
                                    regStatus = Pair(success, msg)
                                }
                            },
                            enabled = hasBalance && processingMsg.isEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RaivalPrimary,
                                disabledContainerColor = RaivalSurfaceLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "SECURE SEAT WITH COINS",
                                color = if (hasBalance) Color.Black else RaivalTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
    if (showJoinedToursStatusDialog) {
        Dialog(onDismissRequest = { showJoinedToursStatusDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎮 JOINED TOURNAMENTS",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Track your active entries and match status",
                        style = MaterialTheme.typography.bodySmall,
                        color = RaivalTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    val joinedRegs = userRegistrations
                    if (joinedRegs.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = RaivalTextSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Tournaments Joined Yet",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Browse active tournaments below and hit 'JOIN NOW' to secure your bracket spot!",
                                    color = RaivalTextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(joinedRegs, key = { it.id }) { reg ->
                                val tour = allTournamentsList.find { it.id == reg.tournamentId }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = tour?.title ?: "Unknown Tournament",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                modifier = Modifier.weight(1f)
                                            )                                                                                        
// Tournament Status Badge
                                            val isTourOpen = tour?.status == "Open"
                                            Surface(
                                                color = if (isTourOpen) RaivalSuccess.copy(alpha = 0.15f) else RaivalError.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isTourOpen) "OPEN" else "COMPLETED",
                                                    color = if (isTourOpen) RaivalSuccess else RaivalError,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
// Game & Mode info
                                        Text(
                                            text = "${tour?.game ?: "DLS"} • ${tour?.mode ?: "1 vs 1"}",
                                            color = RaivalSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                         )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
// Registration / Player details
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("YOUR GAMER TAG", fontSize = 8.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                                Text(reg.inGameName, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("SLOTS JOINED", fontSize = 8.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                                Text("${tour?.players ?: 0}/${tour?.maxPlayers ?: 16} Players", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("REGISTRATION DATE", fontSize = 8.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                                val dateStr = try {
                                                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(reg.registeredAt))
                                                }
catch (e: Exception) {
                                                    "Recently"
                                                }
                                                Text(dateStr, fontSize = 11.sp, color = Color.White)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("PRIZE POOL", fontSize = 8.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                                val prizeStr = if ((tour?.coinPrize ?: 0) > 0) "${tour?.coinPrize} Coins" else "GHS ${tour?.prize ?: 0.0}"
                                                Text(prizeStr, fontSize = 11.sp, color = RaivalSecondary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        
// Live Bracket Match Status section
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                             text = "🎯 YOUR BRACKET MATCHES",
                                             fontSize = 9.sp,
                                             color = RaivalPrimary,
                                             fontWeight = FontWeight.Black
                                        )
                                        val myTourMatches = userMatches.filter { it.tournamentId == reg.tournamentId }
                                        if (myTourMatches.isEmpty()) {
                                             Spacer(modifier = Modifier.height(6.dp))
                                             Text(
                                                 text = "⏳ Bracket drawing in progress. Waiting for participants to fill remaining slots.",
                                                 fontSize = 11.sp,
                                                 color = RaivalTextSecondary,
                                                 fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                             )
                                        }
else {
                                             myTourMatches.forEach { match ->
                                                 val opponentName = if (match.player1Name.equals(reg.playerName, ignoreCase = true)) match.player2Name else match.player1Name
                                                 val isCompleted = match.status == "Completed"
                                                                                                  Spacer(modifier = Modifier.height(8.dp))
                                                 Card(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                                                     shape = RoundedCornerShape(8.dp),
                                                     border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                                 ) {
                                                     Column(modifier = Modifier.padding(10.dp)) {
                                                         Row(
                                                             modifier = Modifier.fillMaxWidth(),
                                                             horizontalArrangement = Arrangement.SpaceBetween,
                                                             verticalAlignment = Alignment.CenterVertically
                                                         ) {
                                                             Text(
                                                                 text = match.round.uppercase(),
                                                                 fontSize = 9.sp,
                                                                 color = RaivalTextSecondary,
                                                                 fontWeight = FontWeight.Bold
                                                             )
                                                                                                                          Surface(
                                                                 color = if (isCompleted) RaivalSuccess.copy(alpha = 0.15f) else RaivalSecondary.copy(alpha = 0.15f),
                                                                 shape = RoundedCornerShape(4.dp)
                                                             ) {
                                                                 Text(
                                                                     text = if (isCompleted) "COMPLETED" else "PENDING",
                                                                     color = if (isCompleted) RaivalSuccess else RaivalSecondary,
                                                                     fontSize = 8.sp,
                                                                     fontWeight = FontWeight.Bold,
                                                                     modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                 )
                                                             }
                                                         }
                                                                                                                  Spacer(modifier = Modifier.height(6.dp))
                                                                                                                  Row(
                                                             modifier = Modifier.fillMaxWidth(),
                                                             horizontalArrangement = Arrangement.SpaceBetween,
                                                             verticalAlignment = Alignment.CenterVertically
                                                         ) {
                                                             Text(
                                                                 text = "vs $opponentName",
                                                                 fontSize = 12.sp,
                                                                 color = Color.White,
                                                                 fontWeight = FontWeight.Bold
                                                             )
                                                                                                                          if (isCompleted) {
                                                                 val scoreStr = "${match.player1Score ?: 0} - ${match.player2Score ?: 0}"
                                                                 val didIWin = match.winnerName.equals(currentUser?.username, ignoreCase = true)
                                                                 Row(verticalAlignment = Alignment.CenterVertically) {
                                                                     Text(
                                                                         text = scoreStr,
                                                                         fontSize = 12.sp,
                                                                         color = Color.White,
                                                                         fontWeight = FontWeight.Black
                                                                     )
                                                                     Spacer(modifier = Modifier.width(6.dp))
                                                                     Text(
                                                                         text = if (didIWin) "WON" else "LOST",
                                                                         fontSize = 10.sp,
                                                                         color = if (didIWin) RaivalSuccess else RaivalError,
                                                                         fontWeight = FontWeight.Black
                                                                     )
                                                                 }
                                                             }
else {
                                                                 Text(
                                                                     text = "VIEW BRACKET",
                                                                     fontSize = 10.sp,
                                                                     color = RaivalPrimary,
                                                                     fontWeight = FontWeight.Black,
                                                                     modifier = Modifier.clickable {
                                                                         showJoinedToursStatusDialog = false
                                                                         tour?.let { onSelectTournament(it) }
                                                                     }
                                                                 )
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
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showJoinedToursStatusDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CLOSE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }}

@Composable
fun TournamentCard(
    tournament: Tournament,
    isRegistered: Boolean,
    onSelect: (Tournament) -> Unit,
    onQuickJoin: (Tournament) -> Unit) {
    val progress = remember(tournament) { tournament.players.toFloat() / tournament.maxPlayers.toFloat() }
        
// Dynamic color accent based on game
    val gameAccent = remember(tournament) {
        when {
            tournament.game.contains("eFootball", ignoreCase = true) -> RaivalAccent
            tournament.game.contains("DLS", ignoreCase = true) -> RaivalBrightLime
            else -> RaivalElectricPurple
        }
    }
    val gameIcon = remember(tournament) {
        if (tournament.game.contains("DLS", ignoreCase = true) || tournament.game.contains("eFootball", ignoreCase = true)) {
            Icons.Default.SportsEsports
        }
else {
            Icons.Default.Gamepad
        }
    }
    val cardBgRes = remember(tournament) {
        TournamentBannerResolver.resolve(tournament.banner)
            ?: when {
                tournament.game.contains("eFootball", ignoreCase = true) -> {
                    val eBgs = listOf(R.drawable.bg_efootball_1, R.drawable.bg_efootball_2, R.drawable.bg_efootball_3)
                    eBgs[Math.abs(tournament.id) % eBgs.size]
                }
                tournament.game.contains("DLS", ignoreCase = true) -> {
                    val dlsBgs = listOf(
                        R.drawable.dls_bg_new1,
                        R.drawable.dls_bg_new2,
                        R.drawable.dls_bg_new3,
                        R.drawable.dls_bg_new4
                    )
                    dlsBgs[Math.abs(tournament.id) % dlsBgs.size]
                }
                tournament.game.contains("FC", ignoreCase = true) -> {
                    val fcBgs = listOf(
                    R.drawable.fc_bg_new1,
                    R.drawable.fc_bg_new2,
                    R.drawable.fc_bg_new3,
                    R.drawable.fc_bg_new4
                )
                fcBgs[Math.abs(tournament.id) % fcBgs.size]
            }
            else -> {
                val genBgs = listOf(R.drawable.bg_gaming_1, R.drawable.bg_gaming_2, R.drawable.bg_gaming_3, R.drawable.bg_gaming_4)
                genBgs[Math.abs(tournament.id) % genBgs.size]
            }
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tournament_card_${tournament.id}"),
        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, gameAccent.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = cardBgRes),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.85f
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.60f)
                            )
                        )
                    )
            )
            Column(modifier = Modifier.padding(16.dp)) {            
// Top Badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {                
// Game Category Tag with Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .background(gameAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(gameIcon, contentDescription = null, tint = gameAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tournament.game.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = gameAccent
                        )
                    }                    
// Member Capacity Badge
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        modifier = Modifier
                            .background(RaivalPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${tournament.maxPlayers} MEMBERS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = RaivalPrimary
                        )
                    }                    
// Theme Format Badge
                    val formatBadge = remember(tournament) {
                        when (tournament.style) {
                            "EPL Style" -> "EPL LEAGUE"
                            "Champions League Style" -> "UCL CHAMPIONS"
                            "World Cup Style" -> "WORLD CUP"
                            else -> {
                                if (tournament.format == "League") "LEAGUE" else "KNOCKOUT"
                            }
                        }
                    }
                    if (formatBadge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val formatBadgeBg = remember(tournament, formatBadge) {
                            when (formatBadge) {
                                "EPL LEAGUE" -> Color(0xFF3F0A44)
                                "UCL CHAMPIONS" -> Color(0xFF5C0000)
                                "WORLD CUP" -> Color(0xFF0A4F35)
                                "LEAGUE" -> RaivalPrimary.copy(alpha = 0.2f)
                                else -> RaivalSecondary.copy(alpha = 0.2f)
                            }
                        }
                        val formatBadgeText = remember(tournament, formatBadge) {
                            when (formatBadge) {
                                "EPL LEAGUE" -> Color(0xFFFFDF1B)
                                "UCL CHAMPIONS" -> Color(0xFFFFD700)
                                "WORLD CUP" -> Color(0xFFFFD700)
                                "LEAGUE" -> RaivalPrimary
                                else -> RaivalSecondary
                            }
                        }
                        val formatBadgeIcon = remember(tournament, formatBadge) {
                            when (formatBadge) {
                                "EPL LEAGUE" -> Icons.Default.MilitaryTech
                                "UCL CHAMPIONS" -> Icons.Default.Stars
                                "WORLD CUP" -> Icons.Default.EmojiEvents
                                "LEAGUE" -> Icons.Default.Leaderboard
                                else -> Icons.Default.Star
                            }
                        }
                        Row(
                            modifier = Modifier
                                .background(formatBadgeBg, shape = RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, formatBadgeText.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = formatBadgeIcon,
                                contentDescription = null,
                                tint = formatBadgeText,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatBadge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = formatBadgeText,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }                
// Entry fee badge
                val entryFeeCoins = remember(tournament) { if (tournament.coinEntryFee > 0) tournament.coinEntryFee else tournament.entryFee.toInt() }
                Box(
                    modifier = Modifier
                        .background(
                            if (entryFeeCoins > 0) RaivalSecondary.copy(alpha = 0.15f) else RaivalSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (entryFeeCoins > 0) "$entryFeeCoins COINS" else "FREE TO JOIN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (entryFeeCoins > 0) RaivalSecondary else RaivalSuccess
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
// Tournament Title
            Text(
                text = tournament.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            
// Sub info (Date, Time, Mode)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = tournament.date, fontSize = 11.sp, color = RaivalTextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${tournament.time} GMT", fontSize = 11.sp, color = RaivalTextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = tournament.mode, fontSize = 11.sp, color = RaivalTextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
// Progress indicator with numeric status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Joined Arena: ${tournament.players}/${tournament.maxPlayers} gamers",
                    fontSize = 11.sp,
                    color = RaivalTextSecondary
                )
                val left = tournament.maxPlayers - tournament.players
                Text(
                    text = if (left == 0) "FULL" else "$left slots left",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (left < 5 && left > 0) RaivalSecondary else RaivalPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = gameAccent,
                trackColor = RaivalSurfaceLight
            )
            Spacer(modifier = Modifier.height(16.dp))
            
// Card footer: Prize + Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "TOTAL PRIZE POOL", fontSize = 9.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                    val prizeCoins = if (tournament.coinPrize > 0) tournament.coinPrize else tournament.prize.toInt()
                    Text(
                        text = if (prizeCoins > 0) "$prizeCoins Coins" else "Free Play",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = RaivalSecondary
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {                    
// Outlined Info button
                    OutlinedButton(
                        onClick = { onSelect(tournament) },
                        border = BorderStroke(1.dp, RaivalTextSecondary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("RULES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }                    
// Primary Action button (Quick Join / Registered status)
                    if (isRegistered) {
                        Button(
                            onClick = { onSelect(tournament) },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSuccess),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("JOINED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
else if (tournament.status == "Open") {
                        Button(
                            onClick = { onQuickJoin(tournament) },
                            colors = ButtonDefaults.buttonColors(containerColor = gameAccent),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("QUICK JOIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
else {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("CLOSED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RaivalTextSecondary)
                        }
                    }
                }
            }
        }
    }}}
// -------------------------------------------------------------
// TOURNAMENT DETAIL SCREEN
// -------------------------------------------------------------

@Composable
fun TournamentDetailScreen(
    initialTournament: Tournament,
    viewModel: RaivalViewModel,
    currentUser: User,
    onBack: () -> Unit) {
    val liveTournaments by viewModel.tournaments.collectAsState()
    val tournament = liveTournaments.find { it.id == initialTournament.id } ?: initialTournament
    var inGameName by remember { mutableStateOf("") }
    var teamRatingInput by remember { mutableStateOf("") }
    var accountTeamInput by remember { mutableStateOf("") }
    var mmPhone by remember { mutableStateOf(currentUser.phone) }
    var mmMethod by remember { mutableStateOf("MTN") }
    var isRegDialogVisible by remember { mutableStateOf(false) }
    var processingMsg by remember { mutableStateOf("") }
    var regStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val registrationsForTournament by viewModel.userRegistrations.collectAsState()
    val isAlreadyRegistered = registrationsForTournament.any { it.tournamentId == tournament.id }
    var selectedDetailTab by remember { mutableIntStateOf(0) }
 
// 0: Overview, 1: Playoff Bracket
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {        
// Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RaivalPrimary)
            }
            Text(
                text = "Tournament Details",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }        
// Hero Card
        val detailBgRes = remember(tournament) {
            when {
                tournament.game.contains("eFootball", ignoreCase = true) -> {
                    val efBgs = listOf(R.drawable.bg_efootball_1, R.drawable.bg_efootball_2, R.drawable.bg_efootball_3)
                    efBgs[Math.abs(tournament.id) % efBgs.size]
                }
                tournament.game.contains("DLS", ignoreCase = true) -> {
                    val dlsBgs = listOf(
                        R.drawable.dls_bg_new1,
                        R.drawable.dls_bg_new2,
                        R.drawable.dls_bg_new3,
                        R.drawable.dls_bg_new4
                    )
                    dlsBgs[Math.abs(tournament.id) % dlsBgs.size]
                }
                else -> R.drawable.bg_gaming_1
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RaivalSurface),
            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.35f))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = detailBgRes),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.85f
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Black.copy(alpha = 0.65f)
                                )
                            )
                        )
                )
                Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tournament.game.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = RaivalPrimary,
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                if (tournament.status == "Open") RaivalSuccess.copy(alpha = 0.2f) else RaivalError.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tournament.status,
                            color = if (tournament.status == "Open") RaivalSuccess else RaivalError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = tournament.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                
// EXPORT & IMPORT LOCAL SYNC BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    val context = androidx.compose.ui.platform.LocalContext.current
                                        Button(
                        onClick = {
                            val code = viewModel.exportTournamentToJson(tournament)
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                            android.widget.Toast.makeText(context, "Tournament code copied! Send this to your friends.", android.widget.Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = RaivalSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Code", color = RaivalSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                                        if (currentUser.role == "admin" || tournament.organizer == currentUser.username) {
                        var showImportPlayerDialog by remember { mutableStateOf(false) }
                        var playerCodeInput by remember { mutableStateOf("") }
                        var playerStatusMsg by remember { mutableStateOf("") }
                                                Button(
                            onClick = {
                                showImportPlayerDialog = true
                                playerCodeInput = ""
                                playerStatusMsg = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Player", color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                                                if (showImportPlayerDialog) {
                            Dialog(onDismissRequest = { showImportPlayerDialog = false }) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.25f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Import Player Registration", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Paste your friend's exported registration code below to add them to this tournament on your device.", color = RaivalTextSecondary, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedTextField(
                                            value = playerCodeInput,
                                            onValueChange = { playerCodeInput = it },
                                            placeholder = { Text("Paste player code here...", color = RaivalTextSecondary) },
                                            modifier = Modifier.fillMaxWidth().height(100.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = RaivalPrimary,
                                                unfocusedBorderColor = RaivalSurfaceLight,
                                                focusedContainerColor = RaivalSurfaceLight,
                                                unfocusedContainerColor = RaivalSurfaceLight
                                            )
                                        )
                                        if (playerStatusMsg.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                playerStatusMsg,
                                                color = if (playerStatusMsg.contains("success", ignoreCase = true)) RaivalSuccess else RaivalSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = { showImportPlayerDialog = false }) {
                                                Text("CANCEL", color = RaivalTextSecondary)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    if (playerCodeInput.isBlank()) {
                                                        playerStatusMsg = "Please paste a code first."
                                                        return@Button
                                                    }
                                                    viewModel.importRegistration(playerCodeInput, tournament.id) { success, msg ->
                                                        playerStatusMsg = msg
                                                        if (success) {
                                                            playerCodeInput = ""
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                                            ) {
                                                Text("IMPORT", color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
// Detail Row Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ENTRY FEE", fontSize = 10.sp, color = RaivalTextSecondary)
                        val entryFeeCoins = if (tournament.coinEntryFee > 0) tournament.coinEntryFee else tournament.entryFee.toInt()
                        Text(
                            if (entryFeeCoins > 0) "$entryFeeCoins Coins" else "FREE",
                            fontWeight = FontWeight.Bold,
                            color = RaivalSecondary
                        )
                    }
                    Column {
                        Text("PRIZE POOL", fontSize = 10.sp, color = RaivalTextSecondary)
                        val prizeCoins = if (tournament.coinPrize > 0) tournament.coinPrize else tournament.prize.toInt()
                        Text(
                            if (prizeCoins > 0) "$prizeCoins Coins" else "Free Play",
                            fontWeight = FontWeight.Bold,
                            color = RaivalPrimary
                        )
                    }
                    Column {
                        Text("FORMAT", fontSize = 10.sp, color = RaivalTextSecondary)
                        Text(
                            tournament.mode,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (tournament.format == "League") RaivalPrimary else RaivalSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "TOURNAMENT STRUCTURE: ${tournament.format.uppercase()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Setup Style: ${tournament.style} (Fair Matchmaking rules apply)",
                                fontSize = 10.sp,
                                color = RaivalTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
        Spacer(modifier = Modifier.height(16.dp))
        
// Segmented Tabs: Overview vs Bracket
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RaivalSurface, shape = RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (selectedDetailTab == 0) RaivalPrimary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { selectedDetailTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OVERVIEW",
                    color = if (selectedDetailTab == 0) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (selectedDetailTab == 1) RaivalPrimary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { selectedDetailTab = 1 }
                    .padding(vertical = 10.dp)
                    .testTag("bracket_tab_btn"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PLAYOFF BRACKET",
                    color = if (selectedDetailTab == 1) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (selectedDetailTab == 0) {
            Spacer(modifier = Modifier.height(24.dp))
            
// Description Section
        Text(
            text = "Description",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = RaivalPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = tournament.description.ifEmpty { "Join the tournament and prove your skills in this competitive gaming session." },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        
// Grid of Mode, Map, Time, Organizer
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Map: ${tournament.map.ifEmpty { "Standard/Random" }}", color = Color.White, fontSize = 13.sp)
                }
                Row(modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Time: ${tournament.time} GMT", color = Color.White, fontSize = 13.sp)
                }
                Row {
                    Icon(Icons.Default.Person, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Organizer: ${tournament.organizer}", color = Color.White, fontSize = 13.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
// Rules Section
        Text(
            text = "Rules & Regulations",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = RaivalPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RaivalSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val rulesList = tournament.rules.split("\n").filter { it.isNotBlank() }
                if (rulesList.isEmpty()) {
                    Text("1. Standard competitive gameplay rules.\n2. Friendly sportsmanship is mandatory.\n3. Cheating or hacking will result in an immediate ban.", color = Color.White, lineHeight = 20.sp, fontSize = 13.sp)
                }
else {
                    rulesList.forEachIndexed { idx, rule ->
                        Row(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text("${idx + 1}. ", color = RaivalSecondary, fontWeight = FontWeight.Bold)
                            Text(rule, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        
// Actions
        if (isAlreadyRegistered) {
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            val context = androidx.compose.ui.platform.LocalContext.current
                        Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { 
/* Already inside! Open social chat room maybe */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSuccess),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("YOU ARE REGISTERED", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                    onClick = {
                        val reg = registrationsForTournament.find { it.tournamentId == tournament.id }
                        if (reg != null) {
                            val code = viewModel.exportRegistration(reg, currentUser)
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                            android.widget.Toast.makeText(context, "Registration entry code copied! Send it to the host.", android.widget.Toast.LENGTH_LONG).show()
                        }
else {
                            android.widget.Toast.makeText(context, "Failed to find registration entry.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COPY ENTRY CODE FOR HOST", color = RaivalPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text(
                    text = "Send this code to the tournament host so they can import you on their device.",
                    color = RaivalTextSecondary,
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
        }
else if (tournament.status == "Open") {
            Button(
                onClick = { isRegDialogVisible = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("REGISTER FOR TOURNAMENT", color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
else {
            Button(
                onClick = { },
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("REGISTRATION CLOSED", color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
        }
else {
            TournamentBracketView(
                tournament = tournament,
                viewModel = viewModel,
                currentUser = currentUser
            )
        }
    }
    
// Registration Flow Modal
    if (isRegDialogVisible) {
        Dialog(onDismissRequest = { isRegDialogVisible = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tournament Entry",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (processingMsg.isNotEmpty()) {
                        Text(processingMsg, color = RaivalSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (regStatus != null) {
                        Text(
                            text = regStatus?.second ?: "",
                            color = if (regStatus?.first ?: false) RaivalSuccess else RaivalError,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                isRegDialogVisible = false
                                regStatus = null
                                processingMsg = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                        ) {
                            Text("OK", color = Color.Black)
                        }
                    }
else {
                        OutlinedTextField(
                            value = inGameName,
                            onValueChange = { inGameName = it },
                            label = { Text("Your In-Game Name (IGN)") },
                            placeholder = { Text("e.g. Messi10") },
                            supportingText = { Text("This is your username or team name in either eFootball, DLS, or FC Mobile", color = RaivalTextSecondary, fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.Gamepad, contentDescription = null, tint = RaivalPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = RaivalPrimary,
                                focusedIndicatorColor = RaivalPrimary
                            )
                        )                        
// Determine game field label and helper text
                        val ratingLabel = when {
                            tournament.game.contains("FC Mobile", ignoreCase = true) || tournament.game.contains("FC", ignoreCase = true) || tournament.game.contains("FIFA", ignoreCase = true) -> "FC Mobile Team OVR"
                            tournament.game.contains("eFootball", ignoreCase = true) || tournament.game.contains("PES", ignoreCase = true) -> "eFootball Collective Strength"
                            else -> "DLS Team Strength"
                        }
                        val ratingPlaceholder = when {
                            tournament.game.contains("FC Mobile", ignoreCase = true) || tournament.game.contains("FC", ignoreCase = true) || tournament.game.contains("FIFA", ignoreCase = true) -> "e.g. 102"
                            tournament.game.contains("eFootball", ignoreCase = true) || tournament.game.contains("PES", ignoreCase = true) -> "e.g. 3100"
                            else -> "e.g. 85"
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = teamRatingInput,
                            onValueChange = { teamRatingInput = it.filter { char -> char.isDigit() } },
                            label = { Text(ratingLabel) },
                            placeholder = { Text(ratingPlaceholder) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = RaivalSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = RaivalSecondary,
                                focusedIndicatorColor = RaivalSecondary
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = accountTeamInput,
                            onValueChange = { accountTeamInput = it },
                            label = { Text("Built Account Team (Model Club)") },
                            placeholder = { Text("e.g. Barcelona, Brazil, Liverpool, Spain...") },
                            leadingIcon = { Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = RaivalSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = RaivalSecondary,
                                focusedIndicatorColor = RaivalSecondary
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Or tap a popular preset team:",
                            fontSize = 11.sp,
                            color = RaivalTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val popularTeams = listOf("Barcelona", "Brazil", "Spain", "Liverpool")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            popularTeams.forEach { team ->
                                val isSelected = accountTeamInput.equals(team, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) RaivalSecondary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) RaivalSecondary else Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { accountTeamInput = team }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = when (team) {
                                            "Barcelona" -> "Barcelona 🇪🇸"
                                            "Brazil" -> "Brazil 🇧🇷"
                                            "Spain" -> "Spain 🇪🇸"
                                            "Liverpool" -> "Liverpool 🏴󠁧󠁢󠁥󠁮󠁧󠁿"
                                            else -> team
                                        },
                                        color = if (isSelected) RaivalSecondary else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
// ⏱️ Rule Warning Banner
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = RaivalError.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, RaivalError.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Rule Warning",
                                        tint = RaivalSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "🚨 IMPORTANT RULES & DISQUALIFICATION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = RaivalSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• ⏱️ 5-MINUTE RULE: You must enter the match chat and check in within 5 minutes.\n" +
                                           "• 🚪 Failing to join and check in on time results in AUTOMATIC DISQUALIFICATION.\n" +
                                           "• 🕒 15-MINUTE MATCH LIMIT: Each match is strictly dedicated 15 minutes (including the 5-min chat), after which system default results auto-resolve it to begin the next set of matches.\n" +
                                           "• 🔑 Home Player (Player 1) must share the match lobby code in chat.",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 13.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
// Amount to pay info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Deducted Fee:", color = RaivalTextSecondary)
                            val entryFeeCoins = if (tournament.coinEntryFee > 0) tournament.coinEntryFee else tournament.entryFee.toInt()
                            Text(text = if (entryFeeCoins > 0) "$entryFeeCoins Coins" else "FREE", fontWeight = FontWeight.Bold, color = RaivalSecondary)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { isRegDialogVisible = false }) {
                                Text("Cancel", color = RaivalError)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (inGameName.trim().isEmpty()) {
                                        processingMsg = "Please fill in your in-game name."
                                        return@Button
                                    }
                                    val ratingInt = teamRatingInput.toIntOrNull()
                                    if (ratingInt == null || ratingInt <= 0) {
                                        processingMsg = "Please enter a valid numeric $ratingLabel."
                                        return@Button
                                    }
                                    if (accountTeamInput.trim().isEmpty()) {
                                        processingMsg = "Please select or enter the Built Account Team."
                                        return@Button
                                    }
                                    
// Specific range warnings/validations to make it realistic
                                    val isValidRange = when {
                                        tournament.game.contains("FC Mobile", ignoreCase = true) || tournament.game.contains("FC", ignoreCase = true) || tournament.game.contains("FIFA", ignoreCase = true) -> ratingInt in 50..160
                                        tournament.game.contains("eFootball", ignoreCase = true) || tournament.game.contains("PES", ignoreCase = true) -> ratingInt in 1000..3500
                                        else -> ratingInt in 30..105 
// DLS
                                    }
                                    if (!isValidRange) {
                                        processingMsg = "Please enter a realistic rating/strength for ${tournament.game}."
                                        return@Button
                                    }
                                    processingMsg = "Deducting registration coins..."
                                    viewModel.registerForTournament(
                                        tournamentId = tournament.id,
                                        inGameName = inGameName,
                                        paymentMethod = "COINS",
                                        phone = currentUser.phone,
                                        teamRating = ratingInt,
                                        modelTeam = accountTeamInput.trim()
                                    ) { success, msg ->
                                        processingMsg = ""
                                        regStatus = Pair(success, msg)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                            ) {
                                Text("CONFIRM ENTRY", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }}
// -------------------------------------------------------------
// TAB 2: LEADERBOARD & RANKINGS
// -------------------------------------------------------------

@Composable
fun LeaderboardTab(viewModel: RaivalViewModel, currentUser: User, onSelectUser: (User) -> Unit) {
    val allUsers by viewModel.allUsers.collectAsState()
    var selectedRankTimeframe by remember { mutableStateOf("all_time") }
 
// "all_time", "monthly", "weekly"
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RaivalSurface)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = RaivalSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "GLOBAL HALL OF FAME",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Compete, earn Raival Points, and top the leaderboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RaivalTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }        
// Sub Timeframe selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("all_time", "All-Time"),
                    Pair("monthly", "This Month"),
                    Pair("weekly", "This Week")
                ).forEach { (id, label) ->
                    val isSelected = selectedRankTimeframe == id
                    Button(
                        onClick = { selectedRankTimeframe = id },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) RaivalPrimary else RaivalSurface
                        ),
                        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
// Performance Chart - Custom canvas
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Top Player Points Distribution",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = RaivalSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
// Chart Drawing
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        val points = listOf(1400f, 1200f, 850f, 720f, 420f)
                        val maxPoint = 1600f
                        val path = Path()
                        val spacing = size.width / (points.size - 1)
                        points.forEachIndexed { index, point ->
                            val x = index * spacing
                            val y = size.height - (point / maxPoint * size.height)
                            if (index == 0) {
                                path.moveTo(x, y)
                            }
else {
                                path.lineTo(x, y)
                            }                            
// Draw a dot on each node
                            drawCircle(
                                color = RaivalPrimary,
                                radius = 4.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                        }
                        drawPath(
                            path = path,
                            color = RaivalPrimary,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
// Custom legends
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("#1", "#2", "#3", "#4", "#5").forEach { rank ->
                            Text(rank, color = RaivalTextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        items(
            items = allUsers.take(15).mapIndexed { index, user -> Pair(index + 1, user) },
            key = { it.second.id }
        ) { (rank, user) ->
            val isSelf = user.username == currentUser.username
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onSelectUser(user) }
                    .testTag("leaderboard_user_${user.username}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelf) RaivalPrimary.copy(alpha = 0.1f) else RaivalSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isSelf) RaivalPrimary else RaivalPrimary.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {                    
// Rank Badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                when (rank) {
                                    1 -> RaivalSecondary
                                    2 -> Color(0xFFC0C0C0)
                                    3 -> Color(0xFFCD7F32)
                                    else -> RaivalSurfaceLight
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#$rank",
                            fontWeight = FontWeight.Bold,
                            color = if (rank <= 3) Color.Black else Color.White,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    
// Username and full name
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.username + if (isSelf) " (You)" else "",
                            fontWeight = FontWeight.Bold,
                            color = if (isSelf) RaivalPrimary else Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${user.tournamentsPlayed} Tournaments Played",
                            fontSize = 11.sp,
                            color = RaivalTextSecondary
                        )
                    }                    
// Points
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${user.raivalPoints} SP",
                            fontWeight = FontWeight.Bold,
                            color = RaivalSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Coins Won: ${user.totalWinnings.toInt()}",
                            fontSize = 11.sp,
                            color = RaivalTextPrimary
                        )
                    }
                }
            }
        }
    }
}
// -------------------------------------------------------------
// TAB 4: WALLET & COIN STATS
// -------------------------------------------------------------

@Composable
fun WalletAndAnalyticsTab(viewModel: RaivalViewModel, currentUser: User) {
    val transactions by viewModel.userTransactions.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {        
// Balance Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RaivalSurface),
            border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "AVAILABLE COIN BALANCE", fontSize = 10.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${currentUser.coinBalance} COINS",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = RaivalSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
// Personal Stats Section
        Text(
            text = "Your Statistics",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = RaivalPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Win Rate", fontSize = 10.sp, color = RaivalTextSecondary)
                    val winRate = if (currentUser.tournamentsPlayed > 0) "${(currentUser.wins 
* 100) / currentUser.tournamentsPlayed}%" else "64%"
                    Text(winRate, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = RaivalSuccess)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Raival Points", fontSize = 10.sp, color = RaivalTextSecondary)
                    Text("${currentUser.raivalPoints}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = RaivalSecondary)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Coins Won", fontSize = 10.sp, color = RaivalTextSecondary)
                    Text("${currentUser.totalWinnings.toInt()}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = RaivalPrimary)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
// Transaction History List
        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = RaivalPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions found yet.", color = RaivalTextSecondary)
            }
        }
else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                transactions.forEach { tx ->
                    val isWithdraw = tx.type == "WITHDRAWAL"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isWithdraw) Icons.Default.ArrowOutward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isWithdraw) RaivalError else RaivalSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = tx.tournamentTitle, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    text = "Ref: ${tx.reference} • COINS",
                                    fontSize = 11.sp,
                                    color = RaivalTextSecondary
                                )
                            }
                            Text(
                                text = "${if (isWithdraw) "-" else "+"} ${tx.amount.toInt()} Coins",
                                fontWeight = FontWeight.Bold,
                                color = if (isWithdraw) RaivalError else RaivalSuccess,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        
// Sign Out Button
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
            border = BorderStroke(1.dp, RaivalError.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = RaivalError)
            Spacer(modifier = Modifier.width(8.dp))
            Text("LOG OUT ACCOUNT", color = RaivalError, fontWeight = FontWeight.Bold)
        }
    }}
// -------------------------------------------------------------
// TAB 5: ADMIN CONTROLS PANEL (MIGRATED TO WEB)
// -------------------------------------------------------------

@Composable
fun AdminTab(viewModel: RaivalViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val adminUrl = "https://ais-dev-aji2gvijxweopbvj7yg3zj-999423339111.europe-west2.run.app/admin"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RaivalBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RaivalPrimary)
            }
            Text(
                text = "Admin Panel",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        
// Shield / Security Emblem Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(RaivalPrimary.copy(alpha = 0.1f), CircleShape)
                .border(2.dp, RaivalPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Shield Icon",
                tint = RaivalPrimary,
                modifier = Modifier.size(54.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "SECURE WEB PORTAL",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            ),
            color = RaivalPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ADMIN FUNCTIONS MIGRATED",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        
// Live status pill
        Surface(
            color = RaivalSuccess.copy(alpha = 0.15f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, RaivalSuccess.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(RaivalSuccess, CircleShape)
                )
                Text(
                    text = "CONSOLE ONLINE & SYNCED",
                    color = RaivalSuccess,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
// Card explaining migration benefits/reasons
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RaivalSurface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "To guarantee bulletproof security, high performance under load, and comprehensive management capabilities, all administrator actions are now hosted on the Raival Web Management Console.",
                    color = RaivalTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Web Console Capabilities:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                val benefits = listOf(
                    "🏆 Advanced Bracket Generator" to "Effortlessly configure custom tournament bracket layouts (World Cup Styles, Champions League format, EPL single league structures, or direct knockouts).",
                    "📊 Real-time Economy Audit Ledger" to "Monitor deposit transactions, withdrawals, coin distributions, and participation rewards.",
                    "🛡️ Account & Permission Moderation" to "Ban, unban, and elevate user roles securely via centralized cloud databases.",
                    "💼 Automated Hubtel MoMo Integration" to "Safely review Mobile Money credentials and authorize user withdrawals dynamically."
                )
                benefits.forEach { (title, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("⚡", color = RaivalSecondary, fontSize = 14.sp)
                        Column {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = desc,
                                color = RaivalTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
        
// Primary Launch Button
        Button(
            onClick = {
                try {
                    uriHandler.openUri(adminUrl)
                }
catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Could not open browser link automatically.", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Launch,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "LAUNCH WEB CONSOLE",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        
// Copy Link Button
        OutlinedButton(
            onClick = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(adminUrl))
                android.widget.Toast.makeText(context, "Secure link copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "COPY PORTAL LINK",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun OfflineFeatureBlocker(
    title: String,
    icon: ImageVector,
    onNavigateToCasual: () -> Unit,
    onNavigateToProfile: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RaivalBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = RaivalSurface),
            border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RaivalError,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This screen is currently unavailable offline. Active internet connection is required to sync matchmaking, load tourney pools, or fetch community chats.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = RaivalTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "DISCOVER AVAILABLE OFFLINE FEATURES:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    ),
                    color = RaivalSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigateToCasual,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🎮 PLAY OFFLINE CASUAL GAMES", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onNavigateToProfile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("📊 VIEW PROGRESS & TEST SOUNDS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }}

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun MarketplaceTabWithWallet(
    viewModel: RaivalViewModel,
    currentUser: User,
    onOpenProfile: () -> Unit) {
    var showWalletView by remember { mutableStateOf(false) }
    var selectedListing by remember { mutableStateOf<MarketplaceListing?>(null) }
    var showCreateListing by remember { mutableStateOf(false) }
        val activeListings by viewModel.activeListings.collectAsState()
    val allListings by viewModel.allListings.collectAsState()
    val userTransactions by viewModel.userMarketplaceTransactions.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
        Scaffold(
        topBar = {
            if (!showWalletView && selectedListing == null && !showCreateListing) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (showWalletView) "My Wallet" else "Marketplace",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        if (showWalletView) {
                            IconButton(onClick = { showWalletView = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        }
                    },
                    actions = {                        
// Wallet balance chip
                        Row(
                            modifier = Modifier
                                .background(RaivalSurfaceLight, shape = RoundedCornerShape(20.dp))
                                .clickable { showWalletView = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet",
                                tint = RaivalSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentUser.coinBalance} Coins",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Top Up",
                                tint = RaivalPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                
// Profile button
                        IconButton(
                            onClick = onOpenProfile,
                            modifier = Modifier.testTag("top_bar_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "My Profile",
                                tint = RaivalPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface)
                )
            }
        }
    ) { innerPadding ->
        if (showWalletView) {
            WalletAndAnalyticsTab(viewModel, currentUser)
        }
else if (selectedListing != null) {
            MarketplaceScreen(
                viewModel = viewModel,
                currentUser = currentUser,
                onUserClick = { onOpenProfile() }
            ) 
// Note: This would need modification to show listing detail
        }
else if (showCreateListing) {
            MarketplaceScreen(
                viewModel = viewModel,
                currentUser = currentUser,
                onUserClick = { onOpenProfile() }
            )
        }
else {            
// Marketplace browse view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(RaivalBackground),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {                
// Header with wallet summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Marketplace", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                            Text("${activeListings.size} active listings", color = RaivalTextSecondary, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { showWalletView = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Wallet: ${currentUser.coinBalance} Coins", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
                                
// Listings
                if (activeListings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                            .background(RaivalSurface, shape = RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, RaivalSurfaceLight), shape = RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No listings available", color = RaivalTextSecondary, fontSize = 16.sp)
                            Text("Be the first to list your team!", color = RaivalTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCreateListing = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Create Listing", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(activeListings) { listing ->
                            MarketplaceListingCard(
                                listing = listing,
                                currentUser = currentUser,
                                viewModel = viewModel,
                                onClick = { selectedListing = listing }
                            )
                        }
                    }
                }
            }
        }
    }}

@Composable
fun MarketplaceListingCard(
    listing: MarketplaceListing,
    currentUser: User,
    viewModel: RaivalViewModel,
    onClick: () -> Unit) {
    val isOwner = listing.sellerId == currentUser.id
        Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {                
// Game badge
                Box(
                    modifier = Modifier
                        .background(getGameTheme(listing.game).accentColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = listing.game,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = getGameTheme(listing.game).accentColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                                
// Price
                Text(
                    text = "${listing.price} GHS",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = RaivalSecondary
                )
            }
                        Spacer(modifier = Modifier.height(12.dp))
                        
// Team name and rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = listing.teamName,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "OVR ${listing.ovrRating} • ${listing.specialCardsCount} Special Cards • ${listing.coins} Coins",
                        color = RaivalTextSecondary,
                        fontSize = 12.sp
                    )
                }                                
// Seller info
                if (!isOwner) {
                    Text(
                        text = "by ${listing.sellerName}",
                        color = RaivalPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
                        Spacer(modifier = Modifier.height(12.dp))
                        
// Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isOwner) {
                    OutlinedButton(
                        onClick = { /* Edit listing */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, RaivalSurfaceLight)
                    ) {
                        Text("Manage", color = Color.White, fontSize = 12.sp)
                    }
                }
else {
                    Button(
                        onClick = { /* Initiate purchase */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Buy Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { /* Make offer */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, RaivalSurfaceLight)
                    ) {
                        Text("Make Offer", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }}