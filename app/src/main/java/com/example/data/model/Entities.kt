package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users", indices = [
    Index(value = ["username"], unique = true),
    Index(value = ["email"], unique = true),
    Index(value = ["phone"], unique = true),
    Index(value = ["referralCode"], unique = true),
    Index(value = ["ghanaRegion"]),
    Index(value = ["preferredGame"]),
    Index(value = ["role"])
])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val bio: String = "",
    val paymentMethod: String = "MTN",
    val raivalPoints: Int = 100,
    val balance: Double = 50.0, // GHS cash balance
    val coinBalance: Int = 200,  // DLS coins
    val totalWinnings: Double = 0.0,
    val tournamentsPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winStreak: Int = 0,
    val dailyLoginClaimedAt: Long = 0L,
    val role: String = "player", // "player", "organizer", "admin"
    val status: String = "active", // "active", "banned"
    val createdAt: Long = System.currentTimeMillis(),
    val dlsHandle: String = "",
    val efootballHandle: String = "",
    val discordHandle: String = "",
    val xp: Int = 0,
    val level: Int = 1,
    val streakShields: Int = 0,
    val referralCode: String = "",
    val unlockedSkills: String = "",
    val completedMissions: String = "",
    val selectedAvatar: String = "avatar_default",
    val customTitle: String = "",
    val clanTag: String = "",
    val nameColor: String = "",
    val nameGlow: Boolean = false,
    val profileTheme: String = "default",
    val unlockedThemes: String = "default",
    val unlockedAvatars: String = "avatar_default",
    val welcomeBonusClaimed: Boolean = false,
    val socialFbConnected: Boolean = false,
    val socialTwConnected: Boolean = false,
    val socialAppleConnected: Boolean = false,
    val appRated: Boolean = false,
    val referredActiveCount: Int = 0,
    val coinsClaimedFromTopUp: Boolean = false,
    val ghanaRegion: String = "Greater Accra",
    val ghanaHometown: String = "Accra",
    val preferredGame: String = "Dream League Soccer",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val authUid: String = "",
    val proLicenseExpiresAt: Long = 0L
)

@Entity(tableName = "marketplace_listings", indices = [
    Index(value = ["sellerId"]),
    Index(value = ["game"]),
    Index(value = ["status"]),
    Index(value = ["listedAt"]),
    Index(value = ["sellerId", "status"])
])
data class MarketplaceListing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sellerId: Int,
    val sellerName: String,
    val sellerRating: Float = 4.8f,
    val game: String, // "eFootball", "FC Mobile", "DLS"
    val teamName: String,
    val ovrRating: Int,
    val players: String, // Comma or newline-separated major players
    val specialCardsCount: Int = 5,
    val coins: Int = 150000,
    val description: String = "",
    val price: Double, // GHS
    val listingType: String = "Negotiable", // "Fixed Price", "Negotiable", "Auction"
    val durationDays: Int = 7, // 7, 14, 30
    val status: String = "Active", // "Active", "Sold", "Removed"
    val views: Int = 0,
    val listedAt: Long = System.currentTimeMillis(),
    val screenshotUrl: String = "", // Custom decorative thumbnail/preset image name
    val ratingType: String = "OVR", // "OVR", "Collective Strength", "Team Rating"
    val playerScreenshots: String = "" // Comma-separated or newline-separated filenames/uploaded files
)

