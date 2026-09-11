# Exploration: string-localization

## Current State

`app/src/main/res/values/strings.xml` has exactly one entry (`app_name`). Zero `values-*` locale directories exist, zero `Locale`/`LocaleList`/`setApplicationLocales` usage, no locale config in `app/build.gradle.kts`. Exactly one `stringResource()` call exists anywhere in the codebase (`BoardListScreen.kt:79`, for `app_name`) — everything else is a raw literal. This is a green-field extraction.

Exhaustive inventory: ~54 distinct hardcoded UI-facing string literals across 12 files:
- `BoardListScreen.kt` (5), `CreateBoardScreen.kt` (2), `CreateBoardViewModel.kt` (2), `GameSetupScreen.kt` (10, including a private `GameMode.label()` extension with 5 literals), `GamePlayScreen.kt` (13), `GamePlayViewModel.kt` (3), `ImportBoardsScreen.kt` (4), `ImportBoardsViewModel.kt` (3), `SettingsScreen.kt` (5), `ThemeScreen.kt` (5), `BingoNumberField.kt` (1, "FREE"), `BingoGridDisplay.kt` (1, duplicate "FREE").
- Zero hardcoded strings in `BoardListViewModel`, `SettingsViewModel`, `ThemeViewModel`, `GameSetupViewModel`, `MainActivity`, `BingoRoute`, `BingoNavHost`, `EmptyState` (message is caller-supplied — a good existing pattern).

Categories:
- (a) Static labels/titles/buttons/contentDescriptions — ~40 occurrences, 1:1 `stringResource()` swaps. Includes 5 duplicate "Atrás" content descriptions and 2 duplicate "FREE" literals worth deduplicating into shared resource keys.
- (b) Interpolated strings (6) needing format placeholders: `"#${board.id} · ${board.identifier}"`, `"Modo: ${state.mode.name} · Llamadas: ${state.calledCount}"`, `"¡Bingo! #${win.sequentialNumber} ${win.identifier}"`, `"${letter.name}: ${calls.joinToString(", ")}"`, `"Cartón ${candidate.identifier}$suffix"`, `"${result.imported} importados, ${result.skipped} omitidos"`.
- (c) ViewModel-constructed strings (8, across `CreateBoardViewModel`, `GamePlayViewModel`, `ImportBoardsViewModel`) — architectural question, since no `@HiltViewModel` in this codebase currently takes `Context`/`Application` (verified across all 8 ViewModels).

Testing: only 2 of ~15 test files pin exact literal text — `GamePlayViewModelTest.kt:186` (`assertEquals("Número ya cantado", state.inputError)`) and `ImportBoardsViewModelTest.kt:86` (`assertEquals("1 importados, 1 omitidos", state.resultMessage)`). Everything else uses `assertNotNull`/`assertNull`, a low-risk migration surface. Tests are pure-JVM JUnit, no Robolectric, no Compose UI tests.

## Affected Areas

- `app/src/main/res/values/strings.xml` — needs ~54 new entries.
- `ui/boards/list/BoardListScreen.kt`, `ui/boards/create/CreateBoardScreen.kt` + `CreateBoardViewModel.kt`, `ui/game/setup/GameSetupScreen.kt`, `ui/game/play/GamePlayScreen.kt` + `GamePlayViewModel.kt`, `ui/boards/importexport/ImportBoardsScreen.kt` + `ImportBoardsViewModel.kt`, `ui/boards/settings/SettingsScreen.kt`, `ui/boards/theme/ThemeScreen.kt`, `ui/common/BingoNumberField.kt` + `BingoGridDisplay.kt`.
- `app/src/test/.../GamePlayViewModelTest.kt` (line 186), `ImportBoardsViewModelTest.kt` (line 86) — need updating if ViewModel error/result strings change shape.

## Approaches Considered

1. **Pure `strings.xml` extraction (Spanish only), Composable-level swaps for categories (a)+(b); leave ViewModel error strings as literals for now** — matches literal user request, low risk, no pinned-test breakage; doesn't fully solve hardcoding for the 8 ViewModel-owned strings. Effort: Low.
2. **Same as (1) plus a sealed error/reason type per affected ViewModel, mapped to `stringResource()` in the Composable** — fully solves hardcoding, keeps ViewModels pure/platform-agnostic; changes `UiState` shape for 3 ViewModels, requires updating 2 pinned test assertions. Effort: Medium-High.
3. **Give ViewModels `@ApplicationContext`/`Application` access to call `context.getString()` directly** — smallest ViewModel diff, but breaks this codebase's confirmed clean-architecture convention (zero existing precedent), likely forces Robolectric into currently-pure-JVM tests. Effort: Medium, architecturally regressive.
4. **Also add `values-en/strings.xml` (second language)** — none of what the user asked for; full translation of ~54 strings plus a language-switching UX decision, out of scope of the literal request. Effort: High.

## Recommendation

Approach 1 as immediate scope; propose Approach 2 as an explicit, separately-decided follow-up for the 3 ViewModels in category (c) rather than silently bundling it. Approach 3 should be explicitly rejected. Approach 4 (second language) is out of scope unless the user explicitly asks.

## Risks

- User's message ("localizar") is ambiguous between pure resource-hygiene and adding a second language — needs explicit resolution before `sdd-propose`.
- ViewModel-string architecture question (sealed reason type vs. Context injection vs. leave-as-is) is unresolved and affects `UiState` shape for `CreateBoardViewModel`, `GamePlayViewModel`, `ImportBoardsViewModel`.
- 2 pinned unit-test assertions will break if ViewModel error/result strings change shape (mitigation: assert on enum/reason instead of literal text).
- Duplicate literals ("Atrás" x5, "FREE" x2) need a resource-key naming/dedup decision.
- Interpolated Spanish plural phrasing ("N importados, M omitidos") needs a decision: Android `<plurals>` vs. a fixed two-arg format string.
- No Compose UI test infra / no Robolectric — verification stays manual, consistent with prior changes.

## Open Questions for Proposal

1. Spanish-only resource extraction (hygiene) vs. also adding a second language (e.g. English) now.
2. ViewModel-string architecture: leave 8 ViewModel-owned strings as literals for now (Approach 1) vs. refactor them into a sealed reason/enum type resolved to `stringResource()` in the Composable (Approach 2).
3. Resource-key naming/dedup convention for duplicate literals ("Atrás" x5, "FREE" x2).
4. Plural handling for "N importados, M omitidos": Android `<plurals>` vs. a fixed two-arg format string.

## Ready for Proposal

Yes — no blockers to the investigation itself; the open questions above should be resolved (or explicitly deferred) before `sdd-propose`.
