package com.sergiodev.bingo.ui.home

import com.sergiodev.bingo.domain.model.SampleItem

data class HomeUiState(
    val isLoading: Boolean = true,
    val items: List<SampleItem> = emptyList(),
    val errorMessage: String? = null,
)
