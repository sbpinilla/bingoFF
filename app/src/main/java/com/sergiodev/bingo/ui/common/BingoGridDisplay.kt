package com.sergiodev.bingo.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sergiodev.bingo.domain.model.BingoLetter
import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.domain.model.GridPosition

private val BingoGridBorderWidth = 1.dp

/**
 * Read-only, full-width, bordered 5x5 grid rendering of [board], with a BINGO header row
 * above it. The FREE cell (N column, row 3) is a normal bordered cell showing "FREE".
 */
@Composable
fun BingoGridDisplay(board: BoardCard, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            BingoLetter.entries.forEach { letter ->
                Text(
                    letter.name,
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Column(
            Modifier.fillMaxWidth()
                .border(BingoGridBorderWidth, MaterialTheme.colorScheme.outline),
        ) {
            (1..5).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    BingoLetter.entries.forEachIndexed { index, letter ->
                        val number = board.numberAt(GridPosition(letter, row))
                        Box(
                            Modifier.weight(1f).aspectRatio(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = number?.toString() ?: "FREE",
                                textAlign = TextAlign.Center,
                                style = if (number == null) {
                                    MaterialTheme.typography.labelSmall
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                },
                            )
                        }
                        if (index < BingoLetter.entries.lastIndex) {
                            VerticalDivider(
                                Modifier.fillMaxHeight(),
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
                if (row < 5) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
