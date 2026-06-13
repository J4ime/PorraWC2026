package com.porrawc2026.app.ui.screens.groups

import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.domain.model.PointsCalculator
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupsViewModelTest {

    private lateinit var repository: PorraRepository
    private lateinit var viewModel: GroupsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        
        every { repository.getAllTeams() } returns flowOf(emptyList())
        every { repository.getAllMatches() } returns flowOf(emptyList())
        every { repository.getKnockoutPredictions() } returns flowOf(emptyList())
        
        viewModel = GroupsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `savePrediction calculates points and updates repository`() = runTest {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1", dateTime = "",
            homeTeam = "Team A", awayTeam = "Team B",
            predictedHomeGoals = 2, predictedAwayGoals = 1,
            homeGoals = 2, awayGoals = 1
        )
        
        val slot = slot<MatchEntity>()
        coEvery { repository.updateMatchPrediction(capture(slot)) } returns Unit
        
        viewModel.savePrediction(match)
        
        assertEquals(50, slot.captured.pointsEarned)
        coVerify { repository.updateMatchPrediction(any()) }
    }

    @Test
    fun `savePrediction with wrong result gives 30 points`() = runTest {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1", dateTime = "",
            homeTeam = "Team A", awayTeam = "Team B",
            predictedHomeGoals = 3, predictedAwayGoals = 0,
            homeGoals = 2, awayGoals = 1
        )
        
        val slot = slot<MatchEntity>()
        coEvery { repository.updateMatchPrediction(capture(slot)) } returns Unit
        
        viewModel.savePrediction(match)
        
        assertEquals(30, slot.captured.pointsEarned)
    }

    @Test
    fun `savePrediction with null prediction gives 0 points`() = runTest {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1", dateTime = "",
            homeTeam = "Team A", awayTeam = "Team B",
            predictedHomeGoals = null, predictedAwayGoals = null,
            homeGoals = 2, awayGoals = 1
        )
        
        val slot = slot<MatchEntity>()
        coEvery { repository.updateMatchPrediction(capture(slot)) } returns Unit
        
        viewModel.savePrediction(match)
        
        assertEquals(0, slot.captured.pointsEarned)
    }

    @Test
    fun `getGroupTeams delegates to repository`() = runTest {
        val group = "A"
        viewModel.getGroupTeams(group)
        
        verify { repository.getTeamsByGroup(group) }
    }

    @Test
    fun `getGroupMatches delegates to repository`() = runTest {
        val group = "B"
        viewModel.getGroupMatches(group)
        
        verify { repository.getGroupMatches(group) }
    }
}
