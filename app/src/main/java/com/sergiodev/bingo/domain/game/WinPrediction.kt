package com.sergiodev.bingo.domain.game

import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.domain.model.GameMode

/**
 * Maximum number of distinct called numbers, session-wide, during which the
 * "Posibles ganadores" panel is computed and shown. Past this ceiling the
 * real game has almost certainly been decided elsewhere, so the panel is
 * hidden for the rest of the session rather than frozen on stale data.
 */
const val MAX_PREDICTION_CALLS = 6

/**
 * A (board, column) pair close to winning. [missing] is how many of the
 * pattern's cells are still unsatisfied.
 */
data class PredictionCandidate(
    val boardId: Long,
    val identifier: String,
    val letter: BingoLetter,
    val missing: Int,
)

/**
 * Columna-mode-only near-win candidates: returns `emptyList()` for every
 * other [GameMode] and once distinct [called] numbers exceed
 * [MAX_PREDICTION_CALLS]. Ceiling is derived from `called.size` — the
 * distinct-numbers-called count, not a raw call-history length — so a
 * duplicate manual entry cannot inflate it.
 *
 * A pair already present in [announced] is excluded even if it still
 * numerically satisfies the qualifying rule (missing at most 2 cells).
 * Results are sorted fewest-missing-first, then by board id, then by
 * column ordinal, for deterministic order.
 */
fun predictPossibleWinners(
    mode: GameMode,
    boards: List<BoardCard>,
    called: Set<Int>,
    announced: Set<AnnouncedWin>,
): List<PredictionCandidate> {
    if (mode != GameMode.COLUMNA || called.size > MAX_PREDICTION_CALLS) return emptyList()

    val candidates = mutableListOf<PredictionCandidate>()
    for (board in boards) {
        for (pattern in mode.patterns) {
            if (AnnouncedWin(board.id, pattern.id) in announced) continue
            val missing = pattern.cells.size - BingoWinChecker.matchCount(board, called, pattern)
            if (missing > 2) continue
            val letter = pattern.cells.first().column
            candidates += PredictionCandidate(
                boardId = board.id,
                identifier = board.identifier,
                letter = letter,
                missing = missing,
            )
        }
    }
    return candidates.sortedWith(compareBy({ it.missing }, { it.boardId }, { it.letter.ordinal }))
}
