package com.sergiodev.bingo.ui.boards.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onCreateBoard) { Text("Nuevo cartón") }
            Button(onClick = onStartGame) { Text("Jugar") }
        }

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
