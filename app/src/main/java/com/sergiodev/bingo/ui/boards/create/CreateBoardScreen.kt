package com.sergiodev.bingo.ui.boards.create

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.ui.common.BingoFieldPadding
import com.sergiodev.bingo.ui.common.BingoFieldWidth
import com.sergiodev.bingo.ui.common.BingoFreeCell
import com.sergiodev.bingo.ui.common.BingoNumberField
import com.sergiodev.bingo.ui.common.MAX_NUMBER_LENGTH

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

/**
 * Maps a (letter, column-index) grid position to its flat position in the 24-entry
 * `FocusRequester` list, in reading order B(0-4) -> I(5-9) -> N(10-13) -> G(14-18) -> O(19-23).
 * No skip logic is needed for the FREE cell: it is never part of [numbers], so N's value list
 * already only holds its 4 real slots.
 */
internal fun flatFieldIndex(
    numbers: Map<BingoLetter, List<String>>,
    letter: BingoLetter,
    index: Int,
): Int = BingoLetter.entries.take(letter.ordinal).sumOf { numbers.getValue(it).size } + index

@Composable
fun CreateBoardContent(
    state: CreateBoardUiState,
    onIdentifierChange: (String) -> Unit,
    onNumberChange: (BingoLetter, Int, String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequesters = remember { List(24) { FocusRequester() } }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier.fillMaxSize().imePadding().padding(16.dp).verticalScroll(rememberScrollState())
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = state.identifier,
            onValueChange = onIdentifierChange,
            label = { Text("Identificador") },
            isError = state.identifierError != null,
            supportingText = { state.identifierError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        BingoLetter.entries.forEach { letter ->
            Text(text = letter.name)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val values = state.numbers.getValue(letter)
                val errors = state.fieldErrors[letter]
                values.forEachIndexed { index, value ->
                    if (letter == BingoLetter.N && index == 2) {
                        BingoFreeCell(modifier = Modifier.padding(BingoFieldPadding))
                    }
                    val flatIndex = flatFieldIndex(state.numbers, letter, index)
                    val isLast = flatIndex == 23
                    BingoNumberField(
                        value = value,
                        onValueChange = {
                            onNumberChange(letter, index, it)
                            if (it.length == MAX_NUMBER_LENGTH && !isLast) {
                                focusRequesters.getOrNull(flatIndex + 1)?.requestFocus()
                            }
                        },
                        isError = errors?.getOrNull(index) != null,
                        modifier = Modifier.width(BingoFieldWidth).padding(BingoFieldPadding),
                        focusRequester = focusRequesters[flatIndex],
                        imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
                        onImeAction = {
                            if (isLast) {
                                focusManager.clearFocus()
                            } else {
                                focusRequesters.getOrNull(flatIndex + 1)?.requestFocus()
                            }
                        },
                    )
                }
            }
        }
        Button(onClick = onSubmit) {
            Text("Guardar cartón")
        }
    }
}
