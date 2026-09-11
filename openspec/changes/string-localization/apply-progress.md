# Apply Progress: String Resource Extraction + English Localization

**Phase A of 2** — Phase B (ViewModel reason-type refactors, wiring, and pinned-test updates) is a separate, later apply run and will append to this artifact.

## Status

13/13 Phase A tasks complete (A.1–A.13). Phase B (B.1–B.12) not started — out of scope for this run.

## Completed Tasks (Phase A)

- [x] A.1 Added all Phase A + Phase B-owned keys (Spanish) to `app/src/main/res/values/strings.xml` using `{screen}_{element}` / `common_` convention.
- [x] A.2 Created `app/src/main/res/values-en/strings.xml` with the same keys, English translations, identical placeholder shapes.
- [x] A.3 `ui/boards/list/BoardListScreen.kt`: swapped 5 literals (`board_list_settings_label`, `board_list_play_label`, `board_list_add_label`, `board_list_empty_message`, `board_list_item_label` format string).
- [x] A.4 `ui/boards/create/CreateBoardScreen.kt`: swapped 2 static literals (`create_board_identifier_label`, `create_board_save_button`); `identifierError` display left untouched (Phase B).
- [x] A.5 `ui/game/setup/GameSetupScreen.kt`: swapped 10 literals; converted `GameMode.label()` into a private `@Composable` extension resolving 5 `game_setup_mode_*` keys.
- [x] A.6 `ui/game/play/GamePlayScreen.kt`: swapped all non-ViewModel-owned literals (title, mode status, bingo announcement, number label, submit button, letter calls, possible-winners title, win-prediction candidate, end-game button/dialog/confirm/cancel, common_back); `inputError` display left untouched (Phase B).
- [x] A.7 `ui/boards/importexport/ImportBoardsScreen.kt`: swapped static labels/button + `common_back` (`import_boards_title`, `import_boards_json_label`, `import_boards_submit_button`); `jsonError`/`resultMessage` display left untouched (Phase B).
- [x] A.8 `ui/boards/settings/SettingsScreen.kt`: swapped 5 literals (`settings_title`, `settings_theme_item`, `settings_export_item`, `settings_import_item`, `common_back`).
- [x] A.9 `ui/boards/theme/ThemeScreen.kt`: swapped 5 literals (`theme_title`, `theme_light_option`, `theme_dark_option`, `theme_system_option`, `common_back`).
- [x] A.10 `ui/common/BingoNumberField.kt`: swapped "FREE" literal to `common_free_cell`.
- [x] A.11 `ui/common/BingoGridDisplay.kt`: swapped "FREE" literal to `common_free_cell` (dedup with A.10 — same key, no duplicate resource).
- [x] A.12 Verified: `rg -n '"Atrás"|"FREE"'` across `ui/` finds zero code occurrences (only one KDoc comment mentioning "FREE" in `BingoGridDisplay.kt`, which is prose, not a literal). All 5 back-navigation content descriptions and both FREE-cell labels resolve to `common_back` / `common_free_cell`.
- [x] A.13 Build gate: `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL (existing suite green, unmodified). `./gradlew build` → BUILD SUCCESSFUL (includes lint, assembleDebug, assembleRelease — no MissingTranslation or unused-resource errors).

## Remaining Tasks (Phase B — not started, separate apply run)

- [ ] B.1 `ui/boards/create/CreateBoardUiState.kt`: `identifierError: String?` → `CreateBoardErrorReason?`.
- [ ] B.2 `ui/boards/create/CreateBoardViewModel.kt`: update `onSubmit()` assignment sites.
- [ ] B.3 `ui/boards/create/CreateBoardScreen.kt`: add `toMessage()` extension, wire `supportingText`.
- [ ] B.4 `ui/game/play/GamePlayUiState.kt`: `inputError: String?` → `GamePlayInputErrorReason?`.
- [ ] B.5 `ui/game/play/GamePlayViewModel.kt`: update `PendingEntry.error` type and assignment sites.
- [ ] B.6 `ui/game/play/GamePlayScreen.kt`: add resolver extension, wire `supportingText`.
- [ ] B.7 `ui/boards/importexport/ImportBoardsUiState.kt`: `jsonError`/`resultMessage` → sealed/data types.
- [ ] B.8 `ui/boards/importexport/ImportBoardsViewModel.kt`: update assignment sites.
- [ ] B.9 `ui/boards/importexport/ImportBoardsScreen.kt`: add resolver extensions, wire display + snackbar.
- [ ] B.10 `GamePlayViewModelTest.kt:186`: assert on `GamePlayInputErrorReason.DuplicateCall`.
- [ ] B.11 `ImportBoardsViewModelTest.kt:86`: assert on `ImportResultSummary(imported = 1, skipped = 1)`.
- [ ] B.12 Build gate: `./gradlew testDebugUnitTest` (incl. B.10/B.11) + `./gradlew build`; commit Phase B alone.

## Files Changed (Phase A)

| File | Action |
|---|---|
| `app/src/main/res/values/strings.xml` | Modified — added ~50 keys (all Phase A + Phase B-owned) |
| `app/src/main/res/values-en/strings.xml` | Created — English parity for all keys |
| `app/src/main/java/com/sergiodev/bingo/ui/boards/list/BoardListScreen.kt` | Modified |
| `app/src/main/java/com/sergiodev/bingo/ui/boards/create/CreateBoardScreen.kt` | Modified |
| `app/src/main/java/com/sergiodev/bingo/ui/game/setup/GameSetupScreen.kt` | Modified |
| `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayScreen.kt` | Modified |
| `app/src/main/java/com/sergiodev/bingo/ui/boards/importexport/ImportBoardsScreen.kt` | Modified |
| `app/src/main/java/com/sergiodev/bingo/ui/boards/settings/SettingsScreen.kt` | Modified |
| `app/src/main/java/com/sergiodev/bingo/ui/boards/theme/ThemeScreen.kt` | Modified |
| `app/src/main/java/com/sergiodev/bingo/ui/common/BingoNumberField.kt` | Modified |
| `app/src/main/java/com/sergiodev/bingo/ui/common/BingoGridDisplay.kt` | Modified |

Untouched, per Phase A scope: `CreateBoardViewModel.kt`, `GamePlayViewModel.kt`, `ImportBoardsViewModel.kt`, their 3 `UiState` files, `GamePlayViewModelTest.kt`, `ImportBoardsViewModelTest.kt`.

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command and exact result | `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL, existing suite green, zero files touched under `src/test` |
| Runtime harness command/scenario and exact result | N/A — no Compose UI test infra project-wide (standing convention); manual device verification of Spanish (default) and English (`en` locale) rendering is expected-deferred, consistent with prior localization/theme work |
| Rollback boundary | Revert the 9 listed Composable files + delete `values-en/strings.xml` + revert `values/strings.xml` to its original `app_name`-only content; no ViewModel or test file was touched so nothing else is affected |

## Deviations from Design

None — implementation matches design.md's Phase A file list, resource-key convention, and format-string shapes exactly.

## Build Gate Results

- `./gradlew testDebugUnitTest`: BUILD SUCCESSFUL
- `./gradlew build`: BUILD SUCCESSFUL (assembleDebug, assembleRelease, lint — no errors)

## Next Step

Phase B apply run: implement B.1–B.12 (ViewModel reason types, Composable wiring, pinned test updates), then run `sdd-verify` for the complete change.