@Entity(tableName = "marketplace_offers", indices = [
    Index(value = ["listingId"]),
    Index(value = ["buyerId"]),
    Index(value = ["status"]),
    Index(value = ["listingId", "status"])
])
data class MarketplaceOffer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listingId: Int,
    val buyerId: Int,
    val buyerName: String,
    val offerAmount: Double,
    val status: String = "Pending", // "Chatting", "Pending", "Accepted", "Declined"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "marketplace_transactions", indices = [
    Index(value = ["listingId"]),
    Index(value = ["buyerId"]),
    Index(value = ["sellerId"]),
    Index(value = ["status"]),
    Index(value = ["createdAt"])
])
data class MarketplaceTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listingId: Int,
    val buyerId: Int,
    val buyerName: String,
    val sellerId: Int,
    val sellerName: String,
    val amount: Double,
    val platformFee: Double,
    val totalAmount: Double,
    val status: String = "Payment Held", // "Payment Held", "Team Transferred", "Team Verified", "Completed", "Disputed", "Refunded"
    val paymentMethod: String = "MTN MoMo", // "MTN MoMo", "Vodafone Cash"
    val phone: String,
    val sellerScreenshot: String? = null,
    val buyerScreenshot: String? = null,
    val disputeReason: String? = null,
    val disputeSellerResponse: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "tournaments", indices = [
    Index(value = ["status"]),
    Index(value = ["date"]),
    Index(value = ["game"]),
    Index(value = ["isHidden"]),
    Index(value = ["organizer"]),
    Index(value = ["status", "date"]),
    Index(value = ["status", "game"]),
    Index(value = ["isAutoHosted"])
])
data class Tournament(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val game: String = "DLS",
    val entryFee: Double, // cash entry fee (GHS)
    val coinEntryFee: Int = 0, // coin entry fee (if cash is 0)
    val prize: Double, // cash prize (GHS)
    val coinPrize: Int = 0, // coin prize
    val players: Int = 0,
    val maxPlayers: Int = 16,
    val date: String,
    val time: String,
    val status: String = "Open", // "Open", "Coming Soon", "Closed"
    val description: String = "",
    val mode: String = "1 vs 1", // "1 vs 1" only for DLS
    val map: String = "Dream Arena", // DLS Stadium name
    val rules: String = "", // newline-separated rules
    val schedule: String = "", // newline-separated schedule
    val prizeDistribution: String = "", // newline-separated distribution
    val contact: String = "",
    val banner: String = "",
    val organizer: String = "Raival Admin",
    val format: String = "Knockout", // "League" or "Knockout"
    val style: String = "World Cup Style", // "EPL Style", "Champions League Style", "World Cup Style"
    val isHidden: Boolean = false,
    val isAutoHosted: Boolean = true
)

@Entity(tableName = "registrations", indices = [
    Index(value = ["userId"]),
    Index(value = ["tournamentId"]),
    Index(value = ["userId", "tournamentId"], unique = true),
    Index(value = ["registeredAt"])
])
data class Registration(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val tournamentId: Int,
    val playerName: String,
    val inGameName: String,
    val paymentMethod: String,
    val phone: String,
    val registeredAt: Long = System.currentTimeMillis(),
    val teamRating: Int = 0,
    val modelTeam: String = ""
)

@Entity(tableName = "transactions", indices = [
    Index(value = ["reference"], unique = true),
    Index(value = ["userId"]),
    Index(value = ["tournamentId"]),
    Index(value = ["timestamp"]),
    Index(value = ["status"])
])
data class MoMoTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reference: String,
    val userId: Int,
    val tournamentId: Int,
    val tournamentTitle: String,
    val amount: Double,
    val paymentMethod: String,
    val phone: String,
    val status: String = "success", // "success", "failed", "pending"
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "REGISTRATION" // "REGISTRATION", "WITHDRAWAL", "DEPOSIT"
)

@Entity(tableName = "teams", indices = [
    Index(value = ["leader"]),
    Index(value = ["isPublic"]),
    Index(value = ["createdAt"])
])
data class Team(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val memberCount: Int = 1,
    val maxMembers: Int = 10,
    val wins: Int = 0,
    val leader: String,
    val members: String, // comma-separated usernames
    val tags: String = "", // comma-separated tags
    val createdAt: String,
    val isPublic: Boolean = true
)

@Entity(tableName = "chat_messages", indices = [
    Index(value = ["chatRoomId"]),
    Index(value = ["senderId"]),
    Index(value = ["timestamp"]),
    Index(value = ["chatRoomId", "timestamp"])
])
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatRoomId: String, // e.g., "tournament_1", "team_3", "direct_2"
    val senderId: Int,
    val senderUsername: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null
)

