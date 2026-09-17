package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.*
import com.example.data.model.*

@Database(
    entities = [
        User::class,
        Tournament::class,
        Registration::class,
        MoMoTransaction::class,
        Team::class,
        ChatMessage::class,
        MatchSession::class,
        TournamentMatch::class,
        AppConfig::class,
        MarketplaceListing::class,
        MarketplaceOffer::class,
        MarketplaceTransaction::class,
        CommunityPost::class,
        CommunityComment::class
    ],
    version = 20,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun tournamentDao(): TournamentDao
    abstract fun registrationDao(): RegistrationDao
    abstract fun transactionDao(): TransactionDao
    abstract fun teamDao(): TeamDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun matchSessionDao(): MatchSessionDao
    abstract fun tournamentMatchDao(): TournamentMatchDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun marketplaceListingDao(): MarketplaceListingDao
    abstract fun marketplaceOfferDao(): MarketplaceOfferDao
    abstract fun marketplaceTransactionDao(): MarketplaceTransactionDao
    abstract fun communityPostDao(): CommunityPostDao
    abstract fun communityCommentDao(): CommunityCommentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "raival_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
