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
    val identifierError: CreateBoardErrorReason? = null,
    val submitSuccess: Boolean = false,
) {
    companion object {
        fun defaultNumbers(): Map<BingoLetter, List<String>> =
            BingoLetter.entries.associateWith { letter ->
                List(if (letter == BingoLetter.N) 4 else 5) { "" }
            }
    }
}

/**
 * Reasons `CreateBoardViewModel.onSubmit()` can block board creation with.
 * The Composable layer resolves each case to display text via `stringResource()`.
 */
sealed interface CreateBoardErrorReason {
    data object BlankIdentifier : CreateBoardErrorReason
    data object DuplicateIdentifier : CreateBoardErrorReason
}
