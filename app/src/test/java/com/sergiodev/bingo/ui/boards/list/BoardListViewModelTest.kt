package com.sergiodev.bingo.ui.boards.list

import com.sergiodev.bingo.MainDispatcherRule
import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.domain.repository.BoardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoardListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeBoardRepository : BoardRepository {
        val boards = MutableStateFlow<List<BoardCard>>(emptyList())
        override fun observeBoards(): Flow<List<BoardCard>> = boards
        override suspend fun addBoard(identifier: String, numbers: List<Int>): Result<Unit> =
            Result.success(Unit)
    }

    @Test
    fun initialState_isEmptyList() = runTest {
        val viewModel = BoardListViewModel(FakeBoardRepository())
        assertTrue(viewModel.uiState.value.boards.isEmpty())
    }

    @Test
    fun emitsBoardListInInsertionOrder_asRepositoryUpdates() = runTest {
        val repository = FakeBoardRepository()
        val viewModel = BoardListViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val board1 = BoardCard(id = 1L, identifier = "Casa1", numbers = List(24) { it + 1 })
        val board2 = BoardCard(id = 2L, identifier = "Casa2", numbers = List(24) { it + 30 })
        repository.boards.value = listOf(board1, board2)
        runCurrent()

        assertEquals(listOf(board1, board2), viewModel.uiState.value.boards)
    }
}
