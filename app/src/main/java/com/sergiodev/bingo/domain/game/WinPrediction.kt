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
    val letter: BingoLetter?,
    val missing: Int,
)

/**
 * Per-mode missing-cell threshold below which a (board, pattern) pair
 * qualifies for "Posibles ganadores": COLUMNA needs at most 2 cells left,
 * O/L/I need at most 3, and CARTON_COMPLETO needs at most 10.
 */
private fun missingThreshold(mode: GameMode): Int = when (mode) {
    GameMode.COLUMNA -> 2
    GameMode.O, GameMode.L, GameMode.I -> 3
    GameMode.CARTON_COMPLETO -> 10
}

/**
 * Near-win candidates for every [GameMode], re-evaluated on every newly
 * called number. Only COLUMNA is subject to a call-count ceiling: once
 * distinct [called] numbers exceed [MAX_PREDICTION_CALLS] the COLUMNA panel
 * is hidden for the rest of the session rather than frozen on stale data.
 * Ceiling is derived from `called.size` — the distinct-numbers-called
 * count, not a raw call-history length — so a duplicate manual entry cannot
 * inflate it. O, L, I, and CARTON_COMPLETO have no such ceiling.
 *
 * A pair already present in [announced] is excluded even if it still
 * numerically satisfies the qualifying rule. Results are sorted
 * fewest-missing-first, then by board id, then by column ordinal (COLUMNA
 * candidates before any tie without a letter), for deterministic order.
 */
fun predictPossibleWinners(
    mode: GameMode,
    boards: List<BoardCard>,
    called: Set<Int>,
    announced: Set<AnnouncedWin>,
): List<PredictionCandidate> {
    if (mode == GameMode.COLUMNA && called.size > MAX_PREDICTION_CALLS) return emptyList()

    val threshold = missingThreshold(mode)
    val candidates = mutableListOf<PredictionCandidate>()
    for (board in boards) {
        for (pattern in mode.patterns) {
            if (AnnouncedWin(board.id, pattern.id) in announced) continue
            val missing = pattern.cells.size - BingoWinChecker.matchCount(board, called, pattern)
            if (missing > threshold) continue
            val letter = if (mode == GameMode.COLUMNA) pattern.cells.first().column else null
            candidates += PredictionCandidate(
                boardId = board.id,
                identifier = board.identifier,
                letter = letter,
                missing = missing,
            )
        }
    }
    return candidates.sortedWith(compareBy({ it.missing }, { it.boardId }, { it.letter?.ordinal ?: 0 }))
}
