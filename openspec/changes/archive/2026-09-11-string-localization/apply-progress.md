# Apply Progress: String Resource Extraction + English Localization

**Phase A of 2** — complete and committed to `master` (`0e2dc6d`).
**Phase B of 2** — complete (this run). Both phases done; change ready for `sdd-verify`.

## Status

13/13 Phase A tasks complete (A.1–A.13). 12/12 Phase B tasks complete (B.1–B.12). 25/25 total.

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

## Completed Tasks (Phase B)

- [x] B.1 `ui/boards/create/CreateBoardUiState.kt`: `identifierError: String?` → `CreateBoardErrorReason?`; added `sealed interface CreateBoardErrorReason { BlankIdentifier, DuplicateIdentifier }`. (Landed as an uncommitted partial change from a prior crashed attempt; verified it matched the design's exact shape and continued from it rather than redoing it.)
- [x] B.2 `ui/boards/create/CreateBoardViewModel.kt`: `onSubmit()`'s 2 `.copy(identifierError = "...")` literal sites now assign `CreateBoardErrorReason.BlankIdentifier` / `.DuplicateIdentifier`. No trigger/condition changed.
- [x] B.3 `ui/boards/create/CreateBoardScreen.kt`: added private `@Composable fun CreateBoardErrorReason.toMessage(): String` (mirrors `GameSetupScreen.kt`'s `GameMode.label()` pattern); `supportingText` now resolves via `it.toMessage()` instead of rendering the raw string.
- [x] B.4 `ui/game/play/GamePlayUiState.kt`: `inputError: String?` → `GamePlayInputErrorReason?`; added `sealed interface GamePlayInputErrorReason { InvalidNumber, LetterMismatch, DuplicateCall }`.
- [x] B.5 `ui/game/play/GamePlayViewModel.kt`: private `PendingEntry.error: String?` → `GamePlayInputErrorReason?`; `onSubmitCall()`'s 3 assignment sites (invalid number, letter mismatch, duplicate call) updated to the sealed values.
- [x] B.6 `ui/game/play/GamePlayScreen.kt`: added private `@Composable fun GamePlayInputErrorReason.toMessage(): String`; `BingoNumberField`'s `supportingText` now resolves via `state.inputError?.toMessage()`.
- [x] B.7 `ui/boards/importexport/ImportBoardsUiState.kt`: `jsonError: String?` → `ImportBoardsErrorReason?` (added `sealed interface { BlankInput, InvalidJson }`); `resultMessage: String?` renamed to `resultSummary: ImportResultSummary?` (added `data class ImportResultSummary(val imported: Int, val skipped: Int)`).
- [x] B.8 `ui/boards/importexport/ImportBoardsViewModel.kt`: `onSubmit()`'s 2 `jsonError` sites now assign the sealed values; the post-import assignment now builds `ImportResultSummary(imported = result.imported, skipped = result.skipped)` instead of a pre-formatted string.
- [x] B.9 `ui/boards/importexport/ImportBoardsScreen.kt`: added private `@Composable` extensions `ImportBoardsErrorReason.toMessage()` and `ImportResultSummary.toMessage()` (the latter → `stringResource(R.string.import_result_message, imported, skipped)`); wired both the textarea `supportingText` and the snackbar `LaunchedEffect` through them.
- [x] B.10 `GamePlayViewModelTest.kt:186`: replaced `assertEquals("Número ya cantado", state.inputError)` with `assertEquals(GamePlayInputErrorReason.DuplicateCall, state.inputError)`.
- [x] B.11 `ImportBoardsViewModelTest.kt:86`: replaced `assertEquals("1 importados, 1 omitidos", state.resultMessage)` with `assertEquals(ImportResultSummary(imported = 1, skipped = 1), state.resultSummary)`.
- [x] B.12 Build gate: `./gradlew testDebugUnitTest --tests "*GamePlayViewModelTest*" --tests "*ImportBoardsViewModelTest*"` → BUILD SUCCESSFUL, then full `./gradlew testDebugUnitTest` and `./gradlew build` → both BUILD SUCCESSFUL. Not yet committed — commit is a separate step outside this apply run's scope.

## Files Changed (Phase B)

| File | Action | What Was Done |
|---|---|---|
| `ui/boards/create/CreateBoardUiState.kt` | Modified | `identifierError` field type + `CreateBoardErrorReason` sealed interface (already present from prior crashed attempt; verified matches design) |
| `ui/boards/create/CreateBoardViewModel.kt` | Modified | 2 assignment sites in `onSubmit()` |
| `ui/boards/create/CreateBoardScreen.kt` | Modified | `toMessage()` extension + `supportingText` wiring |
| `ui/game/play/GamePlayUiState.kt` | Modified | `inputError` field type + `GamePlayInputErrorReason` sealed interface |
| `ui/game/play/GamePlayViewModel.kt` | Modified | `PendingEntry.error` type + 3 assignment sites in `onSubmitCall()` |
| `ui/game/play/GamePlayScreen.kt` | Modified | `toMessage()` extension + `supportingText` wiring |
| `ui/boards/importexport/ImportBoardsUiState.kt` | Modified | `jsonError`/`resultSummary` field types + `ImportBoardsErrorReason` sealed interface + `ImportResultSummary` data class |
| `ui/boards/importexport/ImportBoardsViewModel.kt` | Modified | `onSubmit()`'s 2 `jsonError` sites + `resultSummary` assignment |
| `ui/boards/importexport/ImportBoardsScreen.kt` | Modified | 2 `toMessage()` extensions + textarea/snackbar wiring |
| `app/src/test/.../game/play/GamePlayViewModelTest.kt` | Modified | Line 186 assertion now targets `GamePlayInputErrorReason.DuplicateCall` |
| `app/src/test/.../boards/importexport/ImportBoardsViewModelTest.kt` | Modified | Line 86 assertion now targets `ImportResultSummary(imported = 1, skipped = 1)` |

Phase B did not touch `values/strings.xml` or `values-en/strings.xml` — all 8 ViewModel-owned keys already existed from Phase A.

## Work Unit Evidence (Phase B)

| Evidence | Value |
|---|---|
| Focused test command and exact result | `./gradlew testDebugUnitTest --tests "*GamePlayViewModelTest*" --tests "*ImportBoardsViewModelTest*"` → BUILD SUCCESSFUL |
| Runtime harness command/scenario and exact result | N/A — no Compose UI test infra project-wide (standing convention); manual device verification of Spanish (default) and English (`en` locale) rendering is expected-deferred, consistent with Phase A and prior localization/theme work |
| Rollback boundary | Revert the 3 `UiState` files, 2 ViewModels, 3 Composables' display sites, and 2 test files listed above; Phase A's `strings.xml`/`values-en/strings.xml` entries stay untouched and harmless |

## Deviations from Design (Phase B)

None — implementation matches design.md's Phase B file list, sealed-type shapes, and Composable-resolution pattern exactly. The one pre-existing partial edit found at start of this run (`CreateBoardUiState.kt`'s `identifierError`/`CreateBoardErrorReason`) was verified byte-for-byte against the design's specified shape before continuing.

## Build Gate Results (Phase B)

- `./gradlew testDebugUnitTest` (focused, B.10/B.11): BUILD SUCCESSFUL
- `./gradlew testDebugUnitTest` (full suite): BUILD SUCCESSFUL
- `./gradlew build`: BUILD SUCCESSFUL (assembleDebug, assembleRelease, lint, test — no errors)

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

Both phases complete (25/25 tasks). Ready for `sdd-verify` on the complete change. Phase B has not yet been committed — that remains a separate delivery step outside this apply run's scope.
