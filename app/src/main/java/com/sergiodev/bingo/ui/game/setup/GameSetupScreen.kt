package com.sergiodev.bingo.ui.game.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiodev.bingo.R
import com.sergiodev.bingo.domain.model.GameMode
import com.sergiodev.bingo.ui.theme.OnSuccess
import com.sergiodev.bingo.ui.theme.Success

@Composable
fun GameSetupScreen(
    onStartGame: (GameMode) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    GameSetupContent(
        state = state,
        onModeSelected = viewModel::onModeSelected,
        onStartGame = { state.selectedMode?.let(onStartGame) },
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupContent(
    state: GameSetupUiState,
    onModeSelected: (GameMode) -> Unit,
    onStartGame: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.game_setup_title)) },
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
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!state.hasBoards) {
                Text(stringResource(R.string.game_setup_no_boards_message))
            }
            Text(stringResource(R.string.game_setup_select_mode_message))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GameModeChip(
                    mode = GameMode.COLUMNA,
                    selectedMode = state.selectedMode,
                    onModeSelected = onModeSelected,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(GameMode.O, GameMode.L, GameMode.I).forEach { mode ->
                        GameModeChip(
                            mode = mode,
                            selectedMode = state.selectedMode,
                            onModeSelected = onModeSelected,
                            modifier = Modifier.weight(1f).height(56.dp),
                        )
                    }
                }
                GameModeChip(
                    mode = GameMode.CARTON_COMPLETO,
                    selectedMode = state.selectedMode,
                    onModeSelected = onModeSelected,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                )
            }
            Button(
                onClick = onStartGame,
                enabled = state.canStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.game_setup_start_button))
            }
        }
    }
}

@Composable
private fun GameModeChip(
    mode: GameMode,
    selectedMode: GameMode?,
    onModeSelected: (GameMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = selectedMode == mode
    FilterChip(
        selected = selected,
        onClick = { onModeSelected(mode) },
        label = { Text(mode.label(), maxLines = 1) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null) }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Success,
            selectedLabelColor = OnSuccess,
            selectedLeadingIconColor = OnSuccess,
        ),
        modifier = modifier,
    )
}

@Composable
private fun GameMode.label(): String = when (this) {
    GameMode.COLUMNA -> stringResource(R.string.game_setup_mode_columna)
    GameMode.O -> stringResource(R.string.game_setup_mode_o)
    GameMode.L -> stringResource(R.string.game_setup_mode_l)
    GameMode.I -> stringResource(R.string.game_setup_mode_i)
    GameMode.CARTON_COMPLETO -> stringResource(R.string.game_setup_mode_carton_completo)
}
