package com.example.data.repository

import com.example.BuildConfig
import com.example.data.PasswordHelper
import com.example.data.dao.*
import com.example.data.model.*
import com.example.data.sync.SupabaseSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class RaivalRepository(
    private val scope: CoroutineScope,
    private val userDao: UserDao,
    private val tournamentDao: TournamentDao,
    private val registrationDao: RegistrationDao,
    private val transactionDao: TransactionDao,
    private val teamDao: TeamDao,
    private val chatMessageDao: ChatMessageDao,
    private val matchSessionDao: MatchSessionDao,
    private val appConfigDao: AppConfigDao,
    private val marketplaceListingDao: MarketplaceListingDao,
    private val marketplaceOfferDao: MarketplaceOfferDao,
    private val marketplaceTransactionDao: MarketplaceTransactionDao,
    private val tournamentMatchDao: TournamentMatchDao,
    private val communityPostDao: CommunityPostDao,
    private val communityCommentDao: CommunityCommentDao
) {
    private val sync = SupabaseSyncManager
    val appConfig: Flow<AppConfig?> = appConfigDao.getAppConfig()

    suspend fun getAppConfigOneShot(): AppConfig? {
        return appConfigDao.getAppConfigOneShot()
    }

    suspend fun updateAppConfig(config: AppConfig) {
        appConfigDao.insertOrUpdate(config)
        scope.launch { sync.pushInsert("app_config", sync.toJson(config)) }
    }

    suspend fun resetAllCoinsToZero() {
        val users = userDao.getAllUsersOneShot()
        users.forEach { user ->
            val updatedUser = user.copy(coinBalance = 0)
            userDao.updateUser(updatedUser)
            scope.launch { sync.pushUpdate("users", updatedUser.id, sync.toJson(updatedUser)) }
        }
        val config = appConfigDao.getAppConfigOneShot() ?: AppConfig()
        val updatedConfig = config.copy(coinsResetToZeroDone = true)
        appConfigDao.insertOrUpdate(updatedConfig)
        scope.launch { sync.pushInsert("app_config", sync.toJson(updatedConfig)) }
    }
    // Current user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Helper to set logged-in user
    fun setCurrentUser(user: User?) {
        val sanitizedUser = if (user?.role == "admin") user.copy(role = "player") else user
        _currentUser.value = sanitizedUser
    }

    // Refresh current user state from database
    suspend fun refreshCurrentUser() {
        _currentUser.value?.let { user ->
            val updatedUser = userDao.getUserByIdOneShot(user.id)
            _currentUser.value = updatedUser
        }
    }

    // Auth Actions
    suspend fun login(email: String, password: String = ""): User? {
        val user = userDao.getUserByEmail(email)
        if (user != null) {
            // Verify password if one is set on the account
            if (user.passwordHash.isNotEmpty()) {
                val valid = PasswordHelper.verifyPassword(password, user.passwordSalt, user.passwordHash)
                if (!valid) return null
            }
            _currentUser.value = user
        }
        return user
    }

    suspend fun registerUser(
        username: String,
        fullName: String,
        email: String,
        phone: String,
        password: String = ""
    ): User? {
        val existingEmail = userDao.getUserByEmail(email)
        val existingUser = userDao.getUserByUsername(username)
        if (existingEmail != null || existingUser != null) return null

        val salt = PasswordHelper.generateSalt()
        val passwordHash = if (password.isNotEmpty()) PasswordHelper.hashPassword(password, salt) else ""

        val newUser = User(
            username = username,
            fullName = fullName,
            email = email,
            phone = phone,
            balance = 50.0,
            coinBalance = 200,
            raivalPoints = 100,
            passwordHash = passwordHash,
            passwordSalt = salt
        )
        val id = userDao.insertUser(newUser)
        val createdUser = newUser.copy(id = id.toInt())
        scope.launch { sync.pushInsert("users", sync.toJson(createdUser)) }
        _currentUser.value = createdUser
        return createdUser
    }

    suspend fun updateUserProfile(
        fullName: String,
        phone: String,
        bio: String,
        dlsHandle: String,
        efootballHandle: String,
        discordHandle: String,
        ghanaRegion: String = "Greater Accra",
        ghanaHometown: String = "Accra",
        preferredGame: String = "Dream League Soccer"
    ): User? {
        val current = _currentUser.value ?: return null
        val updated = current.copy(
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
        userDao.updateUser(updated)
        scope.launch { sync.pushUpdate("users", updated.id, sync.toJson(updated)) }
        _currentUser.value = updated
        return updated
    }

    suspend fun registerUserForImport(user: User): Int {
        val existing = userDao.getUserByUsername(user.username) ?: userDao.getUserByEmail(user.email)
        if (existing != null) return existing.id
        val id = userDao.insertUser(user).toInt()
        scope.launch { sync.pushInsert("users", sync.toJson(user.copy(id = id))) }
        return id
    }

    suspend fun insertRegistrationForImport(reg: Registration): Long {
        val existing = registrationDao.getRegistrationByUserAndTournament(reg.userId, reg.tournamentId)
        if (existing != null) return -1L
        // Also update the player count on the tournament
        val tour = tournamentDao.getTournamentByIdOneShot(reg.tournamentId)
        if (tour != null) {
            val updatedTour = tour.copy(players = tour.players + 1)
            tournamentDao.updateTournament(updatedTour)
            scope.launch { sync.pushUpdate("tournaments", updatedTour.id, sync.toJson(updatedTour)) }
        }
        val regId = registrationDao.insertRegistration(reg)
        scope.launch { sync.pushInsert("registrations", sync.toJson(reg.copy(id = regId.toInt()))) }
        return regId
    }

    suspend fun saveUserProgress(user: User) {
        userDao.updateUser(user)
        scope.launch { sync.pushUpdate("users", user.id, sync.toJson(user)) }
        _currentUser.value = user
    }

    suspend fun getUserByIdOneShot(id: Int): User? = userDao.getUserByIdOneShot(id)

    // Tournaments
    val allTournaments: Flow<List<Tournament>> = tournamentDao.getAllTournaments()

    fun getTournamentById(id: Int): Flow<Tournament?> = tournamentDao.getTournamentById(id)

    suspend fun createTournament(tournament: Tournament): Long {
        val sanitized = if (tournament.coinEntryFee <= 0 && tournament.entryFee <= 0.0) {
            tournament.copy(coinEntryFee = 10)
        } else {
            tournament
        }
        val insertedId = tournamentDao.insertTournament(sanitized)
        scope.launch { sync.pushInsert("tournaments", sync.toJson(sanitized.copy(id = insertedId.toInt()))) }
        return insertedId
    }

    suspend fun deleteTournament(id: Int) {
        registrationDao.deleteRegistrationsForTournament(id)
        tournamentMatchDao.deleteMatchesForTournament(id)
        tournamentDao.deleteTournamentById(id)
        scope.launch { sync.pushDelete("tournaments", id) }
    }

    suspend fun updateTournament(tournament: Tournament) {
        val sanitized = if (tournament.coinEntryFee <= 0 && tournament.entryFee <= 0.0) {
            tournament.copy(coinEntryFee = 10)
        } else {
            tournament
        }
        tournamentDao.updateTournament(sanitized)
        scope.launch { sync.pushUpdate("tournaments", sanitized.id, sync.toJson(sanitized)) }
    }

    suspend fun cancelAndRefundTournament(id: Int) {
        val tournament = tournamentDao.getTournamentByIdOneShot(id) ?: return
        val registrations = registrationDao.getRegistrationsForTournamentOneShot(id)
        
        registrations.forEach { reg ->
            userDao.getUserByIdOneShot(reg.userId)?.let { user ->
                val updatedUser = if (tournament.coinEntryFee > 0) {
                    user.copy(
                        coinBalance = user.coinBalance + tournament.coinEntryFee,
                        tournamentsPlayed = maxOf(0, user.tournamentsPlayed - 1)
                    )
                } else if (tournament.entryFee > 0.0) {
                    user.copy(
                        balance = user.balance + tournament.entryFee,
                        tournamentsPlayed = maxOf(0, user.tournamentsPlayed - 1)
                    )
                } else {
                    user.copy(
                        tournamentsPlayed = maxOf(0, user.tournamentsPlayed - 1)
                    )
                }
                userDao.updateUser(updatedUser)
                scope.launch { sync.pushUpdate("users", updatedUser.id, sync.toJson(updatedUser)) }
            }
        }
        
        registrationDao.deleteRegistrationsForTournament(id)
        
        val cancelledTour = tournament.copy(
            players = 0,
            status = "Cancelled"
        )
        tournamentDao.updateTournament(cancelledTour)
        scope.launch { sync.pushUpdate("tournaments", cancelledTour.id, sync.toJson(cancelledTour)) }
        
        refreshCurrentUser()
    }

    // Registrations & Payments
    fun getRegistrationsForUser(userId: Int): Flow<List<Registration>> =
        registrationDao.getRegistrationsForUser(userId)

    fun getRegistrationsForTournament(tournamentId: Int): Flow<List<Registration>> =
        registrationDao.getRegistrationsForTournament(tournamentId)

    suspend fun registerForTournament(
        userId: Int,
        tournamentId: Int,
        inGameName: String,
        paymentMethod: String,
        phone: String,
        teamRating: Int = 0,
        modelTeam: String = ""
    ): Boolean {
        val user = userDao.getUserByIdOneShot(userId) ?: return false
        val tournament = tournamentDao.getTournamentByIdOneShot(tournamentId) ?: return false

        if (tournament.status != "Open") return false
        if (tournament.players >= tournament.maxPlayers) return false

        // Check if already registered
        val existingReg = registrationDao.getRegistrationByUserAndTournament(userId, tournamentId)
        if (existingReg != null) return true // already registered

        // Check fee: Can pay via coins or GHS Cash
        if (tournament.entryFee > 0.0) {
            if (user.balance < tournament.entryFee) return false
        } else if (tournament.coinEntryFee > 0) {
            if (user.coinBalance < tournament.coinEntryFee) return false
        }

        // Deduct fee
        val updatedUser = if (tournament.entryFee > 0.0) {
            user.copy(
                balance = user.balance - tournament.entryFee,
                tournamentsPlayed = user.tournamentsPlayed + 1,
                raivalPoints = user.raivalPoints + 20
            )
        } else {
            user.copy(
                coinBalance = user.coinBalance - tournament.coinEntryFee,
                tournamentsPlayed = user.tournamentsPlayed + 1,
                raivalPoints = user.raivalPoints + 10
            )
        }
        userDao.updateUser(updatedUser)
        scope.launch { sync.pushUpdate("users", updatedUser.id, sync.toJson(updatedUser)) }
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updatedUser
        }

        // Increase tournament player count
        val updatedTournament = tournament.copy(
            players = tournament.players + 1,
            status = if (tournament.players + 1 >= tournament.maxPlayers) "Closed" else "Open"
        )
        tournamentDao.updateTournament(updatedTournament)
        scope.launch { sync.pushUpdate("tournaments", updatedTournament.id, sync.toJson(updatedTournament)) }

        // Insert registration record
        val registrationPlayerName = if (inGameName.trim().isNotEmpty()) inGameName.trim() else user.username
        val newRegistration = Registration(
            userId = userId,
            tournamentId = tournamentId,
            playerName = registrationPlayerName,
            inGameName = inGameName,
            paymentMethod = paymentMethod,
            phone = phone,
            teamRating = teamRating,
            modelTeam = modelTeam
        )
        val regId = registrationDao.insertRegistration(newRegistration)
        scope.launch { sync.pushInsert("registrations", sync.toJson(newRegistration.copy(id = regId.toInt()))) }

        // Insert transaction record
        val txRef = "TX-${UUID.randomUUID().toString().take(8).uppercase()}"
        val newTx = MoMoTransaction(
            reference = txRef,
            userId = userId,
            tournamentId = tournamentId,
            tournamentTitle = tournament.title,
            amount = tournament.entryFee,
            paymentMethod = paymentMethod,
            phone = phone,
            status = "success",
            type = "REGISTRATION"
        )
        val txId = transactionDao.insertTransaction(newTx)
        scope.launch { sync.pushInsert("transactions", sync.toJson(newTx.copy(id = txId.toInt()))) }

        // Add automatic system message to tournament chat
        val sysMsg = ChatMessage(
            chatRoomId = "tournament_$tournamentId",
            senderId = 0, // System
            senderUsername = "Raival System",
            message = "🎮 $registrationPlayerName has registered for ${tournament.title}!"
        )
        val msgId = chatMessageDao.insertMessage(sysMsg)
        scope.launch { sync.pushInsert("chat_messages", sync.toJson(sysMsg.copy(id = msgId.toInt()))) }

        return true
    }

    // Deposit or Withdrawal MoMo Flows
    suspend fun requestWithdrawal(amount: Double, paymentMethod: String, phone: String): Boolean {
        if (BuildConfig.IS_DEMO_MODE) {
            android.util.Log.w("RaivalRepository", "Demo mode: Withdrawal blocked")
            return false
        }
        val user = _currentUser.value ?: return false
        if (user.balance < amount || amount < 20.0) return false

        val updatedUser = user.copy(
            balance = user.balance - amount
        )
        userDao.updateUser(updatedUser)
        scope.launch { sync.pushUpdate("users", updatedUser.id, sync.toJson(updatedUser)) }
        _currentUser.value = updatedUser

        val txRef = "WTH-${UUID.randomUUID().toString().take(8).uppercase()}"
        val tx = MoMoTransaction(
            reference = txRef,
            userId = user.id,
            tournamentId = 0,
            tournamentTitle = "Withdrawal to $paymentMethod",
            amount = amount,
            paymentMethod = paymentMethod,
            phone = phone,
            status = "success",
            type = "WITHDRAWAL"
        )
        val txId = transactionDao.insertTransaction(tx)
        scope.launch { sync.pushInsert("transactions", sync.toJson(tx.copy(id = txId.toInt()))) }
        return true
    }

    suspend fun requestDeposit(amount: Double, paymentMethod: String, phone: String): Boolean {
        if (BuildConfig.IS_DEMO_MODE) {
            android.util.Log.w("RaivalRepository", "Demo mode: Deposit blocked")
            return false
        }
        val user = _currentUser.value ?: return false
        if (amount <= 0) return false

        if (paymentMethod == "COIN REFILL" && user.coinsClaimedFromTopUp) {
            return false
        }

        val updatedUser = user.copy(
            coinBalance = user.coinBalance + amount.toInt(),
            coinsClaimedFromTopUp = if (paymentMethod == "COIN REFILL") true else user.coinsClaimedFromTopUp
        )
        userDao.updateUser(updatedUser)
        scope.launch { sync.pushUpdate("users", updatedUser.id, sync.toJson(updatedUser)) }
        _currentUser.value = updatedUser

        val txRef = "DEP-${UUID.randomUUID().toString().take(8).uppercase()}"
        val tx = MoMoTransaction(
            reference = txRef,
            userId = user.id,
            tournamentId = 0,
            tournamentTitle = "Coins Claimed",
            amount = amount,
            paymentMethod = paymentMethod,
            phone = phone,
            status = "success",
            type = "DEPOSIT"
        )
        val txId = transactionDao.insertTransaction(tx)
        scope.launch { sync.pushInsert("transactions", sync.toJson(tx.copy(id = txId.toInt()))) }
        return true
    }

    fun getTransactionsForUser(userId: Int): Flow<List<MoMoTransaction>> =
        transactionDao.getTransactionsForUser(userId)

    fun getAllTransactions(): Flow<List<MoMoTransaction>> =
        transactionDao.getAllTransactions()

    // Leaders & Rankings
    val allUsersByPoints: Flow<List<User>> = userDao.getAllUsersByPoints()

    suspend fun getAllUsersOneShot(): List<User> = userDao.getAllUsersOneShot()

    suspend fun banUser(userId: Int, ban: Boolean) {
        val user = userDao.getUserByIdOneShot(userId) ?: return
        val updated = user.copy(status = if (ban) "banned" else "active")
        userDao.updateUser(updated)
        scope.launch { sync.pushUpdate("users", updated.id, sync.toJson(updated)) }
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updated
        }
    }

    // Teams
    val allTeams: Flow<List<Team>> = teamDao.getAllTeams()

    fun getTeamById(id: Int): Flow<Team?> = teamDao.getTeamById(id)

    suspend fun createTeam(name: String, description: String, tags: String): Long {
        val user = _currentUser.value ?: return -1
        val team = Team(
            name = name,
            description = description,
            leader = user.username,
            members = user.username,
            tags = tags,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
            isPublic = true
        )
        val id = teamDao.insertTeam(team)
        scope.launch { sync.pushInsert("teams", sync.toJson(team.copy(id = id.toInt()))) }
        return id
    }

    suspend fun updateTeam(team: Team) {
        teamDao.updateTeam(team)
        scope.launch { sync.pushUpdate("teams", team.id, sync.toJson(team)) }
    }

    // Chat
    fun getChatMessages(chatRoomId: String): Flow<List<ChatMessage>> =
        chatMessageDao.getMessagesForRoom(chatRoomId)

    suspend fun sendChatMessage(chatRoomId: String, messageText: String, imageUrl: String? = null): Boolean {
        val user = _currentUser.value ?: return false
        var senderUsername = user.username
        
        // Sanitize input — strip HTML/script tags
        val sanitizedMessage = messageText
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("[<>\"';&]"), "")
            .trim()
            .take(500) // Limit message length
        
        try {
            val tournamentId = if (chatRoomId.startsWith("tournament_")) {
                chatRoomId.substringAfter("tournament_").toIntOrNull()
            } else if (chatRoomId.startsWith("match_chat_")) {
                val matchId = chatRoomId.substringAfter("match_chat_").toIntOrNull()
                if (matchId != null) {
                    val tMatch = tournamentMatchDao.getMatchByIdOneShot(matchId)
                    tMatch?.tournamentId
                } else null
            } else {
                null
            }
            if (tournamentId != null) {
                val reg = registrationDao.getRegistrationByUserAndTournament(user.id, tournamentId)
                if (reg != null && reg.playerName.isNotEmpty()) {
                    senderUsername = reg.playerName
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("RaivalRepository", "sendChatMessage username lookup failed", e)
        }

        val msg = ChatMessage(
            chatRoomId = chatRoomId,
            senderId = user.id,
            senderUsername = senderUsername,
            message = sanitizedMessage,
            imageUrl = imageUrl
        )
        val msgId = chatMessageDao.insertMessage(msg)
        scope.launch { sync.pushInsert("chat_messages", sync.toJson(msg.copy(id = msgId.toInt()))) }
        return true
    }

    // Match sessions (Friendly & Quick pairing)
    val allMatchSessions: Flow<List<MatchSession>> = matchSessionDao.getAllMatchSessions()

    fun getMatchSessionById(id: Int): Flow<MatchSession?> = matchSessionDao.getMatchSessionById(id)

    suspend fun createMatchSession(
        hostId: Int,
        hostUsername: String,
        halfLength: Int,
        difficulty: String,
        stadium: String,
        isFriendly: Boolean
    ): MatchSession {
        val roomCode = PasswordHelper.generateRoomCode()
        val session = MatchSession(
            roomCode = roomCode,
            hostId = hostId,
            hostUsername = hostUsername,
            guestId = null,
            guestUsername = null,
            status = "Waiting",
            halfLength = halfLength,
            difficulty = difficulty,
            stadium = stadium,
            isFriendly = isFriendly
        )
        val id = matchSessionDao.insertMatchSession(session)
        val created = session.copy(id = id.toInt())
        scope.launch { sync.pushInsert("match_sessions", sync.toJson(created)) }
        return created
    }

    suspend fun joinMatchSession(roomCode: String, guestId: Int, guestUsername: String): MatchSession? {
        val session = matchSessionDao.getMatchSessionByRoomCode(roomCode) ?: return null
        if (session.status != "Waiting" || session.hostId == guestId) return null
        val updated = session.copy(
            guestId = guestId,
            guestUsername = guestUsername,
            status = "Ready"
        )
        matchSessionDao.updateMatchSession(updated)
        scope.launch { sync.pushUpdate("match_sessions", updated.id, sync.toJson(updated)) }
        
        val sysMsg = ChatMessage(
            chatRoomId = "match_${session.roomCode}",
            senderId = 0,
            senderUsername = "Raival System",
            message = "🎮 $guestUsername entered the lobby! Prepare your team lineups. Ready to play Dream League Soccer!"
        )
        val msgId = chatMessageDao.insertMessage(sysMsg)
        scope.launch { sync.pushInsert("chat_messages", sync.toJson(sysMsg.copy(id = msgId.toInt()))) }
        return updated
    }

    suspend fun confirmMatchStart(sessionId: Int): Boolean {
        val session = matchSessionDao.getMatchSessionByIdOneShot(sessionId) ?: return false
        val updated = session.copy(status = "Playing")
        matchSessionDao.updateMatchSession(updated)
        scope.launch { sync.pushUpdate("match_sessions", updated.id, sync.toJson(updated)) }
        return true
    }

    suspend fun submitMatchScore(
        sessionId: Int,
        userId: Int,
        score: Int,
        screenshot: String? = null
    ): MatchSession? {
        val session = matchSessionDao.getMatchSessionByIdOneShot(sessionId) ?: return null
        val updated = if (session.hostId == userId) {
            session.copy(hostScore = score, hostScreenshot = screenshot)
        } else if (session.guestId == userId) {
            session.copy(guestScore = score, guestScreenshot = screenshot)
        } else {
            return null
        }

        // If both players have submitted score
        val finalSession = if (updated.hostScore != null && updated.guestScore != null) {
            if (updated.hostScore == updated.guestScore) {
                // Scores match -> Automatic acceptance!
                val winnerId = if (updated.hostScore > updated.guestScore) {
                    updated.hostId
                } else if (updated.guestScore > updated.hostScore) {
                    updated.guestId
                } else {
                    null // Draw
                }

                if (winnerId != null) {
                    val winnerUser = userDao.getUserByIdOneShot(winnerId)
                    if (winnerUser != null) {
                        // Reward winner 5 coins
                        val updatedWinner = winnerUser.copy(
                            coinBalance = winnerUser.coinBalance + 5,
                            wins = winnerUser.wins + 1,
                            winStreak = winnerUser.winStreak + 1
                        )
                        userDao.updateUser(updatedWinner)
                        scope.launch { sync.pushUpdate("users", updatedWinner.id, sync.toJson(updatedWinner)) }
                        if (_currentUser.value?.id == winnerId) {
                            _currentUser.value = updatedWinner
                        }
                    }

                    // Loser update
                    val loserId = if (winnerId == updated.hostId) updated.guestId else updated.hostId
                    if (loserId != null) {
                        val loserUser = userDao.getUserByIdOneShot(loserId)
                        if (loserUser != null) {
                            val updatedLoser = loserUser.copy(
                                losses = loserUser.losses + 1,
                                winStreak = 0
                            )
                            userDao.updateUser(updatedLoser)
                            scope.launch { sync.pushUpdate("users", updatedLoser.id, sync.toJson(updatedLoser)) }
                            if (_currentUser.value?.id == loserId) {
                                _currentUser.value = updatedLoser
                            }
                        }
                    }
                }
                updated.copy(status = "Completed", winnerId = winnerId)
            } else {
                // Scores differ -> Disputed status for manual admin verification
                updated.copy(status = "Disputed")
            }
        } else {
            // One player submitted, waiting for opponent's submission
            updated.copy(status = "Verification")
        }

        matchSessionDao.updateMatchSession(finalSession)
        scope.launch { sync.pushUpdate("match_sessions", finalSession.id, sync.toJson(finalSession)) }
        return finalSession
    }

    suspend fun adminResolveDispute(sessionId: Int, winnerId: Int): Boolean {
        val session = matchSessionDao.getMatchSessionByIdOneShot(sessionId) ?: return false
        val updated = session.copy(status = "Completed", winnerId = winnerId)
        matchSessionDao.updateMatchSession(updated)
        scope.launch { sync.pushUpdate("match_sessions", updated.id, sync.toJson(updated)) }

        // Award winner coins
        val winnerUser = userDao.getUserByIdOneShot(winnerId)
        if (winnerUser != null) {
            val updatedWinner = winnerUser.copy(
                coinBalance = winnerUser.coinBalance + 5,
                wins = winnerUser.wins + 1,
                winStreak = winnerUser.winStreak + 1
            )
            userDao.updateUser(updatedWinner)
            scope.launch { sync.pushUpdate("users", updatedWinner.id, sync.toJson(updatedWinner)) }
            if (_currentUser.value?.id == winnerId) {
                _currentUser.value = updatedWinner
            }
        }

        // Award loser
        val loserId = if (winnerId == session.hostId) session.guestId else session.hostId
        if (loserId != null) {
            val loserUser = userDao.getUserByIdOneShot(loserId)
            if (loserUser != null) {
                val updatedLoser = loserUser.copy(
                    losses = loserUser.losses + 1,
                    winStreak = 0
                )
                userDao.updateUser(updatedLoser)
                scope.launch { sync.pushUpdate("users", updatedLoser.id, sync.toJson(updatedLoser)) }
                if (_currentUser.value?.id == loserId) {
                    _currentUser.value = updatedLoser
                }
            }
        }
        return true
    }

    suspend fun addCoins(userId: Int, amount: Int): Boolean {
        if (BuildConfig.IS_DEMO_MODE) {
            android.util.Log.w("RaivalRepository", "Demo mode: addCoins blocked")
            return false
        }
        val user = userDao.getUserByIdOneShot(userId) ?: return false
        val updated = user.copy(coinBalance = user.coinBalance + amount)
        userDao.updateUser(updated)
        scope.launch { sync.pushUpdate("users", updated.id, sync.toJson(updated)) }
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updated
        }
        return true
    }

    suspend fun spendCoins(userId: Int, amount: Int): Boolean {
        if (BuildConfig.IS_DEMO_MODE) {
            android.util.Log.w("RaivalRepository", "Demo mode: spendCoins blocked")
            return false
        }
        val user = userDao.getUserByIdOneShot(userId) ?: return false
        if (user.coinBalance < amount) return false
        val updated = user.copy(coinBalance = user.coinBalance - amount)
        userDao.updateUser(updated)
        scope.launch { sync.pushUpdate("users", updated.id, sync.toJson(updated)) }
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updated
        }
        return true
    }

    // DB Pre-population helper - SEEDS ONLY CODM BATTLE ROYALE TOURNAMENTS
    suspend fun populateInitialData() {
        val existingUsers = userDao.getAllUsersOneShot()
        if (existingUsers.isEmpty()) {
            // Seed Admin (with hashed password — default: "admin123", change on first login)
            val adminSalt = PasswordHelper.generateSalt()
            val adminUser = User(
                username = "Admin",
                fullName = "Raival Admin",
                email = "admin@raival.com",
                phone = "0240000000",
                role = "player",
                balance = 1000.0,
                coinBalance = 5000,
                raivalPoints = 1200,
                ghanaRegion = "Greater Accra",
                ghanaHometown = "Accra",
                preferredGame = "Dream League Soccer",
                passwordHash = PasswordHelper.hashPassword("admin123", adminSalt),
                passwordSalt = adminSalt
            )
            val adminId = userDao.insertUser(adminUser)

            // Seed Akwasi_Gamer
            val gamerUser = User(
                username = "Akwasi_Gamer",
                fullName = "Akwasi Mensah",
                email = "akwasi@raival.com",
                phone = "0241234567",
                role = "player",
                balance = 150.0,
                coinBalance = 1500,
                raivalPoints = 850, // Let's give him a solid mid rank!
                ghanaRegion = "Ashanti",
                ghanaHometown = "Kumasi",
                preferredGame = "eFootball"
            )
            userDao.insertUser(gamerUser)

            // Seed AppConfig
            val defaultConfig = AppConfig(
                id = 1,
                economyMode = "Coin-Only",
                hubtelClientId = "",
                hubtelClientSecret = "",
                hubtelMerchantId = "",
                hubtelApiUrl = "https://api.hubtel.com/v1/",
                hubtelPaymentMethods = "momo, vodafone",
                hubtelMinAmount = 1.0,
                hubtelMaxAmount = 500.0,
                hubtelFeePercentage = 2.0,
                hubtelAutoConfirm = true,
                hubtelWebhookUrl = "https://raival.com/webhook/hubtel",
                coinsResetToZeroDone = false
            )
            appConfigDao.insertOrUpdate(defaultConfig)

            // Seed default tournaments with 2, 4, 8, 16, and 32 member capacities if empty
            if (tournamentDao.getAllTournamentsOneShot().isEmpty()) {
                val seedTournaments = listOf(
                    Tournament(
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
                    ),
                    Tournament(
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
                    ),
                    Tournament(
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
                    ),
                    Tournament(
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
                    ),
                    Tournament(
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
                    ),
                    Tournament(
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
                    ),
                    Tournament(
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
                    ),
                    Tournament(
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
                    ),
                    Tournament(
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
                    ),
                    Tournament(
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
                )
                seedTournaments.forEach { tournamentDao.insertTournament(it) }
            }
            
            // Only seed demo data if Supabase is unreachable (offline-first mode)
            val isOnline = try {
                SupabaseSyncManager.pullTable("users").isNotEmpty()
                true
            } catch (_: Exception) { false }

            if (!isOnline) {
            // Seed Teams
            val team1 = Team(
                name = "Ghana DLS Elite Clan",
                description = "Only DLS top tier mobile players from Kumasi and Accra competing in Dream League.",
                memberCount = 1,
                maxMembers = 10,
                wins = 12,
                leader = "Admin",
                members = "Admin",
                tags = "DLS,Elite,TikiTaka",
                createdAt = "2026-01-15",
                isPublic = true
            )
            teamDao.insertTeam(team1)

            // Seed Marketplace Users
            val sellerGamer = User(
                username = "GamerPro_23",
                fullName = "Kofi GamerPro",
                email = "gamerpro@raival.com",
                phone = "0551112222",
                role = "player",
                balance = 100.0,
                coinBalance = 2000,
                raivalPoints = 980,
                ghanaRegion = "Greater Accra",
                ghanaHometown = "Tema",
                preferredGame = "Dream League Soccer"
            )
            val sellerGamerId = userDao.insertUser(sellerGamer).toInt()

            val sellerPlayerX = User(
                username = "PlayerX_007",
                fullName = "Emmanuel PlayerX",
                email = "playerx@raival.com",
                phone = "0551234567",
                role = "player",
                balance = 200.0,
                coinBalance = 3000,
                raivalPoints = 720,
                ghanaRegion = "Western",
                ghanaHometown = "Takoradi",
                preferredGame = "FC Mobile"
            )
            val sellerPlayerXId = userDao.insertUser(sellerPlayerX).toInt()

            val buyerKing = User(
                username = "SkillKing_1",
                fullName = "Prince SkillKing",
                email = "skillking@raival.com",
                phone = "0559998888",
                role = "player",
                balance = 300.0,
                coinBalance = 4000,
                raivalPoints = 1100,
                ghanaRegion = "Northern",
                ghanaHometown = "Tamale",
                preferredGame = "Dream League Soccer"
            )
            val buyerKingId = userDao.insertUser(buyerKing).toInt()

            // Seed Marketplace Listings
            val l1 = MarketplaceListing(
                sellerId = sellerGamerId,
                sellerName = "GamerPro_23",
                sellerRating = 4.8f,
                game = "eFootball",
                teamName = "FC Barcelona",
                ovrRating = 95,
                players = "⭐ CF - Messi (94 OVR)\n⭐ CM - Iniesta (92 OVR)\n⭐ CM - Xavi (91 OVR)\n⭐ RW - Neymar (90 OVR)\n⭐ CB - Pique (88 OVR)",
                specialCardsCount = 5,
                coins = 150000,
                price = 150.0,
                listingType = "Negotiable",
                description = "Super clean Barca past & present squad. Messi, Xavi, Iniesta, Neymar all maxed out! Selling because I want to build a Madrid theme squad.",
                status = "Active",
                views = 5,
                ratingType = "OVR",
                playerScreenshots = "Messi_card.jpg, Iniesta_card.jpg, Neymar_card.jpg"
            )
            val l1Id = marketplaceListingDao.insertListing(l1).toInt()

            val l2 = MarketplaceListing(
                sellerId = sellerPlayerXId,
                sellerName = "PlayerX_007",
                sellerRating = 4.5f,
                game = "FC Mobile",
                teamName = "Real Madrid",
                ovrRating = 92,
                players = "⭐ CF - Ronaldo (93 OVR)\n⭐ CF - Benzema (91 OVR)\n⭐ CM - Modric (90 OVR)\n⭐ CB - Ramos (89 OVR)",
                specialCardsCount = 3,
                coins = 100000,
                price = 120.0,
                listingType = "Fixed Price",
                description = "Real Madrid legacy squad featuring prime CR7. Solid defense and midfield, perfect for division rivals.",
                status = "Active",
                views = 12,
                ratingType = "Collective Strength",
                playerScreenshots = "Ronaldo_card.jpg, Modric_card.jpg"
            )
            val l2Id = marketplaceListingDao.insertListing(l2).toInt()

            val l3 = MarketplaceListing(
                sellerId = sellerGamerId,
                sellerName = "GamerPro_23",
                sellerRating = 4.8f,
                game = "DLS",
                teamName = "Ghana Black Stars",
                ovrRating = 88,
                players = "⭐ CF - Gyan (89 OVR)\n⭐ LM - Atsu (86 OVR)\n⭐ RM - Ayew (85 OVR)\n⭐ CB - Amartey (84 OVR)",
                specialCardsCount = 2,
                coins = 50000,
                price = 75.0,
                listingType = "Negotiable",
                description = "Full Ghana Black Stars national squad in DLS. Fully upgraded stadium, high fitness and acceleration stats.",
                status = "Active",
                views = 3,
                ratingType = "Team Rating",
                playerScreenshots = "Gyan_card.jpg, Atsu_card.jpg, Ayew_card.jpg"
            )
            val l3Id = marketplaceListingDao.insertListing(l3).toInt()

            // Seed Marketplace Offers/Inquiries
            marketplaceOfferDao.insertOffer(
                MarketplaceOffer(
                    listingId = l1Id,
                    buyerId = sellerPlayerXId,
                    buyerName = "PlayerX_007",
                    offerAmount = 130.0,
                    status = "Chatting"
                )
            )

            marketplaceOfferDao.insertOffer(
                MarketplaceOffer(
                    listingId = l1Id,
                    buyerId = buyerKingId,
                    buyerName = "SkillKing_1",
                    offerAmount = 140.0,
                    status = "Accepted"
                )
            )

            // Seed Some chat messages for Listing 1 Chatroom between GamerPro_23 and PlayerX_007
            val chatRoomId = "marketplace_${l1Id}_buyer_${sellerPlayerXId}"
            chatMessageDao.insertMessage(
                ChatMessage(
                    chatRoomId = chatRoomId,
                    senderId = sellerPlayerXId,
                    senderUsername = "PlayerX_007",
                    message = "Hi, I'm interested in your team. Is GHS 130 acceptable?"
                )
            )
            chatMessageDao.insertMessage(
                ChatMessage(
                    chatRoomId = chatRoomId,
                    senderId = sellerGamerId,
                    senderUsername = "GamerPro_23",
                    message = "Hi! Thanks for your interest. The team has Messi at 94 OVR. I can do GHS 140 minimum."
                )
            )
            } // end if (!isOnline)
        }

    }

    // Marketplace Listings
    val activeListings: Flow<List<MarketplaceListing>> = marketplaceListingDao.getActiveListings()
    val allListings: Flow<List<MarketplaceListing>> = marketplaceListingDao.getAllListings()
    
    fun getListingById(id: Int): Flow<MarketplaceListing?> = marketplaceListingDao.getListingById(id)
    suspend fun getListingByIdOneShot(id: Int): MarketplaceListing? = marketplaceListingDao.getListingByIdOneShot(id)
    fun getListingsBySeller(sellerId: Int): Flow<List<MarketplaceListing>> = marketplaceListingDao.getListingsBySeller(sellerId)

    suspend fun createListing(listing: MarketplaceListing): Long {
        val id = marketplaceListingDao.insertListing(listing)
        scope.launch { sync.pushInsert("marketplace_listings", sync.toJson(listing.copy(id = id.toInt()))) }
        return id
    }
    suspend fun updateListing(listing: MarketplaceListing) {
        marketplaceListingDao.updateListing(listing)
        scope.launch { sync.pushUpdate("marketplace_listings", listing.id, sync.toJson(listing)) }
    }
    suspend fun deleteListing(listing: MarketplaceListing) {
        marketplaceListingDao.deleteListing(listing)
        scope.launch { sync.pushDelete("marketplace_listings", listing.id) }
    }

    // Marketplace Offers
    fun getOffersForListing(listingId: Int): Flow<List<MarketplaceOffer>> = marketplaceOfferDao.getOffersForListing(listingId)
    fun getOffersByBuyer(buyerId: Int): Flow<List<MarketplaceOffer>> = marketplaceOfferDao.getOffersByBuyer(buyerId)
    
    suspend fun makeOffer(offer: MarketplaceOffer): Long {
        val id = marketplaceOfferDao.insertOffer(offer)
        scope.launch { sync.pushInsert("marketplace_offers", sync.toJson(offer.copy(id = id.toInt()))) }
        return id
    }
    suspend fun updateOffer(offer: MarketplaceOffer) {
        marketplaceOfferDao.updateOffer(offer)
        scope.launch { sync.pushUpdate("marketplace_offers", offer.id, sync.toJson(offer)) }
    }
    suspend fun deleteOffersForListing(listingId: Int) = marketplaceOfferDao.deleteOffersForListing(listingId)

    // Marketplace Transactions (Escrow)
    val allMarketplaceTransactions: Flow<List<MarketplaceTransaction>> = marketplaceTransactionDao.getAllTransactions()
    fun getMarketplaceTransactionsForUser(userId: Int): Flow<List<MarketplaceTransaction>> = marketplaceTransactionDao.getTransactionsForUser(userId)
    
    fun getMarketplaceTransactionById(id: Int): Flow<MarketplaceTransaction?> = marketplaceTransactionDao.getTransactionById(id)
    suspend fun getMarketplaceTransactionByIdOneShot(id: Int): MarketplaceTransaction? = marketplaceTransactionDao.getTransactionByIdOneShot(id)
    
    suspend fun createMarketplaceTransaction(transaction: MarketplaceTransaction): Long {
        val id = marketplaceTransactionDao.insertTransaction(transaction)
        scope.launch { sync.pushInsert("marketplace_transactions", sync.toJson(transaction.copy(id = id.toInt()))) }
        return id
    }
    suspend fun updateMarketplaceTransaction(transaction: MarketplaceTransaction) {
        marketplaceTransactionDao.updateTransaction(transaction)
        scope.launch { sync.pushUpdate("marketplace_transactions", transaction.id, sync.toJson(transaction)) }
    }

    // Community Feed Methods
    val allCommunityPosts: Flow<List<CommunityPost>> = communityPostDao.getAllPosts()

    suspend fun getCommunityPostById(id: Int): CommunityPost? = communityPostDao.getPostById(id)
    
    suspend fun createCommunityPost(post: CommunityPost): Long {
        val id = communityPostDao.insertPost(post).toInt()
        scope.launch { sync.pushInsert("community_posts", sync.toJson(post.copy(id = id))) }
        return id.toLong()
    }
    
    suspend fun updateCommunityPost(post: CommunityPost) {
        communityPostDao.updatePost(post)
        scope.launch { sync.pushUpdate("community_posts", post.id, sync.toJson(post)) }
    }
    
    suspend fun deleteCommunityPost(id: Int) {
        communityPostDao.deletePostById(id)
        communityCommentDao.deleteCommentsForPost(id)
        scope.launch { sync.pushDelete("community_posts", id) }
    }
    
    fun getCommentsForPost(postId: Int): Flow<List<CommunityComment>> = communityCommentDao.getCommentsForPost(postId)

    suspend fun addCommentToPost(comment: CommunityComment): Long {
        val id = communityCommentDao.insertComment(comment).toInt()
        scope.launch { sync.pushInsert("community_comments", sync.toJson(comment.copy(id = id))) }
        return id.toLong()
    }
}

