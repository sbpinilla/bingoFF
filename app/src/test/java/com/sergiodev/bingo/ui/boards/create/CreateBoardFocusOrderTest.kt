package com.sergiodev.bingo.ui.boards.create

import com.sergiodev.bingo.domain.model.BingoLetter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * `flatFieldIndex` maps a (letter, column-index) grid position to its flat position in the
 * 24-entry `FocusRequester` list, in reading order B(0-4) -> I(5-9) -> N(10-13) -> G(14-18) ->
 * O(19-23). N only has 4 real value slots because the FREE cell is rendered separately and is
 * never part of `CreateBoardUiState.numbers`, so no skip logic is needed here.
 */
@RunWith(Parameterized::class)
class CreateBoardFocusOrderTest(
    private val letter: BingoLetter,
    private val index: Int,
    private val expectedFlatIndex: Int,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "flatFieldIndex({0}, {1}) == {2}")
        fun data(): List<Array<Any?>> = listOf(
            // B: 0-4
            arrayOf(BingoLetter.B, 0, 0),
            arrayOf(BingoLetter.B, 1, 1),
            arrayOf(BingoLetter.B, 2, 2),
            arrayOf(BingoLetter.B, 3, 3),
            arrayOf(BingoLetter.B, 4, 4),
            // I: 5-9
            arrayOf(BingoLetter.I, 0, 5),
            arrayOf(BingoLetter.I, 1, 6),
            arrayOf(BingoLetter.I, 2, 7),
            arrayOf(BingoLetter.I, 3, 8),
            arrayOf(BingoLetter.I, 4, 9),
            // N: 10-13 (only 4 real slots — FREE is not part of `numbers`)
            arrayOf(BingoLetter.N, 0, 10),
            arrayOf(BingoLetter.N, 1, 11),
            arrayOf(BingoLetter.N, 2, 12),
            arrayOf(BingoLetter.N, 3, 13),
            // G: 14-18
            arrayOf(BingoLetter.G, 0, 14),
            arrayOf(BingoLetter.G, 1, 15),
            arrayOf(BingoLetter.G, 2, 16),
            arrayOf(BingoLetter.G, 3, 17),
            arrayOf(BingoLetter.G, 4, 18),
            // O: 19-23
            arrayOf(BingoLetter.O, 0, 19),
            arrayOf(BingoLetter.O, 1, 20),
            arrayOf(BingoLetter.O, 2, 21),
            arrayOf(BingoLetter.O, 3, 22),
            arrayOf(BingoLetter.O, 4, 23),
        )
    }

    @Test
    fun flatFieldIndex_returnsExpectedFlatPosition() {
        val numbers = CreateBoardUiState.defaultNumbers()

        assertEquals(expectedFlatIndex, flatFieldIndex(numbers, letter, index))
    }
}

class CreateBoardFocusOrderBoundaryTest {
    private val numbers = CreateBoardUiState.defaultNumbers()

    @Test
    fun flatFieldIndex_firstNRow_is10() {
        assertEquals(10, flatFieldIndex(numbers, BingoLetter.N, 0))
    }

    @Test
    fun flatFieldIndex_lastNRow_is13() {
        assertEquals(13, flatFieldIndex(numbers, BingoLetter.N, 3))
    }

    @Test
    fun flatFieldIndex_lastOverallField_is23() {
        assertEquals(23, flatFieldIndex(numbers, BingoLetter.O, 4))
    }
}
