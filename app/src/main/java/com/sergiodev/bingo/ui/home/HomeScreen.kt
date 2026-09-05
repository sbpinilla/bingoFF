package com.sergiodev.bingo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sergiodev.bingo.domain.model.SampleItem

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state = state, onAddItem = viewModel::onAddItem, modifier = modifier)
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onAddItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var label by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("New item") },
        )
        Button(
            onClick = {
                if (label.isNotBlank()) {
                    onAddItem(label)
                    label = ""
                }
            },
        ) {
            Text("Add item")
        }

        when {
            state.isLoading -> CircularProgressIndicator()
            state.errorMessage != null -> Text(text = state.errorMessage)
            state.items.isEmpty() -> Text(text = "No items yet")
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items) { item: SampleItem ->
                    Text(text = item.label)
                }
            }
        }
    }
}
