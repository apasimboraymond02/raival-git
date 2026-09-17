package com.example.data.sync

import com.example.data.SupabaseConfig
import com.example.data.model.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.io.File

object SupabaseSyncManager {

    private fun JsonObjectBuilder.putNull(key: String) {
        put(key, JsonNull)
    }

    // ─── Generic helpers ──────────────────────────────────────────
    suspend fun pullTable(table: String): List<JsonObject> {
        return SupabaseConfig.client.postgrest[table].select().decodeList<JsonObject>()
    }

    suspend fun pullTableRequired(table: String): List<JsonObject> {
        return SupabaseConfig.client.postgrest[table].select().decodeList<JsonObject>()
    }

    suspend fun pushInsert(table: String, data: JsonObject): JsonObject? {
        return try {
            SupabaseConfig.client.postgrest[table].insert(data).decodeSingle<JsonObject>()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseSync", "pushInsert failed: $table", e)
            null
        }
    }

    suspend fun pushUpdate(table: String, id: Int, data: JsonObject) {
        try {
            SupabaseConfig.client.postgrest[table].update(data) { filter { eq("id", id) } }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseSync", "pushUpdate failed: $table id=$id", e)
        }
    }

    suspend fun pushDelete(table: String, id: Int) {
        try {
            SupabaseConfig.client.postgrest[table].delete { filter { eq("id", id) } }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseSync", "pushDelete failed: $table id=$id", e)
        }
    }

    // ─── Realtime subscription ────────────────────────────────────
    private val channels = mutableListOf<RealtimeChannel>()

    fun subscribeToTable(
        table: String,
        scope: CoroutineScope,
        onInsert: suspend (JsonObject) -> Unit,
        onUpdate: suspend (JsonObject) -> Unit,
        onDelete: suspend (JsonObject) -> Unit
    ) {
        scope.launch {
            try {
                val channel = SupabaseConfig.client.realtime.channel("raival-$table") {}
                channels.add(channel)
                channel.subscribe()
                channel.postgresChangeFlow<PostgresAction>(
                    schema = "public"
                ) {
                    this.table = table
                }.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> scope.launch { onInsert(action.record) }
                        is PostgresAction.Update -> scope.launch { onUpdate(action.record) }
                        is PostgresAction.Delete -> scope.launch { onDelete(action.oldRecord) }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseSync", "subscribeToTable failed: $table", e)
            }
        }
    }

    suspend fun unsubscribeAll() {
        channels.clear()
    }

    // ════════════════════════════════════════════════════════════════
    //  ENTITY CONVERTERS
    // ════════════════════════════════════════════════════════════════

    // ─── helpers ───
    private fun JsonObject.findPrimitive(vararg keys: String): JsonPrimitive? {
        for (k in keys) {
            this[k]?.jsonPrimitive?.let { return it }
            this[k.lowercase()]?.jsonPrimitive?.let { return it }
        }
        return null
    }

    private fun JsonObject.s(vararg keys: String): String {
        return findPrimitive(*keys)?.contentOrNull ?: ""
    }

    private fun JsonObject.i(vararg keys: String): Int {
        val p = findPrimitive(*keys) ?: return 0
        return p.intOrNull ?: p.contentOrNull?.toIntOrNull() ?: 0
    }

    private fun JsonObject.l(vararg keys: String): Long {
        val p = findPrimitive(*keys) ?: return 0L
        return p.longOrNull ?: p.contentOrNull?.toLongOrNull() ?: 0L
    }

    private fun JsonObject.d(vararg keys: String): Double {
        val p = findPrimitive(*keys) ?: return 0.0
        return p.doubleOrNull ?: p.contentOrNull?.toDoubleOrNull() ?: 0.0
    }

    private fun JsonObject.b(vararg keys: String): Boolean {
        val p = findPrimitive(*keys) ?: return false
        return p.booleanOrNull ?: (p.contentOrNull?.equals("true", ignoreCase = true) == true)
    }

    private fun JsonObject.f(vararg keys: String): Float {
        val p = findPrimitive(*keys) ?: return 0f
        return p.floatOrNull ?: p.contentOrNull?.toFloatOrNull() ?: 0f
    }

