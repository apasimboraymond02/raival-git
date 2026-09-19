package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Int): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserByIdOneShot(id: Int): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users ORDER BY raivalPoints DESC")
    fun getAllUsersByPoints(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users")
    suspend fun getAllUsersOneShot(): List<User>
}

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments ORDER BY id DESC")
    fun getAllTournaments(): Flow<List<Tournament>>

    @Query("SELECT * FROM tournaments")
    suspend fun getAllTournamentsOneShot(): List<Tournament>

    @Query("SELECT * FROM tournaments ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getTournamentsPaged(limit: Int, offset: Int): List<Tournament>

    @Query("SELECT COUNT(*) FROM tournaments")
    suspend fun getTournamentCount(): Int

    @Query("SELECT * FROM tournaments WHERE id = :id")
    fun getTournamentById(id: Int): Flow<Tournament?>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentByIdOneShot(id: Int): Tournament?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: Tournament): Long

    @Update
    suspend fun updateTournament(tournament: Tournament)

    @Query("DELETE FROM tournaments WHERE id = :id")
    suspend fun deleteTournamentById(id: Int)

    @Query("DELETE FROM tournaments")
    suspend fun deleteAllTournaments()
}

@Dao
interface RegistrationDao {
    @Query("SELECT * FROM registrations WHERE userId = :userId")
    fun getRegistrationsForUser(userId: Int): Flow<List<Registration>>

    @Query("SELECT * FROM registrations WHERE userId = :userId")
    suspend fun getRegistrationsForUserOneShot(userId: Int): List<Registration>

    @Query("SELECT * FROM registrations WHERE tournamentId = :tournamentId")
    fun getRegistrationsForTournament(tournamentId: Int): Flow<List<Registration>>

    @Query("SELECT * FROM registrations WHERE tournamentId = :tournamentId")
    suspend fun getRegistrationsForTournamentOneShot(tournamentId: Int): List<Registration>

    @Query("SELECT * FROM registrations WHERE userId = :userId AND tournamentId = :tournamentId LIMIT 1")
    suspend fun getRegistrationByUserAndTournament(userId: Int, tournamentId: Int): Registration?

    @Query("SELECT * FROM registrations")
    suspend fun getAllRegistrationsOneShot(): List<Registration>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: Registration): Long

    @Query("DELETE FROM registrations WHERE tournamentId = :tournamentId")
    suspend fun deleteRegistrationsForTournament(tournamentId: Int)

    @Query("DELETE FROM registrations")
    suspend fun deleteAllRegistrations()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsForUser(userId: Int): Flow<List<MoMoTransaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<MoMoTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: MoMoTransaction): Long
}

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams ORDER BY wins DESC")
    fun getAllTeams(): Flow<List<Team>>

    @Query("SELECT * FROM teams WHERE id = :id")
    fun getTeamById(id: Int): Flow<Team?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: Team): Long

    @Update
    suspend fun updateTeam(team: Team)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE chatRoomId = :chatRoomId ORDER BY timestamp ASC")
    fun getMessagesForRoom(chatRoomId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE chatRoomId = :chatRoomId ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesForRoomPaged(chatRoomId: String, limit: Int, offset: Int): List<ChatMessage>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE chatRoomId = :chatRoomId")
    suspend fun getMessageCountForRoom(chatRoomId: String): Int

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long
}

@Dao
interface MatchSessionDao {
    @Query("SELECT * FROM match_sessions ORDER BY timestamp DESC")
    fun getAllMatchSessions(): Flow<List<MatchSession>>

    @Query("SELECT * FROM match_sessions WHERE roomCode = :roomCode LIMIT 1")
    suspend fun getMatchSessionByRoomCode(roomCode: String): MatchSession?

    @Query("SELECT * FROM match_sessions WHERE id = :id")
    fun getMatchSessionById(id: Int): Flow<MatchSession?>

    @Query("SELECT * FROM match_sessions WHERE id = :id")
    suspend fun getMatchSessionByIdOneShot(id: Int): MatchSession?

    @Query("SELECT * FROM match_sessions WHERE winnerId = :userId AND status = 'Completed' ORDER BY timestamp DESC")
    suspend fun getCompletedWinsForUser(userId: Int): List<MatchSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchSession(session: MatchSession): Long

    @Update
    suspend fun updateMatchSession(session: MatchSession)

    @Delete
    suspend fun deleteMatchSession(session: MatchSession)
}

@Dao
interface TournamentMatchDao {
    @Query("SELECT * FROM tournament_matches WHERE tournamentId = :tournamentId ORDER BY id ASC")
    fun getMatchesForTournament(tournamentId: Int): Flow<List<TournamentMatch>>

    @Query("SELECT * FROM tournament_matches WHERE tournamentId = :tournamentId ORDER BY id ASC")
    suspend fun getMatchesForTournamentOneShot(tournamentId: Int): List<TournamentMatch>

    @Query("SELECT * FROM tournament_matches WHERE tournamentId = :tournamentId ORDER BY id ASC LIMIT :limit OFFSET :offset")
    suspend fun getMatchesForTournamentPaged(tournamentId: Int, limit: Int, offset: Int): List<TournamentMatch>

