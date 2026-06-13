package com.porrawc2026.app.ui.screens.knockout

import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.repository.PorraRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KnockoutViewModelTest {

    private lateinit var repository: PorraRepository
    private lateinit var viewModel: KnockoutViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        
        every { repository.getKnockoutMatches() } returns flowOf(emptyList())
        every { repository.getKnockoutPredictions() } returns flowOf(emptyList())
        
        viewModel = KnockoutViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `savePrediction delegates to repository`() = runTest {
        val prediction = KnockoutPredictionEntity(
            matchNumber = 73,
            round = "Octavos",
            homeTeamRef = "1A",
            awayTeamRef = "2B",
            winner = 1
        )
        
        coEvery { repository.updateKnockoutPrediction(any()) } returns Unit
        
        viewModel.savePrediction(prediction)
        
        coVerify { repository.updateKnockoutPrediction(prediction) }
    }

    @Test
    fun `savePrediction with null winner delegates correctly`() = runTest {
        val prediction = KnockoutPredictionEntity(
            matchNumber = 74,
            round = "Octavos",
            homeTeamRef = "1C",
            awayTeamRef = "2D",
            winner = null
        )
        
        coEvery { repository.updateKnockoutPrediction(any()) } returns Unit
        
        viewModel.savePrediction(prediction)
        
        coVerify { repository.updateKnockoutPrediction(prediction) }
    }
}