    // ─── User ─────────────────────────────────────────────────────
    fun toJson(u: User) = buildJsonObject {
        put("id", u.id); put("username", u.username); put("fullName", u.fullName)
        put("email", u.email); put("phone", u.phone); put("bio", u.bio)
        put("paymentMethod", u.paymentMethod); put("raivalPoints", u.raivalPoints)
        put("balance", u.balance); put("coinBalance", u.coinBalance)
        put("totalWinnings", u.totalWinnings); put("tournamentsPlayed", u.tournamentsPlayed)
        put("wins", u.wins); put("losses", u.losses); put("winStreak", u.winStreak)
        put("dailyLoginClaimedAt", u.dailyLoginClaimedAt); put("role", u.role)
        put("status", u.status); put("createdAt", u.createdAt)
        put("dlsHandle", u.dlsHandle); put("efootballHandle", u.efootballHandle)
        put("discordHandle", u.discordHandle); put("xp", u.xp); put("level", u.level)
        put("streakShields", u.streakShields); put("referralCode", u.referralCode)
        put("selectedAvatar", u.selectedAvatar); put("customTitle", u.customTitle)
        put("clanTag", u.clanTag); put("nameColor", u.nameColor); put("nameGlow", u.nameGlow)
        put("profileTheme", u.profileTheme)
        put("ghanaRegion", u.ghanaRegion); put("ghanaHometown", u.ghanaHometown)
        put("preferredGame", u.preferredGame)
        put("authUid", u.authUid); put("proLicenseExpiresAt", u.proLicenseExpiresAt)
    }

    fun toUser(obj: JsonObject) = User(
        id = obj.i("id"), username = obj.s("username"), fullName = obj.s("fullName"),
        email = obj.s("email"), phone = obj.s("phone"), bio = obj.s("bio"),
        paymentMethod = obj.s("paymentMethod"), raivalPoints = obj.i("raivalPoints"),
        balance = obj.d("balance"), coinBalance = obj.i("coinBalance"),
        totalWinnings = obj.d("totalWinnings"), tournamentsPlayed = obj.i("tournamentsPlayed"),
        wins = obj.i("wins"), losses = obj.i("losses"), winStreak = obj.i("winStreak"),
        dailyLoginClaimedAt = obj.l("dailyLoginClaimedAt"), role = obj.s("role"),
        status = obj.s("status"), createdAt = obj.l("createdAt"),
        dlsHandle = obj.s("dlsHandle"), efootballHandle = obj.s("efootballHandle"),
        discordHandle = obj.s("discordHandle"), xp = obj.i("xp"), level = obj.i("level"),
        streakShields = obj.i("streakShields"), referralCode = obj.s("referralCode"),
        unlockedSkills = obj.s("unlockedSkills"), completedMissions = obj.s("completedMissions"),
        selectedAvatar = obj.s("selectedAvatar"), customTitle = obj.s("customTitle"),
        clanTag = obj.s("clanTag"), nameColor = obj.s("nameColor"), nameGlow = obj.b("nameGlow"),
        profileTheme = obj.s("profileTheme"), unlockedThemes = obj.s("unlockedThemes"),
        unlockedAvatars = obj.s("unlockedAvatars"), welcomeBonusClaimed = obj.b("welcomeBonusClaimed"),
        socialFbConnected = obj.b("socialFbConnected"), socialTwConnected = obj.b("socialTwConnected"),
        socialAppleConnected = obj.b("socialAppleConnected"), appRated = false,
        referredActiveCount = obj.i("referredActiveCount"), coinsClaimedFromTopUp = obj.b("coinsClaimedFromTopUp"),
        ghanaRegion = obj.s("ghanaRegion"), ghanaHometown = obj.s("ghanaHometown"),
        preferredGame = obj.s("preferredGame"),
        passwordHash = obj.s("passwordHash"), passwordSalt = obj.s("passwordSalt"),
        authUid = obj.s("authUid"), proLicenseExpiresAt = obj.l("proLicenseExpiresAt")
    )

