# Apply Progress: Columna Manual Win Dismissal, Grid Calls Presentation, End Game Action Relocation

**Change**: gameplay-columna-marking
**Mode**: Standard (no strict TDD flag)
**Status**: 23/23 tasks complete — Ready for verify

## Completed Tasks

### Phase 1: ViewModel and State
- [x] 1.1 `GamePlayUiState.dismissedLetters: Set<BingoLetter> = emptySet()` field added
- [x] 1.2 `KEY_DISMISSED_LETTERS` constant added next to `KEY_CALLED_NUMBERS`
- [x] 1.3 `dismissedLetters` `MutableStateFlow` initialized from `SavedStateHandle`, mirroring `calledNumbers`
- [x] 1.4 `onLetterDismissToggled(letter)` toggles the set and persists to `savedStateHandle`
- [x] 1.5 `dismissedLetters` added as 4th `combine` source; `predictPossibleWinners(...)` filtered with `.filterNot { it.letter in dismissed }` before assignment; `predictPossibleWinners` signature/body untouched

### Phase 2: UI — Bordered Grid and Swipe-to-Dismiss
- [x] 2.1 Removed the plain `BingoLetter.entries.forEach { Text(...) }` loop
- [x] 2.2 New private `LetterCallsGrid` composable: bordered `Column` (`colorScheme.outline`), all 5 `BingoLetter` rows, every `GameMode`
- [x] 2.3 New private `LetterCallsRow`: Columna + not-dismissed rows wrap in `SwipeToDismissBox` (`EndToStart` only), `confirmValueChange` fires the toggle and always returns `false`
- [x] 2.4 Dismissed Columna rows render as a plain dimmed/struck-through row with a reopen `IconButton` (`Icons.Default.Lock`)
- [x] 2.5 Non-Columna modes (`O`, `L`, `I`, `CARTON_COMPLETO`) render `LetterCallsRow` with zero swipe/dismiss affordance regardless of `dismissedLetters`

### Phase 3: UI — TopAppBar End Game Relocation
- [x] 3.1 `EndGameAction` trimmed to `show`/`onDismiss`/`onConfirm` dialog-only; title/message/confirm/cancel behavior unchanged
- [x] 3.2 `showEndGameDialog` state lifted to `GamePlayContent` via `rememberSaveable`
- [x] 3.3 `TopAppBar(actions = { IconButton(Icons.Default.Stop) })` added, replacing the body `Button` trigger

### Phase 4: Strings
- [x] 4.1 4 new keys added to `values/strings.xml` (ES)
- [x] 4.2 Same 4 keys added to `values-en/strings.xml` (EN)

### Phase 5: ViewModel Tests
- [x] 5.1 `dismissingLetter_removesItFromPossibleWinners`
- [x] 5.2 `dismissingLetter_doesNotAffectAutomaticWinDetection`
- [x] 5.3 `reopeningDismissedLetter_restoresPredictionEligibility`
- [x] 5.4 `dismissedLetters_surviveSimulatedProcessDeath` (new `handle(dismissedLetters = ...)` param)

### Phase 6: Verification
- [x] 6.1 `./gradlew testDebugUnitTest` — full suite green; `GamePlayViewModelTest` 12/12 passed
- [x] 6.2 `./gradlew build` — exit code 0
- [x] 6.3 `rg`/`git diff` confirmed zero diff on `BingoWinChecker.kt`, `WinAnnouncement.kt`, `AnnouncedWin.kt`, `GameMode.kt`; `predictPossibleWinners`'s signature unchanged
- [x] 6.4 Manual/device smoke test — expected-deferred (no emulator in this environment, consistent with all 14 prior archived changes)

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayUiState.kt` | Modified | Added `dismissedLetters: Set<BingoLetter> = emptySet()` |
| `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayViewModel.kt` | Modified | Added `KEY_DISMISSED_LETTERS`, `dismissedLetters` state, `onLetterDismissToggled`, 4th `combine` source + filter |
| `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayScreen.kt` | Modified | New `LetterCallsGrid`/`LetterCallsRow` composables; `TopAppBar(actions = {...})`; `EndGameAction` trimmed to dialog-only with lifted state |
| `app/src/main/res/values/strings.xml` | Modified | 4 new keys (ES): `game_play_column_mark_won`, `game_play_column_closed_label`, `game_play_column_reopen_description`, `game_play_end_game_icon_description` |
| `app/src/main/res/values-en/strings.xml` | Modified | Same 4 keys (EN) |
| `app/src/test/java/com/sergiodev/bingo/ui/game/play/GamePlayViewModelTest.kt` | Modified | New `handle(dismissedLetters = ...)` param + 4 new test cases |

## Deviations from Design

None functionally. One implementation-level deviation reconciled with the design's own flagged open question:

- **`SwipeToDismissBox` / `rememberSwipeToDismissBoxState` API surface** — the design's open question asked to confirm exact parameter names against the pinned `composeBom = 2026.02.01` (Material3 1.4.0). Verified by extracting and reading the Material3 1.4.0 sources: the non-deprecated `rememberSwipeToDismissBoxState(initialValue, positionalThreshold)` overload does **not** accept `confirmValueChange` — that parameter only exists on a `@Deprecated(level = WARNING)` overload (`rememberSwipeToDismissBoxState(initialValue, confirmValueChange, positionalThreshold)`), which the design's veto-and-spring-back interaction requires. Used the deprecated overload with `@Suppress("DEPRECATION")` on `LetterCallsRow` since the project has no `allWarningsAsErrors`/`Werror` compiler flag, so the deprecation is a warning, not a build error, and this is the only API path that gives the design's "always return false to settle back at `Settled`" veto semantics. Verified compiling cleanly via `./gradlew :app:compileDebugKotlin`.
- Left the now-unused `game_play_end_game_button` string key in place in both locale files rather than deleting it — the design/tasks scope covers adding new keys, not pruning; removing it wasn't in scope and risks an unrelated diff.

## Issues Found

None.

## Workload / PR Boundary

- Mode: single PR (forecast: Low risk, no chaining needed)
- Current work unit: Unit 1 — full change (ViewModel state → UI grid/swipe → TopAppBar → strings → tests)
- Boundary: starts from a clean `master` and ends with all 23 tasks complete, tests green, build gate green
- Estimated review budget impact: ~299 changed lines (`+253/-46` net per `git diff --stat`, entirely within the forecasted 180-260 authored range plus test additions), well under the 400-line budget

## Status

23/23 tasks complete. Ready for verify.
