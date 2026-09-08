package com.sergiodev.bingo.ui.boards.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiodev.bingo.data.json.BoardJsonCodec
import com.sergiodev.bingo.domain.repository.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: BoardRepository,
) : ViewModel() {

    /** Snapshots the current boards, encodes them as JSON, and hands the result to [onReady]. */
    fun exportJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val boards = repository.observeBoards().first()
            onReady(BoardJsonCodec.encode(boards))
        }
    }
}
