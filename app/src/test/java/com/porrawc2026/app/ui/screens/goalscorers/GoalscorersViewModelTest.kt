package com.porrawc2026.app.ui.screens.goalscorers

import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.remote.*
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.util.GoalEventBus
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoalscorersViewModelTest {

    private lateinit var repository: PorraRepository
    private lateinit var liveScoreService: LiveScoreService
    private lateinit var goalEventBus: GoalEventBus
    private lateinit var viewModel: GoalscorersViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        liveScoreService = mockk(relaxed = true)
        goalEventBus = GoalEventBus()

        every { repository.getPlayerPredictions() } returns flowOf(emptyList())
        every { repository.getAllMatches() } returns flowOf(emptyList())
        coEvery { liveScoreService.fetchTopScorers(any()) } returns emptyList()

        viewModel = GoalscorersViewModel(repository, liveScoreService, goalEventBus)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadTopScorers handles empty response`() = runTest {
        coEvery { liveScoreService.fetchTopScorers(any()) } returns emptyList()

        viewModel.refresh()

        advanceUntilIdle()

        assertTrue(viewModel.topScorers.value.isEmpty())
    }

    @Test
    fun `loadTopScorers handles API error gracefully`() = runTest {
        coEvery { liveScoreService.fetchTopScorers(any()) } throws RuntimeException("Network error")

        viewModel.refresh()

        advanceUntilIdle()

        assertTrue(viewModel.topScorers.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadTopScorers sets isLoading correctly`() = runTest {
        coEvery { liveScoreService.fetchTopScorers(any()) } returns emptyList()

        viewModel.refresh()

        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
    }
}
