# Proposal: String Resource Extraction + English Localization

## Intent

Zero localization infrastructure exists today: `values/strings.xml` has exactly one entry (`app_name`), no `values-en/` directory exists, and ~54 distinct Spanish UI string literals are hardcoded across 12 files (Composables and 3 ViewModels). This is a green-field extraction, not a partial migration. Scope is confirmed as extraction **plus** adding English — not Spanish-only hygiene — so the app renders correctly for English-locale devices via standard Android resource resolution (system picks `values-en/` when the device locale matches, else falls back to default `values/` Spanish).

## Scope

### In Scope
- `values/strings.xml` (Spanish, default) and `values-en/strings.xml` (English) with all ~54 keys, translated.
- Replace every hardcoded literal with `stringResource()` across the 12 affected files.
- Dedup: single `R.string.back` (5 "Atrás" content descriptions), single `R.string.free_cell` (2 "FREE" literals).
- Key convention: `snake_case`, screen/feature-prefixed (e.g. `game_setup_title`, `import_result_message`, `game_play_bingo_announcement`).
- Refactor `CreateBoardViewModel`, `GamePlayViewModel`, `ImportBoardsViewModel` to expose a sealed reason/enum type per ViewModel instead of a literal `String` in `UiState`; the Composable maps the reason to `stringResource(R.string.x, ...)`. No `Context`/`Application` injection into ViewModels.
- Android format placeholders for the 6 interpolated strings; fixed 2-arg format string for the import-count message — **no `<plurals>`**.
- Update `GamePlayViewModelTest.kt:186` and `ImportBoardsViewModelTest.kt:86` to assert on the new reason type instead of literal text.

### Out of Scope
- Any in-app language-switcher UI/settings screen or persisted locale override — locale resolution stays automatic (device setting), unlike `theme-switcher`'s manual override pattern. Explicitly deferred.
- Android `<plurals>` resources.
- New Compose UI test infrastructure (none exists project-wide; verification of rendered text stays manual/device).
- Domain layer (`BingoWinChecker`, `WinPrediction`, `GameMode`), `BoardRepository`, `ThemeRepository` — no logic changes, presentation/ViewModel-output-shape only.

## Capabilities

### New Capabilities
- `ui-localization`: string-resource infrastructure (`values/` + `values-en/`) and `stringResource()` adoption across all screens.

### Modified Capabilities
- None. Default (Spanish) rendered text is unchanged; `board-export-import`'s example phrasing ("N importados, M omitidos") is preserved verbatim as a resource default, not altered.

## Approach

Two-phase delivery (this session's 400-line review budget is exceeded by the full change — estimate below):
- **Phase A**: `strings.xml` + `values-en/` + `stringResource()` swaps for static labels/content-descriptions and interpolated strings across the 9 pure-Composable files (no ViewModel changes). Independently buildable/verifiable.
- **Phase B**: the 3 ViewModel reason-type refactors, their Composable mapping, and the 2 pinned test updates. Depends on Phase A's keys existing. Own apply run + build-gate.

Trickiest format strings:
- `game_play_bingo_announcement`: `"¡Bingo! #%1$d %2$s"` (args: `sequentialNumber: Int`, `identifier: String`) / en `"Bingo! #%1$d %2$s"`.
- `win_prediction_candidate`: `"Cartón %1$s%2$s"` (args: `identifier: String`, `suffix: String` — suffix carries its own leading punctuation/space) / en `"Card %1$s%2$s"`.
- `import_result_message`: `"%1$d importados, %2$d omitidos"` / en `"%1$d imported, %2$d skipped"`.

## Affected Areas

| Area | Impact | Description |
|------|--------|--------------|
| `res/values/strings.xml`, `res/values-en/strings.xml` | New | ~54 keys × 2 locales |
| 9 Composable screens/components | Modified | Literal → `stringResource()` |
| `CreateBoardViewModel`, `GamePlayViewModel`, `ImportBoardsViewModel` | Modified | Literal `String` → sealed reason type in `UiState` |
| `GamePlayViewModelTest.kt`, `ImportBoardsViewModelTest.kt` | Modified | Assert on reason type, not literal text |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|--------------|
| Change exceeds 400-line review budget | High | Phase A/B split with own apply + build-gate per phase |
| English translations introduce meaning drift | Med | Manual/device review of rendered English strings |
| Reason-type refactor changes `UiState` shape | Med | Confined to 3 ViewModels; 2 pinned tests updated explicitly |
| Missed literal during 12-file sweep | Low | Cross-check final diff against exploration's exhaustive inventory |

## Rollback Plan

Each phase is independently revertible: Phase A reverts by restoring literals and deleting the two `strings.xml` files; Phase B reverts by restoring the literal-`String` `UiState` fields and their two test assertions. No data/schema migration involved.

## Dependencies

None.

## Success Criteria

- [ ] All ~54 strings resolved via `stringResource()`; zero hardcoded UI literals remain in the 12 files.
- [ ] Device set to English renders `values-en/strings.xml`; default/unset locale renders Spanish, unchanged from today.
- [ ] `GamePlayViewModelTest` and `ImportBoardsViewModelTest` pass against the new reason type.
- [ ] Full unit test suite and `./gradlew build` stay green after both phases.

---
Estimated diff size: ~600-750 changed lines total (54 strings × 2 locales + 12 file swaps + 3 ViewModel refactors), exceeding the 400-line budget — Phase A/B split required at `sdd-tasks`.
