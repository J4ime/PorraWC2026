package com.porrawc2026.app.ui.screens.players

import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlayersViewModelTest {

    private lateinit var repository: PorraRepository
    private lateinit var viewModel: PlayersViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        
        every { repository.getPlayerPredictions() } returns flowOf(emptyList())
        
        viewModel = PlayersViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `savePlayer delegates to repository`() = runTest {
        val prediction = PlayerPredictionEntity(
            rank = 1,
            playerName = "1er Goleador",
            predictedName = "Mbappé",
            pointsPerGoal = 50
        )
        
        coEvery { repository.updatePlayerPrediction(any()) } returns Unit
        
        viewModel.savePlayer(prediction)
        
        coVerify { repository.updatePlayerPrediction(prediction) }
    }

    @Test
    fun `savePlayer with null predictedName delegates correctly`() = runTest {
        val prediction = PlayerPredictionEntity(
            rank = 2,
            playerName = "2do Goleador",
            predictedName = null,
            pointsPerGoal = 30
        )
        
        coEvery { repository.updatePlayerPrediction(any()) } returns Unit
        
        viewModel.savePlayer(prediction)
        
        coVerify { repository.updatePlayerPrediction(prediction) }
    }

    @Test
    fun `savePlayer for rank 3 uses correct points per goal`() = runTest {
        val prediction = PlayerPredictionEntity(
            rank = 3,
            playerName = "3er Goleador",
            predictedName = "Haaland",
            pointsPerGoal = 10
        )
        
        coEvery { repository.updatePlayerPrediction(any()) } returns Unit
        
        viewModel.savePlayer(prediction)
        
        coVerify { repository.updatePlayerPrediction(prediction) }
    }
}
