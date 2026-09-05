package com.sergiodev.bingo.ui.boards.list

import com.sergiodev.bingo.domain.model.BoardCard

data class BoardListUiState(
    val boards: List<BoardCard> = emptyList(),
)
