package com.sergiodev.bingo.domain.game

import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.domain.model.WinPattern

/**
 * Pure, stateless pattern evaluation. Marked state is never persisted:
 * a cell is satisfied when it is FREE (numberAt returns null) or its number
 * is present in [called].
 */
object BingoWinChecker {

    fun isSatisfied(board: BoardCard, called: Set<Int>, pattern: WinPattern): Boolean =
        pattern.cells.all { cell ->
            val number = board.numberAt(cell)
            number == null || number in called
        }

    /**
     * Evaluates every board against the active mode's patterns and returns
     * only the newly completed (board, pattern) pairs not already present in
     * [announced]. Evaluation is strictly per-board, per-pattern — never a
     * global tally.
     */
    fun newWins(
        boards: List<BoardCard>,
        called: Set<Int>,
        mode: com.sergiodev.bingo.domain.model.GameMode,
        announced: Set<AnnouncedWin>,
    ): List<WinAnnouncement> {
        val result = mutableListOf<WinAnnouncement>()
        for (board in boards) {
            for (pattern in mode.patterns) {
                val key = AnnouncedWin(board.id, pattern.id)
                if (key in announced) continue
                if (isSatisfied(board, called, pattern)) {
                    result += WinAnnouncement(
                        boardId = board.id,
                        sequentialNumber = board.id,
                        identifier = board.identifier,
                        patternId = pattern.id,
                    )
                }
            }
        }
        return result
    }
}
