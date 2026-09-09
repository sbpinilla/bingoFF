# Design: Manual Theme Switcher (Light/Dark/System)

Proposal: Engram `sdd/theme-switcher/proposal` / `openspec/changes/theme-switcher/proposal.md`.

## Technical Approach

Add a `ThemeRepository` (Flow-based, mirrors `BoardRepository`) backed by DataStore Preferences, storing a 3-state `ThemeMode` enum (`LIGHT`/`DARK`/`SYSTEM`, default `SYSTEM`). A new `ThemeScreen`+`ThemeViewModel` (mirrors `SettingsScreen`'s chrome) lets the user pick a mode via `ListItem`+`RadioButton`, persisting immediately. `Theme.kt` stays **completely unchanged** — `BingoFFTheme(darkTheme: Boolean = isSystemInDarkTheme(), ...)` keeps its exact signature and default. `MainActivity` gains a new minimal `MainActivityViewModel` that exposes `themeMode: StateFlow<ThemeMode>`; inside `setContent{}` it resolves `ThemeMode` → `Boolean` (`LIGHT`→false, `DARK`→true, `SYSTEM`→`isSystemInDarkTheme()`, evaluated live in the Compose tree) and passes the explicit value into `BingoFFTheme(darkTheme = ...)`. `SettingsScreen` gains one new "Tema" `ListItem` and a passthrough `onNavigateToTheme` callback, exactly like the existing `onNavigateToImport`.

## Architecture Decisions

### Decision: Activity-level state via a dedicated minimal `MainActivityViewModel`

| Option | Tradeoff | Decision |
|---|---|---|
| New `MainActivityViewModel` (`@HiltViewModel`, exposes `StateFlow<ThemeMode>` only) | One extra small file; matches the "one ViewModel per consumer" convention already used by every screen | **Chosen** |
| Reuse `ThemeViewModel` at Activity root via `hiltViewModel()` | Fewer files, but exposes an unused `onModeSelected` mutator to `MainActivity` and mixes screen-scoped concerns with app-root concerns | Rejected |
| `@AndroidEntryPoint` raw field injection of `ThemeRepository` + `collectAsStateWithLifecycle()` directly in `MainActivity` | Zero new files, but breaks the codebase's exclusive `hiltViewModel()` convention (every reactive state today flows through a ViewModel) | Rejected |

`MainActivityViewModel` lives in `MainActivity.kt`'s package and is fetched with `hiltViewModel()` inside `setContent{}` — `ComponentActivity` is already a `ViewModelStoreOwner`, so no `NavBackStackEntry` scoping is needed (unlike screen ViewModels resolved inside `composable{}`).

### Decision: `Theme.kt` untouched; `MainActivity` always passes an explicit `darkTheme`

**Choice**: Keep `BingoFFTheme`'s `darkTheme: Boolean = isSystemInDarkTheme()` default as-is. `MainActivity` now always supplies an explicit resolved value, making the default effectively dead code for that one call site but harmless — no other caller exists, and the signature stays backward-compatible for any future direct caller (e.g. a Preview) that omits the parameter.
**Alternatives considered**: Remove the default (`darkTheme: Boolean` required) — rejected, it's an unnecessary breaking API change to a file explicitly marked "already correct, untouched" in the proposal's scope.
**Rationale**: Smallest possible diff to `Theme.kt` (zero lines) while satisfying "app-wide, no restart" — `MainActivityViewModel.themeMode` recomposes `setContent{}`'s content on every DataStore emission.

### Decision: DataStore key stores the enum name as a `String`, not an `Int`/ordinal

**Choice**: `stringPreferencesKey("theme_mode")` storing `ThemeMode.name`; read side does `ThemeMode.valueOf(name)` wrapped in `runCatching`, falling back to `SYSTEM` on `null`, blank, or an unrecognized value (forward-compat if a future release adds/renames a mode).
**Alternatives considered**: `intPreferencesKey` storing `ordinal` — rejected, silently breaks if enum order ever changes; strings are self-describing and DataStore-idiomatic.
**Rationale**: Matches the confirmed "safe fallback to SYSTEM on missing/corrupt data" requirement with one `runCatching`, no custom `Preferences.Key` type needed.

## Data Flow

    ThemeScreen (RadioButton tap)
          │ onModeSelected(mode)
          ▼
    ThemeViewModel.onModeSelected ──► ThemeRepository.setThemeMode(mode)
                                            │ dataStore.edit { it[KEY] = mode.name }
                                            ▼
                                   DataStore Preferences file (theme_prefs)
                                            │ dataStore.data Flow emits
                                            ▼
                                ThemeRepository.observeThemeMode(): Flow<ThemeMode>
                                     │                              │
                                     ▼                              ▼
                          ThemeViewModel.uiState        MainActivityViewModel.themeMode
                          (ThemeScreen redraws                     │
                           selected RadioButton)                   ▼
                                                    MainActivity setContent{}: resolve darkTheme
                                                    (SYSTEM → isSystemInDarkTheme())
                                                             │
                                                             ▼
                                                   BingoFFTheme(darkTheme = ...)

## File Changes

| File | Action | Description |
|---|---|---|
| `domain/repository/ThemeRepository.kt` | Create | `ThemeMode` enum + `ThemeRepository` interface (`observeThemeMode()`, `suspend setThemeMode()`) |
| `data/repository/DataStoreThemeRepository.kt` | Create | DataStore-backed impl, `stringPreferencesKey`, `SYSTEM` fallback |
| `di/PreferencesModule.kt` | Create | `Context.themeDataStore` delegate + `@Provides` for `DataStore<Preferences>` |
| `di/RepositoryModule.kt` | Modify | Add `@Binds bindThemeRepository(impl: DataStoreThemeRepository): ThemeRepository` |
| `ui/boards/theme/ThemeUiState.kt` | Create | `data class ThemeUiState(val selectedMode: ThemeMode = ThemeMode.SYSTEM)` |
| `ui/boards/theme/ThemeViewModel.kt` | Create | Exposes `uiState`, `onModeSelected(mode)` |
| `ui/boards/theme/ThemeScreen.kt` | Create | `TopAppBar`+back nav (mirrors `SettingsScreen`), 3 `ListItem`+`RadioButton` rows |
| `MainActivityViewModel.kt` | Create | `themeMode: StateFlow<ThemeMode>`, passthrough of `observeThemeMode()` |
| `MainActivity.kt` | Modify | `hiltViewModel()` + resolve `darkTheme`, pass to `BingoFFTheme` |
| `ui/boards/settings/SettingsScreen.kt` | Modify | New "Tema" `ListItem` (`Icons.Default.Palette`) first, `onNavigateToTheme` param |
| `ui/navigation/BingoRoute.kt` | Modify | Add `const val THEME = "theme"` |
| `ui/navigation/BingoNavHost.kt` | Modify | Add `composable(BingoRoute.THEME)`, wire `SettingsScreen.onNavigateToTheme` |
| `gradle/libs.versions.toml` | Modify | Add `datastore` version + `androidx-datastore-preferences`, `androidx-compose-material-icons-extended` (no version, BOM-managed) |
| `app/build.gradle.kts` | Modify | Add both new `implementation(...)` lines |

## Interfaces / Contracts

```kotlin
// domain/repository/ThemeRepository.kt
enum class ThemeMode { LIGHT, DARK, SYSTEM }

interface ThemeRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
```

```kotlin
// data/repository/DataStoreThemeRepository.kt
class DataStoreThemeRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemeRepository {
    override fun observeThemeMode(): Flow<ThemeMode> =
        dataStore.data.map { prefs ->
            prefs[THEME_MODE_KEY]?.let { name ->
                runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | `observeThemeMode()` defaults to `SYSTEM` on empty store | `PreferenceDataStoreFactory.create` over a JUnit `TemporaryFolder` file, pure JVM (no Robolectric) |
| Unit | `setThemeMode` then `observeThemeMode` reflects new value | Same temp-file DataStore, round-trip assertion |
| Unit | Corrupt/unrecognized stored string falls back to `SYSTEM` | Pre-seed the file with an invalid string key value |
| Unit | `ThemeViewModel.uiState` mirrors repository flow; `onModeSelected` delegates to `setThemeMode` | Fake `ThemeRepository`, `runTest` |
| Unit | `MainActivityViewModel.themeMode` mirrors repository flow | Fake `ThemeRepository`, `runTest` |
| Manual (device) | Selecting each mode in `ThemeScreen` applies app-wide without restart; relaunch restores last choice; default (never touched) matches today's system-following behavior | Per standing convention — no Compose UI test infra exists |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary. The new `THEME` route is a plain in-app Compose navigation destination, same shape as existing routes.

## Migration / Rollout

No data migration required — DataStore creates its file lazily on first write; missing file reads as `SYSTEM`, identical to today's pre-change behavior. Rollback: revert `MainActivity.kt` to call `BingoFFTheme { ... }` with no arguments, delete the new `theme/` package, `ThemeRepository`/`DataStoreThemeRepository`, `PreferencesModule.kt`, the `RepositoryModule` binding, the `SettingsScreen` "Tema" item, the `THEME` route, and the two new dependencies. The orphaned DataStore file is inert, not destructive.

## Open Questions

None — all decisions are resolved by the confirmed product decisions and this design.
