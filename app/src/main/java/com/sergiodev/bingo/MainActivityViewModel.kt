package com.sergiodev.bingo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiodev.bingo.domain.repository.ThemeMode
import com.sergiodev.bingo.domain.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Minimal Activity-root ViewModel — exposes only the persisted [ThemeMode]. */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    repository: ThemeRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = repository.observeThemeMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM,
        )
}
