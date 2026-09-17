package com.example.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.ui.viewmodel.RaivalViewModel
import kotlinx.coroutines.flow.emptyFlow
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentCalendarScreen(
    viewModel: RaivalViewModel = viewModel(),
    onBack: () -> Unit
) {
    val tournaments by viewModel.tournaments.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val matchesState by viewModel.getAllTournamentMatches().collectAsState(initial = emptyList())

    val today = java.time.LocalDate.now()
    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }
    var selectedTournamentId by remember { mutableStateOf<Int?>(null) }
    var showTournamentDetail by remember { mutableStateOf<Tournament?>(null) }
    var showMatchDetail by remember { mutableStateOf<TournamentMatch?>(null) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) } // MONTH, WEEK, LIST, UPCOMING
    var selectedFilter by remember { mutableStateOf("All") } // All, Today, This Week, Upcoming

    // Get unique games for filter
    val availableGames = remember(tournaments) {
        listOf("All") + tournaments.map { it.game }.distinct()
    }

    // Filter tournaments based on selected filter
    val filteredTournaments = remember(tournaments, selectedFilter, selectedTournamentId, selectedDate) {
        tournaments.filter { tournament ->
            val isGameMatch = selectedFilter == "All" || tournament.game == selectedFilter
            val isDateMatch = selectedDate == null || tournament.date == selectedDate.toString()
            val isTournamentMatch = selectedTournamentId == null || tournament.id == selectedTournamentId
            isGameMatch && isDateMatch && isTournamentMatch
        }
    }

    // Group tournaments by date
    val tournamentsByDate = remember(filteredTournaments) {
        filteredTournaments.groupBy { it.date }.toSortedMap()
    }

    // Upcoming tournaments (next 30 days)
    val upcomingTournaments = remember(tournaments) {
        tournaments
            .filter { it.status == "Open" }
            .sortedBy { it.date }
            .take(20)
    }

    // User's registered tournaments
    val currentUserId = currentUser?.id
    val userRegistrations by remember(currentUserId) {
        if (currentUserId != null) {
            viewModel.getRegistrationsForUser(currentUserId)
        } else {
            emptyFlow<List<com.example.data.model.Registration>>()
        }
    }.collectAsState(initial = emptyList())

    val userTournamentIds = userRegistrations.map { it.tournamentId }.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tournament Calendar",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // View Mode Selector
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CalendarViewMode.entries.forEach { mode ->
                            val isSelected = viewMode == mode
                            Button(
                                onClick = { viewMode = mode },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) RaivalPrimary else RaivalSurfaceLight,
                                    contentColor = if (isSelected) Color.Black else Color.White
                                ),
                                modifier = Modifier.height(32.dp).padding(horizontal = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(mode.icon, contentDescription = null, tint = if (isSelected) Color.Black else RaivalTextSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(mode.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaivalSurface)
            )
},
            bottomBar = {
                // Mini calendar navigator
                if (viewMode == CalendarViewMode.MONTH || viewMode == CalendarViewMode.WEEK) {
                    MonthNavigator(
                        selectedDate = selectedDate,
                        onDateClick = { date ->
                            selectedDate = date
                            viewMode = CalendarViewMode.LIST
                        },
                        tournamentsByDate = tournamentsByDate,
                        today = today
                    )
                }
            }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (viewMode) {
                CalendarViewMode.MONTH -> MonthCalendarView(
                    selectedDate = selectedDate,
                    onDateClick = { date ->
                        selectedDate = date
                        viewMode = CalendarViewMode.LIST
                    },
                    tournamentsByDate = tournamentsByDate,
                    userTournamentIds = userTournamentIds,
                    onTournamentClick = { tournament ->
                        showTournamentDetail = tournament
                    },
                    today = today
                )
                CalendarViewMode.WEEK -> WeekCalendarView(
                    selectedDate = selectedDate,
                    tournaments = filteredTournaments,
                    userTournamentIds = userTournamentIds,
                    onTournamentClick = { tournament ->
                        showTournamentDetail = tournament
                    },
                    today = today
                )
                CalendarViewMode.LIST -> TournamentListView(
                    tournaments = filteredTournaments,
                    userTournamentIds = userTournamentIds,
                    onTournamentClick = { tournament ->
                        showTournamentDetail = tournament
                    },
                    onRegisterClick = { tournament ->
                        viewModel.selectTournament(tournament.id)
                    }
                )
                CalendarViewMode.UPCOMING -> UpcomingTournamentsView(
                    tournaments = upcomingTournaments,
                    userTournamentIds = userTournamentIds,
                    currentUser = currentUser,
                    viewModel = viewModel,
                    onTournamentClick = { tournament ->
                        showTournamentDetail = tournament
                    }
                )
            }

            // Filter chips (only for LIST and UPCOMING modes)
            if (viewMode in setOf(CalendarViewMode.LIST, CalendarViewMode.UPCOMING)) {
                FilterChipsBar(
                    availableGames = availableGames,
                    selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it }
                )
            }
        }

        // Tournament Detail Dialog
        showTournamentDetail?.let { tournament ->
            TournamentDetailDialog(
                tournament = tournament,
                viewModel = viewModel,
                currentUser = currentUser,
                userRegistered = userTournamentIds.contains(tournament.id),
                onDismiss = { showTournamentDetail = null },
                onRegister = {
                    viewModel.selectTournament(tournament.id)
                    showTournamentDetail = null
                }
            )
        }

        // Match Detail Dialog
        showMatchDetail?.let { match ->
            MatchDetailDialog(
                match = match,
                onDismiss = { showMatchDetail = null }
            )
        }
    }
}

