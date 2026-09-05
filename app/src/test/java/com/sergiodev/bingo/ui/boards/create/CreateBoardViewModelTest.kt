package com.sergiodev.bingo.ui.boards.create

import com.sergiodev.bingo.MainDispatcherRule
import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.domain.repository.BoardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateBoardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeBoardRepository(
        private val existingIdentifiers: Set<String> = emptySet(),
    ) : BoardRepository {
        val boards = MutableStateFlow<List<BoardCard>>(emptyList())
        var lastAdded: Pair<String, List<Int>>? = null

        override fun observeBoards(): Flow<List<BoardCard>> = boards

        override suspend fun addBoard(identifier: String, numbers: List<Int>): Result<Unit> {
            if (identifier in existingIdentifiers) return Result.failure(Exception("duplicate"))
            lastAdded = identifier to numbers
            return Result.success(Unit)
        }
    }

    private fun validNumbers(): Map<BingoLetter, List<String>> = mapOf(
        BingoLetter.B to listOf("1", "2", "3", "4", "5"),
        BingoLetter.I to listOf("16", "17", "18", "19", "20"),
        BingoLetter.N to listOf("31", "32", "34", "35"),
        BingoLetter.G to listOf("46", "47", "48", "49", "50"),
        BingoLetter.O to listOf("61", "62", "63", "64", "65"),
    )

    @Test
    fun rangeValidation_rejectsOutOfRangeNumber() = runTest {
        val repository = FakeBoardRepository()
        val viewModel = CreateBoardViewModel(repository)

        viewModel.onIdentifierChange("Casa1")
        val numbers = validNumbers().toMutableMap()
        numbers[BingoLetter.B] = listOf("16", "2", "3", "4", "5") // 16 out of B's 1-15 range
        numbers.forEach { (letter, values) ->
            values.forEachIndexed { index, value -> viewModel.onNumberChange(letter, index, value) }
        }
        viewModel.onSubmit()
        runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.fieldErrors[BingoLetter.B]?.get(0))
        assertNull(repository.lastAdded)
    }

    @Test
    fun duplicateNumberWithinBoard_isRejected() = runTest {
        val repository = FakeBoardRepository()
        val viewModel = CreateBoardViewModel(repository)

        viewModel.onIdentifierChange("Casa1")
        val numbers = validNumbers().toMutableMap()
        numbers[BingoLetter.I] = listOf("1", "17", "18", "19", "20") // "1" duplicates B's first number
        numbers.forEach { (letter, values) ->
            values.forEachIndexed { index, value -> viewModel.onNumberChange(letter, index, value) }
        }
        viewModel.onSubmit()
        runCurrent()

        val state = viewModel.uiState.value
        val hasDuplicateError = state.fieldErrors.values.flatten().any { it != null }
        assertTrue(hasDuplicateError)
        assertNull(repository.lastAdded)
    }

    @Test
    fun duplicateIdentifier_surfacesInlineError() = runTest {
        val repository = FakeBoardRepository(existingIdentifiers = setOf("Casa1"))
        val viewModel = CreateBoardViewModel(repository)

        viewModel.onIdentifierChange("Casa1")
        validNumbers().forEach { (letter, values) ->
            values.forEachIndexed { index, value -> viewModel.onNumberChange(letter, index, value) }
        }
        viewModel.onSubmit()
        runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.identifierError)
        assertFalse(state.submitSuccess)
    }

    @Test
    fun validSubmission_succeedsAndOrdersNumbersColumnMajor() = runTest {
        val repository = FakeBoardRepository()
        val viewModel = CreateBoardViewModel(repository)

        viewModel.onIdentifierChange("Casa1")
        validNumbers().forEach { (letter, values) ->
            values.forEachIndexed { index, value -> viewModel.onNumberChange(letter, index, value) }
        }
        viewModel.onSubmit()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.submitSuccess)
        assertNull(state.identifierError)
        assertEquals("Casa1", repository.lastAdded?.first)
        assertEquals(
            listOf(1, 2, 3, 4, 5, 16, 17, 18, 19, 20, 31, 32, 34, 35, 46, 47, 48, 49, 50, 61, 62, 63, 64, 65),
            repository.lastAdded?.second,
        )
    }

    companion object {
        private fun assertEquals(expected: Any?, actual: Any?) = org.junit.Assert.assertEquals(expected, actual)
    }
}
