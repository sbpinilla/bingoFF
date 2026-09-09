# Apply Progress: theme-switcher

**Mode**: Standard (no strict TDD flag; standard mode workflow followed).
**Delivery**: Single commit to `master`, `size:exception` accepted by user (Medium 400-line budget risk per tasks forecast).

## Completed Tasks (24/24)

### Phase 1: Dependencies
- [x] 1.1 Added `datastore = "1.2.1"` version and `androidx-datastore-preferences` library entry to `gradle/libs.versions.toml`.
- [x] 1.2 Added `androidx-compose-material-icons-extended` library entry (no version, BOM-managed, matching `androidx-compose-material-icons-core` pattern) to `gradle/libs.versions.toml`.
- [x] 1.3 Added both `implementation(libs.androidx.compose.material.icons.extended)` and `implementation(libs.androidx.datastore.preferences)` to `app/build.gradle.kts`.

### Phase 2: Domain & Data Layer
- [x] 2.1 Created `domain/repository/ThemeRepository.kt` — `enum class ThemeMode { LIGHT, DARK, SYSTEM }` + `ThemeRepository` interface.
- [x] 2.2 Created `data/repository/DataStoreThemeRepository.kt` — `stringPreferencesKey("theme_mode")`, `runCatching { ThemeMode.valueOf(name) }.getOrDefault(SYSTEM)` fallback.

### Phase 3: Dependency Injection
- [x] 3.1 Created `di/PreferencesModule.kt` — `Context.themeDataStore` delegate (`preferencesDataStore(name = "theme_prefs")`), `@Provides @Singleton` for `DataStore<Preferences>` (object + @Provides, matches `DatabaseModule` convention).
- [x] 3.2 Modified `di/RepositoryModule.kt` — added `@Binds abstract fun bindThemeRepository(impl: DataStoreThemeRepository): ThemeRepository`.

### Phase 4: Theme Selection Screen (UI)
- [x] 4.1 Created `ui/boards/theme/ThemeUiState.kt`.
- [x] 4.2 Created `ui/boards/theme/ThemeViewModel.kt` — `uiState` via `stateIn(WhileSubscribed(5_000))` (matches `BoardListViewModel` convention), `onModeSelected` launches `setThemeMode` in `viewModelScope`.
- [x] 4.3 Created `ui/boards/theme/ThemeScreen.kt` — own `TopAppBar` + back nav mirroring `SettingsScreen`'s exact chrome (`WindowInsets(0,0,0,0)`, same `ArrowBack` icon/content description), 3 `ListItem`+`RadioButton` rows ("Claro"/"Oscuro"/"Sistema"), pre-selects active mode, persists immediately on tap.

### Phase 5: Settings Screen Integration
- [x] 5.1 Modified `ui/boards/settings/SettingsScreen.kt` — new "Tema" `ListItem` (`Icons.Default.Palette`) as FIRST item before "Exportar"/"Importar"; added `onNavigateToTheme` parameter following the exact `onNavigateToImport` pattern.
- [x] 5.2 Wired `onNavigateToTheme` at the actual call site — `BingoNavHost` (this project has no `SettingsViewModel` navigation passthrough; navigation callbacks are supplied directly by the composable caller, same as `onNavigateToImport`).

### Phase 6: Navigation
- [x] 6.1 Modified `ui/navigation/BingoRoute.kt` — added `const val THEME = "theme"`.
- [x] 6.2 Modified `ui/navigation/BingoNavHost.kt` — registered `composable(BingoRoute.THEME) { ThemeScreen(...) }` with back nav to Configuración, wired `onNavigateToTheme` into `SettingsScreen`.

### Phase 7: Activity Wiring
- [x] 7.1 Created `MainActivityViewModel.kt` — `@HiltViewModel`, `themeMode: StateFlow<ThemeMode>` passthrough via `stateIn`.
- [x] 7.2 Modified `MainActivity.kt` — `hiltViewModel()` inside `setContent{}`, resolves `darkTheme: Boolean` (LIGHT→false, DARK→true, SYSTEM→`isSystemInDarkTheme()` evaluated live), passes explicit value into unchanged `BingoFFTheme(darkTheme = ...)`. **`Theme.kt` untouched — confirmed zero `git diff`.**

### Phase 8: Tests
- [x] 8.1–8.3 `DataStoreThemeRepositoryTest.kt` — 3 tests: defaults to SYSTEM on empty store, round-trips via `setThemeMode`, falls back to SYSTEM on unrecognized stored string. Uses `PreferenceDataStoreFactory.create` over JUnit `TemporaryFolder`, pure JVM.
- [x] 8.4 `ThemeViewModelTest.kt` — 2 tests: `uiState` mirrors fake repository flow, `onModeSelected` delegates to `setThemeMode`.
- [x] 8.5 `MainActivityViewModelTest.kt` — 3 tests: `themeMode` mirrors repository flow for LIGHT/DARK/SYSTEM.

### Phase 9: Verification
- [x] 9.1 `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL, 128 tests total (120 baseline + 8 new), 0 failures.
- [x] 9.2 `./gradlew build` — BUILD SUCCESSFUL in 2m28s, lint + assembleDebug + assembleRelease all pass with the 2 new dependencies.
- [x] 9.3 `git diff -- app/src/main/java/com/sergiodev/bingo/ui/theme/Theme.kt` — zero lines. Hard constraint satisfied.
- [x] 9.4 Manual/device smoke test — expected-deferred, no emulator in this environment (consistent with all 12 prior archived changes).

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command and exact result | `./gradlew testDebugUnitTest --tests "*Theme*"` implied by full suite run; full `testDebugUnitTest` run: BUILD SUCCESSFUL, 128/128 tests pass (8 new: `DataStoreThemeRepositoryTest` x3, `ThemeViewModelTest` x2, `MainActivityViewModelTest` x3) |
| Runtime harness command/scenario and exact result | N/A — no emulator/device in this environment; `./gradlew build` (full compile + lint + assembleDebug/Release) is the closest available integration proof and passed |
| Rollback boundary | Single commit; revert restores `MainActivity.kt` to parameterless `BingoFFTheme { ... }`, deletes `ui/boards/theme/`, `ThemeRepository`/`DataStoreThemeRepository`, `PreferencesModule.kt`, the `RepositoryModule` binding, `MainActivityViewModel.kt`, the `SettingsScreen` "Tema" item, the `THEME` route, and the 2 new dependency lines. `Theme.kt` was never touched. |

## Actual Changed-Line Count (for size:exception record)

- New files: 394 lines (`MainActivityViewModel.kt` 25, `DataStoreThemeRepository.kt` 31, `PreferencesModule.kt` 24, `ThemeRepository.kt` 13, `ThemeScreen.kt` 97, `ThemeUiState.kt` 5, `ThemeViewModel.kt` 33, `MainActivityViewModelTest.kt` 58, `DataStoreThemeRepositoryTest.kt` 55, `ThemeViewModelTest.kt` 53)
- Modified files diff: 39 insertions + 1 deletion = 40 lines (`app/build.gradle.kts`, `MainActivity.kt`, `RepositoryModule.kt`, `SettingsScreen.kt`, `BingoNavHost.kt`, `BingoRoute.kt`, `gradle/libs.versions.toml`)
- **Total authored changed lines: ~434** — above the 400-line budget, consistent with the tasks-phase forecast (Medium risk, ~330–380 estimate). User explicitly accepted `size:exception` before this apply run.

## Status

24/24 tasks complete. Ready for verify.
