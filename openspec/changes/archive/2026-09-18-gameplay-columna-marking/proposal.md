# Proposal: Columna Manual Win Dismissal, Grid Calls Presentation, End Game Action Relocation

## Intent

`GamePlayScreen` is one shared composable across all 5 `GameMode`s. Only `GameMode.COLUMNA` has 5 independent per-letter `WinPattern`s, so a per-letter manual marker is structurally meaningful only there. Today the operator tracks their own boards against called numbers with zero way to note that an outside player at the physical bingo hall already won a column: the app keeps treating that column as a live near-win candidate. This change adds a lightweight, session-local "closed" marker per `BingoLetter` (Columna only) that stops surfacing a closed letter in "Posibles ganadores," restructures the per-letter calls section into a bordered grid (all modes, presentation-only), and moves "Terminar juego" from a body button into a `TopAppBar` action icon.

## Scope

### In Scope
- `GamePlayViewModel`: new `dismissedLetters: Set<BingoLetter>` state, `SavedStateHandle`-persisted like `calledNumbers` (`KEY_DISMISSED_LETTERS`), toggle action `onToggleLetterDismissed(letter)`.
- `GamePlayUiState`: new `dismissedLetters` field; ViewModel filters the already-computed `predictPossibleWinners` result by this set before assigning `possibleWinners` — `predictPossibleWinners`'s signature and domain logic stay untouched.
- New bordered-grid composable for the per-letter calls section (mirrors `BingoGridDisplay`'s `colorScheme.outline` convention, not its fixed-5×5 code), used by all modes.
- Columna-only: `SwipeToDismissBox` per letter row revealing "Marcar como ganado"; closing shows a dimmed/strikethrough "Cerrado" row. Tapping the closed badge reopens it (toggle); swipe is inert while closed — simplest reversible design, avoids two competing gestures for the same state.
- `TopAppBar` gains its first `actions =` icon (`Icons.Default.Stop`, already in the pinned `material-icons-extended` dependency — no new dependency) triggering the existing unchanged `EndGameAction` confirmation `AlertDialog`.
- New strings in `values/strings.xml` and `values-en/strings.xml`: swipe action label, closed badge label, end-game icon content description.
- JVM unit tests for `dismissedLetters` toggle/persistence/filtering in `GamePlayViewModelTest.kt`.

### Out of Scope
- `BingoWinChecker`, `WinAnnouncement`, `AnnouncedWin`, automatic win detection — unchanged for all modes; dismissal never forces or records a real win.
- O, L, I, CARTON_COMPLETO — no dismissal UI or state, grid presentation only.
- Compose UI test infrastructure — none exists project-wide; swipe interaction verified manually/on device.
- `predictPossibleWinners`'s function signature and domain rules.

## Capabilities

### New Capabilities
- `game-play-screen`: per-letter calls grid presentation (all modes), Columna-only manual win dismissal and its effect on "Posibles ganadores," and the `TopAppBar` end-game action.

### Modified Capabilities
- None (no existing spec documents `GamePlayScreen`'s layout today).

## Approach

Extend `GamePlayViewModel`'s existing `SavedStateHandle` pattern (`calledNumbers`) with a second persisted set, `dismissedLetters`, mutated only by the new toggle action. In the `combine` block, filter `predictPossibleWinners(...)`'s result by `candidate.letter !in dismissedLetters` before storing it — no domain change. In `GamePlayScreen`, replace the 5 plain `Text` rows with one bordered-grid composable; when `state.mode == GameMode.COLUMNA`, each row is wrapped in `SwipeToDismissBox` bound to `onToggleLetterDismissed`. Move `EndGameAction`'s trigger into `TopAppBar(actions = {...})`, keeping its `AlertDialog` untouched.

## Affected Areas

| Area | Impact | Description |
|------|--------|--------------|
| `ui/game/play/GamePlayScreen.kt` | Modified | Grid calls section, Columna swipe UI, `TopAppBar` action |
| `ui/game/play/GamePlayViewModel.kt` | Modified | `dismissedLetters` state, toggle action, filtering |
| `ui/game/play/GamePlayUiState.kt` | Modified | New `dismissedLetters` field |
| `res/values(-en)/strings.xml` | Modified | 3 new strings, both locales |
| `test/.../GamePlayViewModelTest.kt` | Modified | Toggle/persist/filter coverage |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|--------------|
| Swipe interaction has zero precedent/test coverage in this codebase | Low | Standard Material3 `SwipeToDismissBox`, no new dependency; manual/device verification |
| Dismissal and automatic detection visually coexist with no linkage | Low | Intentional per product decision; document in design |
| First `TopAppBar actions =` usage app-wide | Low | Standard Material3 pattern, existing dialog unchanged |

## Rollback Plan

Revert `GamePlayScreen.kt`/`GamePlayViewModel.kt`/`GamePlayUiState.kt` to restore plain `Text` rows, remove `dismissedLetters` and its `SavedStateHandle` key, and move `EndGameAction` back to a body `Button`. No persisted data beyond the current session is affected — `dismissedLetters` never reaches Room.

## Dependencies

- None new — `material-icons-extended` and Compose Material3 `SwipeToDismissBox` are already available.

## Success Criteria

- [ ] Marking a Columna letter closed removes its candidates from "Posibles ganadores" immediately, for all boards.
- [ ] Automatic win detection for the operator's own boards is unaffected by any dismissal.
- [ ] Reopening a closed letter restores its candidates when still numerically qualifying.
- [ ] O, L, I, CARTON_COMPLETO show the new grid with no dismissal affordance.
- [ ] "Terminar juego" triggers the unchanged confirmation dialog from the `TopAppBar` icon.
- [ ] `./gradlew build` and full unit test suite pass with no regressions.

---
Estimated diff size: ~180-260 changed lines (comfortably under the 400-line review budget) — one screen, one ViewModel, one state class, no domain layer, no new dependencies.
