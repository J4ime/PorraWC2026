package com.porrawc2026.app.data.repository

import com.porrawc2026.app.data.local.dao.*
import com.porrawc2026.app.data.local.entity.*
import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PorraRepositoryTest {

    private lateinit var teamDao: TeamDao
    private lateinit var matchDao: MatchDao
    private lateinit var questionDao: QuestionDao
    private lateinit var playerPredictionDao: PlayerPredictionDao
    private lateinit var knockoutPredictionDao: KnockoutPredictionDao
    private lateinit var groupStandingDao: GroupStandingDao
    private lateinit var repository: PorraRepository

    @Before
    fun setup() {
        teamDao = mockk()
        matchDao = mockk()
        questionDao = mockk()
        playerPredictionDao = mockk()
        knockoutPredictionDao = mockk()
        groupStandingDao = mockk()

        repository = PorraRepository(
            teamDao, matchDao, questionDao,
            playerPredictionDao, knockoutPredictionDao, groupStandingDao
        )
    }

    @Test
    fun `getAllTeams returns flow from dao`() = runTest {
        val teams = listOf(TeamEntity("1", "México", "A", 1, "🇲🇽"))
        every { teamDao.getAllTeams() } returns flowOf(teams)

        val result = repository.getAllTeams().first()
        assertEquals(1, result.size)
        assertEquals("México", result[0].name)
    }

    @Test
    fun `getTeamsByGroup filters correctly`() = runTest {
        val teams = listOf(
            TeamEntity("1", "México", "A", 1, "🇲🇽"),
            TeamEntity("2", "Brasil", "C", 1, "🇧🇷")
        )
        every { teamDao.getTeamsByGroup("A") } returns flowOf(teams.filter { it.groupLetter == "A" })

        val result = repository.getTeamsByGroup("A").first()
        assertEquals(1, result.size)
        assertEquals("A", result[0].groupLetter)
    }

    @Test
    fun `getAllGroupMatches returns flow`() = runTest {
        val matches = listOf(
            MatchEntity(1, "A", "J1", "2026-01-01", "México", "Sudáfrica")
        )
        every { matchDao.getAllGroupMatches() } returns flowOf(matches)
        val result = repository.getAllGroupMatches().first()
        assertEquals(1, result.size)
    }

    @Test
    fun `calculateTotalPoints sums all daos`() = runTest {
        coEvery { matchDao.getTotalMatchPoints() } returns 500
        coEvery { questionDao.getTotalQuestionPoints() } returns 300
        coEvery { playerPredictionDao.getTotalPoints() } returns 100
        coEvery { knockoutPredictionDao.getTotalPoints() } returns 200

        val total = repository.calculateTotalPoints()
        assertEquals(1100, total)
    }

    @Test
    fun `calculateTotalPoints returns zero when empty`() = runTest {
        coEvery { matchDao.getTotalMatchPoints() } returns 0
        coEvery { questionDao.getTotalQuestionPoints() } returns 0
        coEvery { playerPredictionDao.getTotalPoints() } returns 0
        coEvery { knockoutPredictionDao.getTotalPoints() } returns 0

        val total = repository.calculateTotalPoints()
        assertEquals(0, total)
    }

    @Test
    fun `insertAllData clears and inserts`() = runTest {
        coEvery { teamDao.deleteAll() } returns Unit
        coEvery { teamDao.insertAll(any()) } returns Unit
        coEvery { matchDao.deleteAll() } returns Unit
        coEvery { matchDao.insertAll(any()) } returns Unit
        coEvery { questionDao.deleteAll() } returns Unit
        coEvery { questionDao.insertAll(any()) } returns Unit
        coEvery { playerPredictionDao.deleteAll() } returns Unit
        coEvery { playerPredictionDao.insertAll(any()) } returns Unit
        coEvery { knockoutPredictionDao.deleteAll() } returns Unit
        coEvery { knockoutPredictionDao.insertAll(any()) } returns Unit
        coEvery { groupStandingDao.deleteAll() } returns Unit
        coEvery { groupStandingDao.insertAll(any()) } returns Unit

        repository.insertAllData(
            teams = listOf(TeamEntity("1", "México", "A", 1, "🇲🇽")),
            matches = emptyList(),
            questions = emptyList(),
            playerPredictions = emptyList(),
            knockoutPredictions = emptyList(),
            standings = emptyList()
        )

        coVerify(exactly = 1) { teamDao.deleteAll() }
        coVerify(exactly = 1) { teamDao.insertAll(any()) }
    }

    @Test
    fun `updateMatchPrediction delegates to dao`() = runTest {
        val match = MatchEntity(1, "A", "J1", "2026-01-01", "México", "Sudáfrica", predHome = 2, predAway = 1)
        coEvery { matchDao.updateMatch(any()) } returns Unit

        repository.updateMatchPrediction(match)
        coVerify { matchDao.updateMatch(match) }
    }

    @Test
    fun `updateQuestionPrediction delegates to dao`() = runTest {
        val question = QuestionEntity(1, "Test", predictedAnswer = true)
        coEvery { questionDao.updateQuestion(any()) } returns Unit

        repository.updateQuestionPrediction(question)
        coVerify { questionDao.updateQuestion(question) }
    }
}
