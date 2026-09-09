package com.sergiodev.bingo.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sergiodev.bingo.domain.repository.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreThemeRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun createDataStore(fileName: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = testScope) { tempFolder.newFile(fileName) }

    @Test
    fun observeThemeMode_defaultsToSystem_onEmptyStore() = testScope.runTest {
        val repository = DataStoreThemeRepository(createDataStore("empty.preferences_pb"))

        assertEquals(ThemeMode.SYSTEM, repository.observeThemeMode().first())
    }

    @Test
    fun setThemeMode_thenObserveThemeMode_roundTripsNewValue() = testScope.runTest {
        val repository = DataStoreThemeRepository(createDataStore("roundtrip.preferences_pb"))

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.observeThemeMode().first())
    }

    @Test
    fun observeThemeMode_fallsBackToSystem_onUnrecognizedStoredValue() = testScope.runTest {
        val dataStore = createDataStore("corrupt.preferences_pb")
        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "NOT_A_VALID_MODE" }
        val repository = DataStoreThemeRepository(dataStore)

        assertEquals(ThemeMode.SYSTEM, repository.observeThemeMode().first())
    }
}