    // ─── Tournament ──────────────────────────────────────────────
    fun toJson(t: Tournament) = buildJsonObject {
        put("id", t.id); put("title", t.title); put("game", t.game)
        put("entryfee", t.entryFee); put("coinentryfee", t.coinEntryFee)
        put("prize", t.prize); put("coinprize", t.coinPrize)
        put("players", t.players); put("maxplayers", t.maxPlayers)
        put("date", t.date); put("time", t.time); put("status", t.status)
        put("description", t.description); put("mode", t.mode); put("map", t.map)
        put("rules", t.rules); put("schedule", t.schedule)
        put("prizedistribution", t.prizeDistribution); put("contact", t.contact)
        put("banner", t.banner); put("organizer", t.organizer); put("format", t.format)
        put("style", t.style); put("ishidden", t.isHidden); put("isautohosted", t.isAutoHosted)
    }

    fun toTournament(obj: JsonObject) = Tournament(
        id = obj.i("id"), title = obj.s("title"), game = obj.s("game"),
        entryFee = obj.d("entryFee", "entryfee"), coinEntryFee = obj.i("coinEntryFee", "coinentryfee"),
        prize = obj.d("prize"), coinPrize = obj.i("coinPrize", "coinprize"),
        players = obj.i("players"), maxPlayers = obj.i("maxPlayers", "maxplayers"),
        date = obj.s("date"), time = obj.s("time"), status = obj.s("status"),
        description = obj.s("description"), mode = obj.s("mode"), map = obj.s("map"),
        rules = obj.s("rules"), schedule = obj.s("schedule"),
        prizeDistribution = obj.s("prizeDistribution", "prizedistribution"), contact = obj.s("contact"),
        banner = obj.s("banner"), organizer = obj.s("organizer"), format = obj.s("format"),
        style = obj.s("style"), isHidden = obj.b("isHidden", "ishidden"), isAutoHosted = obj.b("isAutoHosted", "isautohosted")
    )

    // ─── Registration ────────────────────────────────────────────
    fun toJson(r: Registration) = buildJsonObject {
        put("id", r.id); put("userId", r.userId); put("tournamentId", r.tournamentId)
        put("playerName", r.playerName); put("inGameName", r.inGameName)
        put("paymentMethod", r.paymentMethod); put("phone", r.phone)
        put("registeredAt", r.registeredAt); put("teamRating", r.teamRating)
        put("modelTeam", r.modelTeam)
    }

    fun toRegistration(obj: JsonObject) = Registration(
        id = obj.i("id"), userId = obj.i("userId"), tournamentId = obj.i("tournamentId"),
        playerName = obj.s("playerName"), inGameName = obj.s("inGameName"),
        paymentMethod = obj.s("paymentMethod"), phone = obj.s("phone"),
        registeredAt = obj.l("registeredAt"), teamRating = obj.i("teamRating"),
        modelTeam = obj.s("modelTeam")
    )

    // ─── MoMoTransaction ─────────────────────────────────────────
    fun toJson(t: MoMoTransaction) = buildJsonObject {
        put("id", t.id); put("reference", t.reference); put("userId", t.userId)
        put("tournamentId", t.tournamentId); put("tournamentTitle", t.tournamentTitle)
        put("amount", t.amount); put("paymentMethod", t.paymentMethod)
        put("phone", t.phone); put("status", t.status)
        put("timestamp", t.timestamp); put("type", t.type)
    }

    fun toTransaction(obj: JsonObject) = MoMoTransaction(
        id = obj.i("id"), reference = obj.s("reference"), userId = obj.i("userId"),
        tournamentId = obj.i("tournamentId"), tournamentTitle = obj.s("tournamentTitle"),
        amount = obj.d("amount"), paymentMethod = obj.s("paymentMethod"),
        phone = obj.s("phone"), status = obj.s("status"),
        timestamp = obj.l("timestamp"), type = obj.s("type")
    )

