package com.sergiodev.bingo.ui.boards.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergiodev.bingo.domain.repository.ThemeMode

@Composable
fun ThemeScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThemeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ThemeContent(
        state = state,
        onModeSelected = viewModel::onModeSelected,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeContent(
    state: ThemeUiState,
    onModeSelected: (ThemeMode) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Tema") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ListItem(
                headlineContent = { Text("Claro") },
                leadingContent = {
                    RadioButton(
                        selected = state.selectedMode == ThemeMode.LIGHT,
                        onClick = { onModeSelected(ThemeMode.LIGHT) },
                    )
                },
                modifier = Modifier.clickable { onModeSelected(ThemeMode.LIGHT) },
            )
            ListItem(
                headlineContent = { Text("Oscuro") },
                leadingContent = {
                    RadioButton(
                        selected = state.selectedMode == ThemeMode.DARK,
                        onClick = { onModeSelected(ThemeMode.DARK) },
                    )
                },
                modifier = Modifier.clickable { onModeSelected(ThemeMode.DARK) },
            )
            ListItem(
                headlineContent = { Text("Sistema") },
                leadingContent = {
                    RadioButton(
                        selected = state.selectedMode == ThemeMode.SYSTEM,
                        onClick = { onModeSelected(ThemeMode.SYSTEM) },
                    )
                },
                modifier = Modifier.clickable { onModeSelected(ThemeMode.SYSTEM) },
            )
        }
    }
}
