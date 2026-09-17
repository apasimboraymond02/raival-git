package com.example.ui.screens

import com.example.BuildConfig

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaivalViewModel
import kotlinx.serialization.json.*
import java.util.*

@SuppressLint("DefaultLocale")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaivalProgressionHubScreen(
    viewModel: RaivalViewModel,
    user: User,
    onBack: () -> Unit
) {
    var activeSubScreen by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showSnackbar by remember { mutableStateOf(false) }

    fun triggerNotification(msg: String) {
        snackbarMessage = msg
        showSnackbar = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (activeSubScreen == null) "RAIVAL PROGRESSION HUB" else activeSubScreen?.uppercase() ?: "RAIVAL PROGRESSION HUB",
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        if (activeSubScreen == null) {
                            Text(
                                text = "Lvl ${user.level} • ${user.coinBalance} Coins • ₵${String.format("%.2f", user.balance)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = RaivalSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeSubScreen != null) {
                            activeSubScreen = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface),
                actions = {
                    // Fast XP Simulator — debug builds only. Never grants free XP in production.
                    if (BuildConfig.DEBUG) Button(
                        onClick = {
                            viewModel.addXp(250) { newLvl ->
                                triggerNotification("🎉 LEVEL UP! You reached Level $newLvl!")
                            }
                            triggerNotification("⚡ Gained +250 XP!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary.copy(alpha = 0.15f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sim XP", color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            )
        },
        snackbarHost = {
            if (showSnackbar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = RaivalSecondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = snackbarMessage, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    LaunchedEffect(showSnackbar) {
                        kotlinx.coroutines.delay(3000)
                        showSnackbar = false
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RaivalSurface)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = activeSubScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "SubScreenNavigation"
            ) { screen ->
                when (screen) {
                    null -> MainHubGrid(
                        user = user,
                        onSelect = { activeSubScreen = it }
                    )
                    "Coin Economy Hub" -> CoinEconomyHubView(viewModel, user, ::triggerNotification)
                    "Rivalry System" -> RivalrySystemView(viewModel, user, ::triggerNotification)
                    "Skill Tree & Leveling" -> SkillTreeView(viewModel, user, ::triggerNotification)
                    "Achievement Badges" -> AchievementsView(viewModel, user, ::triggerNotification)
                    "XP & Progression" -> XPProgressionView(viewModel, user, ::triggerNotification)
                    "Daily Missions" -> DailyMissionsView(viewModel, user, ::triggerNotification)
                    "Weekly Tournaments" -> WeeklyTournamentsView(viewModel, user, ::triggerNotification)
                    "Streak Protection" -> StreakProtectionView(viewModel, user, ::triggerNotification)
                    "Training Mode" -> TrainingModeView(viewModel, user, ::triggerNotification)
                    "Pro Tips & Tutorials" -> ProTipsView(viewModel, user, ::triggerNotification)
                    "Referral Chain" -> ReferralChainView(viewModel, user, ::triggerNotification)
                    "Welcome Bonus" -> WelcomeBonusView(viewModel, user, ::triggerNotification)
                    "Social Rewards" -> SocialLoginRewardsView(viewModel, user, ::triggerNotification)
                    "Rating Rewards" -> AppStoreRatingRewardsView(viewModel, user, ::triggerNotification)
                    "Referral Tournament" -> ReferralTournamentView(viewModel, user, ::triggerNotification)
                    "Custom Avatars" -> CustomAvatarsView(viewModel, user, ::triggerNotification)
                    "Custom Gaming Names" -> CustomGamingNamesView(viewModel, user, ::triggerNotification)
                    "Theme Settings" -> ThemeSettingsView(viewModel, user, ::triggerNotification)
                    "Animated Badges" -> AnimatedBadgesView(viewModel, user, ::triggerNotification)
                    "Match Entry Effects" -> MatchEntryAnimationsView(viewModel, user, ::triggerNotification)
                    "Tournament Store" -> TournamentStoreView(viewModel, user, ::triggerNotification)
                    "Custom Notification Tones" -> CustomNotificationTonesView(viewModel, user, ::triggerNotification)
                    "Quick Game Shortcuts" -> QuickGameShortcutsView(viewModel, user, ::triggerNotification)
                    "Match Archive" -> MatchArchiveView(viewModel, user, ::triggerNotification)
                    "Are You Sure? Protection" -> AreYouSureProtectionView(viewModel, user, ::triggerNotification)
                    "Tournament Countdown Widget" -> TournamentCountdownWidgetView(viewModel, user, ::triggerNotification)
                    "Match Reports" -> MatchReportsView(viewModel, user, ::triggerNotification)
                    "Battle Pass System" -> BattlePassSystemView(viewModel, user, ::triggerNotification)
                    "Prediction League" -> PredictionLeagueView(viewModel, user, ::triggerNotification)
                    "Ambassador Program" -> AmbassadorProgramView(viewModel, user, ::triggerNotification)
                    "Official Certifications" -> OfficialCertificationsView(viewModel, user, ::triggerNotification)
                    "Competitive Mode" -> CompetitiveModeView(viewModel, user, ::triggerNotification)
                    "Pro League" -> ProLeagueView(viewModel, user, ::triggerNotification)
                    "Season Playoffs" -> SeasonPlayoffsView(viewModel, user, ::triggerNotification)
                    "Player Card Collection" -> PlayerCardCollectionView(viewModel, user, ::triggerNotification)
                    "Live Tournament Watch" -> LiveTournamentWatchView(viewModel, user, ::triggerNotification)
                    "Interactive Replays" -> InteractiveReplaysView(viewModel, user, ::triggerNotification)
                    "Personal Performance Dashboard" -> PersonalPerformanceDashboardView(viewModel, user, ::triggerNotification)
                    "Opponent Analysis Tool" -> OpponentAnalysisToolView(viewModel, user, ::triggerNotification)
                    "Gaming Tips & Tricks" -> GamingTipsAndTricksView(viewModel, user, ::triggerNotification)
                    "Cross-Platform Play" -> CrossPlatformPlayView(viewModel, user, ::triggerNotification)
                    "Blockchain Integration" -> BlockchainIntegrationView(viewModel, user, ::triggerNotification)
                    "Analytics & Insights" -> AnalyticsAndInsightsView(viewModel, user, ::triggerNotification)
                }
            }
        }
    }
}

@Composable
fun MainHubGrid(
    user: User,
    onSelect: (String) -> Unit
) {
    val items = listOf(
        ProgressionHubItem("Coin Economy Hub", "🪙", "Daily login streaks, match participation, milestones & community", Color(0xFFFFD700)),
        ProgressionHubItem("Rivalry System", "⚔️", "Track head-to-head match drama & stats", RaivalPrimary),
        ProgressionHubItem("Skill Tree & Leveling", "🌳", "Unlock attacking & defensive masteries", RaivalSecondary),
        ProgressionHubItem("Achievement Badges", "🏅", "50+ collection badges to unlock", Color(0xFF00D4FF)),
        ProgressionHubItem("XP & Progression", "📈", "Check levels, milestones & daily logs", Color(0xFFFF5252)),
        ProgressionHubItem("Daily Missions", "📅", "Quests to earn coins & progression XP", Color(0xFF4CAF50)),
        ProgressionHubItem("Weekly Tournaments", "🏆", "週末大會 & join competitive tournaments", Color(0xFFE040FB)),
        ProgressionHubItem("Streak Protection", "🛡️", "Buy win-streak shields & recoveries", Color(0xFF00E676)),
        ProgressionHubItem("Training Mode", "🤖", "Practice with AI difficulty adjustments", Color(0xFFFF9100)),
        ProgressionHubItem("Pro Tips & Tutorials", "💡", "Guides to dominate DLS / eFootball", Color(0xFFFFFF00)),
        ProgressionHubItem("Referral Chain", "🔗", "Earn multiplier rewards from viral invites", Color(0xFF2979FF)),
        ProgressionHubItem("Welcome Bonus", "🎁", "Claim free coins, skins & weekly login rewards", Color(0xFFFFD700)),
        ProgressionHubItem("Social Rewards", "🔗", "Connect social accounts & share match results", Color(0xFF1DA1F2)),
        ProgressionHubItem("Rating Rewards", "⭐", "Rate Raival on Play Store & claim cash rewards", Color(0xFFFF9800)),
        ProgressionHubItem("Referral Tournament", "🏆", "Elite bracket tournament open only to referrers", Color(0xFFE91E63)),
        ProgressionHubItem("Custom Avatars", "🖼️", "Personalize profile templates & upload avatars", Color(0xFF00ABCD)),
        ProgressionHubItem("Custom Gaming Names", "✏️", "Set unique gamer handles, colors, glows & titles", Color(0xFF9C27B0)),
        ProgressionHubItem("Theme Settings", "🌓", "Choose between Light, Dark, or System Default theme modes", Color(0xFF4CAF50)),
        ProgressionHubItem("Animated Badges", "✨", "Collect premium pulsing, glowing & spinning badges", Color(0xFF03A9F4)),
        ProgressionHubItem("Match Entry Effects", "🎬", "Equip professional walkout & stadium entry styles", Color(0xFFE040FB)),
        ProgressionHubItem("Tournament Store", "🛒", "Buy ticket packages, boosters, flash sale bundles", Color(0xFFFFEB3B)),
        ProgressionHubItem("Custom Notification Tones", "🎵", "Assign different sound effects to match notifications", Color(0xFFFF4081)),
        ProgressionHubItem("Quick Game Shortcuts", "⚡", "Create, customize, and place home screen shortcuts", Color(0xFFE040FB)),
        ProgressionHubItem("Match Archive", "🗄️", "View full historic match statistics, reports & notes", Color(0xFF00E676)),
        ProgressionHubItem("Are You Sure? Protection", "🛡️", "Set up double confirm, PIN or Hold-to-Confirm alerts", Color(0xFFFF1744)),
        ProgressionHubItem("Tournament Countdown Widget", "⏳", "Place live tournament timers & track slots", Color(0xFFFF9100)),
        ProgressionHubItem("Match Reports", "📊", "Deep analytical graphs, timeline & tips for matches", Color(0xFF2979FF)),
        ProgressionHubItem("Battle Pass System", "🎟️", "Level up season tiers to claim exclusive coin bundles", Color(0xFFFFD700)),
        ProgressionHubItem("Prediction League", "🔮", "Predict match outcomes & climb the prediction charts", Color(0xFFE040FB)),
        ProgressionHubItem("Ambassador Program", "👑", "Help the community, refer players & earn monthly salaries", Color(0xFFFFD700)),
        ProgressionHubItem("Official Certifications", "📜", "Cryptographic badges & LinkedIn verification", Color(0xFFFFD700)),
        ProgressionHubItem("Competitive Mode", "⚔️", "High-stakes lobby with strict anti-cheat & double prize", Color(0xFFFF5252)),
        ProgressionHubItem("Pro League", "👑", "Invite-only elite league with real money prizes", Color(0xFFFFD700)),
        ProgressionHubItem("Season Playoffs", "🏆", "End-of-season championship with live brackets", Color(0xFFFFE040)),
        ProgressionHubItem("Player Card Collection", "🃏", "Collect, trade and open player card boosters", Color(0xFFE040FB)),
        ProgressionHubItem("Live Tournament Watch", "📺", "Watch live finals, chat & make predictions", Color(0xFF00E676)),
        ProgressionHubItem("Interactive Replays", "🎬", "Slow-motion replays & tactical whiteboard drawing", Color(0xFF03A9F4)),
        ProgressionHubItem("Personal Performance Dashboard", "📊", "Deep analytics & dynamic skill attribute charts", Color(0xFF00D4FF)),
        ProgressionHubItem("Opponent Analysis Tool", "🔍", "Scout opponents, formations & strategic weaknesses", Color(0xFFFF9100)),
        ProgressionHubItem("Gaming Tips & Tricks", "💡", "Pro strategy library & community contributions", Color(0xFFFFFF00)),
        ProgressionHubItem("Cross-Platform Play", "🌐", "Seamless iOS/Android lobby with ping diagnostics", Color(0xFF2979FF)),
        ProgressionHubItem("Blockchain Integration", "🔗", "Public match ledgers & NFT card minting", Color(0xFF4CAF50)),
        ProgressionHubItem("Analytics & Insights", "📈", "Administrative system growth & retention charts", Color(0xFFFF4081))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PROGRESSION HUB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = RaivalSecondary
                        )
                        Text(
                            text = "Level Up Your Game",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Explore game achievements, buy protection shields, unlock skill points, and dominate competitive esports matchmaking!",
                            fontSize = 12.sp,
                            color = RaivalTextSecondary
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "CHOOSE PROGRESSION SYSTEM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = RaivalPrimary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        items(items, key = { it.title }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item.title) },
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(0.5.dp, item.accentColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(item.accentColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.icon, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = item.desc,
                            fontSize = 12.sp,
                            color = RaivalTextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = RaivalTextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

data class ProgressionHubItem(
    val title: String,
    val icon: String,
    val desc: String,
    val accentColor: Color
)


// -----------------------------------------------------------------------------
// FEATURE 1: RIVALRY SYSTEM
// -----------------------------------------------------------------------------
@Composable
fun RivalrySystemView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var selectedRival by remember { mutableStateOf<String?>(null) }
    
    val rivalsList = listOf(
        RivalData("YawPES", "Gold Rival", 4, "🔥 Scale: High (🔥 4/5)", "3-2 Win Rate", "YawPES is on a 2-win streak against you", 10),
        RivalData("Akwasi_Gamer", "Legendary Rival", 5, "🔥 Scale: Extreme (🔥 5/5)", "5-5 Draw", "You won the last epic encounter", 15),
        RivalData("FireStriker", "Competitor", 2, "🔥 Scale: Moderate (🔥 2/5)", "2-1 Win Rate", "First match ended in a draw", 5)
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚔️ RIVALRY STORYLINES", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Rivalries build automatically when you play the same opponent multiple times. Matches decided by 1 goal or less intensify rivalries. Rivalry matches award +20% Coin Bonus!", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Text("YOUR ACTIVE RIVALRIES", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        items(rivalsList, key = { it.name }) { rival ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, if (selectedRival == rival.name) RaivalSecondary else Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).background(RaivalPrimary.copy(alpha = 0.15f), shape = CircleShape), contentAlignment = Alignment.Center) {
                                Text(rival.name.take(2).uppercase(), fontWeight = FontWeight.Bold, color = RaivalPrimary, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(rival.name, fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp)
                                Text(rival.tier, color = RaivalSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Text(rival.heat, color = Color(0xFFFF5722), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Overall Record: ${rival.record}", color = RaivalTextSecondary, fontSize = 12.sp)
                    Text(text = rival.status, color = RaivalTextSecondary, fontSize = 11.sp)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.addXp(120)
                                viewModel.earnCoins(12) // includes 20% bonus
                                notify("⚔️ Rivalry match request sent to ${rival.name}! Prepare for the arena cup! (+20% coins on match completion)")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Challenge Rival", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                notify("💬 You sent trash talk to ${rival.name}: 'Prepare for defeat in the next arena cup!' 🔥")
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, RaivalTextSecondary)
                        ) {
                            Text("Trash Talk", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

data class RivalData(
    val name: String,
    val tier: String,
    val heatLevel: Int,
    val heat: String,
    val record: String,
    val status: String,
    val bonus: Int
)


// -----------------------------------------------------------------------------
// FEATURE 2: SKILL TREE & LEVELING SYSTEM
// -----------------------------------------------------------------------------
@Composable
fun SkillTreeView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    val skills = listOf(
        SkillAbility("power_shot", "Power Shot", "⚔️ Attacking Branch", "+10% shot power in matches", 30, Icons.Default.FlashOn),
        SkillAbility("quick_reflexes", "Quick Reflexes", "🛡️ Defending Branch", "+10% goalkeeper save rate", 30, Icons.Default.Shield),
        SkillAbility("tactical_genius", "Tactical Genius", "🧠 Strategy Branch", "See opponent formation weaknesses", 40, Icons.Default.Psychology),
        SkillAbility("coin_magnet", "Coin Magnet", "💬 Social/Economy Branch", "+5% bonus coins from tournaments", 50, Icons.Default.MonetizationOn),
        SkillAbility("predictor", "Predictor Magic", "✨ Special Branch", "+15% prediction win streak multiplier", 50, Icons.Default.AutoAwesome)
    )

    val unlockedSet = remember(user.unlockedSkills) {
        user.unlockedSkills.split(",").filter { it.isNotEmpty() }.toSet()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🌳 SKILL TREE PROGRESSION", fontWeight = FontWeight.Black, color = RaivalSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Spend earned tournament Coins to unlock custom masteries that enhance your team performance and coin earnings!", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Text("ACTIVE MASTERIES", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        items(skills, key = { it.id }) { skill ->
            val isUnlocked = unlockedSet.contains(skill.id)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, if (isUnlocked) RaivalSecondary.copy(alpha = 0.4f) else Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp).background(if (isUnlocked) RaivalSecondary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Icon(skill.icon, contentDescription = null, tint = if (isUnlocked) RaivalSecondary else Color.White, modifier = Modifier.size(22.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(skill.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text(skill.branch, color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(skill.desc, color = RaivalTextSecondary, fontSize = 12.sp)
                    }
                    
                    if (isUnlocked) {
                        Box(modifier = Modifier.background(RaivalSecondary.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)).padding(6.dp)) {
                            Text("ACTIVE", color = RaivalSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.unlockSkillAbility(skill.id, skill.cost) { success ->
                                    if (success) {
                                        notify("🌳 Unlocked '${skill.name}' mastery successfully!")
                                    } else {
                                        notify("❌ Insufficient coin balance! Earn more coins from tournaments.")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("${skill.cost} Coins", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("XP BOOSTERS SHOP", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Double XP Booster (30 Mins)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("Provides 2x XP multiplier across matches & cups.", fontSize = 11.sp, color = RaivalTextSecondary)
                    }
                    Button(
                        onClick = {
                            viewModel.spendCoins(50) { success ->
                                if (success) {
                                    notify("⚡ Double XP booster activated for 30 minutes!")
                                } else {
                                    notify("❌ Insufficient coins!")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                    ) {
                        Text("50 Coins", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class SkillAbility(
    val id: String,
    val name: String,
    val branch: String,
    val desc: String,
    val cost: Int,
    val icon: ImageVector
)


// -----------------------------------------------------------------------------
// FEATURE 3: ACHIEVEMENT BADGES
// -----------------------------------------------------------------------------
@Composable
fun AchievementsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    val badges = listOf(
        BadgeDef("first_match", "First Steps", "Play your first arena match", "Common", Color(0xFF4CAF50)),
        BadgeDef("fifty_matches", "50 Matches", "Compete in 50 matches overall", "Uncommon", Color(0xFF2196F3)),
        BadgeDef("hat_trick", "Hat-trick Hero", "Score 3+ goals in one competitive match", "Rare", Color(0xFF9C27B0)),
        BadgeDef("clean_sheet", "Clean Sheet King", "Concede zero goals in 5 matches", "Rare", Color(0xFF9C27B0)),
        BadgeDef("cup_winner", "Grand Champion", "Win a weekend championship cup", "Epic", Color(0xFFFF5722)),
        BadgeDef("win_streak_10", "Unstoppable Force", "Achieve an active 10-match win streak", "Legendary", Color(0xFFFFD700)),
        BadgeDef("points_tycoon", "Raival Legend", "Reach 1500+ Raival Points in standard leaderboards", "Mythic", Color(0xFFE91E63))
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏅 MY BADGES COLLECTION", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Earn status trophies and milestone medals for completing high-tier objectives. Select any badge to highlight it publicly on your profile!", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Text("BADGE MEDAL SHOWCASE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        gridItems(badges, 2) { badge ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        notify("🏅 Showcasing '${badge.name}' as your featured badge medal on your public profile card!")
                    },
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, badge.color.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(50.dp).background(badge.color.copy(alpha = 0.12f), shape = CircleShape), contentAlignment = Alignment.Center) {
                        Text("🏅", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(badge.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Text(badge.rarity.uppercase(), color = badge.color, fontWeight = FontWeight.Black, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(badge.desc, color = RaivalTextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

data class BadgeDef(
    val id: String,
    val name: String,
    val desc: String,
    val rarity: String,
    val color: Color
)

fun <T> LazyListScope.gridItems(
    data: List<T>,
    columnCount: Int,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    itemContent: @Composable BoxScope.(T) -> Unit
) {
    val rows = data.chunked(columnCount)
    items(rows) { listRow ->
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = horizontalArrangement
        ) {
            listRow.forEach { item ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                ) {
                    itemContent(item)
                }
            }
            val rSize = listRow.count()
            if (rSize < columnCount) {
                repeat(columnCount - rSize) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


// -----------------------------------------------------------------------------
// FEATURE 4: XP & PROGRESSION SYSTEM
// -----------------------------------------------------------------------------
@Composable
fun XPProgressionView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    val nextLevel = user.level + 1
    val currentXp = user.xp
    val requiredXp = user.level * 1000
    val progress = currentXp.toFloat() / requiredXp.toFloat()

    val levelRewards = listOf(
        LvlReward(5, "25 Coins", "Basic milestone reward bundle"),
        LvlReward(10, "50 Coins + Gold Avatar Frame", "Premium competitive border unlock"),
        LvlReward(25, "100 Coins + Rare Fighter Badge", "Trophy collection expansion pack"),
        LvlReward(50, "200 Coins + Epic Breathing Badge", "Supreme elite competitor banner")
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.15f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 PROGRESSION XP HUB", fontWeight = FontWeight.Bold, color = RaivalSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("LEVEL ${user.level}", fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
                        Text("$currentXp / $requiredXp XP", fontWeight = FontWeight.Bold, color = RaivalSecondary, fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = RaivalSecondary,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Earn ${requiredXp - currentXp} XP more to reach Level $nextLevel!", fontSize = 11.sp, color = RaivalTextSecondary)
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.earnCoins(25)
                    notify("Claimed Daily Login Reward of 25 Coins! Come back tomorrow for more multipliers.")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
            ) {
                Text("Claim Daily Login Reward (25 Coins)", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Text("LEVEL UP REWARD MILESTONES", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        items(levelRewards, key = { it.level }) { reward ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).background(RaivalSecondary.copy(alpha = 0.12f), shape = CircleShape), contentAlignment = Alignment.Center) {
                        Text("🎁", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reach Level ${reward.level}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(reward.title, color = RaivalPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(reward.desc, color = RaivalTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

data class LvlReward(
    val level: Int,
    val title: String,
    val desc: String
)


// -----------------------------------------------------------------------------
// FEATURE 5: DAILY MISSIONS
// -----------------------------------------------------------------------------
@Composable
fun DailyMissionsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    val missions = listOf(
        MissionDef("m_1", "Play 3 Casual Arena Matches", "Casual", 3, 2, 50, 5),
        MissionDef("m_2", "Score 5 Goals in Tournaments", "Skill", 5, 4, 75, 10),
        MissionDef("m_3", "Get 1 Clean Sheet Victory", "Defense", 1, 0, 100, 15),
        MissionDef("m_4", "Submit Referral Invites", "Social", 1, 1, 120, 20)
    )

    val completedSet = remember(user.completedMissions) {
        user.completedMissions.split(",").filter { it.isNotEmpty() }.toSet()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("📅 DAILY MISSIONS TRACKER", fontWeight = FontWeight.Bold, color = RaivalPrimary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Missions reset every 24 hours. Clear missions to secure instant coins & experience boosts!", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Streak Shield Progress: 🔥 5-Day Active Streak (+25% Coins multiplier)", color = RaivalSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ACTIVE QUESTS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                TextButton(onClick = {
                    viewModel.refreshMission(5) { success ->
                        if (success) {
                            notify("🔄 Paid 5 Coins to reroll daily missions! New targets acquired.")
                        } else {
                            notify("❌ Insufficient coins!")
                        }
                    }
                }) {
                    Text("Reroll (5 Coins)", color = RaivalSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(missions, key = { it.id }) { mission ->
            val isClaimed = completedSet.contains(mission.id)
            val isComplete = mission.current >= mission.target
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, if (isClaimed) Color.White.copy(alpha = 0.1f) else RaivalPrimary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(mission.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Box(modifier = Modifier.background(RaivalPrimary.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(mission.category.uppercase(), color = RaivalPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val percentage = mission.current.toFloat() / mission.target.toFloat()
                        LinearProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = RaivalPrimary,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${mission.current}/${mission.target}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row {
                            Text("+${mission.xpAward} XP", color = RaivalSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 8.dp))
                            Text("+${mission.coinAward} Coins", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        
                        if (isClaimed) {
                            Text("CLAIMED", color = RaivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        } else {
                            Button(
                                onClick = {
                                    if (isComplete) {
                                        viewModel.claimMissionReward(mission.id, mission.xpAward, mission.coinAward) { success ->
                                            if (success) {
                                                notify("🎉 Mission Claimed! Earned +${mission.xpAward} XP and +${mission.coinAward} Coins!")
                                            }
                                        }
                                    } else {
                                        notify("🔒 Mission not completed yet. Keep playing casual/tournament games!")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isComplete) RaivalPrimary else RaivalSurfaceLight),
                                border = if (isComplete) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(if (isComplete) "CLAIM" else "IN PROGRESS", color = if (isComplete) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class MissionDef(
    val id: String,
    val title: String,
    val category: String,
    val target: Int,
    val current: Int,
    val xpAward: Int,
    val coinAward: Int
)


// -----------------------------------------------------------------------------
// FEATURE 6: WEEKLY TOURNAMENTS
// -----------------------------------------------------------------------------
@Composable
fun WeeklyTournamentsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    val appConfigState by viewModel.appConfig.collectAsState()
    val isCoinOnly = (appConfigState?.economyMode ?: "Coin-Only") == "Coin-Only"

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏆 WEEKEND CHAMPIONS LEAGUE", fontWeight = FontWeight.Black, color = RaivalSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Every Friday to Sunday, competitive bracket tournaments with enormous prize pools are hosted. Standard bracket matching with live commentary and spectator mode enabled!", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RAIVAL COIN UTILITY CENTER", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Coins are virtual platform tokens earned through gameplay achievements. They represent competitive reputation and are exclusively used for custom match brackets and utility upgrades.",
                        color = RaivalTextSecondary,
                        fontSize = 12.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("💡 HOW TO USE YOUR COINS:", fontWeight = FontWeight.Bold, color = RaivalSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val utilities = listOf(
                        "🏆 Tournament Bracket Entry Fees",
                        "🛡️ Win-Streak Protection Shields",
                        "🌳 Skill Tree Masteries & Upgrades",
                        "🎨 Custom Gaming Names, Titles & Colors",
                        "🎬 Match Walkout & Entry VFX Styles",
                        "🔄 Daily Mission Reroll Tokens"
                    )
                    
                    utilities.forEach { utility ->
                        Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("•", color = RaivalSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
                            Text(utility, color = Color.White, fontSize = 12.sp)
                        }
                    }
                    
                    if (isCoinOnly) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RaivalPrimary.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("⚡", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "COIN-ONLY ECONOMY BOOST: Earn rewards 3x faster from missions and leveling to unlock upgrades instantly!",
                                    color = RaivalPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("WEEKEND SPECIAL SCHEDULES", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Weekend Grand Cup", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text("₵1,200 Pool", color = RaivalSecondary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    Text("Entry Fee: 150 Coins | Saturday 15:00 UTC", color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Join hundreds of elite managers in single elimination qualifiers. Match commentator: AI commentator live streaming.", color = RaivalTextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}


// -----------------------------------------------------------------------------
// FEATURE 7: STREAK PROTECTION
// -----------------------------------------------------------------------------
@Composable
fun StreakProtectionView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    val shieldsList = listOf(
        ShieldDef("basic", "Basic Streak Shield", "Protects 1 loss from breaking active win streak", 5, "Single use"),
        ShieldDef("gold", "Gold Streak Shield", "Protects 2 losses from breaking active win streak", 15, "Two uses"),
        ShieldDef("diamond", "Diamond Streak Shield", "Protects 3 losses from breaking active win streak", 25, "Three uses")
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🛡️ STREAK PROTECTION MECHANISMS", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Maintain high multipliers without losing streak ratings! Buy Shields using tournament Coins to absorb competitive match defeats.", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Current Streak Shields Active: ${user.streakShields} Protection Shields Available!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Text("BUY STREAK PROTECTION COVERS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        items(shieldsList, key = { it.id }) { shield ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).background(RaivalPrimary.copy(alpha = 0.15f), shape = CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(shield.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text(shield.desc, fontSize = 11.sp, color = RaivalTextSecondary)
                        Text(shield.benefit, fontSize = 11.sp, color = RaivalSecondary, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            viewModel.purchaseStreakShield(shield.cost) { success ->
                                if (success) {
                                    notify("🛡️ Successfully purchased ${shield.name}! Your streak is safe.")
                                } else {
                                    notify("❌ Insufficient coins! Complete more daily quests.")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                    ) {
                        Text("${shield.cost} Coins", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Streak Insurance & Recovery", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Did you lose your streak? Pay 20 Coins to reclaiming past streaks within 24 hours of rupture!", color = RaivalTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.spendCoins(20) { success ->
                                if (success) {
                                    notify("🎉 Win streak recovered successfully! Check your stats on the profile card!")
                                } else {
                                    notify("❌ Insufficient coins!")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary)
                    ) {
                        Text("Reclaim Win Streak (20 Coins)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class ShieldDef(
    val id: String,
    val name: String,
    val desc: String,
    val cost: Int,
    val benefit: String
)


// -----------------------------------------------------------------------------
// FEATURE 8: TRAINING MODE
// -----------------------------------------------------------------------------
@Composable
fun TrainingModeView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var difficulty by remember { mutableStateOf("Beginner") }
    var trainingReport by remember { mutableStateOf<String?>(null) }

    val levels = listOf("Beginner", "Amateur", "Pro", "Expert", "Legendary", "Ultimate")

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🤖 PRACTICE TRAINING ARENA", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Hone your tactical setup against advanced AI engines. Training mode carries zero coin risk and awards experience training points!", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Text("SELECT AI DIFFICULTY", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                levels.take(3).forEach { lvl ->
                    Card(
                        modifier = Modifier.weight(1f).clickable { difficulty = lvl },
                        colors = CardDefaults.cardColors(containerColor = if (difficulty == lvl) RaivalPrimary.copy(alpha = 0.15f) else RaivalSurfaceLight),
                        border = BorderStroke(1.dp, if (difficulty == lvl) RaivalPrimary else Color.Transparent)
                    ) {
                        Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                            Text(lvl, color = if (difficulty == lvl) RaivalPrimary else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                levels.drop(3).forEach { lvl ->
                    Card(
                        modifier = Modifier.weight(1f).clickable { difficulty = lvl },
                        colors = CardDefaults.cardColors(containerColor = if (difficulty == lvl) RaivalSecondary.copy(alpha = 0.15f) else RaivalSurfaceLight),
                        border = BorderStroke(1.dp, if (difficulty == lvl) RaivalSecondary else Color.Transparent)
                    ) {
                        Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                            Text(lvl, color = if (difficulty == lvl) RaivalSecondary else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.addXp(50)
                    val r = Random()
                    val matchGoals = r.nextInt(4)
                    val aiGoals = if (difficulty == "Beginner" || difficulty == "Amateur") r.nextInt(2) else r.nextInt(5)
                    trainingReport = """
                        📊 CODM / DLS PRACTICE MATCH COMPLETED
                        -------------------------------------------------
                        Opponent Difficulty: $difficulty
                        Final Score: You $matchGoals - $aiGoals AI
                        Total Ball Possession: ${50 + r.nextInt(15)}%
                        Passing Accuracy: ${70 + r.nextInt(25)}%
                        Shots On Target: ${matchGoals + r.nextInt(5)} shots
                        
                        💡 PRO COACH FEEDBACK: 
                        Your defensive spacing is excellent, but try utilizing fast Tiki-Taka build-ups to break high-pressing AI systems.
                    """.trimIndent()
                    notify("🤖 Practice match completed! Awarded +50 Training XP!")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
            ) {
                Text("LAUNCH PRACTICE DRILL", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }

        if (trainingReport != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("COACH REPORT", fontWeight = FontWeight.Bold, color = RaivalSecondary, fontSize = 12.sp)
                            IconButton(onClick = { trainingReport = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = trainingReport ?: "",
                            color = Color.Green,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}


// -----------------------------------------------------------------------------
// FEATURE 9: PRO TIPS & TUTORIALS
// -----------------------------------------------------------------------------
@Composable
fun ProTipsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val bookmarks = remember { mutableStateListOf<String>() }

    val guides = listOf(
        GuideDef("g1", "DLS Tiki-Taka Mastery", "⚔️ Strategy", "Learn to unlock defensive channels with rapid quick passing combinations.", "5 mins"),
        GuideDef("g2", "Ultimate Corner Kick Guide", "⚽ Set Pieces", "Unleash curl multipliers and score corner goals directly.", "4 mins"),
        GuideDef("g3", "Optimal eFootball Defending", "🛡️ Defense", "How to press strategically without breaking your defense line.", "6 mins"),
        GuideDef("g4", "High Speed Wing Overlaps", "⚡ Attacking", "Exploit wing-backs to cross high accuracy assists.", "3 mins")
    )

    val filtered = guides.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 PRO GAMING LIBRARY", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("In-depth guides compiled by regional esports champion managers. Gain tactical supremacy!", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search tactics & guides") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RaivalPrimary) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedLabelColor = RaivalPrimary,
                    focusedIndicatorColor = RaivalPrimary
                )
            )
        }

        items(filtered, key = { it.id }) { guide ->
            val isBookmarked = bookmarks.contains(guide.id)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.background(RaivalSecondary.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(guide.category.uppercase(), color = RaivalSecondary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        
                        IconButton(onClick = {
                            if (isBookmarked) {
                                bookmarks.remove(guide.id)
                                notify("Removed bookmark for '${guide.title}'")
                            } else {
                                bookmarks.add(guide.id)
                                notify("Guide '${guide.title}' bookmarked safely!")
                            }
                        }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) RaivalPrimary else Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(guide.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Text(guide.desc, color = RaivalTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Read Time: ${guide.time}", color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class GuideDef(
    val id: String,
    val title: String,
    val category: String,
    val desc: String,
    val time: String
)


// -----------------------------------------------------------------------------
// FEATURE 10: REFERRAL CHAIN SYSTEM
// -----------------------------------------------------------------------------
@Composable
fun ReferralChainView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var enteredCode by remember { mutableStateOf("") }
    val refCode = if (user.referralCode.isEmpty()) "SK-REF${user.id + 721}" else user.referralCode

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔗 VIRAL REFERRAL CHAIN", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Invite friends to earn massive coin multipliers! Raival operates a 4-Tier referral engine for maximum growth.", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("YOUR UNIQUE REFERRAL CODE", fontSize = 12.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = refCode,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Monospace,
                            color = RaivalSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.setReferralCode(refCode) {
                                notify("📋 Referral code copied to clipboard! Share with your friends!")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                    ) {
                        Text("SHARE CODE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SUBMIT FRIEND'S CODE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Did a friend invite you? Submit their code below to unlock 20 Coins instantly!", color = RaivalTextSecondary, fontSize = 11.sp)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredCode,
                        onValueChange = { enteredCode = it },
                        label = { Text("Enter Code (e.g. SK-REF789)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLabelColor = RaivalPrimary,
                            focusedIndicatorColor = RaivalPrimary
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.submitFriendReferral(enteredCode) { success, msg ->
                                notify(msg)
                                if (success) enteredCode = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                    ) {
                        Text("SUBMIT REFERRAL", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("VIRAL MULTI-LEVEL MULTIPLIERS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Level 1 (Direct Referral)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text("+20 Coins each", color = RaivalPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Level 2 (Secondary Referral)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text("+10 Coins each", color = RaivalSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Level 3 (Tertiary Referral)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text("+5 Coins each", color = RaivalTextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Level 4 (Quaternary Referral)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text("+2 Coins each", color = RaivalTextSecondary.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// =============================================================================
// BATCH 1: FEATURES 31 - 33
// =============================================================================

// FEATURE 31: WELCOME BONUS & GUIDED TUTORIAL
@Composable
fun WelcomeBonusView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var showPopup by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(0) }
    val tutorials = listOf(
        "👋 Welcome! Raival rewards new players to help start matchmaking immediately.",
        "🎮 Matchmaking: Join competitive brackets to earn professional ratings and rewards.",
        "🪙 Custom Upgrades: Use virtual tournament coins to unlock skill nodes, custom names, and themes."
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.4f))) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎁 NEW PLAYER WELCOME PACKAGE", fontWeight = FontWeight.Black, color = Color(0xFFFFD700), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Get a massive starter package! Earn free coins, skin, double XP & starter badges.", textAlign = TextAlign.Center, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (user.welcomeBonusClaimed) {
                        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFD700).copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                            Text("✅ WELCOME PACKAGE CLAIMED", color = Color(0xFFFFD700), fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.claimWelcomeBonusPackage { success, msg ->
                                    notify(msg)
                                    if (success) showPopup = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                        ) {
                            Text("CLAIM FREE WELCOME BUNDLE (20 Coins)", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        item {
            Text("FIRST WEEK CALENDAR PROGRESS TRACKER", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        item {
            val weekDays = listOf(Pair(1, 20), Pair(2, 10), Pair(3, 10), Pair(4, 5), Pair(5, 5), Pair(6, 5), Pair(7, 10))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weekDays.forEach { (day, coins) ->
                    val claimed = user.completedMissions.split(",").contains("day_${day}_bonus")
                    Card(
                        modifier = Modifier.width(80.dp).clickable {
                            viewModel.claimFirstWeekBonus(day, coins) { _, msg -> notify(msg) }
                        },
                        colors = CardDefaults.cardColors(containerColor = if (claimed) Color.Black else RaivalSurfaceLight),
                        border = BorderStroke(1.dp, if (claimed) Color.White.copy(alpha = 0.1f) else Color(0xFFFFD700).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DAY $day", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (claimed) RaivalTextSecondary else Color(0xFFFFD700))
                            Text("🎁", fontSize = 16.sp)
                            Text("$coins Coins", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (claimed) "CLAIMED" else "CLAIM", fontSize = 9.sp, fontWeight = FontWeight.Black, color = RaivalSecondary)
                        }
                    }
                }
            }
        }

        item {
            Text("FIRST MATCH BONUS CHALLENGES", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FirstMatchRow("Win First Match (+25 Coins)", "win", 25, user, viewModel, notify)
                    FirstMatchRow("Play First Match (+10 Coins)", "play", 10, user, viewModel, notify)
                    FirstMatchRow("Score First Goal (+5 Coins)", "goal", 5, user, viewModel, notify)
                    FirstMatchRow("Complete First Tournament (+50 Coins)", "tournament", 50, user, viewModel, notify)
                }
            }
        }

        item {
            Button(
                onClick = { currentStep = 0; showPopup = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary)
            ) {
                Text("LAUNCH GUIDED APP TUTORIAL TOUR", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showPopup) {
        Dialog(onDismissRequest = { showPopup = false }) {
            Card(modifier = Modifier.padding(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F1A)), border = BorderStroke(2.dp, Color(0xFFFFD700))) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌟 RAIVAL GUIDED TOUR 🌟", fontWeight = FontWeight.Black, color = Color(0xFFFFD700))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(tutorials[currentStep], textAlign = TextAlign.Center, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { showPopup = false }) { Text("Skip", color = RaivalTextSecondary) }
                        Button(
                            onClick = {
                                if (currentStep < tutorials.size - 1) currentStep++
                                else { showPopup = false; notify("🎓 Tutorial complete!") }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                        ) {
                            Text(if (currentStep == tutorials.size - 1) "Finish" else "Next", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FirstMatchRow(title: String, type: String, amt: Int, user: User, viewModel: RaivalViewModel, notify: (String) -> Unit) {
    val claimed = user.completedMissions.split(",").contains("first_match_$type")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Button(
            onClick = { viewModel.claimFirstMatchBonus(type, amt) { _, msg -> notify(msg) } },
            colors = ButtonDefaults.buttonColors(containerColor = if (claimed) Color.Black else RaivalPrimary),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(if (claimed) "CLAIMED" else "CLAIM", fontSize = 11.sp, color = if (claimed) RaivalTextSecondary else Color.Black)
        }
    }
}

// FEATURE 32: SOCIAL LOGIN REWARDS
@Composable
fun SocialLoginRewardsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var shareCount by remember { mutableIntStateOf(0) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFF1DA1F2).copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔗 LINK ACCOUNTS FOR REWARDS", fontWeight = FontWeight.Bold, color = Color(0xFF1DA1F2))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Secure accounts and earn 5 DLS Coins for linking social login managers.", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item { SocialPlatformItem("Facebook", user.socialFbConnected, viewModel, notify) }
        item { SocialPlatformItem("Twitter / X", user.socialTwConnected, viewModel, notify) }
        item { SocialPlatformItem("Apple ID", user.socialAppleConnected, viewModel, notify) }

        item {
            Text("DAILY SOCIAL SHARING ACTIONS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SocialShareRow("Share Match Result (+5 Coins)", shareCount, 3) {
                        if (shareCount < 3) {
                            viewModel.shareSocialActionReward("Share Match Result", 5) { success, msg ->
                                notify(msg)
                                if (success) shareCount++
                            }
                        } else notify("❌ Daily limit reached!")
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SocialShareRow("Share Tournament Win (+10 Coins)", shareCount, 2) {
                        viewModel.shareSocialActionReward("Tournament Win", 10) { _, msg -> notify(msg) }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (user.socialFbConnected || user.socialTwConnected || user.socialAppleConnected) {
                        notify("📸 Imported social profile picture and name details successfully!")
                    } else notify("❌ Link any social account first to auto-fill details!")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary)
            ) {
                Text("Auto-Import Profile Picture & Info", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SocialPlatformItem(platform: String, connected: Boolean, viewModel: RaivalViewModel, notify: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(platform, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text(if (connected) "Connected (Verified)" else "Unlinked (Earn 5 Coins)", color = if (connected) Color.Green else RaivalTextSecondary, fontSize = 11.sp)
            }
            Button(
                onClick = {
                    if (connected) viewModel.disconnectSocialAccount(platform) { _, m -> notify(m) }
                    else viewModel.connectSocialAccount(platform) { _, m -> notify(m) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (connected) Color.Black else RaivalPrimary)
            ) {
                Text(if (connected) "DISCONNECT" else "LINK", color = if (connected) Color.White else Color.Black)
            }
        }
    }
}

@Composable
fun SocialShareRow(title: String, current: Int, limit: Int, onShare: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Daily tracker: $current / $limit", color = RaivalTextSecondary, fontSize = 11.sp)
        }
        Button(onClick = onShare, colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary)) {
            Text("SHARE", color = Color.Black, fontSize = 11.sp)
        }
    }
}

// FEATURE 33: APP STORE RATING REWARDS
@Composable
fun AppStoreRatingRewardsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var stars by remember { mutableStateOf(5) }
    var screenshotUploaded by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⭐ RATE RAIVAL & EARN REWARDS", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Provide reviews and submit screenshots to earn extra tournament coins! Rating popups trigger after match wins.", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Choose Stars Rating", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(5) { i ->
                            Text(
                                "★",
                                fontSize = 36.sp,
                                color = if (i < stars) Color(0xFFFF9800) else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { stars = i + 1 }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.rateAppStoreRating(stars) { _, m -> notify(m) } },
                        colors = ButtonDefaults.buttonColors(containerColor = if (user.appRated) Color.Black else Color(0xFFFF9800)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (user.appRated) "COMPLETED" else "SUBMIT RATING (+10 Coins)", color = if (user.appRated) Color.White else Color.Black)
                    }
                }
            }
        }

        item {
            Text("WRITE STORE REVIEW", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Leave a real review on the Google Play Store to help us improve! Your feedback matters.",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("market://details?id=${context.packageName}")
                                    setPackage("com.android.vending")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                            notify("⭐ Opening Play Store for review. Thank you for your feedback!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OPEN PLAY STORE FOR REVIEW", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("SUBMIT PROOF SCREENSHOT (+10 COINS)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val reviewContext = androidx.compose.ui.platform.LocalContext.current
                    if (screenshotUploaded) {
                        Text("✅ Play Store Review Opened!", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                    Button(
                        onClick = {
                            screenshotUploaded = true
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("market://details?id=${reviewContext.packageName}")
                                )
                                reviewContext.startActivity(intent)
                                notify("📸 Opening Play Store for review. Thank you for your feedback!")
                            } catch (e: Exception) {
                                notify("Could not open Play Store: ${e.localizedMessage}")
                            }
                        },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary)
                        ) {
                            Text("OPEN PLAY STORE TO REVIEW", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// BATCH 2: FEATURES 34 - 37
// =============================================================================

// FEATURE 34: REFERRAL TOURNAMENTS
@Composable
fun ReferralTournamentView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    val qualified = user.referredActiveCount >= 1

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, if (qualified) Color.Green.copy(alpha = 0.3f) else Color(0xFFE91E63).copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏆 ELITE REFERRAL TOURNAMENT", fontWeight = FontWeight.Black, color = Color(0xFFE91E63))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Free weekend brackets open exclusively to players with active referrals. Win 300 DLS Coins, golden referrer badges, and unlock elite masteries!", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ACTIVE REFERRAL CHECKER", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${user.referredActiveCount} Active Referrals verified", fontSize = 15.sp, fontWeight = FontWeight.Black, color = RaivalSecondary)
                    Text(if (qualified) "🎉 You qualify! Access tournament free." else "🔒 Lock: You must invite at least 1 active friend to qualify.", color = if (qualified) Color.Green else RaivalTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.claimReferralReward { _, msg -> notify(msg) } },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                    ) {
                        Text("CLAIM REFERRAL REWARD (+50 Coins)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("PRIZE POOL DETAILS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• 1st Place: 300 Coins + Golden Referrer Badge", color = Color.White, fontSize = 12.sp)
                    Text("• 2nd Place: 150 Coins + Silver Referrer Badge", color = Color.White, fontSize = 12.sp)
                    Text("• 3rd Place: 75 Coins + Bronze Referrer Badge", color = Color.White, fontSize = 12.sp)
                    Text("• Participation: 25 Coins + Referrer Badge", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

// FEATURE 35: CUSTOM PLAYER AVATARS
@Composable
fun CustomAvatarsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var customAvatar by remember { mutableStateOf<String?>(null) }
    val unlocked = remember(user.unlockedAvatars) { user.unlockedAvatars.split(",").toSet() }

    val options = listOf(
        AvatarItem("avatar_default", "Default Striker", "⚽", 0, "Free starter template", "COINS"),
        AvatarItem("avatar_samurai", "Cyber Samurai", "🥷", 50, "Cyberpunk glowing helmet", "COINS"),
        AvatarItem("avatar_coach", "Pixel Coach", "🧠", 75, "Tactical manager retro avatar", "XP"),
        AvatarItem("avatar_gold", "Tiki-Taka King", "👑", 150, "Exclusive gold plated crown", "RAIVAL"),
        AvatarItem("avatar_sniper", "Elite Sniper", "🎯", 80, "Bullseye expert precision gamer", "COINS"),
        AvatarItem("avatar_cadet", "Galactic Cadet", "🧑‍🚀", 100, "Interstellar soccer cadet", "XP"),
        AvatarItem("avatar_gold_boot", "Golden Boot", "🏆", 200, "Ghana championship legendary status", "RAIVAL"),
        AvatarItem("avatar_phoenix", "Phoenix Rising", "🐦", 120, "Legendary competitive spirit", "COINS"),
        AvatarItem("avatar_ninja", "Ninja Striker", "⚔️", 150, "Stealth play and swift moves", "XP"),
        AvatarItem("avatar_tiger", "Tiger Master", "🐯", 80, "Fierce competitive instinct", "RAIVAL"),
        AvatarItem("avatar_wizard", "Wizard Playmaker", "🧙", 180, "Tactical spellcaster & playmaker", "COINS"),
        AvatarItem("avatar_electro", "Electro Gamer", "⚡", 250, "Ultra-glowing power-boost profile", "XP")
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // BALANCES SUMMARY CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "💳 YOUR CURRENCY BALANCES",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Coins
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🪙", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("COINS", color = RaivalTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("${user.coinBalance}", color = RaivalPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(RaivalSurfaceLight))
                        // XP Points
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📈", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("XP POINTS", color = RaivalTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("${user.xp} XP", color = RaivalSecondary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(RaivalSurfaceLight))
                        // Raival Points
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💎", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("RAIVAL PTS", color = RaivalTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("${user.raivalPoints}", color = RaivalAccent, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // ACTIVE AVATAR PREVIEW
        item {
            val currentAvObj = options.find { it.id == user.selectedAvatar }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(60.dp).background(RaivalSecondary.copy(alpha = 0.15f), shape = CircleShape), contentAlignment = Alignment.Center) {
                        Text(customAvatar ?: currentAvObj?.emoji ?: "⚽", fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("YOUR ACTIVE AVATAR", fontWeight = FontWeight.Black, color = RaivalSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
                        Text(if (customAvatar != null) "Custom Photo" else currentAvObj?.name ?: "Default Striker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(if (customAvatar != null) "Uploaded via Device Gallery" else currentAvObj?.desc ?: "Starter profile theme template", color = RaivalTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // GALLERY PHOTO UPLOAD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                border = BorderStroke(1.dp, RaivalSurfaceLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Upload personalized avatar photo from device gallery", color = RaivalTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            customAvatar = "😎"
                            notify("📸 Gallery photo crop & upload successful!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📸", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UPLOAD PHOTO FROM GALLERY", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // MARKETPLACE SECTION
        item {
            Text("AVATAR TEMPLATES MARKET", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp), letterSpacing = 0.5.sp)
        }

        items(options.size, key = { index -> options[index].id }) { index ->
            val av = options[index]
            val unlockedAv = av.id == "avatar_default" || unlocked.contains(av.id)
            val isSelected = user.selectedAvatar == av.id && customAvatar == null
            
            val costColor = when (av.costType) {
                "COINS" -> RaivalPrimary
                "XP" -> RaivalSecondary
                "RAIVAL" -> RaivalAccent
                else -> Color.White
            }

            val costUnitSymbol = when (av.costType) {
                "COINS" -> "🪙"
                "XP" -> "📈"
                "RAIVAL" -> "💎"
                else -> ""
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = if (isSelected) BorderStroke(1.5.dp, RaivalSecondary) else BorderStroke(1.dp, RaivalSurfaceLight)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.05f), shape = CircleShape), contentAlignment = Alignment.Center) {
                            Text(av.emoji, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(av.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ACTIVE", color = RaivalSecondary, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.background(RaivalSecondary.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Text(av.desc, fontSize = 11.sp, color = RaivalTextSecondary, maxLines = 1)
                            
                            // Show cost info explicitly
                            if (!unlockedAv) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    Text(costUnitSymbol, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${av.cost} ${av.costType}",
                                        color = costColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text("Unlocked", color = RaivalBrightLime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    if (unlockedAv) {
                        Button(
                            onClick = {
                                viewModel.selectCustomAvatar(av.id) {
                                    customAvatar = null
                                    notify("Equipped avatar ${av.name}!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color.Black else RaivalSecondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text(if (isSelected) "EQUIPPED" else "EQUIP", color = if (isSelected) Color.White else Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.purchaseCustomAvatar(av.id, av.cost, av.costType) { success, m ->
                                    notify(m)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = costColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("BUY ${av.cost} $costUnitSymbol", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

// FEATURE 36: CUSTOM GAMING NAMES
@Composable
fun CustomGamingNamesView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var editName by remember { mutableStateOf(user.username) }
    var editClan by remember { mutableStateOf(user.clanTag) }
    var selectedTitle by remember { mutableStateOf(user.customTitle) }
    var selectedColor by remember { mutableStateOf(user.nameColor) }
    var glowOn by remember { mutableStateOf(user.nameGlow) }

    val colorsList = listOf(Pair("Gold", "#F4B400"), Pair("Pink", "#FF007F"), Pair("Blue", "#00E5FF"), Pair("Acid", "#39FF14"))
    val titlesList = listOf("", "Pro", "King", "Legend")

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎮 NAME REBRAND LIVE PREVIEW", fontWeight = FontWeight.Bold, color = RaivalSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (editClan.isNotEmpty()) {
                            Text("[$editClan] ", color = RaivalSecondary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        val dCol = if (selectedColor.isNotEmpty()) Color(android.graphics.Color.parseColor(selectedColor)) else Color.White
                        Text(editName, color = dCol, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        if (selectedTitle.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.background(Color(0xFF9C27B0).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Text(selectedTitle, color = Color(0xFF9C27B0), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Unique Gaming Username") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    OutlinedTextField(
                        value = editClan,
                        onValueChange = { editClan = it },
                        label = { Text("Clan Tag (e.g. GH - Free)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Title (+50 Coins)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        titlesList.forEach { title ->
                            Card(
                                modifier = Modifier.weight(1f).clickable { selectedTitle = title },
                                colors = CardDefaults.cardColors(containerColor = if (selectedTitle == title) Color(0xFF9C27B0).copy(alpha = 0.2f) else Color.Black)
                            ) {
                                Box(modifier = Modifier.padding(6.dp), contentAlignment = Alignment.Center) {
                                    Text(if (title.isEmpty()) "None" else title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Text("Select Name Color (+25 Coins)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        colorsList.forEach { (name, hex) ->
                            val parseCol = Color(android.graphics.Color.parseColor(hex))
                            Card(
                                modifier = Modifier.weight(1f).clickable { selectedColor = hex },
                                colors = CardDefaults.cardColors(containerColor = if (selectedColor == hex) parseCol.copy(alpha = 0.15f) else Color.Black)
                            ) {
                                Box(modifier = Modifier.padding(6.dp), contentAlignment = Alignment.Center) {
                                    Text(name, color = parseCol, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Name Glow Effect (+75 Coins)", color = Color.White, fontSize = 12.sp)
                        Switch(checked = glowOn, onCheckedChange = { glowOn = it })
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    var cost = 25
                    if (selectedTitle != user.customTitle && selectedTitle.isNotEmpty()) cost += 50
                    if (selectedColor != user.nameColor && selectedColor.isNotEmpty()) cost += 25
                    if (glowOn != user.nameGlow && glowOn) cost += 75
                    viewModel.changeGamingName(editName, selectedTitle, editClan, selectedColor, glowOn, cost) { _, msg -> notify(msg) }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
            ) {
                Text("APPLY COGNOMEN CHANGES (Cost: 25+ Coins)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// FEATURE 37: APP THEME SETTINGS
@Composable
fun ThemeSettingsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    val currentTheme = user.profileTheme

    val options = listOf(
        Triple("default", "Follow Android System Default", "Automatically match your phone's dark or light theme settings."),
        Triple("light", "Light Mode", "Enjoy a clean, high-contrast bright theme experience."),
        Triple("dark", "Dark Mode", "Immersion gaming vibe with a deep dark aesthetic.")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌓 THEME PREVIEW PANEL",
                        fontWeight = FontWeight.Bold,
                        color = RaivalPrimary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(RaivalSurface, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, RaivalTextSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Preview Arena UI",
                                color = RaivalTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isDarkTheme) "Currently in Dark Mode 🌑" else "Currently in Light Mode ☀️",
                                color = RaivalTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { opt ->
                    val isSelected = currentTheme == opt.first || (currentTheme == "default" && opt.first == "default")
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectProfileTheme(opt.first) {
                                    notify("Switched theme to ${opt.second}!")
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) RaivalPrimary.copy(alpha = 0.12f) else RaivalSurfaceLight
                        ),
                        border = if (isSelected) BorderStroke(2.dp, RaivalPrimary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = opt.second,
                                    fontWeight = FontWeight.Bold,
                                    color = RaivalTextPrimary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = opt.third,
                                    color = RaivalTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectProfileTheme(opt.first) {
                                        notify("Switched theme to ${opt.second}!")
                                    }
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = RaivalPrimary,
                                    unselectedColor = RaivalTextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// BATCH 3: FEATURES 38 - 40
// =============================================================================

// FEATURE 38: ANIMATED BADGES
@Composable
fun AnimatedBadgesView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    val transition = rememberInfiniteTransition(label = "badgePulse")
    
    val pulse by transition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )

    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "spin"
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFF03A9F4).copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✨ PREMIUM ANIMATED BADGES", fontWeight = FontWeight.Bold, color = Color(0xFF03A9F4))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Collect and show off animating badges on your user profile to stand out in the esports arena!", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(54.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }.background(Color(0xFFFFD700).copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🟢", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Pulsing Arena Master", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("Visual effect: Pulse scale loop (1.2s)", color = RaivalSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(54.dp).rotate(spin).background(Color(0xFF00ABCD).copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚙️", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Chronos Spin Grandmaster", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("Visual effect: Spin clockwise (4.0s loop)", color = RaivalSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// FEATURE 39: MATCH ENTRY ANIMATIONS
@Composable
fun MatchEntryAnimationsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var isSimulating by remember { mutableStateOf(false) }
    var selectedAnim by remember { mutableStateOf("default") }

    val anims = listOf(
        AnimEntryDef("default", "Standard Entry", "Simple and clean loading screen entry style", 0, "Free default template"),
        AnimEntryDef("stadium", "Stadium Floodlights", "Dynamic pre-match crowd cheers and spotlighting effects", 25, "Custom visualizers"),
        AnimEntryDef("walkout", "Team Roster Walkout", "Cinematic line-up walkout to build extreme pre-match tension", 50, "Lineup showcase")
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎬 CINEMATIC ENTRY SEQUENCES", fontWeight = FontWeight.Bold, color = Color(0xFFE040FB))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Set custom entrance animations to display before competitive tournaments start and intimidate opponent clubs!", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                anims.forEach { an ->
                    val active = selectedAnim == an.id
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, if (active) Color(0xFFE040FB) else Color.Transparent)) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(an.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text(an.desc, color = RaivalTextSecondary, fontSize = 11.sp)
                                Text(an.benefit, color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    selectedAnim = an.id
                                    notify("Equipped match entry animation: ${an.name}!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (active) Color.Black else RaivalSecondary)
                            ) {
                                Text(if (active) "ACTIVE" else "EQUIP", color = if (active) Color.White else Color.Black)
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { isSimulating = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB))
            ) {
                Text("TEST EQUIPPED ENTRY SIMULATION", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (isSimulating) {
        Dialog(onDismissRequest = { isSimulating = false }) {
            Card(modifier = Modifier.padding(24.dp), colors = CardDefaults.cardColors(containerColor = Color.Black), border = BorderStroke(2.dp, Color(0xFFE040FB))) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎮 ENTRANCE LOADING PREVIEW 🎮", fontWeight = FontWeight.Black, color = Color(0xFFE040FB))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("🏟️ ENTERING ACCRA SPORTS STADIUM...", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Roster style active: ${selectedAnim.uppercase()}", color = RaivalTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("🔊 AI COMMENTARY: \"Whistle blows! Managers clash for ultimate bracket glory in front of the roaring fans!\"", textAlign = TextAlign.Center, color = Color.White, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { isSimulating = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB))) {
                        Text("LAUNCH MATCH", color = Color.White)
                    }
                }
            }
        }
    }
}

// FEATURE 40: TOURNAMENT STORE & BULK DEALS
@Composable
fun TournamentStoreView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var category by remember { mutableStateOf("tickets") }

    val tickets = listOf(
        StoreBundle("t_1", "Single Entry Voucher", "🎫 1 Tournament Entry Ticket", 100, 0),
        StoreBundle("t_2", "Bronze 3-Pack Deal", "🎫 3 Entry Tickets", 270, 10),
        StoreBundle("t_3", "Gold 5-Pack Bulk Saver", "🎫 5 Entry Tickets", 425, 15)
    )

    val boosters = listOf(
        StoreBundle("b_1", "XP Booster Pack", "⚡ 2x XP multiplier for 24 hours", 150, 0),
        StoreBundle("b_2", "Coin Doubler Bundle", "💰 2x Coins for next 5 matches", 200, 10)
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFFFFEB3B).copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🛒 THE TOURNAMENT MARKETPLACE", fontWeight = FontWeight.Bold, color = Color(0xFFFFEB3B))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Buy entry ticket vouchers in bulk packages to maximize savings, or purchase experience rate boosters!", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { category = "tickets" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (category == "tickets") Color(0xFFFFEB3B) else RaivalSurfaceLight)) {
                    Text("Ticket Packs", color = if (category == "tickets") Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
                Button(onClick = { category = "boosters" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (category == "boosters") Color(0xFFFFEB3B) else RaivalSurfaceLight)) {
                    Text("XP Boosters", color = if (category == "boosters") Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            val activeList = if (category == "tickets") tickets else boosters
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                activeList.forEach { deal ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(deal.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                if (deal.discount > 0) {
                                    Box(modifier = Modifier.background(Color.Red.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("SAVE ${deal.discount}%", color = Color.Red, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            Text(deal.desc, color = RaivalTextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${deal.price} Coins", color = Color(0xFFFFD700), fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Button(
                                    onClick = {
                                        if (user.coinBalance >= deal.price) {
                                            viewModel.spendCoins(deal.price) { success ->
                                                if (success) notify("🛒 Purchased ${deal.name}!")
                                                else notify("❌ Insufficient coins!")
                                            }
                                        } else notify("❌ Insufficient coins!")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B))
                                ) {
                                    Text("BUY DEAL", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class StoreBundle(
    val id: String,
    val name: String,
    val desc: String,
    val price: Int,
    val discount: Int
)

// =============================================================================
// FEATURES 51-60 DETAILED IMPLEMENTATION
// =============================================================================

// FEATURE 51: CUSTOM NOTIFICATION TONES
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomNotificationTonesView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("raival_prefs", android.content.Context.MODE_PRIVATE) }

    var volume by remember { mutableFloatStateOf(sharedPrefs.getFloat("notification_volume", 0.8f)) }
    var muteAll by remember { mutableStateOf(sharedPrefs.getBoolean("mute_all", false)) }
    var customFileUploaded by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    
    val categories = remember {
        listOf(
            Triple("Match Found", "Opponent found", "Match found! chime"),
            Triple("Match Start", "Match begins", "Referee whistle"),
            Triple("Goal Scored", "Goal in your match", "Goal celebration horn"),
            Triple("Match Result", "Match ends", "Final whistle"),
            Triple("Tournament Update", "Tournament news", "Tournament fanfare"),
            Triple("Friend Request", "Someone adds you", "Friend request ping")
        )
    }
    
    var selectedTone by remember {
        mutableStateOf(
            categories.associate { cat ->
                cat.first to (sharedPrefs.getString("tone_${cat.first}", "Classic") ?: "Classic")
            }
        )
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎵 CUSTOM NOTIFICATION TONES", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Assign premium sound effects and custom audio clips to stay instantly informed of actions across the Raival Arena.", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GLOBAL CONTROLS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Mute All Sounds", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = muteAll,
                            onCheckedChange = { 
                                muteAll = it
                                sharedPrefs.edit().putBoolean("mute_all", it).apply()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Volume Level: ${(volume * 100).toInt()}%", color = RaivalTextSecondary, fontSize = 12.sp)
                    Slider(
                        value = volume,
                        onValueChange = { 
                            volume = it
                            sharedPrefs.edit().putFloat("notification_volume", it).apply()
                        },
                        enabled = !muteAll,
                        colors = SliderDefaults.colors(thumbColor = RaivalPrimary, activeTrackColor = RaivalPrimary)
                    )
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CUSTOM TONE UPLOAD (.MP3, .WAV)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Upload up to 5 seconds (Max 500KB) of custom victory celebration sound effects.", color = RaivalTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (customFileUploaded == null) {
                        Button(
                            onClick = {
                                isUploading = true
                            },
                            enabled = !isUploading,
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.Upload, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Audio File", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaivalSuccess, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(customFileUploaded ?: "", color = Color.White, fontSize = 13.sp)
                            }
                            IconButton(onClick = { customFileUploaded = null }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = RaivalError)
                            }
                        }
                    }
                }
            }
        }
        
        if (isUploading) {
            item {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1500)
                    customFileUploaded = "my_custom_whistle.mp3"
                    isUploading = false
                    notify("🎵 Custom sound uploaded and saved!")
                }
            }
        }
        
        item {
            Text("CATEGORY ASSIGNMENTS", fontWeight = FontWeight.Black, color = RaivalSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
        
        items(categories, key = { it.first }) { cat ->
            var expandedDropdown by remember { mutableStateOf(false) }
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(cat.first, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text(cat.second, color = RaivalTextSecondary, fontSize = 11.sp)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (muteAll) {
                                        notify("🔇 Sounds are muted globally!")
                                    } else {
                                        notify("🔊 Previewing: [${selectedTone[cat.first] ?: "Classic"}] tone for ${cat.first}!")
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = RaivalPrimary)
                            }
                            
                            Box {
                                Button(
                                    onClick = { expandedDropdown = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSurface),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(selectedTone[cat.first] ?: "Classic", fontSize = 12.sp, color = Color.White)
                                }
                                
                                DropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                                    listOf("Sports", "Gaming", "Music", "Classic", "Humorous", "Silent").forEach { tone ->
                                        DropdownMenuItem(
                                            text = { Text(tone) },
                                            onClick = {
                                                selectedTone = selectedTone.toMutableMap().apply { put(cat.first, tone) }
                                                sharedPrefs.edit().putString("tone_${cat.first}", tone).apply()
                                                expandedDropdown = false
                                                notify("🎵 Tone updated: ${cat.first} is now set to $tone")
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

// FEATURE 52: QUICK GAME SHORTCUTS
@Composable
fun QuickGameShortcutsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var smartShortcutsEnabled by remember { mutableStateOf(true) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var shortcutLabel by remember { mutableStateOf("Quick DLS 1v1") }
    var selectedIconIndex by remember { mutableIntStateOf(0) }
    
    val colors = listOf(RaivalPrimary, RaivalSecondary, Color(0xFF00D4FF), Color(0xFFFF5252), Color(0xFF4CAF50))
    val icons = listOf("🎮", "⭐", "🏆", "⚔️", "❤️")
    
    val shortcuts = remember {
        mutableStateListOf(
            Triple("Favorite Game", "🎮 Matchmaking Room", RaivalPrimary),
            Triple("Quick Match", "⚡ Quick Queue", RaivalSecondary),
            Triple("Current Tournament", "🏆 Active Brackets", Color(0xFF00D4FF)),
            Triple("Training Mode", "🤖 AI Practice Sandbox", Color(0xFF4CAF50))
        )
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚡ QUICK GAME SHORTCUTS", fontWeight = FontWeight.Black, color = RaivalSecondary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Pin widgets and customize quick-launch buttons directly to your phone's home screen or the app dock for single-tap matchmaking.", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CREATE NEW HOMESCREEN WIDGET SHORTCUT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = shortcutLabel,
                        onValueChange = { shortcutLabel = it },
                        label = { Text("Shortcut Label") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Background Color Accent:", color = RaivalTextSecondary, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        colors.forEachIndexed { index, color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = CircleShape)
                                    .border(
                                        width = if (selectedColorIndex == index) 3.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorIndex = index }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Shortcut Symbol:", color = RaivalTextSecondary, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        icons.forEachIndexed { index, icon ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (selectedIconIndex == index) RaivalPrimary.copy(alpha = 0.3f) else RaivalSurface, shape = RoundedCornerShape(8.dp))
                                    .border(width = if (selectedIconIndex == index) 2.dp else 1.dp, color = if (selectedIconIndex == index) RaivalPrimary else RaivalSurfaceLight, shape = RoundedCornerShape(8.dp))
                                    .clickable { selectedIconIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(icon, fontSize = 18.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (shortcutLabel.isNotEmpty()) {
                                shortcuts.add(Triple(shortcutLabel, "Custom Launcher Shortcut", colors[selectedColorIndex]))
                                notify("⚡ Shortcut '$shortcutLabel' pinned to launcher successfully!")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("PIN SHORTCUT", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("SMART AUTO-SHORTCUTS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Let AI place shortcuts contextually based on your behavior", color = RaivalTextSecondary, fontSize = 10.sp)
                        }
                        Switch(checked = smartShortcutsEnabled, onCheckedChange = { smartShortcutsEnabled = it })
                    }
                }
            }
        }
        
        item {
            Text("ACTIVE PINNED SHORTCUTS & ORDERING", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
        
        items(shortcuts, key = { it.first }) { sc ->
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).background(sc.third.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                        Text("⚡", color = sc.third, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sc.first, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text(sc.second, color = RaivalTextSecondary, fontSize = 11.sp)
                    }
                    
                    IconButton(onClick = {
                        val index = shortcuts.indexOf(sc)
                        if (index > 0) {
                            val temp = shortcuts[index]
                            shortcuts[index] = shortcuts[index - 1]
                            shortcuts[index - 1] = temp
                            notify("Reordered shortcuts up")
                        }
                    }) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = {
                        shortcuts.remove(sc)
                        notify("Removed shortcut!")
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RaivalError, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// FEATURE 53: MATCH ARCHIVE
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchArchiveView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedMatchForDetail by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var customNotes by remember { mutableStateOf("") }
    
    val pastMatches = remember {
        listOf(
            Triple("eFootball Weekly Showcase", "Kofi_Striker", "WIN 3-1"),
            Triple("DLS Arena Accra Cup", "Akwasi_Gamer", "LOSS 0-2"),
            Triple("CODM Squad Battle", "Yaw_Dribbler", "WIN 24-18"),
            Triple("Casual WordSearch Duel", "Kojo_Coder", "WIN 420-380"),
            Triple("DLS Daily Sprint", "Ghana_King", "WIN 4-1"),
            Triple("DLS Elite League", "Ama_Pace", "LOSS 1-3")
        )
    }
    
    if (selectedMatchForDetail != null) {
        Dialog(onDismissRequest = { selectedMatchForDetail = null }) {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurface), border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                    Text("🗄️ MATCH HISTORIC ARCHIVE", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(selectedMatchForDetail?.first ?: "", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("Opponent: ${selectedMatchForDetail?.second ?: ""}", color = RaivalTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth().background(RaivalSurfaceLight, shape = RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Column {
                            Text("RESULT DETAILS", fontWeight = FontWeight.Bold, color = RaivalSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(selectedMatchForDetail?.third ?: "", fontWeight = FontWeight.Black, color = if ((selectedMatchForDetail?.third ?: "").contains("WIN")) RaivalSuccess else RaivalError, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("⚽ Possession: 58% vs 42%\n🥅 Shots: 12 (8 on target) vs 6 (2 on target)\n🛡️ Tackle accuracy: 78% vs 64%", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("PERSONAL MATCH NOTES", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customNotes,
                        onValueChange = { customNotes = it },
                        placeholder = { Text("Write strategy notes, opponent tactics or key weaknesses observed...", fontSize = 11.sp, color = RaivalTextSecondary) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { selectedMatchForDetail = null }) {
                            Text("Close", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            notify("📝 Notes saved to match archive!")
                            selectedMatchForDetail = null
                        }, colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)) {
                            Text("SAVE NOTES", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🗄️ COMPREHENSIVE MATCH ARCHIVE", fontWeight = FontWeight.Black, color = Color(0xFF00E676), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Deep-dive historical analyzer containing records of all past official tournaments and friendly scrimmages.", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ARCHIVE STATISTICS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("Win Rate", "66%", RaivalSuccess),
                            Triple("Matches", "${user.wins + user.losses} Total", Color.White),
                            Triple("Goals Avg", "2.8 / game", RaivalSecondary)
                        ).forEach { stat ->
                            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = RaivalSurface)) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stat.first, fontSize = 9.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(stat.second, fontSize = 12.sp, color = stat.third, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Opponent...", fontSize = 12.sp, color = RaivalTextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RaivalPrimary) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(focusedContainerColor = RaivalSurfaceLight, unfocusedContainerColor = RaivalSurfaceLight)
                )
                
                Box {
                    var filterExpanded by remember { mutableStateOf(false) }
                    Button(
                        onClick = { filterExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(selectedFilter, color = Color.White, fontSize = 12.sp)
                    }
                    
                    DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                        listOf("All", "Wins Only", "Losses Only").forEach { f ->
                            DropdownMenuItem(
                                text = { Text(f) },
                                onClick = {
                                    selectedFilter = f
                                    filterExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        val filteredList = pastMatches.filter { match ->
            val matchesSearch = match.second.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Wins Only" -> match.third.contains("WIN")
                "Losses Only" -> match.third.contains("LOSS")
                else -> true
            }
            matchesSearch && matchesFilter
        }
        
        items(filteredList, key = { "${it.first}_${it.second}" }) { match ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { selectedMatchForDetail = match },
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (match.third.contains("WIN")) RaivalSuccess.copy(alpha = 0.15f) else RaivalError.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (match.third.contains("WIN")) "W" else "L",
                                color = if (match.third.contains("WIN")) RaivalSuccess else RaivalError,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(match.second, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text(match.first, color = RaivalTextSecondary, fontSize = 11.sp)
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(match.third, fontWeight = FontWeight.Black, color = if (match.third.contains("WIN")) RaivalSuccess else RaivalError, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RaivalTextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

data class ProtectedAction(
    val title: String,
    val desc: String,
    val isLocked: Boolean,
    val onToggle: (Boolean) -> Unit
)

// FEATURE 54: "ARE YOU SURE?" PROTECTION
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreYouSureProtectionView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var spendCoinsLocked by remember { mutableStateOf(true) }
    var enterTournamentLocked by remember { mutableStateOf(true) }
    var withdrawLocked by remember { mutableStateOf(true) }
    var leaveLocked by remember { mutableStateOf(true) }
    
    var selectedProtectionType by remember { mutableStateOf("PIN Confirm") }
    var userEnteredPin by remember { mutableStateOf("") }
    var activeTestDialog by remember { mutableStateOf<String?>(null) }
    var delayProgress by remember { mutableFloatStateOf(0.0f) }
    var isHolding by remember { mutableStateOf(false) }
    
    if (activeTestDialog != null) {
        Dialog(onDismissRequest = { activeTestDialog = null }) {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurface), border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🛡️ SAFETY PROTECTION GUARD", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (selectedProtectionType) {
                        "Basic" -> {
                            Text("Confirm action: You are about to initiate [${activeTestDialog ?: ""}]. Proceed?", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { activeTestDialog = null }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight)) {
                                    Text("NO", color = Color.White)
                                }
                                Button(onClick = {
                                    notify("✅ Action '${activeTestDialog ?: ""}' approved and completed!")
                                    activeTestDialog = null
                                }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = RaivalSuccess)) {
                                    Text("YES, CONFIRM", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        "Double Confirm" -> {
                            var currentStep by remember { mutableIntStateOf(1) }
                            if (currentStep == 1) {
                                Text("Step 1 of 2: Confirm you wish to proceed with [${activeTestDialog ?: ""}].", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = { currentStep = 2 }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)) {
                                    Text("CONTINUE", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("🔥 CRITICAL WARNING: This action is completely permanent and cannot be undone. Are you absolutely certain?", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(onClick = { activeTestDialog = null }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight)) {
                                        Text("ABORT", color = Color.White)
                                    }
                                    Button(onClick = {
                                        notify("✅ Action '${activeTestDialog ?: ""}' fully finalized!")
                                        activeTestDialog = null
                                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = RaivalSuccess)) {
                                        Text("FORCE FINAL", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        "PIN Confirm" -> {
                            Text("Please enter your 4-Digit Protection PIN:", color = Color.White, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = userEnteredPin,
                                onValueChange = { if (it.length <= 4) userEnteredPin = it },
                                placeholder = { Text("xxxx") },
                                modifier = Modifier.width(100.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { activeTestDialog = null }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight)) {
                                    Text("Cancel", color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        if (userEnteredPin == "1234") {
                                            notify("✅ PIN verified successfully! Action processed!")
                                            activeTestDialog = null
                                        } else {
                                            notify("❌ Incorrect PIN! Please enter the default Sandbox PIN: 1234")
                                        }
                                        userEnteredPin = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                                ) {
                                    Text("VERIFY PIN", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        "Biometric" -> {
                            Text("Scan Biometric Fingerprint to approve:", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(RaivalPrimary.copy(alpha = 0.15f), shape = CircleShape)
                                    .clickable {
                                        notify("👤 Biometric Identity Match! Action approved!")
                                        activeTestDialog = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = "Scan", tint = RaivalPrimary, modifier = Modifier.size(44.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Tap fingerprint scanner icon to approve.", color = RaivalTextSecondary, fontSize = 10.sp)
                        }
                        "Time Delay" -> {
                            Text("HOLD TO PROCEED (3 Seconds Count Down)", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(progress = { delayProgress }, modifier = Modifier.fillMaxWidth(), color = RaivalSuccess)
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isHolding = true
                                                try {
                                                    while (isHolding && delayProgress < 1f) {
                                                        delayProgress += 0.05f
                                                        kotlinx.coroutines.delay(150)
                                                    }
                                                    if (delayProgress >= 1f) {
                                                        notify("✅ Hold countdown complete! Action fully executed!")
                                                        activeTestDialog = null
                                                    }
                                                } finally {
                                                    isHolding = false
                                                    delayProgress = 0f
                                                }
                                            }
                                        )
                                    },
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalSuccess)
                            ) {
                                Text("HOLD TO CONFIRM", color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFFFF1744).copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🛡️ ACCIDENTAL SPENDING PROTECTION", fontWeight = FontWeight.Black, color = Color(0xFFFF1744), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Configure double checking mechanisms and credential checks before costly or irreversible actions occur in the game.", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CHOOSE PROTECTION PROTOCOL TYPE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    listOf("Basic", "Double Confirm", "PIN Confirm", "Biometric", "Time Delay").forEach { type ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { selectedProtectionType = type }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedProtectionType == type, onClick = { selectedProtectionType = type })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(type, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        
        item {
            Text("PROTECTED ACTIONS LIST", fontWeight = FontWeight.Black, color = RaivalSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
        
        val actionList = listOf(
            ProtectedAction("Spend Coins / Balance", "Triggered when purchasing item stores or entering match bets", spendCoinsLocked) { b: Boolean -> spendCoinsLocked = b },
            ProtectedAction("Enter Tournament Brackets", "Triggered on joining ₵20-₵100 fee arenas", enterTournamentLocked) { b: Boolean -> enterTournamentLocked = b },
            ProtectedAction("Withdraw Funds", "Requested when cash is transferred to your MoMo account", withdrawLocked) { b: Boolean -> withdrawLocked = b },
            ProtectedAction("Abandon Active Tournament Match", "Triggered when forfeiting bracket progression", leaveLocked) { b: Boolean -> leaveLocked = b }
        )
        
        items(actionList, key = { it.title }) { action ->
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(action.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text(action.desc, color = RaivalTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { activeTestDialog = action.title },
                            enabled = action.isLocked,
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Test Flow Preview", color = RaivalPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Switch(checked = action.isLocked, onCheckedChange = { action.onToggle(it) })
                }
            }
        }
    }
}

// FEATURE 55: TOURNAMENT COUNTDOWN WIDGET
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentCountdownWidgetView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    val tournaments by viewModel.tournaments.collectAsState()
    var sizePreset by remember { mutableStateOf("Medium (2x4)") }
    var selectedTournamentForWidget by remember { mutableStateOf("Accra Super League 2026") }
    var timerTicks by remember { mutableLongStateOf(86400 * 2 + 15000) }
    
    val matchingTournament = tournaments.find { it.title == selectedTournamentForWidget }
    val registrationsState = matchingTournament?.let {
        viewModel.repository.getRegistrationsForTournament(it.id).collectAsState(initial = emptyList())
    }
    val actualCount = registrationsState?.value?.size ?: matchingTournament?.players ?: 12
    val maxCount = matchingTournament?.maxPlayers ?: 16
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            if (timerTicks > 0) timerTicks--
        }
    }
    
    val formatTimer = { seconds: Long ->
        val days = seconds / 86400
        val hrs = (seconds % 86400) / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        if (days > 0) "${days}d ${hrs}h ${mins}m" else String.format("%02d:%02d:%02d", hrs, mins, secs)
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⏳ LIVE TOURNAMENT COUNTDOWN WIDGET", fontWeight = FontWeight.Black, color = Color(0xFFFF9100), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Display countdown widgets on your mobile launcher to monitor registration timelines, active bracket starting times and slots filled.", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("WIDGET CONFIGURE CONFIG", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Select Target Match:", color = RaivalTextSecondary, fontSize = 11.sp)
                    var targetExpanded by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { targetExpanded = true }, colors = ButtonDefaults.buttonColors(containerColor = RaivalSurface), modifier = Modifier.fillMaxWidth()) {
                            Text(targetExpanded.let { selectedTournamentForWidget }, color = Color.White)
                        }
                        DropdownMenu(expanded = targetExpanded, onDismissRequest = { targetExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                            val allTitles = (tournaments.map { it.title } + listOf("Accra Super League 2026", "Ghana eFootball Showdown", "CODM Independence Cup")).distinct()
                            allTitles.forEach { name ->
                                DropdownMenuItem(text = { Text(name) }, onClick = {
                                    selectedTournamentForWidget = name
                                    targetExpanded = false
                                })
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Widget Grid Size Preset:", color = RaivalTextSecondary, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Small (2x2)", "Medium (2x4)", "Large (4x4)").forEach { size ->
                            Button(
                                onClick = { sizePreset = size },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (sizePreset == size) RaivalPrimary else RaivalSurface)
                            ) {
                                Text(size, color = if (sizePreset == size) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Text("MOBILE SYSTEM LAUNCHER LIVE PREVIEW", fontWeight = FontWeight.Black, color = RaivalSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
        
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                    border = BorderStroke(2.dp, RaivalSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("⏳ Raival Countdown Widget", fontSize = 10.sp, color = RaivalSecondary, fontWeight = FontWeight.Black)
                            Box(modifier = Modifier.background(Color.Red.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("LIVE TIMER", color = Color.Red, fontSize = 7.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(selectedTournamentForWidget, fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Text("Ghana Stadium • 1 vs 1 Mode", color = RaivalTextSecondary, fontSize = 11.sp)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(colors = CardDefaults.cardColors(containerColor = Color.Black), shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(horizontal = 12.dp)) {
                            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = formatTimer(timerTicks),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFFFF9100)
                                )
                            }
                        }
                        
                        if (sizePreset != "Small (2x2)") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$actualCount/$maxCount Joined", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = RaivalSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("₵150 GHS Pool", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        if (sizePreset == "Large (4x4)") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { notify("🔗 Shared countdown link!") },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("SHARE", color = Color.White, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { notify("✅ Registered for $selectedTournamentForWidget!") },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("REGISTER NOW", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// FEATURE 56: MATCH REPORTS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchReportsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var selectedReportType by remember { mutableStateOf("Premium Report") }
    
    val reportMatchHistory = remember {
        listOf(
            "FC Mobile Championship Finals",
            "DLS Weekly Arena Cup",
            "CODM Squad Duel Grand League"
        )
    }
    
    var selectedMatchIndex by remember { mutableIntStateOf(0) }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFF2979FF).copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 MATCH ANALYTICAL REPORTS", fontWeight = FontWeight.Black, color = Color(0xFF2979FF), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Comprehensive performance assessment cards outlining exact team ball possession, defensive actions, shots, passes and custom coach analysis recommendations.", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("REPORT FILTER CONTROLS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Select Target Match To Evaluate:", color = RaivalTextSecondary, fontSize = 11.sp)
                    var matchExpanded by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { matchExpanded = true }, colors = ButtonDefaults.buttonColors(containerColor = RaivalSurface), modifier = Modifier.fillMaxWidth()) {
                            Text(reportMatchHistory[selectedMatchIndex], color = Color.White)
                        }
                        DropdownMenu(expanded = matchExpanded, onDismissRequest = { matchExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                            reportMatchHistory.forEachIndexed { idx, name ->
                                DropdownMenuItem(text = { Text(name) }, onClick = {
                                    selectedMatchIndex = idx
                                    matchExpanded = false
                                })
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Choose Report Analytics Level:", color = RaivalTextSecondary, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Quick Report", "Standard Report", "Premium Report").forEach { lvl ->
                            Button(
                                onClick = { selectedReportType = lvl },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (selectedReportType == lvl) RaivalSecondary else RaivalSurface)
                            ) {
                                Text(lvl, color = if (selectedReportType == lvl) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Text("DETAILED MATCH STATS COMPARISON", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(reportMatchHistory[selectedMatchIndex].uppercase(), fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
                        Text("WIN 3 - 1", fontWeight = FontWeight.Black, color = RaivalSuccess, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    listOf(
                        Triple("Ball Possession", 0.58f, "58% vs 42%"),
                        Triple("Passing Accuracy", 0.84f, "84% vs 71%"),
                        Triple("Tackles Completed", 0.65f, "13/20 vs 8/20"),
                        Triple("Shots on Target", 0.75f, "9/12 vs 3/8")
                    ).forEach { stat ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stat.first, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(stat.third, color = RaivalSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(progress = { stat.second }, modifier = Modifier.fillMaxWidth(), color = RaivalPrimary)
                        }
                    }
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = RaivalPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI COACH INSIGHTS & ACTIONS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Defense analysis indicates a slight overcommitment during counter-attacks in the 30th minute. Coach recommends activating a 'Balanced' tactics switch earlier, and raising player defending masteries inside the Skill Tree system to maintain shape.",
                        color = RaivalTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { notify("📥 PDF Report downloaded to local device successfully!") },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("EXPORT FULL REPORT TO PDF", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

// FEATURE 57: BATTLE PASS SYSTEM
@Composable
fun BattlePassSystemView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var passLevel by remember { mutableIntStateOf(12) }
    var isPremiumUser by remember { mutableStateOf(user.coinBalance < 200) }
    var activeSubTab by remember { mutableStateOf("Rewards") }
    
    val rewardsList = listOf(
        Triple(1, "5 Coins", "15 Coins + Special Badge"),
        Triple(5, "Common Icon", "Rare Player Card"),
        Triple(10, "10 Coins", "Golden Stadium Skin"),
        Triple(15, "XP Boost x1.2", "XP Boost x2.0 (Premium)"),
        Triple(20, "15 Coins", "EPIC Custom Avatars Pack"),
        Triple(25, "Streak Shield x1", "LEGENDARY Golden Whistle Tone")
    )
    
    val seasonQuests = listOf(
        Triple("Win 3 Matches in DLS Arena", "Attacking mastery", "+50 XP"),
        Triple("Log in 5 Days consecutively", "Consistency booster", "+35 XP"),
        Triple("Participate in 1 eFootball tournament", "Diverse competitor", "+45 XP")
    )
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎟️ ARENA BATTLE PASS - SEASON 1", fontWeight = FontWeight.Black, color = Color(0xFFFFD700), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Complete weekly challenges, earn Battle Pass experience points (BP XP), and level up across 50 tiers of rewards!", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("PASS LEVEL: $passLevel", fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp)
                            Text("Current BP XP: 450/1000", color = RaivalTextSecondary, fontSize = 11.sp)
                        }
                        
                        Button(
                            onClick = {
                                passLevel++
                                notify("⚡ Level Up! Reached Battle Pass Level $passLevel!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                        ) {
                            Text("+100 XP", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { 0.45f }, modifier = Modifier.fillMaxWidth(), color = Color(0xFFFFD700))
                }
            }
        }
        
        if (!isPremiumUser) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFFFFD700))) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👑 UNLOCK PREMIUM BATTLE PASS TRACK", fontWeight = FontWeight.Black, color = Color(0xFFFFD700), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Unlock over 100 premium tier gold skins, custom tones, elite badges, and coin caches instantly!", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (user.coinBalance >= 500) {
                                        viewModel.spendCoins(500) { success ->
                                            if (success) {
                                                isPremiumUser = true
                                                notify("👑 Premium Battle Pass Unlocked!")
                                            }
                                        }
                                    } else {
                                        notify("❌ Insufficient Coins! You need 500 Coins.")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                            ) {
                                Text("PREMIUM (500 C)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    if (user.coinBalance >= 800) {
                                        viewModel.spendCoins(800) { success ->
                                            if (success) {
                                                isPremiumUser = true
                                                passLevel += 25
                                                notify("🚀 Premium+ Active! +25 Levels Unlocked!")
                                            }
                                        }
                                    } else {
                                        notify("❌ Insufficient Coins! You need 800 Coins.")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary)
                            ) {
                                Text("PREMIUM+ (800 C)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Rewards", "Daily Quests").forEach { tab ->
                    Button(
                        onClick = { activeSubTab = tab },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (activeSubTab == tab) RaivalPrimary else RaivalSurface)
                    ) {
                        Text(tab, color = if (activeSubTab == tab) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        if (activeSubTab == "Rewards") {
            items(rewardsList, key = { it.first }) { reward ->
                Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("LEVEL ${reward.first}", fontWeight = FontWeight.Black, color = RaivalSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("🎁 Free Track: ${reward.second}", color = Color.White, fontSize = 12.sp)
                            Text("👑 Premium: ${reward.third}", color = Color(0xFFFFD700), fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = {
                                if (passLevel >= reward.first) {
                                    notify("🎁 Claimed Level ${reward.first} reward: ${reward.second}!")
                                } else {
                                    notify("🔒 Lock! Reach Level ${reward.first} first.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (passLevel >= reward.first) RaivalSuccess else RaivalSurface)
                        ) {
                            Text(if (passLevel >= reward.first) "CLAIM" else "LOCKED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (passLevel >= reward.first) Color.Black else Color.White)
                        }
                    }
                }
            }
        } else {
            items(seasonQuests, key = { it.first }) { quest ->
                Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(quest.first, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(quest.second, color = RaivalTextSecondary, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                notify("⚡ Quest Completed! Earned BP ${quest.third}!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                        ) {
                            Text(quest.third, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

// FEATURE 58: TEAM MANAGER MODE
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagerModeView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var teamFormation by remember { mutableStateOf("4-3-3") }
    var teamTactics by remember { mutableStateOf("Tiki-Taka") }
    var isSimulatingMatch by remember { mutableStateOf(false) }
    val simLog = remember { mutableStateListOf<String>() }
    var simCompleted by remember { mutableStateOf(false) }
    var simResultText by remember { mutableStateOf("") }
    
    var botAttackerLevel by remember { mutableIntStateOf(1) }
    var botMidfielderLevel by remember { mutableIntStateOf(1) }
    var botDefenderLevel by remember { mutableIntStateOf(1) }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFF00D4FF).copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("👔 TEAM MANAGER MODE", fontWeight = FontWeight.Black, color = Color(0xFF00D4FF), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Draft your elite esports team of bot athletes, upgrade their skills, define tactical positions, and auto-simulate league matches.", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TEAM TACTICAL FORMATION", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("4-3-3", "4-4-2", "3-5-2").forEach { f ->
                            Button(
                                onClick = { teamFormation = f },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (teamFormation == f) RaivalPrimary else RaivalSurface)
                            ) {
                                Text(f, color = if (teamFormation == f) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Strategic Match Philosophy:", color = RaivalTextSecondary, fontSize = 11.sp)
                    var tacticsExpanded by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { tacticsExpanded = true }, colors = ButtonDefaults.buttonColors(containerColor = RaivalSurface), modifier = Modifier.fillMaxWidth()) {
                            Text(teamTactics, color = Color.White)
                        }
                        DropdownMenu(expanded = tacticsExpanded, onDismissRequest = { tacticsExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                            listOf("Offensive", "Defensive", "Tiki-Taka", "Counter-Attack").forEach { tac ->
                                DropdownMenuItem(text = { Text(tac) }, onClick = {
                                    teamTactics = tac
                                    tacticsExpanded = false
                                })
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("MANAGE BOT ATHLETES SQUAD", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    listOf(
                        Triple("🤖 Bot Attacker", botAttackerLevel) { botAttackerLevel++ },
                        Triple("🤖 Bot Midfielder", botMidfielderLevel) { botMidfielderLevel++ },
                        Triple("🤖 Bot Defender", botDefenderLevel) { botDefenderLevel++ }
                    ).forEach { bot ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(bot.first, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                Text("OVR Rating: ${70 + bot.second * 4} • Level ${bot.second}", color = RaivalTextSecondary, fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    if (user.coinBalance >= 50) {
                                        viewModel.spendCoins(50) { success ->
                                            if (success) {
                                                bot.third()
                                                notify("💪 Upgraded ${bot.first}! OVR increased!")
                                            }
                                        }
                                    } else {
                                        notify("❌ You need 50 Coins for training!")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("TRAIN (50 C)", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Button(
                onClick = {
                    isSimulatingMatch = true
                    simCompleted = false
                    simLog.clear()
                    simLog.add("📋 Whistle blows! Simulating team match utilizing '$teamFormation' '$teamTactics' strategy...")
                },
                enabled = !isSimulatingMatch,
                colors = ButtonDefaults.buttonColors(containerColor = RaivalSuccess),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("INITIATE MATCH SIMULATION", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }
        
        if (isSimulatingMatch) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, RaivalPrimary)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("🎮 SIMULATION INTERACTIVE ENGINE", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 12.sp)
                            if (!simCompleted) {
                                CircularProgressIndicator(color = RaivalPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        simLog.forEach { log ->
                            Text(log, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        
                        if (simCompleted) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth().background(RaivalPrimary.copy(alpha = 0.1f)).padding(8.dp)) {
                                Text(simResultText, color = RaivalPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { isSimulatingMatch = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RaivalSurface)) {
                                Text("CLOSE SIMULATOR", color = Color.White)
                            }
                        }
                    }
                }
                
                LaunchedEffect(isSimulatingMatch) {
                    // Real deterministic simulation: outcome derives from the
                    // drafted squad's OVR ratings, formation and tactics via a
                    // seeded RNG — identical inputs always give identical results.
                    // Training mode only: XP on win, no coin payouts.
                    val attackerOvr = 70 + botAttackerLevel * 4
                    val midOvr = 70 + botMidfielderLevel * 4
                    val defenderOvr = 70 + botDefenderLevel * 4
                    val teamOvr = (attackerOvr + midOvr + defenderOvr) / 3
                    val tacticsBonus = when (teamTactics) {
                        "Offensive" -> 3
                        "Counter-Attack" -> 2
                        "Tiki-Taka" -> 2
                        else -> 1
                    }
                    val formationBonus = when (teamFormation) {
                        "4-3-3" -> 2
                        "4-4-2" -> 1
                        else -> 1
                    }
                    val effectiveOvr = teamOvr + tacticsBonus + formationBonus
                    val opponentOvr = 82
                    val opponentName = "Accra Rivals"
                    val seed = "$teamFormation|$teamTactics|$attackerOvr|$midOvr|$defenderOvr|$opponentOvr".hashCode().toLong()
                    val rng = java.util.Random(seed)
                    simLog.add("📋 Whistle blows! Your $teamFormation ($teamTactics) — OVR $effectiveOvr — faces $opponentName (OVR $opponentOvr).")
                    kotlinx.coroutines.delay(900)
                    val diff = effectiveOvr - opponentOvr
                    val myGoals = (1 + diff / 3 + rng.nextInt(2)).coerceIn(0, 5)
                    val oppGoals = (1 - diff / 4 + rng.nextInt(2)).coerceIn(0, 4)
                    val minute = 45 + rng.nextInt(45)
                    simLog.add("⏱️ $minute' — Full time approaches, score ${user.username} $myGoals - $oppGoals $opponentName.")
                    kotlinx.coroutines.delay(900)
                    simResultText = if (myGoals > oppGoals) {
                        viewModel.addXp(25)
                        "🏆 FULL TIME: ${user.username} $myGoals - $oppGoals $opponentName. Victory! +25 XP (training — no coin payout)."
                    } else if (myGoals == oppGoals) {
                        "🤝 FULL TIME: ${user.username} $myGoals - $oppGoals $opponentName. Draw — train your squad and retry."
                    } else {
                        "💔 FULL TIME: ${user.username} $myGoals - $oppGoals $opponentName. Defeat — upgrade bot OVR or adjust tactics."
                    }
                    simCompleted = true
                }
            }
        }
    }
}

// FEATURE 59: PREDICTION LEAGUE
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionLeagueView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var predictionStreak by remember { mutableIntStateOf(0) }
    var selectedMatchWinner by remember { mutableStateOf<String?>(null) }
    var scoreWinner by remember { mutableStateOf("2") }
    var scoreOpponent by remember { mutableStateOf("1") }
    var predictionMatches by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var predictionIdByTitle by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        try {
            val tournaments = com.example.data.sync.SupabaseSyncManager.pullTable("tournaments")
                .filter { it["status"]?.jsonPrimitive?.contentOrNull == "Open" }
                .take(3)
            predictionMatches = if (tournaments.isNotEmpty()) {
                tournaments.map { t ->
                    val title = t["title"]?.jsonPrimitive?.contentOrNull ?: "Match"
                    val map = t["map"]?.jsonPrimitive?.contentOrNull ?: "Arena"
                    val date = t["date"]?.jsonPrimitive?.contentOrNull ?: "TBD"
                    Triple(title, map, date)
                }
            } else {
                listOf(Triple("No upcoming matches", "Check back later", ""))
            }
            predictionIdByTitle = tournaments.associate { t ->
                (t["title"]?.jsonPrimitive?.contentOrNull ?: "Match") to
                    (t["id"]?.jsonPrimitive?.intOrNull ?: 0)
            }
        } catch (_: Exception) {
            predictionMatches = listOf(Triple("No upcoming matches", "Check back later", ""))
        }
        // Real streak: settled correct predictions from the server
        viewModel.loadMyPredictions { _, streak -> predictionStreak = streak }
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFFE040FB).copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔮 RAIVAL PREDICTION LEAGUE", fontWeight = FontWeight.Black, color = Color(0xFFE040FB), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Predict exact final scores and match winners of top-rated tournament battles to gain massive coins and exclusive predictor badges.", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFFE040FB).copy(alpha = 0.15f), shape = CircleShape), contentAlignment = Alignment.Center) {
                        Text("🔥", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("PREDICTOR STREAK: $predictionStreak MATCHES", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                        Text("Active Streak Bonus multiplier: 1.5x coins earned!", color = RaivalTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
        
        item {
            Text("UPCOMING SHOWDOWN PREDICTIONS", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
        
        if (predictionMatches.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔮", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No upcoming matches available for prediction.", color = RaivalTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Check back when tournaments are scheduled!", color = RaivalTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
        
        items(predictionMatches, key = { it.first }) { match ->
            var predictionSubmitted by remember { mutableStateOf(false) }
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(match.second, color = RaivalSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(match.third, color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(match.first, fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (!predictionSubmitted) {
                        Text("Predict Winner Match Athlete:", color = RaivalTextSecondary, fontSize = 11.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val athletes = match.first.split(" vs ")
                            athletes.forEach { name ->
                                Button(
                                    onClick = { selectedMatchWinner = name },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedMatchWinner == name) Color(0xFFE040FB) else RaivalSurface)
                                ) {
                                    Text(name, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Predict Scoreline:", color = RaivalTextSecondary, fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = scoreWinner,
                                    onValueChange = { scoreWinner = it },
                                    modifier = Modifier.width(44.dp),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                                )
                                Text(":", color = Color.White)
                                OutlinedTextField(
                                    value = scoreOpponent,
                                    onValueChange = { scoreOpponent = it },
                                    modifier = Modifier.width(44.dp),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val pick = selectedMatchWinner
                                if (pick == null) {
                                    notify("Pick a winner first!")
                                } else {
                                    val tid = predictionIdByTitle[match.first] ?: 0
                                    viewModel.submitPrediction(
                                        tid, pick,
                                        scoreWinner.toIntOrNull() ?: 0,
                                        scoreOpponent.toIntOrNull() ?: 0
                                    ) { success, msg ->
                                        if (success) predictionSubmitted = true
                                        notify(msg)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SUBMIT PREDICTION", color = Color.Black, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().background(RaivalSuccess.copy(alpha = 0.1f)).padding(10.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔮 PREDICTION LOCKED IN", color = RaivalSuccess, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                Text("Your pick: $selectedMatchWinner to win ($scoreWinner - $scoreOpponent)", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// FEATURE 60: AMBASSADOR PROGRAM
@Composable
fun AmbassadorProgramView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var salaryClaimed by remember { mutableStateOf(false) }
    var applicationSubmitted by remember { mutableStateOf(false) }
    
    val matchesPlayed = user.wins + user.losses
    val winRatePercent = if (matchesPlayed > 0) (user.wins * 100) / matchesPlayed else 50
    
    val hasPlayedRequiredMatches = matchesPlayed >= 10
    val hasRequiredWinRate = winRatePercent >= 50
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("👑 RAIVAL AMBASSADOR COMMUNITY PROGRAM", fontWeight = FontWeight.Black, color = Color(0xFFFFD700), fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Join Accra's elite gamers as a Raival Ambassador. Help moderate chat rooms, report bugs, run custom tournaments, and earn monthly coin stipends!", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AMBASSADOR ELIGIBILITY STATUS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    listOf(
                        Pair("Play 10+ Arena Matches", hasPlayedRequiredMatches),
                        Pair("Maintain 50%+ Win Rate", hasRequiredWinRate),
                        Pair("Active Account Status", user.status == "active")
                    ).forEach { req ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(req.first, color = Color.White, fontSize = 12.sp)
                            if (req.second) {
                                Icon(Icons.Default.Check, contentDescription = "Passed", tint = RaivalSuccess, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.Close, contentDescription = "Failed", tint = RaivalError, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
        
        if (hasPlayedRequiredMatches && hasRequiredWinRate) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, Color(0xFFFFD700))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🌟 OFFICIAL AMBASSADOR BENEFIT PORTAL", fontWeight = FontWeight.Black, color = Color(0xFFFFD700), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("You are fully eligible to join the Ambassador tier! Claim your monthly stipend salary below.", color = Color.White, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                if (!salaryClaimed) {
                                    viewModel.earnCoins(200)
                                    salaryClaimed = true
                                    notify("💰 Claimed 200 Ambassador Monthly Coins stipend!")
                                }
                            },
                            enabled = !salaryClaimed,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (salaryClaimed) "SALARY CLAIMED" else "CLAIM MONTHLY SALARY (200 COINS)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AMBASSADOR APPLICATION", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("You do not currently meet all Ambassador tier conditions. Submit a waiver application to our admins for reviews.", color = RaivalTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                applicationSubmitted = true
                                notify("📩 Ambassador review application submitted!")
                            },
                            enabled = !applicationSubmitted,
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (applicationSubmitted) "APPLICATION UNDER REVIEW" else "SUBMIT REVIEW APPLICATION", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        
        item {
            Text("AMBASSADOR DAILY CONTRIBUTION QUESTS", fontWeight = FontWeight.Black, color = RaivalSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
        
        val questsList = listOf(
            Triple("Moderate Live Chat Rooms", "Review reported messages for toxicity", "+50 Coins"),
            Triple("Report a Platform Bug", "Submit detailed sandbox logs to devs", "+25 Coins"),
            Triple("Refer 1 New Friend Player", "Share viral registration invite codes", "+30 Coins")
        )
        
        items(questsList, key = { it.first }) { quest ->
            var questClaimed by remember(quest.first) { mutableStateOf(false) }
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(quest.first, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(quest.second, color = RaivalTextSecondary, fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            if (!questClaimed) {
                                val rewardAmt = quest.third.replace("+", "").replace(" Coins", "").toIntOrNull() ?: 20
                                viewModel.earnCoins(rewardAmt)
                                questClaimed = true
                                notify("✅ Contribution rewarded! Gained ${quest.third}!")
                            }
                        },
                        enabled = !questClaimed,
                        colors = ButtonDefaults.buttonColors(containerColor = if (questClaimed) RaivalSurface else RaivalPrimary)
                    ) {
                        Text(if (questClaimed) "CLAIMED" else quest.third, color = if (questClaimed) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

data class AvatarItem(
    val id: String,
    val name: String,
    val emoji: String,
    val cost: Int,
    val desc: String,
    val costType: String = "COINS"
)

data class ThemeItem(
    val id: String,
    val name: String,
    val desc: String,
    val cost: Int,
    val type: String
)

data class AnimEntryDef(
    val id: String,
    val name: String,
    val desc: String,
    val cost: Int,
    val benefit: String
)

// -----------------------------------------------------------------------------
// FEATURE 31: COIN ECONOMY SYSTEM (DAILY, MATCHES, MILESTONES & COMMUNITY)
// -----------------------------------------------------------------------------
@Composable
fun CoinEconomyHubView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    val appConfigState by viewModel.appConfig.collectAsState()
    val isCoinOnly = (appConfigState?.economyMode ?: "Coin-Only") == "Coin-Only"
    val economyBoost = if (isCoinOnly) " (3x Boost Active!)" else ""

    var tipText by remember { mutableStateOf("") }
    var selectedContributionType by remember { mutableStateOf("pro_tip") } // "pro_tip", "score_report", "screenshot_verify"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "🪙 COIN ECONOMY PORTAL",
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD700),
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Reputation, Progression & Utility Tokens",
                                color = RaivalTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🪙", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${user.coinBalance}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    if (isCoinOnly) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RaivalPrimary.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "⚡ COIN-ONLY BOOST: All virtual earnings from daily login streaks, match bonuses, milestones, and contributions are automatically boosted by 300% (3x multiplier)!",
                                color = RaivalPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // STRICT REGULATORY SEPARATION CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0x22FF1744)),
                border = BorderStroke(1.5.dp, Color(0xFFFF1744).copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Security Shield",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "REGULATORY COMPLIANCE DIRECTIVE",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To preserve fair play standards and adhere strictly to virtual currency regulations:\n" +
                                "1. Coins are non-fiat, non-transferable, and cannot be bought using real-cash.\n" +
                                "2. Coins are locked inside the game client and CANNOT be cashed out or converted to real money.\n" +
                                "3. The virtual Coin Economy is completely separated and isolated from all GHS cash wallets or future payment gateways. All matches entered with Coins reward only reputation & virtual badges.",
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 1. DAILY LOGIN STREAKS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "📅 1. DAILY LOGIN STREAKS$economyBoost",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    "Log in consecutively to maximize your coin multipliers. Claim your login reward for each active streak day!",
                    color = RaivalTextSecondary,
                    fontSize = 11.sp
                )

                val streakDays = listOf(
                    Triple(1, 15, "Bronze Star"),
                    Triple(2, 30, "Bronze Double"),
                    Triple(3, 45, "Silver Medal"),
                    Triple(4, 60, "Silver Rush"),
                    Triple(5, 75, "Gold Streak"),
                    Triple(6, 90, "Gold Master"),
                    Triple(7, 150, "Legendary Chest")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    streakDays.forEach { (day, baseAmount, perkName) ->
                        val streakId = "streak_day_$day"
                        val isClaimed = user.completedMissions.split(",").contains(streakId)
                        val multiplier = if (isCoinOnly) 3 else 1
                        val actualReward = baseAmount * multiplier

                        Card(
                            modifier = Modifier
                                .width(94.dp)
                                .clickable(!isClaimed) {
                                    viewModel.claimStreakLoginReward(day, baseAmount) { success, msg ->
                                        notify(msg)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isClaimed) Color.Black else RaivalSurfaceLight
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isClaimed) Color.White.copy(alpha = 0.1f) else Color(0xFFFFD700).copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "DAY $day",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = if (isClaimed) Color.Gray else Color(0xFFFFD700)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (day == 7) "👑" else "🎁", fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "+$actualReward 🪙",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                                Text(
                                    perkName,
                                    fontSize = 8.sp,
                                    color = RaivalTextSecondary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isClaimed) Color.Gray.copy(alpha = 0.2f) else Color(0xFFFFD700).copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isClaimed) "CLAIMED" else "CLAIM",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        color = if (isClaimed) Color.Gray else Color(0xFFFFD700)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. MATCH PARTICIPATION BONUSES
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "⚽ 2. COMPETITIVE MATCH PARTICIPATION BONUSES",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    "Earn virtual coins simply for playing games, regardless of whether you win or lose! Complete practice matches to practice and collect immediate rewards.",
                    color = RaivalTextSecondary,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Casual Arena Play", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            Text("Play friendly 1v1 rooms.", color = RaivalTextSecondary, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Base Bonus: 15 🪙",
                                color = RaivalPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.claimMatchParticipationBonus("Casual Arena", 15) { _, msg ->
                                        notify(msg)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("COMPETE (CASUAL)", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                        border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Ranked Tournament Play", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            Text("Play pro-level qualifiers.", color = RaivalTextSecondary, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Base Bonus: 30 🪙",
                                color = RaivalSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.claimMatchParticipationBonus("Ranked Match", 30) { _, msg ->
                                        notify(msg)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("COMPETE (RANKED)", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. TOURNAMENT MILESTONES
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "🏆 3. TOURNAMENT MILESTONES & ACHIEVEMENTS",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    "Accomplish esports milestones in tournaments to unlock premium coin reserves. Rewards match actual player stats.",
                    color = RaivalTextSecondary,
                    fontSize = 11.sp
                )

                val milestones = listOf(
                    Triple("milestone_first_tour", "My First registered Tournament", "Requires: 1 Tournament Played (Played: ${user.tournamentsPlayed})"),
                    Triple("milestone_five_wins", "Rampage Challenger Milestone", "Requires: 5 Total Match Wins (Wins: ${user.wins})"),
                    Triple("milestone_hot_streak", "Fierce Active Win Streak", "Requires: 3 Hot Win Streak (Streak: ${user.winStreak})"),
                    Triple("milestone_level_five", "Gamer Prestige Level 5", "Requires: Account Level 5 (Current: ${user.level})")
                )

                milestones.forEach { (id, title, req) ->
                    val isClaimed = user.completedMissions.split(",").contains(id)
                    val reward = when (id) {
                        "milestone_first_tour" -> 50
                        "milestone_five_wins" -> 100
                        "milestone_hot_streak" -> 150
                        "milestone_level_five" -> 200
                        else -> 50
                    }
                    val actualReward = if (isCoinOnly) reward * 3 else reward

                    val meetsReq = when (id) {
                        "milestone_first_tour" -> user.tournamentsPlayed >= 1
                        "milestone_five_wins" -> user.wins >= 5
                        "milestone_hot_streak" -> user.winStreak >= 3
                        "milestone_level_five" -> user.level >= 5
                        else -> false
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                        border = BorderStroke(
                            1.dp,
                            if (isClaimed) Color.White.copy(alpha = 0.05f) else if (meetsReq) RaivalSecondary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                Text(req, color = RaivalTextSecondary, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Reward: +$actualReward 🪙",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isClaimed) Color.Gray else RaivalSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.claimTournamentMilestone(id, reward) { _, msg ->
                                        notify(msg)
                                    }
                                },
                                enabled = meetsReq && !isClaimed,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (meetsReq) RaivalSecondary else Color.DarkGray,
                                    disabledContainerColor = Color.Black.copy(alpha = 0.3f)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isClaimed) "CLAIMED" else if (meetsReq) "CLAIM" else "LOCKED",
                                    color = if (meetsReq) Color.Black else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. COMMUNITY CONTRIBUTION REWARDS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "👑 4. COMMUNITY CONTRIBUTION REWARDS",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    "Raival is community-moderated. Help other players, share pro strategies, verify score screenshots, or report verified results to claim contributions rewards!",
                    color = RaivalTextSecondary,
                    fontSize = 11.sp
                )

                // Contribution Type Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        Pair("pro_tip", "📚 Pro Tip"),
                        Pair("score_report", "📋 Score"),
                        Pair("screenshot_verify", "🛡️ Mod Review")
                    )
                    types.forEach { (type, label) ->
                        val active = selectedContributionType == type
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedContributionType = type },
                            colors = CardDefaults.cardColors(
                                containerColor = if (active) RaivalPrimary.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (active) RaivalPrimary else Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) RaivalPrimary else Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val baseContributionCoins = when (selectedContributionType) {
                            "pro_tip" -> 25
                            "score_report" -> 15
                            "screenshot_verify" -> 20
                            else -> 10
                        }
                        val finalReward = if (isCoinOnly) baseContributionCoins * 3 else baseContributionCoins

                        Text(
                            text = when (selectedContributionType) {
                                "pro_tip" -> "Write & submit an eFootball/DLS pro tip or strategy guide. Guide must be at least 10 letters."
                                "score_report" -> "Report final match scorecards with exact custom room keys and scores."
                                "screenshot_verify" -> "Conduct moderator review verifying the uploaded match results screenshot."
                                else -> ""
                            },
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Reward: +$finalReward 🪙",
                            fontWeight = FontWeight.Bold,
                            color = RaivalPrimary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tipText,
                            onValueChange = { tipText = it },
                            label = {
                                Text(
                                    text = when (selectedContributionType) {
                                        "pro_tip" -> "Type your pro gaming strategy tip..."
                                        "score_report" -> "Room Key & scorecard details..."
                                        "screenshot_verify" -> "Match verification notes & screenshot hash..."
                                        else -> "Contribution details..."
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = RaivalPrimary,
                                focusedIndicatorColor = RaivalPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.submitCommunityContribution(selectedContributionType, tipText, baseContributionCoins) { success, msg ->
                                    notify(msg)
                                    if (success) tipText = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                        ) {
                            Text("SUBMIT CONTRIBUTION & CLAIM", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}




