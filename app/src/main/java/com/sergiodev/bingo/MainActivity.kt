package com.sergiodev.bingo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.sergiodev.bingo.ui.navigation.BingoNavHost
import com.sergiodev.bingo.ui.theme.BingoFFTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BingoFFTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BingoNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
