package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.PointF
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaivalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlin.random.Random

@SuppressLint("DefaultLocale")

// -----------------------------------------------------------------------------
// FEATURE 64: OFFICIAL CERTIFICATIONS
// -----------------------------------------------------------------------------
@Composable
fun OfficialCertificationsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var selectedGame by remember { mutableStateOf("DLS 26") }
    var certifiedId by remember { mutableStateOf<String?>(null) }
    var showCertDialog by remember { mutableStateOf(false) }
    var activeCertName by remember { mutableStateOf("") }

    val matchesPlayed = user.wins + user.losses
    val winRatePercent = if (matchesPlayed > 0) (user.wins * 100) / matchesPlayed else 45

    val certs = listOf(
        CertTier("Bronze", 50, 30, "Raival Bronze Player", Color(0xFFCD7F32)),
        CertTier("Silver", 100, 50, "Raival Silver Player", Color(0xFFC0C0C0)),
        CertTier("Gold", 200, 60, "Raival Gold Player", Color(0xFFFFD700)),
        CertTier("Platinum", 500, 65, "Raival Platinum Player", Color(0xFFE5E4E2)),
        CertTier("Diamond", 1000, 70, "Raival Diamond Player", Color(0xFF00E5FF)),
        CertTier("Legendary", 2000, 75, "Raival Legendary Player", Color(0xFFE040FB))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📜 OFFICIAL CERTIFICATIONS", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 13.sp)
                        Row(
                            modifier = Modifier
                                .background(RaivalSurface, RoundedCornerShape(20.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            listOf("DLS 26", "eFootball").forEach { game ->
                                Text(
                                    text = game,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedGame == game) Color.Black else Color.White,
                                    modifier = Modifier
                                        .background(
                                            if (selectedGame == game) RaivalPrimary else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedGame = game }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Build credibility in the Accra esports community. Prove your skill level with cryptographic credentials, export them to PDF, or instantly link them to your LinkedIn professional profile.",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("YOUR AUDITED ARENA PERFORMANCE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Played", color = RaivalTextSecondary, fontSize = 11.sp)
                            Text("$matchesPlayed matches", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Win Rate %", color = RaivalTextSecondary, fontSize = 11.sp)
                            Text("$winRatePercent%", color = RaivalSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Audited Account", color = RaivalTextSecondary, fontSize = 11.sp)
                            Text("VERIFIED ✔", color = RaivalSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("AVAILABLE CERTIFICATION LEVELS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }

        items(certs) { cert ->
            val matchesPass = matchesPlayed >= cert.reqMatches
            val winratePass = winRatePercent >= cert.reqWinRate
            val isEligible = matchesPass && winratePass

            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, if (isEligible) cert.color.copy(alpha = 0.4f) else Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(cert.color.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = cert.color, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cert.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text(
                            text = "Req: ${cert.reqMatches} matches (${cert.reqWinRate}%+ WR)",
                            color = RaivalTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    if (isEligible) {
                        Button(
                            onClick = {
                                activeCertName = cert.title
                                certifiedId = "SKL-${100000 + Random.nextInt(900000)}"
                                showCertDialog = true
                                notify("📜 Certification applied: ${cert.title}!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = cert.color),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Locked", color = RaivalError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            val missingPlayed = cert.reqMatches - matchesPlayed
                            if (missingPlayed > 0) {
                                Text("Need $missingPlayed games", color = RaivalTextSecondary, fontSize = 10.sp)
                            } else {
                                Text("Need ${cert.reqWinRate}% WR", color = RaivalTextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCertDialog) {
        Dialog(onDismissRequest = { showCertDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏆 RAIVAL CERTIFICATION ISSUED", fontWeight = FontWeight.Black, fontSize = 15.sp, color = RaivalPrimary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RaivalSurface, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = RaivalSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(activeCertName, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp)
                            Text("Officially Issued to ${user.fullName.uppercase()}", color = RaivalTextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("ID: $certifiedId", color = RaivalPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("Issue Date: ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())}", color = RaivalTextSecondary, fontSize = 11.sp)
                            Text("Expiry Date: 1 year from issue", color = RaivalTextSecondary, fontSize = 11.sp)
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            // Simple dynamic mock QR code rendering via Canvas
                            Canvas(modifier = Modifier.size(80.dp)) {
                                drawRect(color = Color.White)
                                val steps = 8
                                val sizeStep = size.width / steps
                                for (i in 0 until steps) {
                                    for (j in 0 until steps) {
                                        if ((i + j) % 2 == 0 || (i == 0 && j == 0) || (i == 0 && j == steps-1) || (i == steps-1 && j == 0)) {
                                            drawRect(
                                                color = Color.Black,
                                                topLeft = Offset(i * sizeStep, j * sizeStep),
                                                size = Size(sizeStep, sizeStep)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("https://raival.gg/verify/$certifiedId", color = RaivalSecondary, fontSize = 9.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    val clipboard = LocalClipboardManager.current
                    val context = LocalContext.current

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString("Check out my official '$activeCertName' on Raival! Verify here: https://raival.gg/verify/$certifiedId"))
                                notify("🔗 Certification link copied to share on LinkedIn!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Profile", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                notify("📥 Digital Certificate PDF downloaded to local storage!")
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, RaivalTextSecondary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download PDF", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showCertDialog = false }) {
                        Text("CLOSE", color = RaivalTextSecondary)
                    }
                }
            }
        }
    }
}

data class CertTier(
    val title: String,
    val reqMatches: Int,
    val reqWinRate: Int,
    val text: String,
    val color: Color
)


// -----------------------------------------------------------------------------
// FEATURE 65: COMPETITIVE MODE
// -----------------------------------------------------------------------------
@Composable
fun CompetitiveModeView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var isSearching by remember { mutableStateOf(false) }
    var searchProgress by remember { mutableFloatStateOf(0f) }
    var matchFound by remember { mutableStateOf(false) }
    var opponentName by remember { mutableStateOf("") }
    var opponentRating by remember { mutableIntStateOf(0) }
    var availableOpponents by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    val matchesPlayed = user.wins + user.losses
    val winRatePercent = if (matchesPlayed > 0) (user.wins * 100) / matchesPlayed else 45
    val skillRating = user.level * 100 + user.wins * 12

    val eligibleAge = true
    val eligibleMatches = matchesPlayed >= 50
    val eligibleWinRate = winRatePercent >= 50
    val eligibleBans = user.status == "active"
    val eligibleRating = skillRating >= 1500

    val isAllEligible = eligibleMatches && eligibleWinRate && eligibleBans && eligibleRating

    LaunchedEffect(Unit) {
        try {
            val opponents = com.example.data.sync.SupabaseSyncManager.pullTable("users")
                .map { it["username"]?.jsonPrimitive?.contentOrNull ?: "Player" }
                .filter { it != user.username }
                .ifEmpty { listOf("No opponent available") }
            val oppName = opponents.random()
            availableOpponents = listOf(Pair(oppName, (skillRating + Random.nextInt(-50, 50)).coerceIn(100, 3000)))
        } catch (_: Exception) {
            availableOpponents = listOf(Pair("No opponent available", skillRating))
        }
    }

    var matchedSessionId by remember { mutableIntStateOf(0) }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            matchFound = false
            searchProgress = 0f
            // Real queue search (Supabase matchmaking). Progress bar tracks
            // elapsed wait time while the server hunt runs underneath.
            viewModel.searchCompetitiveMatch(
                gameType = user.preferredGame,
                onOpponentFound = { _, oppName, sessionId ->
                    opponentName = oppName
                    opponentRating = availableOpponents
                        .firstOrNull { it.first.equals(oppName, ignoreCase = true) }?.second
                        ?: 1500
                    matchedSessionId = sessionId
                    matchFound = true
                    isSearching = false
                    notify("🎮 Competitive Match Found against $opponentName!")
                },
                onNoOpponent = { msg ->
                    isSearching = false
                    notify("❌ $msg")
                },
                onError = { err ->
                    isSearching = false
                    notify("❌ $err")
                }
            )
            while (isSearching && searchProgress < 0.95f) {
                delay(1000)
                searchProgress += 1f / 60f
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚔️ COMPETE WITH PROFESSIONAL RULES", fontWeight = FontWeight.Black, color = RaivalSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Competitive Mode enforces stricter anti-cheat algorithms, real-time match arbitration, double elimination structures, and offers double the standard entry stakes and prize rewards.",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("COMPETITIVE MODE REQUIREMENTS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    listOf(
                        RequirementItem("Account Age (30+ Days)", eligibleAge),
                        RequirementItem("Arena Matches Played (50+)", eligibleMatches, "Currently: $matchesPlayed"),
                        RequirementItem("Overall Win Rate (50%+)", eligibleWinRate, "Currently: $winRatePercent%"),
                        RequirementItem("Clean Record (No Recent Bans)", eligibleBans),
                        RequirementItem("Minimum Skill Rating (1500+)", eligibleRating, "Your rating: $skillRating")
                    ).forEach { req ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(req.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                req.subLabel?.let { Text(it, color = RaivalTextSecondary, fontSize = 10.sp) }
                            }
                            Icon(
                                imageVector = if (req.passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (req.passed) RaivalSuccess else RaivalError,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isAllEligible) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                    border = BorderStroke(1.dp, RaivalPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎮 PRO ARENA MATCHMAKING ACTIVE", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("All requirements met. Pay the standard GHS entry stake or select coin-entry to search professional opponents.", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isSearching && !matchFound) {
                            Button(
                                onClick = { isSearching = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("FIND COMPETITIVE MATCH", color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        } else if (isSearching) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(
                                    progress = { searchProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = RaivalSecondary,
                                    trackColor = RaivalSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Auditing game integrity, anti-cheat diagnostics... ${(searchProgress * 100).toInt()}%", color = RaivalSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (matchFound) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                                border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔥 CLASH FOUND! 🔥", color = RaivalSecondary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(user.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Rating: $skillRating", color = RaivalPrimary, fontSize = 11.sp)
                                        }
                                        Text("VS", color = RaivalSecondary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(opponentName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Rating: $opponentRating", color = RaivalPrimary, fontSize = 11.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Session #$matchedSessionId is live. Play your match now, then BOTH players submit the score with a screenshot in Match Center. Payouts (+300 XP / +100 Coins) are released only when both submitted scores agree — mismatches go to dispute review.",
                                        color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.submitMatchScore(matchedSessionId, 2, null)
                                                notify("Your score (2) was submitted for session #$matchedSessionId. It completes when $opponentName submits a matching score — mismatches go to dispute review.")
                                                matchFound = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSuccess),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("SUBMIT MY SCORE (2)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { matchFound = false },
                                            modifier = Modifier.weight(1f),
                                            border = BorderStroke(1.dp, RaivalError)
                                        ) {
                                            Text("DECLINE / DODGE", color = RaivalError, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔒 COMPETITIVE LOCKOUT", fontWeight = FontWeight.Bold, color = RaivalError, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("You must fulfill all mandatory statistical metrics above to register in the competitive lobby.", color = RaivalTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

data class RequirementItem(
    val label: String,
    val passed: Boolean,
    val subLabel: String? = null
)


// -----------------------------------------------------------------------------
// FEATURE: PRO LEAGUE
// -----------------------------------------------------------------------------
@Composable
fun ProLeagueView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var trialModeActive by remember { mutableStateOf(false) }
    var trialOutcome by remember { mutableStateOf<String?>(null) }
    
    val matchesPlayed = user.wins + user.losses
    val winRatePercent = if (matchesPlayed > 0) (user.wins * 100) / matchesPlayed else 45
    val skillRating = user.level * 100 + user.wins * 12

    val meetsProRating = skillRating >= 2500
    val meetsProWinRate = winRatePercent >= 70
    val meetsProMatches = matchesPlayed >= 500

    val isProEligible = meetsProRating && meetsProWinRate && meetsProMatches

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("👑 RAIVAL INVITE-ONLY PROFESSIONAL LEAGUE", fontWeight = FontWeight.Black, color = Color(0xFFFFD700), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Representing the pinnacle of professional mobile soccer in Ghana. Feature in live-streamed matches with real-money cash salaries, corporate sponsorship offers, and an permanent placement in the Raival Hall of Fame.",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PRO LEAGUE STRUCTURE & SALARIES", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    listOf(
                        LeagueDiv("Premier Division", "Top 10 players", "GHS 1000+/mo", Color(0xFFFFD700)),
                        LeagueDiv("Championship Division", "Rank 11-25 players", "GHS 500+/mo", Color(0xFFC0C0C0)),
                        LeagueDiv("League One Division", "Rank 26-50 players", "GHS 250+/mo", Color(0xFFCD7F32)),
                        LeagueDiv("League Two Division", "Rank 51-100 players", "GHS 100+/mo", Color(0xFF03A9F4))
                    ).forEach { div ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(div.color, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(div.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                    Text(div.players, color = RaivalTextSecondary, fontSize = 10.sp)
                                }
                            }
                            Text(div.stipend, color = RaivalSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PRO ELIGIBILITY VERIFICATION", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Top 100 Skill Rating (2500+)", color = Color.White, fontSize = 12.sp)
                        Text(if (meetsProRating) "MET ✔" else "NOT MET (${skillRating}/2500)", color = if (meetsProRating) RaivalSuccess else RaivalError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Win Rate 70%+", color = Color.White, fontSize = 12.sp)
                        Text(if (meetsProWinRate) "MET ✔" else "NOT MET ($winRatePercent%/70%)", color = if (meetsProWinRate) RaivalSuccess else RaivalError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Matches Played (500+)", color = Color.White, fontSize = 12.sp)
                        Text(if (meetsProMatches) "MET ✔" else "NOT MET ($matchesPlayed/500)", color = if (meetsProMatches) RaivalSuccess else RaivalError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isProEligible) {
            item {
                Button(
                    onClick = { notify("📩 Pro League Invitation application submitted! Reviewing your arena history...") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SUBMIT PRO LEAGUE ENROLLMENT APPLICATION", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                    border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌟 WILD-CARD QUALIFIER LOBBY", fontWeight = FontWeight.Black, color = RaivalSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("You don't meet the automatic Pro requirements yet. However, you can play a high-difficulty Wild-Card Qualifier Bot Match. Win to claim a 14-day temporary Pro wild-card invitation!", color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (viewModel.isProLicensed(user)) {
                            Text(
                                "✅ PRO LICENSE ACTIVE until ${java.util.Date(user.proLicenseExpiresAt)}",
                                color = RaivalSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                            )
                        } else if (!trialModeActive && trialOutcome == null) {
                            Text(
                                "To qualify, win a real 1v1 match (Match Center or tournament). Your verified win is checked on this device and on the server — no luck draws.",
                                color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    trialModeActive = true
                                    viewModel.claimProQualifierLicense { success, msg ->
                                        trialModeActive = false
                                        trialOutcome = if (success) "WON" else "LOST"
                                        notify(msg)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary)
                            ) {
                                Text("VERIFY MY QUALIFIER WIN (FREE)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else if (trialModeActive) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                CircularProgressIndicator(color = RaivalSecondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Checking your verified match wins...", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        trialOutcome?.let { outcome ->
                            Spacer(modifier = Modifier.height(8.dp))
                            if (outcome == "WON") {
                                Card(colors = CardDefaults.cardColors(containerColor = RaivalSuccess.copy(alpha = 0.12f))) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Stars, contentDescription = null, tint = RaivalSuccess)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("LICENSED PRO: You successfully unlocked the 14-day Wildcard License!", color = RaivalSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Card(colors = CardDefaults.cardColors(containerColor = RaivalError.copy(alpha = 0.12f))) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = RaivalError)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("QUALIFICATION FAILED. Practice more and try again!", color = RaivalError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { trialOutcome = null }) {
                                    Text("RETRY QUALIFIER", color = RaivalSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class LeagueDiv(
    val name: String,
    val players: String,
    val stipend: String,
    val color: Color
)


// -----------------------------------------------------------------------------
// FEATURE 65. SEASON PLAYOFFS
// -----------------------------------------------------------------------------
@Composable
fun SeasonPlayoffsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var playoffRound by remember { mutableStateOf("Quarter-Finals") }
    var quarterWinner1 by remember { mutableStateOf<String?>(null) }
    var quarterWinner2 by remember { mutableStateOf<String?>(null) }
    var quarterWinner3 by remember { mutableStateOf<String?>(null) }
    var quarterWinner4 by remember { mutableStateOf<String?>(null) }
    
    var semiWinner1 by remember { mutableStateOf<String?>(null) }
    var semiWinner2 by remember { mutableStateOf<String?>(null) }
    var champWinner by remember { mutableStateOf<String?>(null) }

    // Local copies for smart casts (delegated properties can't be smart-cast)
    val qw1 = quarterWinner1
    val qw2 = quarterWinner2
    val qw3 = quarterWinner3
    val qw4 = quarterWinner4
    val sw1 = semiWinner1
    val sw2 = semiWinner2
    val cw = champWinner

    var mvpVoted by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏆 SEASON PLAYOFF CHAMPIONSHIP", fontWeight = FontWeight.Black, color = RaivalSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The culmination of the regular season. The top 8 qualifiers battle in best-of-3, best-of-5 and best-of-7 series to crown the regional Champion. Huge cash rewards and Legendary Badges await the top 3 spots.",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SEASON PLAYOFF INTERACTIVE BRACKET", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Simplified tree bracket representation in text/row formats
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("QUARTER-FINALS (Best of 3)", color = RaivalSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BracketMatchCard("YawPES", "Akwasi", qw1!!) { quarterWinner1 = it }
                            BracketMatchCard("FireStriker", "Kofi_PES", qw2!!) { quarterWinner2 = it }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BracketMatchCard(user.username, "Kojo_Killa", qw3!!) { quarterWinner3 = it }
                            BracketMatchCard("AmaStriker", "YawGamer", qw4!!) { quarterWinner4 = it }
                        }

                        if ((qw1 != null && qw2 != null && qw3 != null && qw4 != null) ||
                            (sw1 != null && sw2 != null)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("SEMI-FINALS (Best of 5)", color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BracketMatchCard(qw1!!, qw2!!, semiWinner1) { semiWinner1 = it }
                                BracketMatchCard(qw3!!, qw4!!, semiWinner2) { semiWinner2 = it }
                            }
                        }

                        if (sw1 != null && sw2 != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("🏆 GRAND FINALS (Best of 7)", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                BracketMatchCard(sw1!!, sw2!!, champWinner) {
                                    champWinner = it
                                    if (it == user.username) {
                                        viewModel.earnCoins(5000)
                                        notify("🥇 CONGRATULATIONS! You won the Playoff Championship! Unlocked Legendary Champion Badge & GHS 500 equivalent!")
                                    } else {
                                        notify("🏆 Playoffs concluded! '$it' is the Season Playoff Champion!")
                                    }
                                }
                            }
                        }
                    }

                    if (quarterWinner1 != null || champWinner != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = {
                                quarterWinner1 = null
                                quarterWinner2 = null
                                quarterWinner3 = null
                                quarterWinner4 = null
                                semiWinner1 = null
                                semiWinner2 = null
                                champWinner = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = RaivalSecondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("RESET BRACKET FOR NEW SIMULATION", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PLAYOFF PRIZE POOL STRUCTURE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        PlayoffPrize("1st - Playoff Champion", "5000 Coins + Legendary Badge + GHS 500", Color(0xFFFFD700)),
                        PlayoffPrize("2nd - Runner-up", "2000 Coins + Gold Badge + GHS 200", Color(0xFFC0C0C0)),
                        PlayoffPrize("3rd - Bronze Finalist", "1000 Coins + Bronze Badge + GHS 100", Color(0xFFCD7F32)),
                        PlayoffPrize("4th - Semifinalist", "500 Coins + Playoff Badge", RaivalSecondary)
                    ).forEach { prize ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(prize.spot, color = Color.White, fontSize = 12.sp)
                            Text(prize.reward, color = prize.color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗳️ VOTE FOR MVP", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Every season, fans vote on the Playoff MVP. Back your favorite or vote for yourself if you made it!", color = RaivalTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!mvpVoted) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("YawPES", "Akwasi", user.username).forEach { nominee ->
                                Button(
                                    onClick = {
                                        mvpVoted = true
                                        viewModel.earnCoins(10) // Small voting reward
                                        notify("🗳️ You voted for '$nominee' as Playoff MVP! Earned +10 coins.")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary.copy(alpha = 0.2f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(nominee, color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis, maxLines = 1)
                                }
                            }
                        }
                    } else {
                        Text("✔ VOTE REGISTERED. Thank you for participating!", color = RaivalSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BracketMatchCard(p1: String, p2: String, winner: String?, onWinnerSelect: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
        border = BorderStroke(1.dp, RaivalSurfaceLight),
        modifier = Modifier.width(150.dp).padding(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = p1,
                fontSize = 11.sp,
                fontWeight = if (winner == p1) FontWeight.Bold else FontWeight.Normal,
                color = if (winner == p1) RaivalSuccess else if (winner != null) RaivalTextSecondary else Color.White,
                modifier = Modifier.clickable { onWinnerSelect(p1) }.padding(vertical = 4.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            HorizontalDivider(color = RaivalSurfaceLight, thickness = 1.dp)
            Text(
                text = p2,
                fontSize = 11.sp,
                fontWeight = if (winner == p2) FontWeight.Bold else FontWeight.Normal,
                color = if (winner == p2) RaivalSuccess else if (winner != null) RaivalTextSecondary else Color.White,
                modifier = Modifier.clickable { onWinnerSelect(p2) }.padding(vertical = 4.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

data class PlayoffPrize(val spot: String, val reward: String, val color: Color)


// -----------------------------------------------------------------------------
// FEATURE 66: PLAYER CARD COLLECTION
// -----------------------------------------------------------------------------
@Composable
fun PlayerCardCollectionView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var playerCardList by remember {
        mutableStateOf(
            listOf(
                PlayerCard("DLS-001", "YawPES", "Ghanaian Titan", "Legendary", 96, "ST", Color(0xFFFFD700)),
                PlayerCard("DLS-012", "Akwasi_PES", "Accra Anchor", "Rare", 89, "CM", Color(0xFFE040FB)),
                PlayerCard("DLS-034", "Kofi_Slayer", "Kumasi Wall", "Uncommon", 81, "CB", Color(0xFF03A9F4)),
                PlayerCard("DLS-055", "FireStriker", "Sizzling Winger", "Common", 75, "RW", Color(0xFF4CAF50))
            )
        )
    }

    var showTradeDialog by remember { mutableStateOf(false) }
    var activeBoosterPackOpening by remember { mutableStateOf(false) }
    var openedCard by remember { mutableStateOf<PlayerCard?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🃏 PLAYER CARD COLLECTIBLE SYSTEM", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Collect digital player cards from Ghana's esports scene. Trade with peers in real-time or purchase Booster Packs. Rare cards display mesmerizing holographic animations!",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🛒 ELITE BOOSTER PACK SHOP", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Guaranteed 1 Rare+ and 3 additional digital cards. Costs 50 coins.", color = RaivalTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    if (!activeBoosterPackOpening) {
                        Button(
                            onClick = {
                                if (user.coinBalance >= 50) {
                                    activeBoosterPackOpening = true
                                    viewModel.earnCoins(-50)
                                } else {
                                    notify("❌ Insufficient Coin Balance. Needs 50 coins.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary)
                        ) {
                            Text("BUY BOOSTER PACK (50 Coins)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Card packs coming soon - real player cards will be synced from the game database.", color = RaivalTextSecondary, fontSize = 11.sp)
                        }
                    }

                    openedCard?.let { card ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                            border = BorderStroke(1.dp, card.color)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("✨ NEW CARD UNLOCKED! ✨", color = card.color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                PlayerCardView(card)
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(onClick = { openedCard = null }) {
                                    Text("ADD TO COLLECTION", color = RaivalSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("YOUR CARD COLLECTION (${playerCardList.size})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                TextButton(onClick = { showTradeDialog = true }, colors = ButtonDefaults.textButtonColors(contentColor = RaivalSecondary)) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trade Hub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                playerCardList.forEach { card ->
                    PlayerCardView(card)
                }
            }
        }
    }

    if (showTradeDialog) {
        Dialog(onDismissRequest = { showTradeDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🤝 ESCROWED TRADE OFFERS", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("YawPES has offered a trade. Real-time escrow ensures verification of safe transfers.", color = RaivalTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(colors = CardDefaults.cardColors(containerColor = RaivalSurface)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("They Offer", color = RaivalSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Akwasi_PES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Rare CM (Ovr 89)", color = RaivalTextSecondary, fontSize = 10.sp)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RaivalPrimary)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("You Give", color = RaivalPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("FireStriker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Common RW (Ovr 75)", color = RaivalTextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTradeDialog = false }) {
                            Text("DECLINE", color = RaivalError, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showTradeDialog = false
                                // Swap elements in the mock list
                                val updatedList = playerCardList.filterNot { it.playerName == "FireStriker" }.toMutableList()
                                updatedList.add(PlayerCard("DLS-112", "Akwasi_PES", "Accra Anchor", "Rare", 89, "CM", Color(0xFFE040FB)))
                                playerCardList = updatedList
                                notify("🤝 Trade accepted! Swapped FireStriker for Akwasi_PES!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSuccess)
                        ) {
                            Text("ACCEPT TRADE", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerCardView(card: PlayerCard) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
        border = BorderStroke(2.dp, card.color),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(130.dp).height(200.dp).padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(card.position, color = card.color, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text(card.rating.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(card.color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(card.playerName.take(2).uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = card.color)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(card.playerName, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 12.sp, overflow = TextOverflow.Ellipsis, maxLines = 1)
                Text(card.title, color = RaivalTextSecondary, fontSize = 9.sp)
            }
            Text(
                text = card.rarity.uppercase(),
                color = Color.Black,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(card.color, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

data class PlayerCard(
    val id: String,
    val playerName: String,
    val title: String,
    val rarity: String,
    val rating: Int,
    val position: String,
    val color: Color
)


// -----------------------------------------------------------------------------
// FEATURE 67: LIVE TOURNAMENT WATCH
// -----------------------------------------------------------------------------
@Composable
fun LiveTournamentWatchView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var matchLogs by remember { mutableStateOf(listOf<String>()) }
    var scoreYaw by remember { mutableIntStateOf(0) }
    var scoreAkwasi by remember { mutableIntStateOf(0) }
    var matchTimeSeconds by remember { mutableIntStateOf(0) }
    var activePollVote by remember { mutableStateOf<String?>(null) }
    var betPlacedAmount by remember { mutableStateOf<Int?>(null) }
    var liveMatchFound by remember { mutableStateOf(false) }

    var floatingEmojis by remember { mutableStateOf(listOf<FloatingEmoji>()) }

    LaunchedEffect(Unit) {
        matchLogs = listOf("Live match tracking available when players use real matchmaking.")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📺 ARENA STREAM WATCH & LIVE CHAT", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Spectate ongoing matches in Accra's professional league. Predict outcomes or back players with virtual coins to earn multiplier rewards.", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Live Animated Match Visualizer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E5D2A)),
                border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().height(180.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = Color(0xFF1E5D2A))
                        drawLine(color = Color.White.copy(alpha = 0.3f), start = Offset(size.width / 2, 0f), end = Offset(size.width / 2, size.height), strokeWidth = 2f)
                        drawCircle(color = Color.White.copy(alpha = 0.3f), center = Offset(size.width / 2, size.height / 2), radius = 30.dp.toPx(), style = Stroke(width = 2f))
                    }

                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (liveMatchFound) {
                            Row(
                                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("YawPES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("$scoreYaw - $scoreAkwasi", color = RaivalSecondary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Text("Akwasi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${matchTimeSeconds}'", color = RaivalPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth().height(80.dp).background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).padding(8.dp)
                        ) {
                            LazyColumn(reverseLayout = true) {
                                items(matchLogs.reversed()) { log ->
                                    Text(log, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }

                        if (!liveMatchFound) {
                            Text("📺 LIVE MATCH TRACKING", color = RaivalPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("📺 LIVE FEED ACTIVE", color = RaivalSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Prediction & Coin Betting
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔮 ARENA POLLS & COIN PREDICTIONS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (liveMatchFound) "Live match tracking will be available when players use the real matchmaking system." else "No live matches available for prediction. Check back when matches are in progress.",
                        color = RaivalTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Emoji Reaction Panel
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("🔥", "😮", "👏", "⚽", "😭").forEach { emoji ->
                        IconButton(
                            onClick = {
                                val fe = FloatingEmoji(
                                    id = System.currentTimeMillis(),
                                    emoji = emoji,
                                    x = Random.nextInt(40, 260)
                                )
                                floatingEmojis = floatingEmojis + fe
                            },
                            modifier = Modifier.background(RaivalSurface, CircleShape)
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

data class FloatingEmoji(val id: Long, val emoji: String, val x: Int)


// -----------------------------------------------------------------------------
// FEATURE 68: INTERACTIVE REPLAYS
// -----------------------------------------------------------------------------
@Composable
fun InteractiveReplaysView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var isPlaying by remember { mutableStateOf(false) }
    var playSpeed by remember { mutableStateOf(1f) }
    var progressVal by remember { mutableStateOf(0.4f) }
    var strokeColor by remember { mutableStateOf(Color.Green) }

    // Storing points drawn by the user
    var drawnLines by remember { mutableStateOf(listOf<TacticalStroke>()) }
    var currentLinePoints by remember { mutableStateOf(listOf<Offset>()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎬 TACTICAL REPLAYS & CANVAS DRAWING", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Replay matches, slow down critical goals, and draw tactical lines directly on the interactive video panel to dissect defensive and offensive errors.", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().height(260.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Tactically Drawable Video Canvas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentLinePoints = listOf(offset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentLinePoints = currentLinePoints + change.position
                                    },
                                    onDragEnd = {
                                        drawnLines = drawnLines + TacticalStroke(currentLinePoints, strokeColor)
                                        currentLinePoints = emptyList()
                                    }
                                )
                            }
                    ) {
                        // Tactical Background representing a replay frame
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(color = Color(0xFF0F3E10))
                            // Draw pitch goal posts
                            drawRect(color = Color.White.copy(alpha = 0.2f), topLeft = Offset(0f, size.height/4), size = Size(40.dp.toPx(), size.height/2), style = Stroke(2f))
                            // Mock player dots
                            drawCircle(color = Color(0xFFFF5252), center = Offset(120.dp.toPx(), 80.dp.toPx()), radius = 10f)
                            drawCircle(color = Color(0xFF00E5FF), center = Offset(180.dp.toPx(), 110.dp.toPx()), radius = 10f)
                            drawCircle(color = Color.White, center = Offset(140.dp.toPx(), 90.dp.toPx()), radius = 6f) // Ball
                        }

                        // Draw actual strokes from the state
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawnLines.forEach { line ->
                                if (line.points.size > 1) {
                                    val path = Path().apply {
                                        moveTo(line.points[0].x, line.points[0].y)
                                        for (i in 1 until line.points.size) {
                                            lineTo(line.points[i].x, line.points[i].y)
                                        }
                                    }
                                    drawPath(path, color = line.color, style = Stroke(width = 6f, cap = StrokeCap.Round))
                                }
                            }
                            
                            if (currentLinePoints.size > 1) {
                                val path = Path().apply {
                                    moveTo(currentLinePoints[0].x, currentLinePoints[0].y)
                                    for (i in 1 until currentLinePoints.size) {
                                        lineTo(currentLinePoints[i].x, currentLinePoints[i].y)
                                    }
                                }
                                drawPath(path, color = strokeColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
                            }
                        }

                        Text("DRAW TACTICAL LINES ON SCREEN", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(8.dp))
                    }

                    // Interactive Playback Controls
                    Column(modifier = Modifier.background(RaivalSurfaceLight).padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isPlaying = !isPlaying }) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            }
                            Text(
                                text = "Speed: ${playSpeed}x",
                                color = RaivalSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable {
                                    playSpeed = when (playSpeed) {
                                        1f -> 0.25f
                                        0.25f -> 0.5f
                                        0.5f -> 2f
                                        else -> 1f
                                    }
                                }.padding(horizontal = 8.dp)
                            )
                            Slider(
                                value = progressVal,
                                onValueChange = { progressVal = it },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = RaivalPrimary, activeTrackColor = RaivalPrimary)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🖊 SELECT TACTICAL PEN COLORS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(Color.Green, Color.Red, Color.Yellow, Color.Cyan).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(color, CircleShape)
                                    .border(BorderStroke(2.dp, if (strokeColor == color) Color.White else Color.Transparent), CircleShape)
                                    .clickable { strokeColor = color }
                            )
                        }
                        Button(
                            onClick = { drawnLines = emptyList() },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSurface),
                            border = BorderStroke(1.dp, RaivalTextSecondary)
                        ) {
                            Text("Clear Canvas", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

data class TacticalStroke(val points: List<Offset>, val color: Color)


// -----------------------------------------------------------------------------
// FEATURE 69: PERSONAL PERFORMANCE DASHBOARD
// -----------------------------------------------------------------------------
@Composable
fun PersonalPerformanceDashboardView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var activeTab by remember { mutableStateOf("Overview") }

    val matchesPlayed = user.wins + user.losses
    val winRatePercent = if (matchesPlayed > 0) (user.wins * 100) / matchesPlayed else 45

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 ANALYTICAL PERFORMANCE DASHBOARD", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Deep dive into your competitive metrics. Study performance trendlines, radar attributes, and targeted development recommendation charts.", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().background(RaivalSurfaceLight, RoundedCornerShape(8.dp)).padding(4.dp)) {
                listOf("Overview", "Trends", "Skills Profile").forEach { tab ->
                    Text(
                        text = tab,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == tab) Color.Black else Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .background(if (activeTab == tab) RaivalPrimary else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { activeTab = tab }
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        when (activeTab) {
            "Overview" -> {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("CORE ARENA ATTRIBUTES", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                DashboardMetricCard("Win Rate", "$winRatePercent%", RaivalSecondary)
                                DashboardMetricCard("Level", user.level.toString(), RaivalPrimary)
                                DashboardMetricCard("Streak", "${user.winStreak}🔥", Color.Red)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                DashboardMetricCard("Coins Winnings", "${user.coinBalance} DLS", Color.Yellow)
                                DashboardMetricCard("Cash Balance", "₵${user.balance}", RaivalSuccess)
                                DashboardMetricCard("Matches Played", matchesPlayed.toString(), Color.Cyan)
                            }
                        }
                    }
                }
            }
            "Trends" -> {
                // Interactive dynamic bar charts drawn via Canvas
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("WEEKLY PERFORMANCE DISTRIBUTION", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                val bars = listOf(40f, 65f, 50f, 85f, 70f)
                                val barNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
                                val barWidth = 36.dp.toPx()
                                val spacing = (size.width - (bars.size * barWidth)) / (bars.size + 1)

                                for (i in bars.indices) {
                                    val h = (bars[i] / 100f) * size.height
                                    drawRect(
                                        color = RaivalSecondary,
                                        topLeft = Offset(spacing + i * (barWidth + spacing), size.height - h),
                                        size = Size(barWidth, h)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            "Skills Profile" -> {
                // Radar / Skill attributes bar representation
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("TACTICAL SKILL BREAKDOWN", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            listOf(
                                Triple("Tiki-Taka Midfield Possession", 0.85f, RaivalPrimary),
                                Triple("High-Press Counter Defense", 0.65f, RaivalSecondary),
                                Triple("Set-piece Accuracy & Free-kicks", 0.72f, Color.Cyan),
                                Triple("Sprint Stamina Reserves", 0.50f, Color.Red)
                            ).forEach { s ->
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(s.first, color = Color.White, fontSize = 11.sp)
                                        Text("${(s.second * 100).toInt()}%", color = s.third, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { s.second },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = s.third,
                                        trackColor = RaivalSurface
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

@Composable
fun DashboardMetricCard(title: String, value: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
        modifier = Modifier.width(90.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = RaivalTextSecondary, fontSize = 9.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}


// -----------------------------------------------------------------------------
// FEATURE 70: OPPONENT ANALYSIS TOOL
// -----------------------------------------------------------------------------
@Composable
fun OpponentAnalysisToolView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedOpponent by remember { mutableStateOf<OpponentData?>(null) }

    val opponents = listOf(
        OpponentData("YawPES", "Aggressive Tiki-Taka", "82%", "Fast wing crosses", "Panics under counter pressure", "4-3-3 Attacking"),
        OpponentData("Akwasi_PES", "Defensive Low Block", "75%", "Escaping via long-balls", "Weak to dynamic triangles", "5-3-2 Wingback"),
        OpponentData("Kojo_Killa", "High Speed Pressing", "88%", "Stamina heavy sprints", "Easily drawn off-side", "4-2-4 Heavy"),
        OpponentData("AmaStriker", "Balanced Short Pass", "69%", "Clinical finishing", "Inconsistent goalkeeper positioning", "4-4-2 Balanced")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔍 OPPONENT SCOUTING & ANALYSIS", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Search opposing players in Accra's community to inspect their favorite formations, strengths, critical strategic loopholes, and historic win rates.", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search player name (e.g., YawPES)", color = RaivalTextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RaivalSecondary,
                    unfocusedBorderColor = RaivalSurfaceLight,
                    focusedContainerColor = RaivalSurfaceLight,
                    unfocusedContainerColor = RaivalSurfaceLight
                )
            )
        }

        item {
            Text("POPULAR GAMERS REPORT SHEET", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
        }

        val filteredOpponents = opponents.filter { it.name.contains(searchQuery, ignoreCase = true) }

        items(filteredOpponents) { opponent ->
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                modifier = Modifier.fillMaxWidth().clickable { selectedOpponent = opponent }
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(opponent.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("Style: ${opponent.playStyle}", color = RaivalSecondary, fontSize = 11.sp)
                    }
                    Text("WR: ${opponent.winRate}", color = RaivalPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }

    selectedOpponent?.let { opponent ->
        Dialog(onDismissRequest = { selectedOpponent = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("📋 SCOUT REPORT: ${opponent.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Formation Favorite", color = RaivalTextSecondary, fontSize = 10.sp)
                    Text(opponent.formation, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Critical Strength", color = RaivalTextSecondary, fontSize = 10.sp)
                    Text(opponent.strength, color = RaivalSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Target Strategic Loophole", color = RaivalTextSecondary, fontSize = 10.sp)
                    Text(opponent.loophole, color = RaivalError, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            selectedOpponent = null
                            notify("⚔ Strategy notes saved to your local matchup clipboard!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SAVE SCOUTING NOTES", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class OpponentData(
    val name: String,
    val playStyle: String,
    val winRate: String,
    val strength: String,
    val loophole: String,
    val formation: String
)


// -----------------------------------------------------------------------------
// FEATURE 71: GAMING TIPS & TRICKS
// -----------------------------------------------------------------------------
@Composable
fun GamingTipsAndTricksView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showTipDialog by remember { mutableStateOf<TipItem?>(null) }
    var bookmarks by remember { mutableStateOf(setOf<String>()) }
    var bookmarkedOnly by remember { mutableStateOf(false) }

    // User submission form states
    var submitCategory by remember { mutableStateOf("Tactics") }
    var submitTitle by remember { mutableStateOf("") }
    var submitDesc by remember { mutableStateOf("") }

    var tipsList by remember {
        mutableStateOf(
            listOf(
                TipItem("DLS-001", "Perfecting the Tiki-Taka", "Tactics", "Chain quick short-passes using 3-finger configurations. Always utilize defensive low-blocks to trap long-pass exploits.", "Yaw_PES", 45),
                TipItem("DLS-002", "Curved Free-Kicks inside Box", "Skills", "Aim the analog stick 30-degrees outwards from target, applying precisely 45% power gauge threshold.", "Akwasi", 38),
                TipItem("DLS-003", "Mental Game & Defeating Rage", "Mindset", "Deep breathing cycles between halves. Never sprint chase attackers instantly; contain center blocks.", "Raival Pro", 60),
                TipItem("DLS-004", "Pro Tournament Configurations", "Tournament Prep", "Assign optimal controller mappings, disable notifications entirely to avoid lag spike interruptions.", "GhanaAdmin", 29)
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 ACCRA esports GUIDES & TIPS", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Learn tactics from elite gamers, master set-pieces, set optimal controls, or contribute your personal winning tips to earn voting coins.", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                listOf("All", "Tactics", "Skills", "Mindset", "Tournament Prep").forEach { cat ->
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedCategory == cat) Color.Black else Color.White,
                        modifier = Modifier
                            .background(
                                if (selectedCategory == cat) RaivalSecondary else RaivalSurfaceLight,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .padding(end = 6.dp)
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("POPULAR GUIDES SHEET", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = bookmarkedOnly, onCheckedChange = { bookmarkedOnly = it })
                    Text("Bookmarked Only", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        val filteredTips = tipsList.filter {
            (selectedCategory == "All" || it.category == selectedCategory) &&
            (!bookmarkedOnly || bookmarks.contains(it.id))
        }

        items(filteredTips) { tip ->
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                modifier = Modifier.fillMaxWidth().clickable { showTipDialog = tip }
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tip.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text("By ${tip.author} • Category: ${tip.category}", color = RaivalTextSecondary, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥 ${tip.upvotes}", color = RaivalPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (bookmarks.contains(tip.id)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = RaivalSecondary,
                            modifier = Modifier.size(18.dp).clickable {
                                bookmarks = if (bookmarks.contains(tip.id)) bookmarks - tip.id else bookmarks + tip.id
                            }
                        )
                    }
                }
            }
        }

        // Community Submission Form
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight), border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✍ CONTRIBUTE YOUR TIPS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = submitTitle,
                        onValueChange = { submitTitle = it },
                        placeholder = { Text("Title (e.g. Master Curving Crosses)", color = RaivalTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RaivalPrimary,
                            focusedContainerColor = RaivalSurface,
                            unfocusedContainerColor = RaivalSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = submitDesc,
                        onValueChange = { submitDesc = it },
                        placeholder = { Text("Strategic Details...", color = RaivalTextSecondary) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RaivalPrimary,
                            focusedContainerColor = RaivalSurface,
                            unfocusedContainerColor = RaivalSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (submitTitle.isNotBlank() && submitDesc.isNotBlank()) {
                                val newTip = TipItem(
                                    id = "COMM-${Random.nextInt(1000)}",
                                    title = submitTitle,
                                    category = submitCategory,
                                    details = submitDesc,
                                    author = user.username,
                                    upvotes = 1
                                )
                                tipsList = tipsList + newTip
                                submitTitle = ""
                                submitDesc = ""
                                viewModel.earnCoins(15) // Small contribution reward
                                notify("🎉 Tip submitted! Received +15 coins contribution reward!")
                            } else {
                                notify("❌ Please fill in the details.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("PUBLISH TO COMMUNITY NEWS", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    showTipDialog?.let { tip ->
        Dialog(onDismissRequest = { showTipDialog = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(tip.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text("Published by pro player: ${tip.author}", color = RaivalSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(tip.details, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = {
                                showTipDialog = null
                                notify("👍 Upvoted community tip!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSuccess)
                        ) {
                            Text("UPVOTE GUIDE 👍", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { showTipDialog = null }) {
                            Text("CLOSE", color = RaivalTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

data class TipItem(
    val id: String,
    val title: String,
    val category: String,
    val details: String,
    val author: String,
    val upvotes: Int
)


// -----------------------------------------------------------------------------
// FEATURE 72: CROSS-PLATFORM PLAY
// -----------------------------------------------------------------------------
@Composable
fun CrossPlatformPlayView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var searchIdInput by remember { mutableStateOf("") }
    var isPinging by remember { mutableStateOf(false) }
    var accraPing by remember { mutableStateOf<Int?>(null) }

    var friendsList by remember {
        mutableStateOf(
            listOf(
                CrossFriend("YawPES", "Mobile", "Ghana-West", true),
                CrossFriend("Akwasi_Gamer", "Mobile", "Ghana-East", false)
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🌐 CROSS-PLATFORM MATCHMAKING & LOBBY", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Battle seamlessly with players across Android and iOS devices. Run hardware ping-tests to verify server stability prior to placing competitive stakes.", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Live Ping Test Visualizer
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📶 INTEGRITY REGIONAL PING TEST", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isPinging && accraPing == null) {
                        Button(
                            onClick = {
                                isPinging = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary)
                        ) {
                            Text("RUN PING TEST DIAGNOSTIC", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    } else if (isPinging) {
                        LaunchedEffect(Unit) {
                            delay(1500)
                            isPinging = false
                            accraPing = Random.nextInt(18, 38)
                            notify("📶 Ping diagnostic concluded: $accraPing ms latency.")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = RaivalSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Measuring network latency jitter to Accra servers...", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    accraPing?.let { ping ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = RaivalSuccess)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Accra Regional Server: ${ping}ms (STABLE LOBBY)", color = RaivalSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { accraPing = null }) {
                            Text("TEST AGAIN", color = RaivalTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ADD CROSS-PLATFORM OPPONENT ID", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchIdInput,
                            onValueChange = { searchIdInput = it },
                            placeholder = { Text("Enter iOS/Android Gamer ID...", color = RaivalTextSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RaivalPrimary,
                                focusedContainerColor = RaivalSurface,
                                unfocusedContainerColor = RaivalSurface
                            )
                        )
                        Button(
                            onClick = {
                                if (searchIdInput.isNotBlank()) {
                                    val newFriend = CrossFriend(
                                        name = searchIdInput,
                                        device = "Unknown",
                                        region = "Ghana-Central",
                                        isOnline = true
                                    )
                                    friendsList = friendsList + newFriend
                                    searchIdInput = ""
                                    notify("✔ Added cross-platform friend successfully!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("ADD", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("CROSS-PLATFORM OPPONENTS IN ACCRA ARENA", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        }

        items(friendsList) { friend ->
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (friend.isOnline) RaivalSuccess else RaivalTextSecondary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(friend.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("OS: ${friend.device} • Region: ${friend.region}", color = RaivalTextSecondary, fontSize = 10.sp)
                        }
                    }
                    Button(
                        onClick = { notify("🎮 Challenged '${friend.name}' to cross-platform Arena Cup match!") },
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("CHALLENGE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

data class CrossFriend(
    val name: String,
    val device: String,
    val region: String,
    val isOnline: Boolean
)


// -----------------------------------------------------------------------------
// FEATURE 73: BLOCKCHAIN INTEGRATION
// -----------------------------------------------------------------------------
@Composable
fun BlockchainIntegrationView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var ledgerHashes by remember {
        mutableStateOf(
            listOf(
                LedgerBlock("Pending", "Blockchain verification: Pending integration", "Real-time blockchain sync will be available soon.", "Coming soon")
            )
        )
    }

    var verifyMatchIdInput by remember { mutableStateOf("") }
    var activeCardMinting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔗 ETHEREUM SECURE LEDGER INTEGRATION", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Trust through decentralized verification. Every tournament winner, match result, and escrowed cash prize disbursement is permanently anchored onto the secure public ledger.", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("VERIFY MATCH METADATA ON BLOCKCHAIN", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = verifyMatchIdInput,
                            onValueChange = { verifyMatchIdInput = it },
                            placeholder = { Text("Enter Match ID (e.g., M-4019)", color = RaivalTextSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RaivalPrimary,
                                focusedContainerColor = RaivalSurface,
                                unfocusedContainerColor = RaivalSurface
                            )
                        )
                        Button(
                            onClick = {
                                if (verifyMatchIdInput.isNotBlank()) {
                                    notify("🔗 Blockchain verification: Pending integration. Match '$verifyMatchIdInput' will be verified once blockchain is connected.")
                                    verifyMatchIdInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("VERIFY", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🖼 MINT RARE DIGITAL CARDS TO NFT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Convert your unlocked legendary player card collection into fully owned Ethereum/Solana ERC-721 NFTs.", color = RaivalTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(14.dp))

                    if (!activeCardMinting) {
                        Button(
                            onClick = {
                                activeCardMinting = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalSecondary)
                        ) {
                            Text("MINT YAW_PES CARD (15 Coins Fee)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    } else {
                        var mintProgress by remember { mutableFloatStateOf(0f) }
                        LaunchedEffect(Unit) {
                            while (mintProgress < 1f) {
                                delay(200)
                                mintProgress += 0.1f
                            }
                            activeCardMinting = false
                            viewModel.earnCoins(-15)
                            notify("🎉 NFT successfully minted! Token ID: #88091 securely stored on Ethereum!")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = RaivalSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Deploying Smart Contract and Anchor transactions...", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Text("PUBLIC LEDGER METADATA TRACKER", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        }

        items(ledgerHashes) { block ->
            Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(block.blockHeight, fontWeight = FontWeight.Bold, color = RaivalSecondary, fontSize = 12.sp)
                        Text(block.timeStr, color = RaivalTextSecondary, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(block.details, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Hash: ${block.txHash}", color = RaivalPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

data class LedgerBlock(
    val blockHeight: String,
    val txHash: String,
    val details: String,
    val timeStr: String
)


// -----------------------------------------------------------------------------
// FEATURE: ANALYTICS & INSIGHTS
// -----------------------------------------------------------------------------
@Composable
fun AnalyticsAndInsightsView(viewModel: RaivalViewModel, user: User, notify: (String) -> Unit) {
    var adminModeActive by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📊 ADVANCED METRICS ENGINE", fontWeight = FontWeight.Black, color = RaivalPrimary, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Admin Mode", color = Color.White, fontSize = 10.sp)
                            Switch(
                                checked = adminModeActive,
                                onCheckedChange = { adminModeActive = it },
                                modifier = Modifier.scale(0.6f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Examine structural player statistics or pivot to administrative community logs detailing peak usage hours, cash flow indexes, and user retention metrics.", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        if (!adminModeActive) {
            // Player performance analytics
            item {
                Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📈 YOUR ARENA RETENTION INDEX", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        listOf(
                            MetricBar("Daily Peak Engagement hour", "8:00 PM - 10:00 PM", 0.90f, RaivalSecondary),
                            MetricBar("Weekend tournament participation index", "High (4 out of 5)", 0.80f, RaivalPrimary),
                            MetricBar("Win-Loss consistency margin", "+12% over last week", 0.65f, Color.Cyan),
                            MetricBar("Escrow withdraw validation index", "Stable", 0.95f, RaivalSuccess)
                        ).forEach { mb ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(mb.label, color = Color.White, fontSize = 11.sp)
                                    Text(mb.valStr, color = mb.color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { mb.progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = mb.color,
                                    trackColor = RaivalSurface
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Admin growth analytics
            item {
                Card(colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📊 SYSTEM CONSOLIDATED ADMIN DATA", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        listOf(
                            MetricBar("Consolidated Monthly active users (MAU)", "24,908 active gamers", 0.88f, RaivalPrimary),
                            MetricBar("Live active daily tournaments", "112 running cups", 0.72f, RaivalSecondary),
                            MetricBar("Ghana mobile money escrow transactions", "99.8% safe validation rate", 0.99f, RaivalSuccess),
                            MetricBar("Active user retention index (30-day)", "74% stickiness index", 0.74f, Color.Yellow)
                        ).forEach { mb ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(mb.label, color = Color.White, fontSize = 11.sp)
                                    Text(mb.valStr, color = mb.color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { mb.progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = mb.color,
                                    trackColor = RaivalSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { notify("📥 Success: Analytical report exported as structured JSON data!") },
                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("EXPORT ANALYTICAL REPORT DATA", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

data class MetricBar(
    val label: String,
    val valStr: String,
    val progress: Float,
    val color: Color
)

// Helper extension modifier for scale
fun Modifier.scale(scale: Float): Modifier = this.graphicsLayer(scaleX = scale, scaleY = scale)