enum class CalendarViewMode(val icon: ImageVector, val label: String) {
    MONTH(Icons.Filled.CalendarMonth, "Month"),
    WEEK(Icons.Filled.CalendarViewWeek, "Week"),
    LIST(Icons.AutoMirrored.Filled.List, "List"),
    UPCOMING(Icons.Filled.Schedule, "Upcoming")
}

@Composable
fun MonthNavigator(
    selectedDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    tournamentsByDate: Map<String, List<Tournament>>,
    today: LocalDate
) {
    val currentMonth = remember { mutableStateOf(YearMonth.now()) }
    val daysInMonth = currentMonth.value.lengthOfMonth()
    val firstDayOfWeek = LocalDate.of(currentMonth.value.year, currentMonth.value.monthValue, 1).dayOfWeek

    Card(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, RaivalSurfaceLight)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth.value = currentMonth.value.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = RaivalPrimary)
                }
                Text(
                    text = currentMonth.value.toString(),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                IconButton(onClick = { currentMonth.value = currentMonth.value.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = RaivalPrimary)
                }
            }

            // Day headers
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(text = day, color = RaivalTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                }
            }

            // Calendar grid
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val totalCells = firstDayOfWeek.value + daysInMonth
                val weeks = (totalCells + 6) / 7
                for (week in 0 until weeks) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (dayOfWeek in 0..6) {
                            val cellIndex = week * 7 + dayOfWeek
                            val dayNumber = cellIndex - firstDayOfWeek.value + 1
                            val isCurrentMonth = dayNumber in 1..daysInMonth
                            val date = if (isCurrentMonth) LocalDate.of(currentMonth.value.year, currentMonth.value.monthValue, dayNumber) else null
                            val hasTournaments = date?.let { tournamentsByDate[it.toString()]?.isNotEmpty() == true } == true
                            val isSelected = selectedDate == date
                            val isToday = date == today

                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(40.dp)
                                    .background(
                                        color = when {
                                            isSelected -> RaivalPrimary
                                            isToday && !isSelected -> RaivalSecondary.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        },
                                        shape = CircleShape
                                    )
                                    .clip(CircleShape)
                                    .clickable(enabled = isCurrentMonth) { date?.let { onDateClick(it) } }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCurrentMonth) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNumber.toString(),
                                            color = if (isSelected) Color.Black else if (isToday) RaivalSecondary else Color.White,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                        if (hasTournaments && !isSelected) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier.size(4.dp)
                                                    .background(RaivalSecondary, shape = CircleShape)
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