    // ─── Team ────────────────────────────────────────────────────
    fun toJson(t: Team) = buildJsonObject {
        put("id", t.id); put("name", t.name); put("description", t.description)
        put("memberCount", t.memberCount); put("maxMembers", t.maxMembers)
        put("wins", t.wins); put("leader", t.leader); put("members", t.members)
        put("tags", t.tags); put("createdAt", t.createdAt); put("isPublic", t.isPublic)
    }

    fun toTeam(obj: JsonObject) = Team(
        id = obj.i("id"), name = obj.s("name"), description = obj.s("description"),
        memberCount = obj.i("memberCount"), maxMembers = obj.i("maxMembers"),
        wins = obj.i("wins"), leader = obj.s("leader"), members = obj.s("members"),
        tags = obj.s("tags"), createdAt = obj.s("createdAt"), isPublic = obj.b("isPublic")
    )

    // ─── ChatMessage ─────────────────────────────────────────────
    fun toJson(m: ChatMessage) = buildJsonObject {
        put("id", m.id); put("chatRoomId", m.chatRoomId); put("senderId", m.senderId)
        put("senderUsername", m.senderUsername); put("message", m.message)
        put("timestamp", m.timestamp)
        m.imageUrl?.let { put("imageUrl", it) } ?: putNull("imageUrl")
    }

    fun toChatMessage(obj: JsonObject) = ChatMessage(
        id = obj.i("id"), chatRoomId = obj.s("chatRoomId"), senderId = obj.i("senderId"),
        senderUsername = obj.s("senderUsername"), message = obj.s("message"),
        timestamp = obj.l("timestamp"), imageUrl = obj["imageUrl"]?.jsonPrimitive?.contentOrNull
    )

    // ─── MatchSession ────────────────────────────────────────────
    fun toJson(s: MatchSession) = buildJsonObject {
        put("id", s.id); put("roomCode", s.roomCode); put("hostId", s.hostId)
        put("hostUsername", s.hostUsername)
        s.guestId?.let { put("guestId", it) } ?: putNull("guestId")
        s.guestUsername?.let { put("guestUsername", it) } ?: putNull("guestUsername")
        put("status", s.status); put("halfLength", s.halfLength)
        put("difficulty", s.difficulty); put("stadium", s.stadium)
        s.hostScore?.let { put("hostScore", it) } ?: putNull("hostScore")
        s.guestScore?.let { put("guestScore", it) } ?: putNull("guestScore")
        s.winnerId?.let { put("winnerId", it) } ?: putNull("winnerId")
        s.hostScreenshot?.let { put("hostScreenshot", it) } ?: putNull("hostScreenshot")
        s.guestScreenshot?.let { put("guestScreenshot", it) } ?: putNull("guestScreenshot")
        put("isFriendly", s.isFriendly); put("timestamp", s.timestamp)
    }

    fun toMatchSession(obj: JsonObject) = MatchSession(
        id = obj.i("id"), roomCode = obj.s("roomCode"), hostId = obj.i("hostId"),
        hostUsername = obj.s("hostUsername"),
        guestId = obj["guestId"]?.jsonPrimitive?.intOrNull,
        guestUsername = obj["guestUsername"]?.jsonPrimitive?.contentOrNull,
        status = obj.s("status"), halfLength = obj.i("halfLength"),
        difficulty = obj.s("difficulty"), stadium = obj.s("stadium"),
        hostScore = obj["hostScore"]?.jsonPrimitive?.intOrNull,
        guestScore = obj["guestScore"]?.jsonPrimitive?.intOrNull,
        winnerId = obj["winnerId"]?.jsonPrimitive?.intOrNull,
        hostScreenshot = obj["hostScreenshot"]?.jsonPrimitive?.contentOrNull,
        guestScreenshot = obj["guestScreenshot"]?.jsonPrimitive?.contentOrNull,
        isFriendly = obj.b("isFriendly"),
        gameType = obj["gametype"]?.jsonPrimitive?.contentOrNull ?: obj["gameType"]?.jsonPrimitive?.contentOrNull ?: "eFootball",
        timestamp = obj.l("timestamp")
    )

