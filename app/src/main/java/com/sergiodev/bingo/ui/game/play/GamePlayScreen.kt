package com.sergiodev.bingo.ui.game.play

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiodev.bingo.R
import com.sergiodev.bingo.domain.game.PredictionCandidate
import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.domain.model.GameMode
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
        onLetterDismissToggled = viewModel::onLetterDismissToggled,
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
    onLetterDismissToggled: (BingoLetter) -> Unit,
    onEndGame: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showEndGameDialog by rememberSaveable { mutableStateOf(false) }

    // The system back gesture (including its edge swipe) is disabled here: the column
    // dismiss swipe in LetterCallsRow shares the same edge and was sometimes mistaken for
    // a back gesture, exiting the screen mid-game. Use the TopAppBar's back icon instead.
    BackHandler(enabled = true) {}

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
                actions = {
                    IconButton(onClick = { showEndGameDialog = true }) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = stringResource(R.string.game_play_end_game_icon_description),
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
                supportingText = state.inputError?.toMessage(),
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

            LetterCallsGrid(
                state = state,
                onLetterDismissToggled = onLetterDismissToggled,
            )

            EndGameAction(
                show = showEndGameDialog,
                onDismiss = { showEndGameDialog = false },
                onConfirm = {
                    onEndGame()
                    showEndGameDialog = false
                },
            )
        }
    }
}

/**
 * Bordered/divided grid of all 5 [BingoLetter] rows, mirroring [com.sergiodev.bingo.ui.common.BingoGridDisplay]'s
 * Material3 `colorScheme.outline` convention. Renders for every [GameMode]; the swipe-to-dismiss
 * affordance inside [LetterCallsRow] only activates for [GameMode.COLUMNA].
 */
@Composable
private fun LetterCallsGrid(
    state: GamePlayUiState,
    onLetterDismissToggled: (BingoLetter) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        BingoLetter.entries.forEachIndexed { index, letter ->
            LetterCallsRow(
                letter = letter,
                calls = state.callsByLetter[letter].orEmpty(),
                dismissible = state.mode == GameMode.COLUMNA,
                dismissed = letter in state.dismissedLetters,
                onDismissToggled = { onLetterDismissToggled(letter) },
            )
            if (index < BingoLetter.entries.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

/**
 * One bordered row of [LetterCallsGrid]. Only wrapped in [SwipeToDismissBox] when [dismissible]
 * and not already [dismissed]; a dismissed row renders as a plain, dimmed, struck-through row
 * with a reopen [IconButton] instead — [SwipeToDismissBox] has no built-in "stay dismissed while
 * remaining in the list" semantics, so this avoids fighting the component's intended use.
 * Swiping never dismisses directly — it opens a confirmation dialog first; reopening a closed
 * row is not destructive to the tracking state, so it stays a plain tap with no confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
private fun LetterCallsRow(
    letter: BingoLetter,
    calls: List<Int>,
    dismissible: Boolean,
    dismissed: Boolean,
    onDismissToggled: () -> Unit,
) {
    var showDismissConfirm by remember { mutableStateOf(false) }

    val rowContent: @Composable () -> Unit = {
        Text(
            text = stringResource(
                R.string.game_play_letter_calls,
                letter.name,
                calls.joinToString(", "),
            ),
            modifier = Modifier.padding(8.dp),
        )
    }

    when {
        dismissible && dismissed -> {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.game_play_letter_calls,
                            letter.name,
                            calls.joinToString(", "),
                        ),
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                Text(
                    stringResource(R.string.game_play_column_closed_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = onDismissToggled) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = stringResource(R.string.game_play_column_reopen_description),
                    )
                }
            }
        }

        dismissible -> {
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        showDismissConfirm = true
                    }
                    false
                },
            )
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                enableDismissFromEndToStart = true,
                backgroundContent = {
                    Row(
                        Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            stringResource(R.string.game_play_column_mark_won),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            ) {
                Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                    rowContent()
                }
            }
        }

        else -> rowContent()
    }

    if (showDismissConfirm) {
        AlertDialog(
            onDismissRequest = { showDismissConfirm = false },
            title = { Text(stringResource(R.string.game_play_column_dismiss_dialog_title, letter.name)) },
            text = { Text(stringResource(R.string.game_play_column_dismiss_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissToggled()
                        showDismissConfirm = false
                    },
                ) {
                    Text(stringResource(R.string.game_play_column_dismiss_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDismissConfirm = false }) {
                    Text(stringResource(R.string.game_play_column_dismiss_cancel_button))
                }
            },
        )
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
private fun EndGameAction(show: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.game_play_end_game_dialog_title)) },
        text = { Text(stringResource(R.string.game_play_end_game_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.game_play_end_game_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.game_play_end_game_cancel_button))
            }
        },
    )
}

@Composable
private fun GamePlayInputErrorReason.toMessage(): String = when (this) {
    GamePlayInputErrorReason.InvalidNumber -> stringResource(R.string.game_play_error_invalid_number)
    GamePlayInputErrorReason.LetterMismatch -> stringResource(R.string.game_play_error_letter_mismatch)
    is GamePlayInputErrorReason.DuplicateCall ->
        stringResource(R.string.game_play_error_duplicate_call, number)
}