@Composable
fun MonthCalendarView(
    selectedDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    tournamentsByDate: Map<String, List<Tournament>>,
    userTournamentIds: Set<Int>,
    onTournamentClick: (Tournament) -> Unit,
    today: LocalDate
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 100.dp)
    ) {
        // Selected date tournaments
        val selectedDateTournaments = selectedDate?.let { tournamentsByDate[it.toString()] } ?: emptyList()
        
        if (selectedDateTournaments.isNotEmpty()) {
            val dateToShow = selectedDate ?: LocalDate.now()
            item {
                Text(
                    text = "Tournaments on ${SimpleDateFormat("MMM dd, yyyy", Locale.US).format(java.util.Date.from(dateToShow.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            items(selectedDateTournaments) { tournament ->
                TournamentCalendarCard(
                    tournament = tournament,
                    isRegistered = userTournamentIds.contains(tournament.id),
                    onClick = { onTournamentClick(tournament) }
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp)
                        .background(RaivalSurface, shape = RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.dp, RaivalSurfaceLight), shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventAvailable, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tournaments on ${SimpleDateFormat("MMM dd, yyyy", Locale.US).format(java.util.Date.from((selectedDate ?: LocalDate.now()).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()))}",
                            color = RaivalTextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tap a date with tournaments or switch to List view",
                            color = RaivalTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // All tournaments grouped by date
        item {
            Text(
                text = "All Scheduled Tournaments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        tournamentsByDate.forEach { (dateStr, tournaments) ->
            val date = LocalDate.parse(dateStr)
            val isPast = date.isBefore(today)
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RaivalSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isPast) RaivalTextSecondary.copy(alpha = 0.3f) else RaivalSurfaceLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US).format(java.util.Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())),
                                fontWeight = FontWeight.Bold,
                                color = if (isPast) RaivalTextSecondary else RaivalSecondary,
                                fontSize = 14.sp
                            )
                            if (!isPast) {
                                Text(
                                    text = "${tournaments.size} tournament${if (tournaments.size > 1) "s" else ""}",
                                    color = RaivalTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tournaments) { tournament ->
                                TournamentCalendarCard(
                                    tournament = tournament,
                                    isRegistered = userTournamentIds.contains(tournament.id),
                                    isPast = isPast,
                                    onClick = { onTournamentClick(tournament) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekCalendarView(
    selectedDate: LocalDate?,
    tournaments: List<Tournament>,
    userTournamentIds: Set<Int>,
    onTournamentClick: (Tournament) -> Unit,
    today: LocalDate
) {
    val weekStart = remember(selectedDate, today) {
        selectedDate?.let { it.minusDays((it.dayOfWeek.value - 1).toLong()) } ?: today.minusDays((today.dayOfWeek.value - 1).toLong())
    }
    
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
    val tournamentsByDay = remember(tournaments, weekStart) {
        tournaments.groupBy { LocalDate.parse(it.date) }.mapValues { it.value }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 100.dp)
    ) {
        // Week header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${SimpleDateFormat("MMM dd", Locale.US).format(java.util.Date.from(weekStart.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()))} - ${SimpleDateFormat("MMM dd, yyyy", Locale.US).format(java.util.Date.from(weekStart.plusDays(6).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { /* navigate prev week */ }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Week", tint = RaivalPrimary)
                    }
                    IconButton(onClick = { /* navigate next week */ }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Week", tint = RaivalPrimary)
                    }
                }
            }
        }

        // Days of week
        weekDays.forEach { day ->
            val dayTournaments = tournamentsByDay[day] ?: emptyList()
            val isToday = day == today
            val isSelected = selectedDate == day
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) RaivalPrimary.copy(alpha = 0.15f) else RaivalSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSelected) RaivalPrimary else RaivalSurfaceLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(RaivalSecondary, shape = CircleShape)
                                            .padding(end = 8.dp)
                                    )
                                }
                                Text(
                                    text = SimpleDateFormat("EEE, MMM dd", Locale.US).format(java.util.Date.from(day.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())),
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) RaivalSecondary else Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            if (dayTournaments.isNotEmpty()) {
                                Text(
                                    text = "${dayTournaments.size} match${if (dayTournaments.size > 1) "es" else ""}",
                                    color = RaivalTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        
                        if (dayTournaments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                dayTournaments.forEach { tournament ->
                                    TournamentCalendarCard(
                                        tournament = tournament,
                                        isRegistered = userTournamentIds.contains(tournament.id),
                                        compact = true,
                                        onClick = { onTournamentClick(tournament) }
                                    )
                                }
                            }
                        } else if (isToday) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tournaments scheduled today",
                                color = RaivalTextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentListView(
    tournaments: List<Tournament>,
    userTournamentIds: Set<Int>,
    onTournamentClick: (Tournament) -> Unit,
    onRegisterClick: (Tournament) -> Unit
) {
    if (tournaments.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("No tournaments found", color = RaivalTextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Try adjusting your filters", color = RaivalTextSecondary, fontSize = 12.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(tournaments) { tournament ->
            TournamentCalendarCard(
                tournament = tournament,
                isRegistered = userTournamentIds.contains(tournament.id),
                onClick = { onTournamentClick(tournament) },
                onRegisterClick = { onRegisterClick(tournament) }
            )
        }
    }
}

@Composable
fun UpcomingTournamentsView(
    tournaments: List<Tournament>,
    userTournamentIds: Set<Int>,
    currentUser: com.example.data.model.User?,
    viewModel: RaivalViewModel,
    onTournamentClick: (Tournament) -> Unit
) {
    if (tournaments.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("No upcoming tournaments", color = RaivalTextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Check back later for new competitions!", color = RaivalTextSecondary, fontSize = 12.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header stats
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RaivalSurfaceLight),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, RaivalSecondary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Upcoming Tournaments", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("${tournaments.size} open for registration", color = RaivalTextSecondary, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Registered", fontWeight = FontWeight.Bold, color = RaivalSecondary, fontSize = 12.sp)
                        Text("${userTournamentIds.size}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                }
            }
        }

        items(tournaments) { tournament ->
            TournamentCalendarCard(
                tournament = tournament,
                isRegistered = userTournamentIds.contains(tournament.id),
                onClick = { onTournamentClick(tournament) },
                onRegisterClick = {
                    viewModel.selectTournament(tournament.id)
                },
                showRegisterButton = !userTournamentIds.contains(tournament.id) && tournament.status == "Open" && currentUser != null
            )
        }
    }
}

