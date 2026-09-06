package com.sergiodev.bingo.ui.game.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiodev.bingo.domain.model.GameMode

@Composable
fun GameSetupScreen(
    onStartGame: (GameMode) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    GameSetupContent(
        state = state,
        onModeSelected = viewModel::onModeSelected,
        onStartGame = { state.selectedMode?.let(onStartGame) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupContent(
    state: GameSetupUiState,
    onModeSelected: (GameMode) -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!state.hasBoards) {
            Text("Registra al menos un cartón para poder jugar")
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            state.availableModes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, state.availableModes.size),
                    icon = {},
                    label = { Text(mode.label(), maxLines = 1) },
                )
            }
        }
        Button(onClick = onStartGame, enabled = state.canStart) {
            Text("Jugar")
        }
    }
}

private fun GameMode.label(): String = when (this) {
    GameMode.COLUMNA -> "Columna"
    GameMode.O -> "O"
    GameMode.L -> "L"
    GameMode.I -> "I"
    GameMode.CARTON_COMPLETO -> "Completo"
}
