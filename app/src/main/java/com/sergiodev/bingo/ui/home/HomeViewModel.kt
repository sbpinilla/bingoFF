package com.sergiodev.bingo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiodev.bingo.domain.repository.SampleItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SampleItemRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.observeItems()
        .map { items -> HomeUiState(isLoading = false, items = items, errorMessage = null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(isLoading = true, items = emptyList(), errorMessage = null),
        )

    fun onAddItem(label: String) {
        viewModelScope.launch {
            repository.add(label)
        }
    }
}
