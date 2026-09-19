package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaivalViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: RaivalViewModel,
    currentUser: User,
    onTabSwitch: (Int) -> Unit,
    onBack: () -> Unit
) {
    val posts by viewModel.communityPosts.collectAsState()
    val tournaments by viewModel.tournaments.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedCategoryTab by remember { mutableStateOf("All") }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var selectedPostForComments by remember { mutableStateOf<CommunityPost?>(null) }

    val filteredPosts = remember(posts, selectedCategoryTab) {
        if (selectedCategoryTab == "All") {
            posts
        } else {
            posts.filter { it.category.equals(selectedCategoryTab, ignoreCase = true) }
        }
    }

    val categories = listOf("All", "General", "Tournaments", "Stats", "Teams")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = RaivalBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreatePostDialog = true },
                containerColor = RaivalPrimary,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .testTag("create_post_fab")
                    .padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                RaivalSurfaceLight.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RaivalPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "COMMUNITY HUB",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Discuss matches, showcase stats & recruit allies",
                            style = MaterialTheme.typography.bodySmall,
                            color = RaivalTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(RaivalPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, RaivalPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚡ ACTIVE FEED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = RaivalPrimary
                        )
                    }
                }
            }

            // Category Tab Row
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategoryTab).coerceAtLeast(0),
                containerColor = Color.Transparent,
                contentColor = RaivalPrimary,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[categories.indexOf(selectedCategoryTab).coerceAtLeast(0)]),
                            color = RaivalPrimary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategoryTab == category
                    Tab(
                        selected = isSelected,
                        onClick = { selectedCategoryTab = category },
                        text = {
                            Text(
                                text = when (category) {
                                    "All" -> "🌎 All Feed"
                                    "General" -> "💬 Discussion"
                                    "Tournaments" -> "🏆 Tournaments"
                                    "Stats" -> "📊 Stats Showcase"
                                    "Teams" -> "👥 Find Teams"
                                    else -> category
                                },
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else RaivalTextSecondary
                            )
                        },
                        modifier = Modifier.testTag("tab_$category")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Feed List
            if (filteredPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(RaivalSurfaceLight.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = RaivalPrimary.copy(alpha = 0.6f),
                                modifier = Modifier.size(50.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "NO FEEDS PUBLISHED YET",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Discuss game strategies, recruit squad mates, or boast about your historic win steaks!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RaivalTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showCreatePostDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_state_create_post_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create First Post", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("community_feed_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredPosts, key = { it.id }) { post ->
                        CommunityPostCard(
                            post = post,
                            currentUser = currentUser,
                            onLikeClick = { viewModel.toggleLikePost(post) },
                            onCommentClick = { selectedPostForComments = post },
                            onDeleteClick = { viewModel.deletePost(post.id) },
                            onRegisterTournament = { tId ->
                                onTabSwitch(1) // Tournaments tab
                            },
                            onJoinTeam = { teamId ->
                                // Join Team Recruitment logically
                                scope.launch {
                                    val currentTeam = teams.find { it.id == teamId }
                                    if (currentTeam != null && !currentTeam.members.split(",").contains(currentUser.username)) {
                                        val updatedMembers = "${currentTeam.members},${currentUser.username}"
                                        viewModel.repository.updateTeam(
                                            currentTeam.copy(
                                                members = updatedMembers,
                                                memberCount = currentTeam.memberCount + 1
                                            )
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Create Post Dialog
    if (showCreatePostDialog) {
        CreatePostDialog(
            currentUser = currentUser,
            tournaments = tournaments,
            teams = teams,
            onDismiss = { showCreatePostDialog = false },
            onPostCreated = { title, content, category, tId, tTitle, statGame, statValue, statLabel, teamId, teamName ->
                viewModel.createPost(
                    title = title,
                    content = content,
                    category = category,
                    tournamentId = tId,
                    tournamentTitle = tTitle,
                    statGame = statGame,
                    statValue = statValue,
                    statLabel = statLabel,
                    teamId = teamId,
                    teamName = teamName
                )
                showCreatePostDialog = false
            }
        )
    }

    // Comments Bottom Sheet / Dialog
    if (selectedPostForComments != null) {
        CommentsDialog(
            post = selectedPostForComments ?: CommunityPost(userId = 0, username = "", title = "", content = "", category = ""),
            viewModel = viewModel,
            onDismiss = { selectedPostForComments = null }
        )
    }
}

@Composable
fun CommunityPostCard(
    post: CommunityPost,
    currentUser: User,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRegisterTournament: (Int) -> Unit,
    onJoinTeam: (Int) -> Unit
) {
    val isLiked = remember(post.likedByUserIds, currentUser.id) {
        post.likedByUserIds.split(",").contains(currentUser.id.toString())
    }

    val categoryColor = when (post.category.lowercase()) {
        "tournaments" -> RaivalPrimary
        "stats" -> RaivalBrightLime
        "teams" -> RaivalAccent
        else -> RaivalElectricPurple
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: User Profile, Timestamp, Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(RaivalSurfaceLight, CircleShape)
                            .border(1.5.dp, categoryColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.username.take(2).uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Column {
                        Text(
                            text = "@${post.username}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = formatTimestamp(post.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = RaivalTextSecondary
                        )
                    }
                }

                // Category Badge
                Box(
                    modifier = Modifier
                        .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, categoryColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = post.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = categoryColor,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title & Content
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = RaivalTextPrimary
            )

            // Dynamic Attachments
            if (post.tournamentId != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinkedTournamentCard(
                    tournamentId = post.tournamentId,
                    title = post.tournamentTitle ?: "Esports Tournament",
                    onRegisterClick = { onRegisterTournament(post.tournamentId) }
                )
            } else if (post.statGame != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinkedStatsCard(
                    game = post.statGame,
                    value = post.statValue ?: "0%",
                    label = post.statLabel ?: "Win Rate"
                )
            } else if (post.teamId != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinkedTeamCard(
                    teamId = post.teamId,
                    teamName = post.teamName ?: "United Squad",
                    onJoinClick = { onJoinTeam(post.teamId) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(color = RaivalSurfaceLight, thickness = 1.dp)

            Spacer(modifier = Modifier.height(8.dp))

            // Action row: Like, Comment, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Like Action
                    val iconScale = remember { Animatable(1f) }
                    val scope = rememberCoroutineScope()
                    IconButton(
                        onClick = {
                            scope.launch {
                                iconScale.animateTo(1.4f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                                iconScale.animateTo(1f)
                            }
                            onLikeClick()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) RaivalPrimary else RaivalTextSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(iconScale.value)
                            )
                            Text(
                                text = post.likesCount.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isLiked) RaivalPrimary else RaivalTextSecondary
                            )
                        }
                    }

                    // Comment Action
                    IconButton(
                        onClick = onCommentClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                contentDescription = "Comment",
                                tint = RaivalTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = post.commentsCount.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = RaivalTextSecondary
                            )
                        }
                    }
                }

                // Delete Action (Only if creator or admin)
                if (currentUser.username == post.username || currentUser.role == "admin") {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = RaivalError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LinkedTournamentCard(
    tournamentId: Int,
    title: String,
    onRegisterClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(RaivalPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = RaivalPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Linked Tournament • Active Registration",
                        style = MaterialTheme.typography.labelSmall,
                        color = RaivalTextSecondary
                    )
                }
            }

            Button(
                onClick = onRegisterClick,
                colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Join", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LinkedStatsCard(
    game: String,
    value: String,
    label: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, RaivalBrightLime.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(RaivalBrightLime.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = RaivalBrightLime,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = game,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Verified Player Stats Card",
                        style = MaterialTheme.typography.labelSmall,
                        color = RaivalTextSecondary
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = RaivalBrightLime
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = RaivalTextSecondary
                )
            }
        }
    }
}

@Composable
fun LinkedTeamCard(
    teamId: Int,
    teamName: String,
    onJoinClick: () -> Unit
) {
    var joined by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, RaivalAccent.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(RaivalAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = RaivalAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = teamName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Recruiting Esports Team",
                        style = MaterialTheme.typography.labelSmall,
                        color = RaivalTextSecondary
                    )
                }
            }

            Button(
                onClick = {
                    joined = true
                    onJoinClick()
                },
                enabled = !joined,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RaivalAccent,
                    disabledContainerColor = RaivalSurface
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = if (joined) "Requested" else "Apply",
                    color = if (joined) RaivalTextSecondary else Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostDialog(
    currentUser: User,
    tournaments: List<Tournament>,
    teams: List<Team>,
    onDismiss: () -> Unit,
    onPostCreated: (String, String, String, Int?, String?, String?, String?, String?, Int?, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }

    // Attachment fields
    var selectedTournamentId by remember { mutableStateOf<Int?>(null) }
    var selectedTournamentTitle by remember { mutableStateOf<String?>(null) }

    var selectedStatGame by remember { mutableStateOf<String?>(null) }
    var selectedStatValue by remember { mutableStateOf("") }
    var selectedStatLabel by remember { mutableStateOf("Win Rate") }

    var selectedTeamId by remember { mutableStateOf<Int?>(null) }
    var selectedTeamName by remember { mutableStateOf<String?>(null) }

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showTournamentDropdown by remember { mutableStateOf(false) }
    var showTeamDropdown by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = RaivalSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📢 PUBLISH POST",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Post Title", color = RaivalTextSecondary) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RaivalPrimary,
                        unfocusedBorderColor = RaivalSurfaceLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_post_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Content Input
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Share your thoughts...", color = RaivalTextSecondary) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RaivalPrimary,
                        unfocusedBorderColor = RaivalSurfaceLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("create_post_content_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selector
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = RaivalTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { showCategoryDropdown = true },
                        colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                        border = BorderStroke(1.dp, RaivalSurfaceLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category, color = Color.White, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                        }
                    }

                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false },
                        modifier = Modifier.background(RaivalSurface)
                    ) {
                        listOf("General", "Tournaments", "Stats", "Teams").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = Color.White) },
                                onClick = {
                                    category = cat
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Contextual attachments
                when (category) {
                    "Tournaments" -> {
                        Text(
                            text = "Link Active Tournament",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = RaivalTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { showTournamentDropdown = true },
                                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTournamentTitle ?: "Choose an active tournament...",
                                        color = if (selectedTournamentTitle == null) RaivalTextSecondary else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Default.Link, contentDescription = null, tint = RaivalPrimary)
                                }
                            }

                            DropdownMenu(
                                expanded = showTournamentDropdown,
                                onDismissRequest = { showTournamentDropdown = false },
                                modifier = Modifier.background(RaivalSurface)
                            ) {
                                if (tournaments.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No active tournaments", color = RaivalTextSecondary) },
                                        onClick = { showTournamentDropdown = false }
                                    )
                                } else {
                                    tournaments.forEach { t ->
                                        DropdownMenuItem(
                                            text = { Text(t.title, color = Color.White) },
                                            onClick = {
                                                selectedTournamentId = t.id
                                                selectedTournamentTitle = t.title
                                                showTournamentDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "Stats" -> {
                        Text(
                            text = "Share Player Stats",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = RaivalTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    selectedStatGame = "Dream League Soccer"
                                    selectedStatValue = "${currentUser.winStreak} Streak"
                                    selectedStatLabel = "Win Streak"
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedStatGame == "Dream League Soccer") RaivalPrimary else RaivalSurfaceLight
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "DLS Stats",
                                    color = if (selectedStatGame == "Dream League Soccer") Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    selectedStatGame = "eFootball"
                                    selectedStatValue = "Level ${currentUser.level}"
                                    selectedStatLabel = "Player Level"
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedStatGame == "eFootball") RaivalPrimary else RaivalSurfaceLight
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "eFootball",
                                    color = if (selectedStatGame == "eFootball") Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (selectedStatGame != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Status Attachment:", color = RaivalTextSecondary, fontSize = 12.sp)
                                    Text(
                                        "$selectedStatGame (${selectedStatValue})",
                                        color = RaivalBrightLime,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    "Teams" -> {
                        Text(
                            text = "Link Recruiting Team",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = RaivalTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { showTeamDropdown = true },
                                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                                border = BorderStroke(1.dp, RaivalAccent.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTeamName ?: "Choose a recruiting team...",
                                        color = if (selectedTeamName == null) RaivalTextSecondary else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = RaivalAccent)
                                }
                            }

                            DropdownMenu(
                                expanded = showTeamDropdown,
                                onDismissRequest = { showTeamDropdown = false },
                                modifier = Modifier.background(RaivalSurface)
                            ) {
                                if (teams.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No active teams", color = RaivalTextSecondary) },
                                        onClick = { showTeamDropdown = false }
                                    )
                                } else {
                                    teams.forEach { team ->
                                        DropdownMenuItem(
                                            text = { Text(team.name, color = Color.White) },
                                            onClick = {
                                                selectedTeamId = team.id
                                                selectedTeamName = team.name
                                                showTeamDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(24.dp))

                // Publish Button
                Button(
                    onClick = {
                        if (title.isNotEmpty() && content.isNotEmpty()) {
                            onPostCreated(
                                title, content, category,
                                selectedTournamentId, selectedTournamentTitle,
                                selectedStatGame, selectedStatValue, selectedStatLabel,
                                selectedTeamId, selectedTeamName
                            )
                        }
                    },
                    enabled = title.isNotEmpty() && content.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RaivalPrimary,
                        disabledContainerColor = RaivalSurfaceLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("publish_post_submit_btn")
                ) {
                    Text(
                        "Publish Post",
                        color = if (title.isNotEmpty() && content.isNotEmpty()) Color.Black else RaivalTextSecondary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsDialog(
    post: CommunityPost,
    viewModel: RaivalViewModel,
    onDismiss: () -> Unit
) {
    val commentsFlow = remember(post.id) { viewModel.getCommentsForPost(post.id) }
    val comments by commentsFlow.collectAsState(initial = emptyList())
    var newCommentText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = RaivalSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💬 COMMENTS (${comments.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Post Preview Header
                Card(
                    colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            post.title,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            post.content,
                            color = RaivalTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Comments List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("comments_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (comments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = RaivalTextSecondary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No comments yet. Start the discussion!",
                                        color = RaivalTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(comments, key = { it.id }) { comment ->
                            CommentRow(comment = comment)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add Comment Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RaivalSurfaceLight, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Add a comment...", color = RaivalTextSecondary, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comment_input"),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (newCommentText.isNotEmpty()) {
                                viewModel.addComment(post.id, newCommentText) {
                                    newCommentText = ""
                                }
                            }
                        },
                        enabled = newCommentText.isNotEmpty(),
                        modifier = Modifier.testTag("submit_comment_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (newCommentText.isNotEmpty()) RaivalPrimary else RaivalTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentRow(comment: CommunityComment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(RaivalSurfaceLight, CircleShape)
                .border(1.dp, RaivalPrimary.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.username.take(2).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .background(RaivalSurfaceLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "@${comment.username}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = formatTimestamp(comment.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = RaivalTextSecondary,
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodySmall,
                color = RaivalTextPrimary
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        else -> "$days days ago"
    }
}
