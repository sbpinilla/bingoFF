# Proposal: Manual Theme Switcher (Light/Dark/System)

## Intent

Dark mode already renders correctly today — `BingoFFTheme` defaults to `isSystemInDarkTheme()`, and both `LightColorScheme`/`DarkColorScheme` already exist. What's missing is a user-facing override: `MainActivity.kt` calls `BingoFFTheme` with zero arguments, so the app always follows the OS setting with no way to force Light or Dark regardless of system state. This change adds a persisted "Tema" setting so users control the theme explicitly — no dark-theme visual design work is needed, only a manual override on top of what already works.

## Scope

### In Scope
- 3-state `ThemeMode` enum (`LIGHT`/`DARK`/`SYSTEM`), default `SYSTEM` — preserves today's passive behavior for anyone who never opens the new setting.
- New `ThemeRepository`/`ThemeRepositoryImpl`, DataStore Preferences-backed (new dependency), mirroring `BoardRepository`/`RoomBoardRepository`'s `Flow`-based shape.
- New Hilt module providing the DataStore `Preferences` instance and binding `ThemeRepositoryImpl`.
- New `ThemeScreen` (own `TopAppBar` + back nav) with a `ListItem`+`RadioButton` list of the 3 options; selecting one persists immediately, no save button, matching this app's reactive convention.
- New "Tema" `ListItem` at the top of `SettingsScreen`'s list (order becomes Tema, Exportar, Importar), with its own icon.
- New `BingoRoute.THEME` constant + `BingoNavHost` wiring.
- `MainActivity.kt` collects the persisted `ThemeMode` reactively (first Activity-level Hilt state in this codebase) and resolves it to the `darkTheme: Boolean` passed into `BingoFFTheme`, applying app-wide without restart.
- Icon dependency: none of `Palette`/`DarkMode`/`Contrast` exist in the pinned `material-icons-core`-only artifact (its curated set is a fixed ~50 icons — Add, ArrowBack, Settings, Share, etc.). Unlike the prior `FileDownload`→`Share` substitution, no core icon conveys "theme," so this change also adds `material-icons-extended` (same AndroidX artifact family, version-aligned via `composeBom`) and uses `Icons.Default.Palette`.

### Out of Scope
- Dark/light `ColorScheme` definitions in `Theme.kt` — already correct, untouched.
- `Success`/`OnSuccess` hardcoded tokens in `Color.kt` — unaffected by dark mode, untouched.
- `BoardRepository`/`RoomBoardRepository`/`BoardDao` and all existing screens' behavior — unchanged.
- Compose UI test infrastructure — none exists project-wide; verification stays manual/device, per standing convention.

## Capabilities

### New Capabilities
- `theme-preference`: persisted 3-state theme setting (Light/Dark/System), its DataStore-backed repository, and the dedicated theme selection screen.

### Modified Capabilities
- `board-export-import`: "Configuración Screen" requirement's item count changes from exactly 2 (Exportar, Importar) to exactly 3 (Tema, Exportar, Importar), Tema first.
- `app-navigation`: adds a `THEME` route/destination alongside the existing Configuración/Importar routes.

## Approach

Follow the existing `BoardRepository` Flow-first pattern: `ThemeRepositoryImpl` exposes `observeThemeMode(): Flow<ThemeMode>` and `setThemeMode(mode: ThemeMode)` over a DataStore `Preferences` instance, DI-provided the same way `DatabaseModule`/`RepositoryModule` provide Room. `MainActivity` collects that flow via `collectAsState()`, resolves `SYSTEM` to `isSystemInDarkTheme()` and `LIGHT`/`DARK` to explicit booleans, and passes the result into `BingoFFTheme(darkTheme = ...)`. `ThemeScreen` mirrors `SettingsScreen`'s stateful/stateless split, with a new `ThemeViewModel` calling `setThemeMode` directly on selection.

## Affected Areas

| Area | Impact | Description |
|------|--------|--------------|
| `ui/theme/Theme.kt` | Unchanged | Already accepts `darkTheme` param |
| `MainActivity.kt` | Modified | Collects persisted `ThemeMode`, resolves `darkTheme` boolean |
| `ui/boards/settings/SettingsScreen.kt` | Modified | New "Tema" `ListItem` at top + `onNavigateToTheme` callback |
| New `ui/boards/theme/ThemeScreen.kt` + `ThemeViewModel.kt` | New | `RadioButton` list, immediate persistence |
| New `domain/repository/ThemeRepository.kt` + impl | New | DataStore-backed, mirrors `BoardRepository` |
| New `di/PreferencesModule.kt` (or existing module) | New | DataStore `Preferences` provider + `@Binds` |
| `ui/navigation/BingoRoute.kt` / `BingoNavHost.kt` | Modified | New `THEME` route + destination |
| `app/build.gradle.kts` / `libs.versions.toml` | Modified | New `datastore-preferences` + `material-icons-extended` deps |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|--------------|
| Two new dependencies in one change (DataStore + icons-extended) | Low | Both official AndroidX artifacts, version-aligned via `composeBom`; no transitive conflicts expected |
| Activity-level reactive Hilt state is a new pattern (previously screen-level only) | Low | Keep collection to a single `Flow<ThemeMode>`; document as intentional in `sdd-design` |
| No Compose UI test infra to verify radio-selection UI | Low | Manual/device smoke test, per standing convention |
| `material-icons-extended` pulls a large icon set | Low | R8 minification + resource shrinking already enabled; only referenced icons ship |

## Rollback Plan

Revert `MainActivity.kt` to call `BingoFFTheme { ... }` with no arguments (restores pure system-following behavior), remove the "Tema" `ListItem`/route/screen, and drop the two new dependencies. No schema/data-layer migration needed — DataStore data is additive and simply orphaned, not destructive, if removed.

## Dependencies

- `androidx.datastore:datastore-preferences` (new)
- `androidx.compose.material:material-icons-extended` (new)

## Success Criteria

- [ ] Selecting Claro/Oscuro/Sistema in `ThemeScreen` persists immediately and applies app-wide without restart.
- [ ] App relaunch restores the last-selected theme.
- [ ] Default (never-touched) behavior remains identical to today's system-following behavior.
- [ ] "Tema" appears first in `SettingsScreen`, followed by Exportar/Importar, unchanged.
- [ ] `./gradlew build` and full unit test suite pass with no regressions.

---
Estimated diff size: ~300-380 changed lines (under the 400-line review budget, but closer to it than most prior changes — monitor during `sdd-tasks`).
