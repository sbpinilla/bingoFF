package com.sergiodev.bingo.ui.boards.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.domain.model.GridPosition

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

@Composable
fun BoardListContent(
    state: BoardListUiState,
    onCreateBoard: () -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                NavigationBarItem(
                    selected = false,
                    onClick = onCreateBoard,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("Agregar") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Configuración") },
                )
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Button(
                onClick = onStartGame,
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
            ) { Text("Jugar") }

            // LazyColumn supports an arbitrary number of boards — no hardcoded cap.
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
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
        (1..5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BingoLetter.entries.forEach { letter ->
                    val number = board.numberAt(GridPosition(letter, row))
                    Text(text = number?.toString() ?: "FREE")
                }
            }
        }
    }
}
