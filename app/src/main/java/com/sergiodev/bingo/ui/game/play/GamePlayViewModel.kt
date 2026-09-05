package com.sergiodev.bingo.ui.game.play

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergiodev.bingo.domain.game.AnnouncedWin
import com.sergiodev.bingo.domain.game.BingoWinChecker
import com.sergiodev.bingo.domain.game.GameSession
import com.sergiodev.bingo.domain.game.WinAnnouncement
import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.domain.model.GameMode
import com.sergiodev.bingo.domain.repository.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val ARG_MODE = "mode"
private const val KEY_CALLED_NUMBERS = "calledNumbers"

private data class PendingEntry(
    val numberInput: String = "",
    val selectedLetter: BingoLetter? = null,
    val overridden: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class GamePlayViewModel @Inject constructor(
    repository: BoardRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mode: GameMode =
        GameMode.valueOf(savedStateHandle.get<String>(ARG_MODE) ?: GameMode.COLUMNA.name)

    private val calledNumbers = MutableStateFlow(
        savedStateHandle.get<ArrayList<Int>>(KEY_CALLED_NUMBERS)?.toList() ?: emptyList(),
    )
    private val pending = MutableStateFlow(PendingEntry())

    val uiState: StateFlow<GamePlayUiState> = combine(
        repository.observeBoards(),
        calledNumbers,
        pending,
    ) { boards, called, pendingEntry ->
        val session = rebuildSession(mode, called, boards)
        GamePlayUiState(
            numberInput = pendingEntry.numberInput,
            selectedLetter = pendingEntry.selectedLetter,
            letterOverridden = pendingEntry.overridden,
            inputError = pendingEntry.error,
            callsByLetter = groupByLetter(called),
            winners = session.winners,
            mode = mode,
            calledCount = called.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GamePlayUiState(mode = mode),
    )

    fun onNumberInputChanged(value: String) {
        pending.update { current ->
            if (current.overridden) {
                current.copy(numberInput = value, error = null)
            } else {
                val derived = value.toIntOrNull()?.let(BingoLetter::fromNumber)
                current.copy(numberInput = value, selectedLetter = derived, error = null)
            }
        }
    }

    fun onLetterSelected(letter: BingoLetter) {
        pending.update { it.copy(selectedLetter = letter, overridden = true, error = null) }
    }

    /**
     * Confirms the pending call. Never trusts a manual override blindly:
     * if the selected letter contradicts the number's actual range, submit
     * is blocked with an inline error instead of marking a call under a
     * letter no board can match.
     */
    fun onSubmitCall() {
        val current = pending.value
        val number = current.numberInput.toIntOrNull()
        val derivedLetter = number?.let(BingoLetter::fromNumber)

        if (number == null || derivedLetter == null) {
            pending.update { it.copy(error = "Número inválido (1-75)") }
            return
        }

        val confirmedLetter = current.selectedLetter ?: derivedLetter
        if (confirmedLetter != derivedLetter) {
            pending.update { it.copy(error = "La letra seleccionada no corresponde a ese número") }
            return
        }

        val updated = calledNumbers.value + number
        calledNumbers.value = updated
        savedStateHandle[KEY_CALLED_NUMBERS] = ArrayList(updated)
        pending.value = PendingEntry()
    }

    private fun groupByLetter(called: List<Int>): Map<BingoLetter, List<Int>> {
        val map = linkedMapOf<BingoLetter, MutableList<Int>>()
        for (number in called) {
            val letter = BingoLetter.fromNumber(number) ?: continue
            map.getOrPut(letter) { mutableListOf() }.add(number)
        }
        return map
    }

    /**
     * Deterministically rebuilds announced/winners by replaying [calledNumbers]
     * in order — the only way this survives a process-death restart, since
     * only [mode] and the raw call history are persisted.
     */
    private fun rebuildSession(
        mode: GameMode,
        calledNumbers: List<Int>,
        boards: List<BoardCard>,
    ): GameSession {
        var announced = emptySet<AnnouncedWin>()
        val winners = mutableListOf<WinAnnouncement>()
        val calledSoFar = mutableSetOf<Int>()
        for (number in calledNumbers) {
            calledSoFar += number
            val newWins = BingoWinChecker.newWins(boards, calledSoFar, mode, announced)
            winners += newWins
            announced = announced + newWins.map { AnnouncedWin(it.boardId, it.patternId) }
        }
        return GameSession(mode = mode, calledNumbers = calledNumbers, announced = announced, winners = winners)
    }
}
