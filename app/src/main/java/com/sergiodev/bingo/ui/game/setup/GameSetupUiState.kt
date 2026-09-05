package com.sergiodev.bingo.ui.game.setup

import com.sergiodev.bingo.domain.model.GameMode

data class GameSetupUiState(
    val hasBoards: Boolean = false,
    val availableModes: List<GameMode> = GameMode.entries.toList(),
    val selectedMode: GameMode? = null,
) {
    val canStart: Boolean get() = hasBoards && selectedMode != null
}
