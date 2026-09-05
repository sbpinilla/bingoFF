package com.sergiodev.bingo.ui.boards.create

import com.sergiodev.bingo.domain.model.BingoLetter

/**
 * 24 manual number entry fields grouped by column (5 each B/I/G/O, 4 for N,
 * since FREE is never entered).
 */
data class CreateBoardUiState(
    val identifier: String = "",
    val numbers: Map<BingoLetter, List<String>> = defaultNumbers(),
    val fieldErrors: Map<BingoLetter, List<String?>> = emptyMap(),
    val identifierError: String? = null,
    val submitSuccess: Boolean = false,
) {
    companion object {
        fun defaultNumbers(): Map<BingoLetter, List<String>> =
            BingoLetter.entries.associateWith { letter ->
                List(if (letter == BingoLetter.N) 4 else 5) { "" }
            }
    }
}
