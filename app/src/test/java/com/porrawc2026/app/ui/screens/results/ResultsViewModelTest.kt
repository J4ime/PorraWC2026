package com.porrawc2026.app.ui.screens.results

import com.porrawc2026.app.data.local.entity.*
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
class ResultsViewModelTest {

    private lateinit var repository: PorraRepository
    private lateinit var apiService: ApiService
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        apiService = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshLiveScores handles API error`() = runTest {
        every { repository.getAllMatches() } returns flowOf(emptyList())
        every { repository.getAllQuestions() } returns flowOf(emptyList())
        every { repository.getPlayerPredictions() } returns flowOf(emptyList())
        every { repository.getKnockoutPredictions() } returns flowOf(emptyList())
        coEvery { repository.calculateTotalPoints() } returns 0
        
        coEvery { apiService.getWorldCupMatches(any()) } throws RuntimeException("Network error")
        
        val viewModel = ResultsViewModel(repository, apiService).also { it.ioDispatcher = testDispatcher }
        viewModel.refreshLiveScores()
        
        advanceUntilIdle()
        
        assertFalse(viewModel.isRefreshing.value)
    }
}
