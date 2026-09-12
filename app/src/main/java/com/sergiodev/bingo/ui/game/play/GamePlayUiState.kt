package com.sergiodev.bingo.ui.game.play

import com.sergiodev.bingo.domain.game.PredictionCandidate
import com.sergiodev.bingo.domain.game.WinAnnouncement
import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.domain.model.GameMode

data class GamePlayUiState(
    val numberInput: String = "",
    val selectedLetter: BingoLetter? = null,
    val letterOverridden: Boolean = false,
    val inputError: GamePlayInputErrorReason? = null,
    val callsByLetter: Map<BingoLetter, List<Int>> = emptyMap(),
    val winners: List<WinAnnouncement> = emptyList(),
    val mode: GameMode = GameMode.COLUMNA,
    val calledCount: Int = 0,
    val possibleWinners: List<PredictionCandidate> = emptyList(),
)

/**
 * Reasons `GamePlayViewModel.onSubmitCall()` can block confirming a pending call with.
 * The Composable layer resolves each case to display text via `stringResource()`.
 */
sealed interface GamePlayInputErrorReason {
    data object InvalidNumber : GamePlayInputErrorReason
    data object LetterMismatch : GamePlayInputErrorReason
    data class DuplicateCall(val number: Int) : GamePlayInputErrorReason
}
