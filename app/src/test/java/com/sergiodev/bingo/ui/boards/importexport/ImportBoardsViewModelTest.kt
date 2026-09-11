package com.sergiodev.bingo.ui.boards.importexport

import com.sergiodev.bingo.MainDispatcherRule
import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.domain.repository.BoardRepository
import com.sergiodev.bingo.domain.repository.ImportResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImportBoardsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeBoardRepository : BoardRepository {
        val boards = MutableStateFlow<List<BoardCard>>(emptyList())
        var lastImported: List<BoardCard>? = null
        var resultToReturn = ImportResult(imported = 0, skipped = 0)

        override fun observeBoards(): Flow<List<BoardCard>> = boards

        override suspend fun addBoard(identifier: String, numbers: List<Int>): Result<Unit> =
            Result.success(Unit)

        override suspend fun importBoards(boards: List<BoardCard>): ImportResult {
            lastImported = boards
            return resultToReturn
        }
    }

    @Test
    fun submit_malformedJson_setsErrorAndClearsText() = runTest {
        val repository = FakeBoardRepository()
        val viewModel = ImportBoardsViewModel(repository)

        viewModel.onJsonTextChange("not valid json {{{")
        viewModel.onSubmit()
        runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.jsonError)
        assertEquals("", state.jsonText)
        assertNull(repository.lastImported)
    }

    @Test
    fun submit_blankText_setsErrorWithoutCallingRepository() = runTest {
        val repository = FakeBoardRepository()
        val viewModel = ImportBoardsViewModel(repository)

        viewModel.onJsonTextChange("   ")
        viewModel.onSubmit()
        runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.jsonError)
        assertNull(repository.lastImported)
    }

    @Test
    fun submit_wellFormedJson_callsImportAndSetsResultMessage() = runTest {
        val repository = FakeBoardRepository()
        repository.resultToReturn = ImportResult(imported = 1, skipped = 1)
        val viewModel = ImportBoardsViewModel(repository)

        val board = BoardCard(id = 1, identifier = "Casa1", numbers = List(24) { it + 1 })
        val json = com.sergiodev.bingo.data.json.BoardJsonCodec.encode(listOf(board))

        viewModel.onJsonTextChange(json)
        viewModel.onSubmit()
        runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.jsonError)
        assertEquals("", state.jsonText)
        assertEquals(listOf(board), repository.lastImported)
        assertEquals(ImportResultSummary(imported = 1, skipped = 1), state.resultSummary)
    }
}
