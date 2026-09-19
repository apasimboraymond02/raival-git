package com.example.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaivalViewModel

@SuppressLint("DefaultLocale")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    user: User,
    viewModel: RaivalViewModel,
    isCurrentUser: Boolean,
    onBack: () -> Unit,
    onStatClick: ((Int) -> Unit)? = null,
    onSelectUser: ((User) -> Unit)? = null,
    showWalletInitially: Boolean = false,
    onOpenAdmin: (() -> Unit)? = null
) {
    val allTournaments by viewModel.tournaments.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    
    // Dynamic registrations fetch for the selected user
    val registrations by remember(user.id) {
        viewModel.getRegistrationsForUser(user.id)
    }.collectAsState(initial = emptyList())

    // Dynamic upcoming match schedules fetch for the selected user
    val upcomingMatches by remember(user.username) {
        viewModel.getMatchesForUser(user.username)
    }.collectAsState(initial = emptyList())

    // Editing State for bio & details
    var isEditing by remember { mutableStateOf(false) }
    var editFullName by remember { mutableStateOf(user.fullName) }
    var editPhone by remember { mutableStateOf(user.phone) }
    var editBio by remember { mutableStateOf(user.bio.ifEmpty { "DLS Arena Contender ⚽" }) }
    var editDlsHandle by remember { mutableStateOf(user.dlsHandle) }
    var editEfootballHandle by remember { mutableStateOf(user.efootballHandle) }
    var editDiscordHandle by remember { mutableStateOf(user.discordHandle) }
    var editGhanaRegion by remember { mutableStateOf(user.ghanaRegion) }
    var editGhanaHometown by remember { mutableStateOf(user.ghanaHometown) }
    var editPreferredGame by remember { mutableStateOf(user.preferredGame) }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf("") }
    var showProgressionHub by remember { mutableStateOf(false) }
    var showLeaderboard by remember { mutableStateOf(false) }
    var showWallet by remember(showWalletInitially) { mutableStateOf(showWalletInitially) }

    // Derive Rank based on allUsers sorted list
    val rankIndex = allUsers.indexOfFirst { it.id == user.id }
    val rankDisplay = if (rankIndex != -1) "#${rankIndex + 1}" else "Unranked"

    // Derive DLS Player Rating Card stats (PAC, SHO, PAS, DRI, DEF, PHY)
    // Dynamic values based on their actual stats (wins, losses, SP)
    val totalMatches = user.wins + user.losses
    val winRate = if (totalMatches > 0) (user.wins.toDouble() / totalMatches * 100).toInt() else 50
    val ovrRating = (60 + (user.raivalPoints / 35).coerceIn(0, 39)) // Caps at 99 OVR
    
    val pace = (70 + (user.wins % 25)).coerceIn(50, 99)
    val shooting = (65 + (user.wins % 30)).coerceIn(50, 99)
    val passing = (75 + (user.raivalPoints / 40)).coerceIn(50, 99)
    val dribbling = (72 + (user.winStreak * 4)).coerceIn(50, 99)
    val defending = (60 + (user.losses % 15)).coerceIn(50, 99)
    val physicality = (68 + (user.tournamentsPlayed % 15)).coerceIn(50, 99)

    // Tier / Division Name
    val divisionName = when {
        user.raivalPoints >= 1000 -> "Legendary Division"
        user.raivalPoints >= 500 -> "Professional Division"
        user.raivalPoints >= 200 -> "Semi-Pro Division"
        else -> "Amateur Division"
    }

    val divisionColor = when {
        user.raivalPoints >= 1000 -> RaivalSecondary // Amber/Gold
        user.raivalPoints >= 500 -> Color(0xFF00D4FF) // Cyan
        user.raivalPoints >= 200 -> Color(0xFF9E9EAF) // Silver/Grey
        else -> Color(0xFFCD7F32) // Bronze
    }

    // List of trophies / badges based on achievements
    val badges = remember(user) {
        val list = mutableListOf<Triple<String, String, ImageVector>>()
        list.add(Triple("DLS Player", "Official Raival Mobile competitor", Icons.Default.SportsEsports))
        if (user.wins >= 25) {
            list.add(Triple("Elite Striker", "Has scored dozens of tournament wins", Icons.Default.SportsBasketball))
        }
        if (user.winStreak >= 3) {
            list.add(Triple("Hot Streak", "On a fiery active win streak", Icons.Default.Whatshot))
        }
        if (user.raivalPoints >= 1000) {
            list.add(Triple("Golden Manager", "Reached Legendary 1000+ SP Club", Icons.Default.EmojiEvents))
        }
        if (user.totalWinnings >= 200.0) {
            list.add(Triple("Cash King", "Earned over ₵200 cash in the arena", Icons.Default.Payments))
        }
        if (user.coinBalance >= 500) {
            list.add(Triple("Coin Tycoon", "Holding more than 500 DLS coins", Icons.Default.MonetizationOn))
        }
        list
    }

    if (showProgressionHub) {
        RaivalProgressionHubScreen(
            viewModel = viewModel,
            user = user,
            onBack = { showProgressionHub = false }
        )
        return
    }

    if (showLeaderboard) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Global Leaderboard", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { showLeaderboard = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LeaderboardTab(viewModel, currentUser = viewModel.currentUser.collectAsState().value ?: user) { selectedUser ->
                    onSelectUser?.invoke(selectedUser)
                    showLeaderboard = false
                }
            }
        }
        return
    }

    if (showWallet) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Wallet & Analytics", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { showWallet = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                WalletAndAnalyticsTab(viewModel, currentUser = viewModel.currentUser.collectAsState().value ?: user)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isCurrentUser) "My Profile" else "${user.username}'s Profile",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isCurrentUser && onOpenAdmin != null && user.role == "admin") {
                        IconButton(onClick = onOpenAdmin) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Admin Panel",
                                tint = RaivalPrimary
                            )
                        }
                    }
                    if (isCurrentUser) {
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("profile_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = RaivalError
                            )
                        }
                        
                        IconButton(
                            onClick = { 
                                if (isEditing) {
                                    // Save
                                    isSaving = true
                                    viewModel.updateProfile(
                                        fullName = editFullName,
                                        phone = editPhone,
                                        bio = editBio,
                                        dlsHandle = editDlsHandle,
                                        efootballHandle = editEfootballHandle,
                                        discordHandle = editDiscordHandle,
                                        ghanaRegion = editGhanaRegion,
                                        ghanaHometown = editGhanaHometown,
                                        preferredGame = editPreferredGame
                                    ) { success ->
                                        isSaving = false
                                        if (success) {
                                            isEditing = false
                                            saveMessage = "Profile updated successfully!"
                                        } else {
                                            saveMessage = "Error updating profile."
                                        }
                                    }
                                } else {
                                    isEditing = true
                                }
                            },
                            modifier = Modifier.testTag("edit_profile_toggle")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = RaivalPrimary, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(
                                    imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = if (isEditing) "Save" else "Edit Profile",
                                    tint = RaivalPrimary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(RaivalSurface)
                .padding(innerPadding)
                .testTag("user_profile_scrollable"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Info Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(RaivalPrimary, RaivalSecondary)
                                    ),
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .background(RaivalSurface, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val avatarEmoji = when (user.selectedAvatar) {
                                "avatar_default" -> "⚽"
                                "avatar_samurai" -> "🥷"
                                "avatar_coach" -> "🧠"
                                "avatar_gold" -> "👑"
                                "avatar_sniper" -> "🎯"
                                "avatar_cadet" -> "🧑‍🚀"
                                "avatar_gold_boot" -> "🏆"
                                "avatar_phoenix" -> "🐦"
                                "avatar_ninja" -> "⚔️"
                                "avatar_tiger" -> "🐯"
                                "avatar_wizard" -> "🧙"
                                "avatar_electro" -> "⚡"
                                else -> if (user.selectedAvatar.length <= 2) user.selectedAvatar else "⚽"
                            }
                            Text(
                                text = avatarEmoji,
                                fontSize = 44.sp,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black
                                )
                            )
                            // Small floating badge for division
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(26.dp)
                                    .background(divisionColor, shape = CircleShape)
                                    .border(2.dp, RaivalSurface, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (user.raivalPoints >= 1000) Icons.Default.Star else Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = editFullName,
                                onValueChange = { editFullName = it },
                                label = { Text("Full Name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RaivalPrimary,
                                    focusedLabelColor = RaivalPrimary,
                                    unfocusedBorderColor = RaivalTextSecondary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Phone Number") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RaivalPrimary,
                                    focusedLabelColor = RaivalPrimary,
                                    unfocusedBorderColor = RaivalTextSecondary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editBio,
                                onValueChange = { editBio = it },
                                label = { Text("Short Bio") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RaivalPrimary,
                                    focusedLabelColor = RaivalPrimary,
                                    unfocusedBorderColor = RaivalTextSecondary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editDlsHandle,
                                onValueChange = { editDlsHandle = it },
                                label = { Text("DLS Friend Code") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RaivalPrimary,
                                    focusedLabelColor = RaivalPrimary,
                                    unfocusedBorderColor = RaivalTextSecondary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editEfootballHandle,
                                onValueChange = { editEfootballHandle = it },
                                label = { Text("eFootball ID") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RaivalPrimary,
                                    focusedLabelColor = RaivalPrimary,
                                    unfocusedBorderColor = RaivalTextSecondary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editDiscordHandle,
                                onValueChange = { editDiscordHandle = it },
                                label = { Text("Discord Username") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RaivalPrimary,
                                    focusedLabelColor = RaivalPrimary,
                                    unfocusedBorderColor = RaivalTextSecondary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editGhanaRegion,
                                onValueChange = { editGhanaRegion = it },
                                label = { Text("Ghana Region") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RaivalPrimary,
                                    focusedLabelColor = RaivalPrimary,
                                    unfocusedBorderColor = RaivalTextSecondary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editGhanaHometown,
                                onValueChange = { editGhanaHometown = it },
                                label = { Text("City / Hometown") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RaivalPrimary,
                                    focusedLabelColor = RaivalPrimary,
                                    unfocusedBorderColor = RaivalTextSecondary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editPreferredGame,
                                onValueChange = { editPreferredGame = it },
                                label = { Text("Preferred Esports Game") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RaivalPrimary,
                                    focusedLabelColor = RaivalPrimary,
                                    unfocusedBorderColor = RaivalTextSecondary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                        } else {
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RaivalPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = if (user.bio.isNotEmpty()) user.bio else "DLS Arena Contender ⚽",
                                style = MaterialTheme.typography.bodySmall,
                                color = RaivalTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .clickable(enabled = onStatClick != null) { onStatClick?.invoke(2) }
                                    .background(RaivalAccent.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, RaivalAccent.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = RaivalAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "LEVEL ${user.level} • ${user.xp} XP",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = RaivalAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(Color(0xFF0F2C11).copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)), shape = RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "Cloud",
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "SECURED IN SUPABASE CLOUD DATABASE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = Color(0xFF81C784),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            if (saveMessage.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = saveMessage,
                                    color = RaivalSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = RaivalPrimary.copy(alpha = 0.15f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "DLS Friend Code", fontSize = 10.sp, color = RaivalTextSecondary)
                                    Text(text = user.dlsHandle.ifEmpty { "Not Set" }, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = RaivalSecondary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "eFootball ID", fontSize = 10.sp, color = RaivalTextSecondary)
                                    Text(text = user.efootballHandle.ifEmpty { "Not Set" }, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color(0xFF5865F2), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Discord Tag", fontSize = 10.sp, color = RaivalTextSecondary)
                                    Text(text = user.discordHandle.ifEmpty { "Not Set" }, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // PRO Hub Launcher Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable { showProgressionHub = true },
                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, RaivalSecondary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(RaivalSecondary.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = RaivalSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RAIVAL PRO HUB",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Skill Tree, Daily Missions, Streak Shields & XP",
                                fontSize = 12.sp,
                                color = RaivalTextSecondary
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = RaivalTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Arena Services & Portal Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ARENA SERVICES & PORTAL",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = RaivalPrimary,
                        modifier = Modifier.padding(bottom = 4.dp).testTag("arena_portal_title")
                    )

                    // 1. Leaderboard Portal Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLeaderboard = true }
                            .testTag("leaderboard_portal_card"),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(RaivalPrimary.copy(alpha = 0.15f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Leaderboard,
                                    contentDescription = null,
                                    tint = RaivalPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GLOBAL LEADERBOARD",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "View live tournament rankings & top contenders",
                                    fontSize = 11.sp,
                                    color = RaivalTextSecondary
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = RaivalTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 2. Wallet & Analytics Portal Card (Only show if viewing own profile)
                    if (isCurrentUser) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showWallet = true }
                                .testTag("wallet_portal_card"),
                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(RaivalSecondary.copy(alpha = 0.15f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wallet,
                                        contentDescription = null,
                                        tint = RaivalSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "MY WALLET & ANALYTICS",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Manage coins, cash winnings, and view match stats",
                                        fontSize = 11.sp,
                                        color = RaivalTextSecondary
                                    )
                                }
                                
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = RaivalTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // DLS FUT/Ultimate Team Style Card Section
            item {
                Column {
                    Text(
                        text = "DLS ULTIMATE TEAM PLAYER CARD",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = RaivalPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // The Ultimate DLS Player Card Frame
                        Card(
                            modifier = Modifier
                                .width(220.dp)
                                .height(320.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF232526),
                                                Color(0xFF414345)
                                            )
                                        )
                                    )
                                    .border(
                                        border = BorderStroke(
                                            width = 3.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    RaivalSecondary,
                                                    Color(0xFFFFD700),
                                                    RaivalPrimary
                                                )
                                            )
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                // Background graphic representing soccer pitch overlay
                                Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
                                    drawCircle(
                                        color = Color.White,
                                        radius = size.width / 2,
                                        center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Header of card (OVR + Pos)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$ovrRating",
                                                fontSize = 42.sp,
                                                fontWeight = FontWeight.Black,
                                                color = RaivalSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "MID",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        
                                        // Ghana flag shield or custom badge graphic
                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(modifier = Modifier.height(14.dp)) {
                                                Box(modifier = Modifier.width(10.dp).fillMaxHeight().background(Color(0xFFFF0000)))
                                                Box(modifier = Modifier.width(10.dp).fillMaxHeight().background(Color(0xFFFCD116)))
                                                Box(modifier = Modifier.width(10.dp).fillMaxHeight().background(Color(0xFF006B3F)))
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "GHA",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = RaivalTextSecondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    // Player Name inside Card
                                    Text(
                                        text = user.username.uppercase(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(RaivalSecondary.copy(alpha = 0.4f))
                                            .padding(vertical = 4.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Stats Grid Layout
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = "PAC", fontSize = 10.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                            Text(text = "$pace", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Black)
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            Text(text = "SHO", fontSize = 10.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                            Text(text = "$shooting", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Black)
                                        }
                                        
                                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(RaivalSecondary.copy(alpha = 0.2f)))
                                        
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = "PAS", fontSize = 10.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                            Text(text = "$passing", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Black)
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            Text(text = "DRI", fontSize = 10.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                            Text(text = "$dribbling", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Black)
                                        }
                                        
                                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(RaivalSecondary.copy(alpha = 0.2f)))

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = "DEF", fontSize = 10.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                            Text(text = "$defending", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Black)
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            Text(text = "PHY", fontSize = 10.sp, color = RaivalTextSecondary, fontWeight = FontWeight.Bold)
                                            Text(text = "$physicality", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ghanaian Esports Pro Profile / Card
            item {
                Column {
                    Text(
                        text = "GHANAIAN ESPORTS PRO STANDING",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = RaivalPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, RaivalSecondary.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Row with Ghanaian Flag, Hometown & Game
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Ghanaian Flag decorative avatar / box
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(RaivalSurface, shape = CircleShape)
                                        .border(BorderStroke(2.dp, RaivalSecondary), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Stylized Ghana flag
                                    Column(modifier = Modifier.size(24.dp, 16.dp)) {
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFFFF0000))) // Red
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFFFCD116)), contentAlignment = Alignment.Center) { // Yellow with small black star
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(8.dp)
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF006B3F))) // Green
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (user.ghanaHometown.isNotEmpty()) "${user.ghanaHometown}, ${user.ghanaRegion}" else "Accra, Greater Accra",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Official National Esports Athlete",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RaivalTextSecondary
                                    )
                                }

                                // Preferred Game Badge
                                Box(
                                    modifier = Modifier
                                        .background(RaivalPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                        .border(BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.4f)), shape = RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = user.preferredGame.ifEmpty { "Dream League Soccer" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RaivalPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RaivalTextSecondary.copy(alpha = 0.1f)))

                            Spacer(modifier = Modifier.height(16.dp))

                            // Stats Grid: National Rank, Regional Rank, Total Earnings
                            val ghanaPlayers = allUsers.filter { it.ghanaRegion.isNotEmpty() }.sortedByDescending { it.raivalPoints }
                            val nationalRankIdx = ghanaPlayers.indexOfFirst { it.id == user.id }
                            val nationalRank = if (nationalRankIdx != -1) "#${nationalRankIdx + 1}" else "Rank Pending"

                            val regionalPlayers = allUsers.filter { it.ghanaRegion.equals(user.ghanaRegion, ignoreCase = true) }.sortedByDescending { it.raivalPoints }
                            val regionalRankIdx = regionalPlayers.indexOfFirst { it.id == user.id }
                            val regionalRank = if (regionalRankIdx != -1) "#${regionalRankIdx + 1}" else "Rank Pending"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // National Standing Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = RaivalSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "NATIONAL RANK",
                                        fontSize = 10.sp,
                                        color = RaivalTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = nationalRank,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = RaivalSecondary
                                    )
                                }

                                // Regional Standing Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF00D4FF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${user.ghanaRegion.uppercase()} RANK",
                                        fontSize = 10.sp,
                                        color = RaivalTextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = regionalRank,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF00D4FF)
                                    )
                                }

                                // Total Winnings Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "TOTAL WINNINGS",
                                        fontSize = 10.sp,
                                        color = RaivalTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "₵ ${user.totalWinnings.toInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Mini regional leaderboard preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(RaivalSurface, shape = RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "REGIONAL COMPETITIVE INSIGHTS",
                                        fontSize = 10.sp,
                                        color = RaivalPrimary,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "There are ${regionalPlayers.size} registered pro athletes competing in the ${user.ghanaRegion} Region. Your win percentage of $winRate% places you in the top ${if (winRate > 75) "10%" else if (winRate > 50) "35%" else "75%"} of regional rosters.",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Community Ranking & Division Info Card
            item {
                Column {
                    Text(
                        text = "COMMUNITY STANDING",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = RaivalPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onStatClick != null) { onStatClick?.invoke(1) },
                        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = divisionName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = divisionColor
                                )
                                Text(
                                    text = "Earn more Raival Points to advance divisions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RaivalTextSecondary
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .background(RaivalSurface, shape = RoundedCornerShape(12.dp))
                                        .border(BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Rank: $rankDisplay",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = RaivalSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stats Grid Section
            item {
                Column {
                    Text(
                        text = "GAMER CORE STATISTICS",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = RaivalPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Wins Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = RaivalPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Wins", style = MaterialTheme.typography.bodySmall, color = RaivalTextSecondary)
                                Text(text = "${user.wins}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Losses Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = Color.Red)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Losses", style = MaterialTheme.typography.bodySmall, color = RaivalTextSecondary)
                                Text(text = "${user.losses}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Win Rate Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Percent, contentDescription = null, tint = RaivalSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Win %", style = MaterialTheme.typography.bodySmall, color = RaivalTextSecondary)
                                Text(text = "$winRate%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Current Streak Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFFF4500))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Win Streak", style = MaterialTheme.typography.bodySmall, color = RaivalTextSecondary)
                                Text(text = "🔥 ${user.winStreak}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Total Winnings Cash
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = RaivalSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Cash Won", style = MaterialTheme.typography.bodySmall, color = RaivalTextSecondary)
                                Text(text = "₵${user.totalWinnings.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // DLS Coins Balance
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = onStatClick != null) { onStatClick?.invoke(0) },
                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Paid, contentDescription = null, tint = RaivalPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "DLS Coins", style = MaterialTheme.typography.bodySmall, color = RaivalTextSecondary)
                                Text(text = "₵${user.coinBalance}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Marketplace Seller Transparency Section
            item {
                val userTxs by remember(user.id) {
                    viewModel.repository.getMarketplaceTransactionsForUser(user.id)
                }.collectAsState(initial = emptyList())

                val sales = userTxs.filter { it.sellerId == user.id }
                val completedSales = sales.filter { it.status == "Completed" }
                val disputedSales = sales.filter { it.status == "Disputed" }
                val pendingSales = sales.filter { it.status in listOf("Payment Held", "Team Transferred", "Team Verified") }
                
                val completedCount = completedSales.size
                val disputedCount = disputedSales.size
                
                val totalVolume = completedSales.sumOf { it.amount }
                
                var trustScore = 100
                var trustText = "Provisional (New Seller)"
                var trustColor = RaivalPrimary
                var trustIcon = Icons.Default.VerifiedUser

                val totalRated = completedCount + disputedCount
                if (totalRated > 0) {
                    trustScore = (completedCount * 100) / totalRated
                    if (trustScore >= 95) {
                        trustText = "Elite Trader (Excellent)"
                        trustColor = RaivalSuccess
                        trustIcon = Icons.Default.Verified
                    } else if (trustScore >= 85) {
                        trustText = "Trusted Seller (Good)"
                        trustColor = RaivalSecondary
                        trustIcon = Icons.Default.CheckCircle
                    } else if (trustScore >= 70) {
                        trustText = "Fair Trader (Neutral)"
                        trustColor = Color.Yellow
                        trustIcon = Icons.Default.Info
                    } else {
                        trustText = "High Risk (Caution)"
                        trustColor = RaivalError
                        trustIcon = Icons.Default.Warning
                    }
                }

                Column {
                    Text(
                        text = "🛒 MARKETPLACE TRANSPARENCY & TRUST",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = RaivalPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("seller_trust_card"),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                        border = BorderStroke(1.dp, trustColor.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Trust Score Heading and Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically, 
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = trustIcon,
                                        contentDescription = null,
                                        tint = trustColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Seller Trust Score",
                                            fontSize = 12.sp,
                                            color = RaivalTextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = trustText,
                                            fontSize = 14.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Large Score Percentage Circle Gauge
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .drawBehind {
                                            drawArc(
                                                color = Color.White.copy(alpha = 0.1f),
                                                startAngle = 0f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                style = Stroke(width = 4.dp.toPx())
                                            )
                                            drawArc(
                                                color = trustColor,
                                                startAngle = -90f,
                                                sweepAngle = (trustScore.toFloat() / 100f) * 360f,
                                                useCenter = false,
                                                style = Stroke(width = 4.dp.toPx())
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$trustScore%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = trustColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = RaivalPrimary.copy(alpha = 0.1f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Stats Breakdown Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${completedCount}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RaivalSuccess
                                    )
                                    Text(
                                        text = "Completed Sales",
                                        fontSize = 10.sp,
                                        color = RaivalTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Box(modifier = Modifier.width(1.dp).height(32.dp).background(RaivalPrimary.copy(alpha = 0.1f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${disputedCount}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (disputedCount > 0) RaivalError else Color.White
                                    )
                                    Text(
                                        text = "Disputes",
                                        fontSize = 10.sp,
                                        color = RaivalTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Box(modifier = Modifier.width(1.dp).height(32.dp).background(RaivalPrimary.copy(alpha = 0.1f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${pendingSales.size}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RaivalSecondary
                                    )
                                    Text(
                                        text = "Pending Escrow",
                                        fontSize = 10.sp,
                                        color = RaivalTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Box(modifier = Modifier.width(1.dp).height(32.dp).background(RaivalPrimary.copy(alpha = 0.1f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "₵${String.format("%.0f", totalVolume)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RaivalSecondary
                                    )
                                    Text(
                                        text = "Volume Traded",
                                        fontSize = 10.sp,
                                        color = RaivalTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Transparency Note or Warnings
                            if (disputedCount > 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(RaivalError.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = RaivalError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Disputed transactions detected. Always trade carefully and ensure listing details match before releasing escrow.",
                                        fontSize = 10.sp,
                                        color = RaivalError,
                                        lineHeight = 13.sp
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(RaivalSuccess.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = RaivalSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "This seller is fully protected by Raival's automated escrow verification mechanism. 100% funds protection guaranteed.",
                                        fontSize = 10.sp,
                                        color = RaivalSuccess,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Achievements / Badges Row
            item {
                Column {
                    Text(
                        text = "COMMUNITY BADGES & ACCOMPLISHMENTS",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = RaivalPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (badges.isEmpty()) {
                                Text(
                                    text = "No badges earned yet. Compete in tournaments to claim your glory!",
                                    color = RaivalTextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    badges.forEach { (name, desc, icon) ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(80.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(RaivalSurface, shape = CircleShape)
                                                    .border(BorderStroke(1.5.dp, RaivalSecondary), shape = CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = name,
                                                    tint = RaivalSecondary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = desc,
                                                fontSize = 8.sp,
                                                color = RaivalTextSecondary,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 10.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tournament History List
            item {
                Column {
                    Text(
                        text = "TOURNAMENT PARTICIPATION HISTORY",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = RaivalPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (registrations.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = RaivalTextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No tournament registrations found.",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Join active tournaments under the Home tab to begin your esport legacy.",
                                    color = RaivalTextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            registrations.forEach { reg ->
                                val matchedTournament = allTournaments.find { it.id == reg.tournamentId }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(RaivalSurface, shape = RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EmojiEvents,
                                                contentDescription = null,
                                                tint = RaivalSecondary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = matchedTournament?.title ?: "DLS Championship Arena",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "IGN: ${reg.inGameName}",
                                                    fontSize = 11.sp,
                                                    color = RaivalPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "•",
                                                    fontSize = 11.sp,
                                                    color = RaivalTextSecondary
                                                )
                                                Text(
                                                    text = matchedTournament?.date ?: "Upcoming",
                                                    fontSize = 11.sp,
                                                    color = RaivalTextSecondary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(horizontalAlignment = Alignment.End) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = when (matchedTournament?.status) {
                                                            "Open" -> RaivalPrimary.copy(alpha = 0.15f)
                                                            else -> RaivalSecondary.copy(alpha = 0.15f)
                                                        },
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = matchedTournament?.status ?: "Completed",
                                                    color = when (matchedTournament?.status) {
                                                        "Open" -> RaivalPrimary
                                                        else -> RaivalSecondary
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Prize: ₵${matchedTournament?.prize?.toInt() ?: 150}",
                                                fontSize = 11.sp,
                                                color = RaivalSecondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Upcoming Match Schedules
            item {
                Column {
                    Text(
                        text = "UPCOMING MATCH SCHEDULES",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = RaivalPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (upcomingMatches.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = RaivalTextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No active matches scheduled.",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Brackets are generated once the tournament is closed/started by administrators.",
                                    color = RaivalTextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val registeredNames = registrations.map { it.playerName } + user.username
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            upcomingMatches.forEach { match ->
                                val matchedTournament = allTournaments.find { it.id == match.tournamentId }
                                val isP1Self = registeredNames.any { it.equals(match.player1Name, ignoreCase = true) }
                                val isP2Self = registeredNames.any { it.equals(match.player2Name, ignoreCase = true) }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                    border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = matchedTournament?.title ?: "DLS Championship Arena",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = RaivalSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = if (match.status == "Pending") RaivalPrimary.copy(alpha = 0.15f) else Color.Green.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = match.status,
                                                    color = if (match.status == "Pending") RaivalPrimary else Color.Green,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Text(
                                            text = match.round,
                                            fontSize = 11.sp,
                                            color = RaivalTextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Player 1
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = match.player1Name,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isP1Self) FontWeight.Black else FontWeight.Normal,
                                                    color = if (isP1Self) RaivalPrimary else Color.White,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = match.player1Score?.toString() ?: "-",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
                                                )
                                            }
                                            
                                            Text(
                                                text = "VS",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RaivalTextSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                            
                                            // Player 2
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = match.player2Name,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isP2Self) FontWeight.Black else FontWeight.Normal,
                                                    color = if (isP2Self) RaivalPrimary else Color.White,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = match.player2Score?.toString() ?: "-",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black,
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
        }
    }
}
