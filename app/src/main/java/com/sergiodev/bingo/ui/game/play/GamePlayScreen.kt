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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                title = { Text("Jugar") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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

            Text("Modo: ${state.mode.name} · Llamadas: ${state.calledCount}")

            state.winners.forEach { win ->
                Card {
                    Text(
                        text = "¡Bingo! #${win.sequentialNumber} ${win.identifier}",
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            BingoNumberField(
                value = state.numberInput,
                onValueChange = onNumberInputChanged,
                label = "Número",
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
                Text("Cantar número")
            }

            HorizontalDivider()

            BingoLetter.entries.forEach { letter ->
                val calls = state.callsByLetter[letter].orEmpty()
                Text(text = "${letter.name}: ${calls.joinToString(", ")}")
            }

            EndGameAction(onConfirm = onEndGame)
        }
    }
}

@Composable
private fun PossibleWinnersSection(possibleWinners: List<PredictionCandidate>) {
    if (possibleWinners.isEmpty()) return

    Text("Posibles ganadores", style = MaterialTheme.typography.titleMedium)
    possibleWinners.forEach { candidate ->
        Text("Cartón ${candidate.identifier} (${candidate.letter.name})")
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
        Text("Terminar juego")
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text("¿Terminar juego?") },
            text = { Text("Se perderán los números cantados y los bingos anunciados de esta partida.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                        show = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Terminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}