    // ─── TournamentMatch ─────────────────────────────────────────
    fun toJson(m: TournamentMatch) = buildJsonObject {
        put("id", m.id); put("tournamentId", m.tournamentId); put("round", m.round)
        put("matchIndex", m.matchIndex); put("player1Name", m.player1Name)
        put("player2Name", m.player2Name)
        m.player1Score?.let { put("player1Score", it) } ?: putNull("player1Score")
        m.player2Score?.let { put("player2Score", it) } ?: putNull("player2Score")
        put("status", m.status); m.winnerName?.let { put("winnerName", it) } ?: putNull("winnerName")
        put("matchDate", m.matchDate); put("matchTime", m.matchTime)
        put("player1Rating", m.player1Rating); put("player2Rating", m.player2Rating)
        put("player1Team", m.player1Team); put("player2Team", m.player2Team)
        put("matchCode", m.matchCode); put("player1CheckedIn", m.player1CheckedIn)
        put("player2CheckedIn", m.player2CheckedIn); put("checkInDeadline", m.checkInDeadline)
        put("player1Disqualified", m.player1Disqualified)
        put("player2Disqualified", m.player2Disqualified); put("matchDeadline", m.matchDeadline)
    }

    fun toTournamentMatch(obj: JsonObject) = TournamentMatch(
        id = obj.i("id"), tournamentId = obj.i("tournamentId"), round = obj.s("round"),
        matchIndex = obj.i("matchIndex"), player1Name = obj.s("player1Name"),
        player2Name = obj.s("player2Name"),
        player1Score = obj["player1Score"]?.jsonPrimitive?.intOrNull,
        player2Score = obj["player2Score"]?.jsonPrimitive?.intOrNull,
        status = obj.s("status"), winnerName = obj["winnerName"]?.jsonPrimitive?.contentOrNull,
        matchDate = obj.s("matchDate"), matchTime = obj.s("matchTime"),
        player1Rating = obj.i("player1Rating"), player2Rating = obj.i("player2Rating"),
        player1Team = obj.s("player1Team"), player2Team = obj.s("player2Team"),
        matchCode = obj.s("matchCode"),
        player1CheckedIn = obj.b("player1CheckedIn"), player2CheckedIn = obj.b("player2CheckedIn"),
        checkInDeadline = obj.l("checkInDeadline"),
        player1Disqualified = obj.b("player1Disqualified"), player2Disqualified = obj.b("player2Disqualified"),
        matchDeadline = obj.l("matchDeadline")
    )

    // ─── AppConfig (Hubtel secrets excluded from sync) ─────────────
    fun toJson(c: AppConfig) = buildJsonObject {
        put("id", c.id); put("economyMode", c.economyMode)
        // Hubtel secrets are NOT synced to cloud — stored locally only
        put("hubtelPaymentMethods", c.hubtelPaymentMethods); put("hubtelMinAmount", c.hubtelMinAmount)
        put("hubtelMaxAmount", c.hubtelMaxAmount); put("hubtelFeePercentage", c.hubtelFeePercentage)
        put("hubtelAutoConfirm", c.hubtelAutoConfirm)
        put("coinsResetToZeroDone", c.coinsResetToZeroDone)
        put("serverAuthoritativeEconomy", c.serverAuthoritativeEconomy)
    }

    fun toAppConfig(obj: JsonObject) = AppConfig(
        id = obj.i("id"), economyMode = obj.s("economyMode"),
        hubtelClientId = obj.s("hubtelClientId"), hubtelClientSecret = obj.s("hubtelClientSecret"),
        hubtelMerchantId = obj.s("hubtelMerchantId"), hubtelApiUrl = obj.s("hubtelApiUrl"),
        hubtelPaymentMethods = obj.s("hubtelPaymentMethods"),
        hubtelMinAmount = obj.d("hubtelMinAmount"), hubtelMaxAmount = obj.d("hubtelMaxAmount"),
        hubtelFeePercentage = obj.d("hubtelFeePercentage"),
        hubtelAutoConfirm = obj.b("hubtelAutoConfirm"), hubtelWebhookUrl = obj.s("hubtelWebhookUrl"),
        coinsResetToZeroDone = obj.b("coinsResetToZeroDone"),
        serverAuthoritativeEconomy = obj.b("serverAuthoritativeEconomy")
    )

