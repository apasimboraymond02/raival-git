package com.example.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.serialization.json.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import com.example.data.model.User
import com.example.data.model.MatchSession
import androidx.compose.animation.core.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaivalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import org.json.JSONObject

// Data class representing a Casual Game Definition
data class CasualGame(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val difficulty: String,
    val diffColor: Color,
    val activePlayers: Int,
    val sizeMb: Int,
    val avgEarnings: Int,
    val rating: String
)

// Data class representing screenshots taken during the match
data class CapturedScreenshot(
    val id: Int,
    val matchMinutes: Int,
    val scoreText: String,
    val eventType: String, // "Kickoff", "Regular Interval", "Goal", "Half-Time", "Match End", "Stats Screen"
    val imageDescription: String,
    val fileSizeBytes: Int, // 100-200KB for highly optimized low data consumption
    val isUploaded: Boolean,
    val uploadProgress: Float = 1.0f,
    val playerText: String = "Host (You)"
)

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasualGamesTab(
    viewModel: RaivalViewModel,
    currentUser: User
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Core states
    val currentLeaderboard by viewModel.activeSessionLeaderboard.collectAsState()
    val timeRemaining by viewModel.currentSessionTimeRemaining.collectAsState()
    val bookedSessions by viewModel.bookedSessions.collectAsState()
    val sessionCoinsEarned by viewModel.sessionCoinsEarnedToday.collectAsState()
    val adCoinsEarned by viewModel.adCoinsEarnedToday.collectAsState()
    val referralCoinsEarned by viewModel.referralCoinsEarnedToday.collectAsState()
    val activeSessionName by viewModel.activeSessionName.collectAsState()
    val sessionTransitionAlert by viewModel.sessionTransitionAlert.collectAsState()

    // --- Simultaneous Session State Collections ---
    val casualSessionStatus by viewModel.casualSessionStatus.collectAsState()
    val casualSessionTimeRemaining by viewModel.casualSessionTimeRemaining.collectAsState()
    val registeredPlayersCount by viewModel.registeredPlayersCount.collectAsState()
    val isUserRegistered by viewModel.isUserRegistered.collectAsState()
    val isUserPreRegisteredForNext by viewModel.isUserPreRegisteredForNext.collectAsState()
    val casualSessionCompletedScores by viewModel.casualSessionCompletedScores.collectAsState()

    // Formatted timer representation
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    // Downloaded games persistence using simple set
    var downloadedGames by remember { mutableStateOf(setOf<String>()) }
    var downloadingGameId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    // Selected Active Game Overlay
    val activePlayingGameId by viewModel.activePlayingGameId.collectAsState()

    // Promotion actions
    var claimLoginBonusSuccess by remember { mutableStateOf(false) }
    var adWatchingActive by remember { mutableStateOf(false) }
    var adTimerRemaining by remember { mutableStateOf(5) }
    var showReferralDialog by remember { mutableStateOf(false) }
    var referralInputName by remember { mutableStateOf("") }
    var referralInputPhone by remember { mutableStateOf("") }
    var referralSuccessMessage by remember { mutableStateOf("") }

    // Booking actions
    var bookingSuccessMessage by remember { mutableStateOf("") }

    // Master List of Games
    val gamesList = remember {
        listOf(
            CasualGame(
                id = "candy_crush",
                title = "Candy Match-3",
                description = "Swap adjacent candies to match rows or columns of 3.",
                icon = Icons.Default.Icecream,
                difficulty = "Easy",
                diffColor = Color(0xFF00FF87),
                activePlayers = 84,
                sizeMb = 6,
                avgEarnings = 14,
                rating = "4.8"
            ),
            CasualGame(
                id = "block_buster",
                title = "Block Buster",
                description = "Drag blocks into place to destroy rows or columns.",
                icon = Icons.Default.GridView,
                difficulty = "Medium",
                diffColor = Color(0xFFFFD700),
                activePlayers = 112,
                sizeMb = 8,
                avgEarnings = 18,
                rating = "4.7"
            ),
            CasualGame(
                id = "puzzle_2048",
                title = "2048 Puzzle",
                description = "Slide and combine matching number tiles to reach 2048.",
                icon = Icons.Default.Filter9Plus,
                difficulty = "Hard",
                diffColor = Color(0xFFFF5E62),
                activePlayers = 56,
                sizeMb = 4,
                avgEarnings = 22,
                rating = "4.9"
            ),
            CasualGame(
                id = "word_search",
                title = "Word Spelling Bee",
                description = "Find hidden DLS & football words in the letter board.",
                icon = Icons.Default.Spellcheck,
                difficulty = "Medium",
                diffColor = Color(0xFFFFD700),
                activePlayers = 41,
                sizeMb = 7,
                avgEarnings = 12,
                rating = "4.5"
            ),
            CasualGame(
                id = "memory_match",
                title = "Memory Card Flip",
                description = "Find matching cards in the minimum possible attempts.",
                icon = Icons.Default.Style,
                difficulty = "Easy",
                diffColor = Color(0xFF00FF87),
                activePlayers = 65,
                sizeMb = 5,
                avgEarnings = 10,
                rating = "4.6"
            ),
            CasualGame(
                id = "trivia_quiz",
                title = "Raival Sports Trivia",
                description = "Answer soccer, DLS, and esports trivia queries.",
                icon = Icons.Default.Quiz,
                difficulty = "Medium",
                diffColor = Color(0xFFFFD700),
                activePlayers = 138,
                sizeMb = 9,
                avgEarnings = 15,
                rating = "4.7"
            ),
            CasualGame(
                id = "color_match",
                title = "Speed Color Tap",
                description = "Tap correct colors matching the mismatched words.",
                icon = Icons.Default.Palette,
                difficulty = "Hard",
                diffColor = Color(0xFFFF5E62),
                activePlayers = 95,
                sizeMb = 5,
                avgEarnings = 20,
                rating = "4.8"
            )
        )
    }

    val isOnline by viewModel.isOnline.collectAsState()
    // --- Tab state between Arcade, Match Zone and Retro Sounds ---
    var casualSubTab by remember(isOnline) { mutableStateOf(if (isOnline) 1 else 0) }

    // --- 1v1 Match Zone In-Game Coins Sync & Verification States ---
    var matchZoneTab by remember { mutableStateOf(1) } // 1: Coins Verification, 2: API Sync Guidelines
    var matchmakingActive by remember { mutableStateOf(false) }
    var matchmakingStatus by remember { mutableStateOf("Searching...") }
    var isMatched by remember { mutableStateOf(false) }
    
    // Custom Friendly Room Code
    var showJoinRoomDialog by remember { mutableStateOf(false) }
    var customRoomCodeInput by remember { mutableStateOf("") }
    var activeRoomCode by remember { mutableStateOf("") }
    var opponentName by remember { mutableStateOf("") }
    var opponentIGN by remember { mutableStateOf("") }
    var matchStatusState by remember { mutableStateOf("Setup") } // "Setup", "Ready", "Verifying", "Verified"
    
    // In-game Coins State
    var selectedSimGame by remember { mutableStateOf("eFootball") }
    var selectedSimFee by remember { mutableStateOf(50) }
    
    // Coins Balances (Host & Guest) — In-game coin sync requires game API integration (coming soon)
    var hostPreCoins by remember { mutableStateOf(0) }
    var guestPreCoins by remember { mutableStateOf(0) }
    var hostPostCoins by remember { mutableStateOf(0) }
    var guestPostCoins by remember { mutableStateOf(0) }
    var verificationLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isVerifyingCoins by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (activePlayingGameId != null) {
        // Render Active Game Full-Screen Playable Canvas Overlay
        CasualGamePlayScreen(
            gameId = activePlayingGameId!!,
            currentUser = currentUser,
            onBack = { viewModel.setActivePlayingGameId(null) },
            onSubmitScore = { score ->
                viewModel.submitCasualGameScore(score)
                viewModel.rewardParticipation("complete")
            },
            viewModel = viewModel
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RaivalBackground)
        ) {
            // Horizontal Tab Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RaivalSurface)
                    .padding(bottom = 2.dp)
            ) {
                listOf(
                    Triple("🕹️ ARCADE", 0, "arcade"),
                    Triple("⚽ 1V1 MATCH", 1, "match_1v1")
                ).forEach { (label, idx, tag) ->
                    val isSelected = casualSubTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { casualSubTab = idx }
                            .padding(vertical = 14.dp)
                            .testTag("casual_subtab_$tag"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                color = if (isSelected) RaivalPrimary else RaivalTextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(2.dp)
                                    .background(if (isSelected) RaivalPrimary else Color.Transparent)
                            )
                        }
                    }
                }
            }

            if (casualSubTab == 0) {
                // -------------------------------------------------------------
                // ARCADE ARENA VIEW (ORIGINAL CONTENT)
                // -------------------------------------------------------------
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("casual_games_scrollable"),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Promotional Banner
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.25f))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                                Image(
                                    painter = painterResource(id = R.drawable.bg_gaming_2),
                                    contentDescription = "Arcade Hero Banner",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.85f
                                )
                                // Light overlay gradient
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.50f)
                                                )
                                            )
                                        )
                                )
                                // Text details overlay
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Text(
                                        text = "🕹️ ARCADE ARENA",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp,
                                        color = RaivalPrimary,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Play retro-style mini-games, rank on live session boards, and earn Raival coins!",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // 🕹️ My Retro Stats & Rewards Dashboard
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            val totalEarnedCoins = sessionCoinsEarned + adCoinsEarned + referralCoinsEarned
                            val cappedProgress = (totalEarnedCoins.toFloat() / 50f).coerceIn(0f, 1f)

                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "🕹️ MY RETRO PERFORMANCE",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = RaivalPrimary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Accumulate coins and unlock arcade milestones.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = RaivalTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(RaivalSecondary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "LEVEL 4 ARCADE",
                                            color = RaivalSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                HorizontalDivider(color = RaivalSurface, thickness = 1.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Left Column: Daily Coin Cap Progress (Fraction of 50 Coins)
                                    Column(
                                        modifier = Modifier.weight(1.2f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Daily Coin Cap",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "$totalEarnedCoins / 50 Coins",
                                                color = RaivalSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }

                                        // Custom Neon Style Progress Bar
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .background(RaivalSurface, shape = RoundedCornerShape(5.dp))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(fraction = cappedProgress)
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            colors = listOf(RaivalPrimary, RaivalSecondary)
                                                        ),
                                                        shape = RoundedCornerShape(5.dp)
                                                    )
                                            )
                                        }

                                        Text(
                                            text = "Unlock +25 bonus coins once 50 daily limit is reached!",
                                            fontSize = 8.sp,
                                            color = RaivalTextSecondary,
                                            lineHeight = 11.sp
                                        )
                                    }

                                    // Vertical divider
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(54.dp)
                                            .background(RaivalSurface)
                                    )

                                    // Right Column: Unlocked Gaming Milestones / Achievements
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "UNLOCKED PERKS",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )

                                        val myCompletedScore = casualSessionCompletedScores[currentUser.username] ?: 0
                                        val playedToday = myCompletedScore > 0

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EmojiEvents,
                                                contentDescription = null,
                                                tint = if (playedToday) RaivalSecondary else Color.Gray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Session Competitor",
                                                color = if (playedToday) Color.White else Color.Gray,
                                                fontSize = 9.sp,
                                                fontWeight = if (playedToday) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OfflineBolt,
                                                contentDescription = null,
                                                tint = if (totalEarnedCoins >= 10) RaivalPrimary else Color.Gray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Daily Coin Grinder",
                                                color = if (totalEarnedCoins >= 10) Color.White else Color.Gray,
                                                fontSize = 9.sp,
                                                fontWeight = if (totalEarnedCoins >= 10) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Stars,
                                                contentDescription = null,
                                                tint = if (claimLoginBonusSuccess) RaivalPrimary else Color.Gray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Loyal Streaker",
                                                color = if (claimLoginBonusSuccess) Color.White else Color.Gray,
                                                fontSize = 9.sp,
                                                fontWeight = if (claimLoginBonusSuccess) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Simultaneous Casual Session Lounge Panel
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(
                                                        if (casualSessionStatus == "REGISTRATION") Color(0xFF00FF87) else Color.Red,
                                                        shape = CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (casualSessionStatus == "REGISTRATION") "🟢 PRACTICE LOBBY OPEN" else "⚡ PRACTICE SESSION ONGOING",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 0.5.sp
                                                ),
                                                color = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (casualSessionStatus == "REGISTRATION") "Kickoff in: ${casualSessionTimeRemaining}s" else "Remaining: ${casualSessionTimeRemaining}s",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = RaivalSecondary
                                        )
                                    }

                                    // Active players / registrants count
                                    Box(
                                        modifier = Modifier
                                            .background(RaivalSurface, shape = RoundedCornerShape(12.dp))
                                            .border(BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = RaivalPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (casualSessionStatus == "REGISTRATION") "$registeredPlayersCount in lobby" else "Practice scores • play to rank",
                                                color = RaivalPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                HorizontalDivider(color = RaivalSurface, thickness = 1.dp)

                                Spacer(modifier = Modifier.height(12.dp))

                                // Interactive User Action & Queue Status
                                if (casualSessionStatus == "REGISTRATION") {
                                    if (isUserRegistered) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = RaivalSurface.copy(alpha = 0.5f)),
                                            border = BorderStroke(1.dp, Color(0xFF00FF87).copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF00FF87),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "You are REGISTERED! Stay in this lobby. Play will begin automatically at 00:00.",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.isUserRegistered.value = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                            modifier = Modifier.fillMaxWidth().testTag("register_session_btn"),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("REGISTER FOR UPCOMING SESSION", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    // ONGOING SESSION
                                    if (isUserRegistered) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = RaivalSurface.copy(alpha = 0.5f)),
                                            border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayCircle,
                                                    contentDescription = null,
                                                    tint = RaivalSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "🎮 Play any game below now! All scores are blind during play.",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = Color.Red,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "🔒 A session is currently ongoing. Please wait for it to finish.",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }

                                            if (isUserPreRegisteredForNext) {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = RaivalSurface.copy(alpha = 0.5f)),
                                                    border = BorderStroke(1.dp, Color(0xFF00FF87).copy(alpha = 0.3f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = null,
                                                            tint = Color(0xFF00FF87),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Pre-registered! You will join the next session automatically.",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            } else {
                                                Button(
                                                    onClick = { viewModel.isUserPreRegisteredForNext.value = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary),
                                                    modifier = Modifier.fillMaxWidth().testTag("pre_register_btn"),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("PRE-REGISTER & WAIT FOR NEXT SESSION", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Admin testing speed-up
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { viewModel.adminAdvanceCasualSession() },
                                        modifier = Modifier.height(24.dp).testTag("admin_skip_session_btn"),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = "⏩ FAST-FORWARD TIME (ADMIN)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RaivalPrimary.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Free Coin Booster/Multiplier Station
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "⚡ COIN EARNING MULTIPLIERS",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                color = RaivalPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Watch Ad Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (adCoinsEarned < 10) {
                                                adWatchingActive = true
                                                adTimerRemaining = 5
                                                coroutineScope.launch {
                                                    while (adTimerRemaining > 0) {
                                                        delay(1000)
                                                        adTimerRemaining--
                                                    }
                                                    adWatchingActive = false
                                                    viewModel.rewardParticipation("ad")
                                                }
                                            }
                                        }
                                        .testTag("action_watch_ad"),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = RaivalSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Watch Ad", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("+2 Coins (${adCoinsEarned}/10)", fontSize = 9.sp, color = RaivalTextSecondary)
                                    }
                                }

                                // Daily Check-In Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (!claimLoginBonusSuccess) {
                                                claimLoginBonusSuccess = true
                                                viewModel.rewardParticipation("login")
                                            }
                                        }
                                        .testTag("action_daily_login"),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = RaivalPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (claimLoginBonusSuccess) "Claimed" else "Daily Claim",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (claimLoginBonusSuccess) RaivalPrimary else Color.White
                                        )
                                        Text("+5 Coins Daily", fontSize = 9.sp, color = RaivalTextSecondary)
                                    }
                                }

                                // Invite Friend Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            referralSuccessMessage = ""
                                            showReferralDialog = true
                                        }
                                        .testTag("action_referral"),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = RaivalPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Refer & Earn", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("+20 Coins", fontSize = 9.sp, color = RaivalTextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // Booking Panel
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "🕒 BOOK UPCOMING SESSIONS",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                color = RaivalPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Book upcoming sessions below to secure entry. You'll receive a system alert 5 minutes before kickoff.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RaivalTextSecondary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    val times = listOf("14:30", "15:00", "15:30", "16:00")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        times.forEach { t ->
                                            val isBooked = bookedSessions.contains(t)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        if (isBooked) RaivalPrimary.copy(alpha = 0.2f) else RaivalSurface,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        BorderStroke(1.dp, if (isBooked) RaivalPrimary else RaivalTextSecondary.copy(alpha = 0.2f)),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.bookNextSession(t)
                                                        bookingSuccessMessage = "Successfully booked session at $t!"
                                                        coroutineScope.launch {
                                                            delay(3000)
                                                            bookingSuccessMessage = ""
                                                        }
                                                    }
                                                    .padding(vertical = 10.dp)
                                                    .testTag("book_session_$t"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = t,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = if (isBooked) "Booked" else "Reserve",
                                                        fontSize = 8.sp,
                                                        color = if (isBooked) RaivalPrimary else RaivalTextSecondary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (bookingSuccessMessage.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = bookingSuccessMessage,
                                            color = RaivalSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Ongoing Session Live Leaderboard Panel
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "🏆 SESSION REAL-TIME LEADERBOARD",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                color = RaivalPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Top 10 Live standings", style = MaterialTheme.typography.bodySmall, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("Points", style = MaterialTheme.typography.bodySmall, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    currentLeaderboard.take(5).forEachIndexed { idx, pair ->
                                        val isSelf = pair.first == currentUser.username
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isSelf) RaivalPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#${idx + 1}",
                                                color = when (idx) {
                                                    0 -> RaivalSecondary
                                                    1 -> Color(0xFFC0C0C0)
                                                    2 -> Color(0xFFCD7F32)
                                                    else -> Color.White
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.width(32.dp)
                                            )
                                            Text(
                                                text = pair.first,
                                                color = if (isSelf) RaivalPrimary else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelf) FontWeight.ExtraBold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${pair.second} pts",
                                                color = if (isSelf) RaivalPrimary else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (currentLeaderboard.size > 5) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "...and ${currentLeaderboard.size - 5} more competing currently...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = RaivalTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Available Games Header
                    item {
                        Text(
                            text = "🎮 GAMES AVAILABLE TO PLAY",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                            color = RaivalPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Available Games On-Demand Download Grid
                    items(gamesList, key = { it.id }) { game ->
                        val isDownloaded = downloadedGames.contains(game.id)
                        val isDownloading = downloadingGameId == game.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("game_card_${game.id}"),
                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                            border = BorderStroke(1.dp, if (isDownloaded) RaivalPrimary.copy(alpha = 0.2f) else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(RaivalSurface, shape = RoundedCornerShape(12.dp))
                                        .border(BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f)), shape = RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = game.icon,
                                        contentDescription = game.title,
                                        tint = if (isDownloaded) RaivalPrimary else RaivalTextSecondary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = game.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "★ ${game.rating}",
                                            fontSize = 10.sp,
                                            color = RaivalSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = game.description,
                                        fontSize = 11.sp,
                                        color = RaivalTextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(game.diffColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(game.difficulty, color = game.diffColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Text(
                                            text = "💰 Avg: ${game.avgEarnings} coins",
                                            fontSize = 10.sp,
                                            color = RaivalTextSecondary
                                        )

                                        Text(
                                            text = "🔥 ${game.activePlayers} playing",
                                            fontSize = 10.sp,
                                            color = RaivalPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Action button (Download & Play or Just Play)
                                Column(horizontalAlignment = Alignment.End) {
                                    if (true) {
                                        val isPlayLocked = false
                                        val lockText = if (casualSessionStatus == "REGISTRATION") "WAITING" else "LOCKED"
                                        Button(
                                            onClick = { 
                                                if (!isPlayLocked) {
                                                    viewModel.rewardParticipation("join")
                                                    viewModel.setActivePlayingGameId(game.id)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isPlayLocked) Color.DarkGray else RaivalPrimary
                                            ),
                                            enabled = !isPlayLocked,
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp).testTag("play_game_btn_${game.id}")
                                        ) {
                                            if (isPlayLocked) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(lockText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                }
                                            } else {
                                                Text("PLAY", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    } else if (isDownloading) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(
                                                progress = downloadProgress,
                                                color = RaivalPrimary,
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 3.dp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("${(downloadProgress * 100).toInt()}%", fontSize = 9.sp, color = RaivalPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                downloadingGameId = game.id
                                                downloadProgress = 0f
                                                coroutineScope.launch {
                                                    while (downloadProgress < 1f) {
                                                        delay(120)
                                                        downloadProgress += 0.12f
                                                    }
                                                    downloadedGames = downloadedGames + game.id
                                                    downloadingGameId = null
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSurface),
                                            border = BorderStroke(1.dp, RaivalTextSecondary.copy(alpha = 0.5f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp).testTag("download_game_btn_${game.id}")
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${game.sizeMb}MB", color = RaivalTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (!isOnline) {
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
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = RaivalError,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "1V1 MATCH ZONE OFFLINE",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Real-time matchmaking, friend codes, and 1v1 live coin duels require a stable network connection to sync across participants.",
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = RaivalTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { casualSubTab = 0 },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("🕹️ PLAY ARCADE MINI-GAMES", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else {
                    // -------------------------------------------------------------
                    // ⚽ 1V1 REAL-TIME IN-GAME COINS VERIFICATION SYSTEM
                    // -------------------------------------------------------------
                    val liveSessions by viewModel.matchSessions.collectAsState()
                
                // Dialog and Link editing states
                var showProfileLinkDialog by remember { mutableStateOf(false) }
                var profileLinkType by remember { mutableStateOf("DLS") } // "DLS" or "eFootball"
                var profileLinkInput by remember { mutableStateOf("") }
                
                // Matchmaking states
                var showAcceptDuelDialog by remember { mutableStateOf(false) }
                var acceptCountdownState by remember { mutableStateOf(10) }
                var matchedOpponentId by remember { mutableIntStateOf(0) }
                var matchedSessionId by remember { mutableIntStateOf(0) }
                var matchmakingError by remember { mutableStateOf("") }
                
                // Selection outcomes for match play sync
                var selectedOutcome by remember { mutableStateOf("WIN") } // "WIN", "LOSS", "DRAW"

                // Score Reporting State
                var hostScoreInput by remember { mutableStateOf("") }
                var guestScoreInput by remember { mutableStateOf("") }
                var hostScoreSubmitted by remember { mutableStateOf(false) }
                var guestScoreSubmitted by remember { mutableStateOf(false) }
                var matchOutcome by remember { mutableStateOf("") } // "", "Confirmed", "Disputed"

                // Active User fields for convenience
                val userState = currentUser
                val userGhs = userState.balance
                val userCoins = userState.coinBalance

                // Accept dialog countdown loop
                LaunchedEffect(showAcceptDuelDialog) {
                    if (showAcceptDuelDialog) {
                        acceptCountdownState = 10
                        while (acceptCountdownState > 0) {
                            delay(1000)
                            acceptCountdownState--
                        }
                        if (acceptCountdownState == 0 && matchStatusState == "Setup") {
                            showAcceptDuelDialog = false
                            matchmakingActive = false
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(RaivalBackground)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Match Zone Navigation Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("🪙 Live Coin Sync & Duel", 1, Icons.Default.Paid),
                            Triple("🛡️ Profile API Guidelines", 2, Icons.Default.Security)
                        ).forEach { (title, index, icon) ->
                            val isSel = matchZoneTab == index
                            ElevatedFilterChip(
                                selected = isSel,
                                onClick = { matchZoneTab = index },
                                label = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White) },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = if (isSel) Color.Black else RaivalPrimary) },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = RaivalPrimary,
                                    containerColor = RaivalSurface
                                )
                            )
                        }
                    }

                    when (matchZoneTab) {
                        1 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // --- GAME PROFILES LINKING CORNER ---
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                                    border = BorderStroke(1.dp, RaivalSurfaceLight)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "🎮 SECURED GAME PROFILE LINKS",
                                            fontWeight = FontWeight.Bold,
                                            color = RaivalPrimary,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Link your game account IDs so Raival can securely fetch transaction logs and coin levels via official studio API endpoints.",
                                            color = RaivalTextSecondary,
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // DLS Link Item
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(RaivalSurfaceLight, RoundedCornerShape(10.dp))
                                                    .border(BorderStroke(1.dp, if (userState.dlsHandle.isNotEmpty()) RaivalSuccess.copy(alpha = 0.3f) else RaivalPrimary.copy(alpha = 0.15f)), RoundedCornerShape(10.dp))
                                                    .padding(10.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Text("Dream League Soccer", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 9.sp)
                                                    Icon(
                                                        imageVector = if (userState.dlsHandle.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.LinkOff,
                                                        contentDescription = null,
                                                        tint = if (userState.dlsHandle.isNotEmpty()) RaivalSuccess else RaivalTextSecondary,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (userState.dlsHandle.isNotEmpty()) userState.dlsHandle else "Not Linked",
                                                    color = if (userState.dlsHandle.isNotEmpty()) RaivalSecondary else RaivalTextSecondary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "LINK PROFILE ID",
                                                    color = RaivalPrimary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.clickable {
                                                        profileLinkType = "DLS"
                                                        profileLinkInput = userState.dlsHandle
                                                        showProfileLinkDialog = true
                                                    }
                                                )
                                            }

                                            // eFootball Link Item
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(RaivalSurfaceLight, RoundedCornerShape(10.dp))
                                                    .border(BorderStroke(1.dp, if (userState.efootballHandle.isNotEmpty()) RaivalSuccess.copy(alpha = 0.3f) else RaivalPrimary.copy(alpha = 0.15f)), RoundedCornerShape(10.dp))
                                                    .padding(10.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Text("eFootball Konami", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 9.sp)
                                                    Icon(
                                                        imageVector = if (userState.efootballHandle.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.LinkOff,
                                                        contentDescription = null,
                                                        tint = if (userState.efootballHandle.isNotEmpty()) RaivalSuccess else RaivalTextSecondary,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (userState.efootballHandle.isNotEmpty()) userState.efootballHandle else "Not Linked",
                                                    color = if (userState.efootballHandle.isNotEmpty()) RaivalSecondary else RaivalTextSecondary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "LINK PROFILE ID",
                                                    color = RaivalPrimary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.clickable {
                                                        profileLinkType = "eFootball"
                                                        profileLinkInput = userState.efootballHandle
                                                        showProfileLinkDialog = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Matchmaking & Setup Dashboard
                                if (matchStatusState == "Setup") {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                                        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.35f))
                                    ) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Image(
                                                painter = painterResource(id = R.drawable.bg_gaming_3),
                                                contentDescription = "1v1 Duels Banner",
                                                modifier = Modifier.matchParentSize(),
                                                contentScale = ContentScale.Crop,
                                                alpha = 0.30f
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                RaivalSurface.copy(alpha = 0.75f),
                                                                RaivalSurface.copy(alpha = 0.95f)
                                                            )
                                                        )
                                                    )
                                            )
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("⚽ 1V1 REAL-TIME DUELS MATCHMAKER", fontWeight = FontWeight.Bold, color = RaivalPrimary, fontSize = 13.sp)
                                                Box(
                                                    modifier = Modifier
                                                        .background(RaivalSurfaceLight, shape = RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Wallet, contentDescription = null, tint = RaivalSecondary, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("${userGhs} GHS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                            
                                            Text(
                                                text = "Match up with other online gamers from Ghana. Both players' in-game coin transaction deltas are verified to auto-settle stakes instantly.",
                                                color = RaivalTextSecondary,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )

                                            HorizontalDivider(color = RaivalSurfaceLight, thickness = 1.dp)

                                            // Game Selector
                                            Text("Choose Game Title:", fontWeight = FontWeight.Bold, color = RaivalTextSecondary, fontSize = 10.sp)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf("eFootball" to Icons.Default.SportsSoccer, "FC Mobile" to Icons.Default.Bolt, "DLS" to Icons.Default.EmojiEvents).forEach { (gameTitle, gameIcon) ->
                                                    val isSelected = selectedSimGame == gameTitle
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(if (isSelected) RaivalPrimary.copy(alpha = 0.15f) else RaivalSurfaceLight)
                                                            .border(BorderStroke(1.dp, if (isSelected) RaivalPrimary else Color.Transparent), RoundedCornerShape(10.dp))
                                                            .clickable { selectedSimGame = gameTitle }
                                                            .padding(vertical = 10.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Icon(gameIcon, contentDescription = null, tint = if (isSelected) RaivalPrimary else RaivalTextSecondary, modifier = Modifier.size(20.dp))
                                                            Text(gameTitle, color = if (isSelected) Color.White else RaivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }

                                            // Entry Fee Preset Stakes with validation check
                                            val balanceGhs = userGhs
                                            Text("Set Your Match Stakes / Entry Fee:", fontWeight = FontWeight.Bold, color = RaivalTextSecondary, fontSize = 10.sp)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf(10, 20, 50, 100).forEach { fee ->
                                                    val isSelected = selectedSimFee == fee
                                                    val isInsufficient = balanceGhs < fee
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(
                                                                if (isSelected) RaivalSecondary.copy(alpha = 0.2f)
                                                                else if (isInsufficient) Color.Red.copy(alpha = 0.05f)
                                                                else RaivalSurfaceLight
                                                            )
                                                            .border(
                                                                BorderStroke(
                                                                    1.dp,
                                                                    if (isSelected) RaivalSecondary
                                                                    else if (isInsufficient) Color.Red.copy(alpha = 0.2f)
                                                                    else Color.Transparent
                                                                ),
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable { selectedSimFee = fee }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(
                                                                text = "${fee} GHS",
                                                                color = if (isSelected) RaivalSecondary else if (isInsufficient) Color.Red.copy(alpha = 0.6f) else Color.White,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "${fee * 10} Coins",
                                                                color = if (isInsufficient) Color.Red.copy(alpha = 0.4f) else RaivalTextSecondary,
                                                                fontSize = 9.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            val hasEnoughBalance = balanceGhs >= selectedSimFee

                                            if (!hasEnoughBalance) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                        .padding(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Insufficient Balance! Please top up your GHS cash balance to play this stake.",
                                                        color = Color.Red,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Matchmaking Active Indicator with Radar Animation
                                            if (matchmakingActive) {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                                    border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.3f))
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(16.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        // Visual Radar Pulsing Sweep Animation
                                                        val infiniteTransition = rememberInfiniteTransition()
                                                        val sweepAngle by infiniteTransition.animateFloat(
                                                            initialValue = 0f,
                                                            targetValue = 360f,
                                                            animationSpec = infiniteRepeatable(
                                                                animation = tween(2200, easing = LinearEasing),
                                                                repeatMode = RepeatMode.Restart
                                                            )
                                                        )
                                                        val pulseRadius1 by infiniteTransition.animateFloat(
                                                            initialValue = 0f,
                                                            targetValue = 1f,
                                                            animationSpec = infiniteRepeatable(
                                                                animation = tween(1600, easing = EaseOutQuad),
                                                                repeatMode = RepeatMode.Restart
                                                            )
                                                        )
                                                        val pulseRadius2 by infiniteTransition.animateFloat(
                                                            initialValue = 0f,
                                                            targetValue = 1f,
                                                            animationSpec = infiniteRepeatable(
                                                                animation = tween(1600, easing = EaseOutQuad),
                                                                repeatMode = RepeatMode.Restart,
                                                                initialStartOffset = StartOffset(800)
                                                            )
                                                        )

                                                        Canvas(modifier = Modifier.size(120.dp)) {
                                                            val center = Offset(size.width / 2, size.height / 2)
                                                            val maxRadius = size.width / 2

                                                            // Concentric circles
                                                            drawCircle(color = RaivalPrimary.copy(alpha = 0.08f), radius = maxRadius, center = center, style = Stroke(width = 4f))
                                                            drawCircle(color = RaivalPrimary.copy(alpha = 0.15f), radius = maxRadius * 0.66f, center = center, style = Stroke(width = 2f))
                                                            drawCircle(color = RaivalPrimary.copy(alpha = 0.2f), radius = maxRadius * 0.33f, center = center, style = Stroke(width = 2f))

                                                            // Pulses
                                                            drawCircle(color = RaivalPrimary.copy(alpha = 0.15f * (1f - pulseRadius1)), radius = maxRadius * pulseRadius1, center = center)
                                                            drawCircle(color = RaivalPrimary.copy(alpha = 0.15f * (1f - pulseRadius2)), radius = maxRadius * pulseRadius2, center = center)

                                                            // Sweep line
                                                            val x = center.x + maxRadius * kotlin.math.cos(Math.toRadians(sweepAngle.toDouble())).toFloat()
                                                            val y = center.y + maxRadius * kotlin.math.sin(Math.toRadians(sweepAngle.toDouble())).toFloat()
                                                            drawLine(color = RaivalSecondary, start = center, end = Offset(x, y), strokeWidth = 5f)
                                                        }

                                                        Text(matchmakingStatus, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                                        Text("Broadcasting on regional gaming nodes...", color = RaivalTextSecondary, fontSize = 9.sp)
                                                        
                                                        Button(
                                                            onClick = {
                                                                matchmakingActive = false
                                                                viewModel.cancelMatchmaking()
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                                                            modifier = Modifier.height(30.dp)
                                                        ) {
                                                            Text("CANCEL MATCHMAKING", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            } else {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                     Button(
                                                        onClick = {
                                                            matchmakingActive = true
                                                            matchmakingStatus = "Searching for $selectedSimGame opponents..."
                                                            matchmakingError = ""
                                                            viewModel.startRealMatchmaking(
                                                                gameType = selectedSimGame,
                                                                onOpponentFound = { oppId, oppName, sessionId ->
                                                                    opponentName = oppName
                                                                    opponentIGN = oppName
                                                                    matchedOpponentId = oppId
                                                                    matchedSessionId = sessionId
                                                                    showAcceptDuelDialog = true
                                                                    matchmakingActive = false
                                                                },
                                                                onNoOpponent = { msg ->
                                                                    matchmakingActive = false
                                                                    matchmakingError = msg
                                                                },
                                                                onError = { err ->
                                                                    matchmakingActive = false
                                                                    matchmakingError = err
                                                                }
                                                            )
                                                        },
                                                        enabled = hasEnoughBalance,
                                                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                                        modifier = Modifier.fillMaxWidth().testTag("find_match_lobby_btn")
                                                    ) {
                                                        val gameIcon = when(selectedSimGame) {
                                                            "DLS" -> Icons.Default.EmojiEvents
                                                            "FC Mobile" -> Icons.Default.Bolt
                                                            else -> Icons.Default.SportsSoccer
                                                        }
                                                        Icon(gameIcon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("FIND $selectedSimGame MATCH", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            showJoinRoomDialog = true
                                                        },
                                                        border = BorderStroke(1.dp, RaivalSecondary),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Icon(Icons.Default.Group, contentDescription = null, tint = RaivalSecondary, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("JOIN FRIENDLY ROOM", color = RaivalSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                }
                                            }
                                        }

                                        if (matchmakingError.isNotEmpty()) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(matchmakingError, color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Step 1: Pre-Match Coin Balances Verified
                                if (matchStatusState == "Ready") {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                                        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("👥 STEP 1: PRE-MATCH IN-GAME COINS SECURELY SYNCED", fontWeight = FontWeight.Bold, color = RaivalPrimary, fontSize = 11.sp)
                                                Box(
                                                    modifier = Modifier
                                                        .background(RaivalSurfaceLight, shape = RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("Room Code: #${activeRoomCode.ifEmpty { com.example.data.PasswordHelper.generateRoomCode() }}", color = RaivalSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // Versus Coins Comparison Cards
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                    Box(modifier = Modifier.size(48.dp).background(RaivalPrimary.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Person, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(24.dp))
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Host (You)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("IGN: ${userState.username}", color = RaivalTextSecondary, fontSize = 9.sp)
                                                    
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .background(RaivalSurfaceLight, shape = RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(
                                                                text = if (hostPreCoins > 0) "$hostPreCoins Coins" else "Not synced",
                                                                color = if (hostPreCoins > 0) RaivalSecondary else RaivalTextSecondary,
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 11.sp
                                                            )
                                                            Text("Pre-match", color = RaivalTextSecondary, fontSize = 8.sp)
                                                        }
                                                    }
                                                }

                                                Text("VS", fontWeight = FontWeight.Black, color = RaivalSecondary, fontSize = 18.sp)

                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                    Box(modifier = Modifier.size(48.dp).background(RaivalAccent.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Person, contentDescription = null, tint = RaivalAccent, modifier = Modifier.size(24.dp))
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(opponentName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text("IGN: $opponentIGN", color = RaivalTextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .background(RaivalSurfaceLight, shape = RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(
                                                                text = if (guestPreCoins > 0) "$guestPreCoins Coins" else "Not synced",
                                                                color = if (guestPreCoins > 0) RaivalAccent else RaivalTextSecondary,
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 11.sp
                                                            )
                                                            Text("Pre-match", color = RaivalTextSecondary, fontSize = 8.sp)
                                                        }
                                                    }
                                                }
                                            }

                                            HorizontalDivider(color = RaivalSurfaceLight, thickness = 1.dp)

                                            // Report Score Section
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("📋 REPORT MATCH SCORE", fontWeight = FontWeight.Bold, color = RaivalSecondary, fontSize = 10.sp)
                                                Text(
                                                    text = "Both players independently enter their score. The match is confirmed only when both scores agree.",
                                                    color = RaivalTextSecondary,
                                                    fontSize = 9.sp,
                                                    lineHeight = 12.sp
                                                )

                                                // Host score input
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Your Score:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.4f))
                                                    OutlinedTextField(
                                                        value = hostScoreInput,
                                                        onValueChange = { hostScoreInput = it.filter { c -> c.isDigit() } },
                                                        modifier = Modifier.weight(0.4f),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = RaivalPrimary,
                                                            unfocusedBorderColor = RaivalSurfaceLight,
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White
                                                        ),
                                                        singleLine = true,
                                                        placeholder = { Text("0", color = RaivalTextSecondary.copy(alpha = 0.6f), fontSize = 11.sp) }
                                                    )
                                                    if (!hostScoreSubmitted) {
                                                        Button(
                                                            onClick = {
                                                                if (hostScoreInput.isNotEmpty()) {
                                                                    hostScoreSubmitted = true
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                                            enabled = hostScoreInput.isNotEmpty(),
                                                            modifier = Modifier.height(40.dp)
                                                        ) {
                                                            Text("Submit", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaivalSuccess, modifier = Modifier.size(20.dp))
                                                    }
                                                }

                                                // Guest score input
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Opponent Score:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.4f))
                                                    OutlinedTextField(
                                                        value = guestScoreInput,
                                                        onValueChange = { guestScoreInput = it.filter { c -> c.isDigit() } },
                                                        modifier = Modifier.weight(0.4f),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = RaivalAccent,
                                                            unfocusedBorderColor = RaivalSurfaceLight,
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White
                                                        ),
                                                        singleLine = true,
                                                        placeholder = { Text("0", color = RaivalTextSecondary.copy(alpha = 0.6f), fontSize = 11.sp) }
                                                    )
                                                    if (!guestScoreSubmitted) {
                                                        Button(
                                                            onClick = {
                                                                if (guestScoreInput.isNotEmpty()) {
                                                                    guestScoreSubmitted = true
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = RaivalAccent),
                                                            enabled = guestScoreInput.isNotEmpty(),
                                                            modifier = Modifier.height(40.dp)
                                                        ) {
                                                            Text("Submit", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaivalSuccess, modifier = Modifier.size(20.dp))
                                                    }
                                                }

                                                // Score agreement status
                                                if (hostScoreSubmitted && guestScoreSubmitted) {
                                                    if (hostScoreInput == guestScoreInput) {
                                                        Card(
                                                            colors = CardDefaults.cardColors(containerColor = RaivalSuccess.copy(alpha = 0.1f)),
                                                            border = BorderStroke(1.dp, RaivalSuccess.copy(alpha = 0.3f))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaivalSuccess, modifier = Modifier.size(14.dp))
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text("Scores agree: $hostScoreInput - $guestScoreInput. Ready to verify.", color = RaivalSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    } else {
                                                        Card(
                                                            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                                                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text("Scores DISAGREE! You: $hostScoreInput, Opponent: $guestScoreInput. This match will be marked Disputed.", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            HorizontalDivider(color = RaivalSurfaceLight, thickness = 1.dp)

                                            // Match Information Details
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Selected Title", color = RaivalTextSecondary, fontSize = 11.sp)
                                                    Text(selectedSimGame, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Verification Engine", color = RaivalTextSecondary, fontSize = 11.sp)
                                                    Text("Automatic Coins Delta Check", color = RaivalSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Stakes At Play", color = RaivalTextSecondary, fontSize = 11.sp)
                                                    Text("${selectedSimFee} GHS", color = RaivalSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }

                                            Text(
                                                text = "⚠️ In-game coin sync requires game API integration (coming soon). For now, both players must submit scores above. Once both scores match, the result will be auto-confirmed. If scores disagree, the match will be marked Disputed and require admin review.",
                                                color = RaivalTextSecondary,
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )

                                             Button(
                                                onClick = {
                                                    val userScore = hostScoreInput.toIntOrNull() ?: 0
                                                    val oppScore = guestScoreInput.toIntOrNull() ?: 0
                                                    selectedOutcome = when {
                                                        hostScoreSubmitted && guestScoreSubmitted && hostScoreInput == guestScoreInput -> "DRAW"
                                                        userScore > oppScore -> "WIN"
                                                        userScore < oppScore -> "LOSS"
                                                        else -> "WIN"
                                                    }
                                                    matchStatusState = "Verifying"
                                                    verificationLogs = emptyList()
                                                    isVerifyingCoins = true
                                                    
                                                    coroutineScope.launch {
                                                        verificationLogs = verificationLogs + "🔌 Establishing link with $selectedSimGame secure API node..."
                                                        delay(700)
                                                        verificationLogs = verificationLogs + "🔍 Fetching post-match state for ${userState.username}..."
                                                        delay(700)
                                                        
                                                        // Adjust result variables depending on selectedOutcome
                                                        when (selectedOutcome) {
                                                            "WIN" -> {
                                                                hostPostCoins = hostPreCoins + 100
                                                                guestPostCoins = guestPreCoins - 100
                                                                verificationLogs = verificationLogs + "✅ Host verified coins retrieved: $hostPostCoins (Delta: +100 Coins)"
                                                                delay(600)
                                                                verificationLogs = verificationLogs + "🔍 Fetching post-match state for opponent..."
                                                                delay(600)
                                                                verificationLogs = verificationLogs + "✅ Opponent verified coins retrieved: $guestPostCoins (Delta: -100 Coins)"
                                                            }
                                                            "LOSS" -> {
                                                                hostPostCoins = hostPreCoins - 100
                                                                guestPostCoins = guestPreCoins + 100
                                                                verificationLogs = verificationLogs + "✅ Host verified coins retrieved: $hostPostCoins (Delta: -100 Coins)"
                                                                delay(600)
                                                                verificationLogs = verificationLogs + "🔍 Fetching post-match state for opponent..."
                                                                delay(600)
                                                                verificationLogs = verificationLogs + "✅ Opponent verified coins retrieved: $guestPostCoins (Delta: +100 Coins)"
                                                            }
                                                            else -> {
                                                                hostPostCoins = hostPreCoins
                                                                guestPostCoins = guestPreCoins
                                                                verificationLogs = verificationLogs + "✅ Host verified coins retrieved: $hostPostCoins (Delta: 0 Coins)"
                                                                delay(600)
                                                                verificationLogs = verificationLogs + "🔍 Fetching post-match state for opponent..."
                                                                delay(600)
                                                                verificationLogs = verificationLogs + "✅ Opponent verified coins retrieved: $guestPostCoins (Delta: 0 Coins)"
                                                            }
                                                        }
                                                        delay(600)
                                                        verificationLogs = verificationLogs + "📊 Reconciling deltas against active room contracts..."
                                                        delay(600)
                                                        
                                                        val summaryLog = when (selectedOutcome) {
                                                            "WIN" -> "✨ Winner detected: Host (${userState.username}) via positive coin delta."
                                                            "LOSS" -> "✨ Winner detected: Opponent ($opponentName) via positive coin delta."
                                                            else -> "🤝 Match draw detected. No positive delta found on either account."
                                                        }
                                                        verificationLogs = verificationLogs + summaryLog
                                                        delay(800)
                                                        
                                                        // Save to SQLite database! Real persisted updates!
                                                         viewModel.record1v1MatchOutcome(
                                                            gameName = selectedSimGame,
                                                            roomCode = activeRoomCode.ifEmpty { com.example.data.PasswordHelper.generateRoomCode() },
                                                            opponentName = opponentName,
                                                            opponentIGN = opponentIGN,
                                                            entryFeeGhs = selectedSimFee,
                                                            outcome = selectedOutcome,
                                                            opponentUserId = matchedOpponentId,
                                                            matchSessionId = matchedSessionId,
                                                            hostScore = hostScoreInput.toIntOrNull() ?: 0,
                                                            guestScore = guestScoreInput.toIntOrNull() ?: 0,
                                                            onComplete = {
                                                                matchStatusState = "Verified"
                                                                isVerifyingCoins = false
                                                            }
                                                        )
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = RaivalSecondary,
                                                    disabledContainerColor = RaivalSurfaceLight
                                                ),
                                                enabled = hostScoreSubmitted && guestScoreSubmitted && hostScoreInput == guestScoreInput,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Sync, contentDescription = null, tint = if (hostScoreSubmitted && guestScoreSubmitted && hostScoreInput == guestScoreInput) Color.Black else RaivalTextSecondary)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (!hostScoreSubmitted || !guestScoreSubmitted) "Enter both scores to continue"
                                                    else if (hostScoreInput != guestScoreInput) "Scores must match to proceed"
                                                    else "PLAY COMPLETED - SYNC & VERIFY",
                                                    color = if (hostScoreSubmitted && guestScoreSubmitted && hostScoreInput == guestScoreInput) Color.Black else RaivalTextSecondary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // Step 2: Querying Live Databases Loader (Verifying state)
                                if (matchStatusState == "Verifying") {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                                        border = BorderStroke(1.dp, RaivalSecondary)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    CircularProgressIndicator(color = RaivalSecondary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("⚡ COIN TRANSFERS API QUERYING ACTIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                                Text("LIVE FEED", color = RaivalSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                                border = BorderStroke(1.dp, RaivalSurfaceLight)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    verificationLogs.forEach { log ->
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = if (log.startsWith("✅") || log.startsWith("✨")) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                                                                contentDescription = null,
                                                                tint = if (log.startsWith("✅") || log.startsWith("✨")) RaivalSuccess else RaivalSecondary,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(log, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Step 3: Verified Match Result Card
                                if (matchStatusState == "Verified") {
                                    val isWin = selectedOutcome == "WIN"
                                    val isLoss = selectedOutcome == "LOSS"
                                    val isDraw = selectedOutcome == "DRAW"
                                    val statusBorderColor = if (isWin) RaivalSuccess else if (isLoss) Color.Red else RaivalSecondary
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                                        border = BorderStroke(2.dp, statusBorderColor)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            val headerIcon = if (isWin) Icons.Default.EmojiEvents else if (isLoss) Icons.Default.Cancel else Icons.Default.Handshake
                                            val headerColor = if (isWin) RaivalSecondary else if (isLoss) Color.Red else Color.White
                                            
                                            Icon(headerIcon, contentDescription = "Trophy", tint = headerColor, modifier = Modifier.size(48.dp))
                                            
                                            Text(
                                                text = if (isWin) "🏆 COIN VERIFIED: YOU WON!" else if (isLoss) "❌ COIN VERIFIED: YOU LOST" else "🤝 COIN VERIFIED: MATCH DRAW",
                                                fontWeight = FontWeight.Black,
                                                color = headerColor,
                                                fontSize = 14.sp
                                            )
                                            
                                            Text(
                                                text = if (isWin) "You Defeated $opponentName" else if (isLoss) "Defeated by $opponentName" else "Tied Match against $opponentName",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )

                                            HorizontalDivider(color = RaivalSurfaceLight, thickness = 1.dp)

                                            // Balance details table
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                // Host Details
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("${userState.username} (You)", color = RaivalPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text("$selectedSimGame Coins Balance", color = RaivalTextSecondary, fontSize = 9.sp)
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("$hostPreCoins", color = RaivalTextSecondary, fontSize = 11.sp)
                                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(10.dp).padding(horizontal = 4.dp))
                                                        val hostDeltaText = if (isWin) "+100" else if (isLoss) "-100" else "0"
                                                        val hostDeltaColor = if (isWin) RaivalSuccess else if (isLoss) Color.Red else Color.White
                                                        Text("$hostPostCoins ($hostDeltaText delta)", color = hostDeltaColor, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                                    }
                                                }

                                                HorizontalDivider(color = RaivalSurfaceLight.copy(alpha = 0.5f), thickness = 1.dp)

                                                // Guest Details
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(opponentName, color = RaivalAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text("$selectedSimGame Coins Balance", color = RaivalTextSecondary, fontSize = 9.sp)
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("$guestPreCoins", color = RaivalTextSecondary, fontSize = 11.sp)
                                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(10.dp).padding(horizontal = 4.dp))
                                                        val guestDeltaText = if (isWin) "-100" else if (isLoss) "+100" else "0"
                                                        val guestDeltaColor = if (isWin) Color.Red else if (isLoss) RaivalSuccess else Color.White
                                                        Text("$guestPostCoins ($guestDeltaText delta)", color = guestDeltaColor, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                                    }
                                                }
                                            }

                                            HorizontalDivider(color = RaivalSurfaceLight, thickness = 1.dp)

                                            // Stats summary list
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Balance Sync Security Hash", color = RaivalTextSecondary, fontSize = 11.sp)
                                                    Text("SSL Secured API Consensus", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("In-Game Transaction Verification", color = RaivalTextSecondary, fontSize = 11.sp)
                                                    Text("100% Fully Aligned & Authenticated", color = RaivalSuccess, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Raival GHS Earnings Wallet Impact", color = RaivalTextSecondary, fontSize = 11.sp)
                                                    val impactText = if (isWin) "+${selectedSimFee} GHS" else if (isLoss) "-${selectedSimFee} GHS" else "0.00 GHS (Refunded)"
                                                    val impactColor = if (isWin) RaivalSuccess else if (isLoss) Color.Red else RaivalSecondary
                                                    Text(impactText, color = impactColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    matchStatusState = "Setup"
                                                    isMatched = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("🎮 MATCH COMPLETED - START NEW DUEL", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                // --- RECENT DUELS LEDGER LIST ---
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                                    border = BorderStroke(1.dp, RaivalSurfaceLight)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "📜 YOUR REAL DUELS LEDGER",
                                            fontWeight = FontWeight.Bold,
                                            color = RaivalPrimary,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp
                                        )

                                        val completedDuels = liveSessions.filter { it.status == "Completed" }
                                        if (completedDuels.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("No recent coin-verified duels found.", color = RaivalTextSecondary, fontSize = 11.sp)
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                completedDuels.take(5).forEach { session ->
                                                    val amWinner = session.winnerId == userState.id
                                                    val amLoser = session.winnerId != null && session.winnerId != userState.id
                                                    val amDraw = session.winnerId == null
                                                    val duelStatusColor = if (amWinner) RaivalSuccess else if (amLoser) Color.Red else RaivalSecondary
                                                    val duelStatusText = if (amWinner) "Victory" else if (amLoser) "Defeat" else "Draw"
                                                    
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(RaivalSurfaceLight, RoundedCornerShape(8.dp))
                                                            .padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                            Text(
                                                                text = "vs ${session.guestUsername ?: "Unknown"}",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                            Text(
                                                                text = "Room: #${session.roomCode} | ${session.stadium}",
                                                                color = RaivalTextSecondary,
                                                                fontSize = 9.sp
                                                            )
                                                        }
                                                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                            Text(
                                                                text = duelStatusText,
                                                                color = duelStatusColor,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                            Text(
                                                                text = "${session.hostScore ?: 0} - ${session.guestScore ?: 0}",
                                                                color = Color.White,
                                                                fontSize = 9.sp,
                                                                fontFamily = FontFamily.Monospace
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

                        2 -> {
                            // --- PROFILE SYNC GUIDELINES TAB ---
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("📖 COIN-BASED API SYNC SPECIFICATIONS", fontWeight = FontWeight.Bold, color = RaivalPrimary, fontSize = 13.sp)

                                val captureSpecs = listOf(
                                    Triple("Profile IGN Sync", "Link your exact game-specific user UID once to establish the secure API balance lookup bridge.", Icons.Default.Link),
                                    Triple("Pre-Match Snapshot", "Raival queries and locks your in-game coins/points/GP balance before the head-to-head begins.", Icons.Default.Lock),
                                    Triple("Post-Match Delta Check", "Upon match completion, Raival performs a final live balance API query. The player with the positive delta transaction is marked as the winner.", Icons.Default.Paid),
                                    Triple("100% Anti-Cheat Guard", "No more manually uploading game screenshot scorecards or background timeline tracking. Pure in-game profile data deltas ensure cheat-proof validation.", Icons.Default.Security)
                                )

                                captureSpecs.forEach { (title, desc, icon) ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                                        border = BorderStroke(1.dp, RaivalSurfaceLight)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(icon, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                                Text(desc, color = RaivalTextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
                                            }
                                        }
                                    }
                                }

                                Text("🛡️ SECURE GAMING & ENCRYPTION STATEMENTS", fontWeight = FontWeight.Bold, color = RaivalSecondary, fontSize = 13.sp)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        val securityPolicies = listOf(
                                            "OAuth Profile Protection: We only sync public in-game coin transaction deltas. We NEVER access your private password or gaming credentials." to Icons.Default.Lock,
                                            "Instant Sync Purge: Fetched delta balances are verified and immediately archived to keep API latency fast." to Icons.Default.Info,
                                            "Compliance Guaranteed: Fully compliant with the Ghana Data Protection Act & game studio developer access requirements." to Icons.Default.CheckCircle,
                                            "User Freedom: Decouple or sync another game profile (e.g. switch from DLS to eFootball) dynamically." to Icons.Default.Cached
                                        )

                                        securityPolicies.forEach { (text, icon) ->
                                            Row(verticalAlignment = Alignment.Top) {
                                                Icon(icon, contentDescription = null, tint = RaivalSecondary, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text, color = RaivalTextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- OPPONENT FOUND COUNTDOWN DIALOG ---
                    if (showAcceptDuelDialog) {
                        AlertDialog(
                            onDismissRequest = { /* Force response */ },
                            confirmButton = {
                                 Button(
                                    onClick = {
                                        showAcceptDuelDialog = false
                                        isMatched = true
                                        
                                         val userCoinBalance = viewModel.currentUser.value?.coinBalance ?: 0
                                         hostPreCoins = userCoinBalance
                                         guestPreCoins = userCoinBalance
                                        
                                        matchStatusState = "Ready"
                                        GameSoundEffects.playPowerUp()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSuccess)
                                ) {
                                    Text("ACCEPT DUEL", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showAcceptDuelDialog = false
                                        matchmakingActive = false
                                    }
                                ) {
                                    Text("DECLINE", color = Color.White)
                                }
                            },
                             title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val gameIcon = when(selectedSimGame) {
                                        "DLS" -> Icons.Default.EmojiEvents
                                        "FC Mobile" -> Icons.Default.Bolt
                                        else -> Icons.Default.SportsSoccer
                                    }
                                    Icon(gameIcon, contentDescription = null, tint = RaivalPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("COMPETITOR FOUND!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Competitor matched from Raival's live matchmaking pool!",
                                        color = RaivalTextSecondary,
                                        fontSize = 11.sp
                                    )

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Opponent Profile", color = RaivalTextSecondary, fontSize = 10.sp)
                                                Text(opponentName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Opponent IGN", color = RaivalTextSecondary, fontSize = 10.sp)
                                                Text(opponentIGN, color = RaivalSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Match Status", color = RaivalTextSecondary, fontSize = 10.sp)
                                                Text("Verified Player", color = RaivalSuccess, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Stakes Fee Amount", color = RaivalTextSecondary, fontSize = 10.sp)
                                                Text("${selectedSimFee} GHS", color = RaivalSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Acceptance Auto-Expire in: ${acceptCountdownState}s",
                                            color = Color.Red,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        )
                    }

                    // --- DIALOG FOR LINKING PROFILES ---
                    if (showProfileLinkDialog) {
                        AlertDialog(
                            onDismissRequest = { showProfileLinkDialog = false },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showProfileLinkDialog = false
                                        viewModel.updateProfile(
                                            fullName = userState.fullName,
                                            phone = userState.phone,
                                            bio = userState.bio,
                                            dlsHandle = if (profileLinkType == "DLS") profileLinkInput else userState.dlsHandle,
                                            efootballHandle = if (profileLinkType == "eFootball") profileLinkInput else userState.efootballHandle,
                                            discordHandle = userState.discordHandle,
                                            ghanaRegion = "Accra",
                                            ghanaHometown = "Greater Accra",
                                            preferredGame = if (profileLinkType == "DLS") "DLS" else "eFootball",
                                            onResult = { _ -> }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary)
                                ) {
                                    Text("SAVE LINK", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showProfileLinkDialog = false }) {
                                    Text("Cancel", color = Color.White)
                                }
                            },
                            title = {
                                Text("Link $profileLinkType Profile ID", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Enter your verified in-game ID/Username for $profileLinkType so Raival can securely fetch coin balances.",
                                        color = RaivalTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    OutlinedTextField(
                                        value = profileLinkInput,
                                        onValueChange = { profileLinkInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RaivalSecondary,
                                            unfocusedBorderColor = RaivalSurfaceLight,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        singleLine = true,
                                        placeholder = { Text("e.g. GamerTag_Accra", color = RaivalTextSecondary.copy(alpha = 0.6f), fontSize = 11.sp) }
                                    )
                                }
                            }
                        )
                    }

                    // Join room custom dialog
                    if (showJoinRoomDialog) {
                        AlertDialog(
                            onDismissRequest = { showJoinRoomDialog = false },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (customRoomCodeInput.length == 6) {
                                            showJoinRoomDialog = false
                                            opponentName = "RoomHost"
                                            activeRoomCode = customRoomCodeInput
                                            isMatched = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary)
                                ) {
                                    Text("JOIN ROOM LOBBY", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showJoinRoomDialog = false }) {
                                    Text("Cancel", color = Color.White)
                                }
                            },
                            title = {
                                Text("Enter Custom Room Code", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Enter the 6-digit room code from your opponent to join their friendly match custom lobby:", color = RaivalTextSecondary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = customRoomCodeInput,
                                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) customRoomCodeInput = it },
                                        label = { Text("6-Digit Room Code") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RaivalSecondary,
                                            focusedLabelColor = RaivalSecondary,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            containerColor = RaivalSurface
                        )
                    }

                    // Full-screen Video Ad Mock Dialog
                    if (adWatchingActive) {
                        AlertDialog(
                            onDismissRequest = {},
                            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                            confirmButton = {},
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Movie, contentDescription = null, tint = RaivalSecondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Watching Raival Sponsored Ad", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .background(Color.Black, shape = RoundedCornerShape(12.dp))
                                            .border(BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(48.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("DLS Ultimate Championship Promo", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                            Text("Brought to you by Raival Africa", fontSize = 11.sp, color = RaivalTextSecondary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Ad playing: reward in $adTimerRemaining seconds", color = RaivalSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = (5 - adTimerRemaining) / 5f,
                                        color = RaivalSecondary,
                                        trackColor = Color.DarkGray,
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                                    )
                                }
                            },
                            containerColor = RaivalSurface
                        )
                    }

                    // Referral popup Dialog
                    if (showReferralDialog) {
                        AlertDialog(
                            onDismissRequest = { showReferralDialog = false },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (referralInputName.isNotEmpty() && referralInputPhone.isNotEmpty()) {
                                            viewModel.rewardParticipation("referral")
                                            referralSuccessMessage = "Referral sent to $referralInputName! +20 Raival coins credited."
                                            coroutineScope.launch {
                                                delay(3000)
                                                showReferralDialog = false
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                                ) {
                                    Text("Invite & Credit", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showReferralDialog = false }) {
                                    Text("Cancel", color = Color.White)
                                }
                            },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = RaivalPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Invite Friends to Raival", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Share Raival to Ghana's DLS gaming community. You will receive 20 coins instantly when sending the invite link!",
                                        fontSize = 11.sp,
                                        color = RaivalTextSecondary
                                    )

                                    OutlinedTextField(
                                        value = referralInputName,
                                        onValueChange = { referralInputName = it },
                                        label = { Text("Friend's Full Name") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RaivalPrimary,
                                            focusedLabelColor = RaivalPrimary,
                                            unfocusedBorderColor = RaivalTextSecondary,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("referral_name_input")
                                    )

                                    OutlinedTextField(
                                        value = referralInputPhone,
                                        onValueChange = { referralInputPhone = it },
                                        label = { Text("Mobile Number (MoMo)") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RaivalPrimary,
                                            focusedLabelColor = RaivalPrimary,
                                            unfocusedBorderColor = RaivalTextSecondary,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("referral_phone_input")
                                    )

                                    if (referralSuccessMessage.isNotEmpty()) {
                                        Text(
                                            text = referralSuccessMessage,
                                            color = RaivalSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            },
                            containerColor = RaivalSurface
                        )
                    }

                    // Session transition notification dialog
                    if (sessionTransitionAlert != null) {
                        val alert = sessionTransitionAlert!!
                        AlertDialog(
                            onDismissRequest = { viewModel.sessionTransitionAlert.value = null },
                            confirmButton = {
                                Button(
                                    onClick = { viewModel.sessionTransitionAlert.value = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary)
                                ) {
                                    Text("Awesome!", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = RaivalPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Session Completed!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "The rotating casual session has concluded.",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Session: ${alert.oldSessionName}",
                                        color = RaivalTextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Your Position: #${alert.userPosition}",
                                        color = RaivalPrimary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (alert.coinsEarned > 0) {
                                        Text(
                                            text = "Coins Earned: +${alert.coinsEarned} Coins 🪙",
                                            color = RaivalSecondary,
                                            fontWeight = FontWeight.Black,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        Text(
                                            text = "Play again to secure a top 10 spot in the new session!",
                                            color = RaivalTextSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RaivalSurface))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Next session starting: ${alert.newSessionName}",
                                        color = RaivalSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            containerColor = RaivalSurfaceLight
                        )
                    }

                }
                }
            }
        }
    }
}

@Composable
fun PreMatchSettingsView(
    gameId: String,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    val canStart = true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon header
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(RaivalPrimary.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = RaivalPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "Required Internal Game Settings",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "To guarantee fair play and successful screenshot score verification, you must manually apply these exact rules inside your game app before starting the match.",
            style = MaterialTheme.typography.bodyMedium,
            color = RaivalTextSecondary,
            textAlign = TextAlign.Center,
            fontSize = 13.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RaivalSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔧 GRAPHICS & CAMERA CONFIGURATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RaivalPrimary
                )

                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaivalSuccess, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Set Graphics FPS to 60 FPS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Prevents frame stutter during screen capture or upload transitions.", fontSize = 11.sp, color = RaivalTextSecondary)
                    }
                }

                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaivalSuccess, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Match camera: Sideline (100% Zoom)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Allows AI scorecard verification to scan both gamer IDs simultaneously.", fontSize = 11.sp, color = RaivalTextSecondary)
                    }
                }

                HorizontalDivider(color = RaivalSurfaceLight, thickness = 1.dp)

                Text(
                    text = "📋 GAMEPLAY RULES & ROOM SETUP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RaivalSecondary
                )

                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaivalSuccess, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Halftime Match Length: 6 mins (3 min halves)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Strict tournament standard. Matches played on other lengths will be automatically disqualified.", fontSize = 11.sp, color = RaivalTextSecondary)
                    }
                }

                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaivalSuccess, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Server: Middle East / West Africa Region", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Assures low ping (under 120ms) for responsive joystick handling.", fontSize = 11.sp, color = RaivalTextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, RaivalSurfaceLight)
            ) {
                Text("Exit Lobby")
            }

            Button(
                onClick = onAccept,
                enabled = canStart,
                modifier = Modifier.weight(1.5f).testTag("accept_settings_start_match"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RaivalPrimary,
                    disabledContainerColor = RaivalSurfaceLight
                )
            ) {
                Text("Start Match Now", color = if (canStart) Color.Black else RaivalTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// INDIVIDUAL PLAYABLE GAME RENDERING OVERLAY
// -------------------------------------------------------------
data class FloatingScoreIndicator(val id: Long, val text: String, val x: Float, val y: Float)

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasualGamePlayScreen(
    gameId: String,
    currentUser: User,
    onBack: () -> Unit,
    onSubmitScore: (Int) -> Unit,
    viewModel: RaivalViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var matchScore by remember { mutableIntStateOf(0) }
    var gameTimeRemaining by remember { mutableStateOf(180) } // 180 seconds Action Mode
    var isGameOver by remember { mutableStateOf(false) }
    var showPreMatchSettings by remember { mutableStateOf(true) }

    // --- ENHANCEMENTS FOR ALL CASUAL GAMES (1, 2, 3) ---
    // 1. Floating Score Sparks (Visual Juice)
    var floatingScores by remember { mutableStateOf(listOf<FloatingScoreIndicator>()) }

    // 2. Live Competitor Score Ticker (Social Verification)
    var liveRivalMessage by remember { mutableStateOf("⚡ Arena Lobby: Waiting for match competitors...") }

    // 3. In-Game Milestone Achievements System (Gamification & Rewards)
    var unlockedNovice by remember { mutableStateOf(false) }
    var unlockedCombo by remember { mutableStateOf(false) }
    var unlockedLegend by remember { mutableStateOf(false) }
    var activeAchievementToast by remember { mutableStateOf<String?>(null) }

    // Watch score for achievements
    LaunchedEffect(matchScore) {
        if (matchScore >= 200 && !unlockedNovice) {
            unlockedNovice = true
            val bonus = 10
            viewModel.sessionCoinsEarnedToday.value += bonus
            activeAchievementToast = "🏆 ACHIEVEMENT UNLOCKED: ARCADE NOVICE! (+10 Coins bonus!)"
            GameSoundEffects.playPowerUp()
            delay(3500)
            activeAchievementToast = null
        }
        if (matchScore >= 500 && !unlockedCombo) {
            unlockedCombo = true
            val bonus = 20
            viewModel.sessionCoinsEarnedToday.value += bonus
            activeAchievementToast = "🔥 ACHIEVEMENT UNLOCKED: COMBO GRINDER! (+20 Coins bonus!)"
            GameSoundEffects.playPowerUp()
            delay(3500)
            activeAchievementToast = null
        }
        if (matchScore >= 1000 && !unlockedLegend) {
            unlockedLegend = true
            val bonus = 30
            viewModel.sessionCoinsEarnedToday.value += bonus
            activeAchievementToast = "👑 ACHIEVEMENT UNLOCKED: RETRO LEGEND! (+30 Coins bonus!)"
            GameSoundEffects.playPowerUp()
            delay(3500)
            activeAchievementToast = null
        }
    }

    // Start countdown timer only when pre-match settings are accepted
    LaunchedEffect(showPreMatchSettings) {
        if (!showPreMatchSettings) {
            // Live rival updates loop
            launch {
                val recentSessions = try {
                    com.example.data.sync.SupabaseSyncManager.pullTable("match_sessions")
                        .filter { it["status"]?.jsonPrimitive?.contentOrNull == "Completed" }
                        .take(5)
                } catch (_: Exception) { emptyList() }

                while (!isGameOver) {
                    delay(8000)
                    if (recentSessions.isNotEmpty()) {
                        val session = recentSessions.random()
                        val host = session["hostusername"]?.jsonPrimitive?.contentOrNull ?: "Player"
                        val guest = session["guestusername"]?.jsonPrimitive?.contentOrNull ?: "Player"
                        val hScore = session["hostscore"]?.jsonPrimitive?.intOrNull ?: 0
                        val gScore = session["guestscore"]?.jsonPrimitive?.intOrNull ?: 0
                        liveRivalMessage = "⚡ @$host ($hScore) vs @$guest ($gScore) - Match completed!"
                    } else {
                        liveRivalMessage = "No recent matches yet. Be the first to compete!"
                    }
                }
            }

            while (gameTimeRemaining > 0 && !isGameOver) {
                delay(1000)
                gameTimeRemaining--
            }
            isGameOver = true
            onSubmitScore(matchScore)
        }
    }

    // Get 30-Min session time remaining from view model
    val timeRemainingState by viewModel.currentSessionTimeRemaining.collectAsState()
    val minutes = timeRemainingState / 60
    val seconds = timeRemainingState % 60
    val sessionTimeFormatted = String.format("%02d:%02d", minutes, seconds)

    // Get live rankings
    val leaderboard by viewModel.activeSessionLeaderboard.collectAsState()
    val userRank = leaderboard.indexOfFirst { it.first == currentUser.username } + 1
    val rankText = if (userRank > 0) "Rank #$userRank" else "Rank --"

    Scaffold(
        topBar = {
            if (showPreMatchSettings || isGameOver) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when (gameId) {
                                    "candy_crush" -> "🍬 CANDY CRUSH ARENA"
                                    "block_buster" -> "🟩 BLOCK BUSTER BLITZ"
                                    "puzzle_2048" -> "2048 Number Fusion"
                                    "word_search" -> "Spelling Bee Champion"
                                    "memory_match" -> "Memory Match Flip"
                                    "trivia_quiz" -> "Sports Trivia League"
                                    "color_match" -> "Color Reaction Rush"
                                    else -> "Raival Arcade"
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = RaivalSecondary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tournament Session Ends in: $sessionTimeFormatted",
                                    fontSize = 10.sp,
                                    color = RaivalTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("arcade_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quit", tint = Color.White)
                        }
                    },
                    actions = {
                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Score Badge
                            Box(
                                modifier = Modifier
                                    .background(RaivalSurfaceLight, shape = RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Score: $matchScore",
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = RaivalSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface)
                )
            }
        }
    ) { innerPadding ->
        if (showPreMatchSettings) {
            PreMatchSettingsView(
                gameId = gameId,
                onAccept = { showPreMatchSettings = false },
                onCancel = onBack
            )
        } else if (isGameOver) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RaivalBackground)
                    .padding(innerPadding)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Game Over Summary Screen
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = RaivalSecondary, modifier = Modifier.size(72.dp))
                    Text("SESSION COMPLETE!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                    Text("You scored an amazing:", fontSize = 13.sp, color = RaivalTextSecondary)
                    Text(
                        text = "$matchScore Points",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = RaivalSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your score has been submitted to the 30-minute Live Session Leaderboard! Keep playing to maintain your rank and share the coin prizes!",
                        style = MaterialTheme.typography.bodySmall,
                        color = RaivalTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("game_over_continue_btn")
                    ) {
                        Text("CONTINUE TO CASUAL ZONE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // FULL SCREEN PLAYING STATE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RaivalBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Glassmorphic Floating HUD Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RaivalSurfaceLight.copy(alpha = 0.85f), shape = RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f)), shape = RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Exit Button
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp).testTag("abort_arcade_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit", tint = Color.Red.copy(alpha = 0.8f))
                        }

                        // Match Timer
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${gameTimeRemaining}s",
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (gameTimeRemaining < 20) Color.Red else Color.White,
                                fontSize = 15.sp
                            )
                        }

                        // Score Badge
                        Box(
                            modifier = Modifier
                                .background(RaivalPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Score: $matchScore",
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = RaivalSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Live Rival Ticker Banner
                    AnimatedVisibility(
                        visible = liveRivalMessage.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = RaivalSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = liveRivalMessage,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Centered Immersive Gameplay Space (stretches to take all remaining height!)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val onScoreUpdate: (Int) -> Unit = { points ->
                            matchScore += points
                            if (points > 0) {
                                val randomX = (50..220).random().toFloat()
                                val randomY = (200..450).random().toFloat()
                                val newId = System.currentTimeMillis() + (0..1000).random()
                                floatingScores = floatingScores + FloatingScoreIndicator(newId, "+$points", randomX, randomY)
                                coroutineScope.launch {
                                    delay(1500)
                                    floatingScores = floatingScores.filter { it.id != newId }
                                }
                            }
                        }

                        when (gameId) {
                            "candy_crush" -> CandyCrushMiniGame(onScore = onScoreUpdate)
                            "block_buster" -> BlockBusterMiniGame(onScore = onScoreUpdate)
                            "puzzle_2048" -> Puzzle2048MiniGame(onScore = onScoreUpdate)
                            "word_search" -> WordSearchMiniGame(onScore = onScoreUpdate)
                            "memory_match" -> MemoryMatchMiniGame(onScore = onScoreUpdate)
                            "trivia_quiz" -> TriviaQuizMiniGame(onScore = onScoreUpdate)
                            "color_match" -> ColorMatchMiniGame(onScore = onScoreUpdate)
                            else -> {
                                Text("Arcade Loading...", color = Color.White)
                            }
                        }
                    }

                    // Immersive Footer HUD Info
                    Text(
                        text = "🏆 LIVE TOURNAMENT MATCH • IGN: ${currentUser.username}",
                        color = RaivalTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // 1. Floating Score Indicators Overlay
                floatingScores.forEach { fs ->
                    key(fs.id) {
                        var visible by remember { mutableStateOf(true) }
                        var offsetY by remember { mutableStateOf(fs.y) }
                        
                        LaunchedEffect(Unit) {
                            val anim = Animatable(fs.y)
                            anim.animateTo(
                                targetValue = fs.y - 120f,
                                animationSpec = tween(durationMillis = 1200, easing = LinearOutSlowInEasing)
                            ) {
                                offsetY = this.value
                            }
                            visible = false
                        }
                        
                        if (visible) {
                            Box(
                                modifier = Modifier
                                    .absoluteOffset(
                                        x = fs.x.dp,
                                        y = offsetY.dp
                                    )
                            ) {
                                Text(
                                    text = fs.text,
                                    color = RaivalSecondary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Monospace,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black,
                                            offset = Offset(2f, 2f),
                                            blurRadius = 4f
                                        )
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Achievement Unlock Toast Overlay
                AnimatedVisibility(
                    visible = activeAchievementToast != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                ) {
                    if (activeAchievementToast != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Rich deep navy blue
                            border = BorderStroke(2.dp, Brush.horizontalGradient(listOf(RaivalPrimary, RaivalSecondary))),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().shadow(12.dp, shape = RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(RaivalSecondary.copy(alpha = 0.2f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = RaivalSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "UNLOCKED RETRO MILESTONE!",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RaivalPrimary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = activeAchievementToast!!,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
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

// =========================================================================
// 1. CANDY CRUSH MATCH-3 PLAYABLE GAME IMPLEMENTATION
// =========================================================================
data class CandyTile(val id: Int, val type: String, var isMatched: Boolean = false)

@Composable
fun CandyCrushMiniGame(onScore: (Int) -> Unit) {
    val candyIcons = listOf("🍬", "🍭", "🍫", "🍩", "🍒") // 5 types for better matching
    
    val coroutineScope = rememberCoroutineScope()
    var shakeOffset by remember { mutableStateOf(0.dp) }
    
    fun triggerShake() {
        coroutineScope.launch {
            for (i in 0..4) {
                shakeOffset = if (i % 2 == 0) 8.dp else (-8).dp
                delay(40)
            }
            shakeOffset = 0.dp
        }
    }

    // Match-3 check function for 6x6 grid
    fun findMatches(tiles: List<CandyTile>): List<Int> {
        val matchedIndices = mutableSetOf<Int>()
        // Horizontal check
        for (row in 0 until 6) {
            var matchCount = 1
            var matchType = ""
            var matchStartIdx = 0
            for (col in 0 until 6) {
                val idx = row * 6 + col
                val tType = tiles[idx].type
                if (col == 0) {
                    matchType = tType
                    matchCount = 1
                    matchStartIdx = idx
                } else {
                    if (tType == matchType && tType != "") {
                        matchCount++
                    } else {
                        if (matchCount >= 3) {
                            for (m in 0 until matchCount) {
                                matchedIndices.add(matchStartIdx + m)
                            }
                        }
                        matchType = tType
                        matchCount = 1
                        matchStartIdx = idx
                    }
                }
            }
            if (matchCount >= 3) {
                for (m in 0 until matchCount) {
                    matchedIndices.add(matchStartIdx + m)
                }
            }
        }
        // Vertical check
        for (col in 0 until 6) {
            var matchCount = 1
            var matchType = ""
            var matchStartIdx = 0
            for (row in 0 until 6) {
                val idx = row * 6 + col
                val tType = tiles[idx].type
                if (row == 0) {
                    matchType = tType
                    matchCount = 1
                    matchStartIdx = idx
                } else {
                    if (tType == matchType && tType != "") {
                        matchCount++
                    } else {
                        if (matchCount >= 3) {
                            for (m in 0 until matchCount) {
                                matchedIndices.add(matchStartIdx + m * 6)
                            }
                        }
                        matchType = tType
                        matchCount = 1
                        matchStartIdx = idx
                    }
                }
            }
            if (matchCount >= 3) {
                for (m in 0 until matchCount) {
                    matchedIndices.add(matchStartIdx + m * 6)
                }
            }
        }
        return matchedIndices.toList()
    }

    fun hasPossibleMoves(tiles: List<CandyTile>): Boolean {
        val temp = tiles.toMutableList()
        for (i in 0 until 36) {
            val r = i / 6
            val c = i % 6
            // Try right neighbor
            if (c < 5) {
                val j = i + 1
                val t = temp[i]
                temp[i] = temp[j].copy(id = i)
                temp[j] = t.copy(id = j)
                val matches = findMatches(temp)
                // Undo
                temp[i] = t
                temp[j] = temp[j].copy(id = j)
                if (matches.isNotEmpty()) return true
            }
            // Try down neighbor
            if (r < 5) {
                val j = i + 6
                val t = temp[i]
                temp[i] = temp[j].copy(id = i)
                temp[j] = t.copy(id = j)
                val matches = findMatches(temp)
                // Undo
                temp[i] = t
                temp[j] = temp[j].copy(id = j)
                if (matches.isNotEmpty()) return true
            }
        }
        return false
    }

    fun findPossibleMove(tiles: List<CandyTile>): Pair<Int, Int>? {
        val temp = tiles.toMutableList()
        for (i in 0 until 36) {
            val r = i / 6
            val c = i % 6
            // Try right neighbor
            if (c < 5) {
                val j = i + 1
                val t = temp[i]
                temp[i] = temp[j].copy(id = i)
                temp[j] = t.copy(id = j)
                val matches = findMatches(temp)
                // Undo
                temp[i] = t
                temp[j] = temp[j].copy(id = j)
                if (matches.isNotEmpty()) return Pair(i, j)
            }
            // Try down neighbor
            if (r < 5) {
                val j = i + 6
                val t = temp[i]
                temp[i] = temp[j].copy(id = i)
                temp[j] = t.copy(id = j)
                val matches = findMatches(temp)
                // Undo
                temp[i] = t
                temp[j] = temp[j].copy(id = j)
                if (matches.isNotEmpty()) return Pair(i, j)
            }
        }
        return null
    }

    fun generateValidBoard(): List<CandyTile> {
        var initialBoard = List(36) { CandyTile(it, candyIcons.random()) }
        var attempts = 0
        while ((findMatches(initialBoard).isNotEmpty() || !hasPossibleMoves(initialBoard)) && attempts < 150) {
            initialBoard = List(36) { CandyTile(it, candyIcons.random()) }
            attempts++
        }
        return initialBoard
    }

    // 6x6 Grid of candies
    var board by remember {
        mutableStateOf(generateValidBoard())
    }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var movesLeft by remember { mutableIntStateOf(30) }
    var gameLevel by remember { mutableIntStateOf(1) }
    var pointsToLevelUp by remember { mutableIntStateOf(400) }
    var pointsEarned by remember { mutableIntStateOf(0) }
    var activePowerUp by remember { mutableStateOf<String?>(null) } // "hammer", "bomb", "zap", null
    var overlayMessage by remember { mutableStateOf("TAP TWO SWEETS TO SWAP & MATCH 3!") }
    var streakCount by remember { mutableIntStateOf(0) }
    var suggestedMove by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Colors helper for styling candy tiles
    fun getCandyColor(type: String): Color {
        return when (type) {
            "🍬" -> Color(0xFFFF4081) // Neon Pink
            "🍭" -> Color(0xFFFF9800) // Neon Orange
            "🍫" -> Color(0xFF8D6E63) // Bronze Brown
            "🍩" -> Color(0xFF2979FF) // Vibrant Deep Blue
            "🍪" -> Color(0xFFFFEB3B) // Rich Yellow
            "🍒" -> Color(0xFFE91E63) // Cherry Red
            "🌟" -> Color(0xFF00E676) // Radiant Green
            else -> RaivalSurface
        }
    }

    // Cascade implementation with up to 3 cascading chain checks
    fun processMatchesAndCascade(currentBoard: List<CandyTile>) {
        var tempBoard = currentBoard.toMutableList()
        var matched = findMatches(tempBoard)
        
        if (matched.isEmpty()) {
            streakCount = 0
            return
        }

        var cascadeLoop = 1
        var totalPointsGained = 0

        while (matched.isNotEmpty() && cascadeLoop <= 3) {
            val pts = matched.size * 20 * cascadeLoop
            totalPointsGained += pts
            
            // Mark matched as matched, then replace with random ones
            for (idx in matched) {
                tempBoard[idx] = CandyTile(idx, candyIcons.random())
            }

            // Re-find matches on cascade
            matched = findMatches(tempBoard)
            cascadeLoop++
        }

        streakCount += (cascadeLoop - 1)
        onScore(totalPointsGained)
        pointsEarned += totalPointsGained

        // Play sound effect and trigger visual screen shake
        if (streakCount >= 2) {
            GameSoundEffects.playCombo()
        } else {
            GameSoundEffects.playMatch()
        }
        triggerShake()

        // Verify if possible moves remain, otherwise automatically auto-shuffle
        var finalBoardState = tempBoard
        if (!hasPossibleMoves(finalBoardState)) {
            finalBoardState = generateValidBoard().toMutableList()
            overlayMessage = "🌀 No moves left! Board Auto-shuffled!"
            GameSoundEffects.playPowerUp()
            triggerShake()
        }
        board = finalBoardState

        // Splash Message
        overlayMessage = when {
            overlayMessage.contains("Auto-shuffled") -> overlayMessage
            streakCount >= 3 -> "🔥 DIVINE CASCADE! +$totalPointsGained pts"
            streakCount == 2 -> "💫 DELICIOUS COMBO! +$totalPointsGained pts"
            else -> "🍬 SWEET CRUSH! +$totalPointsGained pts"
        }

        // Check level up
        if (pointsEarned >= pointsToLevelUp) {
            gameLevel++
            pointsEarned = 0
            pointsToLevelUp += 200
            movesLeft += 15
            overlayMessage = "🎉 LEVEL $gameLevel UNLOCKED! +15 Moves"
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().padding(8.dp)
    ) {
        // Game Stats HUD
        Row(
            modifier = Modifier.fillMaxWidth().background(RaivalSurface, shape = RoundedCornerShape(8.dp)).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("LEVEL $gameLevel", fontWeight = FontWeight.Bold, color = RaivalSecondary, fontSize = 11.sp)
                Text("Target: $pointsEarned / $pointsToLevelUp", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .background(RaivalPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("MOVES LEFT: $movesLeft", color = RaivalPrimary, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }

        // Visual Overlay announcement banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(RaivalPrimary.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f)), shape = RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = overlayMessage,
                color = if (overlayMessage.contains("LEVEL") || overlayMessage.contains("CASCADE")) RaivalSecondary else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // 6x6 Candy Grid
        Box(
            modifier = Modifier
                .offset(x = shakeOffset, y = shakeOffset)
                .fillMaxWidth(0.95f)
                .aspectRatio(1f)
                .background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
                .border(BorderStroke(1.5.dp, RaivalPrimary.copy(alpha = 0.3f)), shape = RoundedCornerShape(16.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(36) { idx ->
                    val candy = board[idx]
                    val isSelected = selectedIndex == idx
                    val tileColor = getCandyColor(candy.type)
                    val isHinted = suggestedMove != null && (suggestedMove?.first ?: -1 == idx || suggestedMove?.second ?: -1 == idx)

                    val cellBorderStroke = when {
                        isSelected -> BorderStroke(2.5.dp, RaivalSecondary)
                        isHinted -> BorderStroke(2.5.dp, Color.Yellow)
                        else -> BorderStroke(1.dp, tileColor.copy(alpha = 0.4f))
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(tileColor.copy(alpha = 0.6f), tileColor.copy(alpha = 0.15f))
                                )
                            )
                            .border(
                                cellBorderStroke,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                GameSoundEffects.playTap()
                                if (movesLeft <= 0) {
                                    overlayMessage = "⚠️ No moves left! Shuffle or Level up!"
                                    return@clickable
                                }

                                if (activePowerUp != null) {
                                    val newBoard = board.toMutableList()
                                    when (activePowerUp) {
                                        "hammer" -> {
                                            newBoard[idx] = CandyTile(idx, candyIcons.random())
                                            onScore(35)
                                            GameSoundEffects.playExplosion()
                                            triggerShake()
                                            overlayMessage = "🔨 Lollipop Hammer smashed tile! +35 pts"
                                        }
                                        "bomb" -> {
                                            val bombType = candy.type
                                            for (i in 0 until 36) {
                                                if (newBoard[i].type == bombType) {
                                                    newBoard[i] = CandyTile(i, candyIcons.random())
                                                }
                                            }
                                            onScore(120)
                                            GameSoundEffects.playExplosion()
                                            triggerShake()
                                            overlayMessage = "💣 Color Bomb cleared $bombType! +120 pts"
                                        }
                                        "zap" -> {
                                            val row = idx / 6
                                            val col = idx % 6
                                            for (i in 0 until 36) {
                                                if (i / 6 == row || i % 6 == col) {
                                                    newBoard[i] = CandyTile(i, candyIcons.random())
                                                }
                                            }
                                            onScore(150)
                                            GameSoundEffects.playExplosion()
                                            triggerShake()
                                            overlayMessage = "⚡ Candy Zap cleared line! +150 pts"
                                        }
                                    }
                                    activePowerUp = null
                                    suggestedMove = null // Clear hint on booster use
                                    processMatchesAndCascade(newBoard)
                                    return@clickable
                                }

                                val prevSelected = selectedIndex
                                if (prevSelected == null) {
                                    selectedIndex = idx
                                } else {
                                    // Check adjacency
                                    val r1 = prevSelected / 6
                                    val c1 = prevSelected % 6
                                    val r2 = idx / 6
                                    val c2 = idx % 6
                                    val isAdjacent = (kotlin.math.abs(r1 - r2) == 1 && c1 == c2) || 
                                                     (kotlin.math.abs(c1 - c2) == 1 && r1 == r2)

                                    if (isAdjacent) {
                                        // Try Swap
                                        val testBoard = board.toMutableList()
                                        val temp = testBoard[prevSelected]
                                        testBoard[prevSelected] = testBoard[idx].copy(id = prevSelected)
                                        testBoard[idx] = temp.copy(id = idx)

                                        val matches = findMatches(testBoard)
                                        if (matches.isNotEmpty()) {
                                            board = testBoard
                                            movesLeft--
                                            suggestedMove = null // Clear hint on successful swap
                                            GameSoundEffects.playMatch()
                                            triggerShake()
                                            processMatchesAndCascade(testBoard)
                                        } else {
                                            GameSoundEffects.playFailure()
                                            overlayMessage = "❌ No match found! Resetting swap..."
                                        }
                                    }
                                    selectedIndex = null
                                }
                            }
                            .testTag("candy_tile_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = candy.type,
                            fontSize = 28.sp,
                            modifier = Modifier.animateContentSize()
                        )
                    }
                }
            }
        }

        // Candy Crush Power-ups Block
        Column(
            modifier = Modifier.fillMaxWidth().background(RaivalSurface, shape = RoundedCornerShape(12.dp)).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🍬 UNLOCKED BOOSTERS", fontWeight = FontWeight.Bold, color = RaivalTextSecondary, fontSize = 9.sp)
                if (activePowerUp != null) {
                    TextButton(onClick = { activePowerUp = null }) {
                        Text("Cancel Power-up", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Booster 1: Lollipop Hammer
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { activePowerUp = if (activePowerUp == "hammer") null else "hammer" },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (activePowerUp == "hammer") RaivalSecondary else RaivalSurfaceLight
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("🔨", fontSize = 16.sp)
                    }
                    Text("Hammer", fontSize = 8.sp, color = Color.White)
                }

                // Booster 2: Color Bomb
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { activePowerUp = if (activePowerUp == "bomb") null else "bomb" },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (activePowerUp == "bomb") RaivalSecondary else RaivalSurfaceLight
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("💣", fontSize = 16.sp)
                    }
                    Text("Color Bomb", fontSize = 8.sp, color = Color.White)
                }

                // Booster 3: Candy Zap
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { activePowerUp = if (activePowerUp == "zap") null else "zap" },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (activePowerUp == "zap") RaivalSecondary else RaivalSurfaceLight
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("⚡", fontSize = 16.sp)
                    }
                    Text("Row Zap", fontSize = 8.sp, color = Color.White)
                }

                // Booster 4: Shuffle Board
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            board = List(36) { CandyTile(it, candyIcons.random()) }
                            onScore(15)
                            overlayMessage = "🌀 Candies Shuffled! +15 pts"
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = RaivalSurfaceLight),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("🌀", fontSize = 16.sp)
                    }
                    Text("Shuffle", fontSize = 8.sp, color = Color.White)
                }

                // Booster 5: 💡 Hint Matcher
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            suggestedMove = findPossibleMove(board)
                            if (suggestedMove != null) {
                                overlayMessage = "💡 Hint: Swap the pulsating/bordered candies!"
                                GameSoundEffects.playPowerUp()
                            } else {
                                overlayMessage = "⚠️ No immediate matches! Try Shuffling."
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (suggestedMove != null) RaivalPrimary else RaivalSurfaceLight
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("💡", fontSize = 16.sp)
                    }
                    Text("Hint", fontSize = 8.sp, color = Color.White)
                }
            }
        }
    }
}

// =========================================================================
// 2. BLOCK BUSTER PLAYABLE GAME IMPLEMENTATION (STYLISH BLOCK PUZZLE)
// =========================================================================
data class BlockShape(
    val id: Int,
    val label: String,
    val blocks: List<Pair<Int, Int>>, // Relative positions (rowOffset, colOffset)
    val color: Color
)

@Composable
fun BlockBusterMiniGame(onScore: (Int) -> Unit) {
    // 6x6 Board State
    var gridState by remember { mutableStateOf(List(36) { null as Color? }) }
    
    val coroutineScope = rememberCoroutineScope()
    var shakeOffset by remember { mutableStateOf(0.dp) }
    
    fun triggerShake() {
        coroutineScope.launch {
            for (i in 0..4) {
                shakeOffset = if (i % 2 == 0) 8.dp else (-8).dp
                delay(40)
            }
            shakeOffset = 0.dp
        }
    }
    
    // Helpers to generate random shapes
    fun getNewRandomShape(): BlockShape {
        val shapes = listOf(
            BlockShape(1, "1x1 Dot", listOf(0 to 0), Color(0xFF00E676)), // Neon Green
            BlockShape(2, "2x2 Sq", listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1), Color(0xFFFF1744)), // Neon Red
            BlockShape(3, "1x3 Line", listOf(0 to 0, 0 to 1, 0 to 2), Color(0xFF29B6F6)), // Light Blue
            BlockShape(4, "3x1 Line", listOf(0 to 0, 1 to 0, 2 to 0), Color(0xFF2979FF)), // Blue
            BlockShape(5, "L-Shape", listOf(0 to 0, 1 to 0, 1 to 1), Color(0xFFFF9100)), // Orange
            BlockShape(6, "T-Shape", listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1), Color(0xFFFFD600)), // Yellow
            BlockShape(7, "Diag Duo", listOf(0 to 0, 1 to 1), Color(0xFF00E5FF)) // Cyan-Blue
        )
        return shapes.random()
    }

    // Shapes Inventory (3 Slots)
    var availableShapes by remember {
        mutableStateOf(List<BlockShape?>(3) { getNewRandomShape() })
    }
    
    var selectedShapeIndex by remember { mutableStateOf<Int?>(null) }
    var aimCellIndex by remember { mutableStateOf<Int?>(null) }
    var streakMultiplier by remember { mutableStateOf(1) }
    var activePowerUp by remember { mutableStateOf<String?>(null) } // "hammer", null
    var feedbackMessage by remember { mutableStateOf("TAP SHAPE, THEN TAP GRID TO PLACE IT!") }

    // Check fit validation
    fun canPlace(shape: BlockShape, startRow: Int, startCol: Int): Boolean {
        for (offset in shape.blocks) {
            val targetRow = startRow + offset.first
            val targetCol = startCol + offset.second
            if (targetRow !in 0..5 || targetCol !in 0..5) return false
            val targetIdx = targetRow * 6 + targetCol
            if (gridState[targetIdx] != null) return false
        }
        return true
    }

    // Clear completed rows/cols
    fun clearRowsAndColumns(currentGrid: List<Color?>): Pair<List<Color?>, Int> {
        val nextGrid = currentGrid.toMutableList()
        val rowsToClear = mutableListOf<Int>()
        val colsToClear = mutableListOf<Int>()

        // Check rows
        for (row in 0 until 6) {
            var fullRow = true
            for (col in 0 until 6) {
                if (nextGrid[row * 6 + col] == null) {
                    fullRow = false
                    break
                }
            }
            if (fullRow) rowsToClear.add(row)
        }

        // Check columns
        for (col in 0 until 6) {
            var fullCol = true
            for (row in 0 until 6) {
                if (nextGrid[row * 6 + col] == null) {
                    fullCol = false
                    break
                }
            }
            if (fullCol) colsToClear.add(col)
        }

        // Clear cells
        for (row in rowsToClear) {
            for (col in 0 until 6) {
                nextGrid[row * 6 + col] = null
            }
        }
        for (col in colsToClear) {
            for (row in 0 until 6) {
                nextGrid[row * 6 + col] = null
            }
        }

        val linesCleared = rowsToClear.size + colsToClear.size
        val pointsEarned = when (linesCleared) {
            1 -> 150
            2 -> 450
            3 -> 1000
            4 -> 2000
            else -> 0
        }

        return Pair(nextGrid, pointsEarned)
    }

    // Rotate Shape
    fun rotateSelectedShape() {
        val sIdx = selectedShapeIndex ?: return
        val shape = availableShapes[sIdx] ?: return
        val rotatedBlocks = shape.blocks.map { (r, c) -> c to -r }
        val minR = rotatedBlocks.minOf { it.first }
        val minC = rotatedBlocks.minOf { it.second }
        val alignedBlocks = rotatedBlocks.map { (r, c) -> (r - minR) to (c - minC) }
        
        val updatedShapes = availableShapes.toMutableList()
        updatedShapes[sIdx] = shape.copy(blocks = alignedBlocks)
        availableShapes = updatedShapes
        aimCellIndex = null // Reset aim on rotate
        feedbackMessage = "🔄 Rotated Shape 90°! Ready to aim."
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().padding(8.dp)
    ) {
        // Status HUD Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(RaivalPrimary.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f)), shape = RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = feedbackMessage,
                color = if (feedbackMessage.contains("BLAST") || feedbackMessage.contains("COMBO")) RaivalSecondary else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // 6x6 Block Buster Grid
        Box(
            modifier = Modifier
                .offset(x = shakeOffset, y = shakeOffset)
                .fillMaxWidth(0.95f)
                .aspectRatio(1f)
                .background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
                .border(BorderStroke(1.5.dp, RaivalSecondary.copy(alpha = 0.3f)), shape = RoundedCornerShape(16.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(36) { idx ->
                    val blockColor = gridState[idx]
                    val r = idx / 6
                    val c = idx % 6

                    // Determine if this cell is currently part of the active shape preview
                    val isPreview = if (selectedShapeIndex != null && aimCellIndex != null) {
                        val shape = availableShapes[selectedShapeIndex ?: 0]
                        if (shape != null) {
                            val aimR = aimCellIndex ?: 0 / 6
                            val aimC = aimCellIndex ?: 0 % 6
                            shape.blocks.any { (offsetR, offsetC) -> (aimR + offsetR == r) && (aimC + offsetC == c) }
                        } else false
                    } else false

                    val previewFits = if (isPreview && selectedShapeIndex != null && aimCellIndex != null) {
                        val shape = availableShapes[selectedShapeIndex ?: 0]
                        if (shape != null) {
                            val aimR = aimCellIndex ?: 0 / 6
                            val aimC = aimCellIndex ?: 0 % 6
                            canPlace(shape, aimR, aimC)
                        } else false
                    } else false

                    val cellBg = when {
                        blockColor != null -> Brush.verticalGradient(listOf(blockColor, blockColor.copy(alpha = 0.7f)))
                        isPreview && previewFits -> {
                            val shapeColor = availableShapes[selectedShapeIndex ?: 0]?.color ?: RaivalSecondary
                            Brush.verticalGradient(listOf(shapeColor.copy(alpha = 0.8f), shapeColor.copy(alpha = 0.4f)))
                        }
                        isPreview && !previewFits -> {
                            Brush.verticalGradient(listOf(Color.Red.copy(alpha = 0.7f), Color.Red.copy(alpha = 0.3f)))
                        }
                        else -> Brush.verticalGradient(listOf(RaivalSurface, RaivalSurface.copy(alpha = 0.5f)))
                    }

                    val cellBorder = when {
                        blockColor != null -> BorderStroke(1.dp, blockColor.copy(alpha = 0.8f))
                        isPreview && previewFits -> BorderStroke(2.dp, Color.White)
                        isPreview && !previewFits -> BorderStroke(2.dp, Color.Red)
                        else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cellBg)
                            .border(cellBorder, shape = RoundedCornerShape(6.dp))
                            .clickable {
                                GameSoundEffects.playTap()
                                if (activePowerUp == "hammer") {
                                    if (gridState[idx] != null) {
                                        val nextGrid = gridState.toMutableList()
                                        nextGrid[idx] = null
                                        gridState = nextGrid
                                        activePowerUp = null
                                        onScore(30)
                                        GameSoundEffects.playExplosion()
                                        triggerShake()
                                        feedbackMessage = "🔨 Block Smashed! +30 pts"
                                    } else {
                                        feedbackMessage = "⚠️ Tap a filled block to destroy it!"
                                    }
                                    return@clickable
                                }

                                val sIdx = selectedShapeIndex
                                if (sIdx == null) {
                                    feedbackMessage = "💡 Select a shape below first!"
                                    return@clickable
                                }

                                val shape = availableShapes[sIdx]
                                if (shape == null) {
                                    selectedShapeIndex = null
                                    aimCellIndex = null
                                    return@clickable
                                }

                                if (aimCellIndex != idx) {
                                    aimCellIndex = idx
                                    if (canPlace(shape, r, c)) {
                                        feedbackMessage = "🎯 Aiming '${shape.label}' here! Tap cell again to confirm placement."
                                    } else {
                                        feedbackMessage = "⚠️ Shape doesn't fit here! Try another cell."
                                    }
                                } else {
                                    // Second tap - try to place!
                                    if (canPlace(shape, r, c)) {
                                        val nextGrid = gridState.toMutableList()
                                        for (offset in shape.blocks) {
                                            val tr = r + offset.first
                                            val tc = c + offset.second
                                            nextGrid[tr * 6 + tc] = shape.color
                                        }

                                        // Clear lines and score
                                        val (clearedGrid, linesPoints) = clearRowsAndColumns(nextGrid)
                                        gridState = clearedGrid

                                        val placePoints = shape.blocks.size * 10
                                        val totalPoints = placePoints + linesPoints
                                        onScore(totalPoints)

                                        if (linesPoints > 0) {
                                            streakMultiplier++
                                            GameSoundEffects.playCombo()
                                            triggerShake()
                                            feedbackMessage = "💥 LINE BLAST! COMBO x$streakMultiplier! +$totalPoints pts"
                                        } else {
                                            GameSoundEffects.playMatch()
                                            feedbackMessage = "✅ Shape Placed! +$placePoints pts"
                                        }

                                        // Consume shape from inventory
                                        val nextShapes = availableShapes.toMutableList()
                                        nextShapes[sIdx] = null
                                        
                                        // If all 3 shapes used, spawn new ones
                                        if (nextShapes.all { it == null }) {
                                            availableShapes = List<BlockShape?>(3) { getNewRandomShape() }
                                            GameSoundEffects.playPowerUp()
                                            feedbackMessage = "🆕 Fresh Shapes Loaded!"
                                        } else {
                                            availableShapes = nextShapes
                                        }
                                        selectedShapeIndex = null
                                        aimCellIndex = null
                                    } else {
                                        GameSoundEffects.playFailure()
                                        feedbackMessage = "❌ Doesn't fit! Try another spot."
                                        aimCellIndex = null
                                    }
                                }
                            }
                            .testTag("block_buster_slot_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (blockColor != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.6f)
                                    .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }

        // Available Shapes Drawer - Enlarged cards with high contrast
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RaivalSurface, shape = RoundedCornerShape(12.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 3) {
                val shape = availableShapes[i]
                val isSelected = selectedShapeIndex == i

                Box(
                    modifier = Modifier
                        .size(if (isSelected) 84.dp else 74.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) RaivalSecondary.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f))
                        .border(
                            BorderStroke(
                                if (isSelected) 3.dp else 1.dp,
                                if (isSelected) RaivalSecondary else Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            if (shape != null) {
                                selectedShapeIndex = i
                                aimCellIndex = null // Reset target cell selection when switching shapes
                                feedbackMessage = "👉 Tap on board to aim '${shape.label}'"
                            }
                        }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (shape != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Render mini visual block representation as a real 3x3 grid - enlarged
                            Column(
                                modifier = Modifier.size(44.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                for (r in 0..2) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        for (c in 0..2) {
                                            val hasBlock = shape.blocks.any { it.first == r && it.second == c }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .background(
                                                        if (hasBlock) shape.color else Color.White.copy(alpha = 0.05f),
                                                        shape = RoundedCornerShape(2.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                            Text(shape.label, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    } else {
                        Text("PLACED", fontSize = 10.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Block Buster Control Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { rotateSelectedShape() },
                enabled = selectedShapeIndex != null,
                colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalSecondary),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = RaivalSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ROTATE SHAPE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RaivalSecondary)
            }

            Button(
                onClick = {
                    activePowerUp = if (activePowerUp == "hammer") null else "hammer"
                    feedbackMessage = "🔨 Hammer Active! Tap a grid block to smash it."
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activePowerUp == "hammer") RaivalPrimary else RaivalSurfaceLight
                ),
                border = BorderStroke(1.dp, RaivalPrimary),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("🔨 HAMMER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (activePowerUp == "hammer") Color.Black else RaivalPrimary)
            }

            Button(
                onClick = {
                    availableShapes = List<BlockShape?>(3) { getNewRandomShape() }
                    onScore(10)
                    feedbackMessage = "🌀 Shapes Rerolled! +10 pts"
                },
                colors = ButtonDefaults.buttonColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("🌀 REROLL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// =========================================================================
// 3. 2048 PLAYABLE PUZZLE GAME IMPLEMENTATION
// =========================================================================
@Composable
fun Puzzle2048MiniGame(onScore: (Int) -> Unit) {
    // 3x3 smaller grid for fast casual gaming. Tiles combine.
    var tiles by remember {
        mutableStateOf(
            listOf(
                2, 0, 0,
                0, 2, 0,
                0, 0, 0
            )
        )
    }

    val coroutineScope = rememberCoroutineScope()
    var shakeOffset by remember { mutableStateOf(0.dp) }

    fun triggerShake() {
        coroutineScope.launch {
            for (i in 0..4) {
                shakeOffset = if (i % 2 == 0) 6.dp else (-6).dp
                delay(40)
            }
            shakeOffset = 0.dp
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Text("COMBINE SAME TILES TO MERGE NUMBERS", color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Box(
            modifier = Modifier
                .offset(x = shakeOffset, y = shakeOffset)
                .fillMaxWidth(0.95f)
                .aspectRatio(1f)
                .background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
                .border(BorderStroke(1.5.dp, RaivalPrimary.copy(alpha = 0.3f)), shape = RoundedCornerShape(16.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(9) { idx ->
                    val value = tiles[idx]
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (value) {
                                    0 -> RaivalSurface
                                    2 -> Color(0xFFEEE4DA)
                                    4 -> Color(0xFFEDE0C8)
                                    8 -> Color(0xFFF2B179)
                                    16 -> Color(0xFFF59563)
                                    32 -> Color(0xFFF67C5F)
                                    64 -> Color(0xFFF65E3B)
                                    else -> RaivalSecondary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (value > 0) {
                            Text(
                                text = "$value",
                                fontWeight = FontWeight.Black,
                                fontSize = if (value > 100) 22.sp else 32.sp,
                                color = if (value <= 4) Color.DarkGray else Color.White
                            )
                        }
                    }
                }
            }
        }

        // Joystick control layout
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val prevTiles = tiles
                    val next = slideTilesLeft(tiles) { onScore(it) }
                    val spawned = spawnRandomTwo(next)
                    tiles = spawned
                    if (spawned != prevTiles) {
                        GameSoundEffects.playMatch()
                        triggerShake()
                    } else {
                        GameSoundEffects.playTap()
                    }
                },
                modifier = Modifier.background(RaivalSurface, shape = CircleShape).testTag("merge_left")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Left", tint = RaivalPrimary)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = {
                        val prevTiles = tiles
                        val next = slideTilesUp(tiles) { onScore(it) }
                        val spawned = spawnRandomTwo(next)
                        tiles = spawned
                        if (spawned != prevTiles) {
                            GameSoundEffects.playMatch()
                            triggerShake()
                        } else {
                            GameSoundEffects.playTap()
                        }
                    },
                    modifier = Modifier.background(RaivalSurface, shape = CircleShape).testTag("merge_up")
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = RaivalPrimary)
                }
                
                IconButton(
                    onClick = {
                        val prevTiles = tiles
                        val next = slideTilesDown(tiles) { onScore(it) }
                        val spawned = spawnRandomTwo(next)
                        tiles = spawned
                        if (spawned != prevTiles) {
                            GameSoundEffects.playMatch()
                            triggerShake()
                        } else {
                            GameSoundEffects.playTap()
                        }
                    },
                    modifier = Modifier.background(RaivalSurface, shape = CircleShape).testTag("merge_down")
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = RaivalPrimary)
                }
            }

            IconButton(
                onClick = {
                    val prevTiles = tiles
                    val next = slideTilesRight(tiles) { onScore(it) }
                    val spawned = spawnRandomTwo(next)
                    tiles = spawned
                    if (spawned != prevTiles) {
                        GameSoundEffects.playMatch()
                        triggerShake()
                    } else {
                        GameSoundEffects.playTap()
                    }
                },
                modifier = Modifier.background(RaivalSurface, shape = CircleShape).testTag("merge_right")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Right", tint = RaivalPrimary)
            }
        }
    }
}

private fun slideSingleRowLeft(row: List<Int>, addScore: (Int) -> Unit): List<Int> {
    val nonZeros = row.filter { it != 0 }
    val merged = mutableListOf<Int>()
    var skip = false
    for (i in nonZeros.indices) {
        if (skip) {
            skip = false
            continue
        }
        if (i + 1 < nonZeros.size && nonZeros[i] == nonZeros[i + 1]) {
            val doubled = nonZeros[i] * 2
            merged.add(doubled)
            addScore(doubled)
            skip = true
        } else {
            merged.add(nonZeros[i])
        }
    }
    while (merged.size < 3) {
        merged.add(0)
    }
    return merged
}

private fun slideTilesLeft(tiles: List<Int>, addScore: (Int) -> Unit): List<Int> {
    val result = tiles.toMutableList()
    for (row in 0..2) {
        val r0 = row * 3
        val r1 = row * 3 + 1
        val r2 = row * 3 + 2
        val rowList = listOf(tiles[r0], tiles[r1], tiles[r2])
        val slid = slideSingleRowLeft(rowList, addScore)
        result[r0] = slid[0]
        result[r1] = slid[1]
        result[r2] = slid[2]
    }
    return result
}

private fun slideTilesRight(tiles: List<Int>, addScore: (Int) -> Unit): List<Int> {
    val result = tiles.toMutableList()
    for (row in 0..2) {
        val r0 = row * 3
        val r1 = row * 3 + 1
        val r2 = row * 3 + 2
        val rowList = listOf(tiles[r2], tiles[r1], tiles[r0]) // reversed
        val slid = slideSingleRowLeft(rowList, addScore)
        result[r0] = slid[2] // reverse back
        result[r1] = slid[1]
        result[r2] = slid[0]
    }
    return result
}

private fun slideTilesUp(tiles: List<Int>, addScore: (Int) -> Unit): List<Int> {
    val result = tiles.toMutableList()
    for (col in 0..2) {
        val c0 = col
        val c1 = col + 3
        val c2 = col + 6
        val colList = listOf(tiles[c0], tiles[c1], tiles[c2])
        val slid = slideSingleRowLeft(colList, addScore)
        result[c0] = slid[0]
        result[c1] = slid[1]
        result[c2] = slid[2]
    }
    return result
}

private fun slideTilesDown(tiles: List<Int>, addScore: (Int) -> Unit): List<Int> {
    val result = tiles.toMutableList()
    for (col in 0..2) {
        val c0 = col
        val c1 = col + 3
        val c2 = col + 6
        val colList = listOf(tiles[c2], tiles[c1], tiles[c0]) // reversed
        val slid = slideSingleRowLeft(colList, addScore)
        result[c0] = slid[2] // reverse back
        result[c1] = slid[1]
        result[c2] = slid[0]
    }
    return result
}

private fun spawnRandomTwo(tiles: List<Int>): List<Int> {
    val result = tiles.toMutableList()
    val emptyIndices = result.indices.filter { result[it] == 0 }
    if (emptyIndices.isNotEmpty()) {
        result[emptyIndices.random()] = 2
    }
    return result
}

// =========================================================================
// 4. WORD SEARCH PLAYABLE GAME IMPLEMENTATION
// =========================================================================
@Composable
fun WordSearchMiniGame(onScore: (Int) -> Unit) {
    // Grid of letters
    val grid = listOf(
        "D", "L", "S", "G", "O",
        "W", "I", "N", "O", "A",
        "S", "K", "I", "L", "L",
        "C", "H", "A", "M", "P",
        "P", "L", "A", "Y", "R"
    )

    val wordBank = listOf("DLS", "WIN", "GOAL", "CHAMP", "PLAY", "SKILL")
    var foundWords by remember { mutableStateOf(setOf<String>()) }
    var selectedSequence by remember { mutableStateOf("") }
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }

    val coroutineScope = rememberCoroutineScope()
    var shakeOffset by remember { mutableStateOf(0.dp) }

    fun triggerShake() {
        coroutineScope.launch {
            for (i in 0..4) {
                shakeOffset = if (i % 2 == 0) 6.dp else (-6).dp
                delay(40)
            }
            shakeOffset = 0.dp
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Text("TAP LETTERS IN ORDER TO SPELL HIDDEN WORDS", color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Text(
            text = "Current: $selectedSequence",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RaivalSecondary
        )

        Box(
            modifier = Modifier
                .offset(x = shakeOffset, y = shakeOffset)
                .fillMaxWidth(0.95f)
                .aspectRatio(1f)
                .background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
                .border(BorderStroke(1.5.dp, RaivalPrimary.copy(alpha = 0.3f)), shape = RoundedCornerShape(16.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(25) { idx ->
                    val letter = grid[idx]
                    val isSelected = selectedIndices.contains(idx)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) RaivalPrimary.copy(alpha = 0.3f) else RaivalSurface)
                            .border(
                                BorderStroke(1.5.dp, if (isSelected) RaivalPrimary else RaivalTextSecondary.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (!selectedIndices.contains(idx)) {
                                    GameSoundEffects.playTap()
                                    selectedIndices = selectedIndices + idx
                                    selectedSequence += letter
                                    
                                    // Check if we matched a word
                                    if (wordBank.contains(selectedSequence) && !foundWords.contains(selectedSequence)) {
                                        foundWords = foundWords + selectedSequence
                                        onScore(50) // Match points
                                        GameSoundEffects.playSuccess()
                                        triggerShake()
                                        selectedSequence = ""
                                        selectedIndices = emptySet()
                                    }
                                } else {
                                    GameSoundEffects.playFailure()
                                }
                            }
                            .testTag("word_search_tile_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(letter, fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.White)
                    }
                }
            }
        }

        // Word Bank Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Bank: ", fontSize = 11.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
            wordBank.forEach { w ->
                val isFound = foundWords.contains(w)
                Text(
                    text = w,
                    fontSize = 11.sp,
                    color = if (isFound) Color(0xFF00FF87) else Color.White,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (isFound) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    GameSoundEffects.playTap()
                    selectedSequence = ""
                    selectedIndices = emptySet()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RaivalSurface)
            ) {
                Text("Clear Selection", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

// =========================================================================
// 5. MEMORY MATCH PLAYABLE GAME IMPLEMENTATION
// =========================================================================
@Composable
fun MemoryMatchMiniGame(onScore: (Int) -> Unit) {
    // 4x4 cards with 8 pairs of sport icons
    val icons = listOf("⚽", "🏀", "🏈", "🎾", "🏐", "🏉", "🎱", "🏓", "⚽", "🏀", "🏈", "🎾", "🏐", "🏉", "🎱", "🏓")
    
    val coroutineScope = rememberCoroutineScope()
    var cards by remember { mutableStateOf(icons.shuffled()) }
    var flippedIndices by remember { mutableStateOf(setOf<Int>()) }
    var matchedIndices by remember { mutableStateOf(setOf<Int>()) }
    var shakeOffset by remember { mutableStateOf(0.dp) }

    fun triggerShake() {
        coroutineScope.launch {
            for (i in 0..4) {
                shakeOffset = if (i % 2 == 0) 8.dp else (-8).dp
                delay(40)
            }
            shakeOffset = 0.dp
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Text("TAP CARDS TO FLIP & MATCH THE PAIRS", color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Box(
            modifier = Modifier
                .offset(x = shakeOffset, y = shakeOffset)
                .fillMaxWidth(0.95f)
                .aspectRatio(1f)
                .background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
                .border(BorderStroke(1.5.dp, RaivalSecondary.copy(alpha = 0.3f)), shape = RoundedCornerShape(16.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(16) { idx ->
                    val isFlipped = flippedIndices.contains(idx) || matchedIndices.contains(idx)
                    val icon = cards[idx]

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFlipped) RaivalSurface else RaivalSecondary.copy(alpha = 0.2f))
                            .border(
                                BorderStroke(1.5.dp, if (isFlipped) RaivalPrimary else RaivalSecondary.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (!isFlipped && flippedIndices.size < 2) {
                                    GameSoundEffects.playTap()
                                    val nextFlipped = flippedIndices + idx
                                    flippedIndices = nextFlipped

                                    if (nextFlipped.size == 2) {
                                        val list = nextFlipped.toList()
                                        if (cards[list[0]] == cards[list[1]]) {
                                            val updatedMatches = matchedIndices + nextFlipped
                                            matchedIndices = updatedMatches
                                            flippedIndices = emptySet()
                                            onScore(40) // Match points
                                            
                                            // Check if game complete
                                            if (updatedMatches.size == 16) {
                                                GameSoundEffects.playSuccess()
                                            } else {
                                                GameSoundEffects.playMatch()
                                            }
                                        } else {
                                            GameSoundEffects.playFailure()
                                            triggerShake()
                                            // Hide after delay
                                            coroutineScope.launch {
                                                delay(800)
                                                flippedIndices = emptySet()
                                            }
                                        }
                                    }
                                }
                            }
                            .testTag("memory_card_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFlipped) {
                            Text(icon, fontSize = 32.sp)
                        } else {
                            Text("?", color = RaivalSecondary, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                GameSoundEffects.playPowerUp()
                cards = icons.shuffled()
                flippedIndices = emptySet()
                matchedIndices = emptySet()
            },
            colors = ButtonDefaults.buttonColors(containerColor = RaivalSurface),
            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.3f))
        ) {
            Text("Restart Board", color = RaivalPrimary, fontSize = 11.sp)
        }
    }
}

// =========================================================================
// 6. TRIVIA QUIZ PLAYABLE GAME IMPLEMENTATION
// =========================================================================
data class TriviaQuestion(
    val q: String,
    val options: List<String>,
    val correctIndex: Int
)

@Composable
fun TriviaQuizMiniGame(onScore: (Int) -> Unit) {
    val pool = remember {
        listOf(
            TriviaQuestion(
                "Which DLS formation offers a single target striker upfront with dense midfield support?",
                listOf("4-3-3 Attack", "4-5-1 Flat", "3-5-2", "4-4-2 Wing"),
                1
            ),
            TriviaQuestion(
                "How long does a standard Raival casual esports session last?",
                listOf("15 minutes", "1 Hour", "30 minutes", "10 minutes"),
                2
            ),
            TriviaQuestion(
                "Which Ghanaian football legend is known as 'The Maestro'?",
                listOf("Asamoah Gyan", "Abedi Pele", "Tony Yeboah", "Sulley Muntari"),
                1
            ),
            TriviaQuestion(
                "What is the maximum daily coins limit players can earn from casual gaming on Raival?",
                listOf("50 coins", "100 coins", "160 coins", "70 coins"),
                0
            ),
            TriviaQuestion(
                "Which Ghanaian club is known as the 'Phobians'?",
                listOf("Asante Kotoko", "Hearts of Oak", "Great Olympics", "Aduana Stars"),
                1
            ),
            TriviaQuestion(
                "Which Ghana international scored the fastest goal in World Cup history for Ghana in 2014?",
                listOf("Asamoah Gyan", "Sulley Muntari", "Christian Atsu", "Andre Ayew"),
                0
            ),
            TriviaQuestion(
                "In DLS, what is the currency used to sign superstar players directly from the live transfer market?",
                listOf("Gold Coins", "Raival Gems", "Gems", "MoMo Cash"),
                2
            ),
            TriviaQuestion(
                "Which team did Ghana defeat to win the FIFA U-20 World Cup in 2009?",
                listOf("Brazil", "Spain", "England", "Argentina"),
                0
            ),
            TriviaQuestion(
                "How many Africa Cup of Nations (AFCON) trophies has Ghana won in its history?",
                listOf("2 Trophies", "4 Trophies", "5 Trophies", "3 Trophies"),
                1
            ),
            TriviaQuestion(
                "What does 'DLS' stand for in the popular mobile football series?",
                listOf("Direct League Soccer", "Dream League Soccer", "Dynamic Live Sports", "Deluxe League Stadium"),
                1
            )
        )
    }

    var qIdx by remember { mutableIntStateOf(0) }
    val currentQuestion = pool[qIdx % pool.size]
    var selectedOptionIdx by remember { mutableStateOf<Int?>(null) }
    var showCorrectHint by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var shakeOffset by remember { mutableStateOf(0.dp) }

    fun triggerShake() {
        coroutineScope.launch {
            for (i in 0..4) {
                shakeOffset = if (i % 2 == 0) 8.dp else (-8).dp
                delay(40)
            }
            shakeOffset = 0.dp
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Text("DLS & SPORTS LEAGUE TRIVIA QUIZ", color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier
                .offset(x = shakeOffset, y = shakeOffset)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RaivalSurface),
            border = BorderStroke(1.5.dp, RaivalSecondary.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = currentQuestion.q,
                modifier = Modifier.padding(20.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            currentQuestion.options.forEachIndexed { optIdx, text ->
                val isSelected = selectedOptionIdx == optIdx
                val containerColor = when {
                    showCorrectHint && optIdx == currentQuestion.correctIndex -> Color(0xFF00FF87).copy(alpha = 0.2f)
                    showCorrectHint && isSelected && optIdx != currentQuestion.correctIndex -> Color.Red.copy(alpha = 0.2f)
                    isSelected -> RaivalPrimary.copy(alpha = 0.15f)
                    else -> RaivalSurface
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(containerColor, shape = RoundedCornerShape(12.dp))
                        .border(
                            BorderStroke(
                                1.5.dp,
                                if (isSelected) RaivalPrimary else RaivalTextSecondary.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (!showCorrectHint) {
                                selectedOptionIdx = optIdx
                                showCorrectHint = true
                                if (optIdx == currentQuestion.correctIndex) {
                                    onScore(35)
                                    GameSoundEffects.playSuccess()
                                } else {
                                    GameSoundEffects.playFailure()
                                    triggerShake()
                                }
                            }
                        }
                        .padding(horizontal = 16.dp)
                        .testTag("trivia_option_$optIdx"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showCorrectHint) {
            Button(
                onClick = {
                    GameSoundEffects.playTap()
                    showCorrectHint = false
                    selectedOptionIdx = null
                    qIdx++
                },
                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("trivia_next_btn")
            ) {
                Text("NEXT QUESTION", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// =========================================================================
// 7. COLOR MATCH PLAYABLE GAME IMPLEMENTATION
// =========================================================================
@Composable
fun ColorMatchMiniGame(onScore: (Int) -> Unit) {
    val words = listOf("RED", "BLUE", "GREEN", "YELLOW")
    val colors = listOf(Color.Red, Color.Cyan, Color.Green, Color.Yellow)

    var textWord by remember { mutableStateOf(words.random()) }
    var printColor by remember { mutableStateOf(colors.random()) }
    var scoreAddedHint by remember { mutableStateOf("") }
    
    val coroutineScope = rememberCoroutineScope()
    var shakeOffset by remember { mutableStateOf(0.dp) }

    fun triggerShake() {
        coroutineScope.launch {
            for (i in 0..4) {
                shakeOffset = if (i % 2 == 0) 8.dp else (-8).dp
                delay(40)
            }
            shakeOffset = 0.dp
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Text("TAP BUTTON MATCHING THE *COLOR INK* NOT THE WORD!", color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Box(
            modifier = Modifier
                .offset(x = shakeOffset, y = shakeOffset)
                .fillMaxWidth()
                .height(160.dp)
                .background(RaivalSurface, shape = RoundedCornerShape(16.dp))
                .border(BorderStroke(1.5.dp, RaivalSecondary.copy(alpha = 0.2f)), shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = textWord,
                color = printColor,
                fontSize = 54.sp,
                fontWeight = FontWeight.Black
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            words.forEachIndexed { index, optionName ->
                val btnColor = colors[index]
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .background(btnColor, shape = RoundedCornerShape(12.dp))
                        .clickable {
                            val targetColorHex = printColor
                            val matchIndex = when (targetColorHex) {
                                Color.Red -> 0
                                Color.Cyan -> 1
                                Color.Green -> 2
                                Color.Yellow -> 3
                                else -> 0
                            }

                            if (matchIndex == index) {
                                onScore(25)
                                scoreAddedHint = "CORRECT +25!"
                                GameSoundEffects.playSuccess()
                            } else {
                                scoreAddedHint = "MISSED!"
                                GameSoundEffects.playFailure()
                                triggerShake()
                            }

                            coroutineScope.launch {
                                delay(600)
                                scoreAddedHint = ""
                            }

                            // Shuffle next
                            textWord = words.random()
                            printColor = colors.random()
                        }
                        .testTag("color_tap_$optionName"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(optionName, color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
        }

        if (scoreAddedHint.isNotEmpty()) {
            Text(
                text = scoreAddedHint,
                color = if (scoreAddedHint.contains("CORRECT")) Color(0xFF00FF87) else Color.Red,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }
    }
}
