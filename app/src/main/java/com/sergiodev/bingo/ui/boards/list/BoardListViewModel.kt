package com.sergiodev.bingo.ui.boards.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiodev.bingo.domain.repository.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * List-only projection — no search/filter state, board lookup was descoped
 * from this change.
 */
@HiltViewModel
class BoardListViewModel @Inject constructor(
    repository: BoardRepository,
) : ViewModel() {

    val uiState: StateFlow<BoardListUiState> = repository.observeBoards()
        .map { boards -> BoardListUiState(boards = boards) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BoardListUiState(),
        )
}