    // ─── MarketplaceListing ──────────────────────────────────────
    fun toJson(l: MarketplaceListing) = buildJsonObject {
        put("id", l.id); put("sellerId", l.sellerId); put("sellerName", l.sellerName)
        put("sellerRating", l.sellerRating); put("game", l.game); put("teamName", l.teamName)
        put("ovrRating", l.ovrRating); put("players", l.players)
        put("specialCardsCount", l.specialCardsCount); put("coins", l.coins)
        put("description", l.description); put("price", l.price)
        put("listingType", l.listingType); put("durationDays", l.durationDays)
        put("status", l.status); put("views", l.views); put("listedAt", l.listedAt)
        put("screenshotUrl", l.screenshotUrl); put("ratingType", l.ratingType)
        put("playerScreenshots", l.playerScreenshots)
    }

    fun toMarketplaceListing(obj: JsonObject) = MarketplaceListing(
        id = obj.i("id"), sellerId = obj.i("sellerId"), sellerName = obj.s("sellerName"),
        sellerRating = obj.f("sellerRating"), game = obj.s("game"), teamName = obj.s("teamName"),
        ovrRating = obj.i("ovrRating"), players = obj.s("players"),
        specialCardsCount = obj.i("specialCardsCount"), coins = obj.i("coins"),
        description = obj.s("description"), price = obj.d("price"),
        listingType = obj.s("listingType"), durationDays = obj.i("durationDays"),
        status = obj.s("status"), views = obj.i("views"), listedAt = obj.l("listedAt"),
        screenshotUrl = obj.s("screenshotUrl"), ratingType = obj.s("ratingType"),
        playerScreenshots = obj.s("playerScreenshots")
    )

    // ─── MarketplaceOffer ────────────────────────────────────────
    fun toJson(o: MarketplaceOffer) = buildJsonObject {
        put("id", o.id); put("listingId", o.listingId); put("buyerId", o.buyerId)
        put("buyerName", o.buyerName); put("offerAmount", o.offerAmount)
        put("status", o.status); put("timestamp", o.timestamp)
    }

    fun toMarketplaceOffer(obj: JsonObject) = MarketplaceOffer(
        id = obj.i("id"), listingId = obj.i("listingId"), buyerId = obj.i("buyerId"),
        buyerName = obj.s("buyerName"), offerAmount = obj.d("offerAmount"),
        status = obj.s("status"), timestamp = obj.l("timestamp")
    )

    // ─── MarketplaceTransaction ──────────────────────────────────
    fun toJson(t: MarketplaceTransaction) = buildJsonObject {
        put("id", t.id); put("listingId", t.listingId); put("buyerId", t.buyerId)
        put("buyerName", t.buyerName); put("sellerId", t.sellerId)
        put("sellerName", t.sellerName); put("amount", t.amount)
        put("platformFee", t.platformFee); put("totalAmount", t.totalAmount)
        put("status", t.status); put("paymentMethod", t.paymentMethod); put("phone", t.phone)
        t.sellerScreenshot?.let { put("sellerScreenshot", it) } ?: putNull("sellerScreenshot")
        t.buyerScreenshot?.let { put("buyerScreenshot", it) } ?: putNull("buyerScreenshot")
        t.disputeReason?.let { put("disputeReason", it) } ?: putNull("disputeReason")
        t.disputeSellerResponse?.let { put("disputeSellerResponse", it) } ?: putNull("disputeSellerResponse")
        put("createdAt", t.createdAt)
    }

