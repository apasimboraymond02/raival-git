package com.example

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.User
import com.example.ui.viewmodel.RaivalViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CasualGamesTest {
    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var viewModel: RaivalViewModel

    @Before
    fun setUp() = runBlocking {
        app = ApplicationProvider.getApplicationContext<Application>()
        
        // Build in-memory database
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Inject db reflectively into AppDatabase companion object's INSTANCE
        try {
            val companionClass = Class.forName("com.example.data.database.AppDatabase\$Companion")
            val instanceField = companionClass.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(AppDatabase.Companion, db)
        } catch (e: Exception) {
            val instanceField = AppDatabase::class.java.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(null, db)
        }

        db.clearAllTables()
        val user = User(
            id = 1,
            username = "TestGamer",
            fullName = "John Doe",
            email = "john@example.com",
            phone = "0240000000",
            bio = "DLS Arena Challenger ⚽",
            role = "user",
            balance = 100.0,
            coinBalance = 50
        )
        db.userDao().insertUser(user)

        viewModel = RaivalViewModel(app)

        // Log in the seeded user
        viewModel.login("john@example.com", "password") { _, _ -> }
        
        // Idle looper so login coroutine completes
        shadowOf(Looper.getMainLooper()).idle()

        val currentUser = viewModel.currentUser.filterNotNull().first()
        assertEquals("TestGamer", currentUser.username)
    }

    @After
    fun tearDown() {
        db.close()
        // Reset the singleton INSTANCE reflectively
        try {
            val companionClass = Class.forName("com.example.data.database.AppDatabase\$Companion")
            val instanceField = companionClass.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(AppDatabase.Companion, null)
        } catch (e: Exception) {
            val instanceField = AppDatabase::class.java.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(null, null)
        }
    }

    @Test
    fun testBookUpcomingSession() {
        // Assert booked sessions is empty initially
        assertTrue(viewModel.bookedSessions.value.isEmpty())

        // Book upcoming session
        viewModel.bookNextSession("14:30")
        viewModel.bookNextSession("15:00")

        val booked = viewModel.bookedSessions.value
        assertEquals(2, booked.size)
        assertTrue(booked.contains("14:30"))
        assertTrue(booked.contains("15:00"))
    }

    @Test
    fun testSubmitGameScoreAndLeaderboardClimb() {
        // Submit high score
        viewModel.submitCasualGameScore(1500)

        val leaderboard = viewModel.activeSessionLeaderboard.value
        assertFalse(leaderboard.isEmpty())

        // User should climb to first place
        val topPlayer = leaderboard.first()
        assertEquals("TestGamer", topPlayer.first)
        assertEquals(1500, topPlayer.second)
    }

    @Test
    fun testDailyCoinCapsAndEarningMultiplier() = runBlocking {
        // Claim ad rewards multiple times (Limit is 10 coins, each ad gives 2 coins)
        for (i in 1..8) {
            viewModel.rewardParticipation("ad")
        }

        // Idle looper so reward participation coroutines complete
        shadowOf(Looper.getMainLooper()).idle()

        // Check our local ad coins tracker has enforced the cap of 10 coins max from ads
        val adCoinsEarnedToday = viewModel.adCoinsEarnedToday.value
        assertEquals(10, adCoinsEarnedToday)

        // Verify total coinBalance updated inside the database flow using one-shot query
        val user = db.userDao().getUserByIdOneShot(1)
        assertNotNull(user)
        // Started with 50 coinBalance, plus 10 from ads = 60
        assertEquals(60, user?.coinBalance)
    }

    @Test
    fun testDailyLoginBonus() = runBlocking {
        viewModel.rewardParticipation("login")

        // Idle looper so reward participation coroutines complete
        shadowOf(Looper.getMainLooper()).idle()

        val user = db.userDao().getUserByIdOneShot(1)
        assertNotNull(user)
        // Started with 50 coinBalance, plus 5 from login = 55
        assertEquals(55, user?.coinBalance)
    }
}
