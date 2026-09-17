package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.TournamentMatch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TournamentBracketTest {
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
    fun testInsertAndRetrieveMatches() = runBlocking {
        val matchDao = db.tournamentMatchDao()
        val match = TournamentMatch(
            tournamentId = 1,
            round = "Quarter-finals",
            matchIndex = 0,
            player1Name = "Akwasi_Gamer",
            player2Name = "Kofi_Pro",
            status = "Pending"
        )
        
        matchDao.insertTournamentMatch(match)
        val retrieved = matchDao.getMatchesForTournamentOneShot(1)
        assertEquals(1, retrieved.size)
        assertEquals("Quarter-finals", retrieved[0].round)
        assertEquals("Akwasi_Gamer", retrieved[0].player1Name)
        assertEquals("Kofi_Pro", retrieved[0].player2Name)
    }

    @Test
    fun testUpdateMatchScore() = runBlocking {
        val matchDao = db.tournamentMatchDao()
        val match = TournamentMatch(
            id = 1,
            tournamentId = 1,
            round = "Quarter-finals",
            matchIndex = 0,
            player1Name = "Akwasi_Gamer",
            player2Name = "Kofi_Pro",
            status = "Pending"
        )
        matchDao.insertTournamentMatch(match)
        
        val retrievedBefore = matchDao.getMatchesForTournamentOneShot(1)[0]
        val updated = retrievedBefore.copy(
            player1Score = 3,
            player2Score = 1,
            status = "Completed",
            winnerName = "Akwasi_Gamer"
        )
        matchDao.updateTournamentMatch(updated)
        
        val retrievedAfter = matchDao.getMatchesForTournamentOneShot(1)[0]
        assertEquals("Completed", retrievedAfter.status)
        assertEquals(3, retrievedAfter.player1Score)
        assertEquals("Akwasi_Gamer", retrievedAfter.winnerName)
    }
}
