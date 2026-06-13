package com.porrawc2026.app.ui.screens.home

import android.content.Context
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.remote.LiveScoreService
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.util.PrefsManager
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
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {

    private lateinit var repository: PorraRepository
    private lateinit var liveScoreService: LiveScoreService
    private lateinit var prefsManager: PrefsManager
    private lateinit var context: Context
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        liveScoreService = mockk(relaxed = true)
        prefsManager = mockk(relaxed = true)
        context = mockk(relaxed = true)
        
        coEvery { prefsManager.getExcelFileNameSync() } returns null
        coEvery { prefsManager.getAutoRefreshSync() } returns true
        coEvery { prefsManager.getNotificationsSync() } returns true
        coEvery { repository.getAllMatches() } returns flowOf(emptyList())
        coEvery { repository.getPlayerPredictions() } returns flowOf(emptyList())
        coEvery { repository.calculateTotalPoints() } returns 0
        coEvery { liveScoreService.fetchScoreUpdates(any()) } returns Pair(emptyList(), emptyList())
        coEvery { liveScoreService.fetchLiveMatchDetails(any()) } returns emptyList()
        
        mockkStatic("com.tom_roush.pdfbox.android.PDFBoxResourceLoader")
        every { com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(any()) } just Runs
        
        viewModel = HomeViewModel(repository, liveScoreService, prefsManager, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("com.tom_roush.pdfbox.android.PDFBoxResourceLoader")
    }

    @Test
    fun `parseMadridDate handles ISO format correctly`() {
        val date = viewModel.parseMadridDate("2026-06-11T21:00:00")
        assertNotNull(date)
    }

    @Test
    fun `parseMadridDate handles UTC format correctly`() {
        val date = viewModel.parseMadridDate("2026-06-11T21:00:00Z")
        assertNotNull(date)
    }

    @Test
    fun `parseMadridDate returns null for blank string`() {
        val date = viewModel.parseMadridDate("")
        assertNull(date)
    }

    @Test
    fun `parseMadridDate returns null for invalid format`() {
        val date = viewModel.parseMadridDate("invalid-date")
        assertNull(date)
    }

    @Test
    fun `matchStatus returns FINISHED when goals are present`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1", dateTime = "",
            homeTeam = "A", awayTeam = "B",
            homeGoals = 2, awayGoals = 1
        )
        
        assertEquals(MatchStatus.FINISHED, viewModel.matchStatus(match))
    }

    @Test
    fun `matchStatus returns UPCOMING for future match`() {
        val futureDate = "2099-12-31T23:59:59"
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1", dateTime = futureDate,
            homeTeam = "A", awayTeam = "B",
            homeGoals = null, awayGoals = null
        )
        
        assertEquals(MatchStatus.UPCOMING, viewModel.matchStatus(match))
    }

    @Test
    fun `matchStatus returns UPCOMING when date is blank`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1", dateTime = "",
            homeTeam = "A", awayTeam = "B",
            homeGoals = null, awayGoals = null
        )
        
        assertEquals(MatchStatus.UPCOMING, viewModel.matchStatus(match))
    }

    @Test
    fun `toDisplay creates correct MatchDisplay`() {
        val match = MatchEntity(
            id = 1, groupName = "Grupo A", matchday = "J1",
            dateTime = "2026-06-11T21:00:00",
            homeTeam = "México", awayTeam = "Sudáfrica",
            predictedHomeGoals = 2, predictedAwayGoals = 1,
            homeGoals = 2, awayGoals = 1,
            pointsEarned = 50,
            tvChannel = "RTVE"
        )
        
        val display = viewModel.toDisplay(match)
        
        assertEquals(1, display.id)
        assertEquals("México", display.homeTeam)
        assertEquals("Sudáfrica", display.awayTeam)
        assertEquals(2, display.homeGoals)
        assertEquals(1, display.awayGoals)
        assertEquals(50, display.pointsEarned)
        assertEquals(MatchStatus.FINISHED, display.status)
    }

    @Test
    fun `dismissValidation sets validationResult to null`() = runTest {
        viewModel.dismissValidation()
        
        assertNull(viewModel.validationResult.value)
    }

    @Test
    fun `toggleNotifications toggles the value`() = runTest {
        val initialValue = viewModel.notificationsEnabled.value
        
        viewModel.toggleNotifications()
        
        assertNotEquals(initialValue, viewModel.notificationsEnabled.value)
    }

    @Test
    fun `toggleAutoRefresh toggles the value`() = runTest {
        val initialValue = viewModel.autoRefreshEnabled.value
        
        viewModel.toggleAutoRefresh()
        
        assertNotEquals(initialValue, viewModel.autoRefreshEnabled.value)
    }

    @Test
    fun `refreshPoints updates totalPoints`() = runTest {
        coEvery { repository.calculateTotalPoints() } returns 1500
        
        viewModel.refreshPoints()
        
        advanceUntilIdle()
        
        assertEquals(1500, viewModel.totalPoints.value)
    }
}
