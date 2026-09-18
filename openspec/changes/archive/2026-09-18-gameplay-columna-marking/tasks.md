# Tasks: Columna Manual Win Dismissal, Grid Calls Presentation, End Game Action Relocation

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~180-260 (6 files, 0 new files) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Full change (ViewModel state → UI grid/swipe → TopAppBar → strings → tests) | PR 1 (single) | `./gradlew testDebugUnitTest --tests "*.GamePlayViewModelTest"` | Manual/device smoke test for swipe gesture and grid layout — N/A, no emulator in this environment (deferred, consistent with all 14 prior archived changes) | Revert the 6 modified files (`GamePlayViewModel.kt`, `GamePlayUiState.kt`, `GamePlayScreen.kt`, both `strings.xml`, `GamePlayViewModelTest.kt`); no Room/DB migration involved |

## Phase 1: ViewModel and State

- [x] 1.1 In `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayUiState.kt`, add `val dismissedLetters: Set<BingoLetter> = emptySet()` field to `GamePlayUiState`.
- [x] 1.2 In `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayViewModel.kt`, add `private const val KEY_DISMISSED_LETTERS = "dismissedLetters"` next to `KEY_CALLED_NUMBERS`.
- [x] 1.3 Add `private val dismissedLetters = MutableStateFlow(...)` initialized from `savedStateHandle.get<ArrayList<String>>(KEY_DISMISSED_LETTERS)?.mapNotNull { runCatching { BingoLetter.valueOf(it) }.getOrNull() }?.toSet() ?: emptySet()`, mirroring the `calledNumbers` init pattern.
- [x] 1.4 Add `fun onLetterDismissToggled(letter: BingoLetter)` that toggles `letter` in/out of `dismissedLetters.value` and writes `savedStateHandle[KEY_DISMISSED_LETTERS] = ArrayList(updated.map { it.name })`.
- [x] 1.5 Add `dismissedLetters` as a 4th source to the existing `combine(...)` block; filter `predictPossibleWinners(...)`'s result with `.filterNot { it.letter in dismissed }` before assigning to `GamePlayUiState.possibleWinners`, and pass `dismissed` through to the emitted `GamePlayUiState.dismissedLetters`. Do not change `predictPossibleWinners`'s signature or body.

## Phase 2: UI — Bordered Grid and Swipe-to-Dismiss

- [x] 2.1 In `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayScreen.kt`, remove the plain `BingoLetter.entries.forEach { Text(...) }` loop.
- [x] 2.2 Add a new private `LetterCallsGrid` composable rendering all 5 `BingoLetter` rows (B, I, N, G, O) for every `GameMode`, bordered/divided using Material3 `colorScheme.outline` (`border` + `HorizontalDivider`/`VerticalDivider`), matching `BingoGridDisplay`'s visual convention without reusing its fixed-5-cell code.
- [x] 2.3 Add a new private `LetterCallsRow` composable: when `state.mode == GameMode.COLUMNA` and the letter is not dismissed, wrap the row in `SwipeToDismissBox` (`EndToStart` only, `enableDismissFromStartToEnd = false`); `confirmValueChange` calls `onLetterDismissToggled(letter)` and returns `false` so the box springs back to `Settled`.
- [x] 2.4 In `LetterCallsRow`, render a dismissed Columna row as a plain (non-swipeable) dimmed row with `textDecoration = TextDecoration.LineThrough` and a reopen `IconButton` (`Icons.Default.Lock`) that calls `onLetterDismissToggled(letter)`.
- [x] 2.5 For non-Columna modes (`O`, `L`, `I`, `CARTON_COMPLETO`), render `LetterCallsRow` with no swipe/dismiss affordance regardless of `dismissedLetters` content.

## Phase 3: UI — TopAppBar End Game Relocation

- [x] 3.1 In `GamePlayScreen.kt`, trim `EndGameAction` to only its confirmation `AlertDialog`, taking `show: Boolean`, `onDismiss: () -> Unit`, `onConfirm: () -> Unit` — preserve title, message, confirm, and cancel behavior unchanged.
- [x] 3.2 In `GamePlayContent`, add `var showEndGameDialog by rememberSaveable { mutableStateOf(false) }` and pass it to `EndGameAction`.
- [x] 3.3 Add a `TopAppBar(actions = { IconButton(onClick = { showEndGameDialog = true }) { Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.game_play_end_game_icon_description)) } })`, replacing the previous body `Button` trigger.

## Phase 4: Strings

- [x] 4.1 In `app/src/main/res/values/strings.xml`, add `game_play_column_mark_won` ("Marcar como ganado"), `game_play_column_closed_label` ("Cerrado"), `game_play_column_reopen_description` ("Reabrir columna"), `game_play_end_game_icon_description` ("Terminar juego").
- [x] 4.2 In `app/src/main/res/values-en/strings.xml`, add the same 4 keys with English values ("Mark as won", "Closed", "Reopen column", "End game"), following the `{screen}_{element}` naming convention.

## Phase 5: ViewModel Tests

- [x] 5.1 In `app/src/test/java/com/sergiodev/bingo/ui/game/play/GamePlayViewModelTest.kt`, add a test asserting `onLetterDismissToggled(letter)` removes that letter's candidate from `possibleWinners` (per spec scenario "Dismissing removes it from possible winners immediately").
- [x] 5.2 Add a test asserting dismissal does not affect `winners`/automatic win detection — a dismissed column that later completes via real called numbers still announces normally (per spec scenario "A dismissed column can still announce a real win later").
- [x] 5.3 Add a test asserting toggling a dismissed, still-qualifying letter again restores it in `possibleWinners` (per spec scenario "Reopening restores prediction eligibility").
- [x] 5.4 Add a test asserting `dismissedLetters` survives simulated `SavedStateHandle` restoration, mirroring the existing `calledNumbers` restoration test pattern, using a new `handle(dismissedLetters = ...)` test helper param (per spec scenario "Dismissed letters survive simulated process death").

## Phase 6: Verification

- [x] 6.1 Run `./gradlew testDebugUnitTest` and confirm all tests pass, including the 4 new cases from Phase 5. — 12/12 tests passed in `GamePlayViewModelTest`, full suite green.
- [x] 6.2 Run `./gradlew build` as the build gate. — exit code 0.
- [x] 6.3 Run `rg` to confirm `predictPossibleWinners`'s function signature is unchanged and that `BingoWinChecker.kt`, `WinAnnouncement.kt`, `AnnouncedWin.kt`, `GameMode.kt` have zero diff against `master`. — confirmed via `git diff`, zero output on all 4 domain files; signature intact.
- [x] 6.4 Manual/device smoke test for the swipe gesture, closed-row reopen, and grid layout across all 5 `GameMode` values — mark expected-deferred (no emulator in this environment, consistent with all 14 prior archived changes).
