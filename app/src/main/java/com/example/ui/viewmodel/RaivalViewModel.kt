package com.example.ui.viewmodel

import com.example.BuildConfig

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import androidx.core.content.edit  // KTX extension for SharedPreferences
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.RaivalRepository
import com.example.data.GeminiHelper
import com.example.data.PasswordHelper
import com.example.data.SupabaseConfig
import com.example.data.sync.SupabaseSyncManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.UUID

@SuppressLint("DefaultLocale")

data class SessionTransitionInfo(
    val oldSessionName: String,
    val newSessionName: String,
    val coinsEarned: Int,
    val userPosition: Int
)

class RaivalViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)

    val repository = RaivalRepository(
        scope = viewModelScope,
        userDao = database.userDao(),
        tournamentDao = database.tournamentDao(),
        registrationDao = database.registrationDao(),
        transactionDao = database.transactionDao(),
        teamDao = database.teamDao(),
        chatMessageDao = database.chatMessageDao(),
        matchSessionDao = database.matchSessionDao(),
        appConfigDao = database.appConfigDao(),
        marketplaceListingDao = database.marketplaceListingDao(),
        marketplaceOfferDao = database.marketplaceOfferDao(),
        marketplaceTransactionDao = database.marketplaceTransactionDao(),
        tournamentMatchDao = database.tournamentMatchDao(),
        communityPostDao = database.communityPostDao(),
        communityCommentDao = database.communityCommentDao()
    )

    // Exposed Flows from DB
    val currentUser: StateFlow<User?> = repository.currentUser
    val sharedPrefs = application.getSharedPreferences("raival_prefs", android.content.Context.MODE_PRIVATE)
    private val cachedEmail = sharedPrefs.getString("last_logged_in_email", null)
    val isStartupLoading = MutableStateFlow(cachedEmail != null)
    val showOnboarding = MutableStateFlow(!sharedPrefs.getBoolean("has_seen_onboarding", false))
    val authState = MutableStateFlow<String?>(null) // null = not logged in, "guest" = guest mode, "auth" = real auth

    fun setHasSeenOnboarding() {
        sharedPrefs.edit().putBoolean("has_seen_onboarding", true).apply()
        showOnboarding.value = false
    }

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    private val _isCloudPulling = MutableStateFlow(false)
    val isCloudPulling: StateFlow<Boolean> = _isCloudPulling.asStateFlow()
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null
    private var syncPollingJob: kotlinx.coroutines.Job? = null

    val tournaments: StateFlow<List<Tournament>> = repository.allTournaments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _activePlayingGameId = MutableStateFlow<String?>(null)
    val activePlayingGameId: StateFlow<String?> = _activePlayingGameId.asStateFlow()
    fun setActivePlayingGameId(id: String?) { _activePlayingGameId.value = id }
    val teams: StateFlow<List<Team>> = repository.allTeams.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val appConfig: StateFlow<AppConfig?> = repository.appConfig.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateAppConfig(config: AppConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAppConfig(config)
        }
    }

    // Community Feed State and Methods
    val communityPosts: StateFlow<List<CommunityPost>> = repository.allCommunityPosts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPost(
        title: String,
        content: String,
        category: String,
        tournamentId: Int? = null,
        tournamentTitle: String? = null,
        statGame: String? = null,
        statValue: String? = null,
        statLabel: String? = null,
        teamId: Int? = null,
        teamName: String? = null,
        onComplete: () -> Unit = {}
    ) {
        if (!isOnline.value) {
            return
        }
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.createCommunityPost(
                CommunityPost(
                    userId = user.id,
                    username = user.username,
                    userAvatar = user.selectedAvatar,
                    title = title,
                    content = content,
                    category = category,
                    tournamentId = tournamentId,
                    tournamentTitle = tournamentTitle,
                    statGame = statGame,
                    statValue = statValue,
                    statLabel = statLabel,
                    teamId = teamId,
                    teamName = teamName
                )
            )
            onComplete()
        }
    }

    fun deletePost(postId: Int, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCommunityPost(postId)
            onComplete()
        }
    }

    fun toggleLikePost(post: CommunityPost) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val userList = post.likedByUserIds.split(",").filter { it.isNotEmpty() }.toMutableList()
            val userIdStr = user.id.toString()
            val newLikedList = if (userList.contains(userIdStr)) {
                userList.remove(userIdStr)
                userList
            } else {
                userList.add(userIdStr)
                userList
            }
            val newLikesCount = newLikedList.size
            val newLikedByUserIds = newLikedList.joinToString(",")
            val updatedPost = post.copy(
                likesCount = newLikesCount,
                likedByUserIds = newLikedByUserIds
            )
            repository.updateCommunityPost(updatedPost)
        }
    }

    fun addComment(postId: Int, content: String, onComplete: () -> Unit = {}) {
        if (!isOnline.value) {
            return
        }
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addCommentToPost(
                CommunityComment(
                    postId = postId,
                    userId = user.id,
                    username = user.username,
                    userAvatar = user.selectedAvatar,
                    content = content
                )
            )
            val post = repository.getCommunityPostById(postId)
            if (post != null) {
                repository.updateCommunityPost(post.copy(commentsCount = post.commentsCount + 1))
            }
            onComplete()
        }
    }

    fun getCommentsForPost(postId: Int): Flow<List<CommunityComment>> {
        return repository.getCommentsForPost(postId)
    }

    fun resetAllCoinsToZero(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetAllCoinsToZero()
            repository.refreshCurrentUser()
            onComplete()
        }
    }
    val allUsers: StateFlow<List<User>> = repository.allUsersByPoints.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Marketplace Flows and States
    val activeListings: StateFlow<List<MarketplaceListing>> = repository.activeListings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val allListings: StateFlow<List<MarketplaceListing>> = repository.allListings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userMarketplaceTransactions: StateFlow<List<MarketplaceTransaction>> = repository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getMarketplaceTransactionsForUser(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMarketplaceTransactions: StateFlow<List<MarketplaceTransaction>> = repository.allMarketplaceTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Marketplace Actions
    fun createMarketplaceListing(listing: MarketplaceListing, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.createListing(listing)
            onComplete(id)
        }
    }

    fun updateMarketplaceListing(listing: MarketplaceListing, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateListing(listing)
            onComplete()
        }
    }

    fun deleteMarketplaceListing(listing: MarketplaceListing, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteListing(listing)
            onComplete()
        }
    }

    fun makeMarketplaceOffer(offer: MarketplaceOffer, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.makeOffer(offer)
            onComplete(id)
        }
    }

    fun updateMarketplaceOffer(offer: MarketplaceOffer, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateOffer(offer)
            onComplete()
        }
    }

    fun createMarketplaceTransaction(transaction: MarketplaceTransaction, onComplete: (Long) -> Unit) {
        if (BuildConfig.IS_DEMO_MODE) {
            onComplete(-1)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.createMarketplaceTransaction(transaction)
            onComplete(id)
        }
    }

    fun updateMarketplaceTransaction(transaction: MarketplaceTransaction, onComplete: () -> Unit = {}) {
        if (BuildConfig.IS_DEMO_MODE) {
            onComplete()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMarketplaceTransaction(transaction)
            // Refresh currentUser in case balance or details changed
            repository.refreshCurrentUser()
            onComplete()
        }
    }

    fun getOffersForListing(listingId: Int): Flow<List<MarketplaceOffer>> = repository.getOffersForListing(listingId)
    
    fun getListingById(id: Int): Flow<MarketplaceListing?> = repository.getListingById(id)

    // Dynamic Selected States
    private val _selectedTournamentId = MutableStateFlow<Int?>(null)
    val selectedTournamentId = _selectedTournamentId.asStateFlow()

    val selectedTournament: StateFlow<Tournament?> = combine(
        _selectedTournamentId.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getTournamentById(id)
        },
        isOnline
    ) { tour, online ->
        if (!online) null else tour
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeChatRoomId = MutableStateFlow<String?>(null)
    val activeChatRoomId = _activeChatRoomId.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessage>> = _activeChatRoomId
        .flatMapLatest { roomId ->
            if (roomId == null) flowOf(emptyList())
            else repository.getChatMessages(roomId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions and registrations are dynamic based on current user
    val userTransactions: StateFlow<List<MoMoTransaction>> = repository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getTransactionsForUser(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userRegistrations: StateFlow<List<Registration>> = repository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getRegistrationsForUser(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and filter state
    val searchQuery = MutableStateFlow("")
    val activeFilter = MutableStateFlow("all") // "all", "featured", "open"

    private val _tournamentPostponedEvent = MutableStateFlow<String?>(null)
    val tournamentPostponedEvent = _tournamentPostponedEvent.asStateFlow()

    fun clearTournamentPostponedEvent() {
        _tournamentPostponedEvent.value = null
    }

    val filteredTournaments: StateFlow<List<Tournament>> = combine(
        repository.allTournaments,
        searchQuery,
        activeFilter,
        isOnline
    ) { list, query, filter, online ->
        if (!online) {
            emptyList()
        } else {
            var result = list.filter { !it.isHidden }

            // Apply Search
            if (query.isNotEmpty()) {
                result = result.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.game.contains(query, ignoreCase = true)
                }
            }

            // Apply Category Filters
            if (filter != "all") {
                when (filter) {
                    "featured" -> {
                        result = result.filter { it.prize >= 150.0 || it.coinPrize >= 1000 }
                    }
                    "open" -> {
                        result = result.filter { it.status == "Open" }
                    }
                    "size_2" -> {
                        result = result.filter { it.maxPlayers == 2 }
                    }
                    "size_4" -> {
                        result = result.filter { it.maxPlayers == 4 }
                    }
                    "size_8" -> {
                        result = result.filter { it.maxPlayers == 8 }
                    }
                    "size_16" -> {
                        result = result.filter { it.maxPlayers == 16 }
                    }
                    "size_32" -> {
                        result = result.filter { it.maxPlayers == 32 }
                    }
                    else -> {
                        result = result.filter {
                            it.game.contains(filter, ignoreCase = true)
                        }
                    }
                }
            }
            result
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val tournamentRescheduleCount = mutableMapOf<Int, Int>()

    fun autoPruneExpiredOrPlayedTournaments() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val todayStr = sdf.format(java.util.Date(now))
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val currentMin = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)

                // 1. Check Open tournaments whose start time has passed
                val allTours = database.tournamentDao().getAllTournamentsOneShot()
                for (tour in allTours) {
                    if (tour.status != "Open") continue
                    if (!tour.isAutoHosted) continue

                    val tourTimeParts = tour.time.split(":")
                    val tourHour = tourTimeParts.getOrNull(0)?.toIntOrNull() ?: continue
                    val tourMin = tourTimeParts.getOrNull(1)?.toIntOrNull() ?: 0

                    val isSameDay = tour.date == todayStr
                    val isPastTime = tourHour < currentHour || (tourHour == currentHour && tourMin <= currentMin)

                    if (!isSameDay || !isPastTime) continue

                    if (tour.players < tour.maxPlayers) {
                        // Not enough players — reschedule
                        val count = tournamentRescheduleCount[tour.id] ?: 0
                        val (newDate, newTime) = if (count == 0) {
                            // First time: +1 hour
                            val newH = (tourHour + 1) % 24
                            val newT = String.format("%02d:%02d", newH, tourMin)
                            val newD = if (newH < tourHour) getOffsetDay(tour.date, 1) else tour.date
                            Pair(newD, newT)
                        } else {
                            // Second+ time: next day same time
                            Pair(getOffsetDay(tour.date, 1), tour.time)
                        }
                        tournamentRescheduleCount[tour.id] = count + 1

                        val updated = tour.copy(
                            date = newDate, time = newTime,
                            description = if (count == 0) "⏰ [RESCHEDULED to $newTime] ${tour.description}"
                                          else "📅 [POSTPONED to $newDate] ${tour.description}"
                        )
                        database.tournamentDao().updateTournament(updated)
                        repository.sendChatMessage("tournament_${tour.id}", "⏰ Tournament rescheduled to **$newDate at $newTime** due to insufficient players.")
                    } else {
                        // Enough players — generate bracket if not already done
                        val existingMatches = database.tournamentMatchDao().getMatchesForTournamentOneShot(tour.id)
                        if (existingMatches.isEmpty()) {
                            generateBracketForTournament(tour.id, force = true) {
                                viewModelScope.launch(Dispatchers.IO) {
                                    autoOrchestrateTournament(tour.id)
                                    val matches = database.tournamentMatchDao().getMatchesForTournamentOneShot(tour.id)
                                    matches.firstOrNull { it.status == "Pending" }?.let { initializeLobby(it.id) }
                                }
                            }
                        }
                    }
}
                
                // 2. Check match deadlines — auto-disqualify no-shows after 5 min check-in
                val allMatches = database.tournamentMatchDao().getAllMatches().firstOrNull() ?: emptyList()
                for (match in allMatches) {
                    if (match.status == "Completed") continue

                    // Check-in deadline expired — auto-disqualify un-checked-in players
                    if (match.checkInDeadline > 0 && now > match.checkInDeadline) {
                        if (!match.player1CheckedIn && !match.player1Disqualified) {
                            disqualifyPlayer(match.id, true)
                        } else if (!match.player2CheckedIn && !match.player2Disqualified) {
                            disqualifyPlayer(match.id, false)
                        }
                    }

                    // Match deadline expired — auto-resolve
                    if (match.matchDeadline > 0 && now > match.matchDeadline) {
                        autoResolveMatchOnTimeout(match.id)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    init {
        // Register network callback to monitor online status in real-time
        try {
            val connectivityManager = application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            // Initial check
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            _isOnline.value = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            val callback = object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    _isOnline.value = true
                    // Refresh data when network becomes available again
                    pullAllDataFromCloud()
                }

                override fun onLost(network: android.net.Network) {
                    _isOnline.value = false
                }
            }
            networkCallback = callback
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            android.util.Log.e("NetworkMonitor", "Failed to register network callback", e)
            _isOnline.value = true // fallback to true
        }

        // 1. Fast-path local session restoration
        if (cachedEmail != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val user = repository.login(cachedEmail)
                    if (user != null) {
                        repository.setCurrentUser(user)
                        android.util.Log.d("RaivalAuth", "Fast-path cached local login successful for $cachedEmail")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RaivalViewModel", "Fast-path login failed", e)
                }
                isStartupLoading.value = false
            }
        } else {
            isStartupLoading.value = false
        }

        // 3. Periodic Auto-Pruning for Tournaments that passed or played
         viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    autoPruneExpiredOrPlayedTournaments()
                } catch (e: Exception) {
                    android.util.Log.e("RaivalViewModel", "Auto-prune failed", e)
                }
                kotlinx.coroutines.delay(10000) // check every 10 seconds
            }
        }

        // 2. Background data initialization
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.populateInitialData()
            } catch (e: Exception) {
                android.util.Log.e("RaivalViewModel", "populateInitialData failed", e)
            }

            try {
                ensureDefaultTournamentsSeeded()
            } catch (e: Exception) {
                android.util.Log.e("RaivalViewModel", "ensureDefaultTournamentsSeeded failed", e)
            }

            pullAllDataFromCloud()
            startSupabaseRealtimeSync()

            // Keep local session active if cached
            if (cachedEmail != null) {
                try {
                    val user = repository.login(cachedEmail)
                    if (user != null) {
                        repository.setCurrentUser(user)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RaivalViewModel", "Session restore failed", e)
                }
            }
        }
    }


    fun pullAllDataFromCloud() {
        _isCloudPulling.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val sync = com.example.data.sync.SupabaseSyncManager

            // Helper to pull table with error logging (continues on failure)
            suspend fun pullTableSafe(table: String): List<JsonObject> {
                return try {
                    sync.pullTable(table)
                } catch (e: Exception) {
                    android.util.Log.e("RaivalCloudPull", "Failed to pull $table: ${e.message}", e)
                    emptyList()
                }
            }

            val usersJson = pullTableSafe("users")
            val configsJson = pullTableSafe("app_config")
            val tournamentsJson = pullTableSafe("tournaments")
            android.util.Log.d("RaivalCloudPull", "Pulled ${tournamentsJson.size} tournaments from Supabase: $tournamentsJson")
            val registrationsJson = pullTableSafe("registrations")
            val transactionsJson = pullTableSafe("transactions")
            val teamsJson = pullTableSafe("teams")
            val messagesJson = pullTableSafe("chat_messages")
            val sessionsJson = pullTableSafe("match_sessions")
            val matchesJson = pullTableSafe("tournament_matches")
            val listingsJson = pullTableSafe("marketplace_listings")
            val offersJson = pullTableSafe("marketplace_offers")
            val mktTransactionsJson = pullTableSafe("marketplace_transactions")
            val postsJson = pullTableSafe("community_posts")
            val commentsJson = pullTableSafe("community_comments")

            usersJson.forEach { database.userDao().insertUser(sync.toUser(it)) }
            configsJson.forEach { database.appConfigDao().insertOrUpdate(sync.toAppConfig(it)) }
            tournamentsJson.forEach { 
                try {
                    val t = sync.toTournament(it)
                    android.util.Log.d("RaivalCloudPull", "Inserting tournament id=${t.id}, title=${t.title}")
                    database.tournamentDao().insertTournament(t)
                } catch (e: Exception) {
                    android.util.Log.e("RaivalCloudPull", "Failed to parse tournament: $it", e)
                }
            }
            registrationsJson.forEach { database.registrationDao().insertRegistration(sync.toRegistration(it)) }
            transactionsJson.forEach { database.transactionDao().insertTransaction(sync.toTransaction(it)) }
            teamsJson.forEach { database.teamDao().insertTeam(sync.toTeam(it)) }
            messagesJson.forEach { database.chatMessageDao().insertMessage(sync.toChatMessage(it)) }
            sessionsJson.forEach { database.matchSessionDao().insertMatchSession(sync.toMatchSession(it)) }
            matchesJson.forEach { database.tournamentMatchDao().insertTournamentMatch(sync.toTournamentMatch(it)) }
            listingsJson.forEach { database.marketplaceListingDao().insertListing(sync.toMarketplaceListing(it)) }
            offersJson.forEach { database.marketplaceOfferDao().insertOffer(sync.toMarketplaceOffer(it)) }
            mktTransactionsJson.forEach { database.marketplaceTransactionDao().insertTransaction(sync.toMarketplaceTransaction(it)) }
            postsJson.forEach { database.communityPostDao().insertPost(sync.toCommunityPost(it)) }
            commentsJson.forEach { database.communityCommentDao().insertComment(sync.toCommunityComment(it)) }
            _isCloudPulling.value = false
        }
    }

    fun syncAllDataToSupabase(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sync = com.example.data.sync.SupabaseSyncManager

                val users = database.userDao().getAllUsersOneShot()
                users.forEach { sync.pushInsert("users", sync.toJson(it)) }

                val config = database.appConfigDao().getAppConfigOneShot()
                if (config != null) sync.pushInsert("app_config", sync.toJson(config))

                val tournaments = database.tournamentDao().getAllTournamentsOneShot()
                tournaments.forEach { sync.pushInsert("tournaments", sync.toJson(it)) }

                val registrations = database.registrationDao().getAllRegistrationsOneShot()
                registrations.forEach { sync.pushInsert("registrations", sync.toJson(it)) }

                onResult(true, "All data synced to Supabase successfully.")
            } catch (e: Exception) {
                onResult(false, "Sync failed: ${e.message}")
            }
        }
    }

    fun startSupabaseRealtimeSync() {
        syncPollingJob?.cancel()
         syncPollingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    try {
                        pullAllDataFromCloud()
                    } catch (_: Exception) {}
                    delay(30_000)
                }
            } catch (_: kotlinx.coroutines.CancellationException) { }
        }
    }

    fun stopSupabaseRealtimeSync() {
        syncPollingJob?.cancel()
        syncPollingJob = null
    }

    // ─── Supabase Chat Sync ──────────────────────────────────────
    fun sendSupabaseChatMessage(chatRoomId: String, message: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val chatMsg = ChatMessage(
                chatRoomId = chatRoomId,
                senderId = user.id,
                senderUsername = user.username,
                message = message,
                timestamp = System.currentTimeMillis()
            )
            database.chatMessageDao().insertMessage(chatMsg)
            try {
                SupabaseSyncManager.pushInsert("chat_messages", SupabaseSyncManager.toJson(chatMsg))
            } catch (_: Exception) {}
        }
    }

    // ─── Supabase Registration Sync ──────────────────────────────
    fun registerForTournamentSupabase(tournamentId: Int, onResult: (Boolean, String) -> Unit) {
        if (BuildConfig.IS_DEMO_MODE) {
            onResult(false, "Demo mode: Tournament registration disabled")
            return
        }
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val tournament = database.tournamentDao().getTournamentByIdOneShot(tournamentId)
            if (tournament == null) { onResult(false, "Tournament not found"); return@launch }

            // Check if tournament is full
            val regCount = database.registrationDao().getRegistrationsForTournamentOneShot(tournamentId).size
            if (regCount >= tournament.maxPlayers) { onResult(false, "Tournament is full"); return@launch }

            val existing = database.registrationDao().getRegistrationByUserAndTournament(user.id, tournamentId)
            if (existing != null) { onResult(false, "Already registered"); return@launch }

            if (user.coinBalance < tournament.coinEntryFee) {
                onResult(false, "Insufficient coins (${tournament.coinEntryFee} required)")
                return@launch
            }

            val reg = Registration(
                tournamentId = tournamentId,
                userId = user.id,
                playerName = user.username,
                inGameName = user.username,
                paymentMethod = "Coins",
                phone = user.phone,
                teamRating = 0,
                modelTeam = ""
            )
            database.registrationDao().insertRegistration(reg)
            repository.spendCoins(user.id, tournament.coinEntryFee)

            try {
                SupabaseSyncManager.pushInsert("registrations", SupabaseSyncManager.toJson(reg))
            } catch (_: Exception) {}

            onResult(true, "Registered! ${tournament.coinEntryFee} coins deducted.")
        }
    }

    // ─── Supabase Coin Sync ──────────────────────────────────────
    fun syncUserToSupabase() {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                SupabaseSyncManager.pushUpdate("users", user.id, SupabaseSyncManager.toJson(user))
            } catch (_: Exception) {}
        }
    }

    // ─── Supabase Post Sync ──────────────────────────────────────
    fun createSupabasePost(title: String, content: String, category: String, onResult: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val post = CommunityPost(
                userId = user.id,
                username = user.username,
                userAvatar = user.selectedAvatar,
                title = title,
                content = content,
                category = category,
                timestamp = System.currentTimeMillis()
            )
            database.communityPostDao().insertPost(post)
            try {
                SupabaseSyncManager.pushInsert("community_posts", SupabaseSyncManager.toJson(post))
            } catch (_: Exception) {}
            onResult(true)
        }
    }


    // Navigation & View Actions
    fun selectTournament(id: Int?) {
        _selectedTournamentId.value = id
        if (id != null) {
            _activeChatRoomId.value = "tournament_$id"
        }
    }

    fun selectChatRoom(roomId: String?) {
        _activeChatRoomId.value = roomId
    }

    // Auth actions
    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        if (!isOnline.value) {
            onResult(false, "Online login is required. Please connect to the internet.")
            return
        }
        val cleanEmail = email.trim().lowercase()
        
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.login(cleanEmail, password)
            if (user != null) {
                if (user.status == "banned") {
                    repository.setCurrentUser(null)
                    onResult(false, "This account has been banned.")
                } else {
                    sharedPrefs.edit().putString("last_logged_in_email", cleanEmail).apply()
                    onResult(true, "Logged in successfully!")
                }
            } else {
                onResult(false, "Invalid email or password.")
            }
        }
    }

    fun register(
        username: String,
        fullName: String,
        email: String,
        phone: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (!isOnline.value) {
            onResult(false, "Online registration is required. Please connect to the internet.")
            return
        }
        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            onResult(false, "Please fill in all fields.")
            return
        }
        if (password.length < 6) {
            onResult(false, "Password must be at least 6 characters.")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.registerUser(username, fullName, email, phone, password)
            val cleanEmail = email.trim().lowercase()
            sharedPrefs.edit().putString("last_logged_in_email", cleanEmail).apply()
            if (user != null) {
                onResult(true, "Account created successfully!")
            } else {
                onResult(false, "Registration failed. Username or email already exists.")
            }
        }
    }

    fun logout() {
        repository.setCurrentUser(null)
        sharedPrefs.edit().remove("last_logged_in_email").apply()
        viewModelScope.launch(Dispatchers.IO) { SupabaseConfig.signOut() }
        authState.value = null
    }

    // ─── Supabase Auth ────────────────────────────────────────────

    fun signUpWithEmail(email: String, password: String, username: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val supabaseId = SupabaseConfig.signUpWithEmail(email, password, username)
                if (supabaseId != null) {
                    val user = User(username = username, email = email, fullName = username, phone = "N/A")
                    val id = repository.registerUserForImport(user)
                    if (id != -1) {
                        val newUser = user.copy(id = id)
                        repository.setCurrentUser(newUser)
                        sharedPrefs.edit().putString("last_logged_in_email", email.trim().lowercase()).apply()
                        onResult(true, "Account created!")
                    } else {
                        onResult(false, "Username or email already exists locally")
                    }
                } else {
                    onResult(false, "Sign up failed. Email may already be in use.")
                }
            } catch (e: Exception) {
                onResult(false, "Error: ${e.localizedMessage}")
            }
        }
    }

    fun signInWithEmail(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val supabaseId = SupabaseConfig.signInWithEmail(email, password)
                if (supabaseId != null) {
                    val user = repository.login(email.trim().lowercase())
                    if (user != null) {
                        sharedPrefs.edit().putString("last_logged_in_email", email.trim().lowercase()).apply()
                        onResult(true, "Welcome back, ${user.username}!")
                    } else {
                        onResult(false, "Account not found locally. Please register first.")
                    }
                } else {
                    onResult(false, "Sign in failed. Check your credentials.")
                }
            } catch (e: Exception) {
                onResult(false, "Error: ${e.localizedMessage}")
            }
        }
    }

    fun continueAsGuest(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            val guestName = "Guest_${(1000..9999).random()}"
            // Real path: Supabase anonymous auth UID links this device to an
            // upgradeable account. Falls back to local-only guest when offline.
            val authUid = try {
                com.example.data.SupabaseConfig.signInAnonymously(guestName)
            } catch (_: Exception) { null }
            val guestUser = User(
                username = guestName,
                fullName = "Guest Player",
                email = "",
                phone = "",
                balance = 0.0,
                coinBalance = 50,
                raivalPoints = 0,
                authUid = authUid ?: ""
            )
            val id = repository.registerUserForImport(guestUser)
            val finalUser = if (id != -1) guestUser.copy(id = id) else guestUser
            repository.setCurrentUser(finalUser)
            if (authUid != null) {
                try {
                    SupabaseSyncManager.pushInsert("users", SupabaseSyncManager.toJson(finalUser))
                } catch (_: Exception) { }
                onResult(true, "Signed in as $guestName (anonymous account — link an email later to keep it).")
            } else {
                onResult(true, "Continuing offline as $guestName. Connect to secure your account.")
            }
        }
    }

    fun uploadMatchScreenshot(matchSessionId: Int, side: String, imageBytes: ByteArray, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val url = SupabaseSyncManager.uploadMatchScreenshot(matchSessionId, side, imageBytes)
            onResult(url)
        }
    }

    fun uploadProfileAvatar(userId: Int, imageBytes: ByteArray, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val url = SupabaseSyncManager.uploadProfileAvatar(userId, imageBytes)
            onResult(url)
        }
    }



    fun updateProfile(
        fullName: String,
        phone: String,
        bio: String,
        dlsHandle: String,
        efootballHandle: String,
        discordHandle: String,
        ghanaRegion: String = "Greater Accra",
        ghanaHometown: String = "Accra",
        preferredGame: String = "Dream League Soccer",
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = repository.updateUserProfile(
                fullName = fullName,
                phone = phone,
                bio = bio,
                dlsHandle = dlsHandle,
                efootballHandle = efootballHandle,
                discordHandle = discordHandle,
                ghanaRegion = ghanaRegion,
                ghanaHometown = ghanaHometown,
                preferredGame = preferredGame
            )
            onResult(updated != null)
        }
    }

    // Tournament registration flow with wallet logic
    fun registerForTournament(
        tournamentId: Int,
        inGameName: String,
        paymentMethod: String,
        phone: String,
        teamRating: Int = 0,
        modelTeam: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        if (!isOnline.value) {
            onResult(false, "You are currently offline. Please connect to the internet to register for tournaments.")
            return
        }
        val user = currentUser.value
        if (user == null) {
            onResult(false, "Please login to register.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.registerForTournament(
                userId = user.id,
                tournamentId = tournamentId,
                inGameName = inGameName,
                paymentMethod = paymentMethod,
                phone = phone,
                teamRating = teamRating,
                modelTeam = modelTeam
            )
            if (success) {
                viewModelScope.launch(Dispatchers.IO) {
                    val tournament = database.tournamentDao().getTournamentByIdOneShot(tournamentId)
                    if (tournament != null && tournament.isAutoHosted && tournament.players >= tournament.maxPlayers) {
                        generateBracketForTournament(tournamentId = tournamentId, force = true) {
                            autoOrchestrateTournament(tournamentId)
                        }
                    }
                }
                onResult(true, "Registration successful!")
            } else {
                onResult(false, "Insufficient wallet balance (GHS) or tournament is full. Please top up!")
            }
        }
    }

    // Wallet Deposit & Withdraw
    fun depositWallet(amount: Double, paymentMethod: String, phone: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.requestDeposit(amount, paymentMethod, phone)
            if (success) {
            }
            onResult(success)
        }
    }

    fun withdrawWallet(amount: Double, paymentMethod: String, phone: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.requestWithdrawal(amount, paymentMethod, phone)
            if (success) {
                onResult(true, "Withdrawal request processed!")
            } else {
                onResult(false, "Insufficient balance or invalid amount. Min withdrawal: GHS 20.")
            }
        }
    }

    // Chat
    fun sendChatMessage(messageText: String, imageUrl: String? = null) {
        if (!isOnline.value) {
            return
        }
        val roomId = _activeChatRoomId.value ?: return
        if (messageText.trim().isEmpty() && imageUrl == null) return

        viewModelScope.launch(Dispatchers.IO) {
            repository.sendChatMessage(roomId, messageText, imageUrl)
        }
    }

    // Team management
    fun createTeam(name: String, description: String, tags: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.createTeam(name, description, tags)
            onResult(id != -1L)
        }
    }

    // Admin Controls
    fun adminCreateTournament(
        title: String,
        game: String,
        entryFee: Double,
        coinEntryFee: Int,
        prize: Double,
        coinPrize: Int,
        maxPlayers: Int,
        date: String,
        time: String,
        description: String,
        mode: String,
        map: String,
        rules: String,
        schedule: String,
        prizeDistribution: String,
        contact: String,
        format: String = "Knockout",
        style: String = "World Cup Style",
        isAutoHosted: Boolean = true,
        onResult: (Boolean) -> Unit
    ) {
        if (currentUser.value?.role != "admin") { onResult(false); return }
        viewModelScope.launch(Dispatchers.IO) {
            // Auto-set maxPlayers for league formats (real players only, no bots)
            val finalMaxPlayers = when {
                style == "EPL Style" -> 8
                style == "Champions League Style" -> 36
                else -> maxPlayers
            }
            val t = Tournament(
                title = title,
                game = game,
                entryFee = entryFee,
                coinEntryFee = coinEntryFee,
                prize = prize,
                coinPrize = coinPrize,
                maxPlayers = finalMaxPlayers,
                date = date,
                time = time,
                description = description,
                mode = mode,
                map = map,
                rules = rules,
                schedule = schedule,
                prizeDistribution = prizeDistribution,
                contact = contact,
                organizer = currentUser.value?.username ?: "Admin",
                format = format,
                style = style,
                isAutoHosted = isAutoHosted
            )
            val id = repository.createTournament(t)
            if (id != -1L) {
                val inserted = t.copy(id = id.toInt())
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun adminCreateTournament(
        title: String,
        game: String,
        entryFee: Double,
        coinEntryFee: Int,
        prize: Double,
        coinPrize: Int,
        maxPlayers: Int,
        date: String,
        time: String,
        description: String,
        mode: String,
        map: String,
        rules: String,
        schedule: String,
        prizeDistribution: String,
        contact: String,
        onResult: (Boolean) -> Unit
    ) {
        adminCreateTournament(
            title = title,
            game = game,
            entryFee = entryFee,
            coinEntryFee = coinEntryFee,
            prize = prize,
            coinPrize = coinPrize,
            maxPlayers = maxPlayers,
            date = date,
            time = time,
            description = description,
            mode = mode,
            map = map,
            rules = rules,
            schedule = schedule,
            prizeDistribution = prizeDistribution,
            contact = contact,
            format = "Knockout",
            style = "World Cup Style",
            isAutoHosted = true,
            onResult = onResult
        )
    }

    fun adminDeleteTournament(id: Int) {
        if (currentUser.value?.role != "admin") return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTournament(id)
        }
    }

    fun adminUpdateTournament(tournament: Tournament) {
        if (currentUser.value?.role != "admin") return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTournament(tournament)
            
            try {
                val matchDao = database.tournamentMatchDao()
                val matches = (matchDao.getAllMatches().firstOrNull() ?: emptyList()).filter { it.tournamentId == tournament.id && it.status == "Pending" }
                matches.forEach { m ->
                    val updatedMatch = m.copy(
                        matchDate = tournament.date,
                        matchTime = tournament.time
                    )
                    matchDao.updateTournamentMatch(updatedMatch)
                }
            } catch (e: Exception) {
                android.util.Log.e("RaivalViewModel", "Error updating tournament matches: ${e.message}", e)
            }
        }
    }

    fun adminCancelAndRefundTournament(tournamentId: Int) {
        if (currentUser.value?.role != "admin") return
        viewModelScope.launch(Dispatchers.IO) {
            repository.cancelAndRefundTournament(tournamentId)
            val cancelledTour = database.tournamentDao().getTournamentByIdOneShot(tournamentId)
            if (cancelledTour != null) {
                            }
            val users = database.userDao().getAllUsersOneShot()
            users.forEach { user ->
                            }
        }
    }

    fun adminDuplicateTournament(tournament: Tournament) {
        if (currentUser.value?.role != "admin") return
        viewModelScope.launch(Dispatchers.IO) {
            val duplicate = tournament.copy(
                id = 0, // Room auto-generates PK
                title = "${tournament.title} (Copy)",
                players = 0,
                status = "Open"
            )
            val newId = repository.createTournament(duplicate)
            if (newId != -1L) {

            }
        }
    }

    fun adminArchiveTournament(tournament: Tournament) {
        if (currentUser.value?.role != "admin") return
        viewModelScope.launch(Dispatchers.IO) {
            val archived = tournament.copy(status = "Completed")
            repository.updateTournament(archived)
                    }
    }

    fun adminToggleHideTournament(tournament: Tournament) {
        if (currentUser.value?.role != "admin") return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = tournament.copy(isHidden = !tournament.isHidden)
            repository.updateTournament(updated)
                    }
    }

    fun ensureDefaultTournamentsSeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tournamentDao = database.tournamentDao()
                val currentTours = tournamentDao.getAllTournamentsOneShot()
                val has11AmJul24 = currentTours.any { it.date == "2026-07-24" && it.time == "11:00" && it.game == "DLS" }
                if (currentTours.isEmpty()) {
                    adminResetTournamentsToInitial()
                } else if (!has11AmJul24) {
                    val tJul24 = Tournament(
                        title = "Dream League Soccer 4-Player Blitz",
                        game = "DLS",
                        entryFee = 0.0,
                        coinEntryFee = 50,
                        prize = 0.0,
                        coinPrize = 300,
                        players = 1,
                        maxPlayers = 4,
                        date = "2026-08-24",
                        time = "11:00",
                        status = "Open",
                        description = "4-Member Dream League Soccer tournament for 11:00 AM, 24th July!",
                        mode = "1 vs 1",
                        map = "Dream Arena",
                        rules = "4-member single elimination bracket",
                        schedule = "Semis: 11:00 GMT\nFinals: 11:20 GMT",
                        prizeDistribution = "1st Place: 200 coins\n2nd Place: 100 coins",
                        contact = "0241113333"
                    )
                    val insertedId = tournamentDao.insertTournament(tJul24)

                }
            } catch (e: Exception) {
                android.util.Log.e("RaivalViewModel", "Error initializing default tournaments: ${e.message}", e)
            }
        }
    }

    fun adminResetTournamentsToInitial() {
        if (currentUser.value?.role != "admin") return
        viewModelScope.launch(Dispatchers.IO) {
            val tournamentDao = database.tournamentDao()
            val registrationDao = database.registrationDao()
            val matchDao = database.tournamentMatchDao()
            val chatMessageDao = database.chatMessageDao()
            val userDao = database.userDao()

            tournamentDao.deleteAllTournaments()
            registrationDao.deleteAllRegistrations()
            matchDao.deleteAllMatches()

            // Re-insert exactly the 10 initial tournaments covering 2, 4, 8, 16, and 32 member capacities
            val t1 = Tournament(
                title = "Dream League Soccer 2-Player Duel",
                game = "DLS",
                entryFee = 0.0,
                coinEntryFee = 50,
                prize = 0.0,
                coinPrize = 100,
                players = 1,
                maxPlayers = 2,
                date = "2026-08-25",
                time = "14:00",
                status = "Open",
                description = "2-Member Instant 1v1 Arena. Winner takes 100 coins!",
                mode = "1 vs 1 Duel",
                map = "Dream Arena",
                rules = "Standard match length\nScreenshot upload required to claim win",
                schedule = "Finals: 14:00 GMT",
                prizeDistribution = "1st Place: 100 coins",
                contact = "0241112222"
            )
            val t1Id = tournamentDao.insertTournament(t1)

            val t2 = Tournament(
                title = "eFootball 2-Member Showdown",
                game = "eFootball",
                entryFee = 0.0,
                coinEntryFee = 25,
                prize = 0.0,
                coinPrize = 50,
                players = 0,
                maxPlayers = 2,
                date = "2026-08-26",
                time = "16:00",
                status = "Open",
                description = "Fast 2-member eFootball 1v1 match.",
                mode = "1 vs 1",
                map = "eFootball Arena",
                rules = "Friendly custom room\nScreenshot submission required",
                schedule = "Finals: 16:00 GMT",
                prizeDistribution = "Winner: 50 Coins",
                contact = "Admin"
            )
            tournamentDao.insertTournament(t2)

            val t3 = Tournament(
                title = "Dream League Soccer 4-Player Blitz",
                game = "DLS",
                entryFee = 0.0,
                coinEntryFee = 100,
                prize = 0.0,
                coinPrize = 400,
                players = 2,
                maxPlayers = 4,
                date = "2026-08-27",
                time = "17:00",
                status = "Open",
                description = "4-Member quick knockout arena. Semi-finals and Grand Final!",
                mode = "1 vs 1",
                map = "Dream Arena",
                rules = "4-member single elimination bracket",
                schedule = "Semis: 17:00 GMT\nFinals: 17:30 GMT",
                prizeDistribution = "1st Place: 300 coins\n2nd Place: 100 coins",
                contact = "0241113333"
            )
            tournamentDao.insertTournament(t3)

            val t4 = Tournament(
                title = "FC Mobile 4-Member Quad Clash",
                game = "FC Mobile",
                entryFee = 0.0,
                coinEntryFee = 50,
                prize = 0.0,
                coinPrize = 200,
                players = 1,
                maxPlayers = 4,
                date = "2026-08-28",
                time = "18:00",
                status = "Open",
                description = "Compact 4-player FC Mobile arena showdown.",
                mode = "1 vs 1",
                map = "Standard Pitch",
                rules = "4-player knockout bracket",
                schedule = "Semis: 18:00 GMT\nFinals: 18:20 GMT",
                prizeDistribution = "1st Place: 150 coins\n2nd Place: 50 coins",
                contact = "0241114444"
            )
            tournamentDao.insertTournament(t4)

            val t5 = Tournament(
                title = "Dream League Soccer Bronze Cup (8 Members)",
                game = "DLS",
                entryFee = 0.0,
                coinEntryFee = 50,
                prize = 0.0,
                coinPrize = 400,
                players = 3,
                maxPlayers = 8,
                date = "2026-08-29",
                time = "18:00",
                status = "Open",
                description = "Compact 8-member 1v1 tier. Perfect for sharpening your defensive formations.",
                mode = "1 vs 1",
                map = "Standard Pitch",
                rules = "Quarter-finals to Finals",
                schedule = "Quarters: 18:00 GMT\nSemis: 18:15 GMT\nFinals: 18:30 GMT",
                prizeDistribution = "1st Place: 250 coins\n2nd Place: 100 coins\n3rd Place: 50 coins",
                contact = "0554443333"
            )
            tournamentDao.insertTournament(t5)

            val t6 = Tournament(
                title = "DLS Premier League (8 Members)",
                game = "DLS",
                entryFee = 0.0,
                coinEntryFee = 150,
                prize = 0.0,
                coinPrize = 1200,
                players = 6,
                maxPlayers = 8,
                date = "2026-08-30",
                time = "14:00",
                status = "Open",
                description = "8-member round robin league table format.",
                mode = "EPL League Table",
                map = "Dream Stadium",
                rules = "Win = 3pts, Draw = 1pt, Loss = 0pt",
                schedule = "Matchdays: 14:00 GMT",
                prizeDistribution = "1st Place: 800 coins\n2nd Place: 400 coins",
                contact = "0241115555"
            )
            tournamentDao.insertTournament(t6)

            val t7 = Tournament(
                title = "Dream League Soccer Silver Cup (16 Members)",
                game = "DLS",
                entryFee = 0.0,
                coinEntryFee = 100,
                prize = 0.0,
                coinPrize = 1000,
                players = 5,
                maxPlayers = 16,
                date = "2026-08-01",
                time = "15:00",
                status = "Open",
                description = "Premier 16-member Dream League Soccer tournament.",
                mode = "1 vs 1",
                map = "Dream Arena",
                rules = "16-player single elimination bracket",
                schedule = "R1: 15:00 GMT\nQuarters: 15:30 GMT\nSemis: 16:00 GMT\nFinals: 16:30 GMT",
                prizeDistribution = "1st Place: 600 coins\n2nd Place: 300 coins\n3rd Place: 100 coins",
                contact = "0241112222"
            )
            tournamentDao.insertTournament(t7)

            val t8 = Tournament(
                title = "FC Mobile Prestige Cup (16 Members)",
                game = "FC Mobile",
                entryFee = 0.0,
                coinEntryFee = 150,
                prize = 0.0,
                coinPrize = 1500,
                players = 4,
                maxPlayers = 16,
                date = "2026-08-02",
                time = "17:00",
                status = "Open",
                description = "FC Mobile 16-member competitive showdown.",
                mode = "1 vs 1",
                map = "Standard Pitch",
                rules = "16-player single elimination bracket",
                schedule = "R1: 17:00 GMT\nQuarters: 17:30 GMT\nSemis: 18:00 GMT\nFinals: 18:30 GMT",
                prizeDistribution = "1st Place: 900 coins\n2nd Place: 450 coins\n3rd Place: 150 coins",
                contact = "0241113333"
            )
            tournamentDao.insertTournament(t8)

            val t9 = Tournament(
                title = "eFootball Ultimate Arena (32 Members)",
                game = "eFootball",
                entryFee = 0.0,
                coinEntryFee = 25,
                prize = 0.0,
                coinPrize = 250,
                players = 0,
                maxPlayers = 32,
                date = "2026-08-03",
                time = "16:00",
                status = "Open",
                description = "Exclusive 32-player invite-only eFootball test tournament.",
                mode = "Friendly custom rooms",
                map = "eFootball Arena",
                rules = "32-player single elimination bracket",
                schedule = "R32: 16:00 GMT\nR16: 16:30 GMT\nQuarters: 17:00 GMT\nSemis: 17:30 GMT\nFinals: 18:00 GMT",
                prizeDistribution = "Winner: 250 Coins",
                contact = "Admin"
            )
            tournamentDao.insertTournament(t9)

            val t10 = Tournament(
                title = "Dream League Soccer Gold Championship (32 Members)",
                game = "DLS",
                entryFee = 0.0,
                coinEntryFee = 200,
                prize = 0.0,
                coinPrize = 2000,
                players = 14,
                maxPlayers = 32,
                date = "2026-08-04",
                time = "14:00",
                status = "Open",
                description = "High tier DLS 32-member championship.",
                mode = "1 vs 1",
                map = "Dream Arena",
                rules = "32-player single elimination bracket",
                schedule = "Knockout Rounds: 14:00 GMT\nFinals: 15:30 GMT",
                prizeDistribution = "1st Place: 1200 coins\n2nd Place: 500 coins\n3rd Place: 300 coins",
                contact = "0240000000"
            )
            tournamentDao.insertTournament(t10)

            val t11 = Tournament(
                title = "Dream League Soccer 4-Player Blitz",
                game = "DLS",
                entryFee = 0.0,
                coinEntryFee = 50,
                prize = 0.0,
                coinPrize = 300,
                players = 1,
                maxPlayers = 4,
                date = "2026-08-24",
                time = "11:00",
                status = "Open",
                description = "4-Member Dream League Soccer tournament for 11:00 AM, 24th July!",
                mode = "1 vs 1",
                map = "Dream Arena",
                rules = "4-member single elimination bracket",
                schedule = "Semis: 11:00 GMT\nFinals: 11:20 GMT",
                prizeDistribution = "1st Place: 200 coins\n2nd Place: 100 coins",
                contact = "0241113333"
            )
            tournamentDao.insertTournament(t11)

            // Also add initial chat message
            val adminId = userDao.getUserByUsername("Admin")?.id ?: 1
            chatMessageDao.insertMessage(
                ChatMessage(
                    chatRoomId = "tournament_${t1Id}",
                    senderId = adminId,
                    senderUsername = "Admin",
                    message = "Let's go! DLS Accra Siege room is live. Play hard, play fair!"
                )
            )

            // Retrieve all newly created tournaments
            try {
                val allTours = tournamentDao.getAllTournamentsOneShot()
                allTours.forEach { tour ->
                                    }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    // LOCAL OFFLINE TESTING & SYNC UTILITIES
    fun exportTournamentToJson(t: Tournament): String {
        return try {
            val obj = org.json.JSONObject().apply {
                put("type", "TOURNAMENT")
                put("id", t.id)
                put("title", t.title)
                put("game", t.game)
                put("entryFee", t.entryFee)
                put("coinEntryFee", t.coinEntryFee)
                put("prize", t.prize)
                put("players", t.players)
                put("maxPlayers", t.maxPlayers)
                put("date", t.date)
                put("time", t.time)
                put("status", t.status)
                put("description", t.description)
                put("mode", t.mode)
                put("map", t.map)
                put("rules", t.rules)
                put("schedule", t.schedule)
                put("prizeDistribution", t.prizeDistribution)
                put("contact", t.contact)
                put("banner", t.banner)
                put("organizer", t.organizer)
            }
            android.util.Base64.encodeToString(obj.toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    fun importTournamentFromJson(base64Str: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val decodedBytes = android.util.Base64.decode(base64Str.trim(), android.util.Base64.NO_WRAP)
                val jsonStr = String(decodedBytes, Charsets.UTF_8)
                val obj = org.json.JSONObject(jsonStr)
                if (obj.optString("type") != "TOURNAMENT") {
                    onResult(false, "Invalid format: Not a tournament code.")
                    return@launch
                }
                val t = Tournament(
                    title = obj.getString("title"),
                    game = obj.optString("game", "DLS"),
                    entryFee = obj.optDouble("entryFee", 10.0),
                    coinEntryFee = obj.optInt("coinEntryFee", 0),
                    prize = obj.optDouble("prize", 150.0),
                    players = 0, // Reset players count for imported tournament on this phone
                    maxPlayers = obj.optInt("maxPlayers", 16),
                    date = obj.getString("date"),
                    time = obj.getString("time"),
                    status = obj.optString("status", "Open"),
                    description = obj.optString("description", ""),
                    mode = obj.optString("mode", "1 vs 1"),
                    map = obj.optString("map", "Dream Arena"),
                    rules = obj.optString("rules", ""),
                    schedule = obj.optString("schedule", ""),
                    prizeDistribution = obj.optString("prizeDistribution", ""),
                    contact = obj.optString("contact", ""),
                    banner = obj.optString("banner", ""),
                    organizer = obj.optString("organizer", "External Admin")
                )
                val id = repository.createTournament(t)
                if (id != -1L) {
                    onResult(true, "Tournament '${t.title}' imported successfully!")
                } else {
                    onResult(false, "Database failed to save imported tournament.")
                }
            } catch (e: Exception) {
                onResult(false, "Error parsing tournament code: ${e.localizedMessage}")
            }
        }
    }

    fun exportRegistration(reg: Registration, user: User): String {
        return try {
            val obj = org.json.JSONObject().apply {
                put("type", "REGISTRATION")
                put("tournamentId", reg.tournamentId)
                put("playerName", reg.playerName)
                put("inGameName", reg.inGameName)
                put("paymentMethod", reg.paymentMethod)
                put("phone", reg.phone)
                put("registeredAt", reg.registeredAt)
                put("teamRating", reg.teamRating)
                put("modelTeam", reg.modelTeam)
                
                val uObj = org.json.JSONObject().apply {
                    put("username", user.username)
                    put("fullName", user.fullName)
                    put("email", user.email)
                    put("phone", user.phone)
                    put("raivalPoints", user.raivalPoints)
                    put("selectedAvatar", user.selectedAvatar)
                }
                put("userProfile", uObj)
            }
            android.util.Base64.encodeToString(obj.toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    fun importRegistration(base64Str: String, targetTournamentId: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val decodedBytes = android.util.Base64.decode(base64Str.trim(), android.util.Base64.NO_WRAP)
                val jsonStr = String(decodedBytes, Charsets.UTF_8)
                val obj = org.json.JSONObject(jsonStr)
                if (obj.optString("type") != "REGISTRATION") {
                    onResult(false, "Invalid format: Not a registration code.")
                    return@launch
                }
                
                val uObj = obj.getJSONObject("userProfile")
                val username = uObj.getString("username")
                val email = uObj.getString("email")
                
                var dbUser = repository.getAllUsersOneShot().find { it.username.equals(username, ignoreCase = true) || it.email.equals(email, ignoreCase = true) }
                val userId = if (dbUser == null) {
                    val newUser = User(
                        username = username,
                        fullName = uObj.optString("fullName", username),
                        email = email,
                        phone = uObj.optString("phone", "0240000000"),
                        raivalPoints = uObj.optInt("raivalPoints", 100),
                        selectedAvatar = uObj.optString("selectedAvatar", "avatar_default"),
                        role = "player"
                    )
                    repository.registerUserForImport(newUser)
                } else {
                    dbUser.id
                }
                
                val reg = Registration(
                    userId = userId,
                    tournamentId = targetTournamentId,
                    playerName = obj.optString("playerName", username),
                    inGameName = obj.optString("inGameName", username),
                    paymentMethod = obj.optString("paymentMethod", "MTN"),
                    phone = obj.optString("phone", "0240000000"),
                    registeredAt = obj.optLong("registeredAt", System.currentTimeMillis()),
                    teamRating = obj.optInt("teamRating", 0),
                    modelTeam = obj.optString("modelTeam", "")
                )
                
                val savedReg = repository.insertRegistrationForImport(reg)
                if (savedReg != -1L) {
                    onResult(true, "Player '$username' successfully registered for this tournament!")
                } else {
                    onResult(false, "Player is already registered or DB write failed.")
                }
            } catch (e: Exception) {
                onResult(false, "Error parsing registration code: ${e.localizedMessage}")
            }
        }
    }

    fun adminToggleBanUser(userId: Int, currentStatus: String) {
        if (currentUser.value?.role != "admin") return
        viewModelScope.launch(Dispatchers.IO) {
            val isBanned = currentStatus == "banned"
            repository.banUser(userId, !isBanned)
            val updatedUser = database.userDao().getUserByIdOneShot(userId)
            if (updatedUser != null) {
                            }
        }
    }

    // Match sessions (Friendly/Matchmaking)
    val matchSessions: StateFlow<List<MatchSession>> = repository.allMatchSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createMatchSession(
        halfLength: Int,
        difficulty: String,
        stadium: String,
        isFriendly: Boolean,
        onResult: (MatchSession) -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = repository.createMatchSession(
                hostId = user.id,
                hostUsername = user.username,
                halfLength = halfLength,
                difficulty = difficulty,
                stadium = stadium,
                isFriendly = isFriendly
            )
            onResult(session)
        }
    }

    fun joinMatchSession(roomCode: String, onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value
        if (user == null) {
            onResult(false, "Please login first.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.joinMatchSession(roomCode, user.id, user.username)
            if (result != null) {
                onResult(true, "Joined room successfully!")
            } else {
                onResult(false, "Invalid room code or lobby is already full/started.")
            }
        }
    }

    fun startMatch(sessionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.confirmMatchStart(sessionId)
        }
    }

    fun submitMatchScore(sessionId: Int, score: Int, screenshot: String? = null) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.submitMatchScore(sessionId, user.id, score, screenshot)
        }
    }

    // Transitional flag for the server-authoritative economy (Phase 3 Edge
    // Functions). When an admin enables serverAuthoritativeEconomy in
    // app_config, the client stops minting coins/XP rewards locally — the
    // server issues them after verifying each event. Default false = current
    // client behavior.
    private fun clientMintingAllowed(): Boolean =
        appConfig.value?.serverAuthoritativeEconomy != true

    fun earnCoins(amount: Int) {
        if (!clientMintingAllowed()) return
        val user = currentUser.value ?: return
        val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
        val finalAmount = if (isCoinOnly) {
            kotlin.math.ceil(amount * 3.0).toInt()
        } else {
            amount * 2 / 3
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.addCoins(user.id, finalAmount)
        }
    }

    fun spendCoins(amount: Int, onResult: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.spendCoins(user.id, amount)
            onResult(success)
        }
    }

    // ─── Real Matchmaking ─────────────────────────────────────────
    fun startRealMatchmaking(
        gameType: String = "eFootball",
        onOpponentFound: (opponentUserId: Int, opponentUsername: String, sessionId: Int) -> Unit,
        onNoOpponent: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val roomCode = PasswordHelper.generateRoomCode()
                val joinResult = SupabaseSyncManager.joinMatchmakingQueue(
                    userId = user.id,
                    username = user.username,
                    roomCode = roomCode,
                    gameType = gameType
                )
                if (joinResult == null) {
                    onError("Failed to join matchmaking queue. Check your connection.")
                    return@launch
                }
                val mySessionId = joinResult["id"]?.jsonPrimitive?.intOrNull ?: 0

                var found = false
                var attempts = 0
                val maxAttempts = 20

                while (!found && attempts < maxAttempts) {
                    delay(3000)
                    attempts++
                    val opponent = SupabaseSyncManager.findWaitingOpponent(
                        excludeUserId = user.id,
                        gameType = gameType
                    )
                    if (opponent != null) {
                        val oppSessionId = opponent["id"]?.jsonPrimitive?.intOrNull ?: 0
                        val oppHostId = opponent["hostid"]?.jsonPrimitive?.intOrNull ?: 0
                        val oppHostUsername = opponent["hostusername"]?.jsonPrimitive?.contentOrNull ?: "Unknown"

                        val claimed = SupabaseSyncManager.claimMatchSession(
                            sessionId = oppSessionId,
                            guestId = user.id,
                            guestUsername = user.username
                        )
                        if (claimed) {
                            SupabaseSyncManager.removeWaitingSession(mySessionId)
                            found = true
                            onOpponentFound(oppHostId, oppHostUsername, oppSessionId)
                        }
                    }
                }
                if (!found) {
                    SupabaseSyncManager.removeWaitingSession(mySessionId)
                    onNoOpponent("No $gameType opponents online right now. Try again later!")
                }
            } catch (e: Exception) {
                onError("Matchmaking error: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Competitive search wrapper: runs the real Supabase matchmaking queue and
     * delivers results on the Main thread (safe for Compose state).
     * Result: Triple(opponentUserId, opponentUsername, sessionId), or an error message.
     */
    fun searchCompetitiveMatch(
        gameType: String,
        onOpponentFound: (opponentUserId: Int, opponentUsername: String, sessionId: Int) -> Unit,
        onNoOpponent: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        startRealMatchmaking(
            gameType = gameType,
            onOpponentFound = { oppId, oppName, sessionId ->
                viewModelScope.launch(Dispatchers.Main) { onOpponentFound(oppId, oppName, sessionId) }
            },
            onNoOpponent = { msg ->
                viewModelScope.launch(Dispatchers.Main) { onNoOpponent(msg) }
            },
            onError = { err ->
                viewModelScope.launch(Dispatchers.Main) { onError(err) }
            }
        )
    }

    fun cancelMatchmaking() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                SupabaseSyncManager.cleanupOldWaitingSessions()
            } catch (_: Exception) {}
        }
    }

    fun record1v1MatchOutcome(
        gameName: String,
        roomCode: String,
        opponentName: String,
        opponentIGN: String,
        entryFeeGhs: Int,
        outcome: String, // "WIN", "LOSS", "DRAW"
        opponentUserId: Int? = null,
        matchSessionId: Int? = null,
        stadium: String? = null,
        difficulty: String? = null,
        hostScore: Int = 0,
        guestScore: Int = 0,
        onComplete: () -> Unit = {}
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val userGhsBalance = user.balance
            val userCoinsBalance = user.coinBalance
            
            val cashDelta = when (outcome) {
                "WIN" -> entryFeeGhs.toDouble()
                "LOSS" -> -entryFeeGhs.toDouble()
                else -> 0.0
            }
            val coinsDelta = when (outcome) {
                "WIN" -> entryFeeGhs * 10
                "LOSS" -> -(entryFeeGhs * 5)
                else -> 0
            }
            
            val updatedUser = user.copy(
                balance = (userGhsBalance + cashDelta).coerceAtLeast(0.0),
                coinBalance = (userCoinsBalance + coinsDelta).coerceAtLeast(0),
                wins = if (outcome == "WIN") user.wins + 1 else user.wins,
                losses = if (outcome == "LOSS") user.losses + 1 else user.losses,
                winStreak = if (outcome == "WIN") user.winStreak + 1 else 0
            )
            database.userDao().updateUser(updatedUser)
            repository.refreshCurrentUser()

            val resolvedStadium = stadium ?: when {
                gameName.contains("DLS", ignoreCase = true) -> "Dream Arena"
                gameName.contains("eFootball", ignoreCase = true) -> "eFootball Stadium"
                gameName.contains("FC", ignoreCase = true) || gameName.contains("FIFA", ignoreCase = true) -> "FC Ultimate Arena"
                else -> "Dream Arena"
            }
            val resolvedDifficulty = difficulty ?: "Professional"

            val session = MatchSession(
                roomCode = roomCode,
                hostId = user.id,
                hostUsername = user.username,
                guestId = opponentUserId,
                guestUsername = opponentName,
                status = "Completed",
                hostScore = hostScore,
                guestScore = guestScore,
                winnerId = if (outcome == "WIN") user.id else if (outcome == "LOSS") opponentUserId else null,
                isFriendly = false,
                stadium = resolvedStadium,
                difficulty = resolvedDifficulty,
                gameType = gameName
            )
            database.matchSessionDao().insertMatchSession(session)
            try {
                SupabaseSyncManager.pushMatchSession(session)
            } catch (_: Exception) {}
            onComplete()
        }
    }

    fun adminResolveDisputedMatch(sessionId: Int, winnerId: Int) {
        if (currentUser.value?.role != "admin") return
        viewModelScope.launch(Dispatchers.IO) {
            repository.adminResolveDispute(sessionId, winnerId)
        }
    }

    fun verifyMatchScreenshots(
        sessionId: Int,
        hostScreenshotUri: Uri?,
        guestScreenshotUri: Uri?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = database.matchSessionDao().getMatchSessionByIdOneShot(sessionId) ?: return@launch
            val context = getApplication<android.app.Application>()
            val verifications = mutableListOf<Map<String, Any>>()

            val hostResult = hostScreenshotUri?.let { uri ->
                try {
                    val pair = GeminiHelper.encodeUriToBase64(context, uri)
                    if (pair == null) null
                    else {
                        val prompt = "Extract the final score from this match screenshot. Return JSON: {\"homeScore\": int, \"awayScore\": int, \"homeTeam\": string, \"awayTeam\": string}"
                        val raw = GeminiHelper.generateVisionResponse(prompt, "You are a match score OCR system.", pair.first, pair.second)
                        val cleaned = GeminiHelper.cleanJsonResponse(raw)
                        val json = org.json.JSONObject(cleaned)
                        mapOf(
                            "type" to "host",
                            "homeScore" to json.optInt("homeScore", 0),
                            "awayScore" to json.optInt("awayScore", 0),
                            "homeTeam" to json.optString("homeTeam", ""),
                            "awayTeam" to json.optString("awayTeam", ""),
                            "raw" to cleaned
                        )
                    }
                } catch (_: Exception) { null }
            }

            val guestResult = guestScreenshotUri?.let { uri ->
                try {
                    val pair = GeminiHelper.encodeUriToBase64(context, uri)
                    if (pair == null) null
                    else {
                        val prompt = "Extract the final score from this match screenshot. Return JSON: {\"homeScore\": int, \"awayScore\": int, \"homeTeam\": string, \"awayTeam\": string}"
                        val raw = GeminiHelper.generateVisionResponse(prompt, "You are a match score OCR system.", pair.first, pair.second)
                        val cleaned = GeminiHelper.cleanJsonResponse(raw)
                        val json = org.json.JSONObject(cleaned)
                        mapOf(
                            "type" to "guest",
                            "homeScore" to json.optInt("homeScore", 0),
                            "awayScore" to json.optInt("awayScore", 0),
                            "homeTeam" to json.optString("homeTeam", ""),
                            "awayTeam" to json.optString("awayTeam", ""),
                            "raw" to cleaned
                        )
                    }
                } catch (_: Exception) { null }
            }

            listOfNotNull(hostResult, guestResult).forEach { verifications.add(it) }

            // Determine winner from Gemini analysis
            val hostScores = hostResult
            val guestScores = guestResult
            if (hostScores != null && guestScores != null) {
                val hostTotal = hostScores["homeScore"] as? Int ?: 0
                val guestTotal = guestScores["awayScore"] as? Int ?: 0
                val geminiWinnerId = when {
                    hostTotal > guestTotal -> session.hostId
                    guestTotal > hostTotal -> session.guestId
                    else -> null
                }
                if (geminiWinnerId != null) {
                    repository.adminResolveDispute(sessionId, geminiWinnerId)
                    verifications.add(mapOf("type" to "auto_resolve", "winnerId" to geminiWinnerId))
                }
            }

            _tournamentVerifications.value = verifications
        }
    }

    private val _tournamentVerifications = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val tournamentVerifications: StateFlow<List<Map<String, Any>>> = _tournamentVerifications.asStateFlow()

    // -------------------------------------------------------------
    // TOURNAMENT BRACKET MANAGEMENT
    // -------------------------------------------------------------
    fun getTournamentMatches(tournamentId: Int): Flow<List<TournamentMatch>> {
        return database.tournamentMatchDao().getMatchesForTournament(tournamentId)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getMatchesForUser(username: String): Flow<List<TournamentMatch>> {
        return flow {
            val user = database.userDao().getUserByUsername(username)
            val registeredNames = if (user != null) {
                database.registrationDao().getRegistrationsForUserOneShot(user.id).map { it.playerName }
            } else {
                emptyList()
            }
            emit(registeredNames)
        }.flatMapLatest { names ->
            database.tournamentMatchDao().getAllMatches().map { list ->
                list.filter { 
                    it.player1Name.equals(username, ignoreCase = true) || 
                    it.player2Name.equals(username, ignoreCase = true) ||
                    names.any { name -> name.equals(it.player1Name, ignoreCase = true) } ||
                    names.any { name -> name.equals(it.player2Name, ignoreCase = true) }
                }
            }
        }
    }

    fun getAllTournamentMatches(): Flow<List<TournamentMatch>> {
        return database.tournamentMatchDao().getAllMatches()
    }

    fun extendTournamentTime(dateStr: String, timeStr: String): Pair<String, String> {
        try {
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].trim().toIntOrNull() ?: 15
                val min = parts[1].trim().toIntOrNull() ?: 0
                val newHour = (hour + 2) % 24
                val newTime = String.format("%02d:%02d", newHour, min)
                if (newHour < hour) {
                    val dateParts = dateStr.split("-")
                    if (dateParts.size == 3) {
                        val y = dateParts[0].toIntOrNull() ?: 2026
                        val m = dateParts[1].toIntOrNull() ?: 7
                        val d = dateParts[2].toIntOrNull() ?: 11
                        val newD = d + 1
                        val newDate = String.format("%04d-%02d-%02d", y, m, newD)
                        return Pair(newDate, newTime)
                    }
                }
                return Pair(dateStr, newTime)
            }
        } catch (e: Exception) {
            // fallback
        }
        return Pair(dateStr, "19:00")
    }

    fun getOffsetDay(dateStr: String, offsetDays: Int): String {
        try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull() ?: 2026
                val m = parts[1].toIntOrNull() ?: 7
                val d = parts[2].toIntOrNull() ?: 11
                val cal = java.util.Calendar.getInstance()
                cal.set(y, m - 1, d)
                cal.add(java.util.Calendar.DAY_OF_YEAR, offsetDays)
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                return format.format(cal.time)
            }
        } catch (e: Exception) {
            // fallback
        }
        return dateStr
    }

    fun generateBracketForTournament(tournamentId: Int, force: Boolean = false, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val matchDao = database.tournamentMatchDao()
            val existing = matchDao.getMatchesForTournamentOneShot(tournamentId)
            if (existing.isNotEmpty()) {
                onComplete()
                return@launch
            }

            val tournament = database.tournamentDao().getTournamentByIdOneShot(tournamentId) ?: return@launch
            val maxPlayers = tournament.maxPlayers

            // Get registered players
            val registrations = database.registrationDao().getRegistrationsForTournamentOneShot(tournamentId)
            val registeredCount = registrations.size

            if (!force && registeredCount < maxPlayers) {
                // Postpone the tournament instead of starting it!
                val currentTitle = tournament.title
                val updatedTitle = if (currentTitle.contains("[POSTPONED]", ignoreCase = true)) {
                    currentTitle
                } else {
                    "🚨 [POSTPONED] $currentTitle"
                }

                val (newDate, newTime) = extendTournamentTime(tournament.date, tournament.time)

                val noticeMessage = "🚨 POSTPONED: The tournament starting time has been extended to $newTime on $newDate to make space for more players to register. Please check back later! 🚨"

                val updatedDescription = if (tournament.description.contains("POSTPONED", ignoreCase = true)) {
                    tournament.description
                } else {
                    "$noticeMessage\n\n${tournament.description}"
                }

                val postponedTour = tournament.copy(
                    title = updatedTitle,
                    time = newTime,
                    date = newDate,
                    description = updatedDescription,
                    status = "Open" // Keep it open for registration
                )

                database.tournamentDao().updateTournament(postponedTour)
                
                // Show a global toast or message
                _tournamentPostponedEvent.value = "⚠️ ${tournament.title} postponed! New starting time is $newTime."
                onComplete()
                return@launch
            }

            val activePlayers = registrations.map { it.playerName }.toMutableList()

            val isEpl = tournament?.title?.contains("EPL", ignoreCase = true) == true || tournament?.title?.contains("Premier League", ignoreCase = true) == true || tournament?.style == "EPL Style"
            val isUcl = tournament?.style == "Champions League Style" || tournament?.title?.contains("UCL", ignoreCase = true) == true || tournament?.title?.contains("Champions League", ignoreCase = true) == true

            // League formats require exact player counts — real players only, no bots
            if (isEpl && activePlayers.size < 8) {
                _tournamentPostponedEvent.value = "⚠️ ${tournament.title} needs ${8 - activePlayers.size} more real players to start (8 required for EPL League)."
                onComplete()
                return@launch
            }
            if (isUcl && activePlayers.size < 36) {
                _tournamentPostponedEvent.value = "⚠️ ${tournament.title} needs ${36 - activePlayers.size} more real players to start (36 required for UCL League)."
                onComplete()
                return@launch
            }

            // No bots — league formats require real players only
            // If not enough real players, bracket generation will postpone

            val matches = mutableListOf<TournamentMatch>()

            // Map active players to their ratings (from registrations or deterministic seed-based values for bots)
            data class PlayerWithRating(val name: String, val rating: Int, val team: String)
            val orderedBotTeams = listOf(
                "Barcelona", "Real Madrid", "Brazil", "Spain", "Liverpool", "Manchester City", "Arsenal", "PSG", "Bayern Munich", "Chelsea"
            )
            var botTeamIdx = 0
            val playersWithRating = activePlayers.map { name ->
                val reg = registrations.find { it.playerName.equals(name, ignoreCase = true) }
                val rating = if (reg != null && reg.teamRating > 0) {
                    reg.teamRating
                } else {
                    // Bot player — use deterministic rating derived from name hash
                    val seed = name.hashCode().toLong()
                    val baseRating = when {
                        tournament.game.contains("FC Mobile", ignoreCase = true) || tournament.game.contains("FC", ignoreCase = true) || tournament.game.contains("FIFA", ignoreCase = true) -> {
                            90 + (seed % 29).toInt() // 90..118
                        }
                        tournament.game.contains("eFootball", ignoreCase = true) || tournament.game.contains("PES", ignoreCase = true) -> {
                            2650 + (seed % 471).toInt() // 2650..3120
                        }
                        else -> { // DLS
                            75 + (seed % 20).toInt() // 75..94
                        }
                    }
                    baseRating
                }
                val team = if (reg != null && reg.modelTeam.isNotEmpty()) {
                    reg.modelTeam
                } else {
                    // Bot player — use ordered team assignment instead of random
                    val assignedTeam = orderedBotTeams[botTeamIdx % orderedBotTeams.size]
                    botTeamIdx++
                    assignedTeam
                }
                PlayerWithRating(name, rating, team)
            }.sortedByDescending { it.rating }

            if (isUcl) {
                // UCL Single League Phase — real players only, no bots
                val uclTeamNames = listOf(
                    "Real Madrid", "Barcelona", "Man City", "Bayern", "PSG", "Juventus", "Arsenal", "Liverpool",
                    "AC Milan", "Dortmund", "Chelsea", "Inter Milan", "Atletico Madrid", "Leverkusen", "Roma", "Napoli",
                    "Aston Villa", "Newcastle", "Sporting CP", "Benfica", "Porto", "Ajax", "PSV", "Feyenoord",
                    "Lazio", "Atalanta", "Girona", "Sociedad", "Monaco", "Lille", "Marseille", "Stuttgart",
                    "Leipzig", "Frankfurt", "Brugge", "Celtic"
                )

                // Map real players with rating (all from registrations, no bots)
                var uclTeamIdx = 0
                val uclPlayersWithRating = activePlayers.map { name ->
                    val reg = registrations.find { it.playerName.equals(name, ignoreCase = true) }
                    val rating = if (reg != null && reg.teamRating > 0) {
                        reg.teamRating
                    } else {
                        // Real player without a rating — assign a default
                        75 + (name.hashCode().toLong() % 10).toInt() // 75..84
                    }
                    val team = if (reg != null && reg.modelTeam.isNotEmpty()) {
                        reg.modelTeam
                    } else {
                        val assigned = uclTeamNames[uclTeamIdx % uclTeamNames.size]
                        uclTeamIdx++
                        assigned
                    }
                    PlayerWithRating(name, rating, team)
                }.sortedByDescending { it.rating }

                // Create Pot-based symmetric pairing:
                val matchesSet = mutableSetOf<Pair<Int, Int>>()
                for (i in 0 until activePlayers.size) {
                    val potOfI = i / maxOf(1, activePlayers.size / 4)
                    // Pot 1 opponents
                    val o1_1 = if (potOfI == 0) (i + 1) % 9 else (i) % 9
                    val o1_2 = if (potOfI == 0) (i + 4) % 9 else (i + 3) % 9
                    
                    // Pot 2 opponents
                    val o2_1 = if (potOfI == 1) 9 + (i - 9 + 1) % 9 else 9 + (i) % 9
                    val o2_2 = if (potOfI == 1) 9 + (i - 9 + 4) % 9 else 9 + (i + 3) % 9
                    
                    // Pot 3 opponents
                    val o3_1 = if (potOfI == 2) 18 + (i - 18 + 1) % 9 else 18 + (i) % 9
                    val o3_2 = if (potOfI == 2) 18 + (i - 18 + 4) % 9 else 18 + (i + 3) % 9
                    
                    // Pot 4 opponents
                    val o4_1 = if (potOfI == 3) 27 + (i - 27 + 1) % 9 else 27 + (i) % 9
                    val o4_2 = if (potOfI == 3) 27 + (i - 27 + 4) % 9 else 27 + (i + 3) % 9
                    
                    val opps = listOf(o1_1, o1_2, o2_1, o2_2, o3_1, o3_2, o4_1, o4_2)
                    for (opp in opps) {
                        if (opp != i) {
                            val pair = if (i < opp) i to opp else opp to i
                            matchesSet.add(pair)
                        }
                    }
                }

                val homeCount = IntArray(activePlayers.size) { 0 }
                val finalPairs = matchesSet.map { (p1, p2) ->
                    val p1Home = if (homeCount[p1] < 4 && (homeCount[p2] >= 4 || (p1 + p2) % 2 == 0)) {
                        homeCount[p1]++
                        true
                    } else {
                        homeCount[p2]++
                        false
                    }
                    if (p1Home) p1 to p2 else p2 to p1
                }

                finalPairs.forEachIndexed { matchIdx, (p1Idx, p2Idx) ->
                    val dayOffset = matchIdx / 4
                    val matchInDay = matchIdx % 4
                    val mDate = getOffsetDay(tournament.date, dayOffset)
                    val mTime = when (matchInDay) {
                        0 -> "14:00"
                        1 -> "16:00"
                        2 -> "18:00"
                        else -> "20:00"
                    }
                    val p1 = uclPlayersWithRating[p1Idx]
                    val p2 = uclPlayersWithRating[p2Idx]
                    
                    matches.add(
                        TournamentMatch(
                            tournamentId = tournamentId,
                            round = "League Match",
                            matchIndex = matchIdx,
                            player1Name = p1.name,
                            player2Name = p2.name,
                            status = "Pending",
                            matchDate = mDate,
                            matchTime = mTime,
                            player1Rating = p1.rating,
                            player2Rating = p2.rating,
                            player1Team = p1.team,
                            player2Team = p2.team
                        )
                    )
                }
            } else if (isEpl) {
                // Generate a true Round-Robin league fixture list for EPL (Circle Method)
                // 8 players all play each other exactly once -> 28 matches, 7 rounds of 4 matches each.
                val eplPlayers = playersWithRating.take(8)
                val listIndices = eplPlayers.indices.toList()
                val tempPlayers = listIndices.toMutableList()
                val n = 8
                var matchIdx = 0
                
                for (round in 0 until 7) {
                    val dayOffset = round / 3
                    val matchInDay = round % 3
                    val mDate = getOffsetDay(tournament.date, dayOffset)
                    val mTime = when (matchInDay) {
                        0 -> "14:00"
                        1 -> "16:30"
                        else -> "19:00"
                    }
                    
                    for (i in 0 until n / 2) {
                        val idx1 = tempPlayers[i]
                        val idx2 = tempPlayers[n - 1 - i]
                        val p1 = eplPlayers[idx1]
                        val p2 = eplPlayers[idx2]
                        
                        matches.add(
                            TournamentMatch(
                                tournamentId = tournamentId,
                                round = "League Match",
                                matchIndex = matchIdx++,
                                player1Name = p1.name,
                                player2Name = p2.name,
                                status = "Pending",
                                matchDate = mDate,
                                matchTime = mTime,
                                player1Rating = p1.rating,
                                player2Rating = p2.rating,
                                player1Team = p1.team,
                                player2Team = p2.team
                            )
                        )
                    }
                    // Rotate indices 1 to n-1
                    val last = tempPlayers.removeAt(tempPlayers.size - 1)
                    tempPlayers.add(1, last)
                }
            } else {
                // Knockout style — use actual registered players only (no bots), byes for non-power-of-2
                val actualCount = activePlayers.size
                val powerOf2 = when {
                    actualCount <= 2 -> 2
                    actualCount <= 4 -> 4
                    actualCount <= 8 -> 8
                    actualCount <= 16 -> 16
                    actualCount <= 32 -> 32
                    else -> 32
                }
                val byes = powerOf2 - actualCount
                // Pad with BYE sentinels, then generate standard power-of-2 bracket
                val bracketPlayers = playersWithRating.map { it } + List(byes) { PlayerWithRating("BYE", 0, "") }

                val firstRoundName = when (powerOf2) {
                    32 -> "Round of 32"
                    16 -> "Round of 16"
                    8 -> "Quarter-finals"
                    4 -> "Semi-finals"
                    else -> "Final"
                }

                if (powerOf2 >= 4) {
                    val roundNames = listOf(firstRoundName, "Quarter-finals", "Semi-finals", "Final")
                    var roundIdx = 0
                    var matchCount = powerOf2 / 2

                    while (matchCount >= 1) {
                        val roundLabel = roundNames.getOrElse(roundIdx) { "Round ${roundIdx + 1}" }
                        val isFirstRound = roundIdx == 0

                        for (i in 0 until matchCount) {
                            if (isFirstRound) {
                                val p1 = bracketPlayers[i]
                                val p2 = bracketPlayers[matchCount * 2 - 1 - i]
                                val isBye = p1.name == "BYE" || p2.name == "BYE"
                                matches.add(TournamentMatch(
                                    tournamentId = tournamentId,
                                    round = firstRoundName, matchIndex = i,
                                    player1Name = p1.name, player2Name = p2.name,
                                    status = if (isBye) "Completed" else "Pending",
                                    winnerName = if (p1.name == "BYE") p2.name else if (p2.name == "BYE") p1.name else null,
                                    matchDate = tournament.date,
                                    matchTime = String.format("%02d:%02d", 14 + (i / 2), (i % 2) * 30),
                                    player1Rating = p1.rating, player2Rating = p2.rating,
                                    player1Team = p1.team, player2Team = p2.team
                                ))
                            } else {
                                matches.add(TournamentMatch(
                                    tournamentId = tournamentId,
                                    round = roundLabel, matchIndex = i,
                                    player1Name = "TBA", player2Name = "TBA",
                                    status = "Pending",
                                    matchDate = tournament.date,
                                    matchTime = String.format("%02d:%02d", 14 + (i / 2), (i % 2) * 30),
                                    player1Rating = 0, player2Rating = 0,
                                    player1Team = "", player2Team = ""
                                ))
                            }
                        }

                        matchCount /= 2
                        roundIdx++
                    }
                } else {
                    // powerOf2 == 2: just a single final
                    if (bracketPlayers.size >= 2) {
                        matches.add(TournamentMatch(
                            tournamentId = tournamentId,
                            round = "Final", matchIndex = 0,
                            player1Name = bracketPlayers[0].name, player2Name = bracketPlayers[1].name,
                            status = if (bracketPlayers[0].name == "BYE" || bracketPlayers[1].name == "BYE") "Completed" else "Pending",
                            winnerName = if (bracketPlayers[0].name == "BYE") bracketPlayers[1].name else if (bracketPlayers[1].name == "BYE") bracketPlayers[0].name else null,
                            matchDate = tournament.date, matchTime = "21:00",
                            player1Rating = bracketPlayers[0].rating, player2Rating = bracketPlayers[1].rating,
                            player1Team = bracketPlayers[0].team, player2Team = bracketPlayers[1].team
                        ))
                    }
                }
            }

            // Advance BYE match winners to next round slots
            for (i in matches.indices) {
                val m = matches[i]
                if (m.status == "Completed" && m.winnerName != null && m.winnerName != "TBA") {
                    val nextRound = when (m.round) {
                        "Round of 32" -> "Round of 16"
                        "Round of 16" -> "Quarter-finals"
                        "Quarter-finals" -> "Semi-finals"
                        "Semi-finals" -> "Final"
                        else -> null
                    }
                    if (nextRound != null) {
                        val nextMatchIndex = m.matchIndex / 2
                        val isPlayer1 = m.matchIndex % 2 == 0
                        for (j in matches.indices) {
                            val nm = matches[j]
                            if (nm.round == nextRound && nm.matchIndex == nextMatchIndex) {
                                val updated = if (isPlayer1) nm.copy(player1Name = m.winnerName)
                                else nm.copy(player2Name = m.winnerName)
                                matches[j] = updated
                                break
                            }
                        }
                    }
                }
            }

            // Format-aware scheduling to ensure players play up to 3 matches a day
            val scheduledMatches = if (isEpl) {
                // For EPL, matches are already generated and ordered by rounds (7 rounds of 4 matches each).
                // Let's schedule 4 matches of each round to the same time, with 3 rounds per day.
                matches.mapIndexed { idx, match ->
                    val roundIndex = idx / 4 // 0 to 6
                    val dayOffset = roundIndex / 3 // Round 0..2 -> Day 0; Round 3..5 -> Day 1; Round 6 -> Day 2
                    val matchInDay = roundIndex % 3 // Round index in day: 0, 1, 2
                    val mDate = getOffsetDay(tournament.date, dayOffset)
                    val mTime = when (matchInDay) {
                        0 -> "14:00"
                        1 -> "16:30"
                        else -> "19:00"
                    }
                    match.copy(matchDate = mDate, matchTime = mTime)
                }
            } else {
                // Knockout style (UCL / WC / Default)
                // Schedule matches round-by-round to pack up to 3 rounds in a day.
                val uniqueRounds = matches.map { it.round }.distinct()
                val roundSchedules = mutableMapOf<String, Pair<Int, String>>()
                uniqueRounds.forEachIndexed { rIdx, roundName ->
                    val rDayOffset = rIdx / 3
                    val rSlotIdx = rIdx % 3
                    val rTime = when (rSlotIdx) {
                        0 -> "14:00"
                        1 -> "16:30"
                        else -> "19:00"
                    }
                    roundSchedules[roundName] = Pair(rDayOffset, rTime)
                }
                
                matches.map { match ->
                    val (rDayOffset, rTime) = roundSchedules[match.round] ?: Pair(0, "14:00")
                    val mDate = getOffsetDay(tournament.date, rDayOffset)
                    match.copy(matchDate = mDate, matchTime = rTime)
                }
            }

            matchDao.insertTournamentMatches(scheduledMatches)
            onComplete()
        }
    }

    fun updateMatchCode(matchId: Int, code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val matchDao = database.tournamentMatchDao()
            val match = (matchDao.getAllMatches().firstOrNull() ?: emptyList()).find { it.id == matchId } ?: return@launch
            val updated = match.copy(matchCode = code)
            matchDao.updateTournamentMatch(updated)
            // Send system message in chat
            repository.sendChatMessage("match_chat_$matchId", "🔑 Match Lobby Code shared by Host: **$code** (Copy it now to join the lobby!)")
        }
    }

    fun checkInPlayer(matchId: Int, isPlayer1: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val matchDao = database.tournamentMatchDao()
            val match = (matchDao.getAllMatches().firstOrNull() ?: emptyList()).find { it.id == matchId } ?: return@launch
            val updated = if (isPlayer1) {
                match.copy(player1CheckedIn = true)
            } else {
                match.copy(player2CheckedIn = true)
            }
            matchDao.updateTournamentMatch(updated)
            val name = if (isPlayer1) match.player1Name else match.player2Name
            repository.sendChatMessage("match_chat_$matchId", "✅ **$name** checked in successfully!")
        }
    }

    fun disqualifyPlayer(matchId: Int, isPlayer1: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val matchDao = database.tournamentMatchDao()
            val match = (matchDao.getAllMatches().firstOrNull() ?: emptyList()).find { it.id == matchId } ?: return@launch
            if (match.status == "Completed") return@launch
            
            val updated = if (isPlayer1) {
                match.copy(player1Disqualified = true, player1CheckedIn = false)
            } else {
                match.copy(player2Disqualified = true, player2CheckedIn = false)
            }
            matchDao.updateTournamentMatch(updated)
            
            val dqName = if (isPlayer1) match.player1Name else match.player2Name
            val winName = if (isPlayer1) match.player2Name else match.player1Name
            
            repository.sendChatMessage("match_chat_$matchId", "🚨 **DISQUALIFIED**: **$dqName** failed to check in within 5 minutes of tournament start and has been automatically disqualified. **$winName** wins by forfeit!")
            
            // Auto submit score for forfeit
            val p1Score = if (isPlayer1) 0 else 3
            val p2Score = if (isPlayer1) 3 else 0
            submitBracketMatchScore(updated, p1Score, p2Score)
        }
    }

    // ─── Pro License (real qualifier) ───────────────────────────────
    // A license is only granted for a VERIFIED completed 1v1 win recorded in
    // match_sessions (local + Supabase) — never by chance. The license is a
    // server-synced expiry timestamp, not a local UI flag.
    fun isProLicensed(user: User): Boolean =
        user.proLicenseExpiresAt > System.currentTimeMillis()

    fun claimProQualifierLicense(onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value ?: return
        if (isProLicensed(user)) {
            onResult(true, "Pro License already active until ${java.util.Date(user.proLicenseExpiresAt)}.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val hasVerifiedWin = try {
                val localWins = database.matchSessionDao().getCompletedWinsForUser(user.id)
                if (localWins.isNotEmpty()) true else {
                    // Fall back to server truth when local DB hasn't synced yet
                    SupabaseSyncManager.pullTable("match_sessions").any {
                        (it["winnerId"]?.jsonPrimitive?.intOrNull
                            ?: it["winnerid"]?.jsonPrimitive?.intOrNull) == user.id &&
                        (it["status"]?.jsonPrimitive?.contentOrNull == "Completed")
                    }
                }
            } catch (_: Exception) { false }
            if (!hasVerifiedWin) {
                onResult(false, "No verified 1v1 win found. Win a real match first, then claim your license.")
                return@launch
            }
            val updated = user.copy(
                proLicenseExpiresAt = System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000,
                xp = user.xp + 500
            )
            repository.saveUserProgress(updated)
            try {
                SupabaseSyncManager.pushUpdate("users", updated.id, SupabaseSyncManager.toJson(updated))
            } catch (_: Exception) { }
            onResult(true, "🎉 Qualifier cleared with a verified win! 14-day Pro License activated.")
        }
    }

    fun initializeLobby(matchId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val matchDao = database.tournamentMatchDao()
            val match = (matchDao.getAllMatches().firstOrNull() ?: emptyList()).find { it.id == matchId } ?: return@launch
            if (match.checkInDeadline == 0L) {
                // Calculate deadlines based on tournament scheduled start time
                val tournament = database.tournamentDao().getTournamentByIdOneShot(match.tournamentId)
                val (checkInDeadline, matchDeadline) = calculateDeadlinesFromTournamentStart(tournament)
                
                // Fetch the tournament to check the game
                val isDls = tournament?.game?.contains("DLS", ignoreCase = true) == true
                
                // Auto-generate match code for DLS if empty
                val autoCode = if (isDls && match.matchCode.isEmpty()) {
                    "SKR${100 + (matchId % 900)}"
                } else {
                    match.matchCode
                }
                
                val updated = match.copy(
                    checkInDeadline = checkInDeadline, 
                    matchDeadline = matchDeadline,
                    matchCode = autoCode
                )
                matchDao.updateTournamentMatch(updated)
                
                // Seed some welcoming system messages
                repository.sendChatMessage("match_chat_$matchId", "👋 Welcome to your dedicated Match Chat for **${match.player1Name}** vs **${match.player2Name}**!")
                repository.sendChatMessage("match_chat_$matchId", "⏱️ **5-MINUTE CHECK-IN**: You must check in within 5 minutes of tournament start (${formatTime(tournament?.date, tournament?.time)}) or be **auto-disqualified**.")
                if (isDls) {
                    repository.sendChatMessage("match_chat_$matchId", "🏠🔑 **DLS AUTOMATIC MATCHMAKING**: Friendly code **$autoCode** generated. Both players enter this in DLS Mobile -> Friendly Match.")
                } else {
                    repository.sendChatMessage("match_chat_$matchId", "🏠 **HOME PLAYER (${match.player1Name})**: Set the Match Lobby Code so your opponent can join.")
                }
                repository.sendChatMessage("match_chat_$matchId", "🕒 **17-MINUTE TOTAL WINDOW**: 15 minutes for match + 2 minutes grace to submit scores from tournament start.")
            } else {
                // If already initialized but matchCode is empty and it is DLS, let's auto-generate it
                val tournament = database.tournamentDao().getTournamentByIdOneShot(match.tournamentId)
                val isDls = tournament?.game?.contains("DLS", ignoreCase = true) == true
                if (isDls && match.matchCode.isEmpty()) {
                    val autoCode = "SKR${100 + (matchId % 900)}"
                    val updated = match.copy(matchCode = autoCode)
                    matchDao.updateTournamentMatch(updated)
                    repository.sendChatMessage("match_chat_$matchId", "🔑 Auto-generated DLS Friendly Match Code: **$autoCode**")
                }
            }
        }
    }
    
    private fun calculateDeadlinesFromTournamentStart(tournament: Tournament?): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val tournamentStartMillis = tournament?.let { tour ->
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                sdf.parse("${tour.date} ${tour.time}")?.time ?: now
            } catch (e: Exception) {
                now
            }
        } ?: now
        
        // If tournament already started, use current time as base
        val baseTime = if (tournamentStartMillis > now) tournamentStartMillis else now
        
        // 5 minutes for check-in from tournament start
        val checkInDeadline = baseTime + 5 * 60 * 1000
        
        // 17 minutes total match window from tournament start (15 min match + 2 min grace for score submission)
        val matchDeadline = baseTime + 17 * 60 * 1000
        
