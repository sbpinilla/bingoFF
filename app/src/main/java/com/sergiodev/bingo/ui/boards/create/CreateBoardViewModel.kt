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
            val newNumbers = updatedNumbers.toMap()
            state.copy(
                numbers = newNumbers,
                fieldErrors = if (state.fieldErrors.isEmpty()) {
                    emptyMap()
                } else {
                    computeFieldErrors(newNumbers, flagBlank = false)
                },
            )
        }
    }

    fun onSubmit() {
        val state = _uiState.value

        if (state.identifier.isBlank()) {
            _uiState.update { it.copy(identifierError = CreateBoardErrorReason.BlankIdentifier) }
            return
        }

        val fieldErrors = computeFieldErrors(state.numbers, flagBlank = true)
        val hasFieldError = fieldErrors.values.any { errors -> errors.any { it != null } }

        if (hasFieldError) {
            _uiState.update { it.copy(fieldErrors = fieldErrors) }
            return
        }

        val orderedNumbers = BingoLetter.entries.flatMap { letter ->
            state.numbers.getValue(letter).map(String::toInt)
        }

        viewModelScope.launch {
            repository.addBoard(state.identifier, orderedNumbers)
                .onSuccess {
                    _uiState.update { it.copy(submitSuccess = true, fieldErrors = emptyMap()) }
                }
                .onFailure {
                    _uiState.update { it.copy(identifierError = CreateBoardErrorReason.DuplicateIdentifier) }
                }
        }
    }
}

/**
 * Pure, testable computation of per-field validity markers.
 *
 * Precedence mirrors the original inline validation: range is checked before
 * duplicates, and duplicate detection only considers values that are already
 * in-range (an out-of-range value never contributes to `duplicates`). The
 * returned strings are internal markers only — the UI reads nullness via
 * `isError`, never the string content.
 */
internal fun computeFieldErrors(
    numbers: Map<BingoLetter, List<String>>,
    flagBlank: Boolean,
): Map<BingoLetter, List<String?>> {
    val valid = BingoLetter.entries.associateWith { letter ->
        numbers.getValue(letter).map { raw -> raw.toIntOrNull()?.takeIf { it in letter.range } }
    }
    val seen = mutableSetOf<Int>()
    val duplicates = mutableSetOf<Int>()
    valid.values.flatten().filterNotNull().forEach { if (!seen.add(it)) duplicates += it }

    return BingoLetter.entries.associateWith { letter ->
        numbers.getValue(letter).map { raw ->
            val n = raw.toIntOrNull()
            when {
                n == null -> if (raw.isBlank() && !flagBlank) null else "invalid"
                n !in letter.range -> "out_of_range"
                n in duplicates -> "duplicate"
                else -> null
            }
        }
    }
}
