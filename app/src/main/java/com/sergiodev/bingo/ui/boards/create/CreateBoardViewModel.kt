package com.sergiodev.bingo.ui.boards.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.domain.repository.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateBoardViewModel @Inject constructor(
    private val repository: BoardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateBoardUiState())
    val uiState: StateFlow<CreateBoardUiState> = _uiState.asStateFlow()

    fun onIdentifierChange(value: String) {
        _uiState.update { it.copy(identifier = value, identifierError = null) }
    }

    fun onNumberChange(letter: BingoLetter, index: Int, value: String) {
        _uiState.update { state ->
            val updatedNumbers = state.numbers.toMutableMap()
            val list = updatedNumbers.getValue(letter).toMutableList()
            list[index] = value
            updatedNumbers[letter] = list
            state.copy(numbers = updatedNumbers, fieldErrors = emptyMap())
        }
    }

    fun onSubmit() {
        val state = _uiState.value

        if (state.identifier.isBlank()) {
            _uiState.update { it.copy(identifierError = "El identificador no puede estar vacío") }
            return
        }

        val fieldErrors = mutableMapOf<BingoLetter, MutableList<String?>>()
        val parsedNumbers = mutableMapOf<BingoLetter, MutableList<Int?>>()
        var hasFieldError = false

        for (letter in BingoLetter.entries) {
            val values = state.numbers.getValue(letter)
            val errors = MutableList<String?>(values.size) { null }
            val parsed = MutableList<Int?>(values.size) { null }
            values.forEachIndexed { index, raw ->
                val number = raw.toIntOrNull()
                when {
                    number == null -> {
                        errors[index] = "Número inválido"
                        hasFieldError = true
                    }
                    number !in letter.range -> {
                        errors[index] = "Fuera de rango ${letter.range.first}-${letter.range.last}"
                        hasFieldError = true
                    }
                    else -> parsed[index] = number
                }
            }
            fieldErrors[letter] = errors
            parsedNumbers[letter] = parsed
        }

        // Duplicate-within-board detection across all successfully parsed numbers.
        val seen = mutableSetOf<Int>()
        val duplicates = mutableSetOf<Int>()
        parsedNumbers.values.flatten().filterNotNull().forEach { number ->
            if (!seen.add(number)) duplicates += number
        }
        if (duplicates.isNotEmpty()) {
            hasFieldError = true
            for (letter in BingoLetter.entries) {
                val parsed = parsedNumbers.getValue(letter)
                val errors = fieldErrors.getValue(letter)
                parsed.forEachIndexed { index, number ->
                    if (number != null && number in duplicates) {
                        errors[index] = "Número duplicado"
                    }
                }
            }
        }

        if (hasFieldError) {
            _uiState.update { it.copy(fieldErrors = fieldErrors) }
            return
        }

        val orderedNumbers = buildList {
            addAll(parsedNumbers.getValue(BingoLetter.B).filterNotNull())
            addAll(parsedNumbers.getValue(BingoLetter.I).filterNotNull())
            addAll(parsedNumbers.getValue(BingoLetter.N).filterNotNull())
            addAll(parsedNumbers.getValue(BingoLetter.G).filterNotNull())
            addAll(parsedNumbers.getValue(BingoLetter.O).filterNotNull())
        }

        viewModelScope.launch {
            repository.addBoard(state.identifier, orderedNumbers)
                .onSuccess {
                    _uiState.update { it.copy(submitSuccess = true, fieldErrors = emptyMap()) }
                }
                .onFailure {
                    _uiState.update { it.copy(identifierError = "Ese identificador ya existe") }
                }
        }
    }
}
