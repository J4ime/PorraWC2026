package com.porrawc2026.app.ui.screens.questions

import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.QuestionEntity
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
class QuestionsViewModelTest {

    private lateinit var repository: PorraRepository
    private lateinit var viewModel: QuestionsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        
        every { repository.getAllQuestions() } returns flowOf(emptyList())
        
        viewModel = QuestionsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `clearMessage sets evalMessage to null`() = runTest {
        viewModel.clearMessage()
        
        assertNull(viewModel.evalMessage.value)
    }
}
