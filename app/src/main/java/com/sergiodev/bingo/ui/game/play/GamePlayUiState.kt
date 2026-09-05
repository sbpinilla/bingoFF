package com.sergiodev.bingo.ui.game.play

import com.sergiodev.bingo.domain.game.WinAnnouncement
import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.domain.model.GameMode

data class GamePlayUiState(
    val numberInput: String = "",
    val selectedLetter: BingoLetter? = null,
    val letterOverridden: Boolean = false,
    val inputError: String? = null,
    val callsByLetter: Map<BingoLetter, List<Int>> = emptyMap(),
    val winners: List<WinAnnouncement> = emptyList(),
    val mode: GameMode = GameMode.COLUMNA,
    val calledCount: Int = 0,
)