    fun toMarketplaceTransaction(obj: JsonObject) = MarketplaceTransaction(
        id = obj.i("id"), listingId = obj.i("listingId"), buyerId = obj.i("buyerId"),
        buyerName = obj.s("buyerName"), sellerId = obj.i("sellerId"),
        sellerName = obj.s("sellerName"), amount = obj.d("amount"),
        platformFee = obj.d("platformFee"), totalAmount = obj.d("totalAmount"),
        status = obj.s("status"), paymentMethod = obj.s("paymentMethod"), phone = obj.s("phone"),
        sellerScreenshot = obj["sellerScreenshot"]?.jsonPrimitive?.contentOrNull,
        buyerScreenshot = obj["buyerScreenshot"]?.jsonPrimitive?.contentOrNull,
        disputeReason = obj["disputeReason"]?.jsonPrimitive?.contentOrNull,
        disputeSellerResponse = obj["disputeSellerResponse"]?.jsonPrimitive?.contentOrNull,
        createdAt = obj.l("createdAt")
    )

    // ─── CommunityPost ───────────────────────────────────────────
    fun toJson(p: CommunityPost) = buildJsonObject {
        put("id", p.id); put("userId", p.userId); put("username", p.username)
        put("userAvatar", p.userAvatar); put("title", p.title); put("content", p.content)
        put("category", p.category); put("timestamp", p.timestamp)
        put("likesCount", p.likesCount); put("likedByUserIds", p.likedByUserIds)
        put("commentsCount", p.commentsCount)
        p.tournamentId?.let { put("tournamentId", it) } ?: putNull("tournamentId")
        p.tournamentTitle?.let { put("tournamentTitle", it) } ?: putNull("tournamentTitle")
        p.statGame?.let { put("statGame", it) } ?: putNull("statGame")
        p.statValue?.let { put("statValue", it) } ?: putNull("statValue")
        p.statLabel?.let { put("statLabel", it) } ?: putNull("statLabel")
        p.teamId?.let { put("teamId", it) } ?: putNull("teamId")
        p.teamName?.let { put("teamName", it) } ?: putNull("teamName")
    }

    fun toCommunityPost(obj: JsonObject) = CommunityPost(
        id = obj.i("id"), userId = obj.i("userId"), username = obj.s("username"),
        userAvatar = obj.s("userAvatar"), title = obj.s("title"), content = obj.s("content"),
        category = obj.s("category"), timestamp = obj.l("timestamp"),
        likesCount = obj.i("likesCount"), likedByUserIds = obj.s("likedByUserIds"),
        commentsCount = obj.i("commentsCount"),
        tournamentId = obj["tournamentId"]?.jsonPrimitive?.intOrNull,
        tournamentTitle = obj["tournamentTitle"]?.jsonPrimitive?.contentOrNull,
        statGame = obj["statGame"]?.jsonPrimitive?.contentOrNull,
        statValue = obj["statValue"]?.jsonPrimitive?.contentOrNull,
        statLabel = obj["statLabel"]?.jsonPrimitive?.contentOrNull,
        teamId = obj["teamId"]?.jsonPrimitive?.intOrNull,
        teamName = obj["teamName"]?.jsonPrimitive?.contentOrNull
    )

    // ════════════════════════════════════════════════════════════════
    //  MATCHMAKING QUEUE (uses match_sessions table with status="Waiting")
    // ════════════════════════════════════════════════════════════════

    suspend fun joinMatchmakingQueue(
        userId: Int,
        username: String,
        roomCode: String,
        gameType: String = "eFootball"
    ): JsonObject? {
        return try {
            val data = buildJsonObject {
                put("roomcode", roomCode)
                put("hostid", userId)
                put("hostusername", username)
                put("status", "Waiting")
                put("halflength", 6)
                put("difficulty", "Legendary")
                put("stadium", "Dream Arena")
                put("isfriendly", false)
                put("timestamp", System.currentTimeMillis())
            }
            SupabaseConfig.client.postgrest["match_sessions"].insert(data).decodeSingle<JsonObject>()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseSync", "joinMatchmakingQueue failed", e)
            null
        }
    }