    @Query("SELECT COUNT(*) FROM tournament_matches WHERE tournamentId = :tournamentId")
    suspend fun getMatchesForTournamentCount(tournamentId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournamentMatch(match: TournamentMatch): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournamentMatches(matches: List<TournamentMatch>)

    @Update
    suspend fun updateTournamentMatch(match: TournamentMatch)

    @Query("SELECT * FROM tournament_matches")
    fun getAllMatches(): Flow<List<TournamentMatch>>

    @Query("DELETE FROM tournament_matches WHERE tournamentId = :tournamentId")
    suspend fun deleteMatchesForTournament(tournamentId: Int)

    @Query("SELECT * FROM tournament_matches WHERE id = :id LIMIT 1")
    suspend fun getMatchByIdOneShot(id: Int): TournamentMatch?

    @Query("DELETE FROM tournament_matches")
    suspend fun deleteAllMatches()
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    fun getAppConfig(): Flow<AppConfig?>

    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    suspend fun getAppConfigOneShot(): AppConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: AppConfig)
}

@Dao
interface MarketplaceListingDao {
    @Query("SELECT * FROM marketplace_listings WHERE status = 'Active' ORDER BY listedAt DESC")
    fun getActiveListings(): Flow<List<MarketplaceListing>>

    @Query("SELECT * FROM marketplace_listings WHERE status = 'Active' ORDER BY listedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getActiveListingsPaged(limit: Int, offset: Int): List<MarketplaceListing>

    @Query("SELECT COUNT(*) FROM marketplace_listings WHERE status = 'Active'")
    suspend fun getActiveListingsCount(): Int

    @Query("SELECT * FROM marketplace_listings ORDER BY listedAt DESC")
    fun getAllListings(): Flow<List<MarketplaceListing>>

    @Query("SELECT * FROM marketplace_listings ORDER BY listedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllListingsPaged(limit: Int, offset: Int): List<MarketplaceListing>

    @Query("SELECT COUNT(*) FROM marketplace_listings")
    suspend fun getAllListingsCount(): Int

    @Query("SELECT * FROM marketplace_listings WHERE id = :id")
    fun getListingById(id: Int): Flow<MarketplaceListing?>

    @Query("SELECT * FROM marketplace_listings WHERE id = :id")
    suspend fun getListingByIdOneShot(id: Int): MarketplaceListing?

    @Query("SELECT * FROM marketplace_listings WHERE sellerId = :sellerId ORDER BY listedAt DESC")
    fun getListingsBySeller(sellerId: Int): Flow<List<MarketplaceListing>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: MarketplaceListing): Long

    @Update
    suspend fun updateListing(listing: MarketplaceListing)

    @Delete
    suspend fun deleteListing(listing: MarketplaceListing)
}

@Dao
interface MarketplaceOfferDao {
    @Query("SELECT * FROM marketplace_offers WHERE listingId = :listingId ORDER BY timestamp DESC")
    fun getOffersForListing(listingId: Int): Flow<List<MarketplaceOffer>>

    @Query("SELECT * FROM marketplace_offers WHERE buyerId = :buyerId ORDER BY timestamp DESC")
    fun getOffersByBuyer(buyerId: Int): Flow<List<MarketplaceOffer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: MarketplaceOffer): Long

    @Update
    suspend fun updateOffer(offer: MarketplaceOffer)

    @Query("DELETE FROM marketplace_offers WHERE listingId = :listingId")
    suspend fun deleteOffersForListing(listingId: Int)
}

@Dao
interface MarketplaceTransactionDao {
    @Query("SELECT * FROM marketplace_transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<MarketplaceTransaction>>

    @Query("SELECT * FROM marketplace_transactions WHERE buyerId = :userId OR sellerId = :userId ORDER BY createdAt DESC")
    fun getTransactionsForUser(userId: Int): Flow<List<MarketplaceTransaction>>

    @Query("SELECT * FROM marketplace_transactions WHERE id = :id")
    fun getTransactionById(id: Int): Flow<MarketplaceTransaction?>

    @Query("SELECT * FROM marketplace_transactions WHERE id = :id")
    suspend fun getTransactionByIdOneShot(id: Int): MarketplaceTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: MarketplaceTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: MarketplaceTransaction)
}

@Dao
interface CommunityPostDao {
    @Query("SELECT * FROM community_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<CommunityPost>>

    @Query("SELECT * FROM community_posts ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPostsPaged(limit: Int, offset: Int): List<CommunityPost>

    @Query("SELECT COUNT(*) FROM community_posts")
    suspend fun getPostsCount(): Int

    @Query("SELECT * FROM community_posts WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: Int): CommunityPost?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: CommunityPost): Long

    @Update
    suspend fun updatePost(post: CommunityPost)

    @Query("DELETE FROM community_posts WHERE id = :id")
    suspend fun deletePostById(id: Int)
}

@Dao
interface CommunityCommentDao {
    @Query("SELECT * FROM community_comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: Int): Flow<List<CommunityComment>>

    @Query("SELECT * FROM community_comments WHERE postId = :postId ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getCommentsForPostPaged(postId: Int, limit: Int, offset: Int): List<CommunityComment>

    @Query("SELECT COUNT(*) FROM community_comments WHERE postId = :postId")
    suspend fun getCommentsForPostCount(postId: Int): Int

    @Query("SELECT * FROM community_comments ORDER BY timestamp ASC")
    fun getAllComments(): Flow<List<CommunityComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommunityComment): Long

    @Query("DELETE FROM community_comments WHERE postId = :postId")
    suspend fun deleteCommentsForPost(postId: Int)
}