@Composable
fun TournamentCalendarCard(
    tournament: Tournament,
    isRegistered: Boolean = false,
    isPast: Boolean = false,
    compact: Boolean = false,
    showRegisterButton: Boolean = false,
    onClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {}
) {
    val gameTheme = getGameTheme(tournament.game)
    val isOpen = tournament.status == "Open"
    val date = try { LocalDate.parse(tournament.date) } catch (e: Exception) { LocalDate.now() }
    val today = LocalDate.now()
    val isUpcoming = date.isAfter(today) || date == today
    val daysUntil = ChronoUnit.DAYS.between(today, date)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(if (isPast) 0.6f else 1f),
        colors = CardDefaults.cardColors(containerColor = if (isRegistered) RaivalPrimary.copy(alpha = 0.1f) else RaivalSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isRegistered) RaivalPrimary.copy(alpha = 0.5f) else gameTheme.accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(if (compact) 12.dp else 16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Game badge
                    Box(
                        modifier = Modifier
                            .background(gameTheme.accentColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tournament.game,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = gameTheme.accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Tournament title
                    Text(
                        text = tournament.title,
                        fontSize = if (compact) 12.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRegistered) RaivalPrimary else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Status badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isRegistered) {
                        Badge(
                            text = "REGISTERED",
                            color = RaivalPrimary,
                            icon = Icons.Default.CheckCircle
                        )
                    } else if (!isPast && isOpen) {
                        Badge(
                            text = "OPEN",
                            color = RaivalSuccess,
                            icon = Icons.Default.PlayCircle
                        )
                    }
                    if (!isPast && !isOpen) {
                        Badge(
                            text = tournament.status,
                            color = RaivalTextSecondary,
                            icon = Icons.Default.Schedule
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))
            
            // Date, time, format row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Date
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(14.dp))
                        Column {
                            Text(
                                text = SimpleDateFormat("EEE, MMM dd", Locale.US).format(java.util.Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())),
                                color = if (isUpcoming) RaivalTextSecondary else RaivalError,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (!compact) {
                                Text(
                                    text = tournament.time,
                                    color = RaivalTextSecondary.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    
                    // Format & size
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(14.dp))
                        Text(
                            text = "${tournament.format} • ${tournament.maxPlayers}P",
                            color = RaivalTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                
                // Days until / Prize
                Column(horizontalAlignment = Alignment.End) {
                    if (!isPast) {
                        if (daysUntil == 0L) {
                            Text("TODAY", color = RaivalSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else if (daysUntil == 1L) {
                            Text("TOMORROW", color = RaivalSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else if (daysUntil <= 7L) {
                            Text("IN $daysUntil DAYS", color = RaivalTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!compact && tournament.prize > 0 || tournament.coinPrize > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = RaivalSecondary, modifier = Modifier.size(12.dp))
                            Text(
                                text = if (tournament.prize > 0) "₵${tournament.prize}" else "${tournament.coinPrize} Coins",
                                color = RaivalSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Register button
            if (showRegisterButton && !compact) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRegisterClick,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HowToReg, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("REGISTER NOW", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun Badge(text: String, color: Color, icon: ImageVector) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
            .border(BorderStroke(1.dp, color), shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(text, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FilterChipsBar(
    availableGames: List<String>,
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(RaivalSurface, shape = RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, RaivalSurfaceLight), shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = RaivalSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("FILTER:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RaivalTextSecondary)
                availableGames.forEach { game ->
                    val isSelected = selectedFilter == game
                    Box(
                        modifier = Modifier
                            .clickable { onFilterChange(game) }
                            .background(if (isSelected) RaivalPrimary else RaivalSurfaceLight, shape = RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, if (isSelected) RaivalPrimary else RaivalSurfaceLight), shape = RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = game,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailDialog(
    tournament: Tournament,
    viewModel: RaivalViewModel,
    currentUser: com.example.data.model.User?,
    userRegistered: Boolean,
    onDismiss: () -> Unit,
    onRegister: () -> Unit
) {
    val context = LocalContext.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(500.dp),
            colors = CardDefaults.cardColors(containerColor = RaivalSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = RaivalTextSecondary, modifier = Modifier.size(24.dp))
                    }
                }
                
                // Tournament info
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tournament.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Badge(text = tournament.game, color = getGameTheme(tournament.game).accentColor, icon = Icons.Default.SportsEsports)
                        Badge(text = tournament.format, color = RaivalSecondary, icon = Icons.AutoMirrored.Filled.FormatListBulleted)
                        Badge(text = "${tournament.maxPlayers} Players", color = RaivalPrimary, icon = Icons.Default.Group)
                    }
                }
                
                HorizontalDivider(color = RaivalSurfaceLight)
                
                // Details grid
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailRow(icon = Icons.Default.CalendarToday, label = "Date", value = tournament.date)
                    DetailRow(icon = Icons.Default.Schedule, label = "Time", value = "${tournament.time} GMT")
                    DetailRow(icon = Icons.Default.EmojiEvents, label = "Format", value = tournament.format)
                    DetailRow(icon = Icons.Default.Person, label = "Mode", value = tournament.mode)
                    DetailRow(icon = Icons.Default.Stadium, label = "Stadium", value = tournament.map)
                    DetailRow(icon = Icons.Default.AttachMoney, label = "Entry Fee", value = if (tournament.entryFee > 0) "₵${tournament.entryFee}" else "${tournament.coinEntryFee} Coins")
                    DetailRow(icon = Icons.Default.MonetizationOn, label = "Prize", value = if (tournament.prize > 0) "₵${tournament.prize}" else "${tournament.coinPrize} Coins")
                    DetailRow(icon = Icons.Default.Group, label = "Slots", value = "${tournament.players}/${tournament.maxPlayers}")
                }
                
                if (tournament.description.isNotEmpty()) {
                    HorizontalDivider(color = RaivalSurfaceLight)
                    Text(
                        text = tournament.description,
                        color = RaivalTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (tournament.rules.isNotEmpty()) {
                    HorizontalDivider(color = RaivalSurfaceLight)
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Rules", fontWeight = FontWeight.Bold, color = RaivalPrimary, fontSize = 12.sp)
                        Text(
                            text = tournament.rules,
                            color = RaivalTextSecondary,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Action buttons
                Spacer(modifier = Modifier.height(8.dp))
                if (!userRegistered && currentUser != null && tournament.status == "Open") {
                    Button(
                        onClick = onRegister,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RaivalPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HowToReg, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("REGISTER FOR TOURNAMENT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else if (userRegistered) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RaivalPrimary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaivalPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("You are registered for this tournament", color = RaivalPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(20.dp).padding(end = 12.dp))
        Text(text = label, color = RaivalTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailDialog(
    match: TournamentMatch,
    onDismiss: () -> Unit
) {
    val isCompleted = match.status == "Completed"
    val p1Wins = isCompleted && match.player1Score != null && match.player2Score != null && match.player1Score > match.player2Score
    val p2Wins = isCompleted && match.player1Score != null && match.player2Score != null && match.player2Score > match.player1Score

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = RaivalSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, RaivalPrimary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = RaivalTextSecondary, modifier = Modifier.size(24.dp))
                    }
                }
                
                Text(
                    text = "${match.round} - Match #${match.matchIndex + 1}",
                    fontWeight = FontWeight.Bold,
                    color = RaivalPrimary,
                    fontSize = 14.sp
                )
                
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RaivalSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("MATCH COMPLETED", color = RaivalSuccess, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Player 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (p1Wins) Icons.Default.EmojiEvents else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (p1Wins) RaivalSecondary else RaivalPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = match.player1Name,
                                    color = if (p1Wins) RaivalSecondary else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                if (match.player1Rating > 0) Text("Rating: ${match.player1Rating}", color = RaivalTextSecondary, fontSize = 10.sp)
                                if (match.player1Team.isNotEmpty()) Text(match.player1Team, color = RaivalTextSecondary, fontSize = 10.sp)
                            }
                        }
                        Text(
                            text = match.player1Score?.toString() ?: "-",
                            color = if (p1Wins) RaivalSecondary else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                    }
                    
                    // VS
                    Text(
                        text = "VS",
                        color = RaivalSecondary,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    
                    // Player 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (p2Wins) Icons.Default.EmojiEvents else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (p2Wins) RaivalSecondary else RaivalAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = match.player2Name,
                                    color = if (p2Wins) RaivalSecondary else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                if (match.player2Rating > 0) Text("Rating: ${match.player2Rating}", color = RaivalTextSecondary, fontSize = 10.sp)
                                if (match.player2Team.isNotEmpty()) Text(match.player2Team, color = RaivalTextSecondary, fontSize = 10.sp)
                            }
                        }
                        Text(
                            text = match.player2Score?.toString() ?: "-",
                            color = if (p2Wins) RaivalSecondary else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                    }
                }
                
                if (match.matchDate.isNotEmpty() && match.matchTime.isNotEmpty()) {
                    HorizontalDivider(color = RaivalSurfaceLight)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = RaivalTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scheduled: ${match.matchDate} @ ${match.matchTime} GMT", color = RaivalTextSecondary, fontSize = 12.sp)
                    }
                }
                
                if (isCompleted && match.winnerName != null) {
                    HorizontalDivider(color = RaivalSurfaceLight)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RaivalSecondary.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WINNER", color = RaivalSecondary, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
                            Text(
                                text = match.winnerName!!,
                                color = RaivalSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}