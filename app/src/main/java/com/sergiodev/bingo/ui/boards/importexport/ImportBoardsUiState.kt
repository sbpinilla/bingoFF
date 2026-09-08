package com.sergiodev.bingo.ui.boards.importexport

/**
 * State for the paste-JSON import screen. [jsonError] non-null puts the
 * textarea in a red-error state (`isError = true`); [resultMessage] carries
 * a one-shot post-import summary ("N importados, M omitidos").
 */
data class ImportBoardsUiState(
    val jsonText: String = "",
    val jsonError: String? = null,
    val resultMessage: String? = null,
)
