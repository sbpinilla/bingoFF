package com.sergiodev.bingo.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sergiodev.bingo.R

internal val BingoFieldWidth = 64.dp
internal val BingoFieldHeight = 56.dp
internal val BingoFieldPadding = 2.dp

const val MAX_NUMBER_LENGTH = 2

/**
 * Keeps only ASCII digits and caps the result at [MAX_NUMBER_LENGTH] characters.
 *
 * `Char::isDigit` is deliberately avoided: it accepts non-ASCII digits (e.g. Arabic-Indic
 * digits) which would reach [String.toIntOrNull] through an unpredictable path. This filter
 * is a pure keystroke sanitizer, not a numeric normalizer — leading zeros are preserved.
 */
fun sanitizeNumberInput(raw: String): String = raw.filter { it in '0'..'9' }.take(MAX_NUMBER_LENGTH)

/**
 * A digit-only, length-capped number entry field shared by every board number field and the
 * game-play called-number field, guaranteeing identical sanitization behavior everywhere.
 */
@Composable
fun BingoNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction? = null,
    onImeAction: (() -> Unit)? = null,
) {
    val fieldModifier = focusRequester?.let { modifier.focusRequester(it) } ?: modifier
    val keyboardOptions = if (imeAction == null) {
        KeyboardOptions(keyboardType = KeyboardType.Number)
    } else {
        KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction)
    }

    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(sanitizeNumberInput(it)) },
        modifier = fieldModifier,
        enabled = enabled,
        label = label?.let { { Text(it) } },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = if (onImeAction != null) {
            KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() })
        } else {
            KeyboardActions()
        },
    )
}

/**
 * A non-editable, visually marked FREE cell matching [BingoNumberField]'s footprint so it
 * never visually drifts from a real entry field.
 */
@Composable
fun BingoFreeCell(modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.width(BingoFieldWidth).height(BingoFieldHeight)) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.common_free_cell), style = MaterialTheme.typography.labelMedium)
        }
    }
}
