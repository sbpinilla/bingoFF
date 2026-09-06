package com.sergiodev.bingo.domain.game

import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.domain.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WinPredictionTest {

    // Column-major: B1-5, I1-5, N1,N2,N4,N5, G1-5, O1-5
    private val board1 = BoardCard(
        id = 1L,
        identifier = "Casa1",
        numbers = listOf(
            3, 7, 12, 14, 15, // B
            16, 17, 18, 19, 20, // I
            31, 32, 34, 35, // N
            46, 47, 48, 49, 50, // G
            61, 62, 63, 64, 65, // O
        ),
    )

    private val board2 = BoardCard(
        id = 2L,
        identifier = "Casa2",
        numbers = listOf(
            1, 2, 4, 5, 6, // B
            21, 22, 23, 24, 25, // I
            33, 36, 37, 38, // N
            51, 52, 53, 54, 55, // G
            66, 67, 68, 69, 70, // O
        ),
    )

    @Test
    fun nonColumnaModes_alwaysReturnEmpty_regardlessOfProgressOrCallCount() {
        val boards = listOf(board1)
        // board1 nearly completes each non-Columna pattern with very few total calls.
        val nearlyAllNumbers = board1.numbers.dropLast(1).toSet()

        listOf(GameMode.O, GameMode.L, GameMode.I, GameMode.CARTON_COMPLETO).forEach { mode ->
            val result = predictPossibleWinners(
                mode = mode,
                boards = boards,
                called = nearlyAllNumbers,
                announced = emptySet(),
            )
            assertTrue("mode=$mode should be empty", result.isEmpty())
        }
    }

    @Test
    fun columna_exactlySixCalls_isIncluded() {
        // board1's B column: 3,7,12,14,15 — 5 calls, plus 1 unrelated call = 6 distinct.
        val called = setOf(3, 7, 12, 14, 46)
        val calledSix = called + 99 // 99 is out of 1..75 range but still a distinct called number

        val result = predictPossibleWinners(
            mode = GameMode.COLUMNA,
            boards = listOf(board1),
            called = calledSix,
            announced = emptySet(),
        )

        assertEquals(6, calledSix.size)
        assertTrue(result.any { it.boardId == 1L && it.letter == BingoLetter.B })
    }

    @Test
    fun columna_sevenCalls_isExcluded() {
        val calledSeven = setOf(3, 7, 12, 14, 46, 99, 98)

        val result = predictPossibleWinners(
            mode = GameMode.COLUMNA,
            boards = listOf(board1),
            called = calledSeven,
            announced = emptySet(),
        )

        assertEquals(7, calledSeven.size)
        assertTrue(result.isEmpty())
    }

    @Test
    fun columna_belowThreshold_isExcluded() {
        // Only 2 of board1's B column numbers called -> missing 3, threshold requires missing <= 2.
        val called = setOf(3, 7)

        val result = predictPossibleWinners(
            mode = GameMode.COLUMNA,
            boards = listOf(board1),
            called = called,
            announced = emptySet(),
        )

        assertTrue(result.none { it.boardId == 1L && it.letter == BingoLetter.B })
    }

    @Test
    fun alreadyAnnouncedPair_isExcludedEvenIfStillQualifying() {
        val called = setOf(3, 7, 12, 14, 15) // completes board1's B column
        val announced = setOf(AnnouncedWin(board1.id, "COLUMN_B"))

        val result = predictPossibleWinners(
            mode = GameMode.COLUMNA,
            boards = listOf(board1),
            called = called,
            announced = announced,
        )

        assertTrue(result.none { it.boardId == 1L && it.letter == BingoLetter.B })
    }

    @Test
    fun boardQualifyingOnTwoColumns_producesTwoEntries() {
        // board1's B column: 3,7,12,14,15 ; board1's I column: 16,17,18,19,20.
        // Both fully called within the 6-call ceiling isn't possible (10 numbers > 6),
        // so instead qualify both via missing <= 2 each: 3 of B's 5 and 3 of I's 5.
        val called = setOf(3, 7, 12, 16, 17, 18) // 6 distinct calls: B missing 2, I missing 2

        val result = predictPossibleWinners(
            mode = GameMode.COLUMNA,
            boards = listOf(board1),
            called = called,
            announced = emptySet(),
        )

        assertTrue(result.any { it.boardId == 1L && it.letter == BingoLetter.B })
        assertTrue(result.any { it.boardId == 1L && it.letter == BingoLetter.I })
        assertEquals(2, result.count { it.boardId == 1L })
    }

    @Test
    fun results_sortedFewestMissingFirst_thenBoardId_thenLetterOrdinal() {
        // board1's N column (31,32,34,35 + FREE): 3 real numbers called -> missing 1.
        // board2's N column (33,36,37,38 + FREE): 2 real numbers called -> missing 2.
        val called = setOf(31, 32, 34, 33, 36) // 5 distinct calls, within the ceiling

        val result = predictPossibleWinners(
            mode = GameMode.COLUMNA,
            boards = listOf(board2, board1),
            called = called,
            announced = emptySet(),
        )

        assertEquals(2, result.size)
        // Fewest-missing-first: board1's N column (missing 1) sorts before board2's (missing 2).
        assertEquals(board1.id, result.first().boardId)
        assertEquals(BingoLetter.N, result.first().letter)
        assertEquals(1, result.first().missing)
        assertEquals(board2.id, result.last().boardId)
        assertEquals(2, result.last().missing)
        for (i in 0 until result.size - 1) {
            assertTrue(result[i].missing <= result[i + 1].missing)
        }
    }
}