@Entity(tableName = "match_sessions", indices = [
    Index(value = ["roomCode"], unique = true),
    Index(value = ["hostId"]),
    Index(value = ["guestId"]),
    Index(value = ["status"]),
    Index(value = ["timestamp"])
])
data class MatchSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val roomCode: String, // 6-digit room code
    val hostId: Int,
    val hostUsername: String,
    val guestId: Int?,
    val guestUsername: String?,
    val status: String = "Waiting", // "Waiting", "Ready", "Playing", "Verification", "Disputed", "Completed"
    val halfLength: Int = 6, // Match half duration (e.g. 6 mins)
    val difficulty: String = "Legendary", // Division (Amateur, Semi-Pro, Professional, Legendary)
    val stadium: String = "Dream Arena", // DLS Stadium
    val hostScore: Int? = null,
    val guestScore: Int? = null,
    val winnerId: Int? = null,
    val hostScreenshot: String? = null,
    val guestScreenshot: String? = null,
    val isFriendly: Boolean = true,
    val gameType: String = "eFootball",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tournament_matches", indices = [
    Index(value = ["tournamentId"]),
    Index(value = ["status"]),
    Index(value = ["round"]),
    Index(value = ["matchCode"]),
    Index(value = ["player1Name"]),
    Index(value = ["player2Name"]),
    Index(value = ["tournamentId", "status"]),
    Index(value = ["tournamentId", "round"]),
    Index(value = ["checkInDeadline"]),
    Index(value = ["matchDeadline"])
])
data class TournamentMatch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tournamentId: Int,
    val round: String, // "Quarter-finals", "Semi-finals", "Final"
    val matchIndex: Int, // e.g. 0 to 3 for Quarters, 0 to 1 for Semis, 0 for Final
    val player1Name: String,
    val player2Name: String,
    val player1Score: Int? = null,
    val player2Score: Int? = null,
    val status: String = "Pending", // "Pending", "Completed"
    val winnerName: String? = null,
    val matchDate: String = "",
    val matchTime: String = "",
    val player1Rating: Int = 0,
    val player2Rating: Int = 0,
    val player1Team: String = "",
    val player2Team: String = "",
    val matchCode: String = "",
    val player1CheckedIn: Boolean = false,
    val player2CheckedIn: Boolean = false,
    val checkInDeadline: Long = 0L,
    val player1Disqualified: Boolean = false,
    val player2Disqualified: Boolean = false,
    val matchDeadline: Long = 0L
)

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1,
    val economyMode: String = "Coin-Only", // "Coin-Only", "Real Cash + Coins"
    val hubtelClientId: String = "",
    val hubtelClientSecret: String = "",
    val hubtelMerchantId: String = "",
    val hubtelApiUrl: String = "https://api.hubtel.com/v1/",
    val hubtelPaymentMethods: String = "momo, vodafone",
    val hubtelMinAmount: Double = 1.0,
    val hubtelMaxAmount: Double = 500.0,
    val hubtelFeePercentage: Double = 2.0,
    val hubtelAutoConfirm: Boolean = true,
    val hubtelWebhookUrl: String = "https://raival.com/webhook/hubtel",
    val coinsResetToZeroDone: Boolean = false,
    val serverAuthoritativeEconomy: Boolean = false
)

@Entity(tableName = "community_posts", indices = [
    Index(value = ["userId"]),
    Index(value = ["category"]),
    Index(value = ["timestamp"]),
    Index(value = ["tournamentId"]),
    Index(value = ["teamId"]),
    Index(value = ["category", "timestamp"]),
    Index(value = ["userId", "timestamp"])
])
data class CommunityPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val username: String,
    val userAvatar: String = "avatar_default",
    val title: String,
    val content: String,
    val category: String, // "General", "Tournaments", "Stats", "Team Recruitment"
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val likedByUserIds: String = "", // Comma-separated list of user IDs who liked it
    val commentsCount: Int = 0,
    // Optional attachment fields
    val tournamentId: Int? = null,
    val tournamentTitle: String? = null,
    val statGame: String? = null,
    val statValue: String? = null,
    val statLabel: String? = null,
    val teamId: Int? = null,
    val teamName: String? = null
)

@Entity(tableName = "community_comments", indices = [
    Index(value = ["postId"]),
    Index(value = ["userId"]),
    Index(value = ["timestamp"]),
    Index(value = ["postId", "timestamp"])
])
data class CommunityComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val userId: Int,
    val username: String,
    val userAvatar: String = "avatar_default",
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)



