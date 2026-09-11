# Tasks: String Resource Extraction + English Localization

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | Phase A ~360, Phase B ~300, combined ~660 |
| 400-line budget risk | Low (per phase) |
| Chained PRs recommended | No (no PR workflow; direct commits to `master`) |
| Suggested split | Phase A commit → Phase B commit (sequential, not chained PRs) |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main (adapted: sequential direct commits, no PR) |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: stacked-to-main
400-line budget risk: Low

Rationale: the Phase A / Phase B split is a confirmed design decision, not a risk-triggered choice. Each phase independently estimates well under 400 lines; only the combined total (~660) exceeds budget, and the two phases are never combined into one review/commit unit.

### Suggested Work Units

| Unit | Goal | Likely commit | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Phase A: resources + pure Composable swaps | Commit 1 | `./gradlew testDebugUnitTest` | N/A — no Compose UI test infra project-wide; manual device check (Spanish default + `en` locale) per design | Revert 9 Composable files + delete `values-en/strings.xml` + revert `values/strings.xml` to `app_name`-only; no ViewModel/test touched |
| 2 | Phase B: ViewModel reason types + Composable wiring + pinned tests | Commit 2 | `./gradlew testDebugUnitTest --tests "*GamePlayViewModelTest*" --tests "*ImportBoardsViewModelTest*"` then full `testDebugUnitTest` | N/A — same reason as Unit 1 | Revert 3 `UiState` files, 2 ViewModels, 3 Composables' display sites, 2 test files; Phase A's `strings.xml` keys stay, harmless |

## Phase A: Resources + Composable String Swaps (no ViewModel changes)

- [x] A.1 Add all ~54 keys (Spanish) to `app/src/main/res/values/strings.xml` using `{screen}_{element}` / `common_` convention, including the 8 ViewModel-owned keys Phase B will consume.
- [x] A.2 Create `app/src/main/res/values-en/strings.xml` with the same ~54 keys, English translations, identical placeholder shapes (e.g. `import_result_message`, `win_prediction_candidate`).
- [x] A.3 `ui/boards/list/BoardListScreen.kt`: swap 5 literals to `stringResource()`, incl. `board_list_item_label` format string.
- [x] A.4 `ui/boards/create/CreateBoardScreen.kt`: swap 2 static literals ("Identificador" label, "Guardar cartón" button) to `stringResource()`; leave `identifierError` display untouched.
- [x] A.5 `ui/game/setup/GameSetupScreen.kt`: swap 10 literals; convert `GameMode.label()` into a private `@Composable` extension resolving 5 `game_setup_mode_*` keys.
- [x] A.6 `ui/game/play/GamePlayScreen.kt`: swap 11 of 13 literals (static labels/buttons + `game_play_mode_status`, `game_play_bingo_announcement`, `game_play_letter_calls`, `win_prediction_candidate`, `common_back`); leave `inputError` display untouched.
- [x] A.7 `ui/boards/importexport/ImportBoardsScreen.kt`: swap 2 of 4 literals (static labels/button, `common_back`); leave `jsonError`/`resultMessage` display untouched.
- [x] A.8 `ui/boards/settings/SettingsScreen.kt`: swap 5 literals to `stringResource()`.
- [x] A.9 `ui/boards/theme/ThemeScreen.kt`: swap 5 literals to `stringResource()`.
- [x] A.10 `ui/common/BingoNumberField.kt`: swap "FREE" literal to `common_free_cell`.
- [x] A.11 `ui/common/BingoGridDisplay.kt`: swap "FREE" literal to `common_free_cell` (dedup with A.10 — same key, no duplicate resource).
- [x] A.12 Verify all 5 "Atrás" content-description call sites (across A.3–A.11 files) resolve to the single `common_back` key.
- [x] A.13 **Build gate**: run `./gradlew testDebugUnitTest` (must stay green, unmodified) and `./gradlew build`; commit Phase A alone.

## Phase B: ViewModel Reason Types + Composable Wiring + Pinned Tests

- [ ] B.1 `ui/boards/create/CreateBoardUiState.kt`: change `identifierError: String?` → `CreateBoardErrorReason?`; add `sealed interface CreateBoardErrorReason { data object BlankIdentifier; data object DuplicateIdentifier }`.
- [ ] B.2 `ui/boards/create/CreateBoardViewModel.kt`: update `onSubmit()`'s 2 assignment sites to use the sealed values instead of literal strings.
- [ ] B.3 `ui/boards/create/CreateBoardScreen.kt`: add private `@Composable fun CreateBoardErrorReason.toMessage(): String` (mirrors `GameMode.label()`); wire `supportingText` through it.
- [ ] B.4 `ui/game/play/GamePlayUiState.kt`: change `inputError: String?` → `GamePlayInputErrorReason?`; add `sealed interface GamePlayInputErrorReason { data object InvalidNumber; data object LetterMismatch; data object DuplicateCall }`.
- [ ] B.5 `ui/game/play/GamePlayViewModel.kt`: change private `PendingEntry.error` field type; update `onSubmitCall()`'s 3 assignment sites (invalid number, letter mismatch, duplicate call).
- [ ] B.6 `ui/game/play/GamePlayScreen.kt`: add private `@Composable` extension resolving `GamePlayInputErrorReason` to text; wire `supportingText` through it.
- [ ] B.7 `ui/boards/importexport/ImportBoardsUiState.kt`: change `jsonError: String?` → `ImportBoardsErrorReason?`; rename `resultMessage: String?` → `resultSummary: ImportResultSummary?`; add `sealed interface ImportBoardsErrorReason { data object BlankInput; data object InvalidJson }` and `data class ImportResultSummary(val imported: Int, val skipped: Int)`.
- [ ] B.8 `ui/boards/importexport/ImportBoardsViewModel.kt`: update `onSubmit()`'s 2 `jsonError` assignment sites and the `resultSummary` assignment.
- [ ] B.9 `ui/boards/importexport/ImportBoardsScreen.kt`: add private `@Composable` extensions resolving `jsonError` and `resultSummary` (→ `stringResource(R.string.import_result_message, imported, skipped)`); wire display + snackbar.
- [ ] B.10 `app/src/test/.../game/play/GamePlayViewModelTest.kt:186`: replace `assertEquals("Número ya cantado", state.inputError)` with `assertEquals(GamePlayInputErrorReason.DuplicateCall, state.inputError)`.
- [ ] B.11 `app/src/test/.../boards/importexport/ImportBoardsViewModelTest.kt:86`: replace the literal-text assertion with `assertEquals(ImportResultSummary(imported = 1, skipped = 1), state.resultSummary)`.
- [ ] B.12 **Build gate**: run `./gradlew testDebugUnitTest` (full suite incl. B.10/B.11) and `./gradlew build`; commit Phase B alone.
