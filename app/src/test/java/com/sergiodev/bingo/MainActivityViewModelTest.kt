package com.sergiodev.bingo

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
class MainActivityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeThemeRepository(initial: ThemeMode) : ThemeRepository {
        val mode = MutableStateFlow(initial)
        override fun observeThemeMode(): Flow<ThemeMode> = mode
        override suspend fun setThemeMode(mode: ThemeMode) {
            this.mode.value = mode
        }
    }

    @Test
    fun themeMode_mirrorsRepositoryFlow_forLight() = runTest {
        val repository = FakeThemeRepository(ThemeMode.LIGHT)
        val viewModel = MainActivityViewModel(repository)
        backgroundScope.launch { viewModel.themeMode.collect {} }
        runCurrent()

        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
    }

    @Test
    fun themeMode_mirrorsRepositoryFlow_forDark() = runTest {
        val repository = FakeThemeRepository(ThemeMode.DARK)
        val viewModel = MainActivityViewModel(repository)
        backgroundScope.launch { viewModel.themeMode.collect {} }
        runCurrent()

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun themeMode_mirrorsRepositoryFlow_forSystem() = runTest {
        val repository = FakeThemeRepository(ThemeMode.SYSTEM)
        val viewModel = MainActivityViewModel(repository)
        backgroundScope.launch { viewModel.themeMode.collect {} }
        runCurrent()

        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
    }
}
