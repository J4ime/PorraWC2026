package com.porrawc2026.app.ui.screens.goalscorers

import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.remote.*
import com.porrawc2026.app.data.repository.PorraRepository
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
class GoalscorersViewModelTest {

    private lateinit var repository: PorraRepository
    private lateinit var apiService: ApiService
    private lateinit var viewModel: GoalscorersViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        apiService = mockk(relaxed = true)
        
        every { repository.getPlayerPredictions() } returns flowOf(emptyList())
        coEvery { apiService.getWorldCupScorers() } returns ScorersResponse(0, emptyList())
        
        viewModel = GoalscorersViewModel(repository, apiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadTopScorers handles empty response`() = runTest {
        val scorersResponse = ScorersResponse(count = 0, scorers = emptyList())
        
        coEvery { apiService.getWorldCupScorers() } returns scorersResponse
        
        viewModel.loadTopScorers()
        
        advanceUntilIdle()
        
        assertTrue(viewModel.topScorers.value.isEmpty())
    }

    @Test
    fun `loadTopScorers handles API error gracefully`() = runTest {
        coEvery { apiService.getWorldCupScorers() } throws RuntimeException("Network error")
        
        viewModel.loadTopScorers()
        
        advanceUntilIdle()
        
        assertTrue(viewModel.topScorers.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadTopScorers sets isLoading correctly`() = runTest {
        val scorersResponse = ScorersResponse(count = 0, scorers = emptyList())
        coEvery { apiService.getWorldCupScorers() } returns scorersResponse
        
        viewModel.loadTopScorers()
        
        advanceUntilIdle()
        
        assertFalse(viewModel.isLoading.value)
    }
}