return Pair(checkInDeadline, matchDeadline)
    }
    
    private fun formatTime(date: String?, time: String?): String {
        return "${date ?: "TBD"} @ ${time ?: "TBD"}"
    }
    
    fun autoResolveMatchOnTimeout(matchId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val matchDao = database.tournamentMatchDao()
            val match = (matchDao.getAllMatches().firstOrNull() ?: emptyList()).find { it.id == matchId } ?: return@launch
            if (match.status == "Completed") return@launch
            
            // Check who checked in — if only one did, they win by forfeit
            if (match.player1CheckedIn && !match.player2CheckedIn) {
                disqualifyPlayer(match.id, false)
            } else if (match.player2CheckedIn && !match.player1CheckedIn) {
                disqualifyPlayer(match.id, true)
            } else if (match.player1CheckedIn && match.player2CheckedIn) {
                // Both checked in but no scores submitted — mark as completed with no winner
                repository.sendChatMessage("match_chat_$matchId", "🚨 **17-MINUTE WINDOW EXPIRED**: Neither player submitted scores. Match marked as completed, scores to be recorded later.")
                val updated = match.copy(
                    player1Score = 0, player2Score = 0,
                    status = "Completed", winnerName = "TBA"
                )
                matchDao.updateTournamentMatch(updated)
                try { SupabaseSyncManager.pushUpdate("tournament_matches", updated.id, SupabaseSyncManager.toJson(updated)) } catch (_: Exception) { }
            } else {
                // Neither checked in — double forfeit, advance TBA
                val updated = match.copy(
                    player1Score = 0, player2Score = 0,
                    status = "Completed", winnerName = "TBA"
                )
                matchDao.updateTournamentMatch(updated)
                repository.sendChatMessage("match_chat_$matchId", "🚨 **DOUBLE FORFEIT**: Neither player showed up. This match slot will advance as TBA.")
            }
        }
    }

    fun updateLiveMatchScore(
        match: TournamentMatch,
        player1Score: Int,
        player2Score: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val matchDao = database.tournamentMatchDao()
            val updatedMatch = match.copy(
                player1Score = player1Score,
                player2Score = player2Score
            )
            matchDao.updateTournamentMatch(updatedMatch)
        }
    }

    fun submitBracketMatchScore(
        match: TournamentMatch,
        player1Score: Int,
        player2Score: Int,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val matchDao = database.tournamentMatchDao()
            val winner = if (player1Score > player2Score) match.player1Name else match.player2Name

            // 1. Update current match status and score
            val updatedMatch = match.copy(
                player1Score = player1Score,
                player2Score = player2Score,
                status = "Completed",
                winnerName = winner
            )
            matchDao.updateTournamentMatch(updatedMatch)

            // 2. Post-match coin distribution logic for participation bonus
            val userDao = database.userDao()
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val baseBonus = 25
            val finalBonus = if (isCoinOnly) baseBonus * 3 else baseBonus * 2 / 3

            val player1 = userDao.getUserByUsername(match.player1Name)
            val player2 = userDao.getUserByUsername(match.player2Name)

            player1?.let { p1 ->
                val updatedP1 = p1.copy(coinBalance = p1.coinBalance + finalBonus)
                if (currentUser.value?.id == p1.id) {
                    repository.saveUserProgress(updatedP1)
                } else {
                    userDao.updateUser(updatedP1)
                }
            }

            player2?.let { p2 ->
                val updatedP2 = p2.copy(coinBalance = p2.coinBalance + finalBonus)
                if (currentUser.value?.id == p2.id) {
                    repository.saveUserProgress(updatedP2)
                } else {
                    userDao.updateUser(updatedP2)
                }
            }

            // 3. Advance winner to the next round or award prize if it's the final match
            val tournamentDao = database.tournamentDao()
            val tournament = tournamentDao.getTournamentByIdOneShot(match.tournamentId)
            val isUcl = tournament?.style == "Champions League Style" || tournament?.title?.contains("UCL", ignoreCase = true) == true || tournament?.title?.contains("Champions League", ignoreCase = true) == true

            if (isUcl) {
                // UCL progression state machine
                val allMatches = matchDao.getMatchesForTournamentOneShot(match.tournamentId)
                
                when (match.round) {
                    "League Match" -> {
                        // Check if all league matches are completed
                        val leagueMatches = allMatches.filter { it.round == "League Match" }
                        val allCompleted = leagueMatches.all { it.status == "Completed" || it.id == match.id }
                        if (allCompleted) {
                            // Compute Standings
                            val table = mutableMapOf<String, LeagueRow>()
                            leagueMatches.forEach { m ->
                                val p1 = m.player1Name
                                val p2 = m.player2Name
                                val s1 = if (m.id == match.id) player1Score else (m.player1Score ?: 0)
                                val s2 = if (m.id == match.id) player2Score else (m.player2Score ?: 0)
                                
                                val r1 = table[p1] ?: LeagueRow(p1).also { table[p1] = it }
                                val r2 = table[p2] ?: LeagueRow(p2).also { table[p2] = it }
                                
                                r1.played++
                                r2.played++
                                r1.gf += s1
                                r1.ga += s2
                                r2.gf += s2
                                r2.ga += s1
                                
                                if (s1 > s2) {
                                    r1.won++
                                    r2.lost++
                                } else if (s2 > s1) {
                                    r2.won++
                                    r1.lost++
                                } else {
                                    r1.drawn++
                                    r2.drawn++
                                }
                            }
                            
                            val sortedStandings = table.values.sortedWith(
                                compareByDescending<LeagueRow> { it.pts }
                                    .thenByDescending { it.gd }
                                    .thenByDescending { it.gf }
                                    .thenBy { it.team }
                            )
                            
                            // Rank 1-8: Advance directly to Round of 16
                            // Rank 9-16: Seeded in play-off draw (indices 8..15)
                            // Rank 17-24: Unseeded in play-off draw (indices 16..23)
                            val seeded = sortedStandings.subList(8, 16)
                            val unseeded = sortedStandings.subList(16, 24)
                            
                            val playOffMatches = mutableListOf<TournamentMatch>()
                            // Create 8 play-off pairings, 2 legs each (Leg 1, Leg 2)
                            for (i in 0 until 8) {
                                val sTeam = seeded[i].team
                                val uTeam = unseeded[7 - i].team // Seed 9 vs Seed 24, etc.
                                
                                val sUser = userDao.getUserByUsername(sTeam)
                                val uUser = userDao.getUserByUsername(uTeam)
                                val sRating = sUser?.coinBalance?.rem(20)?.plus(85) ?: 88
                                val uRating = uUser?.coinBalance?.rem(20)?.plus(85) ?: 85
                                
                                // Leg 1: Unseeded Home
                                playOffMatches.add(
                                    TournamentMatch(
                                        tournamentId = match.tournamentId,
                                        round = "Play-offs Leg 1",
                                        matchIndex = i,
                                        player1Name = uTeam,
                                        player2Name = sTeam,
                                        status = "Pending",
                                        matchDate = getOffsetDay(tournament.date, 36),
                                        matchTime = "15:00",
                                        player1Rating = uRating,
                                        player2Rating = sRating,
                                        player1Team = uTeam.replace("Gamer_", ""),
                                        player2Team = sTeam.replace("Gamer_", "")
                                    )
                                )
                                // Leg 2: Seeded Home
                                playOffMatches.add(
                                    TournamentMatch(
                                        tournamentId = match.tournamentId,
                                        round = "Play-offs Leg 2",
                                        matchIndex = i,
                                        player1Name = sTeam,
                                        player2Name = uTeam,
                                        status = "Pending",
                                        matchDate = getOffsetDay(tournament.date, 36),
                                        matchTime = "19:00",
                                        player1Rating = sRating,
                                        player2Rating = uRating,
                                        player1Team = sTeam.replace("Gamer_", ""),
                                        player2Team = uTeam.replace("Gamer_", "")
                                    )
                                )
                            }
                            matchDao.insertTournamentMatches(playOffMatches)
                            repository.sendChatMessage("match_chat_${match.id}", "🏆 **UCL LEAGUE PHASE COMPLETE!** The 36-team single league phase is over. Top 8 clubs have advanced directly to the Round of 16. Clubs ranked 9-24 have been paired into the high-stakes double-legged **Knockout Play-offs** starting tomorrow!")
                        }
                    }
                    "Play-offs Leg 1", "Play-offs Leg 2" -> {
                        // Check if all 16 play-off matches are completed
                        val poMatches = allMatches.filter { it.round == "Play-offs Leg 1" || it.round == "Play-offs Leg 2" }
                        val allCompleted = poMatches.all { it.status == "Completed" || it.id == match.id }
                        if (allCompleted) {
                            // Calculate aggregate scores for the 8 play-off matchups
                            val winners = mutableListOf<String>()
                            val winnerRatings = mutableMapOf<String, Int>()
                            val winnerTeams = mutableMapOf<String, String>()
                            
                            for (i in 0 until 8) {
                                val leg1 = poMatches.find { it.round == "Play-offs Leg 1" && it.matchIndex == i }
                                val leg2 = poMatches.find { it.round == "Play-offs Leg 2" && it.matchIndex == i }
                                if (leg1 == null || leg2 == null) continue
                                
                                val p1_1 = leg1.player1Name // Unseeded
                                val p2_1 = leg1.player2Name // Seeded
                                val s1_1 = if (leg1.id == match.id) player1Score else (leg1.player1Score ?: 0)
                                val s2_1 = if (leg1.id == match.id) player2Score else (leg1.player2Score ?: 0)
                                
                                val p1_2 = leg2.player1Name // Seeded
                                val p2_2 = leg2.player2Name // Unseeded
                                val s1_2 = if (leg2.id == match.id) player1Score else (leg2.player1Score ?: 0)
                                val s2_2 = if (leg2.id == match.id) player2Score else (leg2.player2Score ?: 0)
                                
                                // Aggregate
                                val unseededTotal = s1_1 + s2_2
                                val seededTotal = s2_1 + s1_2
                                
                                val roundWinner = if (seededTotal > unseededTotal) p1_2 else if (unseededTotal > seededTotal) p1_1 else p1_2 // Seeded wins aggregate tie by default
                                winners.add(roundWinner)
                                val wRating = if (roundWinner == p1_2) leg2.player1Rating else leg2.player2Rating
                                val wTeam = if (roundWinner == p1_2) leg2.player1Team else leg2.player2Team
                                winnerRatings[roundWinner] = wRating
                                winnerTeams[roundWinner] = wTeam
                            }
                            
                            // Get League Standings to fetch the top 8
                            val leagueMatches = allMatches.filter { it.round == "League Match" }
                            val table = mutableMapOf<String, LeagueRow>()
                            leagueMatches.forEach { m ->
                                val r1 = table[m.player1Name] ?: LeagueRow(m.player1Name).also { table[m.player1Name] = it }
                                val r2 = table[m.player2Name] ?: LeagueRow(m.player2Name).also { table[m.player2Name] = it }
                                r1.played++; r2.played++
                                r1.gf += m.player1Score ?: 0; r1.ga += m.player2Score ?: 0
                                r2.gf += m.player2Score ?: 0; r2.ga += m.player1Score ?: 0
                                if ((m.player1Score ?: 0) > (m.player2Score ?: 0)) { r1.won++; r2.lost++ }
                                else if ((m.player2Score ?: 0) > (m.player1Score ?: 0)) { r2.won++; r1.lost++ }
                                else { r1.drawn++; r2.drawn++ }
                            }
                            val sortedStandings = table.values.sortedWith(
                                compareByDescending<LeagueRow> { it.pts }.thenByDescending { it.gd }.thenByDescending { it.gf }
                            )
                            val top8 = sortedStandings.take(8).map { it.team }
                            
                            // Generate Round of 16 (8 matches) and Quarter-finals (4 matches, TBA) on Day 38!
                            val r16Matches = mutableListOf<TournamentMatch>()
                            for (i in 0 until 8) {
                                val tTeam = top8[i]
                                val pTeam = winners[7 - i]
                                
                                val tUser = userDao.getUserByUsername(tTeam)
                                val tRating = tUser?.coinBalance?.rem(20)?.plus(85) ?: 90
                                val pRating = winnerRatings[pTeam] ?: 88
                                
                                r16Matches.add(
                                    TournamentMatch(
                                        tournamentId = match.tournamentId,
                                        round = "Round of 16",
                                        matchIndex = i,
                                        player1Name = tTeam,
                                        player2Name = pTeam,
                                        status = "Pending",
                                        matchDate = getOffsetDay(tournament.date, 37),
                                        matchTime = "14:00",
                                        player1Rating = tRating,
                                        player2Rating = pRating,
                                        player1Team = tTeam.replace("Gamer_", ""),
                                        player2Team = pTeam.replace("Gamer_", "")
                                    )
                                )
                            }
                            
                            // Generate Quarter-finals (4 matches, TBA) on the same day as Round of 16 (Day 38, "18:00")
                            for (i in 0 until 4) {
                                r16Matches.add(
                                    TournamentMatch(
                                        tournamentId = match.tournamentId,
                                        round = "Quarter-finals",
                                        matchIndex = i,
                                        player1Name = "TBA",
                                        player2Name = "TBA",
                                        status = "Pending",
                                        matchDate = getOffsetDay(tournament.date, 37),
                                        matchTime = "18:00",
                                        player1Rating = 0,
                                        player2Rating = 0,
                                        player1Team = "",
                                        player2Team = ""
                                    )
                                )
                            }
                            matchDao.insertTournamentMatches(r16Matches)
                            repository.sendChatMessage("match_chat_${match.id}", "🔥 **PLAY-OFFS COMPLETE!** The 8 play-off winners have been determined. They will now face the top 8 seeded clubs in the **Round of 16** starting tomorrow, with the **Quarter-finals** played on the same evening!")
                        }
                    }
                    "Round of 16" -> {
                        // Advance winner to Quarter-finals (matchIndex = match.matchIndex / 2)
                        val qfIndex = match.matchIndex / 2
                        val isPlayer1 = match.matchIndex % 2 == 0
                        
                        val qfMatch = allMatches.find { it.round == "Quarter-finals" && it.matchIndex == qfIndex }
                        if (qfMatch != null) {
                            val winnerRating = if (winner == match.player1Name) match.player1Rating else match.player2Rating
                            val winnerTeam = if (winner == match.player1Name) match.player1Team else match.player2Team
                            val updatedQF = if (isPlayer1) {
                                qfMatch.copy(player1Name = winner, player1Rating = winnerRating, player1Team = winnerTeam)
                            } else {
                                qfMatch.copy(player2Name = winner, player2Rating = winnerRating, player2Team = winnerTeam)
                            }
                            matchDao.updateTournamentMatch(updatedQF)
                        }
                    }
                    "Quarter-finals" -> {
                        // Check if all Quarter-finals are completed
                        val qfMatches = allMatches.filter { it.round == "Quarter-finals" }
                        val allCompleted = qfMatches.all { it.status == "Completed" || it.id == match.id }
                        if (allCompleted) {
                            // Create Semi-finals (2 matches) on Day 39
                            val sfMatches = mutableListOf<TournamentMatch>()
                            for (i in 0 until 2) {
                                sfMatches.add(
                                    TournamentMatch(
                                        tournamentId = match.tournamentId,
                                        round = "Semi-finals",
                                        matchIndex = i,
                                        player1Name = "TBA",
                                        player2Name = "TBA",
                                        status = "Pending",
                                        matchDate = getOffsetDay(tournament.date, 38),
                                        matchTime = if (i == 0) "15:00" else "19:00",
                                        player1Rating = 0,
                                        player2Rating = 0,
                                        player1Team = "",
                                        player2Team = ""
                                    )
                                )
                            }
                            // Populate Semi-finals winners from Quarter-finals!
                            val winnersList = qfMatches.map { if (it.id == match.id) winner else it.winnerName }.map { it ?: "TBA" }
                            if (winnersList.size >= 4) {
                                val sf1_1 = winnersList[0]
                                val sf1_2 = winnersList[1]
                                val sf2_1 = winnersList[2]
                                val sf2_2 = winnersList[3]
                                
                                val m0 = if (sfMatches.size >= 1) sfMatches[0].copy(
                                    player1Name = sf1_1, player2Name = sf1_2,
                                    player1Rating = 90, player2Rating = 90,
                                    player1Team = sf1_1.replace("Gamer_", ""), player2Team = sf1_2.replace("Gamer_", "")
                                ) else TournamentMatch(
                                    tournamentId = match.tournamentId,
                                    round = "Semi-finals", matchIndex = 0,
                                    player1Name = sf1_1, player2Name = sf1_2,
                                    status = "Pending",
                                    matchDate = getOffsetDay(tournament.date, 20),
                                    matchTime = "19:00",
                                    player1Rating = 90, player2Rating = 90,
                                    player1Team = sf1_1.replace("Gamer_", ""), player2Team = sf1_2.replace("Gamer_", "")
                                )
                                val m1 = if (sfMatches.size >= 2) sfMatches[1].copy(
                                    player1Name = sf2_1, player2Name = sf2_2,
                                    player1Rating = 90, player2Rating = 90,
                                    player1Team = sf2_1.replace("Gamer_", ""), player2Team = sf2_2.replace("Gamer_", "")
                                ) else TournamentMatch(
                                    tournamentId = match.tournamentId,
                                    round = "Semi-finals", matchIndex = 1,
                                    player1Name = sf2_1, player2Name = sf2_2,
                                    status = "Pending",
                                    matchDate = getOffsetDay(tournament.date, 20),
                                    matchTime = "21:00",
                                    player1Rating = 90, player2Rating = 90,
                                    player1Team = sf2_1.replace("Gamer_", ""), player2Team = sf2_2.replace("Gamer_", "")
                                )
                                matchDao.insertTournamentMatches(listOf(m0, m1))
                            } else {
                                matchDao.insertTournamentMatches(sfMatches)
                            }
                            repository.sendChatMessage("match_chat_${match.id}", "⚡ **QUARTER-FINALS COMPLETE!** The semi-finalists are set. The high-intensity **Semi-finals** will kick off tomorrow!")
                        }
                    }
                    "Semi-finals" -> {
                        // Check if all Semi-finals are completed
                        val sfMatches = allMatches.filter { it.round == "Semi-finals" }
                        val allCompleted = sfMatches.all { it.status == "Completed" || it.id == match.id }
                        if (allCompleted) {
                            val finalists = sfMatches.map { if (it.id == match.id) winner else it.winnerName }.map { it ?: "TBA" }
                            if (finalists.size >= 2) {
                                val fMatch = TournamentMatch(
                                    tournamentId = match.tournamentId,
                                    round = "Final",
                                    matchIndex = 0,
                                    player1Name = finalists[0],
                                    player2Name = finalists[1],
                                    status = "Pending",
                                    matchDate = getOffsetDay(tournament.date, 39),
                                    matchTime = "21:00",
                                    player1Rating = 92,
                                    player2Rating = 92,
                                    player1Team = finalists[0].replace("Gamer_", ""),
                                    player2Team = finalists[1].replace("Gamer_", "")
                                )
                                matchDao.insertTournamentMatches(listOf(fMatch))
                                repository.sendChatMessage("match_chat_${match.id}", "🏆 **SEMI-FINALS COMPLETE!** We have our two grand finalists: **${finalists[0]}** vs **${finalists[1]}**. The spectacular UCL Grand Final in Budapest will kick off tomorrow!")
                            }
                        }
                    }
                    "Final" -> {
                        // Award tournament prizes (GHS Cash & DLS Coins) to the champion automatically!
                        if (tournament != null) {
                            val winnerUser = userDao.getUserByUsername(winner)
                            if (winnerUser != null) {
                                val finalWinnerUser = winnerUser.copy(
                                    balance = winnerUser.balance + tournament.prize,
                                    coinBalance = winnerUser.coinBalance + tournament.coinPrize,
                                    totalWinnings = winnerUser.totalWinnings + tournament.prize,
                                    wins = winnerUser.wins + 1
                                )
                                if (currentUser.value?.id == finalWinnerUser.id) {
                                    repository.saveUserProgress(finalWinnerUser)
                                } else {
                                    userDao.updateUser(finalWinnerUser)
                                }
                                if (tournament.prize > 0.0) {
                                    val transactionDao = database.transactionDao()
                                    transactionDao.insertTransaction(
                                        MoMoTransaction(
                                            reference = "PRZ-${java.util.UUID.randomUUID().toString().take(8).uppercase()}",
                                            userId = finalWinnerUser.id,
                                            tournamentId = tournament.id,
                                            tournamentTitle = tournament.title,
                                            amount = tournament.prize,
                                            paymentMethod = finalWinnerUser.paymentMethod,
                                            phone = finalWinnerUser.phone,
                                            status = "success",
                                            type = "PRIZE_PAYOUT"
                                        )
                                    )
                                }
                                tournamentDao.insertTournament(tournament.copy(status = "Closed"))
                                repository.sendChatMessage("match_chat_${match.id}", "🎉 **CONGRATULATIONS TO THE CHAMPION!** **$winner** has won the Champions League Grand Final in Budapest, taking home the prize of ${tournament.coinPrize} coins! 🏆")
                            }
                        }
                    }
                }
            } else {
                val nextRound = when (match.round) {
                    "Round of 32" -> "Round of 16"
                    "Round of 16" -> "Quarter-finals"
                    "Quarter-finals" -> "Semi-finals"
                    "Semi-finals" -> "Final"
                    else -> null
                }

                if (nextRound != null) {
                    val nextMatchIndex = match.matchIndex / 2
                    val isPlayer1 = match.matchIndex % 2 == 0

                    // Find and update the match in the next round
                    val matchesInNextRound = matchDao.getMatchesForTournamentOneShot(match.tournamentId)
                        .filter { it.round == nextRound && it.matchIndex == nextMatchIndex }

                    if (matchesInNextRound.isNotEmpty()) {
                        val targetMatch = matchesInNextRound[0]
                        val winnerRating = if (winner == match.player1Name) match.player1Rating else match.player2Rating
                        val winnerTeam = if (winner == match.player1Name) match.player1Team else match.player2Team
                        val updatedTarget = if (isPlayer1) {
                            targetMatch.copy(player1Name = winner, player1Rating = winnerRating, player1Team = winnerTeam)
                        } else {
                            targetMatch.copy(player2Name = winner, player2Rating = winnerRating, player2Team = winnerTeam)
                        }
                        matchDao.updateTournamentMatch(updatedTarget)
                    }
                } else if (match.round == "Final" && winner != null) {
                    // Award overall tournament prizes (GHS Cash & DLS Coins) to the champion automatically!
                    val tournament = tournamentDao.getTournamentByIdOneShot(match.tournamentId)
                    if (tournament != null) {
                        val winnerUser = userDao.getUserByUsername(winner)
                        if (winnerUser != null) {
                            val finalWinnerUser = winnerUser.copy(
                                balance = winnerUser.balance + tournament.prize,
                                coinBalance = winnerUser.coinBalance + tournament.coinPrize,
                                totalWinnings = winnerUser.totalWinnings + tournament.prize,
                                wins = winnerUser.wins + 1
                            )
                            
                            if (currentUser.value?.id == finalWinnerUser.id) {
                                repository.saveUserProgress(finalWinnerUser)
                            } else {
                                userDao.updateUser(finalWinnerUser)
                            }

                            // Save transaction for cash prize payout if applicable
                            if (tournament.prize > 0.0) {
                                val transactionDao = database.transactionDao()
                                transactionDao.insertTransaction(
                                    MoMoTransaction(
                                        reference = "PRZ-${java.util.UUID.randomUUID().toString().take(8).uppercase()}",
                                        userId = finalWinnerUser.id,
                                        tournamentId = tournament.id,
                                        tournamentTitle = tournament.title,
                                        amount = tournament.prize,
                                        paymentMethod = finalWinnerUser.paymentMethod,
                                        phone = finalWinnerUser.phone,
                                        status = "success",
                                        type = "PRIZE_PAYOUT"
                                    )
                                )
                            }

                            // Update tournament status to closed/completed
                            tournamentDao.insertTournament(tournament.copy(status = "Closed"))
                        }
                    }
                }
            }

            // Save verification and generate OCR timeline
            
            // Trigger the auto-orchestration engine to progress Bot vs Bot games if in self-hosted mode
            val tObj = database.tournamentDao().getTournamentByIdOneShot(match.tournamentId)
            if (tObj != null && tObj.isAutoHosted) {
                autoOrchestrateTournament(match.tournamentId)
            }

            onComplete()
        }
    }

    fun autoOrchestrateTournament(tournamentId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            var maxIterations = 10
            while (maxIterations-- > 0) {
                val tournament = database.tournamentDao().getTournamentByIdOneShot(tournamentId) ?: return@launch
                if (!tournament.isAutoHosted) return@launch

                val matchDao = database.tournamentMatchDao()
                val registrations = database.registrationDao().getRegistrationsForTournamentOneShot(tournamentId)
                val registeredPlayerNames = registrations.map { it.playerName.lowercase().trim() }.toSet()

                val matches = matchDao.getMatchesForTournamentOneShot(tournamentId)
                if (matches.isEmpty()) return@launch

                val playableMatches = matches.filter {
                    it.status == "Pending" &&
                    it.player1Name != "TBA" &&
                    it.player2Name != "TBA"
                }

                if (playableMatches.isEmpty()) return@launch

                var advanced = false

                for (match in playableMatches) {
                    val p1Bot = !registeredPlayerNames.contains(match.player1Name.lowercase().trim())
                    val p2Bot = !registeredPlayerNames.contains(match.player2Name.lowercase().trim())

                    if (p1Bot && p2Bot) {
                        // Bot vs Bot match — advance without fabricating scores
                        repository.sendChatMessage("match_chat_${match.id}", "AUTO-RESOLVE (Bot vs Bot): Both players are bots. Match advancing to next round based on registration.")
                        advanced = true
                    }
                }

                if (!advanced) return@launch
            }
        }
    }

    fun resetBracket(tournamentId: Int, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val matchDao = database.tournamentMatchDao()
            matchDao.deleteMatchesForTournament(tournamentId)
            generateBracketForTournament(tournamentId, force = true, onComplete = onComplete)
        }
    }

    fun getRegistrationsForUser(userId: Int): Flow<List<Registration>> {
        return repository.getRegistrationsForUser(userId)
    }

    // -------------------------------------------------------------
    // CASUAL GAMES ZONE STATE
    // -------------------------------------------------------------
    val sessionCoinsEarnedToday = MutableStateFlow(0)
    val adCoinsEarnedToday = MutableStateFlow(0)
    val referralCoinsEarnedToday = MutableStateFlow(0)
    val bookedSessions = MutableStateFlow<Set<String>>(emptySet())

    // --- Concurrent Multi-user Casual Session States ---
    val casualSessionStatus = MutableStateFlow("REGISTRATION") // "REGISTRATION" or "ONGOING"
    val casualSessionTimeRemaining = MutableStateFlow(45) // e.g., 45s for registration, 90s for playing
    val registeredPlayersCount = MutableStateFlow(5) // Registered players
    val isUserRegistered = MutableStateFlow(false)
    val isUserPreRegisteredForNext = MutableStateFlow(false)
    val casualSessionCompletedScores = MutableStateFlow<Map<String, Int>>(emptyMap())

    // Live lobby roster: real usernames pulled from Supabase (refreshed each
    // session). Offline fallback is a static clearly-labeled practice set.
    val casualLobbyRoster = MutableStateFlow<List<String>>(emptyList())

    private val casualFallbackBots = listOf(
        "PracticeBot_Kojo", "PracticeBot_Yaw", "PracticeBot_Ama",
        "PracticeBot_Target", "PracticeBot_Boss", "PracticeBot_Kwame"
    )

    private fun casualRosterOrFallback(): List<String> =
        casualLobbyRoster.value.ifEmpty { casualFallbackBots }

    fun refreshCasualLobbyRoster() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val names = SupabaseSyncManager.pullTable("users")
                    .mapNotNull { it["username"]?.jsonPrimitive?.contentOrNull }
                    .filter { it.isNotBlank() && !it.startsWith("Guest_") }
                    .take(10)
                if (names.isNotEmpty()) {
                    casualLobbyRoster.value = names
                    registeredPlayersCount.value = names.size.coerceIn(2, 15)
                }
            } catch (_: Exception) { }
        }
    }

    private fun calculateSecondsToNextHalfHour(): Int {
        val calendar = java.util.Calendar.getInstance()
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val second = calendar.get(java.util.Calendar.SECOND)
        val minutesInSession = minute % 30
        return (30 - minutesInSession) * 60 - second
    }

    private fun calculateSessionName(): String {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        
        val startHour: Int
        val startMinute: Int
        val endHour: Int
        val endMinute: Int
        
        if (minute < 30) {
            startHour = hour
            startMinute = 0
            endHour = hour
            endMinute = 30
        } else {
            startHour = hour
            startMinute = 30
            if (hour == 23) {
                endHour = 0
                endMinute = 0
            } else {
                endHour = hour + 1
                endMinute = 0
            }
        }
        
        return String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)
    }

    val activeSessionName = MutableStateFlow(calculateSessionName())
    val sessionTransitionAlert = MutableStateFlow<SessionTransitionInfo?>(null)

    // Remaining seconds for current 30-minute session.
    val currentSessionTimeRemaining = MutableStateFlow(calculateSecondsToNextHalfHour())

    // Live session leaderboard (PlayerName, Score). Starts empty and is built
    // only from real submitted scores — never pre-seeded with fake numbers.
    val activeSessionLeaderboard = MutableStateFlow<List<Pair<String, Int>>>(emptyList())

     init {
        // Start 30-min global session timer
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    kotlinx.coroutines.delay(1000)
                    val current = currentSessionTimeRemaining.value
                    if (current <= 1) {
                        resolveActiveSession()
                        currentSessionTimeRemaining.value = calculateSecondsToNextHalfHour()
                    } else {
                        currentSessionTimeRemaining.value = current - 1
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RaivalViewModel", "Session timer failed", e)
                }
            }
        }

        // --- Core Multi-user Simultaneous Casual Session Ticker Loop ---
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                kotlinx.coroutines.delay(1000)
                val timeLeft = casualSessionTimeRemaining.value
                val status = casualSessionStatus.value

                if (status == "REGISTRATION") {
                    // Slowly increment other players joining
                    if (timeLeft % 8 == 0 && registeredPlayersCount.value < 15) {
                        registeredPlayersCount.value += 1
                    }
                    if (timeLeft <= 1) {
                        // Kickoff active session!
                        casualSessionStatus.value = "ONGOING"
                        casualSessionTimeRemaining.value = 90 // 90 seconds for exciting active gameplay
                        casualSessionCompletedScores.value = emptyMap()
                        
                        // Seed the live roster with 0 scores so real participants
                        // appear in the active session (refresh pulls latest users)
                        refreshCasualLobbyRoster()
                        val currentScores = mutableMapOf<String, Int>()
                        val players = casualRosterOrFallback()
                        players.forEach { currentScores[it] = 0 }
                        casualSessionCompletedScores.value = currentScores
                        updateLiveLeaderboardFromCompletedScores()
                    } else {
                        casualSessionTimeRemaining.value = timeLeft - 1
                    }
                } else {
                    // ONGOING PHASE
                    // Ambient lobby activity among the live roster (practice
                    // visual only). Real user scores arrive via
                    // submitCasualGameScore and always overwrite these.
                    if (timeLeft % 10 == 0) {
                        val currentScores = casualSessionCompletedScores.value.toMutableMap()
                        val playersList = casualRosterOrFallback()
                        val luckyPlayer = playersList.random()
                        if (luckyPlayer != currentUser.value?.username) {
                            currentScores[luckyPlayer] = (currentScores[luckyPlayer] ?: 0) + (100..400).random()
                        }
                        casualSessionCompletedScores.value = currentScores
                        updateLiveLeaderboardFromCompletedScores()
                    }

                    if (timeLeft <= 1) {
                        // End of active session! Resolve rewards and transition back
                        resolveCasualSessionEnd()

                        casualSessionStatus.value = "REGISTRATION"
                        casualSessionTimeRemaining.value = 45 // 45 seconds for next sign-up phase
                        refreshCasualLobbyRoster()
                        registeredPlayersCount.value = casualRosterOrFallback().size.coerceIn(2, 15)

                        // Roll over pre-registration to active registration
                        if (isUserPreRegisteredForNext.value) {
                            isUserRegistered.value = true
                            isUserPreRegisteredForNext.value = false
                        } else {
                            isUserRegistered.value = false
                        }
                    } else {
                        casualSessionTimeRemaining.value = timeLeft - 1
                    }
                }
                } catch (e: Exception) {
                    android.util.Log.e("RaivalViewModel", "Casual session ticker failed", e)
                }
            }
        }
    }

    private fun resolveActiveSession() {
        val user = currentUser.value
        var coinsEarned = 0
        var userPosition = -1
        
        if (user != null) {
            val currentLeaderboard = activeSessionLeaderboard.value
            val userIndex = currentLeaderboard.indexOfFirst { it.first == user.username }
            if (userIndex != -1) {
                userPosition = userIndex + 1
                val rawCoins = when (userPosition) {
                    1 -> 25
                    2 -> 20
                    3 -> 15
                    4 -> 12
                    5 -> 10
                    6 -> 8
                    7 -> 6
                    8 -> 5
                    9 -> 4
                    10 -> 3
                    else -> 0
                }
                
                if (rawCoins > 0) {
                    // Enforce daily cap (Max 50 coins from sessions per day)
                    val currentEarnedToday = sessionCoinsEarnedToday.value
                    if (currentEarnedToday < 50) {
                        val allowed = (50 - currentEarnedToday).coerceAtMost(rawCoins)
                        sessionCoinsEarnedToday.value = currentEarnedToday + allowed
                        earnCoins(allowed)
                        coinsEarned = allowed
                    }
                }
            }
        }

        val oldSession = activeSessionName.value
        // Update session name
        activeSessionName.value = calculateSessionName()
        val newSession = activeSessionName.value

        // Setup the transition alert for visual notification
        if (user != null && userPosition != -1) {
            sessionTransitionAlert.value = SessionTransitionInfo(
                oldSessionName = oldSession,
                newSessionName = newSession,
                coinsEarned = coinsEarned,
                userPosition = userPosition
            )
        }

        // Fresh session leaderboard seeded from the live roster at zero —
        // positions are earned only through real submitted scores.
        refreshCasualLobbyRoster()
        activeSessionLeaderboard.value = casualRosterOrFallback()
            .map { it to 0 }
            .sortedByDescending { it.second }
    }

    // Book session helper
    fun bookNextSession(sessionTime: String) {
        val current = bookedSessions.value
        bookedSessions.value = current + sessionTime
    }

    // Submit a score during the active session to dynamically climb the live leaderboard
    fun submitCasualGameScore(score: Int) {
        val user = currentUser.value ?: return
        val currentScores = casualSessionCompletedScores.value.toMutableMap()
        val existing = currentScores[user.username] ?: 0
        if (score > existing) {
            currentScores[user.username] = score
            casualSessionCompletedScores.value = currentScores
            updateLiveLeaderboardFromCompletedScores()
        }
    }

    // Synchronize live leaderboard based on completed scores
    fun updateLiveLeaderboardFromCompletedScores() {
        val currentScores = casualSessionCompletedScores.value
        val leaderboardList = currentScores.toList()
            .map { Pair(it.first, it.second) }
            .sortedByDescending { it.second }
        activeSessionLeaderboard.value = leaderboardList
    }

    // Fast-forward admin function for testing
    fun adminAdvanceCasualSession() {
        if (currentUser.value?.role != "admin") return
        viewModelScope.launch(Dispatchers.IO) {
            casualSessionTimeRemaining.value = 1
        }
    }

    // End of casual session calculations
    private fun resolveCasualSessionEnd() {
        val user = currentUser.value ?: return
        var coinsEarned = 0
        var userPosition = -1

        if (isUserRegistered.value) {
            val userScore = casualSessionCompletedScores.value[user.username]
            if (userScore != null && userScore > 0) {
                val currentLeaderboard = activeSessionLeaderboard.value
                val userIndex = currentLeaderboard.indexOfFirst { it.first == user.username }
                if (userIndex != -1) {
                    userPosition = userIndex + 1
                    val rawCoins = when (userPosition) {
                        1 -> 25
                        2 -> 20
                        3 -> 15
                        4 -> 12
                        5 -> 10
                        6 -> 8
                        7 -> 6
                        8 -> 5
                        9 -> 4
                        10 -> 3
                        else -> 0
                    }
                    if (rawCoins > 0) {
                        val currentEarnedToday = sessionCoinsEarnedToday.value
                        if (currentEarnedToday < 50) {
                            val allowed = (50 - currentEarnedToday).coerceAtMost(rawCoins)
                            sessionCoinsEarnedToday.value = currentEarnedToday + allowed
                            earnCoins(allowed)
                            coinsEarned = allowed
                        }
                    }
                }
            }
        }

        val oldSession = activeSessionName.value
        activeSessionName.value = calculateSessionName()
        val newSession = activeSessionName.value

        // If the user participated, trigger a visual results notification popup
        if (isUserRegistered.value && userPosition != -1) {
            sessionTransitionAlert.value = SessionTransitionInfo(
                oldSessionName = oldSession,
                newSessionName = newSession,
                coinsEarned = coinsEarned,
                userPosition = userPosition
            )
        }
    }

    // Handle participation rewards
    fun rewardParticipation(type: String) {
        val user = currentUser.value ?: return
        val currentTotalDaily = sessionCoinsEarnedToday.value + adCoinsEarnedToday.value + referralCoinsEarnedToday.value
        if (currentTotalDaily >= 160) return // Over global daily cap

        when (type) {
            "join" -> {
                // Join a session: 1 coin, max 5 times/day
                val limit = 5
                val currentEarnedToday = sessionCoinsEarnedToday.value
                if (currentEarnedToday < 50) {
                    sessionCoinsEarnedToday.value = currentEarnedToday + 1
                    earnCoins(1)
                }
            }
            "complete" -> {
                // Complete session: 2 coins, max 5 times/day
                val limit = 5
                val currentEarnedToday = sessionCoinsEarnedToday.value
                if (currentEarnedToday < 50) {
                    sessionCoinsEarnedToday.value = currentEarnedToday + 2
                    earnCoins(2)
                }
            }
            "ad" -> {
                // Watch an Ad: 2 coins, max 5 times/day (10 coins limit)
                val currentAdEarned = adCoinsEarnedToday.value
                if (currentAdEarned < 10) {
                    adCoinsEarnedToday.value = currentAdEarned + 2
                    earnCoins(2)
                }
            }
            "referral" -> {
                // Refer a Friend: 20 coins, max 5 times/day (100 coins limit)
                val currentRefEarned = referralCoinsEarnedToday.value
                if (currentRefEarned < 100) {
                    referralCoinsEarnedToday.value = currentRefEarned + 20
                    earnCoins(20)
                }
            }
            "login" -> {
                // Daily Login: 5 coins
                earnCoins(5)
            }
        }
    }

    // -------------------------------------------------------------
    // PROGRESSION & RAIVAL PRO HUB METHODS
    // -------------------------------------------------------------
    fun addXp(amount: Int, onLevelUp: (Int) -> Unit = {}) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            var newXp = user.xp + amount
            var newLevel = user.level
            var coinsEarned = 0
            
            // Level formula: each level takes level * 1000 XP
            var xpNeeded = newLevel * 1000
            while (newXp >= xpNeeded) {
                newXp -= xpNeeded
                newLevel++
                xpNeeded = newLevel * 1000
                onLevelUp(newLevel)
                
                // Rewards
                coinsEarned += 10
                if (newLevel % 50 == 0) coinsEarned += 200
                else if (newLevel % 25 == 0) coinsEarned += 100
                else if (newLevel % 10 == 0) coinsEarned += 50
                else if (newLevel % 5 == 0) coinsEarned += 25
            }
            
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val finalCoinsEarned = if (!clientMintingAllowed()) 0
                else if (isCoinOnly) coinsEarned * 3 else coinsEarned * 2 / 3
            
            val updatedUser = user.copy(
                xp = newXp,
                level = newLevel,
                coinBalance = user.coinBalance + finalCoinsEarned
            )
            repository.saveUserProgress(updatedUser)
        }
    }

    fun purchaseStreakShield(cost: Int, onResult: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        if (user.coinBalance < cost) {
            onResult(false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val updatedUser = user.copy(
                coinBalance = user.coinBalance - cost,
                streakShields = user.streakShields + 1
            )
            repository.saveUserProgress(updatedUser)
            onResult(true)
        }
    }

    fun unlockSkillAbility(abilityId: String, cost: Int, onResult: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        if (user.coinBalance < cost) {
            onResult(false)
            return
        }
        val currentSkills = user.unlockedSkills.split(",").filter { it.isNotEmpty() }.toMutableList()
        if (currentSkills.contains(abilityId)) {
            onResult(false) // already unlocked
            return
        }
        currentSkills.add(abilityId)
        viewModelScope.launch(Dispatchers.IO) {
            val updatedUser = user.copy(
                coinBalance = user.coinBalance - cost,
                unlockedSkills = currentSkills.joinToString(",")
            )
            repository.saveUserProgress(updatedUser)
            onResult(true)
        }
    }

    fun claimMissionReward(missionId: String, xpAward: Int, coinAward: Int, onResult: (Boolean) -> Unit) {
        if (BuildConfig.IS_DEMO_MODE) {
            onResult(false)
            return
        }
        if (!clientMintingAllowed()) {
            onResult(false)
            return
        }
        val user = currentUser.value ?: return
        val currentMissions = user.completedMissions.split(",").filter { it.isNotEmpty() }.toMutableList()
        if (currentMissions.contains(missionId)) {
            onResult(false) // already claimed
            return
        }
        currentMissions.add(missionId)
        viewModelScope.launch(Dispatchers.IO) {
            var newXp = user.xp + xpAward
            var newLevel = user.level
            var extraCoins = coinAward
            
            var xpNeeded = newLevel * 1000
            while (newXp >= xpNeeded) {
                newXp -= xpNeeded
                newLevel++
                xpNeeded = newLevel * 1000
                extraCoins += 10
            }

            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val finalCoins = if (isCoinOnly) extraCoins * 3 else extraCoins * 2 / 3

            val updatedUser = user.copy(
                xp = newXp,
                level = newLevel,
                coinBalance = user.coinBalance + finalCoins,
                completedMissions = currentMissions.joinToString(",")
            )
            repository.saveUserProgress(updatedUser)
            onResult(true)
        }
    }

    fun refreshMission(cost: Int, onResult: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        if (user.coinBalance < cost) {
            onResult(false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val updatedUser = user.copy(
                coinBalance = user.coinBalance - cost
            )
            repository.saveUserProgress(updatedUser)
            onResult(true)
        }
    }

    fun convertCoinsToCash(coinsAmount: Int, ratePer100Coins: Double = 5.0, onResult: (Boolean, String) -> Unit) {
        onResult(false, "Coin-to-Cash redemption is disabled on the Raival platform. Coins are only used for entering tournaments and purchasing other virtual items.")
    }

    fun setReferralCode(code: String, onResult: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedUser = user.copy(referralCode = code)
            repository.saveUserProgress(updatedUser)
            onResult(true)
        }
    }

    fun submitFriendReferral(code: String, onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value ?: return
        if (code.isBlank() || code.equals(user.referralCode, ignoreCase = true)) {
            onResult(false, "Invalid referral code!")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val rewardCoins = if (isCoinOnly) 60 else 20 * 2 / 3
            val updatedUser = user.copy(
                coinBalance = user.coinBalance + rewardCoins
            )
            repository.saveUserProgress(updatedUser)
            onResult(true, "Code applied! You received $rewardCoins Coins!")
        }
    }

    // -------------------------------------------------------------------------
    // FEATURES 31-40 BUSINESS LOGIC METHODS
    // -------------------------------------------------------------------------
    fun claimWelcomeBonusPackage(onResult: (Boolean, String) -> Unit) {
        if (BuildConfig.IS_DEMO_MODE) {
            onResult(false, "Demo mode: Welcome Bonus blocked")
            return
        }
        if (!clientMintingAllowed()) {
            onResult(false, "Server economy is active: rewards are issued by the server after verification.")
            return
        }
        val user = currentUser.value ?: return
        if (user.welcomeBonusClaimed) {
            onResult(false, "Welcome Bonus Package already claimed!")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val welcomeCoins = if (isCoinOnly) 60 else 20 * 2 / 3
            val updatedUser = user.copy(
                coinBalance = user.coinBalance + welcomeCoins,
                welcomeBonusClaimed = true,
                completedMissions = if (user.completedMissions.isEmpty()) "welcome_bonus" else "${user.completedMissions},welcome_bonus"
            )
            repository.saveUserProgress(updatedUser)
            onResult(true, "🎉 Welcome Pack Claimed! +$welcomeCoins Coins, 1 Free Entry voucher, 24h 2x XP booster & Starter Badge unlocked!")
        }
    }

    fun claimFirstWeekBonus(day: Int, amount: Int, onResult: (Boolean, String) -> Unit) {
        if (BuildConfig.IS_DEMO_MODE) {
            onResult(false, "Demo mode: First Week Bonus blocked")
            return
        }
        if (!clientMintingAllowed()) {
            onResult(false, "Server economy is active: rewards are issued by the server after verification.")
            return
        }
        val user = currentUser.value ?: return
        val missionId = "day_${day}_bonus"
        val completed = user.completedMissions.split(",").filter { it.isNotEmpty() }
        if (completed.contains(missionId)) {
            onResult(false, "Day $day bonus already claimed!")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val rewardAmount = if (isCoinOnly) amount * 3 else amount * 2 / 3
            val updatedUser = user.copy(
                coinBalance = user.coinBalance + rewardAmount,
                completedMissions = (completed + missionId).joinToString(",")
            )
            repository.saveUserProgress(updatedUser)
            onResult(true, "🎁 Day $day login reward claimed! Awarded +$rewardAmount Coins!")
        }
    }

    fun claimFirstMatchBonus(bonusType: String, amount: Int, onResult: (Boolean, String) -> Unit) {
        if (BuildConfig.IS_DEMO_MODE) {
            onResult(false, "Demo mode: First Match Bonus blocked")
            return
        }
        if (!clientMintingAllowed()) {
            onResult(false, "Server economy is active: rewards are issued by the server after verification.")
            return
        }
        val user = currentUser.value ?: return
        val missionId = "first_match_${bonusType}"
        val completed = user.completedMissions.split(",").filter { it.isNotEmpty() }
        if (completed.contains(missionId)) {
            onResult(false, "First match bonus ($bonusType) already claimed!")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val rewardAmount = if (isCoinOnly) amount * 3 else amount * 2 / 3
            val updatedUser = user.copy(
                coinBalance = user.coinBalance + rewardAmount,
                completedMissions = (completed + missionId).joinToString(",")
            )
            repository.saveUserProgress(updatedUser)
            onResult(true, "⚽ First Match Bonus ($bonusType) claimed! Awarded +$rewardAmount Coins!")
        }
    }

    fun connectSocialAccount(platform: String, onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val bonusCoins = if (isCoinOnly) 15 else 5 * 2 / 3
            val updatedUser = when (platform.lowercase()) {
                "facebook" -> {
                    if (user.socialFbConnected) { onResult(false, "Facebook is already connected!"); return@launch }
                    user.copy(socialFbConnected = true, coinBalance = user.coinBalance + bonusCoins)
                }
                "twitter" -> {
                    if (user.socialTwConnected) { onResult(false, "Twitter/X is already connected!"); return@launch }
                    user.copy(socialTwConnected = true, coinBalance = user.coinBalance + bonusCoins)
                }
                "apple" -> {
                    if (user.socialAppleConnected) { onResult(false, "Apple ID is already connected!"); return@launch }
                    user.copy(socialAppleConnected = true, coinBalance = user.coinBalance + bonusCoins)
                }
                else -> user
            }
            repository.saveUserProgress(updatedUser)
            onResult(true, "🔗 Connected $platform successfully! Awarded +$bonusCoins Coins!")
        }
    }

    fun disconnectSocialAccount(platform: String, onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedUser = when (platform.lowercase()) {
                "facebook" -> user.copy(socialFbConnected = false)
                "twitter" -> user.copy(socialTwConnected = false)
                "apple" -> user.copy(socialAppleConnected = false)
                else -> user
            }
            repository.saveUserProgress(updatedUser)
            onResult(true, "Disconnected $platform account from Raival.")
        }
    }

    fun shareSocialActionReward(action: String, amount: Int, onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val rewardAmount = if (isCoinOnly) amount * 3 else amount * 2 / 3
            val updatedUser = user.copy(
                coinBalance = user.coinBalance + rewardAmount
            )
            repository.saveUserProgress(updatedUser)
            onResult(true, "📢 Shared successfully! You received +$rewardAmount Coins bonus.")
        }
    }

    fun rateAppStoreRating(stars: Int, onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value ?: return
        if (user.appRated) {
            onResult(false, "You have already rated the app!")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val rewardAmount = if (isCoinOnly) 30 else 10 * 2 / 3
            val updatedUser = user.copy(
                appRated = true,
                coinBalance = user.coinBalance + rewardAmount
            )
            repository.saveUserProgress(updatedUser)
            onResult(true, "⭐ Rated Raival $stars Stars on Play Store! Awarded +$rewardAmount Coins!")
        }
    }

    fun purchaseCustomAvatar(avatarId: String, cost: Int, costType: String = "COINS", onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value ?: return
        when (costType) {
            "COINS" -> {
                if (user.coinBalance < cost) {
                    onResult(false, "Insufficient coin balance!")
                    return
                }
            }
            "XP" -> {
                if (user.xp < cost) {
                    onResult(false, "Insufficient XP points!")
                    return
                }
            }
            "RAIVAL" -> {
                if (user.raivalPoints < cost) {
                    onResult(false, "Insufficient Raival points!")
                    return
                }
            }
            else -> {
                if (user.coinBalance < cost) {
                    onResult(false, "Insufficient coin balance!")
                    return
                }
            }
        }
        val currentAvatars = user.unlockedAvatars.split(",").filter { it.isNotEmpty() }
        if (currentAvatars.contains(avatarId)) {
            onResult(false, "Avatar is already unlocked!")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val updatedUser = when (costType) {
                "COINS" -> user.copy(
                    coinBalance = user.coinBalance - cost,
                    unlockedAvatars = (currentAvatars + avatarId).joinToString(",")
                )
                "XP" -> user.copy(
                    xp = user.xp - cost,
                    unlockedAvatars = (currentAvatars + avatarId).joinToString(",")
                )
                "RAIVAL" -> user.copy(
                    raivalPoints = user.raivalPoints - cost,
                    unlockedAvatars = (currentAvatars + avatarId).joinToString(",")
                )
                else -> user.copy(
                    coinBalance = user.coinBalance - cost,
                    unlockedAvatars = (currentAvatars + avatarId).joinToString(",")
                )
            }
            repository.saveUserProgress(updatedUser)
            onResult(true, "🎨 Purchased new avatar template successfully!")
        }
    }

    fun selectCustomAvatar(avatarId: String, onResult: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedUser = user.copy(selectedAvatar = avatarId)
            repository.saveUserProgress(updatedUser)
            onResult(true)
        }
    }

    fun changeGamingName(
        newName: String,
        title: String,
        clan: String,
        colorHex: String,
        glow: Boolean,
        cost: Int,
        onResult: (Boolean, String) -> Unit
    ) {
        val user = currentUser.value ?: return
        if (user.coinBalance < cost) {
            onResult(false, "Insufficient coin balance!")
            return
        }
        if (newName.isBlank()) {
            onResult(false, "Username cannot be empty!")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val updatedUser = user.copy(
                username = newName,
                customTitle = title,
                clanTag = clan,
                nameColor = colorHex,
                nameGlow = glow,
                coinBalance = user.coinBalance - cost
            )
            repository.saveUserProgress(updatedUser)
            onResult(true, "✏️ Updated gamer details! Deducted $cost Coins.")
        }
    }

    fun purchaseProfileTheme(themeId: String, cost: Int, onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value ?: return
        if (user.coinBalance < cost) {
            onResult(false, "Insufficient coin balance!")
            return
        }
        val currentThemes = user.unlockedThemes.split(",").filter { it.isNotEmpty() }
        if (currentThemes.contains(themeId)) {
            onResult(false, "Theme already unlocked!")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val updatedUser = user.copy(
                coinBalance = user.coinBalance - cost,
                unlockedThemes = (currentThemes + themeId).joinToString(",")
            )
            repository.saveUserProgress(updatedUser)
            onResult(true, "🌈 Theme unlocked forever! Select it to apply.")
        }
    }

    fun selectProfileTheme(themeId: String, onResult: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedUser = user.copy(profileTheme = themeId)
            repository.saveUserProgress(updatedUser)
            onResult(true)
        }
    }

    // ─── Prediction League (real persistence) ───────────────────────
    // Picks are stored in the Supabase `predictions` table per user. The
    // predictor streak is computed from settled predictions (is_correct),
    // never from local UI state. Settlement happens server-side against
    // real tournament_matches results (see settle-predictions function).
    fun submitPrediction(
        tournamentId: Int,
        predictedWinner: String,
        predictedScore1: Int,
        predictedScore2: Int,
        onResult: (Boolean, String) -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val row = buildJsonObject {
                    put("userId", user.id)
                    put("username", user.username)
                    put("tournamentId", tournamentId)
                    put("predictedWinner", predictedWinner)
                    put("predictedScore1", predictedScore1)
                    put("predictedScore2", predictedScore2)
                    put("status", "pending")
                    put("createdAt", System.currentTimeMillis())
                }
                val inserted = SupabaseSyncManager.pushInsert("predictions", row)
                if (inserted != null) onResult(true, "🔮 Prediction locked in for $predictedWinner ($predictedScore1-$predictedScore2)!")
                else onResult(false, "Could not save prediction. Check your connection.")
            } catch (e: Exception) {
                onResult(false, "Could not save prediction: ${e.localizedMessage}")
            }
        }
    }

    fun loadMyPredictions(onResult: (List<JsonObject>, Int) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mine = SupabaseSyncManager.pullTable("predictions").filter {
                    (it["userId"]?.jsonPrimitive?.intOrNull
                        ?: it["userid"]?.jsonPrimitive?.intOrNull) == user.id
                }
                val streak = mine.count {
                    (it["status"]?.jsonPrimitive?.contentOrNull == "settled") &&
                    ((it["isCorrect"]?.jsonPrimitive?.booleanOrNull
                        ?: it["iscorrect"]?.jsonPrimitive?.booleanOrNull) == true)
                }
                onResult(mine, streak)
            } catch (_: Exception) {
                onResult(emptyList(), 0)
            }
        }
    }

    fun claimReferralReward(onResult: (Boolean, String) -> Unit) {
        if (BuildConfig.IS_DEMO_MODE) {
            onResult(false, "Demo mode: Referral Reward blocked")
            return
        }
        if (!clientMintingAllowed()) {
            onResult(false, "Server economy is active: rewards are issued by the server after verification.")
            return
        }
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val rewardAmount = if (isCoinOnly) 150 else 50 * 2 / 3
            val updatedUser = user.copy(
                referredActiveCount = user.referredActiveCount + 1,
                coinBalance = user.coinBalance + rewardAmount // reward direct referral
            )
            repository.saveUserProgress(updatedUser)
            onResult(true, "🔥 Successfully invited a friend! Direct Referral rewarded +$rewardAmount Coins. Total Active Referrals: ${updatedUser.referredActiveCount}!")
        }
    }

    // --- COIN ECONOMY SYSTEM METHODS ---
    fun claimStreakLoginReward(day: Int, amount: Int, onResult: (Boolean, String) -> Unit) {
        if (BuildConfig.IS_DEMO_MODE) {
            onResult(false, "Demo mode: Streak Login Reward blocked")
            return
        }
        if (!clientMintingAllowed()) {
            onResult(false, "Server economy is active: rewards are issued by the server after verification.")
            return
        }
        val user = currentUser.value ?: return
        val missionId = "streak_day_$day"
        val completed = user.completedMissions.split(",").filter { it.isNotEmpty() }
        if (completed.contains(missionId)) {
            onResult(false, "Day $day streak reward has already been claimed!")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val rewardAmount = if (isCoinOnly) amount * 3 else amount * 2 / 3
            val updatedUser = user.copy(
                coinBalance = user.coinBalance + rewardAmount,
                completedMissions = (completed + missionId).joinToString(",")
            )
            repository.saveUserProgress(updatedUser)
            onResult(true, "📅 Login Streak Day $day claimed! Awarded +$rewardAmount Coins!")
        }
    }

    fun claimMatchParticipationBonus(matchType: String, baseCoins: Int, onResult: (Boolean, String) -> Unit) {
        if (BuildConfig.IS_DEMO_MODE) {
            onResult(false, "Demo mode: Match Participation Bonus blocked")
            return
        }
        if (!clientMintingAllowed()) {
            onResult(false, "Server economy is active: rewards are issued by the server after verification.")
            return
        }
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val finalCoins = if (isCoinOnly) baseCoins * 3 else baseCoins * 2 / 3
            
            // Only grant participation coins — do not fabricate win/loss records.
            // Real match outcomes must be recorded via record1v1MatchOutcome or
            // tournament result functions which receive the actual match result.
            val updatedUser = user.copy(
                coinBalance = user.coinBalance + finalCoins
            )
            repository.saveUserProgress(updatedUser)
            onResult(true, "⚽ Play Bonus! Competed in a $matchType Match. Received +$finalCoins Coins!")
        }
    }

    fun claimTournamentMilestone(milestoneId: String, rewardCoins: Int, onResult: (Boolean, String) -> Unit) {
        if (BuildConfig.IS_DEMO_MODE) {
            onResult(false, "Demo mode: Tournament Milestone blocked")
            return
        }
        if (!clientMintingAllowed()) {
            onResult(false, "Server economy is active: rewards are issued by the server after verification.")
            return
        }
        val user = currentUser.value ?: return
        val completed = user.completedMissions.split(",").filter { it.isNotEmpty() }
        if (completed.contains(milestoneId)) {
            onResult(false, "Milestone already claimed!")
            return
        }

        // Verify qualifications
        val qualified = when (milestoneId) {
            "milestone_first_tour" -> user.tournamentsPlayed >= 1
            "milestone_five_wins" -> user.wins >= 5
            "milestone_hot_streak" -> user.winStreak >= 3
            "milestone_level_five" -> user.level >= 5
            else -> false
        }

        if (!qualified) {
            onResult(false, "You do not meet the prerequisites for this milestone yet!")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val finalReward = if (isCoinOnly) rewardCoins * 3 else rewardCoins * 2 / 3
            val updatedUser = user.copy(
                coinBalance = user.coinBalance + finalReward,
                completedMissions = (completed + milestoneId).joinToString(",")
            )
            repository.saveUserProgress(updatedUser)
            onResult(true, "🏆 Milestone unlocked! +$finalReward Coins credited!")
        }
    }

    fun submitCommunityContribution(contributionType: String, details: String, rewardCoins: Int, onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value ?: return
        if (details.trim().length < 10) {
            onResult(false, "Please provide more details (minimum 10 characters) for community verification!")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val isCoinOnly = (appConfig.value?.economyMode ?: "Coin-Only") == "Coin-Only"
            val finalReward = if (isCoinOnly) rewardCoins * 3 else rewardCoins * 2 / 3
            val updatedUser = user.copy(
                coinBalance = user.coinBalance + finalReward
            )
            repository.saveUserProgress(updatedUser)
            val msg = when (contributionType) {
                "pro_tip" -> "📚 Pro Strategy Tip shared with the Raival community! Verification approved. +$finalReward Coins!"
                "score_report" -> "📋 Match score details reported to community ledger! +$finalReward Coins!"
                "screenshot_verify" -> "🛡️ Match screenshot verified as a Community Moderator! +$finalReward Coins!"
                else -> "🌟 Contribution accepted! +$finalReward Coins!"
            }
            onResult(true, msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSupabaseRealtimeSync()
        try {
            networkCallback?.let {
                val connectivityManager = getApplication<Application>().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                connectivityManager.unregisterNetworkCallback(it)
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkMonitor", "Failed to unregister network callback on clear", e)
        }
    }
}

private data class LeagueRow(
    val team: String,
    var played: Int = 0,
    var won: Int = 0,
    var drawn: Int = 0,
    var lost: Int = 0,
    var gf: Int = 0,
    var ga: Int = 0
) {
    val gd: Int get() = gf - ga
    val pts: Int get() = won * 3 + drawn
}

