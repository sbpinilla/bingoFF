package com.sergiodev.bingo.ui.game.setup

import com.sergiodev.bingo.MainDispatcherRule
import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.domain.model.GameMode
import com.sergiodev.bingo.domain.repository.BoardRepository
import com.sergiodev.bingo.domain.repository.ImportResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameSetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeBoardRepository : BoardRepository {
        val boards = MutableStateFlow<List<BoardCard>>(emptyList())
        override fun observeBoards(): Flow<List<BoardCard>> = boards
        override suspend fun addBoard(identifier: String, numbers: List<Int>): Result<Unit> =
            Result.success(Unit)
        override suspend fun importBoards(boards: List<BoardCard>): ImportResult =
            ImportResult(imported = 0, skipped = 0)
    }

    @Test
    fun startBlocked_whenZeroBoardsRegistered() = runTest {
        val repository = FakeBoardRepository()
        val viewModel = GameSetupViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onModeSelected(GameMode.COLUMNA)
        runCurrent()

        assertFalse(viewModel.uiState.value.canStart)
    }

    @Test
    fun startAllowed_withAtLeastOneBoardAndColumnaPreselected() = runTest {
        val repository = FakeBoardRepository()
        val viewModel = GameSetupViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        assertFalse(viewModel.uiState.value.canStart)

        repository.boards.value = listOf(BoardCard(id = 1L, identifier = "Casa1", numbers = List(24) { it + 1 }))
        runCurrent()

        // COLUMNA is preselected by default, so registering a board alone is enough to start.
        assertTrue(viewModel.uiState.value.canStart)
        assertEquals(GameMode.COLUMNA, viewModel.uiState.value.selectedMode)

        viewModel.onModeSelected(GameMode.O)
        runCurrent()

        assertTrue(viewModel.uiState.value.canStart)
        assertEquals(GameMode.O, viewModel.uiState.value.selectedMode)
    }

    @Test
    fun exposesAllFiveModes() = runTest {
        val viewModel = GameSetupViewModel(FakeBoardRepository())
        assertEquals(GameMode.entries.toList(), viewModel.uiState.value.availableModes)
    }

    @Test
    fun columnaPreselected_byDefault() = runTest {
        val viewModel = GameSetupViewModel(FakeBoardRepository())
        assertEquals(GameMode.COLUMNA, viewModel.uiState.value.selectedMode)
    }
}
