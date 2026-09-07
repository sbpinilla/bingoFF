package com.sergiodev.bingo.ui.game.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiodev.bingo.domain.model.GameMode
import com.sergiodev.bingo.domain.repository.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GameSetupViewModel @Inject constructor(
    repository: BoardRepository,
) : ViewModel() {

    private val selectedMode = MutableStateFlow<GameMode?>(GameMode.COLUMNA)

    val uiState: StateFlow<GameSetupUiState> = combine(
        repository.observeBoards(),
        selectedMode,
    ) { boards, mode ->
        GameSetupUiState(hasBoards = boards.isNotEmpty(), selectedMode = mode)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GameSetupUiState(selectedMode = GameMode.COLUMNA),
    )

    fun onModeSelected(mode: GameMode) {
        selectedMode.value = mode
    }
}
