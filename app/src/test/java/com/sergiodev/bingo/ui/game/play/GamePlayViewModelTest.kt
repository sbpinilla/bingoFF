package com.sergiodev.bingo.ui.game.play

import androidx.lifecycle.SavedStateHandle
import com.sergiodev.bingo.MainDispatcherRule
import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.domain.model.GameMode
import com.sergiodev.bingo.domain.repository.BoardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GamePlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeBoardRepository(initial: List<BoardCard> = emptyList()) : BoardRepository {
        val boards = MutableStateFlow(initial)
        override fun observeBoards(): Flow<List<BoardCard>> = boards
        override suspend fun addBoard(identifier: String, numbers: List<Int>): Result<Unit> =
            Result.success(Unit)
    }

    // Column-major: B1-5, I1-5, N1,N2,N4,N5, G1-5, O1-5
    private val board1 = BoardCard(
        id = 1L,
        identifier = "Casa1",
        numbers = listOf(3, 7, 12, 14, 15, 16, 17, 18, 19, 20, 31, 32, 34, 35, 46, 47, 48, 49, 50, 61, 62, 63, 64, 65),
    )
    private val board2 = BoardCard(
        id = 2L,
        identifier = "Casa2",
        numbers = listOf(1, 2, 4, 5, 6, 21, 22, 23, 24, 25, 33, 36, 37, 38, 51, 52, 53, 54, 55, 66, 67, 68, 69, 70),
    )

    private fun handle(calledNumbers: List<Int>? = null, mode: GameMode = GameMode.COLUMNA): SavedStateHandle {
        val map = mutableMapOf<String, Any?>("mode" to mode.name)
        if (calledNumbers != null) map["calledNumbers"] = ArrayList(calledNumbers)
        return SavedStateHandle(map)
    }

    @Test
    fun typingNumber_autoSelectsLetterFromRange() = runTest {
        val viewModel = GamePlayViewModel(FakeBoardRepository(), handle())
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onNumberInputChanged("42")
        runCurrent()

        assertEquals(BingoLetter.N, viewModel.uiState.value.selectedLetter)
        assertEquals(false, viewModel.uiState.value.letterOverridden)
    }

    @Test
    fun overrideBeforeNumber_confirmsOverriddenLetterWhenRangeMatches() = runTest {
        val viewModel = GamePlayViewModel(FakeBoardRepository(listOf(board1)), handle())
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onLetterSelected(BingoLetter.B)
        viewModel.onNumberInputChanged("7")
        runCurrent()
        viewModel.onSubmitCall()
        runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.inputError)
        assertEquals(listOf(7), state.callsByLetter[BingoLetter.B])
        assertEquals("", state.numberInput)
    }

    @Test
    fun overriddenLetterContradictingRange_blocksSubmitWithError() = runTest {
        val viewModel = GamePlayViewModel(FakeBoardRepository(listOf(board1)), handle())
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onLetterSelected(BingoLetter.G)
        viewModel.onNumberInputChanged("42") // 42 is in N's range, not G's
        runCurrent()
        viewModel.onSubmitCall()
        runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.inputError)
        assertTrue(state.callsByLetter[BingoLetter.N].isNullOrEmpty())
    }

    @Test
    fun cumulativeWinners_secondBoardAnnouncedWithoutDuplicatingFirst() = runTest {
        val repository = FakeBoardRepository(listOf(board1, board2))
        val viewModel = GamePlayViewModel(repository, handle())
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        // Complete board1's B column: 3,7,12,14,15
        listOf(3, 7, 12, 14, 15).forEach { number ->
            viewModel.onNumberInputChanged(number.toString())
            runCurrent()
            viewModel.onSubmitCall()
            runCurrent()
        }

        var state = viewModel.uiState.value
        assertTrue(state.winners.any { it.boardId == 1L && it.patternId == "COLUMN_B" })
        assertEquals(1, state.winners.size)

        // Complete board2's B column: 1,2,4,5,6
        listOf(1, 2, 4, 5, 6).forEach { number ->
            viewModel.onNumberInputChanged(number.toString())
            runCurrent()
            viewModel.onSubmitCall()
            runCurrent()
        }

        state = viewModel.uiState.value
        assertTrue(state.winners.any { it.boardId == 2L && it.patternId == "COLUMN_B" })
        // board1's COLUMN_B still present exactly once (not re-announced)
        assertEquals(1, state.winners.count { it.boardId == 1L && it.patternId == "COLUMN_B" })
    }

    @Test
    fun replayRebuild_reproducesWinnersAfterSimulatedProcessDeath() = runTest {
        val repository = FakeBoardRepository(listOf(board1))
        val restoredHandle = handle(calledNumbers = listOf(3, 7, 12, 14, 15))
        val viewModel = GamePlayViewModel(repository, restoredHandle)
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.winners.any { it.boardId == 1L && it.patternId == "COLUMN_B" })
        assertEquals(5, state.calledCount)
    }

    @Test
    fun columnaMode_populatesPossibleWinnersAfterThreeCalls() = runTest {
        val repository = FakeBoardRepository(listOf(board1))
        val viewModel = GamePlayViewModel(repository, handle(mode = GameMode.COLUMNA))
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        // Call 3 of board1's B column numbers (3,7,12,14,15) -> missing 2, qualifies.
        listOf(3, 7, 12).forEach { number ->
            viewModel.onNumberInputChanged(number.toString())
            runCurrent()
            viewModel.onSubmitCall()
            runCurrent()
        }

        val state = viewModel.uiState.value
        assertTrue(state.possibleWinners.any { it.boardId == 1L && it.letter == BingoLetter.B })
    }

    @Test
    fun nonColumnaMode_possibleWinnersAlwaysEmpty() = runTest {
        val repository = FakeBoardRepository(listOf(board1))
        val viewModel = GamePlayViewModel(repository, handle(mode = GameMode.O))
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        listOf(3, 7, 12).forEach { number ->
            viewModel.onNumberInputChanged(number.toString())
            runCurrent()
            viewModel.onSubmitCall()
            runCurrent()
        }

        val state = viewModel.uiState.value
        assertTrue(state.possibleWinners.isEmpty())
    }
}
