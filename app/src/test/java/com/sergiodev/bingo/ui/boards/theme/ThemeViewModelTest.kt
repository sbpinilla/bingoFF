package com.sergiodev.bingo.ui.boards.theme

import com.sergiodev.bingo.MainDispatcherRule
import com.sergiodev.bingo.domain.repository.ThemeMode
import com.sergiodev.bingo.domain.repository.ThemeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeThemeRepository : ThemeRepository {
        val mode = MutableStateFlow(ThemeMode.SYSTEM)
        override fun observeThemeMode(): Flow<ThemeMode> = mode
        override suspend fun setThemeMode(mode: ThemeMode) {
            this.mode.value = mode
        }
    }

    @Test
    fun uiState_mirrorsRepositoryFlow() = runTest {
        val repository = FakeThemeRepository()
        val viewModel = ThemeViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        repository.mode.value = ThemeMode.DARK
        runCurrent()

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.selectedMode)
    }

    @Test
    fun onModeSelected_delegatesToSetThemeMode() = runTest {
        val repository = FakeThemeRepository()
        val viewModel = ThemeViewModel(repository)

        viewModel.onModeSelected(ThemeMode.LIGHT)
        runCurrent()

        assertEquals(ThemeMode.LIGHT, repository.mode.value)
    }
}
