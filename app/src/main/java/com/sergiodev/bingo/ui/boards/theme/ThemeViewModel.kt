package com.sergiodev.bingo.ui.boards.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiodev.bingo.domain.repository.ThemeMode
import com.sergiodev.bingo.domain.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repository: ThemeRepository,
) : ViewModel() {

    val uiState: StateFlow<ThemeUiState> = repository.observeThemeMode()
        .map { mode -> ThemeUiState(selectedMode = mode) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeUiState(),
        )

    fun onModeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }
}
