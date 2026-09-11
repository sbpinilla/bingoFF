package com.sergiodev.bingo.ui.boards.importexport

/**
 * State for the paste-JSON import screen. [jsonError] non-null puts the
 * textarea in a red-error state (`isError = true`); [resultSummary] carries
 * a one-shot post-import summary (imported/skipped counts).
 */
data class ImportBoardsUiState(
    val jsonText: String = "",
    val jsonError: ImportBoardsErrorReason? = null,
    val resultSummary: ImportResultSummary? = null,
)

/**
 * Reasons `ImportBoardsViewModel.onSubmit()` can block importing with.
 * The Composable layer resolves each case to display text via `stringResource()`.
 */
sealed interface ImportBoardsErrorReason {
    data object BlankInput : ImportBoardsErrorReason
    data object InvalidJson : ImportBoardsErrorReason
}

/**
 * One-shot post-import summary carrying the counts needed by the
 * 2-placeholder `import_result_message` format string.
 */
data class ImportResultSummary(val imported: Int, val skipped: Int)
