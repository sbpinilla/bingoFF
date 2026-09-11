# Design: String Resource Extraction + English Localization

Proposal: Engram `sdd/string-localization/proposal` / `openspec/changes/string-localization/proposal.md`.

## Technical Approach

Create `values/strings.xml` (Spanish, default) and `values-en/strings.xml` (English), populate all ~54 keys under a `snake_case`, screen-prefixed convention, and swap every hardcoded literal for `stringResource()`. For the 8 ViewModel-owned strings (3 ViewModels), replace the `String?`/`String` UiState field with a per-ViewModel sealed reason type (or, for the one data-carrying case, a plain data class) so the ViewModel stays platform-agnostic; the owning Composable resolves the reason to `stringResource()` via a private `@Composable` extension function, mirroring the existing `GameMode.label()` pattern in `GameSetupScreen.kt`. Delivered as two independently buildable phases per the proposal's 400-line budget split.

## Architecture Decisions

### Decision: Resource-key convention — `{screen}_{element}[_qualifier]`, `common_` for dedup

| Option | Tradeoff | Decision |
|---|---|---|
| Screen-prefixed snake_case (`game_setup_title`, `import_result_message`) | Matches Android convention, keys are greppable per-screen, mechanical to derive from file location | **Chosen** |
| Flat unprefixed keys | Shorter, but collision-prone across 8 screens and no visual grouping in `strings.xml` | Rejected |

Prefixes: `board_list_`, `create_board_`, `game_setup_`, `game_play_`, `import_boards_`, `settings_`, `theme_`, `common_` (cross-screen shared only). `common_back` replaces all 5 "Atrás" content descriptions; `common_free_cell` replaces both "FREE" literals (`BingoNumberField.kt`, `BingoGridDisplay.kt`). `GameMode.label()`'s 5 branches become `game_setup_mode_columna|o|l|i|carton_completo`. Every other key is a direct 1:1 swap named after its screen + UI role (e.g. `game_play_end_game_button`, `import_boards_submit_button`).

### Decision: Per-ViewModel sealed reason types, not a shared cross-ViewModel type

**Choice**: `CreateBoardErrorReason`, `GamePlayInputErrorReason`, `ImportBoardsErrorReason` — three independent sealed interfaces, one per ViewModel, plus a plain `ImportResultSummary` data class for the one case that carries data.
**Alternatives considered**: One shared `UiErrorReason` enum across all three — rejected: the actual current fields prove the errors are domain-specific (`CreateBoardViewModel.identifierError`: blank vs. duplicate-identifier; `GamePlayViewModel.inputError`: invalid-number vs. letter-mismatch vs. duplicate-call; `ImportBoardsViewModel.jsonError`: blank vs. malformed-JSON) — no case is reused across ViewModels, so a shared type would need per-ViewModel-unused branches.
**Rationale**: Matches this codebase's per-feature isolation (separate `UiState` per screen already); each sealed type lives beside its owning `UiState` file, zero cross-ViewModel coupling.

