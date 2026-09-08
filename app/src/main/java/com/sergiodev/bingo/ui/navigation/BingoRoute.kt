package com.sergiodev.bingo.ui.navigation

/**
 * String-based navigation routes for the app's 4-destination graph.
 * Plain strings (not type-safe `@Serializable` routes) to avoid a second
 * Kotlin plugin under AGP built-in Kotlin — see design "routes decision".
 */
object BingoRoute {
    const val BOARD_LIST = "boardList"
    const val CREATE_BOARD = "createBoard"
    const val GAME_SETUP = "gameSetup"
    const val GAME_PLAY_ARG = "mode"
    const val GAME_PLAY_PATTERN = "gamePlay/{$GAME_PLAY_ARG}"
    const val SETTINGS = "settings"
    const val IMPORT_BOARDS = "importBoards"

    fun gamePlay(mode: String) = "gamePlay/$mode"
}
