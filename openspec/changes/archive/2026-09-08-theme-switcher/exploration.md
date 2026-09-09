# Exploration: theme-switcher

## Current State

Dark theme is already passively supported. `Theme.kt` defines `BingoFFTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content)` with both `DarkColorScheme`/`LightColorScheme` already defined, plus dynamic color on API 31+. The gap is purely a missing manual user override — `MainActivity.kt` calls `BingoFFTheme { ... }` with zero args, so it always follows the system setting. `Color.kt`'s `Success`/`OnSuccess` pair (from a prior change) is hardcoded outside the `ColorScheme` and unaffected by dark mode.

No persistence beyond Room exists — confirmed via `gradle/libs.versions.toml` and a repo-wide grep for `DataStore`/`SharedPreferences` (zero matches). This is a green-field dependency decision. `SettingsScreen.kt`/`SettingsContent` has exactly 2 `ListItem`s (Exportar, Importar) in a `Column` under a standard `TopAppBar` Scaffold; adding "Tema" as a 3rd item at the top is mechanical. `BingoRoute.kt` is a plain `object` of `const val` route strings (4 routes + `SETTINGS`/`IMPORT_BOARDS`); a `THEME` route follows the same pattern. The bottom-bar "Configuración" entry is already fully wired (not inert, unlike the state before `export-import-boards`). `di/DatabaseModule.kt`/`RepositoryModule.kt` give a clear template for a new preferences DI module + repository. `GameSetupScreen.kt`'s `FilterChip`-based `GameModeChip` is the only existing selectable-option UI precedent, though `ListItem`+`RadioButton` is more idiomatic for a dedicated settings screen. Testing is pure-JVM JUnit only, no Robolectric, no Compose UI tests — DataStore Preferences (unlike `SharedPreferences`) works under plain JVM tests via a real temp file, avoiding the Android-stub problem flagged for `org.json` in the prior change's exploration.

## Affected Areas

- `app/src/main/java/com/sergiodev/bingo/ui/theme/Theme.kt` — `BingoFFTheme` params need to be driven by persisted state instead of always-default.
- `app/src/main/java/com/sergiodev/bingo/MainActivity.kt` — sole call site of `BingoFFTheme`; needs new reactive state collection (first Activity-level Hilt state in this codebase).
- `app/src/main/java/com/sergiodev/bingo/ui/boards/settings/SettingsScreen.kt` — new "Tema" `ListItem` at top + new `onNavigateToTheme` callback.
- `app/src/main/java/com/sergiodev/bingo/ui/navigation/BingoRoute.kt` / `BingoNavHost.kt` — new `THEME` route + destination wiring.
- New `ui/boards/theme/` package — `ThemeScreen.kt` + `ThemeViewModel.kt`, following `ui/boards/settings/` convention.
- New `di/` module + `domain/repository/ThemeRepository.kt` + impl — mirrors `BoardRepository`/`RoomBoardRepository`.
- `app/build.gradle.kts` / `gradle/libs.versions.toml` — new DataStore Preferences dependency.

## Approaches Considered

1. **DataStore Preferences, 3-state `ThemeMode` (LIGHT/DARK/SYSTEM)** — Flow-native fits existing repository conventions, Google-recommended, JVM-testable without Robolectric, preserves current passive system-following as an explicit default; net-new dependency + DI plumbing. Effort: Medium.
2. **Plain `SharedPreferences`** — zero new dependency, minimal code, but not reactive without a hand-rolled Flow bridge, breaks the Flow-first repository convention, harder to unit test under the pure-JVM convention. Effort: Low but worse long-term fit.
3. **2-state model (Light/Dark only)** — matches literal user wording, smallest surface, but silently removes today's existing system-following behavior as a default — a UX regression risk if not explicitly called out. Effort: Low.
4. **3-state model (Light/Dark/System)** — platform convention, preserves current behavior as an explicit choice; marginally larger enum/UI. Effort: Low-Medium.
5. **UI: `FilterChip` row (mirrors `GameSetupScreen`) vs `ListItem`+`RadioButton`** — taste decision, not a complexity difference; `RadioButton` is more idiomatic for a settings destination but is a first-time pattern in this codebase.

## Recommendation

DataStore Preferences + 3-state `ThemeMode` (default `SYSTEM`), fitting the existing Flow-first repository architecture and pure-JVM test convention. `ListItem`+`RadioButton` is the marginally more idiomatic UI pick over `FilterChip`, but either is defensible — confirm both in `sdd-propose`.

## Risks

- DataStore vs. SharedPreferences dependency choice needs explicit sign-off (green-field, like the Gson decision in `export-import-boards`).
- 2-state vs. 3-state model is unresolved and affects whether today's passive system-following behavior is preserved or silently dropped.
- Theme screen UI pattern (`FilterChip` vs `ListItem`+`RadioButton`) undecided.
- `Success`/`OnSuccess` hardcoded colors are unaffected by dark mode — may need separate handling if full dark-mode token coverage is expected (likely out of scope).
- Activity-level reactive state collection in `MainActivity` is a new pattern for this codebase (every other Hilt injection is screen-level via `hiltViewModel()`).
- No Compose UI test infra exists; manual smoke-testing is the likely verification path, consistent with prior changes.
- Icon choice for "Tema" `ListItem` (`Palette`/`DarkMode`/`Contrast`) is undecided and is a first-time icon import.

## Open Questions for Proposal

1. DataStore Preferences vs. SharedPreferences (recommend DataStore).
2. 2-state (Light/Dark) vs. 3-state (Light/Dark/System) theme model (recommend 3-state, preserves current behavior).
3. Theme screen UI pattern: `ListItem`+`RadioButton` vs. `FilterChip` row (recommend `ListItem`+`RadioButton`).

## Ready for Proposal

Yes — no blockers, only decisions to resolve explicitly in `sdd-propose`.
