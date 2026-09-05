package com.sergiodev.bingo.ui.boards.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.ui.common.BingoFieldPadding
import com.sergiodev.bingo.ui.common.BingoFieldWidth
import com.sergiodev.bingo.ui.common.BingoFreeCell
import com.sergiodev.bingo.ui.common.BingoNumberField

@Composable
fun CreateBoardScreen(
    onBoardCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateBoardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.submitSuccess) {
        if (state.submitSuccess) onBoardCreated()
    }

    CreateBoardContent(
        state = state,
        onIdentifierChange = viewModel::onIdentifierChange,
        onNumberChange = viewModel::onNumberChange,
        onSubmit = viewModel::onSubmit,
        modifier = modifier,
    )
}

@Composable
fun CreateBoardContent(
    state: CreateBoardUiState,
    onIdentifierChange: (String) -> Unit,
    onNumberChange: (BingoLetter, Int, String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.identifier,
                onValueChange = onIdentifierChange,
                label = { Text("Identificador") },
                isError = state.identifierError != null,
                supportingText = { state.identifierError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        items(BingoLetter.entries) { letter ->
            Text(text = letter.name)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val values = state.numbers.getValue(letter)
                val errors = state.fieldErrors[letter]
                values.forEachIndexed { index, value ->
                    if (letter == BingoLetter.N && index == 2) {
                        BingoFreeCell(modifier = Modifier.padding(BingoFieldPadding))
                    }
                    BingoNumberField(
                        value = value,
                        onValueChange = { onNumberChange(letter, index, it) },
                        isError = errors?.getOrNull(index) != null,
                        modifier = Modifier.width(BingoFieldWidth).padding(BingoFieldPadding),
                    )
                }
            }
        }
        item {
            Button(onClick = onSubmit) {
                Text("Guardar cartón")
            }
        }
    }
}