    suspend fun findWaitingOpponent(excludeUserId: Int, gameType: String = "eFootball"): JsonObject? {
        return try {
            val results = SupabaseConfig.client.postgrest["match_sessions"]
                .select {
                    filter {
                        eq("status", "Waiting")
                        gt("timestamp", System.currentTimeMillis() - 30_000)
                    }
                }
                .decodeList<JsonObject>()
            results.firstOrNull {
                val hostId = it["hostid"]?.jsonPrimitive?.intOrNull ?: 0
                hostId != excludeUserId
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseSync", "findWaitingOpponent failed", e)
            null
        }
    }

    suspend fun claimMatchSession(
        sessionId: Int,
        guestId: Int,
        guestUsername: String
    ): Boolean {
        return try {
            val data = buildJsonObject {
                put("guestid", guestId)
                put("guestusername", guestUsername)
                put("status", "Ready")
            }
            SupabaseConfig.client.postgrest["match_sessions"]
                .update(data) { filter { eq("id", sessionId) } }
            true
        } catch (e: Exception) {
            android.util.Log.e("SupabaseSync", "claimMatchSession failed", e)
            false
        }
    }

    suspend fun removeWaitingSession(sessionId: Int) {
        try {
            SupabaseConfig.client.postgrest["match_sessions"]
                .delete { filter { eq("id", sessionId) } }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseSync", "removeWaitingSession failed", e)
        }
    }

    suspend fun cleanupOldWaitingSessions() {
        try {
            val cutoff = System.currentTimeMillis() - 60_000
            SupabaseConfig.client.postgrest["match_sessions"]
                .delete {
                    filter {
                        eq("status", "Waiting")
                        lt("timestamp", cutoff)
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseSync", "cleanupOldWaitingSessions failed", e)
        }
    }

    suspend fun pushMatchSession(s: MatchSession): JsonObject? {
        return try {
            SupabaseConfig.client.postgrest["match_sessions"]
                .insert(toJson(s))
                .decodeSingle<JsonObject>()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseSync", "pushMatchSession failed", e)
            null
        }
    }

    // ─── CommunityComment ────────────────────────────────────────
    fun toJson(c: CommunityComment) = buildJsonObject {
        put("id", c.id); put("postId", c.postId); put("userId", c.userId)
        put("username", c.username); put("userAvatar", c.userAvatar)
        put("content", c.content); put("timestamp", c.timestamp)
    }

    fun toCommunityComment(obj: JsonObject) = CommunityComment(
        id = obj.i("id"), postId = obj.i("postId"), userId = obj.i("userId"),
        username = obj.s("username"), userAvatar = obj.s("userAvatar"),
        content = obj.s("content"), timestamp = obj.l("timestamp")
    )

    // ════════════════════════════════════════════════════════════════
    //  IMAGE UPLOAD
    // ════════════════════════════════════════════════════════════════

    suspend fun uploadImage(bucket: String, path: String, file: File): String? {
        return try {
            val bucketRef = SupabaseConfig.client.storage[bucket]
            bucketRef.upload(path, file.readBytes()) {
                upsert = true
            }
            "${SupabaseConfig.SUPABASE_URL}/storage/v1/object/public/$bucket/$path"
        } catch (e: Exception) {
            android.util.Log.e("SupabaseSync", "uploadImage failed", e)
            null
        }
    }

    suspend fun uploadMatchScreenshot(matchSessionId: Int, side: String, imageBytes: ByteArray): String? {
        val path = "match_screenshots/${matchSessionId}_${side}_${System.currentTimeMillis()}.jpg"
        val tempFile = File.createTempFile("screenshot_", ".jpg")
        tempFile.writeBytes(imageBytes)
        return try {
            val url = uploadImage("screenshots", path, tempFile)
            tempFile.delete()
            url
        } catch (e: Exception) {
            tempFile.delete()
            null
        }
    }

    suspend fun uploadProfileAvatar(userId: Int, imageBytes: ByteArray): String? {
        val path = "avatars/user_${userId}_${System.currentTimeMillis()}.jpg"
        val tempFile = File.createTempFile("avatar_", ".jpg")
        tempFile.writeBytes(imageBytes)
        return try {
            val url = uploadImage("avatars", path, tempFile)
            if (url != null) {
                pushUpdate("users", userId, buildJsonObject { put("selectedAvatar", url) })
            }
            tempFile.delete()
            url
        } catch (e: Exception) {
            tempFile.delete()
            null
        }
    }
}
