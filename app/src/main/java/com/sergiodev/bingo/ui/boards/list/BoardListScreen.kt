package com.sergiodev.bingo.ui.boards.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiodev.bingo.R
import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.ui.common.BingoGridDisplay
import com.sergiodev.bingo.ui.common.EmptyState

@Composable
fun BoardListScreen(
    onCreateBoard: () -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BoardListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BoardListContent(
        state = state,
        onCreateBoard = onCreateBoard,
        onStartGame = onStartGame,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardListContent(
    state: BoardListUiState,
    onCreateBoard: () -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                windowInsets = WindowInsets(0, 0, 0, 0),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            Surface(tonalElevation = NavigationBarDefaults.Elevation) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                        Text("Configuración", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onStartGame,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                        Text("Jugar", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = onCreateBoard) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                        Text("Agregar", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
    ) { innerPadding ->
        if (state.boards.isEmpty()) {
            EmptyState(
                message = "No hay cartones agregados",
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        } else {
            // LazyColumn supports an arbitrary number of boards — no hardcoded cap.
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.boards) { board ->
                    BoardCardItem(board)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun BoardCardItem(board: BoardCard) {
    Column {
        Text(
            text = "#${board.id} · ${board.identifier}",
            fontWeight = FontWeight.Bold,
        )
        BingoGridDisplay(board = board)
    }
}
