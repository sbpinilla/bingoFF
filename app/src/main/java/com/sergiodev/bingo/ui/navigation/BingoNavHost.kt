package com.sergiodev.bingo.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sergiodev.bingo.ui.boards.create.CreateBoardScreen
import com.sergiodev.bingo.ui.boards.list.BoardListScreen
import com.sergiodev.bingo.ui.game.play.GamePlayScreen
import com.sergiodev.bingo.ui.game.setup.GameSetupScreen

@Composable
fun BingoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = BingoRoute.BOARD_LIST,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(BingoRoute.BOARD_LIST) {
            BoardListScreen(
                onCreateBoard = { navController.navigate(BingoRoute.CREATE_BOARD) },
                onStartGame = { navController.navigate(BingoRoute.GAME_SETUP) },
            )
        }
        composable(BingoRoute.CREATE_BOARD) {
            CreateBoardScreen(onBoardCreated = { navController.popBackStack() })
        }
        composable(BingoRoute.GAME_SETUP) {
            GameSetupScreen(
                onStartGame = { mode -> navController.navigate(BingoRoute.gamePlay(mode.name)) },
            )
        }
        composable(
            route = BingoRoute.GAME_PLAY_PATTERN,
            arguments = listOf(navArgument(BingoRoute.GAME_PLAY_ARG) { type = NavType.StringType }),
        ) {
            GamePlayScreen()
        }
    }
}
