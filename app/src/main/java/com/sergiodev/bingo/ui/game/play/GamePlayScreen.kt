package com.sergiodev.bingo.ui.game.play

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiodev.bingo.R
import com.sergiodev.bingo.domain.game.PredictionCandidate
import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.ui.common.BingoNumberField

@Composable
fun GamePlayScreen(
    onEndGame: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GamePlayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    GamePlayContent(
        state = state,
        onNumberInputChanged = viewModel::onNumberInputChanged,
        onLetterSelected = viewModel::onLetterSelected,
        onSubmitCall = viewModel::onSubmitCall,
        onEndGame = onEndGame,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayContent(
    state: GamePlayUiState,
    onNumberInputChanged: (String) -> Unit,
    onLetterSelected: (BingoLetter) -> Unit,
    onSubmitCall: () -> Unit,
    onEndGame: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.game_play_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PossibleWinnersSection(state.possibleWinners)

            Text(stringResource(R.string.game_play_mode_status, state.mode.name, state.calledCount))

            state.winners.forEach { win ->
                Card {
                    Text(
                        text = stringResource(
                            R.string.game_play_bingo_announcement,
                            win.sequentialNumber,
                            win.identifier,
                        ),
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            BingoNumberField(
                value = state.numberInput,
                onValueChange = onNumberInputChanged,
                label = stringResource(R.string.game_play_number_label),
                isError = state.inputError != null,
                supportingText = state.inputError,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BingoLetter.entries.forEach { letter ->
                    FilterChip(
                        selected = state.selectedLetter == letter,
                        onClick = { onLetterSelected(letter) },
                        label = { Text(letter.name) },
                    )
                }
            }

            Button(onClick = onSubmitCall) {
                Text(stringResource(R.string.game_play_submit_call_button))
            }

            HorizontalDivider()

            BingoLetter.entries.forEach { letter ->
                val calls = state.callsByLetter[letter].orEmpty()
                Text(
                    text = stringResource(
                        R.string.game_play_letter_calls,
                        letter.name,
                        calls.joinToString(", "),
                    ),
                )
            }

            EndGameAction(onConfirm = onEndGame)
        }
    }
}

@Composable
private fun PossibleWinnersSection(possibleWinners: List<PredictionCandidate>) {
    if (possibleWinners.isEmpty()) return

    Text(stringResource(R.string.game_play_possible_winners_title), style = MaterialTheme.typography.titleMedium)
    possibleWinners.forEach { candidate ->
        val suffix = candidate.letter?.let { " (${it.name})" }.orEmpty()
        Text(stringResource(R.string.win_prediction_candidate, candidate.identifier, suffix))
    }
    HorizontalDivider()
}

@Composable
private fun EndGameAction(onConfirm: () -> Unit) {
    var show by rememberSaveable { mutableStateOf(false) }

    Button(
        onClick = { show = true },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
    ) {
        Text(stringResource(R.string.game_play_end_game_button))
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text(stringResource(R.string.game_play_end_game_dialog_title)) },
            text = { Text(stringResource(R.string.game_play_end_game_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                        show = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.game_play_end_game_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) {
                    Text(stringResource(R.string.game_play_end_game_cancel_button))
                }
            },
        )
    }
}
