package com.sergiodev.bingo.ui.boards.importexport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiodev.bingo.data.json.BoardJsonCodec
import com.sergiodev.bingo.domain.repository.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportBoardsViewModel @Inject constructor(
    private val repository: BoardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportBoardsUiState())
    val uiState: StateFlow<ImportBoardsUiState> = _uiState.asStateFlow()

    fun onJsonTextChange(text: String) {
        _uiState.update { it.copy(jsonText = text, jsonError = null) }
    }

    fun onSubmit() {
        val text = _uiState.value.jsonText

        if (text.isBlank()) {
            _uiState.update { it.copy(jsonText = "", jsonError = "El texto no puede estar vacío") }
            return
        }

        val boards = try {
            BoardJsonCodec.decode(text)
        } catch (e: Exception) {
            _uiState.update { it.copy(jsonText = "", jsonError = "El JSON no es válido") }
            return
        }

        viewModelScope.launch {
            val result = repository.importBoards(boards)
            _uiState.update {
                it.copy(
                    jsonText = "",
                    jsonError = null,
                    resultMessage = "${result.imported} importados, ${result.skipped} omitidos",
                )
            }
        }
    }

    fun onResultMessageShown() {
        _uiState.update { it.copy(resultMessage = null) }
    }
}
