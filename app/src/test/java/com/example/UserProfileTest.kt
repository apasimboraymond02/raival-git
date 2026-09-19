package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserProfileTest {
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testUserStatsAndProfilePersistence() = runBlocking {
        val userDao = db.userDao()
        val user = User(
            username = "TestGamer",
            fullName = "John Doe",
            email = "john@example.com",
            phone = "0240000000",
            bio = "DLS Arena Challenger ⚽",
            raivalPoints = 450,
            coinBalance = 300,
            totalWinnings = 120.0,
            tournamentsPlayed = 5,
            wins = 8,
            losses = 2,
            winStreak = 4
        )

        val id = userDao.insertUser(user)
        val retrieved = userDao.getUserById(id.toInt()).first()

        assertNotNull(retrieved)
        assertEquals("TestGamer", retrieved?.username)
        assertEquals("John Doe", retrieved?.fullName)
        assertEquals("0240000000", retrieved?.phone)
        assertEquals("DLS Arena Challenger ⚽", retrieved?.bio)
        assertEquals(450, retrieved?.raivalPoints)
        assertEquals(300, retrieved?.coinBalance)
        assertEquals(120.0, retrieved?.totalWinnings ?: 0.0, 0.01)
        assertEquals(5, retrieved?.tournamentsPlayed)
        assertEquals(8, retrieved?.wins)
        assertEquals(2, retrieved?.losses)
        assertEquals(4, retrieved?.winStreak)
    }

    @Test
    fun testUpdateProfileDetails() = runBlocking {
        val userDao = db.userDao()
        val user = User(
            id = 1,
            username = "TestGamer",
            fullName = "John Doe",
            email = "john@example.com",
            phone = "0240000000",
            bio = "DLS Arena Challenger ⚽"
        )
        userDao.insertUser(user)

        val retrievedBefore = userDao.getUserById(1).first()!!
        val updated = retrievedBefore.copy(
            fullName = "Johnathan Doe",
            phone = "0559999999",
            bio = "Elite DLS Striker 🔥"
        )
        userDao.updateUser(updated)

        val retrievedAfter = userDao.getUserById(1).first()!!
        assertEquals("Johnathan Doe", retrievedAfter.fullName)
        assertEquals("0559999999", retrievedAfter.phone)
        assertEquals("Elite DLS Striker 🔥", retrievedAfter.bio)
    }

    @Test
    fun testMarketplaceTransactionTrustFlow() = runBlocking {
        val transactionDao = db.marketplaceTransactionDao()
        
        val completedTx = com.example.data.model.MarketplaceTransaction(
            listingId = 1,
            buyerId = 5,
            buyerName = "BuyerA",
            sellerId = 99,
            sellerName = "TrustySeller",
            amount = 150.0,
            platformFee = 7.5,
            totalAmount = 157.5,
            status = "Completed",
            phone = "0241111111"
        )
        
        val disputedTx = com.example.data.model.MarketplaceTransaction(
            listingId = 2,
            buyerId = 6,
            buyerName = "BuyerB",
            sellerId = 99,
            sellerName = "TrustySeller",
            amount = 200.0,
            platformFee = 10.0,
            totalAmount = 210.0,
            status = "Disputed",
            phone = "0242222222"
        )
        
        transactionDao.insertTransaction(completedTx)
        transactionDao.insertTransaction(disputedTx)
        
        val userTransactions = transactionDao.getTransactionsForUser(99).first()
        
        assertEquals(2, userTransactions.size)
        val sales = userTransactions.filter { it.sellerId == 99 }
        val completed = sales.filter { it.status == "Completed" }
        val disputed = sales.filter { it.status == "Disputed" }
        
        assertEquals(1, completed.size)
        assertEquals(1, disputed.size)
        
        val totalRated = completed.size + disputed.size
        val trustScore = if (totalRated == 0) 100 else (completed.size * 100) / totalRated
        assertEquals(50, trustScore)
    }
}
