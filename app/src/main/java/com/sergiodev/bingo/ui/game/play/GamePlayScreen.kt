package com.sergiodev.bingo.ui.game.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiodev.bingo.domain.model.BingoLetter

@Composable
fun GamePlayScreen(
    modifier: Modifier = Modifier,
    viewModel: GamePlayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    GamePlayContent(
        state = state,
        onNumberInputChanged = viewModel::onNumberInputChanged,
        onLetterSelected = viewModel::onLetterSelected,
        onSubmitCall = viewModel::onSubmitCall,
        modifier = modifier,
    )
}

@Composable
fun GamePlayContent(
    state: GamePlayUiState,
    onNumberInputChanged: (String) -> Unit,
    onLetterSelected: (BingoLetter) -> Unit,
    onSubmitCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Modo: ${state.mode.name} · Llamadas: ${state.calledCount}")

        state.winners.forEach { win ->
            Card {
                Text(
                    text = "¡Bingo! #${win.sequentialNumber} ${win.identifier}",
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        OutlinedTextField(
            value = state.numberInput,
            onValueChange = onNumberInputChanged,
            label = { Text("Número") },
            isError = state.inputError != null,
            supportingText = { state.inputError?.let { Text(it) } },
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
    }
}