**Confirmed field-level shape** (read from current source):
- `CreateBoardUiState.identifierError: String?` → `CreateBoardErrorReason?`. Only 2 real triggers: `onSubmit()` blank check ("no puede estar vacío") and `repository.addBoard(...).onFailure` ("ya existe"). `fieldErrors: Map<BingoLetter, List<String?>>` is **out of scope** — its strings (`"invalid"`, `"out_of_range"`, `"duplicate"`) are already internal markers never rendered (`computeFieldErrors`'s own doc comment: "the UI reads nullness via `isError`, never the string content").
- `GamePlayUiState.inputError: String?` (backed by private `PendingEntry.error: String?`) → `GamePlayInputErrorReason?`. 3 triggers in `onSubmitCall()`: invalid number, letter/number mismatch, duplicate call.
- `ImportBoardsUiState.jsonError: String?` → `ImportBoardsErrorReason?`. 2 triggers in `onSubmit()`: blank text, JSON decode failure.
- `ImportBoardsUiState.resultMessage: String?` → renamed `resultSummary: ImportResultSummary?`, `data class ImportResultSummary(val imported: Int, val skipped: Int)`. Not an error/reason — it carries the two counts needed by the 2-placeholder format string, so a plain data class (not an enum) is the smallest behaviorally-transparent replacement.

### Decision: Win-prediction candidate suffix — one format string, Kotlin-computed suffix arg

**Choice**: Single resource `win_prediction_candidate` = `"Cartón %1$s%2$s"` (es) / `"Card %1$s%2$s"` (en). The Composable keeps computing `val suffix = candidate.letter?.let { " (${it.name})" }.orEmpty()` exactly as today and passes it as `%2$s`.
**Alternatives considered**: Two resources (with-letter / without-letter) — rejected: would duplicate the "Cartón %1$s" prefix in both variants per locale and forces the leading-space-before-parenthesis formatting into `strings.xml` twice instead of once in Kotlin, raising drift risk between locales for no behavioral gain.
**Rationale**: Zero logic change (suffix computation untouched), single resource per locale, exact output byte-for-byte identical to today.

## Data Flow

    GamePlayViewModel.onSubmitCall()
          │ pending.update { it.copy(error = GamePlayInputErrorReason.DuplicateCall) }
          ▼
    GamePlayUiState.inputError: GamePlayInputErrorReason?
          │ collectAsStateWithLifecycle()
          ▼
    GamePlayContent (Composable)
          │ state.inputError?.let { it.toMessage() }   // private @Composable extension
          │      when (reason) { DuplicateCall -> stringResource(R.string.game_play_error_duplicate_call) ... }
          ▼
    BingoNumberField(supportingText = resolvedString)

Same shape for `CreateBoardErrorReason` (→ `CreateBoardScreen.kt`) and `ImportBoardsErrorReason` / `ImportResultSummary` (→ `ImportBoardsScreen.kt`, the latter feeding `stringResource(R.string.import_result_message, it.imported, it.skipped)` before `snackbarHostState.showSnackbar(...)`).

## File Changes — Phase A (resources + pure Composable swaps, no ViewModel changes)

| File | Action | Description |
|---|---|---|
| `app/src/main/res/values/strings.xml` | Modify | Add **all ~54 keys** (Spanish), including the 8 ViewModel-owned ones consumed only in Phase B |
| `app/src/main/res/values-en/strings.xml` | Create | Same ~54 keys, English translations |
| `ui/boards/list/BoardListScreen.kt` | Modify | 5 literals incl. `board_list_item_label` format string |
| `ui/boards/create/CreateBoardScreen.kt` | Modify | 2 static literals only ("Identificador" label, "Guardar cartón" button) — `identifierError` display untouched here |
| `ui/game/setup/GameSetupScreen.kt` | Modify | 10 literals incl. `GameMode.label()` → `@Composable` extension resolving 5 `game_setup_mode_*` keys |
| `ui/game/play/GamePlayScreen.kt` | Modify | 11 of 13 literals: static labels/buttons + 4 format strings (`game_play_mode_status`, `game_play_bingo_announcement`, `game_play_letter_calls`, `win_prediction_candidate`, `common_back`) — `inputError` display untouched here |
| `ui/boards/importexport/ImportBoardsScreen.kt` | Modify | 2 of 4 literals: static labels/button/`common_back` — `jsonError`/`resultMessage` display untouched here |
| `ui/boards/settings/SettingsScreen.kt` | Modify | 5 literals |
| `ui/boards/theme/ThemeScreen.kt` | Modify | 5 literals |
| `ui/common/BingoNumberField.kt` | Modify | 1 literal → `common_free_cell` |
| `ui/common/BingoGridDisplay.kt` | Modify | 1 literal → `common_free_cell` (dedup with above) |

Phase A leaves `CreateBoardViewModel.kt`, `GamePlayViewModel.kt`, `ImportBoardsViewModel.kt`, their 3 `UiState` files, and the 2 test files completely untouched.

## File Changes — Phase B (ViewModel reason-type refactors + their Composable consumption)

| File | Action | Description |
|---|---|---|
| `ui/boards/create/CreateBoardUiState.kt` | Modify | `identifierError: String?` → `CreateBoardErrorReason?`; add `sealed interface CreateBoardErrorReason { data object BlankIdentifier; data object DuplicateIdentifier }` in same file |
| `ui/boards/create/CreateBoardViewModel.kt` | Modify | `onSubmit()`'s 2 `.copy(identifierError = "...")` sites now assign the sealed values |
| `ui/boards/create/CreateBoardScreen.kt` | Modify | `supportingText = { state.identifierError?.let { Text(it) } }` → resolve via new private `@Composable fun CreateBoardErrorReason.toMessage(): String` |
| `ui/game/play/GamePlayUiState.kt` | Modify | `inputError: String?` → `GamePlayInputErrorReason?`; add `sealed interface GamePlayInputErrorReason { data object InvalidNumber; data object LetterMismatch; data object DuplicateCall }` in same file |
| `ui/game/play/GamePlayViewModel.kt` | Modify | Private `PendingEntry.error: String?` → `GamePlayInputErrorReason?`; `onSubmitCall()`'s 3 assignment sites updated |
| `ui/game/play/GamePlayScreen.kt` | Modify | `supportingText = state.inputError` → `state.inputError?.let { it.toMessage() }` via new private `@Composable` extension |
| `ui/boards/importexport/ImportBoardsUiState.kt` | Modify | `jsonError: String?` → `ImportBoardsErrorReason?`; `resultMessage: String?` → `resultSummary: ImportResultSummary?`; add both types in same file |
| `ui/boards/importexport/ImportBoardsViewModel.kt` | Modify | `onSubmit()`'s 2 `jsonError` sites + the `resultMessage` assignment updated |
| `ui/boards/importexport/ImportBoardsScreen.kt` | Modify | `state.jsonError?.let { Text(it) } }` and the snackbar text resolve via new private `@Composable` extensions |
| `app/src/test/.../game/play/GamePlayViewModelTest.kt` | Modify | Line 186 |
| `app/src/test/.../boards/importexport/ImportBoardsViewModelTest.kt` | Modify | Line 86 |

Phase B does **not** touch `strings.xml`/`values-en/strings.xml` — all 8 ViewModel-owned keys were already created in Phase A.

## Interfaces / Contracts

```kotlin
// ui/boards/create/CreateBoardUiState.kt
sealed interface CreateBoardErrorReason {
    data object BlankIdentifier : CreateBoardErrorReason
    data object DuplicateIdentifier : CreateBoardErrorReason
}

// ui/game/play/GamePlayUiState.kt
sealed interface GamePlayInputErrorReason {
    data object InvalidNumber : GamePlayInputErrorReason
    data object LetterMismatch : GamePlayInputErrorReason
    data object DuplicateCall : GamePlayInputErrorReason
}

// ui/boards/importexport/ImportBoardsUiState.kt
sealed interface ImportBoardsErrorReason {
    data object BlankInput : ImportBoardsErrorReason
    data object InvalidJson : ImportBoardsErrorReason
}
data class ImportResultSummary(val imported: Int, val skipped: Int)
```

Format strings (identical shape es/en unless noted):
| Key | es | en | Args |
|---|---|---|---|
| `board_list_item_label` | `"#%1$d · %2$s"` | same | `board.id: Long`, `board.identifier: String` |
| `game_play_mode_status` | `"Modo: %1$s · Llamadas: %2$d"` | `"Mode: %1$s · Calls: %2$d"` | `mode.name: String`, `calledCount: Int` |
| `game_play_bingo_announcement` | `"¡Bingo! #%1$d %2$s"` | `"Bingo! #%1$d %2$s"` | `sequentialNumber: Long`, `identifier: String` |
| `game_play_letter_calls` | `"%1$s: %2$s"` | same | `letter.name: String`, pre-joined calls `String` |
| `win_prediction_candidate` | `"Cartón %1$s%2$s"` | `"Card %1$s%2$s"` | `identifier: String`, Kotlin-computed `suffix: String` |
| `import_result_message` | `"%1$d importados, %2$d omitidos"` | `"%1$d imported, %2$d skipped"` | `imported: Int`, `skipped: Int` |

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | `GamePlayViewModelTest.kt:186` | `assertEquals(GamePlayInputErrorReason.DuplicateCall, state.inputError)` replaces the literal-text assertion; no other change to that test |
| Unit | `ImportBoardsViewModelTest.kt:86` | `assertEquals(ImportResultSummary(imported = 1, skipped = 1), state.resultSummary)` replaces the literal-text assertion |
| Unit | Full existing suite | Must stay green unmodified otherwise — no other test pins string content (verified: only these 2 of ~15 test files do) |
| Manual (device) | Every screen renders correctly in Spanish (default) and English (device locale set to `en`) | No Compose UI test infra exists project-wide; per standing convention (see `theme-switcher` design) |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary. Pure resource + presentation-layer change.

## Migration / Rollout

No data/schema migration. Phase A and Phase B are independently revertible: Phase A reverts by restoring literals in the 11 listed files and deleting `values-en/strings.xml` (and the `app_name`-only `values/strings.xml` reverts to its single entry); Phase B reverts by restoring the literal-`String` fields in the 3 `UiState` files, their 2 ViewModels, the 3 Composables' display sites, and the 2 test assertions — Phase A's `strings.xml` entries are harmless if left in place since nothing else changes.

## Open Questions

None — all decisions are resolved by the confirmed product decisions and this design.
