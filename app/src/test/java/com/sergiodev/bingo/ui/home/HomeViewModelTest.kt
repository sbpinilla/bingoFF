package com.sergiodev.bingo.ui.home

import com.sergiodev.bingo.MainDispatcherRule
import com.sergiodev.bingo.domain.model.SampleItem
import com.sergiodev.bingo.domain.repository.SampleItemRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Local unit test for [HomeViewModel], which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeSampleItemRepository : SampleItemRepository {
        val items = MutableStateFlow<List<SampleItem>>(emptyList())
        var added: String? = null

        override fun observeItems(): Flow<List<SampleItem>> = items

        override suspend fun add(label: String) {
            added = label
            items.value = items.value + SampleItem(id = items.value.size.toLong(), label = label)
        }
    }

    @Test
    fun initialState_isLoadingWithNoItems() = runTest {
        val repository = FakeSampleItemRepository()
        val viewModel = HomeViewModel(repository)

        val state = viewModel.uiState.value

        assertTrue(state.isLoading)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun stateReflectsRepositoryEmission() = runTest {
        val repository = FakeSampleItemRepository()
        val viewModel = HomeViewModel(repository)

        // Keep the upstream flow alive so stateIn's WhileSubscribed sharing collects it.
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        repository.items.value = listOf(SampleItem(id = 1, label = "First"))
        runCurrent()

        val updated = viewModel.uiState.value
        assertEquals(false, updated.isLoading)
        assertEquals(listOf(SampleItem(id = 1, label = "First")), updated.items)
    }

    @Test
    fun onAddItem_delegatesToRepository() = runTest {
        val repository = FakeSampleItemRepository()
        val viewModel = HomeViewModel(repository)

        viewModel.onAddItem("New item")
        runCurrent()

        assertEquals("New item", repository.added)
    }
}
